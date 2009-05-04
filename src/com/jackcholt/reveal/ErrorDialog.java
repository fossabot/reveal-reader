package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;


/**
 * ErrorDialog for informing users of something that happening
 * 
 * @author Dave Packham
 * @author Shon Vella
 */

public class ErrorDialog extends Dialog {
	public ErrorDialog(Context _this, Throwable t, String ...strings) {
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
        
        StringBuilder sb = new StringBuilder();
        
        if (strings != null && strings.length > 0) {
            for (String string : strings) {
                sb.append(string);
                sb.append('\n');
            }
        }
        if (t != null) {
            sb.append("stacktrace:\n");
            sb.append(Util.getStackTrace(t));
        }
        final String info = sb.toString();

        TextView confirmTextView = (TextView) findViewById(R.id.confirm_error_send_stacktrace);
        confirmTextView.setText(info);

        Button sendButton = (Button) findViewById(R.id.send_error_btn);
        sendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info);
                    dismiss();
                }
        });

        Button dontSendButton = (Button) findViewById(R.id.dont_send_error_btn);
        dontSendButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                        dismiss();
                }
        });
        FlurryAgent.onEvent("ErrorDialog");
        setTitle(R.string.error_title);
    }

	public static ErrorDialog create(Context _this, Throwable t, String ...strings) {
		ErrorDialog dlg = new ErrorDialog(_this, t, strings);
		dlg.show();
        return dlg;
	}
}