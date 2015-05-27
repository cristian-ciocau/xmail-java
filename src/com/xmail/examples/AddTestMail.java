package com.xmail.examples;

import com.xmail.main.MailClient.MailClient;


/**
 * Created by cristian on 5/13/15.
 */
class AddTestMail {

    public static void main(String[] args) {

        String to = "to@example.com";
        String from = "from@example.com";
        String headers = "From: <" + from + ">";

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

        // And send the email
        try {
            MailClient mailer = new MailClient();
            mailer.mail(to, from, subject, message, headers, attachments);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        System.out.println("An email for <" + to + "> was prepared for delivery.");

    }
}
