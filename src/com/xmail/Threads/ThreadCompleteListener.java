package com.xmail.Threads;

import com.xmail.XmailService.XmailThread;

/**
 * Created by cristian on 4/29/15.
 */
public interface ThreadCompleteListener {
    void notifyOfThreadComplete(final Thread thread);
}
