package com.jackcholt.reveal;

import java.io.IOException;

import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Adapter;
import android.widget.TextView;

public class HistoryDialog extends ListActivity {

	
	TextView selection;
	
	@Override
	
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Main main = new Main();
        
        String[] mystring = main.openBooks;
        
        // Load the layout
 
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mystring));
        
        selection=(TextView)findViewById(R.id.historylist);
        
      
       
    }
    
 
        

}

			
			