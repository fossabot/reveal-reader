/**
 * 
 */
package com.jackcholt.reveal;

import android.os.Process;

/**
 * Abstract base class for safe runnable objects that don't let exceptions get
 * back to the OS.
 * 
 * @author shon
 * 
 */
public abstract class SafeRunnable implements Runnable {

    /**
     * Constructs SafeRunnable
     */
    public SafeRunnable() {
    }

    // @Override
    final public void run() {
        // keep it smooth
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        try {
            protectedRun();
        } catch (final Throwable t) {
            String message = "Uncaught exception: " + Util.getStackTrace(t);
            Log.e(getClass().getSimpleName(), message);
            Util.unexpectedError(Main.getMainApplication(), t);
        }
    }

    /**
     * Implemented by subclasses to provide the runnable functionality.
     */
    public abstract void protectedRun();

}
