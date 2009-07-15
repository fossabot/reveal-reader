package com.jackcholt.reveal.data;

import android.database.Cursor;

import com.jackcholt.reveal.Main;

/**
 * Checks for updates to the DB for Dismissable Dialogs
 * 
 * by Dave Packham
 */

public class PopDialogCheck {

    public static final String DATABASE_NAME = "popdialog.db";
    public static final String DATABASE_TABLE = "dialogs";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_CREATE = "create table " + DATABASE_TABLE
            + " (_id integer primary key autoincrement, " + "dialogname text not null, dismissed text not null);";

    public static final String COL_DIALOGNAME = "dialogname";
    public static final String COL_DISMISSED = "dismissed";

    private int id;
    private String dialogname;
    private String dismissed;

    public int getId() {
        return id;
    }

    public String getDialogName() {
        return dialogname;
    }

    public String getDismissed() {
        return dismissed;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDialogName(String dialogname) {
        this.dialogname = dialogname;
    }

    public void setDismissed(String dismissed) {
        this.dismissed = dismissed;
    }
}