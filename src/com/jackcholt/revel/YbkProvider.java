package com.jackcholt.revel;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class YbkProvider extends ContentProvider {
    public static final String KEY_MIMETYPE = "mimetype";
    public static final Uri CONTENT_URI = Uri.parse("content://com.jackcholt.revel.ybk");
    public static final Uri BOOK_CONTENT_URI = Uri.parse("content://com.jackcholt.revel.ybk/book");
    public static final Uri CHAPTER_CONTENT_URI = Uri.parse("content://com.jackcholt.revel.ybk/chapter");
    public static final Uri ORDER_CONTENT_URI = Uri.parse("content://com.jackcholt.revel.ybk/order");
    public static final int BOOK = 0;
    public static final int BOOKS = 1;
    public static final int CHAPTER = 2;
    public static final int CHAPTERS = 3;
    public static final int ORDER = 4;
    public static final int ORDERS = 5;
    public static final String TAG = "YbkProvider";
    public static final String DATABASE_NAME = "revel";
    public static final int DATABASE_VERSION = 1;
    public static final String BOOK_TABLE_NAME = "books";
    /** Unique id. Data type: INTEGER */
    public static final String _ID = "_id";
    public static final String BINDING_TEXT = "binding_text";
    public static final String BOOK_TITLE = "book_title";
    public static final String SHORT_TITLE = "short_title";
    public static final String METADATA = "metadata";
    public static final String CHAPTER_TABLE_NAME = "chapters";
    public static final String CHAPTER_OFFSET = "offset";
    public static final String CHAPTER_LENGTH = "length";
    /** Foreign key to the books table. Data type: INTEGER */
    public static final String BOOK_ID = "book_id";
    public static final String FILE_NAME = "file_name";
    public static final String CHAPTER_ORDER_NAME = "order_name";
    public static final String CHAPTER_ORDER_NUMBER = "order_number";
    public static final String CHAPTER_NAVBAR_TITLE = "navbar_title";
    public static final String CHAPTER_HISTORY_TITLE = "history_title";
    /** Is the chapter a navigation chapter? Data type: INTEGER. 
     *  Use {@link CHAPTER_TYPE_NO_NAV} and 
     *  {@link CHAPTER_TYPE_NAV} to set values.
     */
    public static final String CHAPTER_NAV_FILE = "nav_file";
    /** Should the user be able to zoom the page? Data type: INTEGER. Used when
     * the chapter contains a picture. 
     *  Use {@link CHAPTER_ZOOM_MENU_OFF} and 
     *  {@link CHAPTER_ZOOM_MENU_ON} to set values.
     */
    public static final String CHAPTER_ZOOM_PICTURE = "zoom_picture";
    public static final String ORDER_TABLE_NAME = "chapters";
    /** Foreign key to the chapters table. Data type: INTEGER */
    public static final String CHAPTER_ID = "chapter_id";
    public static final String BOOK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.revel.ybk.book";
    public static final String BOOK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.revel.ybk.book";
    public static final String CHAPTER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.revel.ybk.chapter";
    public static final String CHAPTER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.revel.ybk.chapter";
    public static final String ORDER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.revel.ybk.order";
    public static final String ORDER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.revel.ybk.order";
    /** Non navigation chapter */
    public static final int CHAPTER_TYPE_NONNAV = 0; 
    /** Navigation chapter */
    public static final int CHAPTER_TYPE_NAV = 1; 
    /** All links open according to Popup view settings */
    public static final int CHAPTER_TYPE_SETTINGS = 2;
    /** Zoom menu will not be available */
    public static final int CHAPTER_ZOOM_MENU_OFF = 0;
    /** Zoom menu will be available  */
    public static final int CHAPTER_ZOOM_MENU_ON = 1;
    
    private static final int INDEX_FILENAME_STRING_LENGTH = 48;
    private static final String BINDING_FILENAME = "\\BINDING.HTML";
    private static final String BOOKMETADATA_FILENAME = "\\BOOKMETADATA.HTML.GZ"; 
    private static final String ORDER_CONFIG_FILENAME = "\\ORDER.CFG";
    
    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "chapter/#", CHAPTER);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "chapter", CHAPTERS);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "book/#", BOOK);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "book", BOOKS);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "order/#", ORDER);
        URI_MATCHER.addURI("com.jackcholt.revel.ybk", "order", ORDERS);
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " ("
                    + YbkProvider._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + YbkProvider.FILE_NAME + " TEXT,"
                    + YbkProvider.BINDING_TEXT + " TEXT,"
                    + YbkProvider.BOOK_TITLE + " TEXT,"
                    + YbkProvider.SHORT_TITLE + " TEXT,"
                    + YbkProvider.METADATA + " TEXT"
                    + "); "
                    + "CREATE TABLE " + CHAPTER_TABLE_NAME + " ("
                    + YbkProvider._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + YbkProvider.BOOK_ID + " INTEGER,"
                    + YbkProvider.FILE_NAME + " TEXT,"
                    + YbkProvider.CHAPTER_OFFSET + " INTEGER,"
                    + YbkProvider.CHAPTER_LENGTH + " INTEGER,"
                    + YbkProvider.CHAPTER_ORDER_NAME + " TEXT,"
                    + YbkProvider.CHAPTER_ORDER_NUMBER + " INTEGER,"
                    + YbkProvider.CHAPTER_NAVBAR_TITLE + " TEXT," 
                    + YbkProvider.CHAPTER_HISTORY_TITLE + " TEXT," 
                    + YbkProvider.CHAPTER_NAV_FILE + " INTEGER,"
                    + YbkProvider.CHAPTER_ZOOM_PICTURE + " INTEGER"
                    + "); "
                    + "CREATE TABLE " + ORDER_TABLE_NAME + " ("
                    + YbkProvider._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + YbkProvider.BOOK_ID + " INTEGER,"
                    + YbkProvider.CHAPTER_ID + " INTEGER,"
                    + YbkProvider.FILE_NAME + " TEXT"
                    + "); "
                    
            );
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, 
                final int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + ORDER_TABLE_NAME + ";" +
            		"DROP TABLE IF EXISTS " + CHAPTER_TABLE_NAME + ";" +
                    "DROP TABLE IF EXISTS " + BOOK_TABLE_NAME + ";" 
                    );
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public int delete(final Uri uri, final String selection, 
            final String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(final Uri uri) {
        switch (URI_MATCHER.match(uri)) {
        case CHAPTERS:
            return CHAPTER_CONTENT_TYPE;

        case CHAPTER:
            return CHAPTER_CONTENT_ITEM_TYPE;

        case BOOKS:
            return BOOK_CONTENT_TYPE;

        case BOOK:
            return BOOK_CONTENT_ITEM_TYPE;

        case ORDERS:
            return ORDER_CONTENT_TYPE;

        case ORDER:
            return ORDER_CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId;
        switch (URI_MATCHER.match(uri)) {
        case CHAPTERS:
            if (values.containsKey(YbkProvider.BOOK_ID) == false 
                    || values.containsKey(YbkProvider.FILE_NAME) == false 
                    || values.containsKey(YbkProvider.CHAPTER_OFFSET) == false 
                    || values.containsKey(YbkProvider.CHAPTER_LENGTH) == false 
                    || values.containsKey(YbkProvider.CHAPTER_ORDER_NAME) == false 
                    || values.containsKey(YbkProvider.CHAPTER_ORDER_NUMBER) == false
                    || values.containsKey(YbkProvider.CHAPTER_NAVBAR_TITLE) == false
                    || values.containsKey(YbkProvider.CHAPTER_HISTORY_TITLE) == false) {
                throw new IllegalArgumentException("One of the following parameters were not passed while adding a chapter: "
                        + YbkProvider.BOOK_ID + " ," + YbkProvider.FILE_NAME + " ," 
                        + YbkProvider.CHAPTER_OFFSET + " ," + YbkProvider.CHAPTER_LENGTH
                        + " ," + YbkProvider.CHAPTER_ORDER_NAME + " ," 
                        + YbkProvider.CHAPTER_ORDER_NUMBER + " ," 
                        + YbkProvider.CHAPTER_NAVBAR_TITLE + " ," 
                        + YbkProvider.CHAPTER_HISTORY_TITLE);
            }
            if (values.containsKey(YbkProvider.CHAPTER_NAV_FILE) == false) {
                values.put(YbkProvider.CHAPTER_NAV_FILE, CHAPTER_TYPE_SETTINGS);
            }
            if (values.containsKey(YbkProvider.CHAPTER_ZOOM_PICTURE) == false) {
                values.put(YbkProvider.CHAPTER_NAV_FILE, CHAPTER_ZOOM_MENU_OFF);
            }

            rowId = db.insert(CHAPTER_TABLE_NAME, YbkProvider.CHAPTER_ORDER_NAME, values);
            if (rowId > 0) {
                Uri chapUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(chapUri, null);
                return chapUri;
            }
            
            break;
        case BOOKS:
            if (values.containsKey(YbkProvider.FILE_NAME) == false
                    || values.containsKey(YbkProvider.BINDING_TEXT) == false
                    || values.containsKey(YbkProvider.BOOK_TITLE) == false
                    || values.containsKey(YbkProvider.SHORT_TITLE) == false) {
                throw new IllegalArgumentException("One of the following parameters were not passed while adding a book: "
                        + YbkProvider.FILE_NAME + " ," + YbkProvider.BINDING_TEXT + " ," 
                        + YbkProvider.BOOK_TITLE + " ," + YbkProvider.SHORT_TITLE
                        + " ," + YbkProvider.METADATA);
            }
            if (values.containsKey(YbkProvider.METADATA) == false) {
                values.put(YbkProvider.METADATA, (String)null);
            }

            rowId = db.insert(BOOK_TABLE_NAME, YbkProvider.METADATA, values);
            if (rowId > 0) {
                Uri bookUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(bookUri, null);
                return bookUri;
            }
            
            break;
        case ORDERS:
            if (values.containsKey(YbkProvider.FILE_NAME) == false
                    || values.containsKey(YbkProvider.CHAPTER_ID) == false
                    || values.containsKey(YbkProvider.BOOK_ID) == false) {
                throw new IllegalArgumentException("One of the following parameters were not passed while adding an order: "
                        + YbkProvider.FILE_NAME + " ," + YbkProvider.CHAPTER_ID + " ," 
                        + YbkProvider.BOOK_ID);
            }
            
            rowId = db.insert(ORDER_TABLE_NAME, YbkProvider.FILE_NAME, values);
            if (rowId > 0) {
                Uri orderUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(orderUri, null);
                return orderUri;
            }
            
            break;
            
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, 
            final String selection, final String[] selectionArgs, 
            final String sortOrder) {
        return null;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, 
            final String selection, final String[] selectionArgs) {
        return 0;
    }

}
