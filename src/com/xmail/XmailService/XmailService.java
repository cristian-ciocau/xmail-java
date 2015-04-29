package com.xmail.XmailService;

import com.xmail.Threads.NotifyingThread;
import com.xmail.Threads.ThreadCompleteListener;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService implements ThreadCompleteListener {
    int runningSmtpThreads = 0;

    public void start() {
        NotifyingThread newThread = new XmailThread();
        newThread.addListener(this);
        newThread.start();

        runningSmtpThreads++;
    }

    public void notifyOfThreadComplete(Thread finishedThread) {
        runningSmtpThreads--;
        System.out.println("Thread finished");
    }
}
