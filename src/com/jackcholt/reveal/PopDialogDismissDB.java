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
     * @param _this
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
     * @param _this
     * @param DialogName
     * @param DialogNumber
     */
    public static void addDismissedDialog(Context _this, String DialogName, int DialogNumber) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("dialogName", DialogName + DialogNumber);
        initialValues.put("dismissed", "1");

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
     * @param _this 
     * @param DialogName
     */
    public void checkForDialogDismissed(Context _this, String DialogName) {
        try {
            db = _this.openOrCreateDatabase(DATABASE_NAME, 0, null);
            //db.equals(TABLE_DIALOGS, "id=" + Id, null);
        } catch (SQLException e) {
            Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
        } finally {
            db.close();
        }
    }
}
