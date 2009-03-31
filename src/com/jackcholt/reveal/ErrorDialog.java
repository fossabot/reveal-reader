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