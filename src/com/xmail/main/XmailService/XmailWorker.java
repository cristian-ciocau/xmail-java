package com.xmail.main.XmailService;

import com.xmail.main.Config;
import com.xmail.main.SMTP.AdvancedSender;
import com.xmail.main.XmailService.Models.QueuedMails;
import com.xmail.main.IO.FileUtils;
import com.xmail.main.SMTP.SMTP;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailWorker extends Thread {
    final static Logger logger = Logger.getLogger(XmailWorker.class);

    /**
     * XmailWorker.process()
     *
     * Sends a new or queued email
     *
     */
    public void process(int mailId) {
        final String to, data;
        String mail_path = "";
        int status;
        QueuedMails mail = null;

        logger.info("XmailWorker started");

        MailQueue queue = MailQueue.getInstance();
        if(!queue.open()) {
            logger.error("Could not open the database");
            return;
        }

        try {

            mail = queue.getEmail(mailId);

            to = mail.get("mail_to").toString();
            mail_path = mail.get("email_path").toString();

            data = FileUtils.getFileContents(mail_path);

            int retryCount = Integer.parseInt(mail.get("retry").toString());

            AdvancedSender sender = new AdvancedSender();

            // Set the port specified in config
            sender.port = Config.port;

            sender.ehlo = Config.ehlo;
            sender.from = mail.get("mail_from").toString();

            // If we are attempting to send a queued mail, we need to use the last used settings
            if(retryCount > 0) {
                sender.bindingIPv4 = mail.get("bind_ipv4").toString();
                sender.bindingIPv6 = mail.get("bind_ipv6").toString();

                sender.setMxIndex(Integer.parseInt(mail.get("mx_ctr").toString()));
                sender.setIpIndex(Integer.parseInt(mail.get("ip_ctr").toString()));
                sender.setIpv6Used(Boolean.parseBoolean(mail.get("ipv6_used").toString()));
            }

            // if the last error was occurred during wrapping socket for TLS, skip TLS this time
            if(Integer.parseInt(mail.get("last_code").toString()) == 105) {
                sender.disableTls();
            }

            logger.info("Start sending mail for <" + to + ">, attempt no. " + Integer.toString(retryCount));

            // And send!
            status = sender.send(to, data);

            if (status == SMTP.SUCCESS) {
                // okay
                queue.deleteEmail(mail);
                logger.info("Email for <" + to + "> was sent okay.");
            }
            else if(status == SMTP.MAILBOX_NOT_EXISTS || status == SMTP.PERMANENT_ERROR) {

                // send bounce
                XmailBounce bounce = new XmailBounce();
                bounce.sendBounce(mail.get("mail_from").toString(), mail.get("mail_to").toString(), mail_path,
                        sender.getLastCode(), sender.getLastMessage(), Integer.parseInt(mail.get("retry").toString()),
                        sender.getRemoteMta());

                // remove the file from disk
                queue.deleteEmail(mail);

                logger.info("Email for <" + to + "> could not be delivered. Not queued.");
            }
            else {
                // check the log and queue if necessarily
                if(queue.queueEmail(mail, sender.getLastCode(), sender.getLastMessage(), sender.getIpIndex(),
                        sender.getMxIndex(), sender.isIpv6Used(), sender.getIpv4(), sender.getIpv6())) {

                    logger.info("Email for <" + to + "> could not be delivered. Queued.");
                }
                else {
                    // send bounce
                    XmailBounce bounce = new XmailBounce();
                    bounce.sendBounce(mail.get("mail_from").toString(), mail.get("mail_to").toString(), mail_path,
                            sender.getLastCode(), sender.getLastMessage(), Integer.parseInt(mail.get("retry").toString()),
                            sender.getRemoteMta());

                    // remove the file from disk
                    queue.deleteEmail(mail);

                    logger.info("Email for <" + to + "> could not be delivered. Status code= " + Integer.toString(status) + ". Not queued.");
                }
            }

        } catch (IOException e) {
            logger.error("Can not open email file: " + mail_path);
        }
        catch (Exception e) {
            logger.error("Unknown error occurred during sending email: " + e.getMessage());
        }
        finally {

            // Mark the email as "not processed" if the system crashed
            if(mail != null) {
                queue.changeEmailStatus(mail, 0);
            }

            // Close the queue
            queue.close();
        }

        logger.info("XmailWorker finished");
    }
}
