/**
 * 
 */
package com.jackcholt.reveal;

/**
 * Abstract base class for safe runnable objects that don't let exceptions get back to the OS.
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

    @Override
    final public void run() {
        try {
           protectedRun(); 
        }
        catch (Throwable t)
        {
            String message = "Uncaught exception: " + Util.getStackTrace(t);
            Log.e(getClass().getSimpleName(), message);
            ReportError.reportError("UNCAUGHT_EXCEPTION_" + message);
        }
    }
    
    /**
     * Implemented by subclasses to provide the runnable functionality.
     */
    public abstract void protectedRun();
    

}
