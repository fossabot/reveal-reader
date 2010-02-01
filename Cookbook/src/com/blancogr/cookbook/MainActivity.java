package com.blancogr.cookbook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final String PREFS_NAME = "cookbookPrefs";
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//final Context context = getApplicationContext();
		setContentView(R.layout.main);
    
//		TextView topNavTitle = (TextView)findViewById(R.id.top_nav_title);
//        topNavTitle.setText(R.string.app_name);
//        
        
//        ImageButton next = new ImageButton(this);
//        next.setImageResource(R.drawable.btn_next);
//        next.setScaleType(ScaleType.CENTER_INSIDE);
//        next.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//          //do stuff here
//        }
//      });
    }
    
   
   
/*   public void getNames() {
	   DBHandler dbHandler = new DBHandler(context);
       dbHandler.createDatabase();
       
       Cursor c = dbHandler.getRecipeNames();
       String str = "";
       while (c.isAfterLast() != true) {
       	str += "\n";
       	str += c.getString(0);
       	str += ": ";
       	str += c.getString(1);
       	c.moveToNext();
       }
       
       Toast.makeText(this, str, Toast.LENGTH_LONG);
      
   }
   */
}