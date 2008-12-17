package com.jackcholt.revel;

import java.io.IOException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private YbkFileReader mYbkReader;
    private String mLibraryDir;
    private SharedPreferences mSharedPref;
    private static final String TAG = "YbkViewActivity";
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String strLibDir = mLibraryDir = mSharedPref.getString("library_dir", "/sdcard/");

        setContentView(R.layout.view_ybk);
        
        WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);  
        
        
        Long bookId = savedInstanceState != null 
            ? (Long) savedInstanceState.get(YbkProvider._ID)
            : null;
            
        if (null == bookId) {
            Bundle extras = getIntent().getExtras();
            bookId = extras != null
                ? (Long) extras.get(YbkProvider._ID)
                : null;
        }
        
        if (null == bookId) {
            throw new IllegalStateException("A YBK bookId was not passed in the intent.");
        } else {
            ContentResolver contRes = getContentResolver();
            Cursor bookCursor = contRes.query(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"book"), bookId),
                    new String[] {YbkProvider.FILE_NAME}, null, null, null);
            String filePath = "";
            
            if (bookCursor.getCount() == 1) {
                bookCursor.moveToFirst();
                filePath = bookCursor.getString(0);
            }
            
            try {
                YbkFileReader ybkReader = mYbkReader = new YbkFileReader(filePath);
                
                String fileToOpen = "\\" + ybkReader.getBookShortTitle() + ".html.gz";
                String content = ybkReader.readInternalFile(fileToOpen);
                if (content == null) {
                    fileToOpen = "\\" + ybkReader.getBookShortTitle() + ".html";
                    content = ybkReader.readInternalFile(fileToOpen);
                }
                
                if (content == null) {
                    ybkView.loadData("YBK file has no index page.",
                            "text/plain","utf-8");
                    
                    Log.e("revel", "YBK file has no index page.");
                    return;
                }
                
                loadChapter(filePath, fileToOpen);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            
            
            
            
            
            //ybkView.loadUrl(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), bookId).toString());
            
            ybkView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                    Log.d(TAG, "WebView URL: " + url);

                    int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
                    String dataString = url.substring(ContentUriLength + 1);
                    
                    String[] urlParts = dataString.split("/");
                    
                    String book = mLibraryDir + urlParts[0] + ".ybk";
                    
                    String chapter = "";
                    for (int i = 0; i < urlParts.length; i++) {
                       chapter += "\\" + urlParts[i];
                    }
                    chapter += ".gz";
                    
                    Log.i(TAG, "Loading chapter '" + chapter + "'");
                    
                    loadChapter(book, chapter);
                    return true;
                }
             });

        }
        
    }
    
    /**
     * Uses a YbkFileReader to get the content of a chapter and loads into the 
     * WebView.
     * 
     * @param filePath The path to the YBK file from which to read the chapter. 
     * @param chapter The "filename" of the chapter to load.
     */
    private void loadChapter(final String filePath, final String chapter) {
    
        WebView ybkView = mYbkView; 
        
        try {
            mYbkReader = new YbkFileReader(filePath);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    
        try {
            
            String content = mYbkReader.readInternalFile(chapter);
            ybkView.loadDataWithBaseURL(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString(),
                    Util.htmlize(content),
                    "text/html","utf-8","");
        } catch (IOException e) {
            ybkView.loadData("The chapter could not be opened.",
                    "text/plain","utf-8");
            
            Log.e(TAG, "A chapter in " + filePath + " could not be opened. " + e.getMessage());
            
            return;
        }

    }
}
