package com.xmail.Threads;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by cristian on 4/29/15.
 */
public abstract class NotifyingThread extends Thread {

    // The list of current listeners
    private final Set<ThreadCompleteListener> listeners
            = new CopyOnWriteArraySet<ThreadCompleteListener>();

    /**
     * NotifyingThread.addListener()
     *
     * Adds a listener
     *
     * @param listener
     */
    public final void addListener(final ThreadCompleteListener listener) {
        listeners.add(listener);
    }

    /**
     * NotifyingThread.removeListener()
     *
     * Removes a listener
     *
     * @param listener
     */
    public final void removeListener(final ThreadCompleteListener listener) {
        listeners.remove(listener);
    }

    /**
     * NotifyingThread.notifyListeners()
     *
     * Notify the listeners that this thread finished
     *
     */
    private final void notifyListeners() {
        for (ThreadCompleteListener listener : listeners) {
            listener.notifyOfThreadComplete(this);
        }
    }

    /**
     * NotifyingThread.run()
     *
     * Runs our custom method and notify the listeners after the job will be finished
     *
     */
    @Override
    public final void run() {
        try {
            doRun();
        } finally {
            notifyListeners();
        }
    }

    /**
     * NotifyingThread.doRun()
     *
     * All runnable code for this type of thread will be added in this method
     *
     */
    public abstract void doRun();
}
