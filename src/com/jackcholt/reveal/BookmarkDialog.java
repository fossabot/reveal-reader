package com.jackcholt.reveal;

import java.io.IOException;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

public class BookmarkDialog extends ListActivity {
    private final String TAG = "BookmarkDialog";
    public static final String ADD_BOOKMARK = "add_bookmark";
    public static final String UPDATE_BOOKMARK = "update_bookmark";
    public static final String DELETE_BOOKMARK = "delete_bookmark";
    private static final int GOTO_BOOKMARK_ID = 1;
    private static final int UPDATE_BOOKMARK_ID = 2;
    private static final int DELETE_BOOKMARK_ID = 3;

    List<History> mData;
    ArrayAdapter<History> mHistAdapter;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            // Change DEBUG to "0" in Global.java when building a RELEASE
            // Version
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

            registerForContextMenu(getListView());

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
                Util.displayError(this, ioe, getResources().getString(R.string.error_bookmark_load));
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        try {
            super.onCreateContextMenu(menu, v, menuInfo);
            Bundle extras = getIntent().getExtras();
            
            menu.add(0, GOTO_BOOKMARK_ID, 0, R.string.goto_bookmark);
            if (extras != null && extras.getBoolean("fromMain") == false) {
                menu.add(0, UPDATE_BOOKMARK_ID, 0, R.string.update_bookmark);
            }
            menu.add(0, DELETE_BOOKMARK_ID, 0, R.string.delete_bookmark);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }
    
    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        History hist = (History) getListView().getItemAtPosition(menuInfo.position);
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, hist.id);

        try {
            
            switch (item.getItemId()) {
            case GOTO_BOOKMARK_ID:
                intent.putExtra(YbkDAO.FROM_HISTORY, true);
                setResult(RESULT_OK, intent);

                finish();
                break;
                
            case UPDATE_BOOKMARK_ID:
                intent.putExtra(UPDATE_BOOKMARK, true);
                setResult(RESULT_OK, intent);
                
                finish();
                break;
                
            case DELETE_BOOKMARK_ID:
                intent.putExtra(YbkDAO.BOOKMARK_NUMBER, hist.bookmarkNumber);
                intent.putExtra(DELETE_BOOKMARK, true);
                setResult(RESULT_OK, intent);
                
                finish();
                break;
                
            default:
                return super.onContextItemSelected(item);
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
        return true;
    }

}
