package com.jackcholt.reveal;

import javax.xml.parsers.FactoryConfigurationError;

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

	public static void reportErrorToWebsite(String errorToReport) {

		try {
			// Send the errorToReport string to the website.
			FlurryAgent.onEvent("ReportErrorToWebsite");
			WebView mWebView = new WebView(Main.getMainApplication());
			mWebView.clearCache(true);
			mWebView.getSettings().setJavaScriptEnabled(true);
			mWebView.loadUrl("http://revealreader.thepackhams.com/revealError.html?Error="
					+ errorToReport);

		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void reportError(final String errorToReport) {
		// Change DEBUG to "0" in Global.java when building a RELEASE Version for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0 ) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(Main.getMainApplication(), "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(Main.getMainApplication(), "VYRRJFNLNSTCVKBF73UP");
		}

		Thread t = new Thread() {
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				Looper.prepare();
				reportErrorToWebsite(errorToReport);
			}
		};
		t.start();

	}
}
