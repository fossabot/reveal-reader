package com.jackcholt.revel;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
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
    private static final int FILE_NONEXIST = 1;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLibraryDir = mSharedPref.getString("library_dir", "/sdcard/");

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
                    
                    if (!chapter.contains("#")) {
                        chapter += ".gz";                        
                    }
                    
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
        String chap = chapter;
        String content = "";
        
        Log.d(TAG, "FilePath: " + filePath);
        
        File testFile = new File(filePath);
        if (!testFile.exists()) {
            showDialog(FILE_NONEXIST);
        } else {
        
            try {
                mYbkReader = new YbkFileReader(filePath);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        
            try {
                int hashLoc = -1;
                if ((hashLoc = chap.indexOf("#")) != -1) {
                    String fragment = chap.substring(hashLoc + 1);
                    
                    if (!Util.isInteger(fragment)) {
                        
                        // to need read a special footnote chapter
                        String footnotes = chap.substring(0, chap.lastIndexOf("\\")) + "_.html.gz";
                        Log.d(TAG, "footnote file: " + footnotes);
                        
                        String verse = chap.substring(chap.lastIndexOf("\\") + 1, chap.lastIndexOf("."));
                        Log.d(TAG, "verse: " + verse);
                        
                        content = mYbkReader.readInternalFile(footnotes);
                        
                        content = content.substring(content.indexOf('\002' + verse + '\002') + verse.length() + 2);
                        
                        if (content.indexOf('\002') != -1) {
                            content = content.substring(0, content.indexOf('\002'));
                        }
                    } else {
                        chap = chap.substring(0, hashLoc);
                        content = mYbkReader.readInternalFile(chap);
                        if (content == null) {
                            content = mYbkReader.readInternalFile(chap + ".gz");
                            if (content == null) {
                                throw new IllegalStateException("Unable to read chapter '" + chap + ".gz'");                                
                            }
                        }
                    }
                } else {
                    content = mYbkReader.readInternalFile(chap);
                    if (content == null) {
                        // Try it without the .gz 
                        chap.substring(0, chap.length() - 3);
                        content = mYbkReader.readInternalFile(chap);
                        if (content == null) {
                            throw new IllegalStateException("Unable to read chapter '" + chap + "'");
                        }
                    }
                }
                
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
    
    @Override
    protected Dialog onCreateDialog(int id) {
        
        switch (id) {
        case FILE_NONEXIST :
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.reference_not_found)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                }
            })
            .create();
        }
        return null;
    }
}
