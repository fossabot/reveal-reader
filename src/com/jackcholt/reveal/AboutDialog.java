package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.widget.Button;

import com.flurry.android.FlurryAgent;


/**
 * AboutDialog for telling people who we be  :)
 * 
 * by Dave Packham
 */

public class AboutDialog extends Dialog {
	public AboutDialog(Context _this) {
	    super(_this);
        // Change DEBUG to "0" in Global.java when building a RELEASE Version for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0 ) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(Main.getMainApplication(), "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(Main.getMainApplication(), "VYRRJFNLNSTCVKBF73UP");
		}
		FlurryAgent.onEvent("AboutDialog");
	    setContentView(R.layout.dialog_about);
	
	    Button close = (Button) findViewById(R.id.close_about_btn);
	    close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    dismiss();
            }
	    });
	    String title;
	    try {
			title = getContext().getString(R.string.app_name) + 
			" : " + getContext().getPackageManager().getPackageInfo
			(getContext().getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
			         
			//Grab the Global updated version instead of a static one
			title += String.format(" %d", Global.SVN_VERSION);
			FlurryAgent.onEvent("AboutDialog");
			                                      
		    setTitle(title);
			 
	        } catch (NameNotFoundException e) {
	        	title = "Unknown version";
	    }
	}

	public static AboutDialog create(Context _this) {
		AboutDialog dlg = new AboutDialog(_this);
		dlg.show();
		return dlg;
    }
}