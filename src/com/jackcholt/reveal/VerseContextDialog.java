package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jackcholt.reveal.data.YbkDAO;

public class VerseContextDialog extends ListActivity {
    public static final int ANNOTATE_ID = 0;
    public static final int GOTO_TOP_ID = 1;
    public static final String MENU_ITEM_TAG = "menu_item_id";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);

            setContentView(R.layout.view_verse_context);

            createListMenu();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    private void createListMenu() {
        List<String> menuItemStrings = new ArrayList<String>();

        menuItemStrings.add(this.getResources().getString(R.string.menu_annotate));
        menuItemStrings.add(this.getResources().getString(R.string.menu_goto_top));

        setListAdapter(new ArrayAdapter<String>(this, R.layout.verse_menu_row, menuItemStrings));
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);

        setResult(RESULT_OK, new Intent().putExtra(MENU_ITEM_TAG, selectionRowId).putExtra(YbkDAO.VERSE,
                getIntent().getIntExtra(YbkDAO.VERSE,-1)).putExtra(YbkDAO.BOOK_FILENAME,
                getIntent().getStringExtra(YbkDAO.BOOK_FILENAME)).putExtra(YbkDAO.CHAPTER_FILENAME,
                getIntent().getStringExtra(YbkDAO.CHAPTER_FILENAME)));
        finish();
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
