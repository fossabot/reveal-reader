package com.jackcholt.reveal;

public class PopDialogCheck {
    
    public static final String DATABASE_NAME = "popdialog.db";
    public static final String DATABASE_TABLE = "dialogs";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_CREATE =
        "create table " + DATABASE_TABLE + " (_id integer primary key autoincrement, "
                + "dialogname text not null, dismissed text not null);";
    
    public static final String COL_DIALOGNAME = "dialogname";
    public static final String COL_DISMISSED = "dismissed";
    
    private int id;
    private String dialogname;
    private String dismissed;
    
    public int getId() {
        return id;
    }
    public String getTitle() {
        return dialogname;
    }
    public String getBody() {
        return dismissed;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setTitle(String title) {
        this.dialogname = title;
    }
    public void setBody(String body) {
        this.dismissed = body;
    }
}