package com.xmail.examples;

import com.xmail.main.IO.FileUtils;
import com.xmail.main.SMTP.Composer;
import com.xmail.main.XmailService.MailQueue;
import com.xmail.Config;

import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * Created by cristian on 5/13/15.
 */
class AddTestMail {

    public static void main(String[] args) {

        String from = Config.testMailFrom;

        String to = Config.testMailTo;
        String headers = "From: <" + Config.testMailFromHeader + ">";

        String subject = "Lorem ipsum";
        String message = "<h1>Lorem ipsum for com.xmail</h1>\n\n" +
                "<p>Lorem ipsum dolor sit amet,</p> \n" +
                "<p>Consectetur adipiscing elit. Vestibulum at metus sed velit porttitor mattis. Nunc a augue diam.</p> \n" +
                "<p>Fusce dictum ante id tortor sollicitudin, ultrices porttitor quam scelerisque. Ut lobortis metus et leo tincidunt laoreet.</p> \n" +
                "<p>Pellentesque fermentum, eros nec aliquam aliquet, magna sem porttitor enim, et posuere lorem nibh quis enim. </p>\n" +
                "<p>Sed sit amet tortor rutrum, egestas nibh non, egestas nisi. Pellentesque mollis magna id tempus mollis. </p>\n" +
                "<p>Sed accumsan vitae nisi sagittis fermentum. Ut egestas nec dolor vitae molestie. Nam in ligula nec neque fermentum imperdiet elementum nec tortor. </p>\n" +
                "<p>Quisque blandit tellus mollis mi placerat vehicula. Morbi vitae pellentesque turpis, eget imperdiet purus. </p>\n" +
                "<p>&nbsp;</p>\n" +
                "<p>Cras id ullamcorper metus.</p>";

        String[] attachments = new String[] {System.getProperty("user.dir") + "/src/com/xmail/test/resources/lorem_ipsum.gif"};

        try {
            Composer composer = new Composer();
            Map<String, String> mail = composer.compose(to, subject, message, headers, attachments);

            File file = File.createTempFile("xmail", ".eml", new File(Config.mailPath));
            String mailPath = file.getAbsolutePath();

            FileUtils.putFileContents(file, mail.get("headers") + "\r\n" + mail.get("content"));

            MailQueue queue = MailQueue.getInstance();
            if(!queue.init(Config.dbPath)) {
                System.out.println("Could not create the database.");
                System.exit(-1);
            }
            if(!queue.open()) {
                System.out.println("Could not connect to database.");
                System.exit(-1);
            }
            queue.addEmail(to, from, mailPath);
            queue.close();
        }
        catch (IOException e) {
            System.out.println("Can not write .eml file: " + e.getMessage() +
                    ". Please check if you have write access to the mail folder.");
            System.exit(-2);
        }
        catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
            System.exit(-3);
        }

        System.out.println("An email for <" + to + "> was prepared for delivery.");

    }
}
