package com.jackcholt.reveal;

import java.io.IOException;
import java.util.List;

import com.flurry.android.FlurryAgent;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;
import com.nullwire.trace.ExceptionHandler;

public class BookmarkDialog extends ListActivity {
    private final String TAG = "BookmarkDialog";
    public static final String ADD_BOOKMARK = "add_bookmark";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
    
            ExceptionHandler.register(this, "http://revealreader.thepackhams.com/exception.php");
    
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
            FlurryAgent.onEvent("BookMarkDialog");
    
            Bundle extras = getIntent().getExtras();
    
            setContentView(R.layout.dialog_bookmark);
    
            Button addBtn = (Button) findViewById(R.id.addBMButton);
    
            if (extras != null && extras.getBoolean("fromMain") == true) {
                addBtn.setVisibility(View.GONE);
            }
    
            try {
                YbkDAO ybkDao = YbkDAO.getInstance(this);
    
                List<History> data = ybkDao.getBookmarkList();
    
                // Now create a simple array adapter and set it to display
                ArrayAdapter<History> histAdapter = new ArrayAdapter<History>(this, R.layout.history_list_row, data);
    
                setListAdapter(histAdapter);
    
                addBtn.setOnClickListener(new OnClickListener() {
    
                    public void onClick(final View view) {
    
                        Log.d(TAG, "Adding a bookmark");
    
                        Intent intent = new Intent(getBaseContext(), YbkViewActivity.class);
                        intent.putExtra(ADD_BOOKMARK, true);
                        setResult(RESULT_OK, intent);
    
                        finish();
                    }
    
                });
            } catch (IOException ioe) {
                // TODO - add friendly message
                Util.displayError(this, ioe, null);
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        try {
            Log.d(TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
            History hist = (History) listView.getItemAtPosition(selectionRowId);
    
            Intent intent = new Intent(this, YbkViewActivity.class);
            intent.putExtra(YbkDAO.ID, hist.id);
            intent.putExtra(YbkDAO.FROM_HISTORY, true);
            setResult(RESULT_OK, intent);
    
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
