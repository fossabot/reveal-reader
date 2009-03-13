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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private long mBookId;
    private String mBookFileName;
    private String mChapFileName;
    private Button mBookBtn;
    private Button mChapBtn;
    private YbkFileReader mYbkReader;
    //private String mLibraryDir;
    private SharedPreferences mSharedPref;
    private boolean mShowPictures;
    private boolean BOOLshowFullScreen;
    private String mFragment;
    private String mDialogFilename = "Never set";
    private String mChapBtnText = "Not Set";
    private String mHistTitle = "";
    private int mChapOrderNbr = -1;
    private boolean mBackButtonPressed = false;
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
        
        if (!requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
            Log.w(TAG, "Progress bar is not supported");
        }
        
        setProgressBarIndeterminateVisibility(true);

        Long bookId = null;
        Boolean isFromHistory = null;

        HashMap<String, Comparable> statusMap = (HashMap<String, Comparable>) getLastNonConfigurationInstance();
        if (statusMap != null) {
            mBookId = bookId = (Long) statusMap.get("bookId");
            mBookFileName = (String) statusMap.get("bookFileName");
            mChapFileName = (String) statusMap.get("chapFileName");
            mHistTitle = (String) statusMap.get("histTitle");
            
        } else { 

            if (savedInstanceState != null) {
                bookId = (Long) savedInstanceState.get(YbkProvider._ID);            
                isFromHistory = (Boolean) savedInstanceState.get(YbkProvider.FROM_HISTORY);
            } else {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    isFromHistory = (Boolean) extras.get(YbkProvider.FROM_HISTORY);
                    bookId = (Long) extras.get(YbkProvider._ID);
                }
            }
            
            if (isFromHistory != null) {
                // bookId is actually the history id
                Cursor histCurs = managedQuery(
                        ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"history"), bookId), 
                        null, null, null, null);
                
                if (histCurs.moveToFirst()) {
                    bookId = histCurs.getLong(histCurs.getColumnIndex(YbkProvider.BOOK_ID));
                    mBookFileName = histCurs.getString(histCurs.getColumnIndex(YbkProvider.FILE_NAME));
                    mChapFileName = histCurs.getString(histCurs.getColumnIndex(YbkProvider.CHAPTER_NAME));
                    mHistTitle = histCurs.getString(histCurs.getColumnIndex(YbkProvider.HISTORY_TITLE));
                }
            }
        }    
        
        if (bookId == null) {
            Toast.makeText(this, R.string.book_not_loaded, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBookId = bookId;

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        /*mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        if(!mLibraryDir.endsWith("/")) {
        	mLibraryDir = mLibraryDir + "/";
        }*/
        
        mShowPictures = mSharedPref.getBoolean("show_pictures", true);
        
    	BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);
    	
        if (BOOLshowFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE); 
        }
         
        setContentView(R.layout.view_ybk);

        final WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);  
        ybkView.getSettings().setJavaScriptEnabled(true);
        final ImageButton mainBtn = (ImageButton) findViewById(R.id.mainMenu);
        mBookBtn = (Button) findViewById(R.id.bookButton);
        final Button chapBtn = mChapBtn = (Button) findViewById(R.id.chapterButton);
        chapBtn.setOnClickListener(new OnClickListener() {
            /** set the chapter button so it scrolls the window to the top */
            public void onClick(final View v) {
                mYbkView.loadUrl("javascript:location.href=\"#top\";");
            }
        });
        
        mainBtn.setOnClickListener(new OnClickListener() {

            public void onClick(final View view) {
                
                if (mChapFileName != null) {
                    // Save the previous book and chapter to history if there
                    // is one
                    ContentValues values = new ContentValues();
                    values.put(YbkProvider.BOOK_ID, mBookId);
                    //if (TextUtils.isEmpty(mHistTitle)) throw new IllegalStateException("HistoryTitle is empty"); // TODO Get rid of this

                    values.put(YbkProvider.HISTORY_TITLE, mHistTitle);
                    values.put(YbkProvider.CHAPTER_NAME, mChapFileName);
                    // TODO Temporarily using 0 until we figure out how to get the actual value
                    values.put(YbkProvider.SCROLL_POS, 0);
                    
                    Log.d(TAG, "Saving history for: " + values);
                    
                    getContentResolver().insert(
                            Uri.withAppendedPath(YbkProvider.CONTENT_URI,"history"), 
                            values);
                }
                
                
                finish();
            }
            
        });
                    
        Cursor bookCursor = managedQuery(
                ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"book"), 
                        bookId),
                new String[] {YbkProvider.FILE_NAME}, null, null, null);
        
        if (bookCursor.getCount() == 1) {
            bookCursor.moveToFirst();
            mBookFileName = bookCursor.getString(0);
        } else {
            mBookFileName = "";
        }
        
        try {
            YbkFileReader ybkReader = mYbkReader = new YbkFileReader(mBookFileName);
            String shortTitle = ybkReader.getBookShortTitle();
            if (mChapFileName == null) {
                String tryFileToOpen = "\\" + shortTitle + ".html.gz";
                String content = ybkReader.readInternalFile(tryFileToOpen);
                if (content == null) {
                    tryFileToOpen = "\\" + shortTitle + ".html";
                    content = ybkReader.readInternalFile(tryFileToOpen);
                }
                
                
                if (content == null) {
                    ybkView.loadData("YBK file has no index page.",
                            "text/plain","ISO_8859-1");
                    
                    Log.e(TAG, "YBK file has no index page.");
                    return;
                }
                mChapFileName = tryFileToOpen;
            }
            
            if (loadChapter(mBookFileName, mChapFileName)) {
                setBookBtn(shortTitle, mBookFileName, mChapFileName);
            }

            
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
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
                setProgressBarIndeterminateVisibility(true);
                
                String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, "/sdcard/reveal/ebooks/");
                
                Log.d(TAG, "WebView URL: " + url);
                String book;
                String chapter = "";
                String shortTitle = null;
                
                if (url.indexOf('@') != -1) {
                    book = mBookFileName;
                    chapter = mChapFileName;
                } else {
                
                    int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
                    
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
                    
                    if (!chapter.contains("#")) {
                        chapter += ".gz";                        
                    }
                }
                Log.i(TAG, "Loading chapter '" + chapter + "'");
                
                if (loadChapter(book, chapter)) {                    
                    setBookBtn(shortTitle,book,chapter);
                }
                setProgressBarIndeterminateVisibility(false);
                
                
                return true;
            }
            
            public void onPageFinished(final WebView view, final String url) {
                // make it jump to the internal link
                if (mFragment != null) {
                    Log.d(TAG, "In onPageFinished(). Jumping to #" + mFragment);
                    view.loadUrl("javascript:location.href=\"#" + mFragment + "\"");
                    mFragment = null;
                } else if (url.indexOf('@') != -1) {
                    view.loadUrl("javascript:location.href=\"#top\"");
                }
                
                
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
                if (loadChapter(filePath, "index") ) {
                    setBookBtn(shortTitle, filePath, fileToOpen);
                    Log.d(TAG, "Book loaded");
                } 
                setProgressBarIndeterminateVisibility(false);
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
            .setIcon(android.R.drawable.ic_menu_compass);
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
            setProgressBarIndeterminateVisibility(true);
            if (mChapOrderNbr > 0) {
                loadChapterByOrderId(mBookId, --mChapOrderNbr);
            }
            setProgressBarIndeterminateVisibility(false);
            return true;
        case NEXT_ID:
            setProgressBarIndeterminateVisibility(true);
            
            Cursor c = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), 
                    new String[] {"max(" + YbkProvider.CHAPTER_ORDER_NUMBER + ")"}, 
                    YbkProvider.BOOK_ID + "=?", new String[] {Long.toString(mBookId)}, 
                    null);
            
            int maxOrder = -1;

            if (c.getCount() == 1) {
                c.moveToFirst();
                maxOrder = c.getInt(0);
            }
            
            if (maxOrder > mChapOrderNbr) {
                loadChapterByOrderId(mBookId, ++mChapOrderNbr);
            }
            setProgressBarIndeterminateVisibility(false);
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
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case CALL_HISTORY:
                setProgressBarIndeterminateVisibility(true);
                extras = data.getExtras();
                long histId = extras.getLong(YbkProvider._ID);
                
                Cursor histCurs = managedQuery(
                        ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"history"), histId), 
                        null, null, null, null);
                
                if (histCurs.moveToFirst()) {
                    mBookId = histCurs.getLong(histCurs.getColumnIndex(YbkProvider.BOOK_ID));
                    mBookFileName = histCurs.getString(histCurs.getColumnIndex(YbkProvider.FILE_NAME));
                    mChapFileName = histCurs.getString(histCurs.getColumnIndex(YbkProvider.CHAPTER_NAME));
                    mHistTitle = histCurs.getString(histCurs.getColumnIndex(YbkProvider.HISTORY_TITLE));
                    
                    Log.d(TAG, "Loading chapter from history file: " + mBookFileName + " chapter: " + mChapFileName);
                    
                    if (loadChapter(mBookFileName, mChapFileName)) {
                        String[] projection = new String[] {YbkProvider.SHORT_TITLE};
                        Cursor c = managedQuery(
                                ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), mBookId), 
                                projection, null, null, null);

                        if (c.moveToFirst()) {
                            
                            setBookBtn(c.getString(c.getColumnIndex(YbkProvider.SHORT_TITLE)), 
                                    mBookFileName, mChapFileName);
                        }
                        
                    }
                } else {
                    Log.e(TAG, "Couldn't load chapter from history");
                }
                
                setProgressBarIndeterminateVisibility(false);
                
                return;

            case CALL_BOOKMARK:
                setProgressBarIndeterminateVisibility(true);
                extras = data.getExtras();
                
                boolean addBookMark = extras.getBoolean(BookmarkDialog.ADD_BOOKMARK);
                
                if (addBookMark) {
                    showDialog(ASK_BOOKMARK_NAME);
                } else {
                    long bmId = extras.getLong(YbkProvider.BOOKMARK_NUMBER);
                    
                    Cursor bmCurs = managedQuery(
                            ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"bookmark"), bmId), 
                            null, null, null, null);
                    
                    if (bmCurs.moveToFirst()) {
                        mBookId = bmCurs.getLong(bmCurs.getColumnIndex(YbkProvider.BOOK_ID));
                        mBookFileName = bmCurs.getString(bmCurs.getColumnIndex(YbkProvider.FILE_NAME));
                        mChapFileName = bmCurs.getString(bmCurs.getColumnIndex(YbkProvider.CHAPTER_NAME));
                        mHistTitle = bmCurs.getString(bmCurs.getColumnIndex(YbkProvider.HISTORY_TITLE));
                        
                        Log.d(TAG, "Loading chapter from bookmark file: " + mBookFileName + " chapter: " + mChapFileName);
                        
                        if (loadChapter(mBookFileName, mChapFileName)) {
                            String[] projection = new String[] {YbkProvider.SHORT_TITLE};
                            Cursor c = managedQuery(
                                    ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), mBookId), 
                                    projection, null, null, null);
    
                            if (c.moveToFirst()) {
                                
                                setBookBtn(c.getString(c.getColumnIndex(YbkProvider.SHORT_TITLE)), 
                                        mBookFileName, mChapFileName);
                            }
                            
                        }
                    } else {
                        Log.e(TAG, "Couldn't load chapter from bookmarks");
                    }
                }
                
                setProgressBarIndeterminateVisibility(false);
                
                return;

            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /**
     * Load a chapter as identified by the id field of the chapters table.
     * 
     * @param chapterId The record id of the chapter to load.
     * @return Did the chapter load?
     */
    private boolean loadChapterByOrderId(final long bookId, final int orderId) {
        
        Cursor c = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), 
                new String[] {YbkProvider._ID}, YbkProvider.CHAPTER_ORDER_NUMBER + "=? AND " + YbkProvider.BOOK_ID + "=?", 
                new String[] {Integer.toString(orderId), Long.toString(bookId)}, null);
        
        if (c.getCount() == 1) {
            c.moveToFirst();
            int chapterId = c.getInt(0);
                
            return loadChapter(chapterId);
            
        } else if (c.getCount() == 0) {    
            throw new IllegalStateException("No chapters found for order_number: " + orderId);
        } else {
            throw new IllegalStateException(
                    "Too many rows returned from a query for one chapter (order_number: " + orderId + ")");
        }
            
    }

    /**
     * Load a chapter as identified by the id field of the chapters table.
     * 
     * @param chapterId The record id of the chapter to load.
     * @return Did the chapter load?
     */
    private boolean loadChapter(final int chapterId) {
        boolean bookLoaded = false;
                
        Cursor c = managedQuery(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), chapterId), 
                new String[] {YbkProvider.FILE_NAME, YbkProvider.BOOK_ID}, null, null, null);
        
        if (c.getCount() == 1) {
            c.moveToFirst();
            String chapter = c.getString(0);
            int bookId = c.getInt(1);
            
            c = managedQuery(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), bookId), 
                new String[] {YbkProvider.FILE_NAME}, null, null, null);
            
            if (c.getCount() == 1) {
                c.moveToFirst();
                String bookFileName = c.getString(0);
                
                String[] pathParts = bookFileName.split("/");
                String[] fileNameParts = pathParts[pathParts.length - 1].split("\\.");
                String shortTitle = fileNameParts[0];
                
                if (bookLoaded = loadChapter(bookFileName, chapter)) {
                    setBookBtn(shortTitle, bookFileName, chapter);
                }
            
            
            } else if (c.getCount() == 0) {    
                throw new IllegalStateException("No books found for id: " + bookId);
            } else {
                throw new IllegalStateException(
                        "Too many rows returned from a query for one book (id: " + bookId + ")");
            }
        } else if (c.getCount() == 0) {    
            throw new IllegalStateException("No chapters found for id: " + chapterId);
        } else {
            throw new IllegalStateException(
                    "Too many rows returned from a query for one chapter (id: " + chapterId + ")");
        }
        
        return bookLoaded;    
    }
    
    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the 
     * WebView.
     * 
     * @param filePath The path to the YBK file from which to read the chapter. 
     * @param chapter The "filename" of the chapter to load.
     */
    private boolean loadChapter(String filePath, final String chapter) {
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
            
            Log.d(TAG, "FilePath: " + filePath);
            
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
                    try {
                        ybkReader = mYbkReader = new YbkFileReader(filePath);
                        
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }    
             
                Cursor c = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), 
                        new String[] {YbkProvider._ID}, "lower(" + YbkProvider.FILE_NAME + ")=?", 
                        new String[] {filePath.toLowerCase()}, null);
                
                int count = c.getCount();
                if (count == 1) {
                    c.moveToFirst();
                    bookId = c.getLong(0);
                } else if (count == 0){
                    throw new IllegalStateException("No books found for '" + filePath);
                } else {
                    throw new IllegalStateException("More than one book found for '" + filePath);
                }
                
                
                try {
                    if (chap.equals("index")) {
                        String shortTitle = ybkReader.getBookShortTitle();
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
                            mFragment = fragment = chap.substring(hashLoc + 1);
                            
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
                            throw new IllegalStateException("Unable to read chapter '" + chap + "'");
                            
                        } // label_get_content:
                        
                    }
    
                    Cursor chapCurs = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"chapter"), 
                            new String[] {YbkProvider.CHAPTER_ORDER_NUMBER}, 
                            "lower(" + YbkProvider.FILE_NAME + ")=?", 
                            new String[] {chap.toLowerCase()}, null);
                
                    if (chapCurs.getCount() == 1) {
                        chapCurs.moveToFirst();
                        mChapOrderNbr = chapCurs.getInt(0);
                    } else if (chapCurs.getCount() == 0){
                        mChapOrderNbr = -1;
                    } else {
                        throw new IllegalStateException(
                                "More than one chapter returned when attempting to get order number for: " 
                                + chap);
                    }
                    
                    // replace MS-Word "smartquotes" and other extended characters with spaces
                    content = content.replace('\u0093', '"').replace('\u0094','"');
                    
                    String strUrl = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString();
                    mHistTitle = mChapBtnText;
                    setChapBtnText(content);
    
                    String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, "/sdcard/reveal/ebooks/");
                    
                    content = Util.processIfbook(content, getContentResolver(), libDir);
                    content = Util.convertAhtag(content);
                    content = Util.convertIfvar(content);
                    
                    ybkView.loadDataWithBaseURL(strUrl, Util.htmlize(content, mSharedPref),
                            "text/html","utf-8","");
                    
                    Log.d(TAG, "Content Height: " + ybkView.getContentHeight());
                    
                    bookLoaded = true;
                    
                    if (!mBackButtonPressed) {
                        if (mChapFileName != null && !chap.equalsIgnoreCase(mChapFileName)) {
                            // Save the previous book and chapter to history if there
                            // is one
                            ContentValues values = new ContentValues();
                            values.put(YbkProvider.BOOK_ID, mBookId);
                            
                            //if (TextUtils.isEmpty(mHistTitle)) throw new IllegalStateException("HistoryTitle is empty"); // TODO Get rid of this

                            values.put(YbkProvider.HISTORY_TITLE, mHistTitle);
                            values.put(YbkProvider.CHAPTER_NAME, mChapFileName);
                            // TODO Temporarily using 0 until we figure out how to get the actual value
                            values.put(YbkProvider.SCROLL_POS, 0);
                            
                            Log.d(TAG, "Saving history for: " + values);
                            
                            getContentResolver().insert(
                                    Uri.withAppendedPath(YbkProvider.CONTENT_URI,"history"), 
                                    values);
                        }
                        
                        mChapFileName = chap;
                        mBookFileName = filePath;
                        mBookId = bookId;
                        mHistTitle = mChapBtnText;
                    }
                } catch (IOException e) {
                    ybkView.loadData("The chapter could not be opened.",
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
                    
                    Log.i(TAG, "Text entered " + bmName); 
                    
                    ContentValues values = new ContentValues();
                    values.put(YbkProvider.BOOK_ID, mBookId);
                    
                    values.put(YbkProvider.HISTORY_TITLE, bmName);
                    values.put(YbkProvider.CHAPTER_NAME, mChapFileName);
                    // TODO Temporarily using 0 until we figure out how to get the actual value
                    values.put(YbkProvider.SCROLL_POS, 0);
                    
                    Log.d(TAG, "Saving bookmark for: " + values);
                    
                    getContentResolver().insert(
                            Uri.withAppendedPath(YbkProvider.CONTENT_URI,"bookmark"), 
                            values);

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
        Log.d(TAG, "concat file: " + concatChap);
        
        String endString = ".";
        if (chap.endsWith(".html.gz")) {
            endString = ".html.gz";
        }
        String verse = chap.substring(chap.lastIndexOf("\\") + 1, chap.lastIndexOf(endString));
        Log.d(TAG, "verse: " + verse);
        
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
            
            ContentResolver contRes = getContentResolver(); 
            
            Uri lastHistUri = ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "history"), 
                    YbkProvider.GET_LAST_HISTORY);
            
            Cursor c = managedQuery(lastHistUri,null, null, null, null);
            
            int count = c.getCount();
            if (count == 1) {
                c.moveToFirst();
                String bookFileName = c.getString(0);
                String chapFileName = c.getString(1);
                int scrollPos = c.getInt(2);
                
                mBackButtonPressed = true;
                if (loadChapter(bookFileName, chapFileName)) {
                    int slashPos = bookFileName.lastIndexOf("/");
                    int dotPos = bookFileName.indexOf(".");
                    
                    setBookBtn(bookFileName.substring(slashPos + 1, dotPos),bookFileName,chapFileName);
                    
                    contRes.delete(lastHistUri, null, null);
                }
                
                mBackButtonPressed = false;
                
            } else {
                Toast.makeText(this, R.string.no_more_history, Toast.LENGTH_LONG).show();
            }
            
            setProgressBarIndeterminateVisibility(false);
            return true;
        }

        return super.onKeyDown(keyCode, msg);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        HashMap<String, Comparable> stateMap = new HashMap<String, Comparable>();
        
        stateMap.put("bookFileName", mBookFileName);
        stateMap.put("chapFileName", mChapFileName);
        stateMap.put("bookId", mBookId);
        stateMap.put("histTitle", mHistTitle);
        
        return stateMap;
        
    }
}

