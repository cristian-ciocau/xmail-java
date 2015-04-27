package com.company;

public class Main {

    private static String mx = "mailwhere.com";
    private static String ehlo = "ceakki.net";
    private static String mail = "c@ceakki.net";
    private static String rcpt = "vlad@mailwhere.com";
    private static String subj = "Java SMTP Client";
    private static String mesg = "Hooray if you can read this it means the code works!";

    public static void main(String[] args) {

        XmailSend send = new XmailSend();

        send.mx = mx;
        send.ehlo = ehlo;

        send.send();

        System.out.println(send.log);

    }
}
