package com.jackcholt.reveal;

import java.io.File;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;

/**
 * RefreshDialog for asking people to please wait for refresh
 * 
 * by Dave Packham
 */


public class DeleteEbookDialog extends Dialog {
    public DeleteEbookDialog(Context _this, int DELETE_ID) {
        super(_this);
 /*       setContentView(R.layout.delete_ebook);
        
        final ContentResolver res = Main.getMainApplication().getContentResolver();
        
        Button delete = (Button) findViewById(R.id.delte_ebook_btn);
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Delete the book
    			AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
    			long bookId = menuInfo.id;
    			Uri thisBookUri = ContentUris.withAppendedId(mBookUri, bookId);
    			Cursor bookCurs = managedQuery(thisBookUri, new String[] { YbkProvider.FILE_NAME },
    					null, null, null);

    			String fileName = bookCurs.moveToFirst() ? bookCurs.getString(0) : null;

    			if (fileName != null) {
    				File file = new File(fileName);
    				if (file.exists()) {
    					file.delete();
    				}

    				res.delete(thisBookUri, null, null);
    				//refreshBookList();
    			}
            }
        });
        Button close = (Button) findViewById(R.id.dont_delte_ebook_btn);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
        FlurryAgent.onEvent("DeleteEbook");
        setTitle(R.string.really_delete);
        
        //menu.add(0, DELETE_ID, 0, R.string.really_delete);
*/     
    }

    public static DeleteEbookDialog create(Context _this, int DELETE_ID) {
    	DeleteEbookDialog dlg = new DeleteEbookDialog(_this, DELETE_ID);
        dlg.show();
        return dlg;
    }
}