package com.xmail.XmailService;

import com.xmail.Threads.NotifyingThread;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailThread extends NotifyingThread {

    public void doRun() {
        String to, content, headers;

        to = "vlad@mailwhere.com";
        content = "Hi there. I will be very happy if you will receive this email.";
        headers = "From: cristian@ceakki.eu\r\n";

        XmailSend sender = new XmailSend();

        sender.port = 24;

        sender.ehlo = "tcp";
        sender.from = "critian@ceakki.eu";

        sender.bindingIP = "0.0.0.0";

        try {
            boolean status = sender.send(to, content, headers);

            if (status) {
                // okay
            } else {
                // check the log and queue if necessarily
            }
        }
        finally {
            // queue it?
        }

        System.out.println(sender.log);

    }
}
