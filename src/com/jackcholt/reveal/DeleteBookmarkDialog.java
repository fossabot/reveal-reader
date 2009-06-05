package com.jackcholt.reveal;

import java.text.MessageFormat;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.History;

/**
 * For asking people to confirm the delete.
 * 
 * @author Dave Packham
 * @author Shon Vella
 * @author Jack Holt
 */

public class DeleteBookmarkDialog extends Dialog {
    public DeleteBookmarkDialog(final Context _this, final History hist) {
        super(_this);
        setContentView(R.layout.delete_bookmark);

        Button delete = (Button) findViewById(R.id.delete_bookmark_btn);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {

                    YbkService.requestRemoveHistory(_this, hist);
                    dismiss();
                } catch (RuntimeException rte) {
                    Util.unexpectedError(_this, rte);
                } catch (Error e) {
                    Util.unexpectedError(_this, e);
                }

            }
        });

        Button close = (Button) findViewById(R.id.dont_delete_bookmark_btn);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        TextView titleView = (TextView) findViewById(R.id.confirm_delete_bookmark);
        String title = _this.getResources().getString(R.string.confirm_delete_bookmark);
        title = MessageFormat.format(title, hist.title);
        titleView.setText(title);

        FlurryAgent.onEvent("DeleteBookmark");
        setTitle(R.string.really_delete_bookmark);
    }

    public static DeleteBookmarkDialog create(final Context _this, final History hist) {
        DeleteBookmarkDialog dlg = new DeleteBookmarkDialog(_this, hist);
        dlg.show();
        return dlg;
    }
}