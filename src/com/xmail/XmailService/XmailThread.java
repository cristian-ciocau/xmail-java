package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import com.xmail.IO.FileUtils;
import com.xmail.Threads.NotifyingThread;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailThread extends NotifyingThread {
    final static Logger logger = Logger.getRootLogger();

    int mailId;

    public void doRun() {
        String to = "", mail_path = "", data = "";
        boolean status = false;

        QueuedMails mail = null;

        try {
            XmailQueue queue = new XmailQueue();
            queue.init(XmailConfig.dbPath);

            mail = queue.getEmail(mailId);

            to = mail.get("mail_to").toString();
            mail_path = mail.get("email_path").toString();

            data = FileUtils.getFileContent(mail_path);

            XmailSend sender = new XmailSend();

            sender.port = 24;

            sender.ehlo = XmailConfig.ehlo;
            sender.from = mail.get("mail_from").toString();

            sender.bindingIP = "0.0.0.0";

            status = sender.send(to, data);

            if (status) {
                // okay
                mail.delete();
                logger.info("Email " + to +  " for was sent okay.");
            } else {
                // check the log and queue if necessarily
                mail.set("status", 0);
                mail.saveIt();

                logger.info("Email " + to +  " could not be delivered.");
            }
        } catch (IOException e) {
            logger.error("Can not open email file: " + mail_path);
        }
        catch (UnknownError e) {
            logger.error("Unknown error occurred during sending the email for " + to + " : " + e.getMessage());
        }
        finally {
            // queue it?
            if(!status && mail != null) {
                mail.set("status", 0);
                mail.saveIt();
            }
        }

    }

    public void addMailId(int id) {
        mailId = id;
    }
}
