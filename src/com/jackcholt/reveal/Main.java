package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;

//import com.nullwire.trace.ExceptionHandler;

public class Main extends ListActivity implements OnGestureListener {

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

    private boolean BOOLshowFullScreen;

    private final Handler mUpdateLibHandler = new Handler();

    // private static boolean mUpdating = false;

    private List<Book> mBookTitleList;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Debug.startMethodTracing("reveal");

        // send an exception email via this URL
        // ExceptionHandler.register(this,
        // "http://revealreader.thepackhams.com/exception.php");

        mApplication = this;

        // Change DEBUG to "0" in Global.java when building a RELEASE Version
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

        // Get the YanceyBook DAO
        // mYbkDao = YbkDAO.getInstance(this);

        BOOLshowFullScreen = sharedPref.getBoolean("show_fullscreen", false);

        if (BOOLshowFullScreen) {
            getWindow()
                    .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.main);

        // To capture LONG_PRESS gestures
        gestureScanner = new GestureDetector(this);
        registerForContextMenu(getListView());

        boolean configChanged = (getLastNonConfigurationInstance() != null);

        if (!configChanged) {
            BOOLshowSplashScreen = mSharedPref.getBoolean("show_splash_screen", true);

            if (BOOLshowSplashScreen) {
                Util.showSplashScreen(this);
            }
        }

        // Is Network up or not?
        if (Util.isNetworkUp(this)) {
            // Actually go ONLINE and check... duhhhh
            UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
        }

