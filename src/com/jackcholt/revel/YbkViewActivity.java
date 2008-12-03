package com.jackcholt.revel;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class YbkViewActivity extends Activity {
    private WebView mYbkView;
    private YbkFileReader mYbkReader;
    
    public static final String KEY_FILEPATH = "filePath";
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        YbkFileReader ybkReader;
        setContentView(R.layout.view_ybk);
        
        WebView ybkView = mYbkView = (WebView) findViewById(R.id.ybkView);  
        
        
        String filePath = savedInstanceState != null 
            ? (String) savedInstanceState.get(KEY_FILEPATH)
            : null;
            
        if (null == filePath) {
            Bundle extras = getIntent().getExtras();
            filePath = extras != null
                ? (String) extras.get(KEY_FILEPATH)
                : null;
        }
        
        if (null == filePath) {
            throw new IllegalStateException("A YBK file name was not passed in the intent.");
        } else {
            try {
                ybkReader = mYbkReader = new YbkFileReader(filePath);
            } catch (IOException ioe) {
                ybkView.loadData("That book could not be opened.",
                        "text/plain","utf-8");
                
                Log.e("revel", filePath + " could not be opened. " + ioe.getMessage());
                
                return;
            }

            try {
                String fileToOpen = "\\" + ybkReader.getMBookShortTitle() + ".html.gz";
                String content = ybkReader.readInternalFile(fileToOpen);
                if (content == null) {
                    fileToOpen = "\\" + ybkReader.getMBookShortTitle() + ".html";
                    content = ybkReader.readInternalFile(fileToOpen);
                }
                
                if (content == null) {
                    ybkView.loadData("YBK file has no index page.",
                            "text/plain","utf-8");
                    
                    Log.e("revel", "YBK file has no index page.");
                    return;
                }
                
                ybkView.loadDataWithBaseURL("",Util.htmlize(content),
                        "text/html","utf-8","");
            } catch (IOException e) {
                ybkView.loadData("The index of the book could not be opened.",
                        "text/plain","utf-8");
                
                Log.e("revel", "The index of " + filePath + " could not be opened. " + e.getMessage());
                
                return;
            }
            
            ybkView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                    Log.d(Global.TAG, "WebView URL: " + url);
                    super.shouldOverrideUrlLoading(view, url);
                    
                    return true;
                }
             });

        }
        
    }
    
}
