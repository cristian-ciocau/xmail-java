package com.xmail.XmailService;

import com.xmail.XmailService.Models.QueuedMails;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService {
    final static Logger logger = Logger.getRootLogger();

    final ConcurrentLinkedDeque<Thread> smtpThreadsList = new ConcurrentLinkedDeque<Thread>();

    boolean shutdown = false;

    /**
     * XmailService.start()
     *
     * Starts the email processing
     */
    public void start() {

        // Initialize the Shutdown Hook
        initShutdownHook();

        // Initialize the Mail Queue
        MailQueue queue = MailQueue.getInstance();
        queue.init(XmailConfig.dbPath);
        queue.open();

        // Initialize the outgoing IP addresses queue
        IpQueue ipQueue = IpQueue.getInstance();
        ipQueue.init(XmailConfig.outgoingIPv4, XmailConfig.outgoingIPv6);

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
            for (final QueuedMails mail : mails) {

                if (smtpThreadsList.size() < XmailConfig.maxSmtpThreads) {

                    mail.set("status", 1).saveIt();

                    Thread newThread = new Thread() {
                        @Override
                        public void run() {
                            synchronized (smtpThreadsList) {
                                smtpThreadsList.add(this);
                            }

                            try {
                                XmailWorker worker = new XmailWorker();
                                worker.process((Integer) mail.get("id"));
                            }
                            finally {
                                synchronized (smtpThreadsList) {
                                    smtpThreadsList.remove(this);
                                }
                            }
                        }
                    };
                    newThread.setDaemon(true);
                    newThread.start();

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

        queue.close();
    }

    /**
     * XmailService.initShutdownHook()
     *
     * Sets a shutdown hook handler which will attempt to grateful exit
     */
    private void initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                shutdown = true;

                if (logger.isDebugEnabled()) {
                    logger.debug("Catch exit signal.");
                }

                for (Thread thread : smtpThreadsList) {

                    try {
                        if (thread.isAlive()) {
                            thread.join();
                        }
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                    }

                }
            }
        }));
    }
}
