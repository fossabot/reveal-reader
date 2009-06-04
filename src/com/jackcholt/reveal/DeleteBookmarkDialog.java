package com.jackcholt.reveal;

import java.io.File;
import java.text.MessageFormat;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.Book;

/**
 * DeleteEbookDialog for asking people to please confirm the delete
 * 
 * @author Dave Packham
 * @author Shon Vella
 */

public class DeleteBookmarkDialog extends Dialog {
    public DeleteBookmarkDialog(final Context _this, final Book book, final ArrayAdapter<Book> bookListAdapter) {
        super(_this);
        setContentView(R.layout.delete_ebook);

        Button delete = (Button) findViewById(R.id.delte_ebook_btn);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    File file = new File(book.fileName);
                    if (file.exists()) {
                        if (!file.delete()) {
                            // TODO - should tell user about this
                        }
                    }
                    YbkService.requestRemoveBook(_this, book.fileName);
                    bookListAdapter.remove(book);
                    dismiss();
                } catch (RuntimeException rte) {
                    Util.unexpectedError(_this, rte);
                } catch (Error e) {
                    Util.unexpectedError(_this, e);
                }

            }
        });

        Button close = (Button) findViewById(R.id.dont_delte_ebook_btn);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        TextView titleView = (TextView) findViewById(R.id.confirm_delete_ebook);
        String title = _this.getResources().getString(R.string.confirm_delete_ebook);
        title = MessageFormat.format(title, book.title, new File(book.fileName).getName());
        titleView.setText(title);

        FlurryAgent.onEvent("DeleteEbook");
        setTitle(R.string.really_delete_title);
    }

    public static DeleteEbookDialog create(final Context _this, final Book book,
            final ArrayAdapter<Book> bookListAdapter) {
        DeleteEbookDialog dlg = new DeleteEbookDialog(_this, book, bookListAdapter);
        dlg.show();
        return dlg;
    }
}