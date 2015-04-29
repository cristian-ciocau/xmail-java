package com.xmail;

import java.util.Map;

public class Main {

    private static String mx = "mailwhere.com";
    private static String ehlo = "ceakki.eu";
    private static String mail = "root@ceakki.eu";
    private static String rcpt = "vlad@mailwhere.com";
    private static String subj = "Java SMTP Client";
    private static String mesg = "Hooray if you can read this it means the code works!";

    public static void main(String[] args) {

        Map<String, String> data;

        XmailComposer composer = new XmailComposer();

        data = composer.compose(rcpt, subj, mesg, "From: " + mail + "\r\n");

        System.out.println(data);
        System.exit(3);

        XmailSend send = new XmailSend();

        send.port = 24;

        send.ehlo = ehlo;
        send.from = mail;

        send.send(rcpt, data.get("content"), data.get("headers"));

        System.out.println(send.log);

    }
}
