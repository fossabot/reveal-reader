package com.jackcholt.reveal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.Chapter;
import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

public class YbkViewActivity extends Activity implements OnGestureListener {
    private WebView mYbkView;

    private String mBookFileName;
    private String mChapFileName;
    private int mScrollYPos = 0;
    private Button mBookBtn;
    private Button mChapBtn;
    private YbkFileReader mYbkReader;
    @SuppressWarnings("unused")
    private boolean mShowPictures;
    private boolean mShowFullScreen;
    private String mFragment;
    private String mDialogFilename = "Never set";
    private String mChapBtnText = null;
    private String mHistTitle = "";
    private String strFontSize = "";
    private int mChapOrderNbr = 0;
    private boolean mBackButtonPressed = false;
    private String mDialogChapter;
    private String mNavFile = "1";
    private boolean mThemeIsDialog = false;
    private boolean mBookWalk = false;
    private int mBookWalkIndex = -1;
    private String mOrigChapName;
    private Handler mHandler = new Handler();

    private static final String TAG = "YbkViewActivity";
    private static final int FILE_NONEXIST = 1;
    private static final int INVALID_CHAPTER = 2;
    private static final int ASK_BOOKMARK_NAME = 3;
    private static final int CHAPTER_NONEXIST = 4;
    private static final int PREVIOUS_ID = Menu.FIRST;
    private static final int NEXT_ID = Menu.FIRST + 1;
    private static final int HISTORY_ID = Menu.FIRST + 2;
    private static final int BOOKMARK_ID = Menu.FIRST + 3;
    public static final int CALL_HISTORY = 1;
    public static final int CALL_BOOKMARK = 2;

    private GestureDetector gestureScanner;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            setProgressBarIndeterminateVisibility(true);

            Util.startFlurrySession(this);
            FlurryAgent.onEvent(TAG);

            gestureScanner = new GestureDetector(this);

            initDisplayFeatures();

            long historyId = 0;
            String content = null;
            String strUrl = null;

            HashMap<String, Comparable> statusMap = (HashMap<String, Comparable>) getLastNonConfigurationInstance();
            if (statusMap != null) {
                mBookFileName = (String) statusMap.get("bookFileName");
                mChapFileName = (String) statusMap.get("chapFileName");
                mHistTitle = (String) statusMap.get("histTitle");
                mScrollYPos = (Integer) statusMap.get("scrollYPos");
                Log.d(TAG, "Scroll Position Y: " + mScrollYPos);

                if (savedInstanceState != null) {
                    content = (String) savedInstanceState.get("content");
                    strUrl = (String) savedInstanceState.getString("strUrl");
                }

            } else {

                if (savedInstanceState != null) {
                    mBookFileName = (String) savedInstanceState.get("bookFileName");
                } else {
                    Bundle extras = getIntent().getExtras();
                    if (extras != null) {
                        historyId = extras.getLong(YbkDAO.HISTORY_ID);
                        mBookFileName = extras.getString(YbkDAO.FILENAME);
                        content = (String) extras.get("content");
                        strUrl = (String) extras.getString("strUrl");
                        mBookWalk = extras.get(Main.BOOK_WALK_INDEX) != null;
                    }
                }

                if (historyId != 0) {
                    History hist = YbkDAO.getInstance(this).getHistory(historyId);
                    mBookFileName = hist.bookFileName;
                    mChapFileName = hist.chapterName;
                    mHistTitle = hist.title;
                }
            }

            if (mBookFileName == null) {
                Toast.makeText(this, R.string.book_not_loaded, Toast.LENGTH_LONG).show();
                Log.e(TAG, "In onCreate(): Book not loaded");
                finish();
                return;
            }

            Log.d(TAG, "BookFileName: " + mBookFileName);

            
            // check online for updated thumbnail
            Util.thumbOnlineUpdate(mBookFileName.replaceAll(".ybk$", ""));
            
            
            if ((this instanceof YbkPopupActivity)) {
                mThemeIsDialog = true;
                setContentView(R.layout.view_popup_ybk);

            } else {
                setContentView(R.layout.view_ybk);
            }

