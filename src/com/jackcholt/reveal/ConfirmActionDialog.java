package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Generic reusable confirmation dialog.
 * 
 * @author Shon Vella
 */

public class ConfirmActionDialog extends Dialog implements View.OnClickListener {

    /**
     * Constructs a confirm dialag.
     * 
     * @param _this parent Context
     * @param title title for the dialog
     * @param message message for the dialog
     * @param okButtonLabel label for the ok button
     * @param action action to perform if confirmed
     */
    private ConfirmActionDialog(final Context _this, String title, String message, String okButtonLabel,
            final SafeRunnable action) {
        super(_this);
        setContentView(R.layout.dialog_confirm_action);

        ((Button) findViewById(R.id.ok_btn)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                action.run();
                dismiss();
            }
        });

        if (okButtonLabel != null) {
            ((Button) findViewById(R.id.ok_btn)).setText(okButtonLabel);
        }

        ((Button) findViewById(R.id.cancel_btn)).setOnClickListener(this);

        if (message != null) {
            ((TextView) findViewById(R.id.confirm_message)).setText(message);
        }

        if (title != null) {
            setTitle(title);
        }
    }

    public void onClick(View v) {
        dismiss();
    }

    /**
     * Performs an action if confirmed by user.
     * 
     * @param _this parent Context
     * @param title title for the dialog
     * @param message message for the dialog
     * @param okButtonLabel label for the ok button
     * @param action action to perform if confirmed
     */
    public static void confirmedAction(final Context _this, String title, String message, String okButtonLabel,
            SafeRunnable action) {
        new ConfirmActionDialog(_this, title, message, okButtonLabel, action).show();
    }

    /**
     * Performs an action if confirmed by user.
     * 
     * @param _this parent Context
     * @param title string resource id of title for the dialog
     * @param message string resource id of message for the dialog
     * @param okButtonLabel string resource id of label for the ok button
     * @param action action to perform if confirmed
     */
    public static void confirmedAction(final Context _this, int title, int message, int okButtonLabel,
            SafeRunnable action) {
        new ConfirmActionDialog(_this, _this.getResources().getString(title), _this.getResources().getString(message),
                _this.getResources().getString(okButtonLabel), action).show();
    }

    /**
     * Performs an action if confirmed by user.
     * 
     * @param _this parent Context
     * @param title string resource id of title for the dialog
     * @param message message for the dialog
     * @param okButtonLabel string resource id of label for the ok button
     * @param action action to perform if confirmed
     */
    public static void confirmedAction(final Context _this, int title, String message, int okButtonLabel,
            SafeRunnable action) {
        new ConfirmActionDialog(_this, _this.getResources().getString(title), message, _this.getResources().getString(
                okButtonLabel), action).show();
    }
}