package com.company;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    private static String mx = "mailwhere.com";
    private static String ehlo = "ceakki.eu";
    private static String mail = "root@ceakki.eu";
    private static String rcpt = "vlad@mailwhere.com";
    private static String subj = "Java SMTP Client";
    private static String mesg = "Hooray if you can read this it means the code works!";

    private static String CRLF = "\r\n";

    public static void main(String[] args) {

        String data;

        data = "Subject: " + subj + CRLF;
        data += "To: " + rcpt + CRLF;
        data += "From: " + mail + CRLF;
        data += "Date: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()) + CRLF;
        data += CRLF;
        data += mesg;

        XmailSend send = new XmailSend();

        send.port = 24;

        send.ehlo = ehlo;
        send.from = mail;

        send.send(rcpt, data);

        System.out.println(send.log);

    }
}
