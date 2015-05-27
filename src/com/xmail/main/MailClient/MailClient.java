package com.xmail.main.MailClient;

import com.xmail.main.Config;
import com.xmail.main.IO.FileUtils;
import com.xmail.main.SMTP.Composer;
import com.xmail.main.XmailService.MailQueue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by cristian on 5/27/15.
 */
public class MailClient {

    /**
     * MailClient.mail()
     *
     * A basic sending mail method which can be used in every project which uses Xmail Java
     *
     * @param to
     * @param from
     * @param subject
     * @param message
     * @param headers
     * @param attachments
     * @throws Exception
     */
    public void mail(String to, String from, String subject, String message, String headers, String[] attachments) throws Exception {
        try {
            Composer composer = new Composer();
            Map<String, String> mail = composer.compose(to, subject, message, headers, attachments);

            File file = File.createTempFile("xmail", ".eml", new File(Config.mailPath));
            String mailPath = file.getAbsolutePath();

            FileUtils.putFileContents(file, mail.get("headers") + "\r\n" + mail.get("content"));

            MailQueue queue = MailQueue.getInstance();
            if(!queue.init(Config.dbPath)) {
                throw new Exception("Could not create the database.");
            }
            if(!queue.open()) {
                throw new Exception("Could not connect to database.");
            }
            queue.addEmail(to, from, mailPath);
            queue.close();
        }
        catch (IOException e) {
            throw new Exception("Can not write .eml file: " + e.getMessage() +
                    ". Please check if you have write access to the mail folder.");
        }
        catch (Exception e) {
            throw new Exception(e.getClass() + ": " + e.getMessage());
        }
    }
}
