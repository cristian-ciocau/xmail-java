package com.xmail.XmailService;

import com.xmail.Database.QueuedMails;
import com.xmail.Threads.ThreadCompleteListener;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService implements ThreadCompleteListener {
    final static Logger logger = Logger.getRootLogger();

    int runningSmtpThreads = 0;

    List<XmailThread> smtpThreadsList = new ArrayList<XmailThread>();

    boolean shutdown = false;

    public void start() {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                shutdown = true;

                if(logger.isDebugEnabled()) {
                    logger.debug("Catch exit signal.");
                }

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

        logger.info("XmailService started.");

        // Loop forever
        while (true) {

            if (shutdown) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Exiting infinite loop.");
                }

                break;
            }

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

                    logger.info(newThread.getName() + " sending email to " + mail.get("mail_to").toString() + "...");

                    runningSmtpThreads++;

                }
                else {
                    logger.info("Maximum of concurrent running SMTP threads reached...");
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
        logger.info(finishedThread.getName() + " ended.");
    }
}