            final WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);
            ybkView.getSettings().setJavaScriptEnabled(true);
            ybkView.getSettings().setBuiltInZoomControls(true);

            checkAndSetFontSize(getSharedPrefs(), ybkView);

            if ((this instanceof YbkPopupActivity)) {
                Log.d(TAG, "strUrl: " + strUrl);
                Log.d(TAG, "content: " + content);
                ybkView.loadDataWithBaseURL(strUrl, content, "text/html", "utf-8", "");
            } else {

                final ImageButton mainBtn = (ImageButton) findViewById(R.id.mainMenu);
                mBookBtn = (Button) findViewById(R.id.bookButton);
                final Button chapBtn = mChapBtn = (Button) findViewById(R.id.chapterButton);
                chapBtn.setOnClickListener(new OnClickListener() {
                    /**
                     * set the chapter button so it scrolls the window to the top
                     */
                    public void onClick(final View v) {
                        mYbkView.scrollTo(0, 0);
                    }
                });

                mainBtn.setOnClickListener(new OnClickListener() {

                    public void onClick(final View view) {

                        YbkDAO ybkDao = YbkDAO.getInstance(getBaseContext());
                        if (!mBackButtonPressed && !mThemeIsDialog && mChapBtnText != null && mChapFileName != null) {
                            // Save the book and chapter to history if there is one
                            ybkDao.insertHistory(mBookFileName, mChapBtnText, mChapFileName, mYbkView.getScrollY());
                        }
                        finish();
                    }

                });
            }

            try {
                mYbkReader = YbkFileReader.getReader(this, mBookFileName);
                Book book = mYbkReader.getBook();
                if (book == null) {
                    mYbkReader.unuse();
                    mYbkReader = null;
                    throw new FileNotFoundException(mBookFileName);
                }

                String shortTitle = book.shortTitle;
                if (mChapFileName == null) {
                    if (mBookWalk) {
                        mBookWalkIndex = -1;
                        Chapter firstChapter = getNextBookWalkerChapter();
                        if (firstChapter == null) {
                            setResult(RESULT_OK, new Intent().putExtra(Main.BOOK_WALK_INDEX, getIntent().getExtras()
                                    .getInt(Main.BOOK_WALK_INDEX, -1)));
                            finish();
                        } else {
                            mChapFileName = firstChapter.fileName;
                        }
                    } else
                        mChapFileName = "\\" + shortTitle + ".html";
                }

                if (!(this instanceof YbkPopupActivity)) {
                    if (loadChapter(mBookFileName, mChapFileName)) {
                        setBookBtn(shortTitle, mBookFileName, mChapFileName);
                    }
                }

            } catch (IOException ioe) {
                Log.e(TAG, "Could not load: " + mBookFileName + " chapter: " + mChapFileName + ". " + ioe.getMessage());

                Toast.makeText(
                        this,
                        "Could not load : " + mBookFileName + " chapter: " + mChapFileName + ". Please report this at "
                                + getResources().getText(R.string.website), Toast.LENGTH_LONG).show();
            }
            setWebViewClient(ybkView);

            setProgressBarIndeterminateVisibility(false);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
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
        mShowPictures = getSharedPrefs().getBoolean("show_pictures", true);

        mShowFullScreen = getSharedPrefs().getBoolean("show_fullscreen", false);

        if (mShowFullScreen) {
            getWindow()
                    .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (!requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
            Log.w(TAG, "Progress bar is not supported");
        }
    }

    /**
     * Handle unexpected error.
     * 
     * @param t
     */
    private void unexpectedError(Throwable t) {
        finish();
        Util.unexpectedError(this, t, "book: " + mBookFileName, "chapter: " + mChapFileName);
    }

    @Override
    protected void onStart() {
        try {
            Util.startFlurrySession(this);
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
            FlurryAgent.onEndSession(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /**
     * Encapsulate the logic of setting the WebViewClient.
     * 
     * @param view
     *            The WebView for which we're setting the WebViewClient.
     */
    private void setWebViewClient(final WebView view) {
        view.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                try {
                    int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
                    final boolean HANDLED_BY_HOST_APP = true;
                    final boolean HANDLED_BY_WEBVIEW = false;
                    boolean urlHandler = HANDLED_BY_HOST_APP;
                    final Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$",
                            Pattern.CASE_INSENSITIVE);
                    final Pattern urlPattern = Pattern.compile("^http://[A-Z0-9.-]+\\.[A-Z]{2,4}.+",
                            Pattern.CASE_INSENSITIVE);

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

                    String lowerUrl = url.toLowerCase();
                    if (lowerUrl.startsWith("mailto:") || lowerUrl.startsWith("geo:") || lowerUrl.startsWith("tel:")
                            || lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } else if (url.length() > ContentUriLength + 1) {
                        setProgressBarIndeterminateVisibility(true);

                        Log.d(TAG, "WebView URL: " + url);
                        String book;
                        String chapter = "";
                        String shortTitle = null;

                        if (url.indexOf('@') != -1) {
                            Matcher emailMatcher = emailPattern.matcher(url);
                            if (emailMatcher.matches()) {
                                Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + url));
                                startActivity(emailIntent);
                            } else {
                                view.scrollTo(0, 0);
                            }
                        } else {

                            String dataString;
                            try {
                                dataString = URLDecoder.decode(url.substring(ContentUriLength + 1), "UTF-8");
                            } catch (UnsupportedEncodingException uee) {
                                dataString = url.substring(ContentUriLength + 1);
                            }

                            String httpString = dataString;

                            if (!httpString.toLowerCase().startsWith("http://")) {
                                httpString = "http://" + httpString;
                            }

                            Matcher urlMatcher = urlPattern.matcher(httpString);
                            if (urlMatcher.matches()) {
                                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(httpString));
                                startActivity(webIntent);
                                return urlHandler;
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
                                ybkRdr = YbkFileReader.getReader(YbkViewActivity.this, book);
                                Book bookObj = ybkRdr.getBook();

                                if (null != bookObj) {

                                    String chap = chapter;
                                    int pos;
                                    if ((pos = chapter.indexOf("#")) != -1) {
                                        chap = chapter.substring(0, pos);
                                    }
                                    boolean chapterExists = ybkRdr.chapterExists(chap)
                                            || ybkRdr.chapterExists(chapter.substring(0, chap.lastIndexOf("\\"))
                                                    + "_.html.gz");

                                    boolean bookLoaded = false;
                                    if (chapterExists) {
                                        bookLoaded = loadChapter(book, chapter);
                                    } else {
                                        mDialogChapter = chap.substring(chapter.lastIndexOf("\\") + 1);
                                        showDialog(CHAPTER_NONEXIST);
                                    }

                                    if (bookLoaded) {
                                        setBookBtn(shortTitle, book, chapter);
                                        mScrollYPos = 0;
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
                            }
                        }

                    }

                    return urlHandler;
                } catch (RuntimeException rte) {
                    unexpectedError(rte);
                    return false;
                } catch (Error e) {
                    unexpectedError(e);
                    return false;
                }
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                try {
                    // make it jump to the internal link
                    if (mFragment != null) {

                        view.loadUrl("javascript:location.href=\"#" + mFragment + "\"");

                        mFragment = null;
                    } else if (url.indexOf('@') != -1) {
                        view.scrollTo(0, 0);
                    } else if (mScrollYPos != 0) {
                        view.scrollTo(0, mScrollYPos);
                    }

                    Log.d(TAG, "Height of ybkView content: " + view.getContentHeight());

                    setProgressBarIndeterminateVisibility(false);
                    if (mBookWalk) {
                        mHandler.postDelayed(new ChapterWalker(), 100);
                    }
                } catch (RuntimeException rte) {
                    unexpectedError(rte);
                } catch (Error e) {
                    unexpectedError(e);
                }

            }
        });
    }

    /**
     * Set the book and chapter buttons.
     * 
     * @param shortTitle
     *            The text to be used on the Book Button.
     * @param filePath
     *            The path to the YBK file that contains the chapter to load.
     * @param fileToOpen
     *            The internal path to the chapter to load.
     */
    public void setBookBtn(final String shortTitle, final String filePath, final String fileToOpen) {
        Button bookBtn = mBookBtn;
        Button chapBtn = mChapBtn;

        if (bookBtn != null) {
            if (shortTitle != null) {
                bookBtn.setText(shortTitle);
            }

            bookBtn.setOnClickListener(new OnClickListener() {

                public void onClick(final View v) {
                    setProgressBarIndeterminateVisibility(true);

                    try {
                        if (loadChapter(filePath, "index")) {
                            setBookBtn(shortTitle, filePath, fileToOpen);
                        }
                    } catch (IOException ioe) {
                        Log.w(TAG, "Could not load index page of " + filePath);
                        setProgressBarIndeterminateVisibility(false);
                    }
                }

            });

            bookBtn.setVisibility(View.VISIBLE);
        }

        if (chapBtn != null) {
            /*
             * Checks to see if the title is too long for the button. This prevents the buttons becoming too large and
             * the view window being smaller. - Adam Gessel
             */

            if (mChapBtnText.length() > 20) {

                String mChapBtnTextSmall = mChapBtnText.substring(0, 20) + "...";
                chapBtn.setText(mChapBtnTextSmall);

            } else {

                chapBtn.setText(mChapBtnText);

            }

            chapBtn.setVisibility(View.VISIBLE);
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
            menu.add(Menu.NONE, PREVIOUS_ID, Menu.NONE, R.string.menu_previous).setIcon(R.drawable.previous_chapter);
            menu.add(Menu.NONE, NEXT_ID, Menu.NONE, R.string.menu_next).setIcon(R.drawable.next_chapter);

        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        try {
            super.onPrepareOptionsMenu(menu);

            MenuItem prevItem = menu.findItem(PREVIOUS_ID);
            MenuItem nextItem = menu.findItem(NEXT_ID);
            if (mChapOrderNbr < 1) {
                prevItem.setVisible(false);
                prevItem.setEnabled(false);
                nextItem.setVisible(false);
                nextItem.setEnabled(false);
            } else {
                boolean hasNext = false;
                boolean hasPrev = mChapOrderNbr > 1;
                hasNext = mYbkReader.getChapterByOrder(mChapOrderNbr + 1) != null;
                prevItem.setVisible(hasNext || hasPrev);
                prevItem.setEnabled(hasPrev);
                nextItem.setVisible(hasNext || hasPrev);
                nextItem.setEnabled(hasNext);
            }
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        try {
            switch (item.getItemId()) {
            case PREVIOUS_ID:
                if (mChapOrderNbr > 0) {
                    setProgressBarIndeterminateVisibility(true);
                    try {
                        loadChapterByOrderId(mBookFileName, mChapOrderNbr - 1);
                    } catch (IOException ioe) {
                        Log.e(TAG, "Could not move to the previous chapter. " + ioe.getMessage());
                    }
                    setProgressBarIndeterminateVisibility(false);
                }
                return true;
            case NEXT_ID:
                if (mChapOrderNbr != -1) {
                    setProgressBarIndeterminateVisibility(true);

                    try {
                        loadChapterByOrderId(mBookFileName, mChapOrderNbr + 1);
                    } catch (IOException ioe) {
                        Log.e(TAG, "Could not move to the next chapter. " + ioe.getMessage());
                    }

                    setProgressBarIndeterminateVisibility(false);
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
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        Bundle extras;
        long histId;

        try {
            YbkDAO ybkDao = YbkDAO.getInstance(this);

            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                case CALL_HISTORY:
                    setProgressBarIndeterminateVisibility(true);

                    extras = data.getExtras();
                    histId = extras.getLong(YbkDAO.HISTORY_ID);

                    History hist = ybkDao.getHistory(histId);

                    if (hist != null) {
                        mBookFileName = hist.bookFileName;
                        Book book = ybkDao.getBook(mBookFileName);
                        if (book != null) {
                            mBookFileName = book.fileName;
                        }

                        mChapFileName = hist.chapterName;
                        mHistTitle = hist.title;
                        mScrollYPos = hist.scrollYPos;
                        mNavFile = "1";

                        Log
                                .d(TAG, "Loading chapter from history file: " + mBookFileName + " chapter: "
                                        + mChapFileName);

                        try {
                            if (loadChapter(mBookFileName, mChapFileName)) {
                                setBookBtn(book.shortTitle, mBookFileName, mChapFileName);

                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Couldn't load chapter from history. " + ioe.getMessage());
                            FlurryAgent.onError("YbkViewActivity", "Couldn't load chapter from history", "WARNING");
                        }
                    } else {
                        Log.e(TAG, "Couldn't load chapter from history. ");
                        FlurryAgent.onError("YbkViewActivity", "Couldn't load chapter from history", "WARNING");
                    }

                    setProgressBarIndeterminateVisibility(false);

                    return;

                case CALL_BOOKMARK:
                    extras = data.getExtras();

                    boolean addBookMark = extras.getBoolean(BookmarkDialog.ADD_BOOKMARK);
                    boolean updateBookmark = extras.getBoolean(BookmarkDialog.UPDATE_BOOKMARK);
                    boolean deleteBookmark = extras.getBoolean(BookmarkDialog.DELETE_BOOKMARK);

                    if (addBookMark) {
                        showDialog(ASK_BOOKMARK_NAME);
                    } else if (updateBookmark) {
                        int bmId = extras.getInt(YbkDAO.BOOKMARK_NUMBER);
                        // update the bookmark
                        ybkDao.updateBookmark(bmId, mBookFileName, mChapFileName, mYbkView.getScrollY());
                    } else if (deleteBookmark) {
                        int bmId = extras.getInt(YbkDAO.BOOKMARK_NUMBER);
                        hist = ybkDao.getBookmark(bmId);
                        DeleteBookmarkDialog.create(this, hist);
                    } else {
                        // go to bookmark
                        setProgressBarIndeterminateVisibility(true);
                        histId = extras.getLong(YbkDAO.HISTORY_ID);

                        History bm = ybkDao.getHistory(histId);

                        if (bm != null) {
                            Book book = ybkDao.getBook(bm.bookFileName);
                            if (book != null) {
                                mBookFileName = book.fileName;
                            }
                            mChapFileName = bm.chapterName;
                            mHistTitle = bm.title;
                            mScrollYPos = bm.scrollYPos;
                            mNavFile = "1";

                            Log.d(TAG, "Loading chapter from bookmark file: " + mBookFileName + " chapter: "
                                    + mChapFileName);

                            try {
                                if (loadChapter(mBookFileName, mChapFileName)) {

                                    setBookBtn(book.shortTitle, mBookFileName, mChapFileName);

                                    mYbkView.scrollTo(0, mScrollYPos);
                                }
                            } catch (IOException ioe) {
                                Log.e(TAG, "Couldn't load chapter from bookmarks. " + ioe.getMessage());
                                FlurryAgent.onError("YbkViewActivity", "Couldn't load chapter from bookmarks",
                                        "WARNING");
                            }
                        } else {
                            Log.e(TAG, "Couldn't load chapter from bookmarks");
                            FlurryAgent.onError("YbkViewActivity", "Couldn't load chapter from bookmarks", "WARNING");
                        }

                        setProgressBarIndeterminateVisibility(false);

                    }

                    return;

                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        } catch (RuntimeException rte) {
            unexpectedError(rte);
        } catch (Error e) {
            unexpectedError(e);
        }
    }

    /**
     * Load a chapter as identified by the the order id.
     * 
     * @param
     * @param orderId
     *            The order id of the chapter to load.
     * @return Did the chapter load?
     * @throws IOException
     *             If there was a problem reading the chapter.
     */
    private boolean loadChapterByOrderId(final String bookFileName, final int orderId) throws IOException {

        boolean bookLoaded = false;

        Chapter chap = mYbkReader.getChapterByOrder(orderId);

        if (chap != null) {
            Book book = mYbkReader.getBook();
            mNavFile = "1";
            if (bookLoaded = loadChapter(book.fileName, chap.fileName)) {
                setBookBtn(book.shortTitle, book.fileName, chap.fileName);
            }

        } else {
            Toast.makeText(this, R.string.no_swipe_available, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No chapters found for order id: " + orderId);
        }

        return bookLoaded;

    }

    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the WebView.
     * 
     * @param filePath
     *            The path to the YBK file from which to read the chapter.
     * @param chapter
     *            The "filename" of the chapter to load.
     * @throws IOException
     */
    private boolean loadChapter(String filePath, final String chapter) throws IOException {
        boolean bookLoaded = false;
        WebView ybkView = mYbkView;
        YbkFileReader ybkReader = mYbkReader;

        if (needToSaveBookChapterToHistory(chapter)) {
            // Save the book and chapter to history if there is one
            YbkDAO.getInstance(this).insertHistory(mBookFileName, mChapBtnText, mChapFileName, mYbkView.getScrollY());

            if (mBackButtonPressed) {
                YbkDAO.getInstance(this).popBackStack();
            }
        }

        // check the format of the internal file name
        if (!chapter.equals("index") && chapter.toLowerCase().indexOf(".html") == -1) {
            showDialog(INVALID_CHAPTER);
            Log.e(TAG, "The chapter is invalid: " + chapter);
        }

        // get rid of any urlencoded spaces
        filePath = filePath.replace("%20", " ");
        String chap = chapter.replace("%20", " ");

        String content = "";
        String fragment = mFragment = null;

        File testFile = new File(getSharedPrefs().getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY), filePath);
        if (!testFile.exists()) {
            // set the member property that holds the name of the book file
            // we couldn't find
            if (TextUtils.isEmpty(filePath)) {
                mDialogFilename = "No file";
            } else {
                mDialogFilename = testFile.getName();
            }

            showDialog(FILE_NONEXIST);
        }

        // Only create a new YbkFileReader if we're opening a different book
        if (!ybkReader.getFilename().equalsIgnoreCase(filePath)) {
            ybkReader.unuse();
            ybkReader = mYbkReader = YbkFileReader.getReader(this, filePath);
        }

        Book book = ybkReader.getBook();

        try {
            if (chap.equals("index")) {
                String shortTitle = book.shortTitle;
                String tryFileToOpen = "\\" + shortTitle + ".html.gz";
                content = ybkReader.readInternalFile(tryFileToOpen);
                if (content == null) {
                    tryFileToOpen = "\\" + shortTitle + ".html";
                    content = ybkReader.readInternalFile(tryFileToOpen);
                }

                if (content == null) {
                    ybkView.loadData("YBK file has no index page.", "text/plain", "utf-8");

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
                    fragment = Util.independentSubstring(chap, hashLoc + 1);
                    if (fragment.indexOf(".") != -1) {
                        fragment = Util.independentSubstring(fragment, 0, fragment.indexOf("."));
                    }

                    mFragment = fragment;

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
                    ybkView
                            .loadData(getResources().getString(R.string.error_unloadable_chapter), "text/plain",
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

            String strUrl = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString();

            int posEnd = content.toLowerCase().indexOf("<end>");

            String nf = "1";
            if (!mBackButtonPressed && posEnd != -1) {
                String header = content.substring(0, posEnd);
                String headerLower = header.toLowerCase();

                Log.d(TAG, "Chapter header: " + header);

                int nfLoc = headerLower.indexOf("<nf>");
                int nfEndLoc = headerLower.length();
                if (nfLoc != -1) {
                    if (-1 != (nfEndLoc = headerLower.indexOf('<', nfLoc + 4))) {
                        nf = Util.independentSubstring(header, nfLoc + 4, nfEndLoc);
                    }
                }
            }

            if (!(isShowInPopup(chapter))) {
                mHistTitle = mChapBtnText;
                setChapBtnText(content);
            }

            content = Util.processIfbook(content, this);
            content = convertAhtags(content);
            content = Util.convertIfvar(content);
            content = Util.htmlize(content, getSharedPrefs());

            if ((isShowInPopup(chapter))) {
                showChapterInPopup(content, book, strUrl);
            } else {
                ybkView.loadDataWithBaseURL(strUrl, content, "text/html", "utf-8", "");
                if (chapObj != null) {
                    mChapOrderNbr = chapObj.orderNumber;
                } else {
                    mChapOrderNbr = -1;
                }
                mNavFile = nf;
                mBookFileName = book.fileName;
                mChapFileName = chap;
            }

            bookLoaded = true;

        } catch (IOException e) {
            ybkView.loadData(getResources().getString(R.string.error_unloadable_chapter), "text/plain", "utf-8");

            Log.e(TAG, chap + " in " + filePath + " could not be opened. " + e.getMessage());
            return false;
        }

        return bookLoaded;
    }

    private void showChapterInPopup(String content, Book book, String strUrl) {
        setProgressBarIndeterminateVisibility(true);
        Intent popupIntent = new Intent(this, YbkPopupActivity.class);
        popupIntent.putExtra("content", content);
        popupIntent.putExtra("strUrl", strUrl);
        popupIntent.putExtra(YbkDAO.FILENAME, book.fileName);
        startActivity(popupIntent);
        setProgressBarIndeterminateVisibility(false);
    }

    private String convertAhtags(String content) {
        return content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>", "<span class=\"ah\" id=\"ah$1\">$2</span>");
    }

    private String fixSmartQuotes(String content) {
        return content.replace('\u0093', '"').replace('\u0094', '"');
    }

    private boolean needToSaveBookChapterToHistory(final String chapter) {
        return !(isShowInPopup(chapter)) && !mThemeIsDialog && mChapBtnText != null && mChapFileName != null;
    }

    private boolean isShowInPopup(final String chapter) {
        return ((!mBackButtonPressed && currentChapIsNotNavFile() && !mThemeIsDialog && !chapter.equals("index")) || forceShowInPopup())
                && !mBookWalk;
    }

    private boolean forceShowInPopup() {
        if (null == mOrigChapName || mOrigChapName.length() < 1)
            return false;
        return mOrigChapName.charAt(0) == '^';
    }

    private boolean currentChapIsNotNavFile() {
        return mNavFile.equals("0");
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
                final View textEntryView = factory.inflate(R.layout.view_ask_bm, null);
                final EditText et = (EditText) textEntryView.findViewById(R.id.ask_bm_name);

                return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_info).setTitle(
                        "Enter Bookmark Name").setView(textEntryView).setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                try {
                                    String bmName = (String) et.getText().toString();

                                    // Log.i(TAG, "Text entered " + bmName);
                                    YbkDAO ybkDao = YbkDAO.getInstance(getBaseContext());
                                    int bookmarkNumber = ybkDao.getMaxBookmarkNumber();

                                    // insert the bookmark
                                    ybkDao.insertBookmark(mBookFileName, bmName, mChapFileName, mYbkView.getScrollY(),
                                            bookmarkNumber);

                                } catch (IOException ioe) {
                                    // TODO - add a friendly message
                                    Util.displayError(YbkViewActivity.this, ioe, getResources().getString(
                                            R.string.error_bookmark_save));
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
            String title;
            switch (id) {
            case FILE_NONEXIST:
                // replace the replaceable parameters
                title = getResources().getString(R.string.reference_not_found);
                title = MessageFormat.format(title, mDialogFilename);
                dialog.setTitle(title);
                break;
            case CHAPTER_NONEXIST:
                // replace the replaceable parameters
                title = getResources().getString(R.string.chapter_not_found);
                title = MessageFormat.format(title, mDialogChapter);
                dialog.setTitle(title);
                break;
            case INVALID_CHAPTER:
                // replace the replaceable parameters
                title = getResources().getString(R.string.invalid_chapter);
                title = MessageFormat.format(title, mDialogFilename);
                dialog.setTitle(title);
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
     * @param chap
     *            The chapter to read.
     * @param ybkReader
     *            The YbkReader to use in order to access the chapter.
     * @return The content of the section.
     * @throws IOException
     *             If the Ybk file cannot be read.
     */
    private String readConcatFile(final String chap, final YbkFileReader ybkReader) throws IOException {
        // need to read a special footnote chapter
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
     * @param content
     *            The content of the chapter.
     */
    private void setChapBtnText(final String content) {
        try {
            int endPos = content.indexOf("<end>");
            if (-1 == endPos) {
                throw new IllegalStateException("Chapter has no header");
            }
            String header = content.substring(0, endPos);
            int startFN = header.toLowerCase().indexOf("<fn>");
            if (-1 == startFN) {
                throw new IllegalStateException("Chapter has no full name");
            }

            // get past the <fn> tag
            startFN += 4;

            int endFN = header.substring(startFN).indexOf("<");
            if (-1 == endFN) {
                throw new IllegalStateException("full name does not end properly");
            }

            // Set endFN to the position in the header;
            endFN += startFN;

            String chapBtnText = header.substring(startFN, endFN);

            if ((chapBtnText.indexOf(":")) != -1) {
                String[] textParts = chapBtnText.split(":");
                chapBtnText = textParts[1].trim();
            }

            if (chapBtnText.length() > 30) {
                chapBtnText = chapBtnText.substring(0, 30);
            }
            // NOTE: Apparently in the in order to conserve memory, Android
            // implementation of String.substring() just points
            // to a offset and location within the original String. That is all
            // well and good, except that we keep a copy of the title
            // around in the history stack, which causes the whole internal
            // character array of the chapter to be referenced after we have
            // moved on from the chapter, so force making a copy of just the
            // string we want.
            mChapBtnText = new String(chapBtnText.toCharArray());
        } catch (IllegalStateException ise) {
            // does no on any good to percolate this exception, so log it, use a
            // default and move on
            Log.e(TAG, ise.toString());
            // try getting the first line
            String chapBtnText = "";
            int endPos = content.indexOf('\n');
            if (endPos > 0) {
                chapBtnText = Util.formatTitle(content.substring(0, endPos));
            }
            // if still nothing, use a default
            mChapBtnText = chapBtnText.length() > 0 ? chapBtnText : getResources().getString(R.string.unknown);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                setProgressBarIndeterminateVisibility(true);
                if (mThemeIsDialog) {
                    finish();
                } else {
                    YbkDAO ybkDao = YbkDAO.getInstance(this);
                    while (true) {
                        History hist = ybkDao.popBackStack();

                        if (hist == null) {
                            Log.d(TAG, "backStack is empty. Going to main menu.");
                            finish();
                        } else {
                            Book book = ybkDao.getBook(hist.bookFileName);
                            if (book == null) {
                                Log.e(TAG, "Major error.  There was a history in the back stack for which no "
                                        + "book could be found");
                                continue;
                            }
                            String bookFileName = book.fileName;
                            String chapFileName = hist.chapterName;
                            mScrollYPos = hist.scrollYPos;

                            Log.d(TAG, "Going back to: " + bookFileName + ", " + chapFileName);

                            mBackButtonPressed = true;
                            try {
                                if (loadChapter(bookFileName, chapFileName)) {

                                    setBookBtn(book.shortTitle, bookFileName, chapFileName);

                                }
                            } catch (IOException ioe) {
                                Log.e(TAG, "Could not return to the previous page " + ioe.getMessage());
                                continue;
                            }

                            mBackButtonPressed = false;

                        }
                        break;
                    }
                }

                return true;
            }

            return super.onKeyDown(keyCode, msg);
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

            stateMap.put("bookFileName", mBookFileName);
            stateMap.put("chapFileName", mChapFileName);
            stateMap.put("histTitle", mHistTitle);
            stateMap.put("scrollYPos", mYbkView.getScrollY());
            Log.d(TAG, "Scroll Y Pos: " + mYbkView.getScrollY());

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
            outState.putString(YbkDAO.FILENAME, mBookFileName);
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
            if (isFinishing() && !mThemeIsDialog) {
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

    private class ChapterWalker extends SafeRunnable {

        @Override
        public void protectedRun() {
            try {
                // YbkDAO ybkDao = YbkDAO.getInstance(YbkViewActivity.this);
                Chapter nextChapter = getNextBookWalkerChapter();
                if (nextChapter == null) {
                    setResult(RESULT_OK, new Intent().putExtra(Main.BOOK_WALK_INDEX, getIntent().getExtras().getInt(
                            Main.BOOK_WALK_INDEX, -1)));
                    finish();
                } else {
                    setProgressBarIndeterminateVisibility(true);
                    if (loadChapter(mBookFileName, nextChapter.fileName))
                        setBookBtn(mYbkReader.getBook().shortTitle, mBookFileName, nextChapter.fileName);
                    ;
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
        return gestureScanner.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return gestureScanner.onTouchEvent(me);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mChapOrderNbr > 0 && Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX <= -1500) {
                setProgressBarIndeterminateVisibility(true);

                try {
                    // Toast.makeText(this, R.string.menu_next,
                    // Toast.LENGTH_SHORT).show();
                    loadChapterByOrderId(mBookFileName, mChapOrderNbr + 1);
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not move to the next chapter. " + ioe.getMessage());
                }
                setProgressBarIndeterminateVisibility(false);
            }
            if (velocityX >= 1500) {
                setProgressBarIndeterminateVisibility(true);
                try {
                    // Toast.makeText(this, R.string.menu_previous,
                    // Toast.LENGTH_SHORT).show();
                    loadChapterByOrderId(mBookFileName, mChapOrderNbr - 1);
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not move to the previous chapter. " + ioe.getMessage());
                }
                setProgressBarIndeterminateVisibility(false);
            }
        }
        return false;
    }

    public void onLongPress(MotionEvent e) {
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
        return mBookFileName;
    }
}
