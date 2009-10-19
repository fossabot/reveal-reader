package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;
import com.nullwire.trace.ExceptionHandler;

public class Main extends ListActivity {

    private static final int HISTORY_ID = Menu.FIRST;
    private static final int BOOKMARK_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
    private static final int BROWSER_ID = Menu.FIRST + 4;
    private static final int HELP_ID = Menu.FIRST + 5;
    private static final int ABOUT_ID = Menu.FIRST + 6;
    private static final int DONATE_ID = Menu.FIRST + 7;
    private static final int LICENSE_ID = Menu.FIRST + 8;
    private static final int REVELUPDATE_ID = Menu.FIRST + 9;
    private static final int DELETE_ID = Menu.FIRST + 10;
    private static final int OPEN_ID = Menu.FIRST + 11;
    private static final int RESET_ID = Menu.FIRST + 12;
    private static final int BOOK_WALKER_ID = Menu.FIRST + 13;
    private static final int PROPERTIES_ID = Menu.FIRST + 14;

    public static int mNotifId = 1;
    public static Main mApplication;
    private static final int ACTIVITY_SETTINGS = 0;
    private static final int LIBRARY_NOT_CREATED = 0;
    private static final int WALK_BOOK = 20;

    // private static final boolean DONT_ADD_BOOKS = false;
    private static final boolean ADD_BOOKS = true;
    public static final String BOOK_WALK_INDEX = "bw_index";

    private NotificationManager mNotifMgr;
    private boolean BOOLshowSplashScreen;
    private static boolean BOOLsplashed = false;
    private static boolean BOOLcheckedOnline = false;
    private boolean BOOLshowFullScreen;
    private final Handler mHandler = new Handler();
    @SuppressWarnings("unused")
    private TextView selection;
    private String strFontSize = "";

    private List<Book> mBookTitleList;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            if (Global.DEBUG == 2) {
                Debug.startMethodTracing("reveal");
            }
            // Disable the Flurry Uncaught Exception Handler
            FlurryAgent.setCaptureUncaughtExceptions(false);
            // and enable the one that emails us :)
            ExceptionHandler.register(this, "http://revealreader.thepackhams.com/exception.php");

            mApplication = this;

            Util.startFlurrySession(this);
            FlurryAgent.onEvent("Main");

            // If this is the first time we've run (the default) then we need to
            // init some values
            if (getSharedPrefs().getBoolean("first_run", true)) {
                SharedPreferences.Editor editor = getSharedPrefs().edit();
                editor.putBoolean("first_run", false);
                editor.putBoolean("show_splash_screen", true);
                editor.putBoolean("show_fullscreen", false);
                editor.commit();
            }

            mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            BOOLshowFullScreen = getSharedPrefs().getBoolean("show_fullscreen", false);

