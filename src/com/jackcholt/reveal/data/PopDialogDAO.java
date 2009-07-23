package com.jackcholt.reveal.data;

import com.jackcholt.reveal.Main;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Checks for updates to the DB for Dismissable Dialogs
 * 
 * by Dave Packham
 */

public class PopDialogDAO extends SQLiteOpenHelper {

    private static final String TAG = "PopDialogDAO";
    private static SQLiteDatabase db;
    private static String dName;
    private static String tName;
    private static String sql;
    public static final String KEY_ID = "_id";

    private static PopDialogDAO instance;
    
    private PopDialogDAO(Context ctx, String dbName, String sql, String tableName, int ver){
        super(ctx, dbName, null, ver);
        Log.i(TAG, "Creating or opening database [ " + dbName + " ].");
        PopDialogDAO.sql = sql;
        dName = dbName;
        tName = tableName;
    }
    
    public static PopDialogDAO getInstance(Context ctx, String dbName, String sql, String tableName, int ver){
        if(instance == null){
            instance = new PopDialogDAO(ctx, dbName, sql, tableName, ver);
            try{
                Log.i(TAG, "Creating or opening the database [ " + dbName + " ].");
                db = instance.getWritableDatabase();
            }catch(SQLiteException se){
                Log.e(TAG, "Cound not create and/or open the database [ " + dbName + " ] that will be used for reading and writing.", se);
            }
        }
        return instance;
    }

    public void close(){
        if(instance != null){
            Log.i(TAG, "Closing the database [ " + dName + " ].");
            db.close();
            instance = null;
        }
    }
    
    @Override
    public void onCreate(SQLiteDatabase db){
        Log.i(TAG, "Trying to create database table if it isn't existed [ " + sql + " ].");
        try{
            db.execSQL(sql);
        }catch(SQLException se){
            Log.e(TAG, "Cound not create the database table according to the SQL statement [ " + sql + " ].", se);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
       Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
       try{
           db.execSQL("DROP TABLE IF EXISTS " + tName);
       }catch(SQLException se){
            Log.e(TAG, "Cound not drop the database table [ " + tName + " ].", se);
        }
       onCreate(db);
    }

    public long insert(String table, ContentValues values){
        return db.insert(table, null, values);
    }
    
    public Cursor get(String table, String[] columns){
        return db.query(table, columns, null, null, null, null, null);
    }
    
    public Cursor get(String table, String[] columns, String query){ 
        return db.query(table, columns, query, null, null, null, null); 
    } 
    
    public Cursor get(String table, String[] columns, long id){
        Cursor cursor =db.query(true, table, columns, KEY_ID + "=" + id, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }
    
    public int delete(String table) {
        return db.delete(table, "1", null);
    }
    
    public int delete(String table, long id) {
        return db.delete(table, KEY_ID + "=" + id, null);
    }
    
    public int update(String table, long id, ContentValues values) {
        return db.update(table, values, KEY_ID + "=" + id, null);
    }
    
    public boolean isMyDialogDismissed(String dlgName) {
        PopDialogDAO dao = PopDialogDAO.getInstance(Main.getMainApplication(), PopDialogCheck.DATABASE_NAME,
                PopDialogCheck.TABLE_CREATE, PopDialogCheck.DATABASE_TABLE, PopDialogCheck.DATABASE_VERSION);

        Cursor c = dao.get(PopDialogCheck.DATABASE_TABLE, new String[] {"dismissed"}, "dialogname='" + dlgName + "' and dismissed= '1'");

        if (c != null) {
            boolean cCount = (c.getCount() == 1);
            c.close();
            return cCount;
        }
        c.close();
        return false;
    }
}
