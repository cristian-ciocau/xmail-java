package com.xmail.XmailService;

import com.xmail.XmailService.Models.QueuedMails;
import com.xmail.IO.FileUtils;
import com.xmail.SMTP.SMTP;
import com.xmail.Threads.NotifyingThread;
import com.xmail.XmailSender.XmailSender;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailThread extends NotifyingThread {
    final static Logger logger = Logger.getRootLogger();

    int mailId;

    public void doRun() {
        final String to, data;
        String mail_path = "";
        int status = SMTP.UNKNOWN_ERROR;
        QueuedMails mail = null;
        XmailQueue queue = null;

        logger.info("Start sending email...");

        String bindingIPv4 = "0.0.0.0";
        String bindingIPv6 = "::1";


        try {
            queue = new XmailQueue();
            queue.init(XmailConfig.dbPath);

            mail = queue.getEmail(mailId);

            to = mail.get("mail_to").toString();
            mail_path = mail.get("email_path").toString();

            data = FileUtils.getFileContent(mail_path);

            XmailSender sender = new XmailSender();

            sender.port = 24;

            sender.ehlo = XmailConfig.ehlo;
            sender.from = mail.get("mail_from").toString();

            sender.bindingIPv4 = bindingIPv4;
            sender.bindingIPv6 = bindingIPv6;

            status = sender.send(to, data);

            if (status == SMTP.SUCCESS) {
                // okay
                mail.delete();
                logger.info("Email was sent okay.");
            }
            else if(status == SMTP.MAILBOX_NOT_EXISTS || status == SMTP.PERMANENT_ERROR) {
                // do not queue this email
                queue.saveError(mail, sender.getLastCode(), sender.getLastMessage());

                // avoid adding again this mail to queue
                mail = null;

                logger.info("Email could not be delivered. Not queued.");
            }
            else {
                // check the log and queue if necessarily
                queue.queueEmail(mail, sender.getLastCode(), sender.getLastMessage(), sender.getIpIndex(),
                        sender.getMxIndex(), sender.isIpv6Used(), bindingIPv4, bindingIPv6);

                // avoid adding again this mail to queue
                mail = null;

                logger.info("Email could not be delivered. Qeued.");
            }

        } catch (IOException e) {
            logger.error("Can not open email file: " + mail_path);
        }
        catch (UnknownError e) {
            logger.error("Unknown error occurred during sending email: " + e.getMessage());
        }
        finally {
            // queue it?
            if(status != SMTP.SUCCESS && mail != null && queue != null) {
                queue.queueEmail(mail, 999, "internal error");
            }
        }

    }

    public void addMailId(int id) {
        mailId = id;
    }
}