            if (BOOLshowFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

            setContentView(R.layout.main);

            // To capture LONG_PRESS gestures
            registerForContextMenu(getListView());

            if (!(isConfigChanged())) {
                BOOLshowSplashScreen = getSharedPrefs().getBoolean("show_splash_screen", true);

                if (BOOLshowSplashScreen && !BOOLsplashed) {
                    Util.showSplashScreen(this);
                    // only show splash screen once per process instantiation
                    BOOLsplashed = true;
                }
            }

            // Is Network up or not?
            if (!BOOLcheckedOnline && Util.isNetworkUp(this)) {
                // only check once per process instantiation
                BOOLcheckedOnline = true;

                // and wait a little bit to kick it off so it won't slow down
                // the initial display of the list
                mHandler.postDelayed(new SafeRunnable() {
                    @Override
                    public void protectedRun() {
                        // Actually go ONLINE and check... duhhhh
                        UpdateChecker.checkForNewerVersion(Main.this, Global.SVN_VERSION);
                        // Check for a message from US :)
                        MOTDDialog.create(Main.this);
                        // Check for version Notes Unique for this REV
                        RevNotesDialog.create(Main.this);
                    }
                }, 5000);
            }

            if (!(isConfigChanged())) {
                // Check for SDcard presence
                // if we have one create the dirs and look fer ebooks
                if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(this, getResources().getString(R.string.sdcard_required), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Util.createDefaultDirs(this);
                }
            }

            String libDir = getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

            boolean upgrading = false;

            // delete old versions of databases
            File oldDBFiles[] = { new File("/data/data/com.jackcholt.reveal/databases/reveal_ybk.db"),
                    new File(libDir, "reveal_ybk.db"), new File(libDir, "reveal_ybk.lg") };

            for (File oldDBFile : oldDBFiles) {
                if (oldDBFile.exists()) {
                    oldDBFile.delete();
                    upgrading = true;
                }
            }

            // if new version of db doesn't exist, create it
            if (!(new File(new File(libDir, YbkDAO.DATA_DIR), YbkDAO.BOOKS_FILE).exists())) {
                if (upgrading) {
                    // had older version, do upgrading message
                    RefreshDialog.create(this, RefreshDialog.UPGRADE_DB);
                }
                updateBookList();
            }

            refreshBookList();

        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    private boolean isConfigChanged() {
        return getLastNonConfigurationInstance() != null;
    }

    @Override
    protected void onStart() {
        Util.startFlurrySession(this);
        super.onStart();
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
            FlurryAgent.onEndSession(this);

            if (Global.DEBUG == 2) {
                Debug.stopMethodTracing();
            }

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
        mHandler.removeCallbacks(mUpdateBookList);
        mHandler.post(mUpdateBookList);
    }

    void refreshNotify(String message) {
        Util.sendNotification(this, message, R.drawable.ebooksmall, getResources().getString(R.string.app_name),
                mNotifMgr, mNotifId++, Main.class);
    }

    /**
     * Updates the book list.
     */
    protected void updateBookList() {
        refreshLibrary(getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY));
    }

    /**
     * Convenience method to make calling refreshLibrary() without any parameters retaining its original behavior.
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
     *            If true, run the code that will add new books to the database as well as the code that removes missing
     *            books from the database (which runs regardless).
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

        YbkDAO ybkDao = YbkDAO.getInstance(this);

        File[] files = libraryDir.listFiles(new YbkFilter());

        Set<String> fileSet = new HashSet<String>();
        if (files != null) {
            for (File file : files)
                fileSet.add(file.getName());
        }

        // get a list of files on disk
        Set<String> dbSet = new HashSet<String>();
        for (Book book : ybkDao.getBooks()) {
            dbSet.add(book.fileName);
        }

        // if adding files, then calculate set of files on disk, but not in the db
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

            final ProgressNotification progressNotification = new ProgressNotification(this, mNotifId++,
                    R.drawable.ebooksmall, getResources().getString(R.string.refreshing_library));
            progressNotification.update(count, 0);

            Completion callback = new Completion() {
                volatile int remaining = count;

                public void completed(boolean succeeded, String message) {
                    if (succeeded) {
                        scheduleRefreshBookList();
                    } else {
                        Util.sendNotification(Main.this, message, android.R.drawable.stat_sys_warning, getResources()
                                .getString(R.string.app_name), mNotifMgr, mNotifId++, Main.class);
                    }
                    remaining--;
                    progressNotification.update(count, count - remaining);
                    if (remaining <= 0) {
                        progressNotification.hide();
                        refreshNotify(getResources().getString(R.string.refreshed_library));
                    }
                }
            };

            progressNotification.show();

            // schedule the deletion of the db entries that are not on disk
            for (String file : removeFiles)
                YbkService.requestRemoveBook(this, file, callback);

            // schedule the adding of books on disk that are not in the db
            for (String file : addFiles) {
                YbkService.requestAddBook(this, file, null, callback);
            }
        }
    }

    /**
     * Refresh the list of books in the main list.
     */
    private void refreshBookList() {
        mBookTitleList = YbkDAO.getInstance(this).getBookTitles();
        // Now create a simple adapter that finds icons and set it to display
        setListAdapter(new IconicAdapter(this));
        selection = (TextView) findViewById(R.id.label);
    }

