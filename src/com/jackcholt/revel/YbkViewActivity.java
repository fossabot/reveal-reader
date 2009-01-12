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
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private ImageButton mMainBtn;
    private Button mBookBtn;
    private Button mChapBtn;
    private YbkFileReader mYbkReader;
    private String mLibraryDir;
    private SharedPreferences mSharedPref;
    private static final String TAG = "YbkViewActivity";
    private static final int FILE_NONEXIST = 1;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/ebooks/");

        setContentView(R.layout.view_ybk);

        final WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);  
        final ImageButton mainBtn = mMainBtn = (ImageButton) findViewById(R.id.mainMenu);
        final Button bookBtn = mBookBtn = (Button) findViewById(R.id.bookButton);
        final Button chapBtn = mChapBtn = (Button) findViewById(R.id.chapterButton);
        //LinearLayout bcLayout = mBcLayout = (LinearLayout) findViewById(R.id.breadCrumb); 
        
        mainBtn.setOnClickListener(new OnClickListener() {

            public void onClick(final View view) {
                //chapBtn.setVisibility(View.INVISIBLE);
                //bookBtn.setVisibility(View.INVISIBLE);
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
        
        if (null == bookId) {
            throw new IllegalStateException("A YBK bookId was not passed in the intent.");
        } else {
            ContentResolver contRes = getContentResolver();
            Cursor bookCursor = contRes.query(ContentUris.withAppendedId(Uri.withAppendedPath(YbkProvider.CONTENT_URI,"book"), bookId),
                    new String[] {YbkProvider.FILE_NAME}, null, null, null);
            final String filePath;
            
            if (bookCursor.getCount() == 1) {
                bookCursor.moveToFirst();
                filePath = bookCursor.getString(0);
            } else {
                filePath = "";
            }
            
            try {
                YbkFileReader ybkReader = mYbkReader = new YbkFileReader(filePath);
                String shortTitle = ybkReader.getBookShortTitle();
                String tryFileToOpen = "\\" + shortTitle + ".html.gz";
                String content = ybkReader.readInternalFile(tryFileToOpen);
                if (content == null) {
                    tryFileToOpen = "\\" + shortTitle + ".html";
                    content = ybkReader.readInternalFile(tryFileToOpen);
                }
                
                final String fileToOpen = tryFileToOpen;
                
                if (content == null) {
                    ybkView.loadData("YBK file has no index page.",
                            "text/plain","utf-8");
                    
                    Log.e("revel", "YBK file has no index page.");
                    return;
                }
                
                loadChapter(filePath, fileToOpen);
                bookBtn.setText(shortTitle);
                bookBtn.setOnClickListener(new OnClickListener() {
                    private String bookFilePath = filePath;
                    private String bookFileToOpen = fileToOpen;
                    
                    public void onClick(final View v) {
                        loadChapter(bookFilePath, bookFileToOpen);
                        Log.d(TAG, "Book loaded");
                    }
                    
                });
                bookBtn.setVisibility(View.VISIBLE);
                
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            
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
        ybkView.getSettings().setJavaScriptEnabled(true);
        String chap = chapter;
        String content = "";
        String fragment = null;
        
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
                
                // use the dreaded break <label> in order to simplify conditional nesting
                label_get_content:
                if ((hashLoc = chap.indexOf("#")) != -1) {
                    fragment = chap.substring(hashLoc + 1);
                    
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
                    
                    // Try it without the .gz 
                    chap.substring(0, chap.length() - 3);
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
                    throw new IllegalStateException("Unable to read chapter '" + chap + "'");
                    
                } // label_get_content:
                
                String strUrl = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book").toString();
                /*if (fragment != null) {
                    content += "<script language=\"javascript\">location.href=\"#" + fragment + "\";alert('running javascript')</script>";
                    Log.d(TAG, "content: " + Util.tail(content, 200));
                }*/
                
                content = processIfbook(content);
                
                ybkView.loadDataWithBaseURL(strUrl, Util.htmlize(content),
                        "text/html","utf-8","");
                
                // make it jump to the internal link
                /*if (fragment != null) {
                    ybkView.loadUrl("javascript:location.href=\"#" + fragment + "\"");
                }*/
                
            } catch (IOException e) {
                ybkView.loadData("The chapter could not be opened.",
                        "text/plain","utf-8");
                
                Log.e(TAG, "A chapter in " + filePath + " could not be opened. " + e.getMessage());
                
                return;
            }
            
        }
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

    private String processIfbook(final String content) {
        StringBuilder newContent = new StringBuilder();
        StringBuilder oldContent = new StringBuilder(content);
        ContentResolver contRes = getContentResolver();
        int pos = 0;
        
        while ((pos = oldContent.indexOf("<ifbook=")) != -1) {
            boolean fullIfBookFound = false;
            
            // copy text before <ifbook> tag to new content and remove from old
            newContent.append(oldContent.substring(0, pos));
            oldContent.delete(0, pos);
            
            int gtPos = oldContent.indexOf(">");
            if (gtPos != -1) {
                
                // grab the bookname by skipping the beginning of the ifbook tag
                String bookName = oldContent.substring(8, gtPos);
                int elsePos = oldContent.indexOf("<elsebook=" + bookName + ">");
                if (elsePos != -1 && elsePos > gtPos) {
                
                    int endPos = oldContent.indexOf("<endbook=" + bookName + ">");
                    if (endPos != -1 && endPos > elsePos) {
                        
                        fullIfBookFound = true;
                        
                        Cursor c = contRes.query(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), 
                                new String[] {YbkProvider.FILE_NAME}, "lower(" + YbkProvider.FILE_NAME + ") = lower(?)", 
                                new String[] {mLibraryDir + bookName + ".ybk"}, null);
                        
                        int count = c.getCount();
                        if (count == 1) {
                            newContent.append(oldContent.substring(gtPos + 1,elsePos));
                            Log.d(TAG, "Appending: " + oldContent.substring(gtPos + 1,elsePos));
                        } else if (count == 0) {
                            newContent.append(oldContent.substring(elsePos + bookName.length() + 11, endPos));
                        } else {
                            throw new IllegalStateException("More than one record for the same book");
                        }
                        
                        // remove just-parsed <ifbook> tag structure so we can find the next
                        oldContent.delete(0, endPos + bookName.length() + 10);
                    }
                }
            } 
            
            // remove just-parsed <ifbook> tag so we can find the next
            if (!fullIfBookFound) {
                oldContent.delete(0,8);
            }
            
        }
        
        // copy the remaining content over
        newContent.append(oldContent);
        
        return newContent.toString();
    }
}

