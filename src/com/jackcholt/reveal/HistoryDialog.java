package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryDialog extends Dialog implements View.OnClickListener {
	String[] items={"blah", "blah2"};
		TextView selection;
		
        public HistoryDialog(Context context) {
        	
        	super(context);
        	setListAdapter(new ArrayAdapter<String>(getContext(),
        		R.id.historylist,
        			items));
        	setContentView(R.layout.history);
        	selection=(TextView)findViewById(R.id.selection);
        	
        	Button btn = (Button)findViewById(R.id.button1);
        	btn.setOnClickListener(this);
        	
        }
        
        
		

		private void setListAdapter(ArrayAdapter<String> arrayAdapter) {
			// TODO Auto-generated method stub
			
		}




		public void onListItemClick(ListView parent, View v, int position,
        		long id) {
        		selection.setText(items[position]);
        		}
        public static HistoryDialog create(Context context) {
                HistoryDialog dlg = new HistoryDialog(context);
                dlg.show();
                return dlg;
        }

		@Override
		public void onClick(View v) {
			HistoryDialog.this.cancel();
			
		}


}
