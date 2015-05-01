package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import com.xmail.IO.FileUtils;
import com.xmail.Threads.NotifyingThread;

import java.io.IOException;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailThread extends NotifyingThread {

    int mailId;

    public void doRun() {
        String to, mail_path, data;
        boolean status = false;

        XmailQueue queue = new XmailQueue();
        queue.init(XmailConfig.dbPath);

        QueuedMails mail = queue.getEmail(mailId);

        to = mail.get("mail_to").toString();
        mail_path = mail.get("email_path").toString();

        try {
            data = FileUtils.getFileContent(mail_path);
        }
        catch (IOException e) {
            mail.set("status", 0);
            mail.saveIt();
            return;
        }

        XmailSend sender = new XmailSend();

        sender.port = 24;

        sender.ehlo = XmailConfig.ehlo;
        sender.from = mail.get("mail_from").toString();

        sender.bindingIP = "0.0.0.0";

        try {
            status = sender.send(to, data);

            if (status) {
                // okay
                mail.delete();
            } else {
                // check the log and queue if necessarily
                mail.set("status", 0);
                mail.saveIt();
            }
        }
        finally {
            // queue it?
            if(!status) {
                mail.set("status", 0);
                mail.saveIt();
            }
        }

        System.out.println(sender.log);

    }

    public void addMailId(int id) {
        mailId = id;
    }
}
