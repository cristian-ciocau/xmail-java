package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import com.xmail.Threads.NotifyingThread;
import com.xmail.Threads.ThreadCompleteListener;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService implements ThreadCompleteListener {
    int runningSmtpThreads = 0;

    List<XmailThread> smtpThreadsList = new ArrayList<XmailThread>();

    boolean shutdown = false;

    public void start() {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                shutdown = true;

                for (XmailThread thread : smtpThreadsList) {
                    try {
                        if (thread.isAlive()) {
                            thread.join();
                        }
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            }
        }));


        XmailQueue queue = new XmailQueue();

        queue.init(XmailConfig.dbPath);

        // Loop forever
        while (true) {

            System.out.println(shutdown);
            if(shutdown) break;

            List<QueuedMails> mails = queue.getEmails(XmailConfig.maxSmtpThreads);

            for (QueuedMails mail : mails) {

                if (runningSmtpThreads < XmailConfig.maxSmtpThreads) {

                    mail.set("status", 1).saveIt();

                    XmailThread newThread = new XmailThread();
                    newThread.addListener(this);
                    newThread.addMailId((Integer) mail.get("id"));
                    newThread.setDaemon(true);
                    smtpThreadsList.add(newThread);
                    newThread.start();

                    runningSmtpThreads++;

                }
            }

            try {
                Thread.sleep(XmailConfig.loopTime * 1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void notifyOfThreadComplete(Thread finishedThread) {
        runningSmtpThreads--;
        System.out.println("Thread finished");
    }
}
