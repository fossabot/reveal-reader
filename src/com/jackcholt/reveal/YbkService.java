package com.jackcholt.reveal;

import java.io.File;
import java.io.FileNotFoundException;
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

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.Book;
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
    public static final String CHARSET_KEY = "charset";

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
    @SuppressWarnings("unused")
    private NotificationManager mNotifMgr;
    @SuppressWarnings("unused")
    private volatile int mNotifId = Integer.MIN_VALUE;

    private SharedPreferences mSharedPref;

    // kludge to get around the fact that we can't pass callbacks through the
    // service simply even though
    // the service is only for the local process.

    private static Map<Long, Completion[]> callbackMap = new TreeMap<Long, Completion[]>();

    @Override
    public void onCreate() {
        try {
            Util.startFlurrySession(this);
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
            final String charset = extras.getString(CHARSET_KEY);
            final long callbacksID = Long.valueOf(extras.getLong(CALLBACKS_KEY));

            switch (action) {
            case ADD_BOOK:
                Log.i(TAG, "Received request to add book: " + target);
                if (target != null) {
                    Runnable job = new SafeRunnable() {

                        @Override
                        public void protectedRun() {
                            String bookName = target.replaceFirst("\\.[^\\.]$", "");
                            boolean succeeded;
                            String message;
                            YbkFileReader ybkRdr = null;
                            try {
                                // clean up any previous instance of the book
                                YbkFileReader.closeReader(target);
                                YbkDAO ybkDao = YbkDAO.getInstance(YbkService.this);
                                ybkDao.deleteBook(target);

                                String useCharset = charset;

                                // kludge for the known Cyrillic books
                                if (useCharset == null) {
                                    if (target.equalsIgnoreCase("km.ybk") || target.equalsIgnoreCase("vz.ybk")
                                            || target.equalsIgnoreCase("nz.ybk")) {
                                        useCharset = "CP1251";
                                    } else {
                                        useCharset = YbkFileReader.DEFAULT_YBK_CHARSET;
                                    }
                                }

                                // Add the book.
                                ybkRdr = YbkFileReader.addBook(YbkService.this, target, useCharset);
                                Book book = ybkRdr.getBook();
                                bookName = book.title == null ? bookName : book.title;
                                message = "Added '" + bookName + "' to the library";
                                succeeded = true;
                            } catch (InvalidFileFormatException ioe) {
                                succeeded = false;
                                message = "Could not add '" + Util.lookupBookName(YbkService.this, bookName) + "'.";
                                Util.displayError(Main.getMainApplication(), null, getResources().getString(
                                        R.string.error_damaged_ebook), bookName);
                            } catch (IOException ioe) {
                                succeeded = false;
                                message = "Could not add '" + Util.lookupBookName(YbkService.this, bookName) + "'.";
                                Util.unexpectedError(Main.getMainApplication(), ioe, "book: " + target);
                            } finally {
                                if (ybkRdr != null) {
                                    ybkRdr.unuse();
                                    ybkRdr = null;
                                }
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
                            YbkDAO ybkDAO = YbkDAO.getInstance(YbkService.this);
                            if (ybkDAO.deleteBook(target)) {
                                succeeded = true;
                                message = "Removed '" + bookName + "' from the library";
                            } else {
                                message = "Could not remove book '" + bookName + "'.";
                                succeeded = true;
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
                                Completion callbacks[] = callbackMap.get(Long.valueOf(callbacksID));
                                List<String> downloads = Util.fetchTitle(new File(target), new URL(source), libDir,
                                        context, callbacks);
                                if (downloads.isEmpty()) {
                                    throw new FileNotFoundException();
                                }
                                for (String download : downloads) {
                                    requestAddBook(context, download, null, callbacks);
                                }
                            } catch (IOException ioe) {
                                String targetFileName = new File(target).getName(); 
                                Log.e(TAG, "Unable to download '" + source + "': " + ioe.toString());
                                for (Completion callback : callbackMap.remove(Long.valueOf(callbacksID))) {
                                    callback.completed(false, "Could not download '" + targetFileName + "'. " + ioe.toString());
                                }
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
            FlurryAgent.onEndSession(this);
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
    public static void requestAddBook(Context context, String target, String charset, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, ADD_BOOK).putExtra(TARGET_KEY,
                target).putExtra(CHARSET_KEY, charset);
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
