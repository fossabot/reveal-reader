package com.jackcholt.reveal;

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
        super.onCreate(savedInstanceState);
		// Change DEBUG to "0" in Global.java when building a RELEASE Version for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0 ) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(Main.getMainApplication(), "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(Main.getMainApplication(), "VYRRJFNLNSTCVKBF73UP");
		}
		FlurryAgent.onEvent("HistoryDialog");
		
        setContentView(R.layout.dialog_history);
        
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        YbkDAO ybkDao = YbkDAO.getInstance(this);
        
        List<History> data = ybkDao.getHistoryList();
        
        // Now create a simple array adapter and set it to display
        ArrayAdapter<History> histAdapter = 
                new ArrayAdapter<History>(this, R.layout.history_list_row, data);

        setListAdapter(histAdapter);
       
    }
	
	@Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        History hist = (History) listView.getItemAtPosition(selectionRowId);

        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, hist.id);
        intent.putExtra(YbkDAO.FROM_HISTORY, true);
        setResult(RESULT_OK, intent);
        
        finish();
    }
	
	/** Called when the activity is going away. */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(Main.getMainApplication());
	}
}

			
			