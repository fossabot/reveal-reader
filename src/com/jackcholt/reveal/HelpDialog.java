package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.LinearLayout;


/**
 * HelpDialog for online HELP system
 * 
 * by Dave Packham
 */

public class HelpDialog extends Dialog {
    public HelpDialog(Context _this) {
            super(_this);
            setContentView(R.layout.help);
         
        	WebView wv = new WebView(_this);
    	    wv.clearCache(true);
    	    wv.getSettings().setJavaScriptEnabled(true);
    	    wv.loadUrl("http://revealreader.thepackhams.com/revealHelp.html");
    		
    		Dialog dialog = new Dialog(_this) {
    			public boolean onKeyDown(int keyCode, KeyEvent event){
    				if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT)
    					this.dismiss();
    					return true;
    				}
    		};
    		
    		dialog.addContentView(wv, new LinearLayout.LayoutParams(  
    				                      LinearLayout.LayoutParams.WRAP_CONTENT,  
    				                      LinearLayout.LayoutParams.FILL_PARENT));
          	String title;
          	title = "Reveal Reader Help";
			setTitle(title);
   
    		dialog.show();
      
    }

    public static HelpDialog create(Context _this) {
    		HelpDialog dlg = new HelpDialog(_this);
            return dlg;
    }
};