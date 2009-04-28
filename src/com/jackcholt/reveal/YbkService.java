package com.jackcholt.reveal;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.jackcholt.reveal.data.StorageException;
import com.jackcholt.reveal.data.YbkDAO;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;

/**
 * Service that initiates and coordinates all the background activities of
 * downloading books and updating the library so the don't step on each other's
 * feet.
 * 
 * @author Shon Vella
 * 
 */
public class YbkService extends Service {

    private static final String TAG = "YbkService";
    public static final String ACTION_KEY = "action";
    public static final String TARGET_KEY = "target";
    public static final String SOURCE_KEY = "source";
    public static final String CALLBACKS_KEY = "callbacks";

    public static final int ADD_BOOK = 1;
    public static final int REMOVE_BOOK = 2;
    public static final int DOWNLOAD_BOOK = 3;

    private volatile Looper mLibLooper;
    private volatile Handler mLibHandler;
    private volatile Looper mDownloadLooper;
    private volatile Handler mDownloadHandler;
    private NotificationManager mNotifMgr;
    private volatile int mNotifId = Integer.MIN_VALUE;

    private SharedPreferences mSharedPref;

    // kludge to get around the fact that we can't pass callbacks through the
    // service simply even though
    // the service is only for the local process.

    private static Map<Long, Completion[]> callbackMap = new TreeMap<Long, Completion[]>();

    @Override
    public void onCreate() {
        mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread libThread = new HandlerThread("YbkUpdateWorker");
        libThread.start();
        libThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mLibLooper = libThread.getLooper();
        mLibHandler = new Handler(mLibLooper);

        HandlerThread downloadThread = new HandlerThread("YbkDownloadWorker");
        downloadThread.start();
        libThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mDownloadLooper = downloadThread.getLooper();
        mDownloadHandler = new Handler(mDownloadLooper);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Bundle extras = intent.getExtras();
        int action = extras.getInt(ACTION_KEY);
        final String target = extras.getString(TARGET_KEY);
        final String source = extras.getString(SOURCE_KEY);
        final long callbacksID = Long.valueOf(extras.getLong(CALLBACKS_KEY));

        switch (action) {
        case ADD_BOOK:
            Log.i(TAG, "Received request to add book: " + target);
            if (target != null) {
                Runnable job = new SafeRunnable() {

                    @Override
                    public void protectedRun() {
                        String bookName = new File(target).getName().replaceFirst("\\.[^\\.]$", "");
                        boolean succeeded;
                        String message;
                        try {
                            // Create an object for reading a ybk file;
                            YbkFileReader ybkRdr = new YbkFileReader(YbkService.this, target);
                            // Tell the YbkFileReader to populate the book info
                            // into
                            // the database;
                            if (ybkRdr.populateBook() != 0) {
                                succeeded = true;
                                message = "Added '" + bookName + "' to the library";
                            } else {
                                succeeded = false;
                                message = "Could not add '" + bookName;
                            }
                        } catch (IOException ioe) {
                            succeeded = false;
                            message = "Could not add '" + bookName + "'.: " + ioe.toString();
                            ReportError.reportError("BAD_EBOOK_FILE_" + bookName);

                        }
                        if (succeeded)
                            Log.i(TAG, message);
                        else
                            Log.e(TAG, message);
                        if (callbacksID != 0) {
                            for (Completion callback : callbackMap.remove(Long.valueOf(callbacksID))) {
                                callback.completed(succeeded, message);
                            }
                        }
                    }
                };
                mLibHandler.post(job);
            } else {
                Log.e(TAG, "Add book request missing target.");
            }
            break;
        case REMOVE_BOOK:
            Log.i(TAG, "Received request to remove book: " + target);
            if (target != null) {
                Runnable job = new SafeRunnable() {
                    @Override
                    public void protectedRun() {
                        String bookName = new File(target).getName().replaceFirst("\\.[^\\.]$", "");
                        boolean succeeded;
                        String message;
                        try {
                            YbkDAO ybkDAO = YbkDAO.getInstance(YbkService.this);
                            if (ybkDAO.deleteBook(target)) {
                                succeeded = true;
                                message = "Removed '" + bookName + "' from the library";
                            } else {
                                message = "Failed to remove book '" + bookName + "'.";
                                succeeded = true;
                            }
                        } catch (StorageException se) {
                            succeeded = false;
                            message = "Failed to remove book '" + bookName + "'.: " + se.toString();
                            ReportError.reportError("BAD_EBOOK_FILE_" + target);
                        }
                        if (succeeded)
                            Log.i(TAG, message);
                        else
                            Log.e(TAG, message);
                        if (callbacksID != 0) {
                            for (Completion callback : callbackMap.remove(Long.valueOf(callbacksID))) {
                                callback.completed(succeeded, message);
                            }
                        }
                    }
                };
                mLibHandler.post(job);
            } else {
                Log.e(TAG, "Remove book request missing target.");
            }
            break;
        case DOWNLOAD_BOOK:
            Log.i(TAG, "Received request to download book: " + source + " to: " + target);

            if (target != null && source != null) {
                // FIXME - these strings should come from shared constants
                // somewhere
                final String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                        Settings.DEFAULT_EBOOK_DIRECTORY);
                final Context context = this;
                Runnable job = new SafeRunnable() {
                    @Override
                    public void protectedRun() {
                        try {
                            List<String> downloads = Util.fetchTitle(new URL(target), new URL(source), libDir, context);
                            for (String download : downloads) {
                                requestAddBook(context, download, callbackMap.get(Long.valueOf(callbacksID)));
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Unable to download '" + source + "': " + ioe.toString());
                            Util.sendNotification(YbkService.this, "Could not download '" + source + "'. "
                                    + ioe.toString(), android.R.drawable.stat_sys_warning, "Reveal Library", mNotifMgr,
                                    mNotifId++, Main.class);
                        }
                    }
                };
                mDownloadHandler.post(job);
            } else {
                Log.e(TAG, "Download book request missing target or filename.");
            }
            break;
        default:
            Log.w(TAG, "Received request to perform unrecognized action: " + action);
            return;
        }
    }

