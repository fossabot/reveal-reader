package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

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
        setContentView(R.layout.refresh_ebooks_wait);
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
            FlurryAgent.onEvent("RefreshTitles");
            break;

        case UPGRADE_DB:
            // Upgrade Dialog
            messageView.setText(R.string.wait_for_refresh);
            setTitle(R.string.upgrade_database);
            FlurryAgent.onEvent("UpgradeDatabase");
            break;
        }
    }

    public static RefreshDialog create(Context _this, int mode) {
        RefreshDialog dlg = new RefreshDialog(_this, mode);
        // Change DEBUG to "0" in Global.java when building a RELEASE Version
        // for the GOOGLE APP MARKET
        // This allows for real usage stats and end user error reporting
        if (Global.DEBUG == 0) {
            // Release Key for use of the END USERS
            FlurryAgent.onStartSession(_this, "BLRRZRSNYZ446QUWKSP4");
        } else {
            // Development key for use of the DEVELOPMENT TEAM
            FlurryAgent.onStartSession(_this, "VYRRJFNLNSTCVKBF73UP");
        }
        dlg.show();
        return dlg;
    }
}