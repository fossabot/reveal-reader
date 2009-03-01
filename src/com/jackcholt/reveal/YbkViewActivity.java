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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private long mBookId;
    private String mBookFileName;
    private String mChapFileName;
    private Button mBookBtn;
    private Button mChapBtn;
    private YbkFileReader mYbkReader;
    private String mLibraryDir;
    private SharedPreferences mSharedPref;
    private boolean mShowPictures;
    private String mFragment;
    private String mDialogFilename = "Never set";
    private String mChapBtnText = "Not Set";
    private String mHistTitle = "";
    private int mChapOrderNbr = -1;
    private boolean mBackButtonPressed = false;
    private static final String TAG = "YbkViewActivity";
    private static final int FILE_NONEXIST = 1;
    private static final int INVALID_CHAPTER = 2;
    private static final int PREVIOUS_ID = Menu.FIRST;
    private static final int NEXT_ID = Menu.FIRST + 1;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!requestWindowFeature(Window.FEATURE_PROGRESS)) {
            Log.w(TAG, "Progress bar is not supported");
        }
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.FEATURE_INDETERMINATE_PROGRESS);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        mShowPictures = mSharedPref.getBoolean("show_pictures", true);
        
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
        
        Long bookId = savedInstanceState != null 
        ? (Long) savedInstanceState.get(YbkProvider._ID)
        : null;
        
        if (null == bookId) {
            Bundle extras = getIntent().getExtras();
            bookId = extras != null
                ? (Long) extras.get(YbkProvider._ID)
                : null;
        }
        
        
        HashMap<String, Comparable> statusMap = (HashMap<String, Comparable>) getLastNonConfigurationInstance();
        if (statusMap != null) {
            mBookId = bookId = (Long) statusMap.get("bookId");
            mBookFileName = (String) statusMap.get("bookFileName");
            mChapFileName = (String) statusMap.get("chapFileName");
            mHistTitle = (String) statusMap.get("histTitle");
            
            
        } else {

            if (null == bookId) {
                throw new IllegalStateException("A YBK bookId was not passed in the intent.");
            } 
    
            mBookId = bookId;
                
            ContentResolver contRes = getContentResolver();
            Cursor bookCursor = contRes.query(
                    ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"book"), 
                            bookId),
                    new String[] {YbkProvider.FILE_NAME}, null, null, null);
            
            //final String filePath;
            
            try {
                if (bookCursor.getCount() == 1) {
                    bookCursor.moveToFirst();
                    mBookFileName = bookCursor.getString(0);
                } else {
                    mBookFileName = "";
                }
            } finally {
                bookCursor.close();
            }
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
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
            
            
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
               
    
        ybkView.setWebViewClient(new WebViewClient() {
            
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.FEATURE_INDETERMINATE_PROGRESS);
                
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
                        dataString = URLDecoder.decode(url.substring(ContentUriLength + 1), "ISO-8859-1");
                    } catch (UnsupportedEncodingException uee) {
                        dataString = url.substring(ContentUriLength + 1);
                    }
                    
                    
                    String[] urlParts = dataString.split("/");
                    
                    
                    // get rid of the book indicator since it is only used in some cases.
                    book = shortTitle = urlParts[0];
                    if (book.charAt(0) == '!' || book.charAt(0) == '^') {
                        shortTitle = urlParts[0] = book.substring(1);
                    }
                    
                    book = mLibraryDir + urlParts[0] + ".ybk";
                    
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
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
                
                
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
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.FEATURE_INDETERMINATE_PROGRESS);
                if (loadChapter(filePath, "index") ) {
                    setBookBtn(shortTitle, filePath, fileToOpen);
                    Log.d(TAG, "Book loaded");
                } 
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
            }
            
        });
        
        bookBtn.setVisibility(View.VISIBLE);

        chapBtn.setText(mChapBtnText);
        
        chapBtn.setVisibility(View.VISIBLE);
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
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
                loadChapterByOrderId(mBookId, --mChapOrderNbr);
            }
            return true;
        case NEXT_ID:
            
            Cursor c = getContentResolver()
                .query(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), 
                    new String[] {"max(" + YbkProvider.CHAPTER_ORDER_NUMBER + ")"}, 
                    YbkProvider.BOOK_ID + "=?", new String[] {Long.toString(mBookId)}, 
                    null);
            
            int maxOrder = -1;
            try {
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    maxOrder = c.getInt(0);
                }
            } finally {
                c.close();
            }
            
            if (maxOrder > mChapOrderNbr) {
                loadChapterByOrderId(mBookId, ++mChapOrderNbr);
            }
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Load a chapter as identified by the id field of the chapters table.
     * 
     * @param chapterId The record id of the chapter to load.
     * @return Did the chapter load?
     */
    private boolean loadChapterByOrderId(final long bookId, final int orderId) {
        ContentResolver contRes = getContentResolver();
        
        Cursor c = contRes.query(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), 
                new String[] {YbkProvider._ID}, YbkProvider.CHAPTER_ORDER_NUMBER + "=? AND " + YbkProvider.BOOK_ID + "=?", 
                new String[] {Integer.toString(orderId), Long.toString(bookId)}, null);
        
        try {
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
        } finally {
            c.close();
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
        
        ContentResolver contRes = getContentResolver();
        
        Cursor c = contRes.query(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "chapter"), chapterId), 
                new String[] {YbkProvider.FILE_NAME, YbkProvider.BOOK_ID}, null, null, null);
        
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                String chapter = c.getString(0);
                int bookId = c.getInt(1);
                
                c = contRes.query(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), bookId), 
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
        } finally {
            c.close();
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
             
                Cursor c = getContentResolver().query(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), 
                        new String[] {YbkProvider._ID}, "lower(" + YbkProvider.FILE_NAME + ")=?", 
                        new String[] {filePath.toLowerCase()}, null);
                
                try {
                    int count = c.getCount();
                    if (count == 1) {
                        c.moveToFirst();
                        bookId = c.getLong(0);
                    } else if (count == 0){
                        throw new IllegalStateException("No books found for '" + filePath);
                    } else {
                        throw new IllegalStateException("More than one book found for '" + filePath);
                    }
                } finally {
                    c.close();
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
    
                    Cursor chapCurs = getContentResolver().query(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"chapter"), 
                            new String[] {YbkProvider.CHAPTER_ORDER_NUMBER}, 
                            "lower(" + YbkProvider.FILE_NAME + ")=?", 
                            new String[] {chap.toLowerCase()}, null);
                    
                    try {
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
                    } finally {
                        chapCurs.close();
                    }
                    
                    // replace MS-Word "smartquotes" and other extended characters with spaces
                    //content = content.replace('\ufffd', ' ');
                    
                    String strUrl = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString();
                    setChapBtnText(content);
    
                    content = Util.processIfbook(content, getContentResolver(), mLibraryDir);
                    content = Util.convertAhtag(content);
                    content = Util.convertIfvar(content);
                    
                    ybkView.loadDataWithBaseURL(strUrl, Util.htmlize(content, mSharedPref),
                            "text/html","utf-8","");
                    
                    bookLoaded = true;
                    
                    if (!mBackButtonPressed) {
                        if (mChapFileName != null) {
                            // Save the previous book and chapter to history if there
                            // is one
                            ContentValues values = new ContentValues();
                            values.put(YbkProvider.BOOK_ID, mBookId);
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
                    
                    Log.e(TAG, "A chapter in " + filePath + " could not be opened. " + e.getMessage());
                    
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
        
        String chapBtnText = mChapBtnText = header.substring(startFN, endFN);
        
        if ((chapBtnText.indexOf(":")) != -1) {
            String[] textParts = chapBtnText.split(":");
            mChapBtnText = textParts[1].trim();
        }
            
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.FEATURE_INDETERMINATE_PROGRESS);
            ContentResolver contRes = getContentResolver(); 
            
            Uri lastHistUri = ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "history"), 
                    YbkProvider.GET_LAST_HISTORY);
            
            Cursor c = contRes.query(lastHistUri,null, null, null, null);
            
            try {
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
                    
                }
            } finally {
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
                c.close();
            }
        }

        return false;
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

