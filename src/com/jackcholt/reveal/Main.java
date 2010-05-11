package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;
import com.nullwire.trace.ExceptionHandler;

public class Main extends ListActivity {
    private static final String TAG = "reveal.Main";
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
    private static final int MOVE_TO_FOLDER_ID = Menu.FIRST + 15;
    private static final int RENAME_ID = Menu.FIRST + 16;

    public static int mNotifId = 1;
    public static Main mApplication;
    private static final int ACTIVITY_SETTINGS = 0;
    private static final int LIBRARY_NOT_CREATED = 0;
    private static final int WALK_BOOK = 20;

    private static final boolean ADD_BOOKS = true;
    public static final String BOOK_WALK_INDEX = "bw_index";
    public static final String FOLDER = "folder";

    private NotificationManager mNotifMgr;
    private boolean mBOOLshowSplashScreen;
    private static boolean mBOOLsplashed = false;
    private static boolean mBOOLcheckedOnline = false;
    private boolean mBOOLshowFullScreen;

    private final Handler mHandler = new Handler();
    @SuppressWarnings("unused")
    private TextView mSelection;

    private List<Book> mBookTitleList;
    private List<Object> mCurrentList;

    private static final int ALL_FOLDER_THRESHOLD = 5;
    private String mCurrentFolder = "";

