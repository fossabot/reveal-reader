package com.jackcholt.reveal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

/**
 * Service that initiates and coordinates all the background activities of downloading books and updating the library so
 * the don't step on each other's feet.
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
    public static final String HISTORY_KEY = "history";

    public static final int ADD_BOOK = 1;
    public static final int REMOVE_BOOK = 2;
    public static final int DOWNLOAD_BOOK = 3;
    public static final int ADD_HISTORY = 4;
    public static final int REMOVE_HISTORY = 5;
    public static final int UPDATE_HISTORY = 6;

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
        try {
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
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        try {
            Bundle extras = intent.getExtras();
            int action = extras.getInt(ACTION_KEY);
            final String target = extras.getString(TARGET_KEY);
            final String source = extras.getString(SOURCE_KEY);
            final History hist = (History) extras.getSerializable(HISTORY_KEY);
            final long callbacksID = Long.valueOf(extras.getLong(CALLBACKS_KEY));

            switch (action) {
            case ADD_BOOK:
                Log.i(TAG, "Received request to add book: " + target);
                if (target != null) {
                    Runnable job = new SafeRunnable() {

                        @Override
                        public void protectedRun() {
                            String bookName = new File(target).getName().replaceFirst("\\.[^\\.]$", "");
                            String bookFriendlyName = Util.lookupBookName(YbkService.this, bookName);
                            if (bookFriendlyName != null) {
                                bookName = bookFriendlyName;
                            }
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
                            } catch (InvalidFileFormatException ioe) {
                                succeeded = false;
                                message = "Could not add '" + bookName + "'.";
                                Util.displayError(Main.getMainApplication(), null, getResources().getString(R.string.error_damaged_ebook), bookName);
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not add '" + bookName + "'.";
                                Util.unexpectedError(Main.getMainApplication(), ioe, "book: " + target);
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
                                    message = "Could not remove book '" + bookName + "'.";
                                    succeeded = true;
                                }
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not remove book '" + bookName + "'.: " + ioe.toString();
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
                    final String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                            Settings.DEFAULT_EBOOK_DIRECTORY);
                    final Context context = this;
                    Runnable job = new SafeRunnable() {
                        @Override
                        public void protectedRun() {
                            try {
                                List<String> downloads = Util.fetchTitle(new URL(target), new URL(source), libDir,
                                        context);
                                for (String download : downloads) {
                                    requestAddBook(context, download, callbackMap.get(Long.valueOf(callbacksID)));
                                }
                            } catch (IOException ioe) {
                                Log.e(TAG, "Unable to download '" + source + "': " + ioe.toString());
                                Util.sendNotification(YbkService.this, "Could not download '" + source + "'. "
                                        + ioe.toString(), android.R.drawable.stat_sys_warning, "Reveal Library",
                                        mNotifMgr, mNotifId++, Main.class);
                            }
                        }
                    };
                    mDownloadHandler.post(job);
                } else {
                    Log.e(TAG, "Download book request missing target or filename.");
                }
                break;
            case ADD_HISTORY:
                Log.d(TAG, "Received request to add history: " + hist);
                if (hist != null) {
                    Runnable job = new SafeRunnable() {

                        @Override
                        public void protectedRun() {
                            boolean succeeded;
                            String message;
                            try {
                                YbkDAO ybkDAO = YbkDAO.getInstance(YbkService.this);
                                if (ybkDAO.insertHistory(hist)) {
                                    succeeded = true;
                                    message = "Added history: '" + hist + "'";
                                } else {
                                    message = "Could not add history: '" + hist + "'";
                                    succeeded = true;
                                }
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not add history: '" + hist + "'";
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
                    Log.e(TAG, "Add history request missing target.");
                }
                break;
            case UPDATE_HISTORY:
                Log.d(TAG, "Received request to update history: " + hist + "'");
                if (hist != null) {
                    Runnable job = new SafeRunnable() {

                        @Override
                        public void protectedRun() {
                            boolean succeeded;
                            String message;
                            try {
                                YbkDAO ybkDAO = YbkDAO.getInstance(YbkService.this);
                                if (ybkDAO.updateHistory(hist)) {
                                    succeeded = true;
                                    message = "Updated history: '" + hist + "'";
                                } else {
                                    message = "Could not update history: '" + hist + "'";
                                    succeeded = true;
                                }
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not update history: '" + hist + "'";
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
                    Log.e(TAG, "Add history request missing target.");
                }
                break;
            case REMOVE_HISTORY:
                Log.d(TAG, "Received request to remove history: " + hist + "'");
                if (hist != null) {
                    Runnable job = new SafeRunnable() {
                        @Override
                        public void protectedRun() {
                            boolean succeeded;
                            String message;
                            try {
                                YbkDAO ybkDao = YbkDAO.getInstance(YbkService.this);
                                if (hist.bookmarkNumber > 0) {
                                    ybkDao.deleteBookmark(hist);
                                } else {
                                    ybkDao.deleteHistory(hist);
                                }
                                succeeded = true;
                                message = "Removed history: '" + hist + "'";
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not remove history: '" + hist + "'";
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
                    Log.e(TAG, "Remove history request missing target.");
                }
                break;
            default:
                Log.w(TAG, "Received request to perform unrecognized action: " + action);
                return;
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            mLibLooper.quit();
            mDownloadLooper.quit();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

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
     * Requests that a history be added to the library.
     * 
     * @param context
     *            the caller's context
     * @param target
     *            the history
     * @param callbacks
     *            (optional) completion callback
     */
    public static void requestAddHistory(Context context, History hist, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, ADD_HISTORY).putExtra(HISTORY_KEY,
                hist);
        if (callbacks != null && callbacks.length != 0) {
            Long callbackID = Long.valueOf(Util.getUniqueTimeStamp());
            callbackMap.put(callbackID, callbacks);
            intent.putExtra(CALLBACKS_KEY, callbackID);
        }
        context.startService(intent);
    }

    public static void requestUpdateHistory(Context context, History hist, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, UPDATE_HISTORY).putExtra(HISTORY_KEY,
                hist);
        if (callbacks != null && callbacks.length != 0) {
            Long callbackID = Long.valueOf(Util.getUniqueTimeStamp());
            callbackMap.put(callbackID, callbacks);
            intent.putExtra(CALLBACKS_KEY, callbackID);
        }
        context.startService(intent);
    }

    /**
     * Requests that a history be removed the library.
     * 
     * @param context
     *            the caller's context
     * @param target
     *            the history
     * @param callbacks
     *            (optional) completion callback
     */
    public static void requestRemoveHistory(Context context, History hist, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, REMOVE_HISTORY).putExtra(
                HISTORY_KEY, hist);
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

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, YbkService.class));
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
