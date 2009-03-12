package com.jackcholt.reveal;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class BookmarkDialog extends ListActivity {
    private Cursor mListCursor;
	private Button mAddBtn;
	private final String TAG = "BookmarkDialog";
	public static final String ADD_BOOKMARK = "add_bookmark";
	
	
	@Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras();
        
        setContentView(R.layout.dialog_bookmark);
        
        Button addBtn = mAddBtn = (Button) findViewById(R.id.addBMButton);

        if (extras != null && extras.getBoolean("fromMain") == true) {
            addBtn.setVisibility(View.GONE);
        }

        mListCursor = managedQuery(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "bookmark"), 
                null, null, null, null);
        
        // Load the layout
         
        // Create an array to specify the fields we want to display in the list (only HISTORY_TITLE)
        String[] from = new String[] {YbkProvider.HISTORY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[] {R.id.historyText};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter historyAdapter = 
                new SimpleCursorAdapter(this, R.layout.history_list_row, mListCursor, from, to);
        
        setListAdapter(historyAdapter);
        
        addBtn.setOnClickListener(new OnClickListener() {

            public void onClick(final View view) {
                
                Log.d(TAG, "Adding a bookmark");
                
                Intent intent = new Intent(getBaseContext(), YbkViewActivity.class);
                intent.putExtra(ADD_BOOKMARK, true);
                setResult(RESULT_OK, intent);
                
                finish();
            }
            
        });

       
    }
	
	@Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        Log.d(TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        
        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkProvider.BOOKMARK_NUMBER, id);
        intent.putExtra(YbkProvider.FROM_HISTORY, true);
        setResult(RESULT_OK, intent);
        
        finish();
    }
 
        

}

			
			