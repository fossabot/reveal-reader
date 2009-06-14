package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

public class Main extends ListActivity {

    private static final int HISTORY_ID = Menu.FIRST;
    private static final int BOOKMARK_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
    private static final int BROWSER_ID = Menu.FIRST + 4;
    private static final int HELP_ID = Menu.FIRST + 5;
    private static final int ABOUT_ID = Menu.FIRST + 6;
    private static final int LICENSE_ID = Menu.FIRST + 7;
    private static final int REVELUPDATE_ID = Menu.FIRST + 8;
    private static final int DELETE_ID = Menu.FIRST + 9;
    private static final int OPEN_ID = Menu.FIRST + 10;
    private static final int RESET_ID = Menu.FIRST + 11;

    private static int mRefreshNotifId = 0;
    public static int mNotifId = 1;
    public static Main mApplication;
    private static final int ACTIVITY_SETTINGS = 0;
    private static final int LIBRARY_NOT_CREATED = 0;

    // private static final boolean DONT_ADD_BOOKS = false;
    private static final boolean ADD_BOOKS = true;

    // Gestures Stuff
    private NotificationManager mNotifMgr;

    @SuppressWarnings("unused")
    private GestureDetector gestureScanner;
    private SharedPreferences mSharedPref;
    private boolean BOOLshowSplashScreen;
    private static boolean BOOLsplashed = false;
    private boolean BOOLshowFullScreen;
    private final Handler mHandler = new Handler();

    // private static boolean mUpdating = false;
    private List<Book> mBookTitleList;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            // Debug.startMethodTracing("reveal");

            mApplication = this;

            // Change DEBUG to "0" in Global.java when building a RELEASE
            // Version
            // for the GOOGLE APP MARKET
            // This allows for real usage stats and end user error reporting
            if (Global.DEBUG == 0) {
                // Release Key for use of the END USERS
                FlurryAgent.onStartSession(this, "BLRRZRSNYZ446QUWKSP4");
            } else {
                // Development key for use of the DEVELOPMENT TEAM
                FlurryAgent.onStartSession(this, "VYRRJFNLNSTCVKBF73UP");
            }
            FlurryAgent.onEvent("Main");

            mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            SharedPreferences sharedPref = mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

            BOOLshowFullScreen = sharedPref.getBoolean("show_fullscreen", false);

            if (BOOLshowFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

            setContentView(R.layout.main);

            // To capture LONG_PRESS gestures
            // gestureScanner = new GestureDetector(this);
            registerForContextMenu(getListView());

            boolean configChanged = (getLastNonConfigurationInstance() != null);

            if (!configChanged) {
                BOOLshowSplashScreen = mSharedPref.getBoolean("show_splash_screen", true);

                if (BOOLshowSplashScreen && !BOOLsplashed) {
                    Util.showSplashScreen(this);
                    // only show splash screen once per process instantiation
                    BOOLsplashed = true;
                }
            }

            // Is Network up or not?
            if (Util.isNetworkUp(this)) {
                // Actually go ONLINE and check... duhhhh
                UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
                // Check for a message from US :)
                // MOTDDialog.create(this);
            }

            if (!configChanged) {
                // Check for SDcard presence
                // if we have one create the dirs and look fer ebooks
                if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                    // Log.e(Global.TAG, "sdcard not installed");
                    Toast.makeText(this, getResources().getString(R.string.sdcard_required), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Util.createDefaultDirs(this);
                    // updateBookList();
                }
            }

            File oldDBfile = new File("/data/data/com.jackcholt.reveal/databases/reveal_ybk.db");
            if (oldDBfile.exists()) {
                oldDBfile.delete();
                // prompt to warn of new DB create
                RefreshDialog.create(this, RefreshDialog.UPGRADE_DB);
                updateBookList();
            } else if (!(new File(
                    mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY),
                    "reveal_ybk.db").exists())) {
                Toast.makeText(this, getResources().getString(R.string.refresh_title), Toast.LENGTH_LONG).show();
                updateBookList();
            }

