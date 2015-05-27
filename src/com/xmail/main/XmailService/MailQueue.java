package com.xmail.main.XmailService;

import com.xmail.Config;
import com.xmail.main.IO.FileUtils;
import com.xmail.main.XmailService.Models.QueuedMails;
import org.apache.log4j.Logger;
import org.javalite.activejdbc.Base;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cristian on 4/30/15.
 */
public class MailQueue {
    final static Logger logger = Logger.getLogger(MailQueue.class);

    private static MailQueue instance = null;
    private static String dbPath;

    /**
     * MailQueue.MailQueue()
     *
     * Exists only to defeat instantiation
     *
     */
    protected MailQueue() {
    }

    /**
     * MailQueue.getInstance()
     *
     * Returns the instance of this singleton class
     *
     * @return
     */
    public static synchronized MailQueue getInstance() {
        if(instance == null) {
            instance = new MailQueue();
        }
        return instance;
    }

    /**
     * MailQueue.init()
     *
     * Initialize the queue
     *
     * @param path path to SQLite database
     */
    public static synchronized boolean init(String path) {

        try {
            File dbFile = new File(path);
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
            if(!dbFile.canRead()) {
                dbFile.delete();
                dbFile.createNewFile();
            }
            dbPath = dbFile.getAbsolutePath();
        }
        catch(IOException e) {
            logger.error("Database creation error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * MailQueue.open()
     *
     * Open the Database connection
     */
    public static synchronized boolean open() {
        try {
            Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + dbPath + "", "", "");
            createQueue();
        }
        catch(Exception e) {
            logger.error("Database open error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * MailQueue.close()
     *
     * Close the Database connection
     */
    public static synchronized void close() {
        try {
            Base.close();
        }
        catch(Exception e) {
            logger.error("Database close error: " + e.getMessage());
        }
    }

    /**
     * MailQueue.createQueue()
     *
     *  Creates the queue table if it not exists.
     */
    public static void createQueue() {
        Base.exec("CREATE TABLE IF NOT EXISTS queued_mails (\n" +
                "    id             INTEGER     PRIMARY KEY,\n" +
                "    mail_from      STRING      NOT NULL,\n" +
                "    mail_to        STRING      NOT NULL,\n" +
                "    email_path     STRING      NOT NULL,\n" +
                "    date_added     DATETIME    NOT NULL,\n" +
                "    date_processed DATETIME    NOT NULL,\n" +
                "    status         INTEGER     NOT NULL\n" +
                "                               DEFAULT (0),\n" +
                "    retry          INTEGER (1) NOT NULL\n" +
                "                               DEFAULT (0),\n" +
                "    mx_ctr         INTEGER (4) NOT NULL\n" +
                "                               DEFAULT (0),\n" +
                "    ip_ctr         INTEGER (4) NOT NULL\n" +
                "                               DEFAULT (0),\n" +
                "    bind_ipv4      STRING (15) NOT NULL\n" +
                "                               DEFAULT (''),\n" +
                "    bind_ipv6      STRING (39) NOT NULL\n" +
                "                               DEFAULT (''),\n" +
                "    ipv6_used      INTEGER (1) DEFAULT (0) \n" +
                "                               NOT NULL,\n" +
                "    last_code      INT (3)     NOT NULL\n" +
                "                               DEFAULT (0),\n" +
                "    last_message   STRING      NOT NULL\n" +
                "                               DEFAULT ('') \n" +
                ");");
    }

    /**
     * MailQueue.getEmails()
     *
     * Returns a list of emails to be processed
     *
     * @param limit specify how many emails to return
     * @return a list of ActiveRecord email objects
     */
    public static synchronized List<QueuedMails> getEmails(int limit) {
        List<QueuedMails> ret = QueuedMails.where("status = 0 AND retry < ? AND date_processed < datetime('now')",
                Config.maxRetryCount - 1)
                .limit(limit)
                .orderBy("date_processed asc");
        return ret;
    }

    /**
     * MailQueue.queueEmail
     *
     * Puts an email back to queue
     *
     * @param mail the ActiveRecord mail object
     * @param lastCode last error Code
     * @param lastMessage last error message
     * @param ipIndex last index used for IP (round robin)
     * @param mxIndex last index used for MX (round robin)
     * @param ipv6Used if IPv6 was used for transmission
     * @param bindingIPv4 the last IPv4 address used for outgoing
     * @param bindingIPv6 the last IPv6 address used for outgoing
     * @return true if the mail was queued | false if the email reached the maximum Retry
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
                           boolean ipv6Used, String bindingIPv4, String bindingIPv6) {

        if(mail != null) {
            bindingIPv4 = (bindingIPv4 != null ? bindingIPv4 : "");
            bindingIPv6 = (bindingIPv6 != null ? bindingIPv6 : "");


            int retry = (Integer) mail.get("retry");
            if(retry < Config.maxRetryCount - 1) {
                retry++;

                long timeAdj = Config.retryTime.get(retry) * 1000;

                /* This is a crazy approach to get the time in UTC ? */
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                Calendar calendar = Calendar.getInstance(timeZone);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.US);
                simpleDateFormat.setTimeZone(timeZone);
                calendar.setTime(new Date(new Date().getTime() + timeAdj));

                mail.set("retry", retry);
                mail.set("date_processed", simpleDateFormat.format(calendar.getTime()));
                mail.set("status", 0);
                mail.set("last_code", Integer.toString(lastCode));
                mail.set("last_message", lastMessage);
                mail.set("mx_ctr", mxIndex);
                mail.set("ip_ctr", ipIndex);
                mail.set("ipv6_used", ipv6Used);
                mail.set("bind_ipv4", bindingIPv4);
                mail.set("bind_ipv6", bindingIPv6);
                mail.saveIt();
            }

            if(retry >= Config.maxRetryCount - 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * MailQueue.getEmail()
     *
     * Returns an email from queue by ID
     *
     * @param id the id of the email to be retrieved
     * @return the ActiveRecord object containing the email information
     */
    public static synchronized QueuedMails getEmail(int id) {
        return QueuedMails.findFirst("id = ? ", id);
    }

    /**
     * MailQueue.deleteEmail()
     *
     * Remove Email from queue and from disk
     *
     * @param mail the ActiveRecord email object to be deleted
     */
    public static synchronized void deleteEmail(QueuedMails mail) {
        String path = mail.get("email_path").toString();
        FileUtils.deleteFile(path);

        mail.delete();
    }

    /**
     * MailQueue.addEmail()
     *
     * Adds a new email to queue
     *
     * @param to
     * @param from
     * @param pathToFile
     */
    public static synchronized void addEmail(String to, String from, String pathToFile) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.US);
        simpleDateFormat.setTimeZone(timeZone);
        calendar.setTime(new Date());

        QueuedMails mail = new QueuedMails();
        mail.set("mail_to", to);
        mail.set("mail_from", from);
        mail.set("email_path", pathToFile);
        mail.set("date_added", simpleDateFormat.format(calendar.getTime()));
        mail.set("date_processed", simpleDateFormat.format(calendar.getTime()));

        mail.saveIt();
    }

    /**
     * MailQueue.queueEmail()
     *
     * Overloaded method for queueEmail()
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     * @param ipIndex
     * @param mxIndex
     * @param ipv6Used
     * @param bindingIPv4
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
                           boolean ipv6Used, String bindingIPv4) {

        return queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, bindingIPv4, "");
    }


    /**
     * MailQueue.queueEmail()
     *
     * Overloaded method for queueEmail()
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     * @param ipIndex
     * @param mxIndex
     * @param ipv6Used
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
                           boolean ipv6Used) {

        return queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, "", "");
    }

    /**
     * MailQueue.queueEmail()
     *
     * Overloaded method for queueEmail()
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     * @param ipIndex
     * @param mxIndex
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex) {
        return queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, false);
    }

    /**
     * MailQueue.queueEmail()
     *
     * Overloaded method for queueEmail()
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     * @param ipIndex
     */
    public synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex) {
        return queueEmail(mail, lastCode, lastMessage, ipIndex, 0);
    }

    /**
     * MailQueue.queueEmail()
     *
     * Overloaded method for queueEmail()
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage) {
        return queueEmail(mail, lastCode, lastMessage, 0, 0);
    }

    /**
     * MailQueue.changeEmailStatus()
     *
     * Changes the email status between waiting | processing
     *
     * @param mail
     * @param status
     * @return
     */
    public static synchronized boolean changeEmailStatus(QueuedMails mail, int status) {
        try {
            mail.set("status", status).saveIt();
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * MailQueue.reset()
     *
     * Set all "sending" mails back to "ready to be sent" state.
     *
     * This method should be used when the service starts,
     * because if the service died during an email sending transaction,
     * it is possible that some emails remained marked as "sending".
     */
    public static synchronized void reset() {
        Base.exec("UPDATE queued_mails SET status = 0");
    }

}
