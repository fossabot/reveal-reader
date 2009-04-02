package com.jackcholt.reveal;

import java.util.List;

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
 
        setContentView(R.layout.dialog_history);
        
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        YbkDAO ybkDao = YbkDAO.getInstance(this);
        
        List<History> data = ybkDao.getHistoryList();
        
        // Now create a simple cursor adapter and set it to display
        ArrayAdapter<History> histAdapter = 
                new ArrayAdapter<History>(this, R.layout.history_list_row, data);

        setListAdapter(histAdapter);
       
    }
	
	@Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, id);
        intent.putExtra(YbkDAO.FROM_HISTORY, true);
        setResult(RESULT_OK, intent);
        
        finish();
    }
 
        

}

			
			