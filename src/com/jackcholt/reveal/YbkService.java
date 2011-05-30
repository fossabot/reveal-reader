package com.jackcholt.reveal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;

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

        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        try {
            switch (intent.getIntExtra(ACTION_KEY, 0)) {
            case ADD_BOOK:
                Log.i(TAG, "Received request to add book: " + intent.getExtras().getString(TARGET_KEY));
                if (null == intent.getExtras().getString(TARGET_KEY)) {
                    Log.e(TAG, "Add book request missing target.");
                    break;
                }

                mLibHandler.post(new AddBookJob(intent));

                break;
            case REMOVE_BOOK:
                Log.i(TAG, "Received request to remove book: " + intent.getExtras().getString(TARGET_KEY));
                if (intent.getExtras().getString(TARGET_KEY) != null) {
                    Runnable removeJob = new SafeRunnable() {
                        @Override
                        public void protectedRun() {
                            String bookName = new File(intent.getExtras().getString(TARGET_KEY)).getName()
                                    .replaceFirst("\\.[^\\.]$", "");
                            boolean succeeded;
                            String message;
                            YbkDAO ybkDAO = YbkDAO.getInstance(YbkService.this);
                            if (ybkDAO.deleteBook(intent.getExtras().getString(TARGET_KEY))) {
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
                            if ((long) Long.valueOf(intent.getExtras().getLong(CALLBACKS_KEY)) != 0) {
                                for (Completion callback : callbackMap.remove(Long.valueOf((long) Long.valueOf(intent
                                        .getExtras().getLong(CALLBACKS_KEY))))) {
                                    callback.completed(succeeded, message);
                                }
                            }
                        }
                    };
                    mLibHandler.post(removeJob);
                } else {
                    Log.e(TAG, "Remove book request missing target.");
                }
                break;
            case DOWNLOAD_BOOK:
                downloadBook(intent);
                break;
            default:
                Log.w(TAG, "Received request to perform unrecognized action: " + intent.getExtras().getInt(ACTION_KEY));
                return;
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    private void downloadBook(final Intent intent) {

        if (null == intent) {
            throw new IllegalArgumentException("Intent for downloading a book is null.");
        }

        if (null == intent.getStringExtra(TARGET_KEY) || null == intent.getStringExtra(SOURCE_KEY)) {
            Log.e(TAG, "Download book request missing target or filename.");
            return;
        }

        Log.i(TAG, "Received request to download book: " + intent.getStringExtra(SOURCE_KEY) + " to: "
                + intent.getStringExtra(TARGET_KEY));

        final Context context = this;
        Runnable job = new SafeRunnable() {
            @Override
            public void protectedRun() {
                try {
                    Completion callbacks[] = callbackMap.get(intent.getLongExtra(CALLBACKS_KEY, 0));
                    List<String> downloads = Util.fetchTitle(new File(intent.getStringExtra(TARGET_KEY)),
                            getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY),
                            context, callbacks);

                    if (downloads.isEmpty()) {
                        throw new FileNotFoundException();
                    }

                    for (String download : downloads) {
                        requestAddBook(context, download, null, callbacks);
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Unable to download '" + intent.getStringExtra(SOURCE_KEY) + "': " + ioe.toString());
                    for (Completion callback : callbackMap.remove(intent.getLongExtra(CALLBACKS_KEY, 0))) {
                        callback.completed(false,
                                "Could not download '" + new File(intent.getStringExtra(TARGET_KEY)).getName() + "'. "
                                        + ioe.toString());
                    }
                }
            }
        };

        mDownloadHandler.post(job);
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
     * @param context the caller's context
     * @param target the absolute filename of the book to be added
     * @param callbacks (optional) completion callback
     */
    public static void requestAddBook(Context context, String target, String charset, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, ADD_BOOK)
                .putExtra(TARGET_KEY, target).putExtra(CHARSET_KEY, charset);

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
     * @param context the caller's context
     * @param target the absolute filename of the book to be removed
     * @param callbacks (optional) completion callback
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
     * @param context the caller's context.
     * @param source URL of the book to be downloaded.
     * @param target the suggested absolute filename of the book to be added.
     * @param callbacks (optional) completion callback.
     */
    public static void requestDownloadBook(Context context, String source, String target, Completion... callbacks) {
        Intent intent = new Intent(context, YbkService.class).putExtra(ACTION_KEY, DOWNLOAD_BOOK)
                .putExtra(SOURCE_KEY, source).putExtra(TARGET_KEY, target);

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

    private final class AddBookJob extends SafeRunnable {
        private final Intent intent;

        private AddBookJob(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void protectedRun() {
            String bookName = intent.getStringExtra(TARGET_KEY).replaceFirst("\\.[^\\.]$", "");
            boolean succeeded = true;
            YbkFileReader ybkRdr = null;
            try {
                // clean up any previous instance of the book
                YbkFileReader.closeReader(intent.getStringExtra(TARGET_KEY));
                YbkDAO.getInstance(YbkService.this).deleteBook(intent.getStringExtra(TARGET_KEY));

                // Add the book.
                ybkRdr = YbkFileReader.addBook(YbkService.this, intent.getStringExtra(TARGET_KEY), determineCharset());
                bookName = ybkRdr.getBook().title == null ? bookName : ybkRdr.getBook().title;
            } catch (InvalidFileFormatException ioe) {
                succeeded = false;
                Log.e(TAG, "Could not add '" + Util.lookupBookName(YbkService.this, bookName) + "'.");
                Util.displayError(Main.getMainApplication(), null,
                        getResources().getString(R.string.error_damaged_ebook), bookName);
            } catch (IOException ioe) {
                succeeded = false;
                Log.e(TAG, "Could not add '" + Util.lookupBookName(YbkService.this, bookName) + "'.");
                Util.unexpectedError(Main.getMainApplication(), ioe, "book: " + intent.getStringExtra(TARGET_KEY));
            } finally {
                if (ybkRdr != null) {
                    ybkRdr.unuse();
                    ybkRdr = null;
                }
            }

            if (intent.getLongExtra(CALLBACKS_KEY, 0) == 0 || null == callbackMap) {
                return;
            }

            Completion[] comps = callbackMap.remove(intent.getLongExtra(CALLBACKS_KEY, 0));
            if (null == comps) {
                return;
            }

            for (int index = 0, compLen = comps.length; index < compLen; index++) {
                comps[index].completed(succeeded, "Added '" + bookName + "' to the library");
            }
        }

        private String determineCharset() {
            if (null != intent.getStringExtra(CHARSET_KEY)) {
                return intent.getStringExtra(CHARSET_KEY);
            }

            // TODO remove this kludge for the known Cyrillic books
            if (intent.getStringExtra(TARGET_KEY).equalsIgnoreCase("km.ybk")
                    || intent.getStringExtra(TARGET_KEY).equalsIgnoreCase("vz.ybk")
                    || intent.getStringExtra(TARGET_KEY).equalsIgnoreCase("nz.ybk")) {
                return "CP1251";
            }

            return YbkFileReader.DEFAULT_YBK_CHARSET;
        }
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
         * @param succeeded true if the request succeeded, false if it failed.
         * @param message message associated with completion (may be null)
         */
        void completed(boolean succeeded, String message);
    }

}
