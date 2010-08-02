package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


/**
 * RefreshDialog for asking people to please wait for refresh
 * 
 * by Dave Packham
 */

public class RefreshDialog extends Dialog {
    // int values for reusable dialogs
    public static final int REFRESH_DB = 0;
    public static final int UPGRADE_DB = 1;

    public RefreshDialog(Context _this, int mode) {
        super(_this);
        setContentView(R.layout.dialog_refresh_ebooks_wait);
        TextView messageView = (TextView) findViewById(R.id.waitMessage);
        Button closeButton = (Button) findViewById(R.id.close_about_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        switch (mode) {
        case REFRESH_DB:
            // Normal refresh Dialog
            messageView.setText(R.string.wait_for_refresh);
            setTitle(R.string.refresh_title);
            break;

        case UPGRADE_DB:
            // Upgrade Dialog
            messageView.setText(R.string.wait_for_refresh);
            setTitle(R.string.upgrade_database);
            break;
        }
    }

    public static RefreshDialog create(Context _this, int mode) {
        RefreshDialog dlg = new RefreshDialog(_this, mode);
        dlg.show();
        return dlg;
    }
}