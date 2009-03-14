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
            //Is Network up or not?
            if (Util.isNetworkUp(_this)) {
	            if (!Global.DEBUGGING) FlurryAgent.onEvent("OnlineHelp");
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
    		HelpDialog dlg = new HelpDialog(_this);
            return dlg;
    }
    
};