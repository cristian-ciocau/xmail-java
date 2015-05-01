package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import com.xmail.Threads.NotifyingThread;
import com.xmail.Threads.ThreadCompleteListener;

import java.util.List;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService implements ThreadCompleteListener {
    int runningSmtpThreads = 0;

    public void start() {

        XmailQueue queue = new XmailQueue();

        queue.init(XmailConfig.dbPath);

        List<QueuedMails> mails = queue.getEmails(XmailConfig.maxSmtpThreads);

        for(QueuedMails mail: mails) {

            if(runningSmtpThreads < XmailConfig.maxSmtpThreads) {

                mail.set("status", 1).saveIt();

                XmailThread newThread = new XmailThread();
                newThread.addListener(this);
                newThread.addMailId((Integer) mail.get("id"));
                //newThread.setDaemon(true);
                newThread.start();

                runningSmtpThreads++;

            }
        }
    }

    public void notifyOfThreadComplete(Thread finishedThread) {
        runningSmtpThreads--;
        System.out.println("Thread finished");
    }
}