    private int mThemeId = -1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                mCurrentFolder = savedInstanceState.getString("mCurrentFolder");
            }

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

            // If this is the first time we've run (the default) then we need to init some values
            if (getSharedPrefs().getBoolean("first_run", true)) {
                SharedPreferences.Editor editor = getSharedPrefs().edit();
                editor.putBoolean("first_run", false);
                editor.putBoolean("show_splash_screen", true);
                editor.putBoolean("show_fullscreen", false);
                editor.putBoolean("show_zoom", false);
                editor.commit();
            }

            mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            mBOOLshowFullScreen = getSharedPrefs().getBoolean("show_fullscreen", false);

            if (mBOOLshowFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

            setContentView(R.layout.main);

            // To capture LONG_PRESS gestures
            registerForContextMenu(getListView());

            if (!(isConfigChanged())) {
                mBOOLshowSplashScreen = getSharedPrefs().getBoolean("show_splash_screen", true);

                if (mBOOLshowSplashScreen && !mBOOLsplashed) {
                    Util.showSplashScreen(this);
                    // only show splash screen once per process instantiation
                    mBOOLsplashed = true;
                }
            }

            // Is Network up or not?
            if (!mBOOLcheckedOnline && Util.areNetworksUp(this)) {
                // only check once per process instantiation
                mBOOLcheckedOnline = true;

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

    @Override
    public void setTheme(int resid) {
        // bug workaround alert: see http://code.google.com/p/android/issues/detail?id=4394
        mThemeId = Util.getTheme(getSharedPrefs());
        super.setTheme(mThemeId);
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
     * @param strLibDir the path to the library directory.
     * @param addNewBooks If true, run the code that will add new books to the database as well as the code that removes
     *        missing books from the database (which runs regardless).
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
        SortedMap<String, SortedSet<String>> folderMap = YbkDAO.getInstance(this).getFolderMap();
        mCurrentList = new ArrayList<Object>();
        String allBooksFolderName = getResources().getString(R.string.all_books_folder);

        if (mCurrentFolder.length() == 0) {
            // top level folder
            if (folderMap.size() >= ALL_FOLDER_THRESHOLD) {
                // if more than a certain number of folders, add an all books folder
                mCurrentList.add(allBooksFolderName);
            }
            // add the list of folders to the current list
            Set<String> excludeSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (Map.Entry<String, SortedSet<String>> folderEntry : folderMap.entrySet()) {
                mCurrentList.add(folderEntry.getKey());
                // remember the books in the folders so we can use it to exclude books from the top level
                excludeSet.addAll(folderEntry.getValue());
            }

            // and also add the books that are not in folders
            for (Book book : mBookTitleList) {
                if (!excludeSet.contains(book.fileName)) {
                    mCurrentList.add(book);
                }
            }
        } else {
            // not top level folder
            // add link back to the top level folder
            mCurrentList.add(getResources().getString(R.string.top_level_folder));
            if (mCurrentFolder.equals(allBooksFolderName)) {
                // if all books folder, add them all in
                mCurrentList.addAll(mBookTitleList);
            } else {
                // add the intersection of books in the folder and the books we have
                SortedSet<String> folderSet = folderMap.get(mCurrentFolder);
                if (folderSet != null) {
                    for (Book book : mBookTitleList) {
                        if (folderSet.contains(book.fileName)) {
                            mCurrentList.add(book);
                        }
                    }
                }
            }
        }

        // Now create a simple adapter that finds icons and set it to display
        setListAdapter(new IconicAdapter(this));
    }

    @SuppressWarnings("unchecked")
    class IconicAdapter extends ArrayAdapter {
        private static final float NEW_WIDTH = 30;
        private static final float NEW_HEIGHT = 37;
        private SharedPreferences sharedPref = getSharedPrefs();
        private String strFontSize = sharedPref.getString(Settings.EBOOK_FONT_SIZE_KEY,
                Settings.DEFAULT_EBOOK_FONT_SIZE);
        private String strRevealDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY);

        // Needed to avoid a deprecated method in SDK 1.6+
        // private Resources res;

        IconicAdapter(Main context) {
            super(context, R.layout.book_list_row, mCurrentList);
            // See the comment above the declaration of this variable
            // res = context.getResources();
        }

        public View getView(int location, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.book_list_row, null);
            }

            TextView label = (TextView) row.findViewById(R.id.label);
            label.setTextSize(Math.round(Integer.parseInt(strFontSize) * 1.5));

            ImageView icon = (ImageView) row.findViewById(R.id.icon);

            Object item = mCurrentList.get(location);
            if (item instanceof Book) {
                // book
                Book book = (Book) item;
                label.setText(book.title);
                String eBookName = book.shortTitle;

                // timer.addEvent("before getting book icon");
                FileInputStream iconInputStream = null;
                try {
                    iconInputStream = new FileInputStream(new File(strRevealDir, "/.thumbnails/" + eBookName + ".jpg"));
                    // timer.addEvent("after actually getting book icon");
                } catch (FileNotFoundException e) {
                    Log.d("IconicAdapter: ", "file Not Found Look online for update");
                    // timer.addEvent("didn't get book icon");
                }

                if (null == iconInputStream) {
                    icon.setImageResource(R.drawable.ebooksmall);
                    return row;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(iconInputStream, null, null);

                if (null == bitmap) {
                    icon.setImageResource(R.drawable.ebooksmall);
                    return row;
                }

                Matrix matrix = new Matrix();
                matrix.postScale(NEW_WIDTH / bitmap.getWidth(), NEW_HEIGHT / bitmap.getHeight());
                icon.setImageDrawable(new BitmapDrawable(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap
                        .getHeight(), matrix, true)));
                /*
                 * TODO change the above (which is deprecated in 1.6+) to the following when we move to 1.6+ of the SDK
                 * icon.setImageDrawable(new BitmapDrawable(res, Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                 * bitmap .getHeight(), matrix, true)));
                 */

            } else {
                String labelText = item.toString();
                label.setText(labelText);
                if (labelText.equals(getResources().getString(R.string.top_level_folder))) {
                    icon.setImageResource(R.drawable.home24);
                } else {
                    icon.setImageResource(R.drawable.folder24y);
                }
            }
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
                return onOpenMenuItem(item);

            case MOVE_TO_FOLDER_ID:
                startActivityForResult(new Intent(this, MoveDialog.class).putExtra("currentFolder", mCurrentFolder)
                        .putExtra("fileName", getContextMenuBook(item).fileName), MOVE_TO_FOLDER_ID);
                return true;
            case DELETE_ID:
                return onDeleteMenuItem(item);

            case RENAME_ID:
                return onRenameMenuItem(item);

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

    protected boolean onOpenMenuItem(MenuItem item) {
        openItem(getContextMenuItem(item));
        return true;
    }

    protected void openItem(Object item) {
        if (item instanceof Book) {
            setProgressBarIndeterminateVisibility(true);
            startActivityForResult(new Intent(this, YbkViewActivity.class).putExtra(YbkDAO.FILENAME,
                    ((Book) item).fileName), YbkViewActivity.SHOW_BOOK);
        } else {
            // open folder
            mCurrentFolder = item.toString();
            if (mCurrentFolder.equals(getResources().getString(R.string.top_level_folder))) {
                mCurrentFolder = "";
            }
            refreshBookList();
        }
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

    protected void walkBook(final int index) {
        if (index < 0 || index < mBookTitleList.size() || null == mBookTitleList.get(index)) {
            return;
        }

        setProgressBarIndeterminateVisibility(true);
        startActivityForResult(new Intent(this, YbkViewActivity.class).putExtra(YbkDAO.FILENAME,
                mBookTitleList.get(index).fileName).putExtra(BOOK_WALK_INDEX, index), WALK_BOOK);

    }

    @SuppressWarnings("unchecked")
    private boolean onDeleteMenuItem(MenuItem item) {
        Object selectedItem = getContextMenuItem(item);

        if (selectedItem instanceof Book) {
            final Book book = (Book) selectedItem;
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
                    Util.deleteFiles(new File(file.getParentFile(), "/.images"), file.getName().replaceFirst(
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
        } else {
            final String folder = selectedItem.toString();
            SafeRunnable action = new SafeRunnable() {
                @Override
                public void protectedRun() {
                    // delete the folder
                    YbkDAO.getInstance(Main.this).removeFolder(folder);
                    refreshBookList();
                }
            };
            String message = MessageFormat.format(getResources().getString(R.string.confirm_delete_folder), folder);
            ConfirmActionDialog.confirmedAction(this, R.string.really_delete_folder, message, R.string.delete, action);
        }
        return true;
    }

    private boolean onRenameMenuItem(MenuItem item) {
        Object selectedItem = getContextMenuItem(item);

        if (selectedItem instanceof Book) {
            // if we ever decide to somehow allow renaming of books, this is where the code would go
        } else {
            final String folder = selectedItem.toString();
            final View textEntryView = LayoutInflater.from(this).inflate(R.layout.view_ask_name, null);
            final EditText et = (EditText) textEntryView.findViewById(R.id.ask_name);
            et.setText(folder);

            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(
                    R.string.folder_name_title).setView(textEntryView).setPositiveButton(R.string.alert_dialog_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            String newFolder = et.getText().toString();
                            if (folder.length() != 0) {
                                YbkDAO.getInstance(Main.this).renameFolder(folder, newFolder);
                                refreshBookList();
                            }
                        }
                    }).create().show();
        }
        return true;
    }

    private Book getContextMenuBook(MenuItem menuitem) {
        Object item = getContextMenuItem(menuitem);
        return item instanceof Book ? (Book) item : null;
    }

    private Object getContextMenuItem(MenuItem menuitem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuitem.getMenuInfo();
        return getListView().getItemAtPosition(menuInfo.position);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        try {
            super.onCreateContextMenu(menu, v, menuInfo);
            Object contextItem = getListAdapter().getItem(((AdapterContextMenuInfo) menuInfo).position);
            if (contextItem instanceof Book) {
                menu.add(0, OPEN_ID, 0, R.string.menu_open_ebook);
                menu.add(0, MOVE_TO_FOLDER_ID, 0, R.string.menu_move_to_folder);
                menu.add(0, DELETE_ID, 0, R.string.menu_delete_ebook);
                menu.add(0, PROPERTIES_ID, 0, R.string.menu_ebook_properties);
            } else if (contextItem instanceof String) {
                String folder = (String) contextItem;
                menu.add(0, OPEN_ID, 0, R.string.menu_open_folder);
                if (!folder.equals(getResources().getString(R.string.all_books_folder))
                        && !folder.equals(getResources().getString(R.string.top_level_folder))) {
                    menu.add(0, DELETE_ID, 0, R.string.menu_delete_folder);
                    menu.add(0, RENAME_ID, 0, R.string.menu_rename_folder);
                }
            }
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
            setContentView(R.layout.main);
            // To capture LONG_PRESS gestures
            registerForContextMenu(getListView());

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
                startActivityForResult(new Intent(this, BookmarkDialog.class).putExtra("fromMain", true),
                        YbkViewActivity.CALL_BOOKMARK);
                return true;

            case OPEN_ID:
                return onOpenMenuItem(item);

            case MOVE_TO_FOLDER_ID:
                startActivityForResult(new Intent(this, MoveDialog.class).putExtra("currentFolder", mCurrentFolder)
                        .putExtra("fileName", getContextMenuBook(item).fileName), MOVE_TO_FOLDER_ID);
                return true;

            case DELETE_ID:
                return onDeleteMenuItem(item);

            case PROPERTIES_ID:
                return onEBookProperties(item);

            case BOOK_WALKER_ID:
                walkBook(0);
                return true;
            case RENAME_ID:
                return onRenameMenuItem(item);
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
            openItem(listView.getItemAtPosition(selectionRowId));
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

        try {
            if (resultCode != RESULT_OK) {
                return;
            }

            Bundle extras = data.getExtras();

            switch (requestCode) {
            case YbkViewActivity.CALL_HISTORY:
            case YbkViewActivity.CALL_BOOKMARK:
                setProgressBarIndeterminateVisibility(true);

                if (extras.getBoolean(BookmarkDialog.DELETE_BOOKMARK)) {
                    DeleteBookmarkDialog.create(this, YbkDAO.getInstance(this).getBookmark(
                            extras.getInt(YbkDAO.BOOKMARK_NUMBER)));
                } else if (extras.getLong(YbkDAO.HISTORY_ID) != 0) {
                    startActivity(new Intent(this, YbkViewActivity.class).putExtra(YbkDAO.HISTORY_ID, extras
                            .getLong(YbkDAO.HISTORY_ID)));
                }

                break;
            case YbkViewActivity.SHOW_BOOK:
                if (extras != null) {
                    String folder = extras.getString(FOLDER);
                    if (!mCurrentFolder.equals(folder)) {
                        mCurrentFolder = folder;
                        refreshBookList();
                    }
                }

            case ACTIVITY_SETTINGS:
                if (mThemeId != Util.getTheme(getSharedPrefs())) {

                    // (Notes: The following is based on both empirical evidence and what I've been able to find in the
                    // developer forums. In Android 1.0, using Acitivy.setTheme() would reset all the theme elements. In
                    // each subsequent version if, fewer and fewer theme elements changes actually take effect unless
                    // the theme is set before the initial call to onCreate(). In Android 2.0 and beyond, some of color
                    // changes that we make when switching to/from the night mode theme don't happen properly. The
                    // result is that after switching themes dynamically, we are left with an unreadable display. The
                    // only way to fully reset the theme is to restart the activity.

                    final Intent intent = new Intent(this, ReloadMainActivity.class);
                    if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
                        // for reasons unknown, possibly related to the version of Android, the ReloadMainActivity
                        // doesn't seem to be found by some of our users. So if the activity can't be found, try to do
                        // it the old way and hope that those who are having this problem are those with an older
                        // version of Android where dynamic setting of the theme actually works.
                        mThemeId = Util.getTheme(getSharedPrefs());
                        setTheme(mThemeId);
                        Log.w(TAG, "The ReloadMainActivity is not found.  We cannot change the theme that way. "
                                + "Trying the old way");
                    } else {
                        startActivity(intent);
                        finish();
                        return;
                    }
                }

                if (extras != null && extras.getBoolean(Settings.EBOOK_DIR_CHANGED)) {

                    YbkDAO.getInstance(this).open(this);
                    String eBookDir = getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                            Settings.DEFAULT_EBOOK_DIRECTORY);
                    
                    refreshLibrary(eBookDir, ADD_BOOKS);
                }
                refreshBookList();
                break;

            case MOVE_TO_FOLDER_ID:
                final String fileName = extras.getString("fileName");

                if (extras.getBoolean(MoveDialog.ADD_FOLDER)) {
                    LayoutInflater factory = LayoutInflater.from(this);
                    final View textEntryView = factory.inflate(R.layout.view_ask_name, null);
                    final EditText et = (EditText) textEntryView.findViewById(R.id.ask_name);

                    new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(
                            R.string.folder_name_title).setView(textEntryView).setPositiveButton(
                            R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    String folder = et.getText().toString();
                                    if (folder.length() != 0) {
                                        YbkDAO.getInstance(Main.this).moveBookToFolder(folder, fileName);
                                        refreshBookList();
                                    }
                                }
                            }).create().show();
                } else {
                    String folder = extras.getString(MoveDialog.MOVE_TO_FOLDER);
                    if (folder != null && fileName != null) {
                        if (folder.length() == 0) {
                            folder = null;
                        }
                        YbkDAO.getInstance(this).moveBookToFolder(folder, fileName);
                        refreshBookList();
                    }
                }
                break;

            case WALK_BOOK:
                if (data.getIntExtra(BOOK_WALK_INDEX, -1) != -1) {
                    walkBook(data.getIntExtra(BOOK_WALK_INDEX, -1) + 1);
                }

                break;
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
                Util.deleteFiles(new File(libDir, ".images"), ".*");
                Util.deleteFiles(new File(libDir, ".thumbnails"), ".*");
                Util.deleteFiles(libDir, ".*\\.(tmp|lg|db)");
                Util.deleteFiles(new File(libDir, "data"), "books\\.dat|.*\\.chp");
                if (!libDir.getAbsoluteFile().toString().equalsIgnoreCase(Settings.DEFAULT_EBOOK_DIRECTORY)) {
                    // cleanup default library directory if it wasn't the one we
                    // were using
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY, ".images"), ".*");
                    Util.deleteFiles(new File(Settings.DEFAULT_EBOOK_DIRECTORY, ".thumbnails"), ".*");
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mCurrentFolder", mCurrentFolder);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCurrentFolder.length() > 0) {
                mCurrentFolder = "";
                refreshBookList();
                return true;
            }
        }
        return super.onKeyDown(keyCode, msg);
    }

}
