package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.webkit.WebView;

import com.flurry.android.FlurryAgent;

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
            wv.loadUrl("http://sites.google.com/site/revealonlinehelp/");
        } else {
            wv.loadData("Cannot get online help.  Your network is currently down.", "text/plain", "utf-8");
        }
        
        show();

    }

    public static OnlineHelpDialog create(Context _this) {
        OnlineHelpDialog dlg = new OnlineHelpDialog(_this);
        return dlg;
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        
        super.onStop();
    }
}