package com.xmail.main.XmailService;

import com.xmail.Config;
import com.xmail.main.IO.FileUtils;
import com.xmail.main.Mime.Mime;
import com.xmail.main.SMTP.Composer;
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

        String from = Config.bounceFrom;
        remoteMta = "[" + remoteMta + "]";

        try {

            // Get the original mail content
            String emailContent = FileUtils.getFileContents(originalMailPath);

            // Prepare the bounce attachments
            String humanReport = Mime.composeHumanReport(originalTo, lastMessage);
            String deliveryStatus = Mime.composeDelivryStatus(originalTo, Config.ehlo, remoteMta, lastCode,
                    lastMessage + " (delivery attempts: " + Integer.toString(deliveryAttempts) + ")");

            // Compose the bounce
            Composer composer = new Composer();
            Map<String, String> mail = composer.composeBounce(to, humanReport, deliveryStatus, emailContent, from);

            // Save email to disk
            File file = File.createTempFile("xmail", ".eml", new File(Config.mailPath));
            String mailPath = file.getAbsolutePath();
            FileUtils.putFileContents(file, mail.get("headers") + "\r\n" + mail.get("content"));

            // Add email to queue
            MailQueue queue = MailQueue.getInstance();
            queue.addEmail(to, from, mailPath);
        }
        catch (IOException e) {
            logger.error("Bounce preparing error: " + e.getMessage());
            return false;
        }

        return true;
    }
}
