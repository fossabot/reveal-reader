package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

/**
 * Hidden eBook notification so people dont keep downloading it
 * 
 * by Dave Packham
 */

public class HiddenEBook extends Dialog {
    
    public HiddenEBook(Context _this) {
        super(_this);
        setContentView(R.layout.dialog_hidden_ebook);
        TextView messageView = (TextView) findViewById(R.id.hidden_ebook_textview);
        Button closeButton = (Button) findViewById(R.id.close_about_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
        messageView.setText(R.string.hidden_ebook);
        setTitle(R.string.hidden_ebook_title);
        FlurryAgent.onEvent("HiddenEBook");
    }

    public static HiddenEBook create(Context _this) {
        HiddenEBook dlg = new HiddenEBook(_this);
        dlg.show();
        return dlg;
    }
}