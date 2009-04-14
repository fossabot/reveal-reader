package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.flurry.android.FlurryAgent;


/**
 * ErrorDialog for informing users of something that happening
 * 
 * by Dave Packham
 */

public class ErrorDialog extends Dialog {
	public ErrorDialog(Context _this) {
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
		FlurryAgent.onEvent("ErrorDialog");
        setContentView(R.layout.error_dialog);

        Button close = (Button) findViewById(R.id.close_about_btn);
        close.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                        dismiss();
                }
        });
        FlurryAgent.onEvent("ErrorDialog");
        setTitle(R.string.error_title);
    }

	public static ErrorDialog create(Context _this) {
		ErrorDialog dlg = new ErrorDialog(_this);
		dlg.show();
        return dlg;
	}
}