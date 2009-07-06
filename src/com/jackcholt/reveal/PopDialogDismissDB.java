package com.jackcholt.reveal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Dave Packham Create the myPopupsViewedDB Sqlite DB to track if a user has
 * seen and dismissed this Popup
 * 
 * @return
 */
public class PopDialogDismissDB {

    private static final String DATABASE_NAME = "myPopupsViewedDB";
    private static final String TABLE_DBVERSION = "t_dbversion";
    private static final String TABLE_DIALOGS = "t_popUpDialogs";
    private static final int DATABASE_VERSION = 1;
    private static String TAG = "DBHelper";

    private static final String DBVERSION_CREATE = "create table " + TABLE_DBVERSION + " ("
            + "version integer not null);";

    private static final String DISMISS_DIALOG_CREATE = "create table if not exists" + TABLE_DIALOGS + " ("
            + "DialogName VARCHAR, " + "Dialog_Dismissed VARCHAR;";

    private static final String DIALOG_DB_DROP = "drop table " + TABLE_DIALOGS + ";";

    private static SQLiteDatabase db;

    /**
     * 
     * @param ctx
     * @return 
     * @return
     */
    public static final void DBCreate(Context _this) {

        try {
            db = _this.openOrCreateDatabase(DATABASE_NAME, 0, null);

            // Check for the existence of the DBVERSION table
            // If it doesn't exist than create the overall data,
            // otherwise double check the version
            Cursor c = db.query("sqlite_master", new String[] { "name" }, "type='table' and name='" + TABLE_DBVERSION
                    + "'", null, null, null, null);
            int numRows = c.getCount();
            if (numRows < 1) {
                CreateDatabase(db);
            } else {
                int version = 0;
                Cursor vc = db.query(true, TABLE_DBVERSION, new String[] { "version" }, null, null, null, null, null,
                        null);
                if (vc.getCount() > 0) {
                    vc.moveToFirst();
                    version = vc.getInt(0);
                }
                vc.close();
                if (version != DATABASE_VERSION) {
                    Log.e(TAG, "database version mismatch");
                }
            }
            c.close();

        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        } finally {
            db.close();
        }
    }

    private static void CreateDatabase(SQLiteDatabase db) {
        try {
            db.execSQL(DBVERSION_CREATE);
            ContentValues args = new ContentValues();
            args.put("version", DATABASE_VERSION);
            db.insert(TABLE_DBVERSION, null, args);

            db.execSQL(DISMISS_DIALOG_CREATE);
        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        }
    }

    public void deleteDatabase(Context _this) {
        try {
            db = _this.openOrCreateDatabase(DATABASE_NAME, 0, null);
            db.execSQL(DIALOG_DB_DROP);
            db.execSQL(DISMISS_DIALOG_CREATE);
        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        } finally {
            db.close();
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        /*
         * try { db.close(); } catch (SQLException e) {
         * Log.d(TAG,"close exception: " + e.getLocalizedMessage()); }
         */
    }

    /**
     * 
     * @param entry
     */
    public void addDismissedDialog(Context _this, String DialogName, String Dismissed) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("dialogName", DialogName);
        initialValues.put("dismissed", Dismissed);

        try {
            db = _this.openOrCreateDatabase(DATABASE_NAME, 0, null);
            db.insert(TABLE_DIALOGS, null, initialValues);
        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        } finally {
            db.close();
        }
    }

    /**
     * 
     * @param Id
     */
    public void deleteDialogDismissed(Context _this, long Id) {
        try {
            db = _this.openOrCreateDatabase(DATABASE_NAME, 0, null);
            db.delete(TABLE_DIALOGS, "id=" + Id, null);
        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        } finally {
            db.close();
        }
    }

    /**
     * 
     * @param Id
     * @return
     */
    /*
     * public NoteEntry fetchNote(Context _this, int Id) { NoteEntry row = new
     * NoteEntry(); try { db = _this.openOrCreateDatabase(DATABASE_NAME,
     * 0,null); Cursor c = db.query(true, TABLE_DIALOGS, new String[] { "id",
     * "description", "note", "category"}, "id=" + Id, null, null, null, null,
     * null); if (c.getCount() > 0) { c.moveToFirst(); row.id = c.getInt(0);
     * row.description = c.getString(2); row.note = c.getString(5); } else {
     * row.id = -1; } c.close(); } catch (SQLException e) {
     * Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage()); } finally {
     * db.close(); } return row; }
     * 
     * /**
     * 
     * @param Id
     * 
     * @param entry
     */
    /*
     * public void updateDialogDismiss(Context _this, long Id, String
     * DialogName) { ContentValues args = new ContentValues();
     * args.put("description", entry.description); args.put("note", entry.note);
     * 
     * try { db = _this.openOrCreateDatabase(DATABASE_NAME, 0,null);
     * db.update(TABLE_DIALOGS, args, "id=" + Id, null); } catch (SQLException
     * e) { Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage()); } finally
     * { db.close(); } }
     */
}
