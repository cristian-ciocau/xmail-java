package com.xmail.XmailService;

import com.xmail.SMTP.AdvancedSender;
import com.xmail.XmailService.Models.QueuedMails;
import com.xmail.IO.FileUtils;
import com.xmail.SMTP.SMTP;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailWorker extends Thread {
    final static Logger logger = Logger.getRootLogger();

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
        QueuedMails mail;

        logger.info("Start sending email...");

        MailQueue queue = MailQueue.getInstance();
        queue.open();

        try {

            mail = queue.getEmail(mailId);

            to = mail.get("mail_to").toString();
            mail_path = mail.get("email_path").toString();

            data = FileUtils.getFileContents(mail_path);

            int retryCount = Integer.parseInt(mail.get("retry").toString());

            AdvancedSender sender = new AdvancedSender();

            // Set the port specified in config
            sender.port = XmailConfig.port;

            sender.ehlo = XmailConfig.ehlo;
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

            // And send!
            status = sender.send(to, data);

            if (status == SMTP.SUCCESS) {
                // okay
                queue.deleteEmail(mail);
                logger.info("Email was sent okay.");
            }
            else if(status == SMTP.MAILBOX_NOT_EXISTS || status == SMTP.PERMANENT_ERROR) {

                // send bounce
                XmailBounce bounce = new XmailBounce();
                bounce.sendBounce(mail.get("mail_from").toString(), mail.get("mail_to").toString(), mail_path,
                        sender.getLastCode(), sender.getLastMessage(), Integer.parseInt(mail.get("retry").toString()),
                        sender.getRemoteMta());

                // remove the file from disk
                queue.deleteEmail(mail);

                logger.info("Email could not be delivered. Not queued.");
            }
            else {
                // check the log and queue if necessarily
                if(queue.queueEmail(mail, sender.getLastCode(), sender.getLastMessage(), sender.getIpIndex(),
                        sender.getMxIndex(), sender.isIpv6Used(), sender.getIpv4(), sender.getIpv6())) {

                    logger.info("Email could not be delivered. Queued.");
                }
                else {
                    // send bounce
                    XmailBounce bounce = new XmailBounce();
                    bounce.sendBounce(mail.get("mail_from").toString(), mail.get("mail_to").toString(), mail_path,
                            sender.getLastCode(), sender.getLastMessage(), Integer.parseInt(mail.get("retry").toString()),
                            sender.getRemoteMta());

                    // remove the file from disk
                    queue.deleteEmail(mail);

                    logger.info("Email could not be delivered. Status code= " + Integer.toString(status) + ". Not queued.");
                }
            }

        } catch (IOException e) {
            logger.error("Can not open email file: " + mail_path);
        }
        catch (UnknownError e) {
            logger.error("Unknown error occurred during sending email: " + e.getMessage());
        }
        finally {

            queue.close();
        }

    }
}