    @SuppressWarnings("unchecked")
    class IconicAdapter extends ArrayAdapter {
        Activity context;

        IconicAdapter(Activity context) {
            super(context, R.layout.book_list_row, mBookTitleList);
        }

        public View getView(int location, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.book_list_row, null);
            }

            TextView label = (TextView) row.findViewById(R.id.label);

            strFontSize = getSharedPrefs().getString(Settings.EBOOK_FONT_SIZE_KEY, Settings.DEFAULT_EBOOK_FONT_SIZE);
            label.setTextSize(Integer.parseInt(strFontSize));
            label.setText(mBookTitleList.get(location).title);
            String eBookName = mBookTitleList.get(location).shortTitle;

            ImageView icon = (ImageView) row.findViewById(R.id.icon);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Main.getMainApplication());
            String strRevealDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

            File eBookIcon = new File(strRevealDir, "/thumbnails/" + eBookName + ".jpg");

            FileInputStream is = null;
            try {
                is = new FileInputStream(eBookIcon);
            } catch (FileNotFoundException e) {
                Log.d("ICON: ", "file Not Found Looking online for update");
                // check online for updated thumbnail
                Util.thumbOnlineUpdate(eBookName);
            }

            if (null == is) {
                icon.setImageResource(R.drawable.ebooksmall);
                return row;
            }

            Bitmap bm = BitmapFactory.decodeStream(is, null, null);

            if (null == bm) {
                icon.setImageResource(R.drawable.ebooksmall);
                return row;
            }
            
            final float newWidth = 20;
            final float newHeight = 25;

            Matrix matrix = new Matrix();
            matrix.postScale(newWidth / bm.getWidth(), newHeight / bm.getHeight());
            icon.setImageDrawable(new BitmapDrawable(Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(),
                    matrix, true)));

            return (row);
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
            case PROPERTIES_ID:
                return onEBookProperties(item);
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
        intent.putExtra(YbkDAO.FILENAME, book.fileName);
        startActivity(intent);
        return true;

    }

    protected boolean onEBookProperties(MenuItem item) {
        final Book book = getContextMenuBook(item);
        String metaData = null;
        String message;
        YbkFileReader ybkReader;
        try {
            ybkReader = YbkFileReader.getReader(this, book.fileName);
            try {
                metaData = ybkReader.readMetaData();
            } finally {
                ybkReader.unuse();
            }
        } catch (IOException e) {
            // couldn't read meta data, that's ok we'll make some up
        }
        if (metaData != null && metaData.length() > 0) {
            message = metaData.replaceFirst("(?i)^.*<end>", "");
        } else {
            message = MessageFormat.format(getResources().getString(R.string.ebook_info_message), book.title,
                    book.fileName);
        }

        new EBookPropertiesDialog(this, getResources().getString(R.string.menu_ebook_properties), message, book).show();
        return true;

    }

    class EBookPropertiesDialog extends InfoDialog {
        final Book book;
        final Spinner spinner;
        final CharsetEntry charsets[];

        protected EBookPropertiesDialog(final Context _this, String title, String message, Book book) {
            super(_this, title, message);
            this.book = book;
            spinner = (Spinner) findViewById(R.id.charset);
            String strings[] = (String[]) getResources().getStringArray(R.array.charsets);
            charsets = new CharsetEntry[strings.length / 2];
            int selected = book.charset == null ? 0 : -1;
            for (int i = 0; i < charsets.length; i++) {
                charsets[i] = new CharsetEntry(strings[i * 2], strings[(i * 2) + 1]);
                if (selected == -1 && charsets[i].value.equalsIgnoreCase(book.charset)) {
                    selected = i;
                }
            }

            if (selected == -1) {
                // the current book charset isn't in the list, force it to Latin
                // for now
                selected = 0;
            }

            ArrayAdapter<CharsetEntry> adapter = new ArrayAdapter<CharsetEntry>(Main.this,
                    android.R.layout.simple_spinner_item, charsets);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(selected);
        }

        @Override
        protected int getContentViewId() {
            return R.layout.dialog_ebook_props;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onClick(View v) {
            super.onClick(v);
            String charset = book.charset == null ? YbkFileReader.DEFAULT_YBK_CHARSET : book.charset;
            int selected = spinner.getSelectedItemPosition();
            String newCharset = charsets[selected].value;
            if (!newCharset.equals(charset)) {
                YbkService.requestRemoveBook(Main.this, book.fileName);
                YbkService.requestAddBook(Main.this, book.fileName, newCharset, new Completion() {

                    public void completed(boolean succeeded, String message) {
                        if (succeeded) {
                            scheduleRefreshBookList();
                        } else {
                            Util.sendNotification(Main.this, message, android.R.drawable.stat_sys_warning,
                                    getResources().getString(R.string.app_name), mNotifMgr, mNotifId++, Main.class);
                        }
                    }
                });
                ((ArrayAdapter<Book>) getListView().getAdapter()).remove(book);
                Map<String, String> filenameMap = new HashMap<String, String>();
                filenameMap.put("filename", book.fileName);
                FlurryAgent.onEvent("ChangeCharset", filenameMap);
                Toast.makeText(
                        Main.this,
                        MessageFormat.format(getResources().getString(R.string.changing_charset),
                                charsets[selected].key), Toast.LENGTH_LONG).show();
            }
        }

    }

    private static class CharsetEntry {
        final String key;
        final String value;

        CharsetEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    protected boolean onBookWalker(int index) {
        if (index >= 0 && index < mBookTitleList.size()) {
            Book book = mBookTitleList.get(index);
            if (book != null) {
                setProgressBarIndeterminateVisibility(true);
                Intent intent = new Intent(this, YbkViewActivity.class);
                intent.putExtra(YbkDAO.FILENAME, book.fileName);
                intent.putExtra(BOOK_WALK_INDEX, index);
                startActivityForResult(intent, WALK_BOOK);
            }
        }
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
                    File file = new File(getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                            Settings.DEFAULT_EBOOK_DIRECTORY), book.fileName);
                    if (file.exists()) {
                        if (!file.delete()) {
                            // TODO - should tell user about this
                        }
                    }
                    // delete associated temporary image files
                    Util.deleteFiles(new File(file.getParentFile(), "/images"), file.getName().replaceFirst(
                            "(.*)\\.[^\\.]+$", "$1")
                            + "_.+");
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
                    book.fileName);
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
            menu.add(0, PROPERTIES_ID, 0, R.string.menu_ebook_properties);
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

            String libDir = getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

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
            menu.add(Menu.NONE, DONATE_ID, Menu.NONE, R.string.donate_menu).setIcon(
                    android.R.drawable.ic_menu_info_details);
            menu.add(Menu.NONE, LICENSE_ID, Menu.NONE, R.string.menu_license).setIcon(
                    android.R.drawable.ic_menu_info_details);
            menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(
                    android.R.drawable.ic_menu_preferences);
            menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(
                    android.R.drawable.ic_menu_share);
            menu.add(Menu.NONE, RESET_ID, Menu.NONE, R.string.reset).setIcon(android.R.drawable.ic_menu_share);
            if (Global.DEBUG == 1)
                menu.add(Menu.NONE, BOOK_WALKER_ID, Menu.NONE, R.string.book_walker).setIcon(
                        android.R.drawable.ic_menu_share);
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
                // RefreshDialog.create(this, RefreshDialog.REFRESH_DB);
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
                UpdateChecker.checkForNewerVersion(this, Global.SVN_VERSION);
                return true;

            case ABOUT_ID:
                AboutDialog.create();
                return true;

            case DONATE_ID:
                DonateDialog.create(this);
                return true;

            case LICENSE_ID:
                LicenseDialog.create(this);
                return true;

            case HELP_ID:
                OnlineHelpDialog.create(this);
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

            case PROPERTIES_ID:
                return onEBookProperties(item);

            case BOOK_WALKER_ID:
                return onBookWalker(0);
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
            intent.putExtra(YbkDAO.FILENAME, book.fileName);
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

        try {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                case YbkViewActivity.CALL_HISTORY:
                case YbkViewActivity.CALL_BOOKMARK:
                    setProgressBarIndeterminateVisibility(true);

                    YbkDAO ybkDao = YbkDAO.getInstance(this);

                    extras = data.getExtras();

                    boolean deleteBookmark = extras.getBoolean(BookmarkDialog.DELETE_BOOKMARK);

                    histId = extras.getLong(YbkDAO.HISTORY_ID);

                    if (deleteBookmark) {
                        int bmId = extras.getInt(YbkDAO.BOOKMARK_NUMBER);
                        History hist = ybkDao.getBookmark(bmId);
                        DeleteBookmarkDialog.create(this, hist);
                    } else if (histId != 0) {
                        intent = new Intent(this, YbkViewActivity.class);
                        intent.putExtra(YbkDAO.HISTORY_ID, histId);
                        startActivity(intent);
                    }

                    break;

                case ACTIVITY_SETTINGS:
                    extras = data.getExtras();
                    boolean libDirChanged = extras.getBoolean(Settings.EBOOK_DIR_CHANGED);

                    if (libDirChanged) {

                        String libDir = getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                                Settings.DEFAULT_EBOOK_DIRECTORY);

                        YbkDAO.getInstance(this).open(this);
                        refreshLibrary(libDir, ADD_BOOKS);
                        refreshBookList();
                    }

                case WALK_BOOK:
                    int lastIndex = data.getIntExtra(BOOK_WALK_INDEX, -1);
                    if (lastIndex != -1)
                        onBookWalker(lastIndex + 1);
                    break;
                }
            }
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

    private void resetApp() {
        SafeRunnable action = new SafeRunnable() {
            @Override
            public void protectedRun() {
                FlurryAgent.onEvent("Reset");
                // cleanup current library directory
                File libDir = new File(getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                        Settings.DEFAULT_EBOOK_DIRECTORY));
                Util.deleteFiles(new File(libDir, "images"), ".*");
                Util.deleteFiles(new File(libDir, "thumbnails"), ".*");
                Util.deleteFiles(libDir, ".*\\.(tmp|lg|db)");
                Util.deleteFiles(new File(libDir, "data"), "books\\.dat|.*\\.chp");
                if (!libDir.getAbsoluteFile().toString().equalsIgnoreCase(Settings.DEFAULT_EBOOK_DIRECTORY)) {
                    // cleanup default library directory if it wasn't the one we
                    // were using
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY, "images"), ".*");
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY, "thumbnails"), ".*");
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY), ".*\\.(tmp|lg|db)");
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY, "data"), "books\\.dat|.*\\.chp");
                }
                // cleanup any sqlite databases
                Util.deleteFiles(new File("/data/data/com.jackcholt.reveal/databases"), ".*\\.db");

                // cleanup preferences (can't seem to delete file, so tell the
                // preferences manager to clear them all)
                getSharedPrefs().edit().clear().commit();

                // shutdown, but first queue a request to restart
                Intent restartIntent = new Intent(Main.this, Main.class);
                restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(restartIntent);
                System.exit(0);
            }
        };
        ConfirmActionDialog.confirmedAction(this, R.string.reset, R.string.confirm_reset, R.string.reset, action);
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    // Display Toast-Message
    public static void displayToastMessage(String message) {
        Toast.makeText(Main.getMainApplication(), message, Toast.LENGTH_LONG).show();
    }

}