    @Override
    public void onDestroy() {
        mLibLooper.quit();
        mDownloadLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Requests that a book be added to the library.
     * 
     * @param context
     *            the caller's context
     * @param target
     *            the absolute filename of the book to be added
     * @param callbacks
     *            (optional) completion callback
     */
    public static void requestAddBook(Context context, String target, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, ADD_BOOK).putExtra(TARGET_KEY,
                target);
        if (callbacks != null && callbacks.length != 0) {
            Long callbackID = Long.valueOf(Util.getUniqueTimeStamp());
            callbackMap.put(callbackID, callbacks);
            intent.putExtra(CALLBACKS_KEY, callbackID);
        }
        context.startService(intent);
    }

    /**
     * Requests that a book be removed the library.
     * 
     * @param context
     *            the caller's context
     * @param target
     *            the absolute filename of the book to be removed
     * @param callbacks
     *            (optional) completion callback
     */
    public static void requestRemoveBook(Context context, String target, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, REMOVE_BOOK).putExtra(TARGET_KEY,
                target);
        if (callbacks != null && callbacks.length != 0) {
            Long callbackID = Long.valueOf(Util.getUniqueTimeStamp());
            callbackMap.put(callbackID, callbacks);
            intent.putExtra(CALLBACKS_KEY, callbackID);
        }
        context.startService(intent);
    }

    /**
     * Requests that a book be downloaded and added to the library.
     * 
     * @param context
     *            the caller's context
     * @param source
     *            URL of the book to be downloaded
     * @param target
     *            the suggested absolute filename of the book to be added
     * @param callbacks
     *            (optional) completion callback
     */
    public static void requestDownloadBook(Context context, String source, String target, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, DOWNLOAD_BOOK).putExtra(SOURCE_KEY,
                source).putExtra(TARGET_KEY, target);
        if (callbacks != null && callbacks.length != 0) {
            Long callbackID = Long.valueOf(Util.getUniqueTimeStamp());
            callbackMap.put(callbackID, callbacks);
            intent.putExtra(CALLBACKS_KEY, callbackID);
        }
        context.startService(intent);
    }

    /**
     * Completion callback interface.
     * 
     * @author shon
     * 
     */
    public static interface Completion {
        /**
         * Called when the request has been completed.
         * 
         * @param succeeded
         *            true if the request succeeded, false if it failed.
         * @param message
         *            message associated with completion (may be null)
         */
        void completed(boolean succeeded, String message);
    }

}
