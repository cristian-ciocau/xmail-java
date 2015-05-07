package com.xmail.XmailService;

import com.xmail.XmailService.Models.QueuedMails;
import org.javalite.activejdbc.Base;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cristian on 4/30/15.
 */
public class XmailQueue {

    public void init(String dbPath) {

        Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + dbPath, "", "");

        createQueue();
    }

    public void createQueue() {
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

    public List<QueuedMails> getEmails(int limit) {
        List<QueuedMails> ret = QueuedMails.where("status = 0 AND retry < ? AND date_processed < datetime('now')",
                XmailConfig.maxRetryCount)
                .limit(limit)
                .orderBy("date_processed asc");
        return ret;
    }

    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
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
                mail.set("binding_ipv6", bindingIPv6);
                mail.saveIt();
            }
        }
    }

    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
                           boolean ipv6Used, String bindingIPv4) {

        queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, bindingIPv4, "::1");
    }


    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex,
                           boolean ipv6Used) {

        queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, ipv6Used, "0.0.0.0", "::1");
    }

    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex, int mxIndex) {
        queueEmail(mail, lastCode, lastMessage, ipIndex, mxIndex, false);
    }

    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage, int ipIndex) {
        queueEmail(mail, lastCode, lastMessage, ipIndex, 0);
    }

    public void queueEmail(QueuedMails mail, int lastCode, String lastMessage) {
        queueEmail(mail, lastCode, lastMessage, -1, -1);
    }

    public void saveError(QueuedMails mail, int lastCode, String lastMessage) {
        if(mail != null) {
            mail.set("status", 3);
            mail.set("last_code", Integer.toString(lastCode));
            mail.set("last_message", lastMessage);
            mail.saveIt();
        }
    }

    public QueuedMails getEmail(int id) {
        return QueuedMails.findFirst("id = ? ", id);
    }
}
