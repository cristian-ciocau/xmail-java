package com.tests;

import com.xmail.IO.FileUtils;
import com.xmail.SMTP.Composer;
import com.xmail.XmailService.MailQueue;
import com.xmail.XmailService.XmailConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * Created by cristian on 5/13/15.
 */
class ComposerTest {

    public void run() {

        String from = "from@example.com";

        String to = "to@example.com";
        String subject = "Cool Test message";
        String message = "<p>Hi there!!</p><p>How are you?</p><p><br/></p><p>I hope to receive well this message</p>";
        String headers = "From: <cristian@mailwhere.com>";
        String[] attachments = new String[] {};

        try {
            Composer composer = new Composer();
            Map<String, String> mail = composer.compose(to, subject, message, headers, attachments);

            File file = File.createTempFile("xmail", ".eml", new File(XmailConfig.mailPath));
            String mailPath = file.getAbsolutePath();

            FileUtils.putFileContents(file, mail.get("headers") + "\r\n" + mail.get("content"));

            MailQueue queue = MailQueue.getInstance();
            queue.init(XmailConfig.dbPath);
            if(!queue.open()) {
                System.out.println("Could not connect to database.");
                return;
            }
            queue.addEmail(to, from, mailPath);
            queue.close();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
