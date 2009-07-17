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

public class PopupHelpDialog extends Dialog {
    public PopupHelpDialog(Context _this, String DialogName) {
        super(_this);

        FlurryAgent.onEvent("PopupHelp");
        setContentView(R.layout.popuphelp);
        String title;
        title = "Reveal Popup Help";
        setTitle(title);

        WebView wv = (WebView) findViewById(R.id.popupView);
        wv.clearCache(true);
        wv.loadUrl("file:///android_asset/newfontsizeoption.html");

        show();

    }

    public static PopupHelpDialog create(Context _this, String DialogName) {
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

        PopupHelpDialog dlg = new PopupHelpDialog(_this, DialogName);
        return dlg;
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(Main.getMainApplication());
    }
}