            refreshBookList();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
            FlurryAgent.onEndSession(this);
            // Debug.stopMethodTracing();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    private final Runnable mUpdateBookList = new Runnable() {
        public void run() {
            try {
                refreshBookList();
            } catch (RuntimeException rte) {
                Util.unexpectedError(Main.this, rte);
            } catch (Error e) {
                Util.unexpectedError(Main.this, e);
            }

        }
    };

    /**
     * Schedule update refresh of the book list on the main thread.
     */
    public void scheduleRefreshBookList() {
        mHandler.post(mUpdateBookList);
    }

    void refreshNotify(String message) {
        Util.sendNotification(this, message, R.drawable.ebooksmall, "Reveal Library", mNotifMgr, mRefreshNotifId,
                Main.class);
    }

    /**
     * Updates the book list.
     */
    protected void updateBookList() {
        refreshLibrary(mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY));
    }

    /**
     * Convenience method to make calling refreshLibrary() without any
     * parameters retaining its original behavior.
     */
    private void refreshLibrary(final String strLibDir) {
        refreshLibrary(strLibDir, ADD_BOOKS);
    }

    /**
     * Refresh the eBook directory.
     * 
     * @param strLibDir
     *            the path to the library directory.
     * @param addNewBooks
     *            If true, run the code that will add new books to the database
     *            as well as the code that removes missing books from the
     *            database (which runs regardless).
     */
    private void refreshLibrary(final String strLibDir, final boolean addNewBooks) {

        // get a list of files from the library directory
        File libraryDir = new File(strLibDir);
        if (!libraryDir.exists()) {
            if (!libraryDir.mkdirs()) {
                Util.displayError(this, (Throwable) null, getResources().getString(R.string.library_not_created));
                return;
            }
        }

        try {
            YbkDAO ybkDao = YbkDAO.getInstance(this);

            File[] files = libraryDir.listFiles(new YbkFilter());

            Set<String> fileSet = new HashSet<String>();
            if (files != null) {
                for (File file : files)
                    fileSet.add(file.getAbsolutePath().toLowerCase());
            }

            // get a list of files on disk
            Set<String> dbSet = new HashSet<String>();
            for (Book book : ybkDao.getBooks()) {
                dbSet.add(book.fileName);
            }

            // if adding files, then calculate set of files on disk, but not in
            // the
            // db
            Set<String> addFiles;
            if (addNewBooks) {
                addFiles = new HashSet<String>(fileSet);
                addFiles.removeAll(dbSet);
            } else {
                addFiles = Collections.emptySet();
            }

            // calculate the set of files in the db but not on disk
            Set<String> removeFiles = dbSet;
            removeFiles.removeAll(fileSet);

            final int count = addFiles.size() + removeFiles.size();
            if (count != 0) {
                Completion callback = new Completion() {
                    volatile int remaining = count;

                    // @Override
                    public void completed(boolean succeeded, String message) {
                        if (succeeded) {
                            refreshNotify(message);
                            scheduleRefreshBookList();
                        } else {
                            Util.sendNotification(Main.this, message, android.R.drawable.stat_sys_warning,
                                    "Reveal Library", mNotifMgr, mNotifId++, Main.class);
                        }
                        if (--remaining <= 0) {
                            refreshNotify("Refreshing of library complete.");
                            // mUpdating = false;
                        }
                    }
                };

                refreshNotify("Refreshing the library");

                // schedule the deletion of the db entries that are not on disk
                for (String file : removeFiles)
                    YbkService.requestRemoveBook(this, file, callback);

                // schedule the adding of books on disk that are not in the db
                for (String file : addFiles) {
                    YbkService.requestAddBook(this, file, callback);
                }
            }
        } catch (IOException ioe) {
            Util.displayError(this, ioe, getResources().getString(R.string.error_lib_refresh));
        }
    }

    /**
     * Refresh the list of books in the main list.
     */
    private void refreshBookList() {

        try {
            YbkDAO ybkDao;
            ybkDao = YbkDAO.getInstance(this);

            mBookTitleList = ybkDao.getBookTitles();
            // Now create a simple adapter and set it to display
            ArrayAdapter<Book> bookAdapter = new ArrayAdapter<Book>(this, R.layout.book_list_row, mBookTitleList);

            setListAdapter(bookAdapter);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Class for filtering non-YBK files out of a list of files
     */
    private class YbkFilter implements FileFilter {
        public boolean accept(File file) {
            return file.getName().toLowerCase().endsWith(".ybk");
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        try {
            switch (item.getItemId()) {
            case OPEN_ID:
                return onOpenBookMenuItem(item);

            case DELETE_ID:
                return onDeleteBookMenuItem(item);
            default:
                return super.onContextItemSelected(item);
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
        return true;
    }

    protected boolean onOpenBookMenuItem(MenuItem item) {
        Book book = getContextMenuBook(item);
        setProgressBarIndeterminateVisibility(true);
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, book.id);
        startActivity(intent);
        return true;

    }

    @SuppressWarnings("unchecked")
    private boolean onDeleteBookMenuItem(MenuItem item) {
        final Book book = getContextMenuBook(item);
        if (book != null) {
            SafeRunnable action = new SafeRunnable() {
                @Override
                public void protectedRun() {
                    // delete the book file
                    File file = new File(book.fileName);
                    if (file.exists()) {
                        if (!file.delete()) {
                            // TODO - should tell user about this
                        }
                    }
                    // delete associated temporary image files
                    Util.deleteFiles(new File(file.getParentFile(), "/images"), file.getName().replaceFirst("(.*)\\.[^\\.]+$", "$1") + "_.+");
                    // remove the book from the database
                    YbkService.requestRemoveBook(Main.this, book.fileName);
                    // remove the book from the on-screen list
                    ((ArrayAdapter<Book>) getListView().getAdapter()).remove(book);
                    Map<String, String> filenameMap = new HashMap<String, String>();
                    filenameMap.put("filename", book.fileName);
                    FlurryAgent.onEvent("DeleteBook", filenameMap);
                }
            };
            String message = MessageFormat.format(getResources().getString(R.string.confirm_delete_ebook), book.title,
                    new File(book.fileName).getName());
            ConfirmActionDialog.confirmedAction(this, R.string.really_delete_title, message, R.string.delete, action);
        }
        return true;
    }

    private Book getContextMenuBook(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        return (Book) getListView().getItemAtPosition(menuInfo.position);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        try {
            super.onCreateContextMenu(menu, v, menuInfo);
            menu.add(0, OPEN_ID, 0, R.string.menu_open_ebook);
            menu.add(0, DELETE_ID, 0, R.string.menu_delete_ebook);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    @Override
    public void onResume() {
        try {
            super.onResume();

            setProgressBarIndeterminateVisibility(false);

            // Set preferences from Setting screen
            SharedPreferences sharedPref = mSharedPref;

            String libDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

            if (!libDir.endsWith("/")) {
                libDir = libDir + "/";
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        try {
            super.onCreateOptionsMenu(menu);
            menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history).setIcon(
                    android.R.drawable.ic_menu_recent_history);
            menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE, R.string.menu_bookmark)
                    .setIcon(android.R.drawable.ic_input_get);
            menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE, R.string.menu_refresh_library).setIcon(
                    android.R.drawable.ic_menu_rotate);
            menu.add(Menu.NONE, BROWSER_ID, Menu.NONE, R.string.menu_browser)
                    .setIcon(android.R.drawable.ic_menu_set_as);
            menu.add(Menu.NONE, HELP_ID, Menu.NONE, R.string.menu_help)
                    .setIcon(android.R.drawable.ic_menu_info_details);
            menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(
                    android.R.drawable.ic_menu_info_details);
            menu.add(Menu.NONE, LICENSE_ID, Menu.NONE, R.string.menu_license).setIcon(
                    android.R.drawable.ic_menu_info_details);
            menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(
                    android.R.drawable.ic_menu_preferences);
            menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(
                    android.R.drawable.ic_menu_share);
            menu.add(Menu.NONE, RESET_ID, Menu.NONE, R.string.reset).setIcon(android.R.drawable.ic_menu_share);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        try {
            switch (item.getItemId()) {
            case REFRESH_LIB_ID:
                RefreshDialog.create(this, RefreshDialog.REFRESH_DB);
                updateBookList();
                return true;

            case RESET_ID:
                resetApp();
                return true;

            case SETTINGS_ID:
                Intent intent = new Intent(this, Settings.class);
                startActivityForResult(intent, ACTIVITY_SETTINGS);
                return true;

            case BROWSER_ID:
                Intent browserIntent = new Intent(this, TitleBrowser.class);
                startActivity(browserIntent);
                return true;

            case REVELUPDATE_ID:
                Toast.makeText(this, R.string.checking_for_new_version_online, Toast.LENGTH_SHORT).show();
                UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
                return true;

            case ABOUT_ID:
                AboutDialog.create(this);
                return true;

            case LICENSE_ID:
                LicenseDialog.create(this);
                return true;

            case HELP_ID:
                HelpDialog.create(this);
                return true;

            case HISTORY_ID:
                startActivityForResult(new Intent(this, HistoryDialog.class), YbkViewActivity.CALL_HISTORY);
                return true;

            case BOOKMARK_ID:
                Intent bmIntent = new Intent(this, BookmarkDialog.class);
                bmIntent.putExtra("fromMain", true);
                startActivityForResult(bmIntent, YbkViewActivity.CALL_BOOKMARK);
                return true;

            case OPEN_ID:
                return onOpenBookMenuItem(item);

            case DELETE_ID:
                return onDeleteBookMenuItem(item);
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        try {
            setProgressBarIndeterminateVisibility(true);

            // Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" +
            // id);
            Book book = (Book) listView.getItemAtPosition(selectionRowId);
            Intent intent = new Intent(this, YbkViewActivity.class);
            intent.putExtra(YbkDAO.ID, book.id);
            startActivity(intent);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /**
     * Used to configure any dialog boxes created by this Activity
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        try {
            switch (id) {
            case LIBRARY_NOT_CREATED:
                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(
                        R.string.library_not_created).setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so do some stuff */
                            }
                        }).create();
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Bundle extras;
        long histId;
        Intent intent;
        boolean fromHistory = false;

        try {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                case YbkViewActivity.CALL_HISTORY:
                case YbkViewActivity.CALL_BOOKMARK:
                    setProgressBarIndeterminateVisibility(true);

                    YbkDAO ybkDao = YbkDAO.getInstance(this);

                    extras = data.getExtras();

                    boolean deleteBookmark = extras.getBoolean(BookmarkDialog.DELETE_BOOKMARK);

                    histId = extras.getLong(YbkDAO.ID);
                    fromHistory = extras.getBoolean(YbkDAO.FROM_HISTORY);

                    if (fromHistory) {
                        intent = new Intent(this, YbkViewActivity.class);
                        intent.putExtra(YbkDAO.ID, histId);
                        intent.putExtra(YbkDAO.FROM_HISTORY, true);
                        startActivity(intent);
                    } else if (deleteBookmark) {
                        int bmId = extras.getInt(YbkDAO.BOOKMARK_NUMBER);
                        History hist = ybkDao.getBookmark(bmId);
                        DeleteBookmarkDialog.create(this, hist);
                    }

                    break;

                case ACTIVITY_SETTINGS:
                    extras = data.getExtras();
                    boolean libDirChanged = extras.getBoolean(Settings.EBOOK_DIR_CHANGED);

                    if (libDirChanged) {

                        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                                Settings.DEFAULT_EBOOK_DIRECTORY);

                        try {
                            YbkDAO.getInstance(this).reopen(this);
                        } catch (IOException ioe) {
                            Util.displayError(this, ioe, getResources().getString(R.string.error_lib_refresh));
                        }
                        refreshLibrary(libDir, ADD_BOOKS);
                        refreshBookList();
                    }
                }
            }
        } catch (IOException ioe) {
            Util.unexpectedError(this, ioe);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // used to give access to "this" in threads and other places
    // DKP
    public static Main getMainApplication() {
        return mApplication;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        try {
            YbkService.stop(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        super.onDestroy();
    }

    private void resetApp() {
        SafeRunnable action = new SafeRunnable() {
            @Override
            public void protectedRun() {
                FlurryAgent.onEvent("Reset");
                // cleanup current library directory
                File libDir = new File(mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                        Settings.DEFAULT_EBOOK_DIRECTORY));
                Util.deleteFiles(libDir, ".*\\.(tmp|lg|db)");
                if (!libDir.getAbsoluteFile().toString().equalsIgnoreCase(Settings.DEFAULT_EBOOK_DIRECTORY)) {
                    // cleanup default library directory if it wasn't the one we
                    // were using
                    Util.deleteFiles(new File(libDir, "images"), ".*");
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY), ".*\\.(tmp|lg|db)");
                }
                // cleanup any sqlite databases
                Util.deleteFiles(new File("/data/data/com.jackcholt.reveal/databases"), ".*\\.db");
                // cleanup preferences (can't seem to delete file, so tell the
                // preferences manager to clear them all)
                mSharedPref.edit().clear().commit();

                // shutdown, but first queue a request to restart
                Intent restartIntent = new Intent(Main.this, Main.class);
                restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(restartIntent);
                System.exit(0);
            }
        };
        ConfirmActionDialog.confirmedAction(this, R.string.reset, R.string.confirm_reset, R.string.reset, action);
    }
}
