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

import com.flurry.android.FlurryAgent;

public class VerseContextDialog extends ListActivity {
    public static final int ANNOTATE_ID = 0;
    public static final int HIGHLIGHT_ID = 1;
    public static final int GOTO_TOP_ID = 2;
    public static final String MENU_ITEM_TAG = "menu_item_id";
    private static final String TAG = "VerseContextDialog";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Util.startFlurrySession(this);
            FlurryAgent.onEvent(TAG);

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
        List<String> data = new ArrayList<String>();

        data.add(this.getResources().getString(R.string.menu_annotate));
        data.add(this.getResources().getString(R.string.menu_highlight));
        data.add(this.getResources().getString(R.string.menu_goto_top));

        setListAdapter(new ArrayAdapter<String>(this, R.layout.verse_menu_row, data));
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);

        setResult(RESULT_OK, new Intent().putExtra(MENU_ITEM_TAG, selectionRowId).putExtra("verseStartPos",
                getIntent().getExtras().getString("verseStartPos")).putExtra("bookFileName",
                getIntent().getExtras().getString("bookFileName")).putExtra("chapterName",
                getIntent().getExtras().getString("chapterName")));

        finish();
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
            FlurryAgent.onEndSession(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
