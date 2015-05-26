package com.xmail.XmailService;

import com.xmail.IO.FileUtils;
import com.xmail.Mime.Mime;
import com.xmail.SMTP.Composer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by cristian on 5/15/15.
 */
public class XmailBounce {
    final static Logger logger = Logger.getRootLogger();

    /**
     * XmailBounce.sendBounce()
     *
     * Adds a bounce email to queue
     *
     * @param to
     * @param originalTo
     * @param originalMailPath
     * @param lastCode
     * @param lastMessage
     * @param deliveryAttempts
     * @param remoteMta
     * @return
     */
    public boolean sendBounce(String to, String originalTo, String originalMailPath, int lastCode, String lastMessage,
                              int deliveryAttempts, String remoteMta) {

        String from = XmailConfig.bounceFrom;
        remoteMta = "[" + remoteMta + "]";

        try {
            String emailContent = FileUtils.getFileContents(originalMailPath);

            String humanReport = Mime.composeHumanReport(originalTo, lastMessage);
            String deliveryStatus = Mime.composeDelivryStatus(originalTo, XmailConfig.ehlo, remoteMta, lastCode,
                    lastMessage + " (delivery attempts: " + Integer.toString(deliveryAttempts) + ")");

            Composer composer = new Composer();
            Map<String, String> mail = composer.composeBounce(to, humanReport, deliveryStatus, emailContent, from);

            File file = File.createTempFile("xmail", ".eml", new File("/Data/git/xmail-java/mail/"));
            String mailPath = file.getAbsolutePath();

            FileUtils.putFileContents(file, mail.get("headers") + "\r\n" + mail.get("content"));

            MailQueue queue = MailQueue.getInstance();
            if(!queue.init(XmailConfig.dbPath)) {
                logger.error("Could not create the database.");
                return false;
            }
            if(!queue.open()) {
                logger.error("Could not connect to database.");
                return false;
            }
            queue.addEmail(to, from, mailPath);
            queue.close();
        }
        catch (IOException e) {
            logger.error("Bounce preparing error: " + e.getMessage());
            return false;
        }

        return true;
    }
}
