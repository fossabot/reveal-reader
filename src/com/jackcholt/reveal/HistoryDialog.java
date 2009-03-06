package com.jackcholt.reveal;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class HistoryDialog extends ListActivity {
    private Cursor mListCursor;
	
	@Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        setTheme(android.R.style.Theme_Dialog);
        
        mListCursor = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "history"), 
                new String[] {YbkProvider.HISTORY_TITLE, YbkProvider._ID}, 
                null, null,
                YbkProvider.CREATE_DATETIME + " DESC");
        
        // Load the layout
         
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[] {YbkProvider.HISTORY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        //int[] to = new int[] {R.id.historyText};
        
        // Now create a simple cursor adapter and set it to display
        //SimpleCursorAdapter historyAdapter = 
               // new SimpleCursorAdapter(this, R.layout.history_list_row, mListCursor, from, to);
        
        //setListAdapter(historyAdapter);
       
    }
	
	@Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkProvider._ID, id);
        intent.putExtra(YbkProvider.FROM_HISTORY, true);
        //startActivityIfNeeded(intent, 0);
        setResult(RESULT_OK, intent);
        
        finish();
    }
 
        

}

			
			