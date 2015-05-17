package com.xmail.Threads;

import com.xmail.XmailService.XmailThread;

/**
 * Created by cristian on 4/29/15.
 */
public interface ThreadCompleteListener {

    /**
     * ThreadCompleteListener.notifyOfThreadComplete()
     *
     * This method will be called when a thread finished
     *
     * @param thread
     */
    void notifyOfThreadComplete(final Thread thread);
}
