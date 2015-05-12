package com.xmail.XmailService;

import com.xmail.IO.FileUtils;
import com.xmail.XmailService.Models.QueuedMails;
import org.javalite.activejdbc.Base;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cristian on 4/30/15.
 */
public class MailQueue {

    /**
     * MailQueue.init()
     *
     * Initialize the queue
     *
     * @param dbPath path to SQLite database
     */
    public static void init(String dbPath) {

        Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + dbPath, "", "");

        createQueue();
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
                "    status         INTEGER     NOT NULL,\n" +
                "    retry          INTEGER (1) NOT NULL,\n" +
                "    mx_ctr         INTEGER (4) NOT NULL,\n" +
                "    ip_ctr         INTEGER (4) NOT NULL,\n" +
                "    bind_ipv4      STRING (15) NOT NULL,\n" +
                "    bind_ipv6      STRING (39) NOT NULL,\n" +
                "    last_code      INT (3)     NOT NULL,\n" +
                "    last_message   STRING      NOT NULL\n" +
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
                XmailConfig.maxRetryCount)
                .limit(limit)
                .orderBy("date_processed asc");
        return ret;
    }

    /**
     * MailQueue.queueEmail
     *
     * Puts an email back to queue
     *
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

            int retry = (Integer) mail.get("retry");
            if(retry < XmailConfig.maxRetryCount) {
                long timeAdj = XmailConfig.retryTime.get(retry) * 60 * 1000;

                /* This is a crazy approach to get the time in UTC ? */
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                Calendar calendar = Calendar.getInstance(timeZone);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.US);
                simpleDateFormat.setTimeZone(timeZone);
                calendar.setTime(new Date(new Date().getTime() + timeAdj));

                mail.set("retry", (Integer) mail.get("retry") + 1);
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
            else {
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

        return queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, bindingIPv4, "::1");
    }


    /**
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

        return queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, "0.0.0.0", "::1");
    }

    /**
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
     *
     * @param mail
     * @param lastCode
     * @param lastMessage
     */
    public static synchronized boolean queueEmail(QueuedMails mail, int lastCode, String lastMessage) {
        return queueEmail(mail, lastCode, lastMessage, -1, -1);
    }

}
