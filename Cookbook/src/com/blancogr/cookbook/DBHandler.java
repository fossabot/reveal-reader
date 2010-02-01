package com.blancogr.cookbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHandler extends SQLiteOpenHelper {
	private static String DB_PATH = "/data/data/com.blancogr.cookbook/databases/";
	private static String DB_NAME = "recipes";
	private static String DB_FULL_NAME = DB_PATH + DB_NAME;
	private SQLiteDatabase sqldb; 
	private final Context context;
	
	public DBHandler(Context ctx) {
		super(ctx, DB_NAME, null, 1);
        this.context = ctx;
    }
	@Override
	public void onCreate(SQLiteDatabase DB_NAME) {
		createDatabase();
	}
	@Override
	public synchronized void close() {
		if(sqldb != null) {
			sqldb.close();
		}
		super.close();
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (context != null) {
			context.deleteDatabase(DB_NAME);
			createDatabase();
		}
		else {
			createDatabase();
		}
	}
	
	public void createDatabase() throws Error {
		// Always start with a fresh db
		if (context != null) {
			context.deleteDatabase(DB_NAME);
		}
		try{
    		sqldb = SQLiteDatabase.openDatabase(DB_FULL_NAME, null, SQLiteDatabase.OPEN_READWRITE);
    	}
		catch(SQLiteException e){ 
    		//database does't exist yet. Create it, copy bytes to the file
			//this.getReadableDatabase();
			try {
				InputStream dbFile = context.getAssets().open(DB_NAME);
				OutputStream outputStr = new FileOutputStream(DB_FULL_NAME);
				byte[] buffer = new byte[1024];
		    	int length;
		    	while ((length = dbFile.read(buffer))>0){
		    		outputStr.write(buffer, 0, length);
		    	}
		    	outputStr.flush();
		    	outputStr.close();
		    	dbFile.close();
		    	sqldb = SQLiteDatabase.openDatabase(DB_FULL_NAME, null, SQLiteDatabase.OPEN_READWRITE);
     		} 
			catch (IOException ex) {
         		throw new Error("Error copying database: " + ex.getMessage());
         	}
     		catch (SQLiteException ex) {
     			throw new Error("Couldn't open DB for reading/writing" + ex.getMessage());
     		}
     	}
		
		
	}	
	public Cursor getRecipeNames() {
		String[] columns={"_id", "recipe_name", "photo"};
				
		Cursor mCursor = sqldb.query("recipes", columns, null, null, null, null, null);
		if (mCursor != null) {
            mCursor.moveToFirst();
        }
		return mCursor;
	}
}
