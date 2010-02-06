package com.jackcholt.reveal;

import java.io.IOException;
import java.util.List;

import com.flurry.android.FlurryAgent;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

public class HistoryDialog extends ListActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Util.startFlurrySession(this);
            FlurryAgent.onEvent("HistoryDialog");

            setContentView(R.layout.dialog_history);

            try {
                YbkDAO ybkDao = YbkDAO.getInstance(this);

                List<History> data = ybkDao.getHistoryList();

                // Now create a simple array adapter and set it to display
                ArrayAdapter<History> histAdapter = new ArrayAdapter<History>(this, R.layout.history_list_row, data);

                setListAdapter(histAdapter);
            } catch (IOException ioe) {
                // TODO - add friendly message
                Util.displayError(this, ioe, getResources().getString(R.string.error_history_load));
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
            Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
            History hist = (History) listView.getItemAtPosition(selectionRowId);

            Intent intent = new Intent(this, YbkViewActivity.class);
            intent.putExtra(YbkDAO.HISTORY_ID, hist.id);
            setResult(RESULT_OK, intent);

            finish();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    @Override
    protected void onStart() {
        try {
            Util.startFlurrySession(this);
            super.onStart();
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
            FlurryAgent.onEndSession(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
