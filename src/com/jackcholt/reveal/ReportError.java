package com.jackcholt.reveal;

import java.net.URLEncoder;

import javax.xml.parsers.FactoryConfigurationError;

import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.webkit.WebView;
import com.flurry.android.FlurryAgent;

/**
 * Reports Errors in the program to the Reveal Website VIA URL with "?errorString"
 * 
 * by Dave Packham
 */

public class ReportError {

    public static void reportErrorToWebsite(String errorToReport, Boolean sendEmail) {

        try {
            // Send the errorToReport string to the website.
            FlurryAgent.onEvent("ReportErrorToWebsite");
            WebView webView = new WebView(Main.getMainApplication());
            webView.clearCache(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("http://revealreader.thepackhams.com/exception.php?StackTrace="
                    + URLEncoder.encode("Build " + Global.SVN_VERSION + "\n" + errorToReport, "UTF-8"));

            // Create the Intent to send Email error report
            if (sendEmail) {
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND).setType("plain/text")
                        .putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "bugs@thepackhams.com" })
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Exception from User").putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                ("Build " + Global.SVN_VERSION + "\n" + errorToReport));
                Main.getMainApplication()
                        .startActivity(Intent.createChooser(emailIntent, "Send Error Report Email..."));
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
