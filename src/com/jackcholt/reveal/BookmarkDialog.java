package com.jackcholt.reveal;

import java.util.List;

import com.jackcholt.reveal.data.History;
import com.jackcholt.reveal.data.YbkDAO;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class BookmarkDialog extends ListActivity {
    private Cursor mListCursor;
	//private Button mAddBtn;
	private final String TAG = "BookmarkDialog";
	public static final String ADD_BOOKMARK = "add_bookmark";
	
	
	@Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras();
        
        setContentView(R.layout.dialog_bookmark);
        
        Button addBtn = (Button) findViewById(R.id.addBMButton);

        if (extras != null && extras.getBoolean("fromMain") == true) {
            addBtn.setVisibility(View.GONE);
        }

        YbkDAO ybkDao = YbkDAO.getInstance(this);
        
        List<History> data = ybkDao.getBookmarkList();
        
        
        // Now create a simple cursor adapter and set it to display
        ArrayAdapter<History> histAdapter = 
                new ArrayAdapter<History>(this, R.layout.history_list_row, data);

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

       
    }
	
	@Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        Log.d(TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        History hist = (History) listView.getItemAtPosition(selectionRowId);

        Intent intent = new Intent(this, YbkViewActivity.class);
        intent.putExtra(YbkDAO.ID, hist.id);
        intent.putExtra(YbkDAO.FROM_HISTORY, true);
        setResult(RESULT_OK, intent);
        
        finish();
    }
 
        

}

			
			