package com.jackcholt.reveal;

import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.content.Context;
import android.webkit.WebView;

/**
 * DonateDialog for online Donations system
 * 
 * by Dave Packham
 */

public class DonateDialog extends Dialog {
    public DonateDialog(Context _this) {
        super(_this);

        setContentView(R.layout.donate);
        String title;
        title = "Reveal PayPal Donations";
        setTitle(title);

        WebView wv = (WebView) findViewById(R.id.donateView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);
        if (Util.isNetworkUp(_this)) {
            wv.loadUrl("file:///android_asset/donate.html");
        } else {
            wv.loadData("Cannot get to the Donation website.  Your network is currently down.", "text/plain", "utf-8");
        }

        show();

    }

    public static DonateDialog create(Context _this) {
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
        FlurryAgent.onEvent("DonateDialog");

        DonateDialog dlg = new DonateDialog(_this);
        return dlg;
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(Main.getMainApplication());
    }
}