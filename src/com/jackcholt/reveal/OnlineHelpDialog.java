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

public class OnlineHelpDialog extends Dialog {
    public OnlineHelpDialog(Context _this) {
        super(_this);

        FlurryAgent.onEvent("OnlineHelp");
        setContentView(R.layout.dialog_help);
        String title;
        title = "Reveal Online Help";
        setTitle(title);

        WebView wv = (WebView) findViewById(R.id.helpView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);
        if (Util.isNetworkUp(_this)) {
            wv.loadUrl("http://revealreader.thepackhams.com/revealHelp.html");
        } else {
            wv.loadData("Cannot get online help.  Your network is currently down.", "text/plain", "utf-8");
        }

        show();

    }

    public static OnlineHelpDialog create(Context _this) {
        // Change DEBUG to "0" in Global.java when building a RELEASE Version
        // for the GOOGLE APP MARKET
        // This allows for real usage stats and end user error reporting
        if (Global.DEBUG == 0) {
            // Release Key for use of the END USERS
            FlurryAgent.onStartSession(_this, "BLRRZRSNYZ446QUWKSP4");
        } else {
            // Development key for use of the DEVELOPMENT TEAM
            FlurryAgent.onStartSession(_this, "VYRRJFNLNSTCVKBF73UP");
        }

        OnlineHelpDialog dlg = new OnlineHelpDialog(_this);
        return dlg;
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(Main.getMainApplication());
    }
}