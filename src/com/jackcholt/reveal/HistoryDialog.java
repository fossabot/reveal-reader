package com.jackcholt.reveal;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.TextView;

public class HistoryDialog extends ListActivity {

	
	
	TextView selection;
	
	@Override
	
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Main main = new Main();
        
        
        
        //String[] mystring = main.openBooks;
       
        // Load the layout
 
        //setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mystring));
        
        selection=(TextView)findViewById(R.id.historylist);
        
      
       
    }
    
 
        

}

			
			