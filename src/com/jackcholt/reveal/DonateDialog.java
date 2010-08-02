package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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


        WebView wv = (WebView) findViewById(R.id.donateView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);
        if (Util.areNetworksUp(_this)) {
            Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7668278");
            _this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else {
            wv.loadData("Cannot get to the Donation website.  Your network is currently down.", "text/plain", "utf-8");
        }

        //show();

    }

    public static DonateDialog create(Context _this) {
        DonateDialog dlg = new DonateDialog(_this);
        return dlg;
    }
}