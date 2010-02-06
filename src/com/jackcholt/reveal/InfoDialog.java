package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Generic reusable information dialog.
 * 
 * @author Shon Vella
 */

public class InfoDialog extends Dialog implements View.OnClickListener {

    /**
     * Constructs an information dialag.
     * 
     * @param _this
     *            parent Context
     * @param title
     *            title for the dialog
     * @param message
     *            message for the dialog
     */
    protected InfoDialog(final Context _this, String title, String message) {
        super(_this);
        setContentView(getContentViewId());

        Button closeBtn = (Button) findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(this);

        if (message != null)
            ((TextView) findViewById(R.id.info_message)).setText(Html.fromHtml(message));

        if (title != null)
            setTitle(title);
    }
    
    /**
     * Gets the contentview resource ID (this is to allow subclasses to use a different contentview.
     *  
     * @return the resource id
     */
    protected int getContentViewId() {
        return R.layout.dialog_info;
    }

    public void onClick(View v) {
        dismiss();
    }

    /**
     * Displays information dialog with given title and message.
     * 
     * @param _this
     *            parent Context
     * @param title
     *            title for the dialog
     * @param message
     *            message for the dialog
     */
    public static void create(final Context _this, String title, String message) {
        new InfoDialog(_this, title, message).show();
    }

    /**
     * Displays information dialog with given title and message.
     * 
     * @param _this
     *            parent Context
     * @param title
     *            string resource id of title for the dialog
     * @param message
     *            string resource id of message for the dialog
     */
    public static void create(final Context _this, int title, int message) {
        new InfoDialog(_this, _this.getResources().getString(title), _this.getResources().getString(message)).show();
    }

    /**
     * Displays information dialog with given title and message.
     * 
     * @param _this
     *            parent Context
     * @param title
     *            string resource id of title for the dialog
     * @param message
     *            message for the dialog
     */
    public static void create(final Context _this, int title, String message) {
        new InfoDialog(_this, _this.getResources().getString(title), message).show();
    }
}