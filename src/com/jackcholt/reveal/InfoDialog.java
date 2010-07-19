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
     * Constructs an information dialog.
     * 
     * @param parentContext parent Context
     * @param title title for the dialog
     * @param message message for the dialog
     */
    protected InfoDialog(final Context parentContext, String title, String message) {
        super(parentContext);
        setContentView(getContentViewId());

        findCloseButton().setOnClickListener(this);

        if (message != null) {
            findInfoMessageView().setText(Html.fromHtml(message));
        }

        if (title != null) {
            setTitle(title);
        }
    }

    private TextView findInfoMessageView() {
        return (TextView) findViewById(R.id.info_message);
    }

    private Button findCloseButton() {
        return (Button) findViewById(R.id.close_btn);
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
     * @param parentContext parent Context
     * @param title title for the dialog
     * @param message message for the dialog
     */
    public static void create(final Context parentContext, String title, String message) {
        new InfoDialog(parentContext, title, message).show();
    }

    /**
     * Displays information dialog with given title and message.
     * 
     * @param parentContext parent Context
     * @param titleId string resource id of title for the dialog
     * @param messageId string resource id of message for the dialog
     */
    public static void create(final Context parentContext, int titleId, int messageId) {
        create(parentContext, titleId, parentContext.getResources().getString(messageId));
    }

    /**
     * Displays information dialog with given title and message.
     * 
     * @param parentContext parent Context
     * @param titleId string resource id of title for the dialog
     * @param message message for the dialog
     */
    public static void create(final Context parentContext, int titleId, String message) {
        create(parentContext, parentContext.getResources().getString(titleId), message);
    }
}