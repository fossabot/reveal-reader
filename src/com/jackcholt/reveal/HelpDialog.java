package com.jackcholt.reveal;

import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.content.Context;
import android.webkit.WebView;

/**
 * HelpDialog for online HELP system
 * 
 * by Dave Packham
 */

public class HelpDialog extends Dialog {
	public HelpDialog(Context _this) {
		super(_this);
		// Is Network up or not?
		if (Util.isNetworkUp(_this)) {
			FlurryAgent.onEvent("OnlineHelp");
			setContentView(R.layout.help);
			String title;
			title = "Reveal Online Help";
			setTitle(title);

			WebView wv = (WebView) findViewById(R.id.helpView);
			wv.clearCache(true);
			wv.getSettings().setJavaScriptEnabled(true);
			wv.loadUrl("http://revealreader.thepackhams.com/revealHelp.html");

			show();
		}
	}

	public static HelpDialog create(Context _this) {
		// Change DEBUG to "0" in Global.java when building a RELEASE Version for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0 ) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(_this, "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(_this, "C9D5YMTMI5SPPTE8S4S4");
		}

		HelpDialog dlg = new HelpDialog(_this);
		return dlg;
	}

	/** Called when the activity is going away. */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession();
	}
};