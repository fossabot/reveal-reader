package com.jackcholt.reveal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.Chapter;
import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

public class YbkViewActivity extends Activity implements OnGestureListener {
    private DisplayChapter mCurrChap = new DisplayChapter();
    private YbkFileReader mYbkReader;
    private String mDialogFilename = "Never set";
    private String mChapBtnText = null;
    private String mHistTitle = "";
    private String strFontSize = "";
    private boolean mBackButtonPressed = false;
    private String mDialogChapter;
    private boolean mBookWalk = false;
    private int mBookWalkIndex = -1;
    private String mOrigChapName;
    private Handler mHandler = new Handler();

    private static final String TAG = "reveal.YbkViewActivity";
    private static final int FILE_NONEXIST = 1;
    private static final int INVALID_CHAPTER = 2;
    private static final int ASK_BOOKMARK_NAME = 3;
    private static final int CHAPTER_NONEXIST = 4;
    private static final int PREVIOUS_ID = R.id.menu_item_previous;
    private static final int NEXT_ID = R.id.menu_item_next;
    private static final int HISTORY_ID = R.id.menu_item_history;
    private static final int BOOKMARK_ID = R.id.menu_item_bookmark;
    private static final int SETTINGS_ID = R.id.menu_item_settings;
    private static final int BROWSER_ID = R.id.menu_item_download;
    private static final int HELP_ID = R.id.menu_item_help;
    private static final int ABOUT_ID = R.id.menu_item_about;
    private static final int DONATE_ID = R.id.menu_item_donate;
    private static final int LICENSE_ID = R.id.menu_item_license;
    private static final int REVELUPDATE_ID = R.id.menu_item_update;
    private static final int NOTE_BROWSER_ID = R.id.menu_item_note_browser;

    public static final int CALL_HISTORY = 1;
    public static final int CALL_BOOKMARK = 2;
    public static final int CALL_VERSE_CONTEXT_MENU = 3;
    public static final int CALL_NOTE_EDITED = 4;
    public static final int SHOW_BOOK = 5;

    private GestureDetector mGestureScanner = new GestureDetector(this);

    private int mThemeId = -1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            initDisplayFeatures();

            setProgressBarIndeterminateVisibility(true);

            String content = null;
            String strUrl = null;

            @SuppressWarnings("unchecked")
            HashMap<String, Comparable> statusMap = (HashMap<String, Comparable>) getLastNonConfigurationInstance();
            if (statusMap != null) {
                mCurrChap.setBookFileName((String) statusMap.get("bookFileName"));
                mCurrChap.setChapFileName((String) statusMap.get("chapFileName"));
                mHistTitle = (String) statusMap.get("histTitle");
                mCurrChap.setScrollYPos((Integer) statusMap.get("scrollYPos"));
                Log.d(TAG, "Scroll Position Y: " + mCurrChap.getScrollYPos());

                if (savedInstanceState != null) {
                    content = (String) savedInstanceState.get("content");
                    strUrl = (String) savedInstanceState.getString("strUrl");
                }
            } else { // statusMap == null
                long historyId = 0;
                if (savedInstanceState != null) {
                    mCurrChap.setBookFileName((String) savedInstanceState.get("bookFileName"));
                } else {
                    Bundle extras = getIntent().getExtras();
                    if (extras != null) {
                        historyId = extras.getLong(YbkDAO.HISTORY_ID);
                        mCurrChap.setBookFileName(extras.getString(YbkDAO.FILENAME));
                        mCurrChap.setChapFileName(extras.getString(YbkDAO.CHAPTER_FILENAME));
                        mCurrChap.setFragment(extras.getString(YbkDAO.VERSE));
                        content = (String) extras.get("content");
                        strUrl = (String) extras.getString("strUrl");
                        mBookWalk = extras.get(Main.BOOK_WALK_INDEX) != null;
                    }
                }
                if (historyId != 0) {
                    History hist = YbkDAO.getInstance(this).getHistory(historyId);
                    mCurrChap.setBookFileName(hist.bookFileName);
                    mCurrChap.setChapFileName(hist.chapterName);
                    mCurrChap.setScrollYPos(hist.scrollYPos);
                    mHistTitle = hist.title;
                }
            }
            if (null == mCurrChap.getBookFileName()) {
                Toast.makeText(this, R.string.book_not_loaded, Toast.LENGTH_LONG).show();
                Log.e(TAG, "In onCreate(): Book not loaded");
                finish();
                return;
            }

            Log.d(TAG, "BookFileName: " + mCurrChap.getBookFileName());

            // check online for updated thumbnail
            Util.thumbOnlineUpdate(mCurrChap.getBookFileName().replaceAll(".ybk$", ""));

            setContentView();
            configWebView();
            checkAndSetFontSize(getSharedPrefs(), findWebView());

            if ((isPopup())) {
                findWebView().loadDataWithBaseURL(strUrl, content, "text/html", "utf-8", "");
            } else {
                setupBreadcrumbButtons();
            }

