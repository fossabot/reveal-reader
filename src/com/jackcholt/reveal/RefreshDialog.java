package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

/**
 * RefreshDialog for asking people to please wait for refresh
 * 
 * by Dave Packham
 */

public class RefreshDialog extends Dialog {
        public RefreshDialog(Context _this) {
                super(_this);
                setContentView(R.layout.refresh_ebooks_wait);

                Button close = (Button) findViewById(R.id.close_about_btn);
                close.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                dismiss();
                        }
                });
                FlurryAgent.onEvent("RefreshTitles");
                setTitle(R.string.refresh_title);
        }

        public static RefreshDialog create(Context _this) {
        		RefreshDialog dlg = new RefreshDialog(_this);
                dlg.show();
                return dlg;
        }
}