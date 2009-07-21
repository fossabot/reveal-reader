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

        setContentView(R.layout.dialog_donate);
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
        FlurryAgent.onEvent("DonateDialog");
        DonateDialog dlg = new DonateDialog(_this);
        return dlg;
    }
}