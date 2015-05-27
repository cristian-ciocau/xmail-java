package com.xmail.main.XmailService;

import com.xmail.Config;
import com.xmail.main.XmailService.Models.QueuedMails;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by cristian on 4/29/15.
 */
public class XmailService {
    final static Logger logger = Logger.getLogger(XmailService.class);

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
        if(!queue.init(Config.dbPath)) {
            logger.error("Could not create the database.");
            return;
        }
        if(!queue.open()) {
            logger.error("Could not open the database");
            return;
        }
        queue.reset();

        // Initialize the outgoing IP addresses queue
        IpQueue ipQueue = IpQueue.getInstance();
        ipQueue.init(Config.outgoingIPv4, Config.outgoingIPv6);

        logger.info("XmailService started.");

        // Loop forever
        while (true) {

            if (shutdown) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Exiting infinite loop.");
                }

                break;
            }

            try {
                List<QueuedMails> mails = queue.getEmails(Config.maxSmtpThreads);
                for (final QueuedMails mail : mails) {

                    if (smtpThreadsList.size() < Config.maxSmtpThreads) {

                        if (!queue.changeEmailStatus(mail, 1)) {
                            logger.error("Can not mark email for processing (db write error).");
                            continue;
                        }

                        Thread newThread = new Thread() {
                            @Override
                            public void run() {
                                synchronized (smtpThreadsList) {
                                    smtpThreadsList.add(this);
                                }

                                try {
                                    XmailWorker worker = new XmailWorker();
                                    worker.process((Integer) mail.get("id"));
                                } finally {
                                    synchronized (smtpThreadsList) {
                                        smtpThreadsList.remove(this);
                                    }
                                }
                            }
                        };
                        newThread.setDaemon(true);
                        newThread.start();

                    } else {
                        logger.info("Maximum of concurrent running SMTP threads reached...");
                    }
                }
            }
            catch (Exception e) {
                logger.error(e.getClass() + ": " + e.getMessage());
            }


            // Please note, this try... catch statement should be separately
            // at the end of while loop
            try {
                // Sleep a while
                Thread.sleep(Config.loopTime * 1000);
            }
            catch(InterruptedException ex) {
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
