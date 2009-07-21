package com.jackcholt.reveal;

import java.net.URLEncoder;

import javax.xml.parsers.FactoryConfigurationError;

import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.webkit.WebView;
import com.flurry.android.FlurryAgent;

/**
 * Reports Errors in the program to the Reveal Website VIA URL with
 * "?errorString"
 * 
 * by Dave Packham
 */

public class ReportError {

    public static void reportErrorToWebsite(String errorToReport, Boolean sendEmail) {

        try {
            // Send the errorToReport string to the website.
            FlurryAgent.onEvent("ReportErrorToWebsite");
            WebView mWebView = new WebView(Main.getMainApplication());
            mWebView.clearCache(true);
            mWebView.getSettings().setJavaScriptEnabled(true);
            String errorURL;
            errorURL = "http://revealreader.thepackhams.com/exception.php?StackTrace=" 
                + URLEncoder.encode("Build " + Global.SVN_VERSION + "\n" + errorToReport, "UTF-8");
            mWebView.loadUrl(errorURL);

            // Create the Intent to send Email error report  
            if (sendEmail = true) {
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
                emailIntent.setType("plain/text");  
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"bugs@thepackhams.com"});  
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Exception from User");
                String message = ("Build " + Global.SVN_VERSION + "\n" + errorToReport);
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);  
                Main.getMainApplication().startActivity(Intent.createChooser(emailIntent, "Send Error Report Email..."));
            }           
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void reportError(final String errorToReport, final Boolean sendEmail) {
        Thread t = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                reportErrorToWebsite(errorToReport, sendEmail);
            }
        };
        t.start();

    }
}
