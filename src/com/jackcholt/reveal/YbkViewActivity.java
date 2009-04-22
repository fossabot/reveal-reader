package com.jackcholt.reveal;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private long mBookId;
    private String mBookFileName;
    private String mChapFileName;
    private int mScrollYPos = 0;
    private Button mBookBtn;
    private Button mChapBtn;
    private YbkFileReader mYbkReader;
    //private YbkDAO mYbkDao;
    //private String mLibraryDir;
    private SharedPreferences mSharedPref;
    @SuppressWarnings("unused")
    private boolean mShowPictures;
    private boolean BOOLshowFullScreen;
    private String mFragment;
    private String mDialogFilename = "Never set";
    private String mChapBtnText = "Not Set";
    private String mHistTitle = "";
    private int mChapOrderNbr = 0;
    private boolean mBackButtonPressed = false;
    private int mHistoryPos = 0;
    private static final String TAG = "YbkViewActivity";
    private static final int FILE_NONEXIST = 1;
    private static final int INVALID_CHAPTER = 2;
    private static final int ASK_BOOKMARK_NAME = 3;
    private static final int PREVIOUS_ID = Menu.FIRST;
    private static final int NEXT_ID = Menu.FIRST + 1;
    private static final int HISTORY_ID = Menu.FIRST + 2;
    private static final int BOOKMARK_ID = Menu.FIRST + 3;
    public static final int CALL_HISTORY = 1;
    public static final int CALL_BOOKMARK = 2;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

        FlurryAgent.onEvent("YbkViewActivity");

        SharedPreferences sharedPref = mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        
        mShowPictures = sharedPref.getBoolean("show_pictures", true);
        
        BOOLshowFullScreen = sharedPref.getBoolean("show_fullscreen", false);
        
        if (BOOLshowFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
         }
        requestWindowFeature(Window.FEATURE_NO_TITLE); 

        if (!requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
            Log.w(TAG, "Progress bar is not supported");
        }
        
        setProgressBarIndeterminateVisibility(true);

        YbkDAO ybkDao = YbkDAO.getInstance(this);

        Long bookId = null;
        Boolean isFromHistory = null;

        HashMap<String, Comparable> statusMap = (HashMap<String, Comparable>) getLastNonConfigurationInstance();
        if (statusMap != null) {
            mBookId = bookId = (Long) statusMap.get("bookId");
            mBookFileName = (String) statusMap.get("bookFileName");
            mChapFileName = (String) statusMap.get("chapFileName");
            mHistTitle = (String) statusMap.get("histTitle");
            mScrollYPos = (Integer) statusMap.get("scrollYPos");
        } else { 

            if (savedInstanceState != null) {
                bookId = (Long) savedInstanceState.get(YbkDAO.ID);            
                isFromHistory = (Boolean) savedInstanceState.get(YbkDAO.FROM_HISTORY);
            } else {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    isFromHistory = (Boolean) extras.get(YbkDAO.FROM_HISTORY);
                    bookId = (Long) extras.get(YbkDAO.ID);
                }
            }
            
            if (isFromHistory != null) {
                // bookId is actually the history id
                History hist = ybkDao.getHistory(bookId);
                bookId = hist.bookId;
                mBookFileName = ybkDao.getBook(bookId).fileName;
                mChapFileName = hist.chapterName;
                mHistTitle = hist.title;
            }
        }    
        
        if (bookId == null) {
            Toast.makeText(this, R.string.book_not_loaded, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBookId = bookId;
         
        setContentView(R.layout.view_ybk);

        final WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);  
        ybkView.getSettings().setJavaScriptEnabled(true);
        final ImageButton mainBtn = (ImageButton) findViewById(R.id.mainMenu);
        mBookBtn = (Button) findViewById(R.id.bookButton);
        final Button chapBtn = mChapBtn = (Button) findViewById(R.id.chapterButton);
        chapBtn.setOnClickListener(new OnClickListener() {
            /** set the chapter button so it scrolls the window to the top */
            public void onClick(final View v) {
                mYbkView.scrollTo(0,0);
            }
        });
        
        mainBtn.setOnClickListener(new OnClickListener() {

            public void onClick(final View view) {
                
                finish();
            }
            
        });
        
        Book book = ybkDao.getBook(bookId);
        
        mBookFileName = book.fileName;
        
        try {
            mYbkReader = new YbkFileReader(this,mBookFileName);
            String shortTitle = book.shortTitle;
            if (mChapFileName == null) {
                mChapFileName = "\\" + shortTitle + ".html";
            }
            
            if (loadChapter(mBookFileName, mChapFileName)) {
                setBookBtn(shortTitle, mBookFileName, mChapFileName);
            }

            
        } catch (IOException ioe) {
            Log.e(TAG, "Could not load: "  + mBookFileName + " chapter: " 
                    + mChapFileName + ". " + ioe.getMessage());
            
            Toast.makeText(this, "Could not load : "  + mBookFileName + " chapter: " 
                    + mChapFileName + ". Please report this at " 
                    + getResources().getText(R.string.website), Toast.LENGTH_LONG);
        }
               
        setWebViewClient(ybkView);

        setProgressBarIndeterminateVisibility(false);
        
    }
    
    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    /**
     * Encapsulate the logic of setting the WebViewClient.
     * @param view The WebView for which we're setting the WebViewClient.
     */
    private void setWebViewClient(final WebView view) {
        view.setWebViewClient(new WebViewClient() {
            
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
                
                boolean success = true;
                
                if (url.length() > ContentUriLength + 1) {
                    setProgressBarIndeterminateVisibility(true);
                
                    String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, 
                            Settings.DEFAULT_EBOOK_DIRECTORY);
                    
                    Log.d(TAG, "WebView URL: " + url);
                    String book;
                    String chapter = "";
                    String shortTitle = null;
                    
                    if (url.indexOf('@') != -1) {
                        view.scrollTo(0, 0);
                    } else {
                    
                        String dataString; 
                        try {
                            dataString = URLDecoder.decode(url.substring(ContentUriLength + 1), "UTF-8");
                        } catch (UnsupportedEncodingException uee) {
                            dataString = url.substring(ContentUriLength + 1);
                        }
                        
                        String[] urlParts = dataString.split("/");
                        
                        // get rid of the book indicator since it is only used in some cases.
                        book = shortTitle = urlParts[0];
                        if (book.charAt(0) == '!' || book.charAt(0) == '^') {
                            shortTitle = urlParts[0] = book.substring(1);
                        }
                        
                        book = libDir + urlParts[0] + ".ybk";
                        
                        for (int i = 0; i < urlParts.length; i++) {
                           chapter += "\\" + urlParts[i];
                        }
                        
                    
                        //Log.i(TAG, "Loading chapter '" + chapter + "'");
                        
                        try {
                            if (!chapter.toLowerCase().endsWith(".gz") 
                                    && loadChapter(book, chapter + ".gz")) {                    
                                setBookBtn(shortTitle,book,chapter);
                            } else {
                                if (loadChapter(book, chapter)) {
                                    setBookBtn(shortTitle,book,chapter);
                                } else {
                                    success = false;
                                }
                            }
                        } catch (IOException ioe) {
                            success = false;
                        }
                    }
                    
                } else {
                    success = false;
                }
                
                mScrollYPos = 0;
                
                return success;
            }
            
            @Override
            public void onPageFinished(final WebView view, final String url) {
                // make it jump to the internal link
                if (mFragment != null) {
                    //Log.d(TAG, "In onPageFinished(). Jumping to #" + mFragment);
                    view.loadUrl("javascript:location.href=\"#" + mFragment + "\"");
                    mFragment = null;
                } else if (url.indexOf('@') != -1) {
                    view.scrollTo(0,0);
                } else if (mScrollYPos != 0){
                    view.scrollTo(0,mScrollYPos);
                }
                
                setProgressBarIndeterminateVisibility(false);
                
            }
        });
    }
    /**
     * Set the book and chapter buttons.
     * 
     * @param shortTitle The text to be used on the Book Button.
     * @param filePath The path to the YBK file that contains the chapter to 
     * load. 
     * @param fileToOpen The internal path to the chapter to load. 
     */
    public void setBookBtn(final String shortTitle, final String filePath, 
            final String fileToOpen) {
        Button bookBtn = mBookBtn;
        Button chapBtn = mChapBtn;
        
        if (shortTitle != null) {
            bookBtn.setText(shortTitle);
        }
        
        bookBtn.setOnClickListener(new OnClickListener() {
            
            public void onClick(final View v) {
                setProgressBarIndeterminateVisibility(true);
                
                try {
                    if (loadChapter(filePath, "index") ) {
                        setBookBtn(shortTitle, filePath, fileToOpen);
                        //Log.d(TAG, "Book loaded");
                    } 
                } catch (IOException ioe) {
                    Log.w(TAG, "Could not load index page of " + filePath);
                    setProgressBarIndeterminateVisibility(false);
                }
            }
            
        });
        
        bookBtn.setVisibility(View.VISIBLE);
        
        /*  
            Checks to see if the title is too long for the button.
            This prevents the buttons becoming too large and the
            view window being smaller. - Adam Gessel 
         */
        
        if ( mChapBtnText.length() > 20 ) {
            
            String mChapBtnTextSmall = mChapBtnText.substring(0, 20) + "...";
            chapBtn.setText(mChapBtnTextSmall);
            
        } else {

            chapBtn.setText(mChapBtnText);
        
        }
        
        chapBtn.setVisibility(View.VISIBLE);
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history)
            .setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE,  R.string.menu_bookmark)
            .setIcon(android.R.drawable.ic_input_get);
        menu.add(Menu.NONE, PREVIOUS_ID, Menu.NONE, R.string.menu_previous)
            .setIcon(android.R.drawable.ic_media_previous);
        menu.add(Menu.NONE, NEXT_ID, Menu.NONE,  R.string.menu_next)
            .setIcon(android.R.drawable.ic_media_next);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        switch(item.getItemId()) {
        case PREVIOUS_ID:
            if (mChapOrderNbr > 0) {
                setProgressBarIndeterminateVisibility(true);
                try {
                    loadChapterByOrderId(mBookId, mChapOrderNbr - 1);
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not move to the previous chapter. " 
                            + ioe.getMessage());
                }
                setProgressBarIndeterminateVisibility(false);
            }
            return true;
        case NEXT_ID:
            if (mChapOrderNbr != -1) {
                setProgressBarIndeterminateVisibility(true);
                
                try {
                    loadChapterByOrderId(mBookId, mChapOrderNbr + 1);
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not move to the next chapter. " 
                            + ioe.getMessage());
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
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, 
            final Intent data) {
        
        Bundle extras;
        long histId;
        
        YbkDAO ybkDao = YbkDAO.getInstance(this);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case CALL_HISTORY:
                setProgressBarIndeterminateVisibility(true);
                
                extras = data.getExtras();
                histId = extras.getLong(YbkDAO.ID);
                
                History hist = ybkDao.getHistory(histId);
                
                if (hist != null) {
                    mBookId = hist.bookId;
                    Book book = ybkDao.getBook(mBookId);
                    if (book != null) {
                        mBookFileName = book.fileName;
                    }
                    
                    mChapFileName = hist.chapterName;
                    mHistTitle = hist.title;
                    mScrollYPos = hist.scrollYPos;
                        
                    Log.d(TAG, "Loading chapter from history file: " + mBookFileName + " chapter: " + mChapFileName);
                    
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
                
                if (addBookMark) {
                    showDialog(ASK_BOOKMARK_NAME);
                } else {
                    setProgressBarIndeterminateVisibility(true);
                    histId = extras.getLong(YbkDAO.ID);
                    
                    History bm = ybkDao.getHistory(histId);
                    
                    if (bm != null) {
                        mBookId = bm.bookId;
                        Book book = ybkDao.getBook(bm.bookId);
                        if (book != null) {
                            mBookFileName = book.fileName;
                        }
                        mChapFileName = bm.chapterName;
                        mHistTitle = bm.title;
                        mScrollYPos = bm.scrollYPos;
                        
                        Log.d(TAG, "Loading chapter from bookmark file: " + mBookFileName + " chapter: " + mChapFileName);
                        
                        try {
                            if (loadChapter(mBookFileName, mChapFileName)) {
                                    
                                setBookBtn(book.shortTitle, 
                                        mBookFileName, mChapFileName);
                            
                                mYbkView.scrollTo(0, mScrollYPos);
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Couldn't load chapter from bookmarks. " + ioe.getMessage());
                            FlurryAgent.onError("YbkViewActivity", "Couldn't load chapter from bookmarks", "WARNING");                        
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
    }
    
    /**
     * Load a chapter as identified by the id field of the book table and the 
     * order id.
     * 
     * @param bookId The record id of the chapter to load.
     * @return Did the chapter load?
     * @throws IOException If there was a problem reading the chapter.
     */
    private boolean loadChapterByOrderId(final long bookId, final int orderId) 
    throws IOException {
        
        boolean bookLoaded = false;
        
        YbkDAO ybkDao = YbkDAO.getInstance(this);
        
        Chapter chap = ybkDao.getChapter(bookId, orderId);
        
        if (chap != null) {
            Book book = ybkDao.getBook(chap.bookId);
            if (bookLoaded = loadChapter(book.fileName, chap.fileName)) {
                setBookBtn(book.shortTitle, book.fileName, chap.fileName);
            }
                
        } else {    
            Log.e(TAG, "No chapters found for order id: " + orderId);
        }
     
        return bookLoaded;
            
    }
    
    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the 
     * WebView.
     * 
     * @param filePath The path to the YBK file from which to read the chapter. 
     * @param chapter The "filename" of the chapter to load.
     * @throws IOException 
     */
    private boolean loadChapter(String filePath, final String chapter) throws IOException {
        boolean bookLoaded = false;
        WebView ybkView = mYbkView; 
        YbkFileReader ybkReader = mYbkReader;
        long bookId = -1L;
        
        // check the format of the internal file name
        if (!chapter.equals("index") 
                && chapter.toLowerCase().indexOf(".html") == -1) {
            showDialog(INVALID_CHAPTER);
            Log.e(TAG, "The chapter is invalid: " + chapter);
        } else {
            
            // get rid of any urlencoded spaces
            filePath = filePath.replace("%20", " ");
            String chap = chapter.replace("%20", " ");
            
            String content = "";
            String fragment = mFragment = null;
            
            //Log.d(TAG, "FilePath: " + filePath);
            
            File testFile = new File(filePath);
            if (!testFile.exists()) {
                // set the member property that holds the name of the book file we
                // couldn't find
                if (TextUtils.isEmpty(filePath)) {
                    mDialogFilename = "No file";
                } else {
                    String[] pathParts = filePath.split("/"); 
                    mDialogFilename = pathParts[pathParts.length-1];
                }
                
                showDialog(FILE_NONEXIST);
            } else {
                // Only create a new YbkFileReader if we're opening a different book
                if (!ybkReader.getFilename().equalsIgnoreCase(filePath)) {
                    ybkReader = mYbkReader = new YbkFileReader(this,filePath);
                }    
             
                YbkDAO ybkDao = YbkDAO.getInstance(this);
                Book book = ybkDao.getBook(filePath.toLowerCase());
                
                bookId = book.id;
                
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
                            ybkView.loadData("YBK file has no index page.",
                                    "text/plain","utf-8");
                            
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
                            chap = chap.substring(0, chap.length() - 1);
                        }
                        
                        // use the dreaded break <label> in order to simplify conditional nesting
                        label_get_content:
                        if (hashLoc != -1) {
                            fragment = chap.substring(hashLoc + 1);
                            if (fragment.indexOf(".") != -1) {
                                fragment = fragment.substring(0, fragment.indexOf("."));
                            }
                            mFragment = fragment;
                            
                            if (!Util.isInteger(fragment)) {
                                
                                // need to read a special footnote chapter
                                content = readConcatFile(chap, mYbkReader);
                                
                                if (content != null) {
                                    break label_get_content;
                                }
                            } else {
                                chap = chap.substring(0, hashLoc);
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
                            
                            if (chap.toLowerCase().endsWith(".gz")) {
                                
                                // Try it without the .gz 
                                chap.substring(0, chap.length() - 3);
                                content = mYbkReader.readInternalFile(chap);
                                if (content != null) {
                                    break label_get_content;
                                }
                            } else {
                                // try it with .gz
                                chap += ".gz";
                                content = mYbkReader.readInternalFile(chap);
                                if (content != null) {
                                    break label_get_content;
                                }
                            }
                            
                            // Need to read special concatenated file
                            content = readConcatFile(chap, mYbkReader);
                            if (content != null) {
                                break label_get_content;
                            }
                            
                            // if we haven't reached a break statement yet, we have a problem.
                            Toast.makeText(this, "Could not read chapter '" + chap + "'", Toast.LENGTH_LONG);
                            return false;
                        } // label_get_content:
                        
                    }
    
                    Chapter chapObj = ybkDao.getChapter(bookId,chap.toLowerCase());
                    
                    if (chapObj != null) {
                        mChapOrderNbr = chapObj.orderNumber;
                    } else {
                        mChapOrderNbr = -1;
                    } 
                    
                    // replace MS-Word "smartquotes" and other extended characters with spaces
                    content = content.replace('\u0093', '"').replace('\u0094','"');
                    
                    String strUrl = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString();
                    mHistTitle = mChapBtnText;
                    setChapBtnText(content);
    
                    String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, 
                            Settings.DEFAULT_EBOOK_DIRECTORY);
                    
                    content = Util.processIfbook(content, this, libDir);
                    
                    // Convert the ahtags
                    content = content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>", 
                        "<span class=\"ah\" id=\"ah$1\">$2</span>");
                    
                    content = Util.convertIfvar(content);
                    
                    ybkView.loadDataWithBaseURL(strUrl, Util.htmlize(content, mSharedPref),
                            "text/html","utf-8","");
                    
                    bookLoaded = true;
                    
                    if (!mBackButtonPressed) {
                        
                        if (mChapFileName != null) {
                            // Save the book and chapter to history if there
                            // is one
                            
                            ybkDao.insertHistory(bookId, mChapBtnText, chap, 
                                    mYbkView.getScrollY());
                            
                            // remove the excess histories
                            ybkDao.deleteHistories();
                        }
                        
                        // Reset the back button to the top of the history list;
                        mHistoryPos = 0;
                        mBookId = bookId;
                        mChapFileName = chap;
                        mScrollYPos = mYbkView.getScrollY();

                    }
                } catch (IOException e) {
                    ybkView.loadData("The chapter could not be opened.  " +
                            "The book may have a corrupted file.  " +
                            "You may want to get a new copy of the book.",
                            "text/plain","utf-8");
                    
                    Log.e(TAG, chap + " in " + filePath + " could not be opened. " + e.getMessage());
                    
                }
            }
        }
        
        return bookLoaded;
    }
    
    /**
     * Used to configure any dialog boxes created by this Activity
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        
        switch (id) {
        case FILE_NONEXIST :
            
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Not Set")
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                }
            })
            .create();
        case INVALID_CHAPTER :
            
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Not Set")
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                }
            })
            .create();
        
        case ASK_BOOKMARK_NAME :
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.view_ask_bm, null);
            final EditText et = (EditText) textEntryView.findViewById(R.id.ask_bm_name);
            
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("Enter Bookmark Name")
            .setView(textEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    String bmName = (String) et.getText().toString();
                    
                    //Log.i(TAG, "Text entered " + bmName); 
                    YbkDAO ybkDao = YbkDAO.getInstance(getBaseContext());
                    int bookmarkNumber = ybkDao.getMaxBookmarkNumber();
                    
                    ybkDao.insertHistory(mBookId, bmName, mChapFileName, 
                            mYbkView.getScrollY(), bookmarkNumber);

                }
            })
            .create();
            
        }
        return null;
        
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        String title;
        switch(id) {
        case FILE_NONEXIST :
            // replace the replaceable parameters
            title = getResources().getString(R.string.reference_not_found);
            title = MessageFormat.format(title, mDialogFilename);
            dialog.setTitle(title);
            break;
        case INVALID_CHAPTER :
            // replace the replaceable parameters
            title = getResources().getString(R.string.invalid_chapter);
            title = MessageFormat.format(title, mDialogFilename);
            dialog.setTitle(title);
            break;
        }
    }
    
    /**
     * Read a section of a special concatenated chapter;
     * @param chap The chapter to read.
     * @param ybkReader The YbkReader to use in order to access the chapter.
     * @return The content of the section.
     * @throws IOException If the Ybk file cannot be read.
     */
    private String readConcatFile(final String chap, final YbkFileReader ybkReader) 
    throws IOException {
     // need to read a special footnote chapter
        String concatChap = chap.substring(0, chap.lastIndexOf("\\")) + "_.html.gz";
        //Log.d(TAG, "concat file: " + concatChap);
        
        String endString = ".";
        if (chap.contains(".html")) {
            endString = ".html";
        } 
        
        String verse = chap.substring(chap.lastIndexOf("\\") + 1, chap.lastIndexOf(endString));
        
        Log.d(TAG, "verse/concatChap: " + verse + "/" + concatChap);
        
        String content = ybkReader.readInternalFile(concatChap);
        
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
        int endPos = content.indexOf("<end>");
        if (-1 == endPos) {
            throw new IllegalStateException("Chapter has no header");
        }
        String header = content.substring(0, endPos);
        int startFN = header.toLowerCase().indexOf("<fn>");
        if (-1 == startFN) {
            throw new IllegalStateException("Chapter has no full name");
        }
        
        //get past the <fn> tag
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
        
        mChapBtnText = chapBtnText;    
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setProgressBarIndeterminateVisibility(true);
            
            YbkDAO ybkDao = YbkDAO.getInstance(this);
            
            History hist = ybkDao.getPreviousHistory(++mHistoryPos);
            if (hist != null) {
                Book book = ybkDao.getBook(hist.bookId);
                String bookFileName = book.fileName;
                String chapFileName = hist.chapterName;
                mScrollYPos = hist.scrollYPos;
                
                
                //Log.d(TAG,"Going back to: " + bookFileName + ", " + chapFileName);
                
                mBackButtonPressed = true;
                try {
                    if (loadChapter(bookFileName, chapFileName)) {
                        
                        setBookBtn(book.shortTitle,bookFileName,chapFileName);
                        
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Could not return to the previous page " + ioe.getMessage());
                }
                
                mBackButtonPressed = false;
                
            } else {
                Toast.makeText(this, R.string.no_more_history, Toast.LENGTH_LONG).show();
            }
            
            return true;
        }

        return super.onKeyDown(keyCode, msg);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        HashMap<String, Comparable<?>> stateMap = new HashMap<String, Comparable<?>>();
        
        stateMap.put("bookFileName", mBookFileName);
        stateMap.put("chapFileName", mChapFileName);
        stateMap.put("bookId", mBookId);
        stateMap.put("histTitle", mHistTitle);
        stateMap.put("scrollYPos", mYbkView.getScrollY());
        return stateMap;
        
    }
}