            try {
                mYbkReader = YbkFileReader.getReader(this, mCurrChap.getBookFileName());
                Book book = mYbkReader.getBook();
                if (book == null) {
                    mYbkReader.unuse();
                    mYbkReader = null;
                    throw new FileNotFoundException(mCurrChap.getBookFileName());
                }

                if (null == mCurrChap.getChapFileName()) {
                    if (mBookWalk) {
                        mBookWalkIndex = -1;
                        Chapter firstChapter = getNextBookWalkerChapter();
                        if (firstChapter == null) {
                            setResult(
                                    RESULT_OK,
                                    new Intent().putExtra(Main.BOOK_WALK_INDEX,
                                            getIntent().getExtras().getInt(Main.BOOK_WALK_INDEX, -1)));
                            finish();
                        } else {
                            mCurrChap.setChapFileName(firstChapter.fileName);
                        }
                    } else {
                        mCurrChap.setChapFileName("\\" + book.shortTitle + ".html");
                    }
                }

                if (!isPopup() && loadChapter(mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), true)) {
                    initFolderBookChapButtons(book.shortTitle, mCurrChap.getBookFileName(), mCurrChap.getChapFileName());
                }

            } catch (IOException ioe) {
                Log.e(TAG,
                        "Could not load: " + mCurrChap.getBookFileName() + " chapter: " + mCurrChap.getChapFileName()
                                + ". " + ioe.getMessage());
                Toast.makeText(this, "Could not load: " + mCurrChap.getBookFileName() + " chapter: " //
                        + mCurrChap.getChapFileName() + ". Please report this at " + //
                        getResources().getText(R.string.website), Toast.LENGTH_LONG).show();
            }
            setWebViewClient();

            setProgressBarIndeterminateVisibility(false);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    private void setupBreadcrumbButtons() {
        findChapterButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                // set the chapter button so it scrolls the window to the top
                findWebView().scrollTo(0, 0);
            }
        });

        findMainButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                YbkDAO.getInstance(getBaseContext()).insertHistory(mCurrChap.getBookFileName(), mChapBtnText,
                        mCurrChap.getChapFileName(), findWebView().getScrollY());
                setResult(RESULT_OK, new Intent(getBaseContext(), Main.class).putExtra(Main.FOLDER, ""));
                finish();
            }
        });
        findFolderButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View view) {
                YbkDAO.getInstance(getBaseContext()).insertHistory(mCurrChap.getBookFileName(), mChapBtnText,
                        mCurrChap.getChapFileName(), findWebView().getScrollY());
                setResult(
                        RESULT_OK,
                        new Intent(getBaseContext(), Main.class).putExtra(Main.FOLDER,
                                YbkDAO.getInstance(getBaseContext()).getBookFolder(mCurrChap.getBookFileName())));
                finish();
            }
        });
    }

    private void configWebView() {
        findWebView().getSettings().setJavaScriptEnabled(true);
        findWebView().addJavascriptInterface(this, "App");

        if (getSharedPrefs().getBoolean("show_zoom", false)) {
            findWebView().getSettings().setBuiltInZoomControls(true);
        } else {
            findWebView().getSettings().setBuiltInZoomControls(false);
        }
    }

    private WebView findWebView() {
        return (WebView) findViewById(R.id.ybkView);
    }

    private ImageButton findMainButton() {
        return (ImageButton) findViewById(R.id.mainMenu);
    }

    private ImageButton findFolderButton() {
        return (ImageButton) findViewById(R.id.folderButton);
    }

    private Button findChapterButton() {
        return (Button) findViewById(R.id.chapterButton);
    }

    private Button findBookButton() {
        return (Button) findViewById(R.id.bookButton);
    }

    /**
     * Overridden in YbkPopupActivity to load the proper layout for it.
     */
    protected void setContentView() {
        setContentView(R.layout.view_ybk);
    }

    /**
     * This is overridden in YbkPopupActivity to return true;
     */
    protected boolean isPopup() {
        return false;
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private Chapter getNextBookWalkerChapter() {
        Chapter nextChapter = null;
        if (mYbkReader != null) {
            do {
                nextChapter = mYbkReader.getChapterByIndex(++mBookWalkIndex);
            } while (nextChapter != null && !nextChapter.fileName.matches("(?i).*\\.html(\\.gz)?"));

        }
        return nextChapter;
    }

    private void checkAndSetFontSize(SharedPreferences sharedPref, final WebView ybkView) {
        // Check and set Fontsize
        int fontSize = ybkView.getSettings().getDefaultFontSize();
        strFontSize = getSharedPrefs().getString(Settings.EBOOK_FONT_SIZE_KEY, Settings.DEFAULT_EBOOK_FONT_SIZE);
        fontSize = Integer.parseInt(strFontSize);

        ybkView.getSettings().setDefaultFontSize(fontSize);
        ybkView.getSettings().setDefaultFixedFontSize(fontSize);
    }

    private void initDisplayFeatures() {
        if (getSharedPrefs().getBoolean("show_fullscreen", false)) {
            getWindow()
                    .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        if (isPopup()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        if (!requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
            Log.w(TAG, "Progress bar is not supported");
        }
        if (!isPopup()) {
            Util.setTheme(getSharedPrefs(), this);
        }
    }

    /**
     * Handle unexpected error.
     * 
     * @param throwable
     */
    private void unexpectedError(Throwable throwable) {
        finish();
        Util.unexpectedError(this, throwable, "book: " + mCurrChap.getBookFileName(),
                "chapter: " + mCurrChap.getChapFileName());
    }

    @Override
    protected void onStart() {
        try {
            super.onStart();

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
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /**
     * Encapsulate the logic of setting the WebViewClient.
     * 
     * @param view The WebView for which we're setting the WebViewClient.
     */
    private void setWebViewClient() {
        findWebView().setWebViewClient(new YbkWebViewClient());
    }

    /**
     * Set the folder, book, and chapter buttons.
     * 
     * @param shortTitle The text to be used on the Book Button.
     * @param filePath The path to the YBK file that contains the chapter to load.
     * @param fileToOpen The internal path to the chapter to load.
     */
    private void initFolderBookChapButtons(final String shortTitle, final String filePath, final String fileToOpen) {
        initBookButton(shortTitle, filePath, fileToOpen);
        initChapterButton();
        initFolderButton();
    }

    private void initFolderButton() {
        if (null == findFolderButton()) {
            return;
        }
        findFolderButton().setVisibility(
                YbkDAO.getInstance(this).getBookFolder(mCurrChap.getBookFileName()).length() != 0 ? View.VISIBLE
                        : View.GONE);
    }

    private void initBookButton(final String shortTitle, final String filePath, final String fileToOpen) {
        if (null == findBookButton()) {
            return;
        }
        if (shortTitle != null) {
            findBookButton().setText(shortTitle);
        }
        findBookButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                setProgressBarIndeterminateVisibility(true);
                try {
                    mCurrChap.setScrollYPos(0);
                    if (loadChapter(filePath, "index", false)) {
                        initFolderBookChapButtons(shortTitle, filePath, fileToOpen);
                    }
                } catch (IOException ioe) {
                    Log.w(TAG, "Could not load index page of " + filePath);
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        });
        findBookButton().setVisibility(View.VISIBLE);
    }

    private void initChapterButton() {
        if (null == findChapterButton()) {
            return;
        }
        findChapterButton().setText(mChapBtnText);
        findChapterButton().setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        boolean showMenu = true;
        try {
            super.onCreateOptionsMenu(menu);

            getMenuInflater().inflate(R.menu.menu_main, menu);
            findExtraSubMenu(menu).clearHeader();

        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return showMenu;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        try {
            findExtraSubMenu(menu).findItem(R.id.menu_item_reset).setVisible(false);
            findExtraSubMenu(menu).findItem(R.id.menu_item_refresh_lib).setVisible(false);
            menu.findItem(SETTINGS_ID).setVisible(false);

            if (mCurrChap.getChapOrderNbr() < 1) {
                menu.findItem(PREVIOUS_ID).setVisible(false).setEnabled(false);
                menu.findItem(NEXT_ID).setVisible(false).setEnabled(false);
                menu.findItem(BROWSER_ID).setVisible(true);
                menu.findItem(NOTE_BROWSER_ID).setVisible(true);
                findExtraSubMenu(menu).findItem(BROWSER_ID).setVisible(false);
                findExtraSubMenu(menu).findItem(NOTE_BROWSER_ID).setVisible(false);
                return super.onPrepareOptionsMenu(menu);
            }

            menu.findItem(PREVIOUS_ID).setVisible(isArrowsVisible()).setEnabled(hasPreviousChapter());
            menu.findItem(NEXT_ID).setVisible(isArrowsVisible()).setEnabled(hasNextChapter());

            if (isArrowsVisible()) {
                menu.findItem(BROWSER_ID).setVisible(false);
                menu.findItem(NOTE_BROWSER_ID).setVisible(false);
                findExtraSubMenu(menu).findItem(BROWSER_ID).setVisible(true);
                findExtraSubMenu(menu).findItem(NOTE_BROWSER_ID).setVisible(true);
            }
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private SubMenu findExtraSubMenu(final Menu menu) {
        return menu.findItem(R.id.menu_extra).getSubMenu();
    }

    private boolean isArrowsVisible() {
        return hasNextChapter() || hasPreviousChapter();
    }

    private boolean hasPreviousChapter() {
        return mCurrChap.getChapOrderNbr() > 1;
    }

    private boolean hasNextChapter() {
        Chapter chap = mYbkReader.getChapterByOrder(mCurrChap.getChapOrderNbr() + 1);
        return chap != null && !chap.fileName.toLowerCase().contains("binding.htm");
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        try {
            switch (item.getItemId()) {
            case PREVIOUS_ID:
                if (mCurrChap.getChapOrderNbr() > 0) {
                    loadAdjacentChapter(-1);
                }
                return true;
            case NEXT_ID:
                if (mCurrChap.getChapOrderNbr() != -1) {
                    loadAdjacentChapter(1);
                }
                return true;

            case HISTORY_ID:
                setProgressBarIndeterminateVisibility(true);
                startActivityForResult(new Intent(this, HistoryDialog.class), CALL_HISTORY);
                setProgressBarIndeterminateVisibility(false);
                return true;

            case BOOKMARK_ID:
                setProgressBarIndeterminateVisibility(true);
                startActivityForResult(new Intent(this, BookmarkDialog.class), CALL_BOOKMARK);
                setProgressBarIndeterminateVisibility(false);
                return true;

            case NOTE_BROWSER_ID:
                startActivityForResult(new Intent(this, NotesListActivity.class), NOTE_BROWSER_ID);
                return true;

            case SETTINGS_ID:
                startActivityForResult(new Intent(this, Settings.class), Main.ACTIVITY_SETTINGS);
                return true;

            case BROWSER_ID:
                startActivity(new Intent(this, TitleBrowser.class));
                return true;

            case REVELUPDATE_ID:
                Toast.makeText(this, R.string.checking_for_new_version_online, Toast.LENGTH_SHORT).show();
                UpdateChecker.checkForNewerVersion(this, Global.SVN_VERSION);
                return true;

            case ABOUT_ID:
                AboutDialog.create(this);
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

            }
            return super.onMenuItemSelected(featureId, item);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {

        Bundle extras;

        try {
            YbkDAO ybkDao = YbkDAO.getInstance(this);

            if (resultCode != RESULT_OK) {
                super.onActivityResult(requestCode, resultCode, intent);
            }

            switch (requestCode) {
            case CALL_HISTORY:
                setProgressBarIndeterminateVisibility(true);

                extras = intent.getExtras();
                History hist = ybkDao.getHistory(extras.getLong(YbkDAO.HISTORY_ID));

                if (hist != null) {
                    mCurrChap.setBookFileName(hist.bookFileName);
                    Book book = ybkDao.getBook(mCurrChap.getBookFileName());
                    if (book != null) {
                        mCurrChap.setBookFileName(book.fileName);
                    }

                    mCurrChap.setChapFileName(hist.chapterName);
                    mHistTitle = hist.title;
                    mCurrChap.setScrollYPos(hist.scrollYPos);
                    mCurrChap.setNavFile("1");

                    Log.d(TAG, "Loading chapter from history file: " + mCurrChap.getBookFileName() + " chapter: "
                            + mCurrChap.getChapFileName());

                    try {
                        if (loadChapter(mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), true)) {
                            initFolderBookChapButtons(book.shortTitle, mCurrChap.getBookFileName(),
                                    mCurrChap.getChapFileName());

                        }
                    } catch (IOException ioe) {
                        Log.e(TAG, "Couldn't load chapter from history. " + ioe.getMessage());
                    }
                } else {
                    Log.e(TAG, "Couldn't load chapter from history. ");
                }

                setProgressBarIndeterminateVisibility(false);

                return;

            case CALL_BOOKMARK:
                extras = intent.getExtras();

                boolean addBookMark = extras.getBoolean(BookmarkDialog.ADD_BOOKMARK);
                boolean updateBookmark = extras.getBoolean(BookmarkDialog.UPDATE_BOOKMARK);
                boolean deleteBookmark = extras.getBoolean(BookmarkDialog.DELETE_BOOKMARK);

                if (addBookMark) {
                    showDialog(ASK_BOOKMARK_NAME);
                } else if (updateBookmark) {
                    int bmId = extras.getInt(YbkDAO.BOOKMARK_NUMBER);
                    // update the bookmark
                    ybkDao.updateBookmark(bmId, mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), findWebView()
                            .getScrollY());
                } else if (deleteBookmark) {
                    DeleteBookmarkDialog.create(this, ybkDao.getBookmark(extras.getInt(YbkDAO.BOOKMARK_NUMBER)));
                } else {
                    // go to bookmark
                    setProgressBarIndeterminateVisibility(true);
                    History bm = ybkDao.getHistory(extras.getLong(YbkDAO.HISTORY_ID));

                    if (bm != null) {
                        Book book = ybkDao.getBook(bm.bookFileName);
                        if (book != null) {
                            mCurrChap.setBookFileName(book.fileName);
                        }
                        mCurrChap.setChapFileName(bm.chapterName);
                        mHistTitle = bm.title;
                        mCurrChap.setScrollYPos(bm.scrollYPos);
                        mCurrChap.setNavFile("1");

                        Log.d(TAG, "Loading chapter from bookmark file: " + mCurrChap.getBookFileName() + " chapter: "
                                + mCurrChap.getChapFileName());

                        try {
                            if (loadChapter(mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), true)) {

                                initFolderBookChapButtons(book.shortTitle, mCurrChap.getBookFileName(),
                                        mCurrChap.getChapFileName());

                                findWebView().scrollTo(0, mCurrChap.getScrollYPos());
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Couldn't load chapter from bookmarks. " + ioe.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Couldn't load chapter from bookmarks");
                    }

                    setProgressBarIndeterminateVisibility(false);
                }

                return;

            case CALL_VERSE_CONTEXT_MENU:
                switch (intent.getIntExtra(VerseContextDialog.MENU_ITEM_TAG, -1)) {
                case VerseContextDialog.ANNOTATE_ID:
                    Log.d(TAG, "starting annotation/highlighting");
                    startNoteEditForResult(intent);
                    break;

                case VerseContextDialog.GOTO_TOP_ID:
                    findWebView().scrollTo(0, 0);
                    break;

                default:
                    Log.e(TAG, "Unsupported verse context menu option: " //
                            + intent.getIntExtra(VerseContextDialog.MENU_ITEM_TAG, -1));
                }
                break;

            case CALL_NOTE_EDITED:
                if (null == intent) {
                    break;
                }
                
                YbkDAO.getInstance(this).insertAnnotHilite(intent.getStringExtra(YbkDAO.NOTE),
                        intent.getIntExtra(YbkDAO.COLOR, AnnotationDialog.NO_HILITE),
                        intent.getIntExtra(YbkDAO.VERSE, -1), intent.getStringExtra(YbkDAO.BOOK_FILENAME),
                        intent.getStringExtra(YbkDAO.CHAPTER_FILENAME));

                try {
                    setProgressBarIndeterminateVisibility(true);
                    mCurrChap.setScrollYPos(findWebView().getScrollY());
                    loadChapter(mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), false, true);
                    findWebView().scrollTo(0, mCurrChap.getScrollYPos());
                } catch (IOException ioe) {
                    throw new IllegalStateException("Couldn't reload chapter", ioe);
                } finally {
                    setProgressBarIndeterminateVisibility(false);
                }
                break;

            case NOTE_BROWSER_ID:

                if (null == mCurrChap) {
                    Log.e(TAG, "The current chapter info is null when trying to jump to the note reference.");
                    break;
                }
                mCurrChap.setBookFileName(intent.getStringExtra(YbkDAO.FILENAME));
                mCurrChap.setChapFileName(intent.getStringExtra(YbkDAO.CHAPTER_FILENAME));
                mCurrChap.setFragment(intent.getStringExtra(YbkDAO.VERSE));
                mCurrChap.setNavFile("1");

                try {
                    if (loadChapter(mCurrChap.getBookFileName(), mCurrChap.getChapFileName(), true)) {
                        initFolderBookChapButtons(ybkDao.getBook(mCurrChap.getBookFileName()).shortTitle,
                                mCurrChap.getBookFileName(), mCurrChap.getChapFileName());
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Couldn't load chapter from annotation. " + ioe.getMessage());
                }
                break;

            case Main.ACTIVITY_SETTINGS:
                activatePreferenceChanges(intent.getExtras());
                break;
            }

            super.onActivityResult(requestCode, resultCode, intent);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    private void activatePreferenceChanges(Bundle extras) {
        if (extras != null && extras.getBoolean(Settings.EBOOK_DIR_CHANGED)) {
            YbkDAO.getInstance(this).open(this);
            Main.getMainApplication().refreshLibrary(
                    getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY));
        }
        Main.getMainApplication().refreshBookList();

        if (mThemeId == Util.getTheme(getSharedPrefs())) {
            return;
        }
        /*
         * (Notes: The following is based on both empirical evidence and what I've been able to find in the developer
         * forums. In Android 1.0, using Acitivy.setTheme() would reset all the theme elements. In each subsequent
         * version if, fewer and fewer theme elements changes actually take effect unless the theme is set before the
         * initial call to onCreate(). In Android 2.0 and beyond, some of color changes that we make when switching
         * to/from the night mode theme don't happen properly. The result is that after switching themes dynamically, we
         * are left with an unreadable display. The only way to fully reset the theme is to restart the activity.
         */
        final Intent intent = new Intent(this, ReloadYbkViewActivity.class);

        if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            /*
             * For reasons unknown, possibly related to the version of Android, the ReloadYbkActivity doesn't seem to be
             * found by some of our users. So if the activity can't be found, try to do it the old way and hope that
             * those who are having this problem are those with an older version of Android where dynamic setting of the
             * theme actually works.
             */
            mThemeId = Util.getTheme(getSharedPrefs());
            setTheme(mThemeId);
            Log.w(TAG, "The ReloadYbkViewActivity is not found.  We cannot change the theme that way. Trying the old "
                    + "way");
            return;

        }

        startActivity(intent.putExtra(YbkDAO.FILENAME, mCurrChap.getBookFileName()));
        finish();
    }

    @Override
    public void setTheme(int resid) {
        // bug workaround alert: see http://code.google.com/p/android/issues/detail?id=4394
        mThemeId = Util.getTheme(getSharedPrefs());
        super.setTheme(mThemeId);
    }

    /**
     * Load a chapter as identified by the the order id.
     * 
     * @param
     * @param orderId The order id of the chapter to load.
     * @return Did the chapter load?
     * @throws IOException If there was a problem reading the chapter.
     */
    private boolean loadChapterByOrderId(final String bookFileName, final int orderId) throws IOException {

        Chapter chap = mYbkReader.getChapterByOrder(orderId);

        if (chap == null) {
            Toast.makeText(this, R.string.no_swipe_available, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No chapters found for order id: " + orderId);
            return false;
        }

        Book book = mYbkReader.getBook();
        mCurrChap.setNavFile("1");
        if (null != book.fileName && null != chap.fileName
                && loadChapter(book.fileName, removeGZipExt(chap.fileName), true)) {
            initFolderBookChapButtons(book.shortTitle, book.fileName, chap.fileName);
            return true;
        }
        return false;
    }

    private String removeGZipExt(String chapFileName) {
        if (null == chapFileName) {
            throw new IllegalArgumentException("Chapter filename is null");
        }
        return (chapFileName.toLowerCase().endsWith(".gz")) ? chapFileName.substring(0, chapFileName.length() - 3)
                : chapFileName;
    }

    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the WebView.
     * 
     * @param filePath The path to the YBK file from which to read the chapter.
     * @param chapter The "filename" of the chapter to load.
     * @param saveToBackStack Should the current chapter be saved to the back stack?
     * @throws IOException
     */
    /* package */boolean loadChapter(String filePath, final String chapter, boolean saveToBackStack) throws IOException {
        return loadChapter(filePath, chapter, saveToBackStack, false);
    }

    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the WebView.
     * 
     * @param filePath The path to the YBK file from which to read the chapter.
     * @param chapter The "filename" of the chapter to load.
     * @param saveToBackStack Should the current chapter be saved to the back stack?
     * @param reloading Are we using loadChapter to re-load a chapter?
     * @throws IOException
     */
    private boolean loadChapter(String filePath, final String chapter, boolean saveToBackStack, boolean reloading)
            throws IOException {

        if (null == mYbkReader) {
            return false;
        }

        YbkFileReader ybkReader = mYbkReader;

        if (needToSaveBookChapterToHistory(chapter)) {
            // Save the book and chapter to history if there is one
            YbkDAO.getInstance(this).insertHistory(mCurrChap.getBookFileName(), mChapBtnText,
                    mCurrChap.getChapFileName(), findWebView().getScrollY());
            if (saveToBackStack) {
                YbkDAO.getInstance(this).addToBackStack(mCurrChap.getBookFileName(), mChapBtnText,
                        mCurrChap.getChapFileName(), findWebView().getScrollY());
            }
        }

        // check the format of the internal file name
        if (!chapter.equals("index") && chapter.toLowerCase().indexOf(".html") == -1) {
            // showDialog(INVALID_CHAPTER);
            Log.e(TAG, "The chapter is invalid: " + chapter);
            return false;
        }

        // get rid of any urlencoded spaces
        filePath = filePath.replace("%20", " ");
        String chap = chapter.replace("%20", " ");

        String content = "";
        // mCurrChap.setFragment(null);

        File testFile = new File(getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY), filePath);
        if (!testFile.exists()) {
            // set the member property that holds the name of the book file we couldn't find
            mDialogFilename = (TextUtils.isEmpty(filePath)) ? "No file" : testFile.getName();
            showDialog(FILE_NONEXIST);
        }

        // Only create a new YbkFileReader if we're opening a different book
        if (isOpeningNewBook(filePath, ybkReader)) {
            ybkReader.unuse();
            ybkReader = mYbkReader = YbkFileReader.getReader(this, filePath);
        }

        try {
            if (chap.equals("index")) {
                String tryFileToOpen = "\\" + ybkReader.getBook().shortTitle + ".html.gz";
                content = ybkReader.readInternalFile(tryFileToOpen);
                if (content == null) {
                    tryFileToOpen = "\\" + ybkReader.getBook().shortTitle + ".html";
                    content = ybkReader.readInternalFile(tryFileToOpen);
                }

                if (content == null) {
                    findWebView().loadData("YBK file has no index page.", "text/plain", "utf-8");

                    Log.e(TAG, "YBK file has no index page.");
                } else {
                    chap = tryFileToOpen;
                }
            } else {

                int hashLoc = -1;

                hashLoc = chap.indexOf("#");
                if (hashLoc + 1 == chap.length()) {
                    // if # is the last character get rid of it.
                    hashLoc = -1;
                    chap = Util.independentSubstring(chap, 0, chap.length() - 1);
                }

                // use the dreaded break <label> in order to simplify conditional nesting
                label_get_content: if (hashLoc != -1) {
                    mCurrChap.setFragment(Util.independentSubstring(chap, hashLoc + 1));
                    if (mCurrChap.getFragment().indexOf(".") != -1) {
                        mCurrChap.setFragment(Util.independentSubstring(mCurrChap.getFragment(), 0, mCurrChap
                                .getFragment().indexOf(".")));
                    }
                    if (!mYbkReader.chapterExists(chap.substring(0, hashLoc))) {
                        // need to read a special footnote chapter
                        content = readConcatFile(chap, mYbkReader);

                        if (content != null) {
                            break label_get_content;
                        }
                    } else {
                        chap = Util.independentSubstring(chap, 0, hashLoc);
                        content = mYbkReader.readInternalFile(chap);
                        if (content != null) {
                            break label_get_content;
                        }

                        content = mYbkReader.readInternalFile(chap + ".gz");
                        if (content != null) {
                            break label_get_content;
                        }
                    }
                } else {
                    content = mYbkReader.readInternalFile(chap);
                    if (content != null) {
                        break label_get_content;
                    }

                    // Need to read special concatenated file
                    content = readConcatFile(chap, mYbkReader);
                    if (content != null) {
                        break label_get_content;
                    }

                    // if we haven't reached a break statement yet, we have a problem.
                    Toast.makeText(this, "Could not read chapter '" + chap + "'", Toast.LENGTH_LONG);
                    findWebView().loadData(getResources().getString(R.string.error_unloadable_chapter), "text/plain",
                            "utf-8");
                    return false;
                } // label_get_content:
            }

            Chapter chapObj = ybkReader.getChapter(chap);
            if (chapObj == null) {
                String concatChap = chap.substring(0, chap.lastIndexOf("\\")) + "_.html.gz";
                chapObj = ybkReader.getChapter(concatChap);
            }

            content = fixSmartQuotes(content);

            int posEnd = content.toLowerCase().indexOf("<end>");

            String nf = (posEnd != -1) ? parseNavFile(content, posEnd) : "1";

            if (!(isShowInPopup(chapter))) {
                mHistTitle = mChapBtnText;
                setChapBtnText(content);
            }

            content = Util.processIfbook(content, this);
            content = convertAhtags(content);
            content = Util.convertIfvar(content);
            // Inside htmlize we now have support for night mode
            content = Util.htmlize(content, getSharedPrefs(), nf);
            content = Util.annotHiliteContent(content,
                    YbkDAO.getInstance(this).getChapterAnnotHilites(ybkReader.getBook().fileName, chap), this);

            if (!reloading && isShowInPopup(chapter)) {
                Log.d(TAG, "Showing chapter in popup");
                showChapterInPopup(content, ybkReader.getBook(), Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book")
                        .toString());
                return true;
            }

            findWebView().loadDataWithBaseURL(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString(),
                    content, "text/html", "utf-8", "");
            mCurrChap.setChapOrderNbr((null == chapObj) ? -1 : chapObj.orderNumber);
            mCurrChap.setNavFile(nf);
            mCurrChap.setBookFileName(ybkReader.getBook().fileName);
            mCurrChap.setChapFileName(chap);

        } catch (IOException e) {
            findWebView().loadData(getResources().getString(R.string.error_unloadable_chapter), "text/plain", "utf-8");

            Log.e(TAG, chap + " in " + filePath + " could not be opened. " + e.getMessage());
            return false;
        }

        return true;
    }

    private boolean isOpeningNewBook(String filePath, YbkFileReader ybkReader) {
        return null == ybkReader.getFilename() || !ybkReader.getFilename().equalsIgnoreCase(filePath);
    }

    private String parseNavFile(String content, int posEnd) {
        String header = content.substring(0, posEnd);
        Log.d(TAG, "Chapter header: " + header);

        int nfLoc = header.toLowerCase().indexOf("<nf>");
        int nfEndLoc = header.length();
        if (nfLoc != -1 && -1 != (nfEndLoc = header.indexOf('<', nfLoc + 4))) {
            return Util.independentSubstring(header, nfLoc + 4, nfEndLoc);
        }
        return "1";
    }

    private void showChapterInPopup(String content, Book book, String strUrl) {
        setProgressBarIndeterminateVisibility(true);
        Intent popupIntent = new Intent(this, YbkPopupActivity.class).putExtra("content", content)
                .putExtra("strUrl", strUrl).putExtra(YbkDAO.FILENAME, book.fileName);
        startActivity(popupIntent);
        setProgressBarIndeterminateVisibility(false);
    }

    private String convertAhtags(String content) {
        return content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>", "<span class=\"ah\" id=\"ah$1\">$2</span>");
    }

    private String fixSmartQuotes(String content) {
        if (null == content) {
            return "";
        }
        return content.replace('\u0093', '"').replace('\u0094', '"');
    }

    private boolean needToSaveBookChapterToHistory(final String chapter) {
        return !(isShowInPopup(chapter)) && !isPopup() && mChapBtnText != null && mCurrChap.getChapFileName() != null;
    }

    private boolean isShowInPopup(final String chapter) {
        return ((!mBackButtonPressed && currentChapIsNotNavFile() && !isPopup() && !chapter.equals("index")) || forceShowInPopup())
                && !mBookWalk;
    }

    private boolean forceShowInPopup() {
        if (null == mOrigChapName || mOrigChapName.length() < 1)
            return false;
        return mOrigChapName.charAt(0) == '^';
    }

    private boolean currentChapIsNotNavFile() {
        return mCurrChap.getNavFile().equals("0");
    }

    /**
     * Used to configure any dialog boxes created by this Activity
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        try {
            switch (id) {
            case FILE_NONEXIST:

                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Not Set")
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so do some stuff */
                            }
                        }).create();

            case CHAPTER_NONEXIST:

                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Not Set")
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so do some stuff */
                            }
                        }).create();

            case INVALID_CHAPTER:

                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Not Set")
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                /* User clicked OK so do some stuff */
                            }
                        }).create();

            case ASK_BOOKMARK_NAME:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.view_ask_name, null);
                final EditText et = (EditText) textEntryView.findViewById(R.id.ask_name);

                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle("Enter Bookmark Name").setView(textEntryView)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                try {
                                    String bmName = (String) et.getText().toString();

                                    YbkDAO ybkDao = YbkDAO.getInstance(getBaseContext());
                                    int bookmarkNumber = ybkDao.getMaxBookmarkNumber();

                                    // insert the bookmark
                                    ybkDao.insertBookmark(mCurrChap.getBookFileName(), bmName,
                                            mCurrChap.getChapFileName(), findWebView().getScrollY(), bookmarkNumber);

                                } catch (IOException ioe) {
                                    // TODO - add a friendly message
                                    Util.displayError(YbkViewActivity.this, ioe,
                                            getResources().getString(R.string.error_bookmark_save));
                                }
                            }
                        }).create();

            }
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }

        return null;

    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        try {
            switch (id) {
            case FILE_NONEXIST:
                dialog.setTitle(MessageFormat.format(getResources().getString(R.string.reference_not_found),
                        mDialogFilename));
                break;
            case CHAPTER_NONEXIST:
                dialog.setTitle(MessageFormat.format(getResources().getString(R.string.chapter_not_found),
                        mDialogChapter));
                break;
            case INVALID_CHAPTER:
                dialog.setTitle(MessageFormat.format(getResources().getString(R.string.invalid_chapter),
                        mDialogFilename));
                break;
            }
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    /**
     * Read a section of a special concatenated chapter;
     * 
     * @param chap The chapter to read.
     * @param ybkReader The YbkReader to use in order to access the chapter.
     * @return The content of the section.
     * @throws IOException If the Ybk file cannot be read.
     */
    private String readConcatFile(final String chap, final YbkFileReader ybkReader) throws IOException {
        // need to read a special footnote chapter
        if (chap.lastIndexOf("\\") == -1) {
            throw new IllegalStateException("The chapter does not contain a backslash");
        }

        String concatChap = chap.substring(0, chap.lastIndexOf("\\")) + "_.html.gz";

        String endString = (chap.contains(".html")) ? ".html" : ".";

        String verse = chap.substring(chap.lastIndexOf("\\") + 1, chap.lastIndexOf(endString));

        Log.d(TAG, "verse/concatChap: " + verse + "/" + concatChap);

        String content = ybkReader.readInternalFile(concatChap);

        if (content == null) {
            Log.e(TAG, "Couldn't find a concatenated chapter for: " + chap);
            return null;
        }

        content = content.substring(content.indexOf('\002' + verse + '\002') + verse.length() + 2);

        if (content.indexOf('\002') != -1) {
            content = content.substring(0, content.indexOf('\002'));
        }

        return content;
    }

    /**
     * Set the chapter button text from the content.
     * 
     * @param content The content of the chapter.
     */
    private void setChapBtnText(final String content) {
        String fullName = extractFullNameFromContent(content);
        mChapBtnText = new String((fullName.length() == 0 || fullName.length() == content.length()) ? getResources()
                .getString(R.string.unknown) : fullName);
    }

    private String extractFullNameFromContent(final String content) {
        return content.replaceAll("(?is)^.*<fn>(.*)<nf>.*$", "$1");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        try {
            if (keyCode != KeyEvent.KEYCODE_BACK) {
                return super.onKeyDown(keyCode, msg);
            }

            if (isPopup()) {
                finish();
                return super.onKeyDown(keyCode, msg);
            }

            setProgressBarIndeterminateVisibility(true);

            while (true) {
                History hist = YbkDAO.getInstance(this).popBackStack();

                if (hist == null) {
                    Log.d(TAG, "backStack is empty. Going to main menu.");
                    finish();
                    break;
                }

                Book book = YbkDAO.getInstance(this).getBook(hist.bookFileName);
                if (book == null) {
                    Log.e(TAG, "Major error.  There was a history in the back stack for which no "
                            + "book could be found");
                    continue;
                }
                mCurrChap.setScrollYPos(hist.scrollYPos);

                Log.d(TAG, "Going back to: " + hist.bookFileName + ", " + hist.chapterName);

                mBackButtonPressed = true;
                try {
                    if (loadChapter(hist.bookFileName, hist.chapterName, false)) {
                        initFolderBookChapButtons(book.shortTitle, hist.bookFileName, hist.chapterName);
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not return to the previous page " + ioe.getMessage());
                    continue;
                }
                mBackButtonPressed = false;
                break;
            }
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return true;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        try {
            HashMap<String, Comparable<?>> stateMap = new HashMap<String, Comparable<?>>();

            stateMap.put("bookFileName", mCurrChap.getBookFileName());
            stateMap.put("chapFileName", mCurrChap.getChapFileName());
            stateMap.put("histTitle", mHistTitle);
            WebView wv = findWebView();
            if (null == wv) {
                stateMap.put("scrollYPos", 0);
            } else {
                stateMap.put("scrollYPos", findWebView().getScrollY());
                Log.d(TAG, "Scroll Y Pos: " + findWebView().getScrollY());
            }

            return stateMap;
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        try {
            outState.putString(YbkDAO.FILENAME, mCurrChap.getBookFileName());
            super.onSaveInstanceState(outState);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (isFinishing() && !isPopup()) {
                YbkDAO.getInstance(this).clearBackStack();
            }
            if (mYbkReader != null) {
                mYbkReader.unuse();
                mYbkReader = null;
            }
            super.onDestroy();
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    private final class YbkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            try {
                int contentUriLength = YbkProvider.CONTENT_URI.toString().length();
                final boolean HANDLED_BY_HOST_APP = true;
                final boolean HANDLED_BY_WEBVIEW = false;

                int sdkVersion = 2;
                try {
                    sdkVersion = Integer.parseInt(Build.VERSION.SDK);
                } catch (NumberFormatException nfe) {
                    // do nothing. Just use the defaulted value.
                }

                if (sdkVersion > 2 && url.contains("book#")) {
                    // this is needed for internal links on SDK >= 1.5
                    return HANDLED_BY_WEBVIEW;
                }

                if (isProtocolRemote(url)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toLowerCase())));
                    } catch (ActivityNotFoundException anfe) {
                        Log.w(TAG, "Could not find the activity to handle: " + url);
                    }
                    return HANDLED_BY_HOST_APP;
                }

                if (url.length() <= contentUriLength + 1) {
                    return HANDLED_BY_HOST_APP;
                }

                Log.d(TAG, "WebView URL: " + url);
                String book;
                String chapter = "";
                String shortTitle = null;

                if (url.indexOf('@') != -1) {
                    if (getEmailPattern().matcher(url).matches()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + url)));
                    } else {
                        // pop up a context menu
                        try {
                            int verse = obtainVerse(url);
                            if (verse > 0) {
                                startActivityForResult(
                                        new Intent(view.getContext(), VerseContextDialog.class)
                                                .putExtra(YbkDAO.VERSE, verse)
                                                .putExtra(YbkDAO.BOOK_FILENAME, getBookFileName())
                                                .putExtra(YbkDAO.CHAPTER_FILENAME, mCurrChap.getChapFileName()),
                                        CALL_VERSE_CONTEXT_MENU);
                            }
                        } catch (NumberFormatException nfe) {
                            Toast.makeText(getBaseContext(), getText(R.string.cannot_find_url), Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                    return HANDLED_BY_HOST_APP;
                }

                String dataString;
                try {
                    dataString = URLDecoder.decode(url.substring(contentUriLength + 1), "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    dataString = url.substring(contentUriLength + 1);
                } catch (IllegalArgumentException iae) {
                    dataString = url.substring(contentUriLength + 1);
                }

                String httpString = dataString;

                if (!httpString.toLowerCase().startsWith("http://")) {
                    httpString = "http://" + httpString;
                }

                if (getUrlPattern().matcher(httpString).matches()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(httpString)));
                    return HANDLED_BY_HOST_APP;
                }

                String[] urlParts = dataString.split("/");

                // keep original chapter name around so we can check for leading characters later
                mOrigChapName = urlParts[0];
                // get rid of the book indicator since it is only used in some cases.
                book = shortTitle = mOrigChapName;
                if (book.charAt(0) == '!' || book.charAt(0) == '^') {
                    shortTitle = urlParts[0] = book.substring(1);
                }

                book = urlParts[0] + ".ybk";

                for (int i = 0; i < urlParts.length; i++) {
                    chapter += "\\" + urlParts[i];
                }

                YbkFileReader ybkRdr = null;
                try {
                    setProgressBarIndeterminateVisibility(true);
                    ybkRdr = YbkFileReader.getReader(YbkViewActivity.this, book);
                    if (null != ybkRdr.getBook()) {

                        boolean bookLoaded = false;
                        if (chapterExists(chapter, ybkRdr, removeChapterFragment(chapter))) {
                            bookLoaded = loadChapter(book, chapter, true);
                        } else {
                            mDialogChapter = removeChapterFragment(chapter).substring(chapter.lastIndexOf("\\") + 1);
                            showDialog(CHAPTER_NONEXIST);
                        }

                        if (bookLoaded) {
                            initFolderBookChapButtons(shortTitle, book, chapter);
                            mCurrChap.setScrollYPos(0);
                        } else {
                            mDialogChapter = chapter.substring(chapter.lastIndexOf("\\") + 1);
                            showDialog(CHAPTER_NONEXIST);
                        }
                    } else {
                        mDialogFilename = book;
                        showDialog(FILE_NONEXIST);
                    }
                } catch (IOException ioe) {
                    mDialogFilename = book;
                    showDialog(FILE_NONEXIST);
                } finally {
                    if (ybkRdr != null) {
                        ybkRdr.unuse();
                        ybkRdr = null;
                    }
                    setProgressBarIndeterminateVisibility(false);
                }

                return HANDLED_BY_HOST_APP;
            } catch (RuntimeException rte) {
                unexpectedError(rte);
                return false;
            } catch (Error e) {
                unexpectedError(e);
                return false;
            }
        }

        private int obtainVerse(final String url) {
            String[] verseUrlParts = url.split("@");
            if (verseUrlParts.length < 2) {
                return 0;
            }
            String verseInfo = verseUrlParts[1];
            return Integer.valueOf(verseInfo.split(",")[0]);
        }

        private String removeChapterFragment(String chapter) {
            return (chapter.contains("#")) ? chapter.substring(0, chapter.indexOf("#")) : chapter;
        }

        private Pattern getUrlPattern() {
            return Pattern.compile("^http://[A-Z0-9.-]+\\.[A-Z]{2,4}.+", Pattern.CASE_INSENSITIVE);
        }

        private Pattern getEmailPattern() {
            return Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
        }

        private boolean isProtocolRemote(final String url) {
            String urlLower = url.toLowerCase();
            return urlLower.startsWith("mailto:") || urlLower.startsWith("geo:") || urlLower.startsWith("tel:")
                    || urlLower.startsWith("http://") || urlLower.startsWith("https://");
        }

        private boolean chapterExists(String chapter, YbkFileReader ybkRdr, String chap) throws IOException {
            return ybkRdr.chapterExists(chap)
                    || ybkRdr.chapterExists(chapter.substring(0, chap.lastIndexOf("\\")) + "_.html.gz");
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            /*
             * try { Log.d(TAG, "fragment " + (null == mCurrChap.getFragment() ? "nil" : mCurrChap.getFragment())); //
             * make it jump to the internal link if (mCurrChap.getFragment() != null) {
             * view.loadUrl("javascript:location.href=\"#" + mCurrChap.getFragment() + "\"");
             * mCurrChap.setFragment(null); } else if (url.indexOf('@') != -1) { view.scrollTo(0, 0); } else if
             * (mCurrChap.getScrollYPos() != 0) { view.scrollTo(0, mCurrChap.getScrollYPos()); }
             * 
             * setProgressBarIndeterminateVisibility(false); if (mBookWalk) { mHandler.postDelayed(new ChapterWalker(),
             * 100); } } catch (RuntimeException rte) { unexpectedError(rte); } catch (Error e) { unexpectedError(e); }
             */

            // In Android 2.0 we can't scroll immediately because rendering hasn't happened yet.
            // We can get an onNewPicture message when rendering is complete, but we also get
            // those messages when there is a picture on the page (before this message), so we have to install a
            // listener here, and have it uninstall itself when completed.
            findWebView().setPictureListener(new PictureListener() {
                public void onNewPicture(WebView view, Picture picture) {
                    try {
                        // make it jump to the internal link
                        if (mCurrChap.getFragment() != null) {
                            view.loadUrl("javascript:location.href=\"#" + mCurrChap.getFragment() + "\"");
                            mCurrChap.setFragment(null);
                        } else if (url.indexOf('@') != -1) {
                            view.scrollTo(0, 0);
                        } else if (mCurrChap.getScrollYPos() != 0) {
                            view.scrollTo(0, mCurrChap.getScrollYPos());
                        }

                        setProgressBarIndeterminateVisibility(false);
                        if (mBookWalk) {
                            mHandler.postDelayed(new ChapterWalker(), 100);
                        }
                    } catch (RuntimeException rte) {
                        unexpectedError(rte);
                    } catch (Error e) {
                        unexpectedError(e);
                    } finally {
                        findWebView().setPictureListener(null);
                    }
                }
            });
        }
    }

    private class ChapterWalker extends SafeRunnable {

        @Override
        public void protectedRun() {
            try {
                Chapter nextChapter = getNextBookWalkerChapter();
                if (nextChapter == null) {
                    setResult(
                            RESULT_OK,
                            new Intent().putExtra(Main.BOOK_WALK_INDEX,
                                    getIntent().getExtras().getInt(Main.BOOK_WALK_INDEX, -1)));
                    finish();
                } else {
                    setProgressBarIndeterminateVisibility(true);
                    if (loadChapter(mCurrChap.getBookFileName(), nextChapter.fileName, true)) {
                        initFolderBookChapButtons(mYbkReader.getBook().shortTitle, mCurrChap.getBookFileName(),
                                nextChapter.fileName);
                    }
                }

            } catch (IOException e) {
                setProgressBarIndeterminateVisibility(false);
                Util.unexpectedError(YbkViewActivity.this, e);
            }
        }
    }

    // Use Swipes to change chapters
    // DKP

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        return mGestureScanner.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return mGestureScanner.onTouchEvent(me);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        if (mCurrChap.getChapOrderNbr() <= 0 || Math.abs(velocityX) <= Math.abs(velocityY)) {
            return false;
        }

        final int velocityThreshold = 1500;
        if (velocityX <= -velocityThreshold && hasNextChapter()) {
            loadAdjacentChapter(1);
        } else if (velocityX >= velocityThreshold && hasPreviousChapter()) {
            loadAdjacentChapter(-1);
        }
        return false;
    }

    private void loadAdjacentChapter(int chapterOffset) {
        setProgressBarIndeterminateVisibility(true);

        try {
            mCurrChap.setScrollYPos(0);
            loadChapterByOrderId(mCurrChap.getBookFileName(), mCurrChap.getChapOrderNbr() + chapterOffset);
        } catch (IOException ioe) {
            Log.e(TAG, "Could not move to the next chapter. " + ioe.getMessage());
        }
        setProgressBarIndeterminateVisibility(false);
    }

    public void onLongPress(MotionEvent e) {
        WebView wv = findWebView();
        wv.scrollTo(0, wv.getScrollY() + 10);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    public String getBookFileName() {
        return mCurrChap.getBookFileName();
    }

    private void startNoteEditForResult(final Intent intent) {
        startActivityForResult(
                new Intent(getBaseContext(), AnnotationDialog.class)
                        .putExtra(YbkDAO.VERSE, intent.getIntExtra(YbkDAO.VERSE, -1))
                        .putExtra(YbkDAO.CHAPTER_FILENAME, intent.getStringExtra(YbkDAO.CHAPTER_FILENAME))
                        .putExtra(YbkDAO.BOOK_FILENAME, intent.getStringExtra(YbkDAO.BOOK_FILENAME)), CALL_NOTE_EDITED);
    }
}
