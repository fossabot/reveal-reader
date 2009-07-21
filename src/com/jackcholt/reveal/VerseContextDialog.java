package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;

public class VerseContextDialog extends ListActivity {
    private static final int ANNOTATE_ID = 1;
    private static final int HIGHLIGHT_ID = 2;
    private static final int GOTO_TOP_ID = 3;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            // Change DEBUG to "0" in Global.java when building a RELEASE Version
            // for the GOOGLE APP MARKET
            // This allows for real usage stats and end user error reporting
            if (Global.DEBUG == 0) {
                // Release Key for use of the END USERS
                FlurryAgent.onStartSession(Main.getMainApplication(), "BLRRZRSNYZ446QUWKSP4");
            } else {
                // Development key for use of the DEVELOPMENT TEAM
                FlurryAgent.onStartSession(Main.getMainApplication(), "VYRRJFNLNSTCVKBF73UP");
            }
            FlurryAgent.onEvent("VerseContextDialog");

            setContentView(R.layout.view_verse_context);


                List<String> data = new ArrayList<String>();

                data.add(this.getResources().getString(R.string.menu_annotate));
                data.add(this.getResources().getString(R.string.menu_highlight));
                data.add(this.getResources().getString(R.string.menu_goto_top));
                
                // Now create a simple array adapter and set it to display
                ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(this, R.layout.verse_menu_row, data);

                setListAdapter(menuAdapter);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        try {
            Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
            
            switch (selectionRowId) {
                
            }

            /*Intent intent = new Intent(this, YbkViewActivity.class);
            intent.putExtra(YbkDAO.ID, hist.id);
            intent.putExtra(YbkDAO.FROM_HISTORY, true);
            setResult(RESULT_OK, intent);*/

            finish();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
            FlurryAgent.onEndSession(Main.getMainApplication());
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