        if (!configChanged) {
            // Check for SDcard presence
            // if we have one create the dirs and look fer ebooks
            if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                // Log.e(Global.TAG, "sdcard not installed");
                Toast.makeText(this, "You must have an SDCARD installed to use Reveal", Toast.LENGTH_LONG).show();
                return;
            } else {
                Util.createDefaultDirs(this);
                // updateBookList();

            }
        }

        refreshBookList();
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
        // Debug.stopMethodTracing();

        // Remove any references so it can be garbage collected.
        // mYbkDao = null;
    }

    private final Runnable mUpdateBookList = new Runnable() {
        public void run() {
            refreshBookList();
        }
    };

    /**
     * Schedule update refresh of the book list on the main thread.
     */
    public void scheduleRefreshBookList() {
        mUpdateLibHandler.post(mUpdateBookList);
    }

    void refreshNotify(String message) {
        Util.sendNotification(this, message, R.drawable.ebooksmall, "Reveal Library", mNotifMgr, mRefreshNotifId,
                Main.class);
    }

    /**
     * Updates the book list.
     * 
     * @throws IOException
     */
    protected void updateBookList() throws IOException {
        // if (mUpdating) {
        // Toast
        // .makeText(this, R.string.update_in_progress,
        // Toast.LENGTH_LONG).show();
        // } else {
        // mUpdating = true;
        refreshLibrary(mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY));
        // }
    }

    /**
     * Convenience method to make calling refreshLibrary() without any parameters retaining its original behavior.
     * 
     * @throws IOException
     */
    private void refreshLibrary(final String strLibDir) throws IOException {
        refreshLibrary(strLibDir, ADD_BOOKS);
    }

    /**
     * Refresh the eBook directory.
     * 
     * @param strLibDir
     *            the path to the library directory.
     * @param addNewBooks
     *            If true, run the code that will add new books to the database as well as the code that removes missing
     *            books from the database (which runs regardless).
     * @throws IOException
     */
    private void refreshLibrary(final String strLibDir, final boolean addNewBooks) throws IOException {

        YbkDAO ybkDao = YbkDAO.getInstance(this);

        // get a list of files from the library directory
        File libraryDir = new File(strLibDir);
        if (!libraryDir.exists()) {
            if (!libraryDir.mkdirs()) {
                Util.displayError(this, null, (String) getResources().getText(R.string.library_not_created));
            }
        }

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

        // if adding files, then calculate set of files on disk, but not in the
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

                @Override
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
            return file.getName().endsWith(".ybk");
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case OPEN_ID:
            return onOpenBookMenuItem(item);

        case DELETE_ID:
            return onDeleteBookMenuItem(item);
        default:
            return super.onContextItemSelected(item);
        }
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
        Book book = getContextMenuBook(item);
        if (book != null) {
            DeleteEbookDialog.create(this, book, (ArrayAdapter<Book>) getListView().getAdapter());
        }
        return true;
    }

    private Book getContextMenuBook(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        // TODO - there's got to be a better way to find the item that was long clicked
        // but haven't found it so far
        View selectedView = menuInfo.targetView;
        ListView listView = getListView();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child == selectedView) {
                return (Book) listView.getItemAtPosition(i);
            }
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // sv - get this working as quickly as possible to test deletion code in JDBM
        menu.add(0, OPEN_ID, 0, R.string.menu_open_ebook);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_ebook);
    }

    @Override
    public void onResume() {
        super.onResume();

        setProgressBarIndeterminateVisibility(false);

        // Set preferences from Setting screen
        SharedPreferences sharedPref = mSharedPref;

        String libDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

        if (!libDir.endsWith("/")) {
            libDir = libDir + "/";
        }

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history).setIcon(
                android.R.drawable.ic_menu_recent_history);
        menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE, R.string.menu_bookmark).setIcon(android.R.drawable.ic_input_get);
        menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE, R.string.menu_refresh_library).setIcon(
                android.R.drawable.ic_menu_rotate);
        menu.add(Menu.NONE, BROWSER_ID, Menu.NONE, R.string.menu_browser).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(Menu.NONE, HELP_ID, Menu.NONE, R.string.menu_help).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, LICENSE_ID, Menu.NONE, R.string.menu_license).setIcon(
                android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(android.R.drawable.ic_menu_share);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        switch (item.getItemId()) {
        case REFRESH_LIB_ID:
            RefreshDialog.create(this);
            try {
                updateBookList();
            } catch (IOException ioe) {
                // TODO - add friendly message
                Util.displayError(this, ioe, null);
            }
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
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {

        setProgressBarIndeterminateVisibility(true);

        // Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        Book book = (Book) listView.getItemAtPosition(selectionRowId);
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, book.id);
        startActivity(intent);

    }

    /**
     * Used to configure any dialog boxes created by this Activity
     */
    @Override
    protected Dialog onCreateDialog(int id) {

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
        return null;
    }

    public boolean onDown(MotionEvent arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    public void onShowPress(MotionEvent arg0) {
        // TODO Auto-generated method stub

    }

    public boolean onSingleTapUp(MotionEvent arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public void onLongPress(MotionEvent arg0) {
        Log.e(Global.TAG, "ONLONGPRESS");
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Bundle extras;
        long histId;
        Intent intent;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case YbkViewActivity.CALL_HISTORY:
            case YbkViewActivity.CALL_BOOKMARK:
                setProgressBarIndeterminateVisibility(true);

                extras = data.getExtras();
                histId = extras.getLong(YbkDAO.ID);

                intent = new Intent(this, YbkViewActivity.class);
                intent.putExtra(YbkDAO.ID, histId);
                intent.putExtra(YbkDAO.FROM_HISTORY, true);
                startActivity(intent);
                break;

            case ACTIVITY_SETTINGS:
                extras = data.getExtras();
                boolean libDirChanged = extras.getBoolean(Settings.EBOOK_DIR_CHANGED);

                if (libDirChanged) {

                    String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                            Settings.DEFAULT_EBOOK_DIRECTORY);

                    try {
                        YbkDAO.getInstance(this).reopen(this);
                        refreshLibrary(libDir, ADD_BOOKS);
                    } catch (IOException ioe) {
                        // TODO - add friendly message
                        Util.displayError(this, ioe, null);
                    }
                    refreshBookList();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // used to give access to "this" in threads and other places
    // DKP
    public static Main getMainApplication() {
        return mApplication;
    }

}
