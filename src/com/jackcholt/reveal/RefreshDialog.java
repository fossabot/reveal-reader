package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

/**
 * RefreshDialog for asking people to please wait for refresh
 * 
 * by Dave Packham
 */

public class RefreshDialog extends Dialog {
    public RefreshDialog(Context _this, int mode) {
        super(_this);
        
        switch (mode) {
        case 0:
            // Normal refresh Dialog
            setContentView(R.layout.refresh_ebooks_wait);
    
            Button close1 = (Button) findViewById(R.id.close_about_btn);
            close1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dismiss();
                }
            });
            FlurryAgent.onEvent("RefreshTitles");
            setTitle(R.string.refresh_title);
            return;
            
        case 1:
            // Upgrade Dialog
            setContentView(R.layout.update_db_wait);
            
            Button close2 = (Button) findViewById(R.id.close_about_btn);
            close2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dismiss();
                }
            });
            FlurryAgent.onEvent("UpgradeDatabase");
            setTitle(R.string.upgrade_database);
            return;
        }
    }

    public static RefreshDialog create(Context _this, int mode) {
        RefreshDialog dlg = new RefreshDialog(_this, mode);
        dlg.show();
        return dlg;
    }
}