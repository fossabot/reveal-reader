package com.jackcholt.reveal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.Chapter;
import com.jackcholt.reveal.data.YbkDAO;

public class YbkProvider extends ContentProvider {
    public static final String KEY_MIMETYPE = "mimetype";

    public static final String AUTHORITY = "com.jackcholt.reveal";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/ybk");

    public static final int BOOK = 0;

    public static final int BOOKS = 1;

    public static final int CHAPTER = 2;

    public static final int CHAPTERS = 3;

    public static final int HISTORY = 4;

    public static final int HISTORIES = 5;

    public static final int BOOKMARK = 6;

    public static final int BOOKMARKS = 7;

    public static final int BACK = 8;

    public static final String TAG = "YbkProvider";

    /*public static final String BOOK_TABLE_NAME = "books";

    public static final String DATABASE_NAME = "reveal_ybk.db";

    public static final int DATABASE_VERSION = 15;

    *//** Unique id. Data type: INTEGER */
    /*public static final String _ID = "_id";

    public static final String BINDING_TEXT = "binding_text";

    public static final String BOOK_TITLE = "book_title";

    public static final String SHORT_TITLE = "short_title";

    public static final String FORMATTED_TITLE = "formatted_title";

    public static final String METADATA = "metadata";

    public static final String CHAPTER_TABLE_NAME = "chapters";

    public static final String CHAPTER_OFFSET = "offset";

    public static final String CHAPTER_LENGTH = "length";

    *//** Foreign key to the books table. Data type: INTEGER */
    /*public static final String BOOK_ID = "book_id";

    public static final String FILE_NAME = "file_name";

    public static final String CHAPTER_ORDER_NAME = "order_name";

    public static final String CHAPTER_ORDER_NUMBER = "order_number";

    public static final String CHAPTER_NAVBAR_TITLE = "navbar_title";

    public static final String CHAPTER_HISTORY_TITLE = "history_title";
    */
    /* Constants for the history table */
    public static final String HISTORY_TABLE_NAME = "history";

    public static final String HISTORY_TITLE = "title";

    public static final String CHAPTER_NAME = "chapter_name";

    public static final String BOOK_NAME = "book_name";

    public static final String SCROLL_POS = "scroll_position";

    public static final String CREATE_DATETIME = "create_datetime";

    public static final String BOOKMARK_NUMBER = "bookmark_id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    /**
     * Is the chapter a navigation chapter? Data type: INTEGER. Use
     * {@link CHAPTER_TYPE_NO_NAV} and {@link CHAPTER_TYPE_NAV} to set values.
     */
    public static final String CHAPTER_NAV_FILE = "nav_file";

    /**
     * Should the user be able to zoom the page? Data type: INTEGER. Used when
     * the chapter contains a picture. Use {@link CHAPTER_ZOOM_MENU_OFF} and
     * {@link CHAPTER_ZOOM_MENU_ON} to set values.
     */
    public static final String CHAPTER_ZOOM_PICTURE = "zoom_picture";

    public static final String BOOK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.book";

    public static final String BOOK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.book";

    public static final String CHAPTER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.chapter";

    public static final String CHAPTER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.chapter";

    public static final String HISTORY_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.history";

    public static final String HISTORY_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.history";

    public static final String BOOKMARK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.bookmark";

    public static final String BOOKMARK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.bookmark";

    public static final String BACK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.back";

    /** Non navigation chapter */
    public static final int CHAPTER_TYPE_NONNAV = 0;

    /** Navigation chapter */
    public static final int CHAPTER_TYPE_NAV = 1;

    /** All links open according to Popup view settings */
    public static final int CHAPTER_TYPE_SETTINGS = 2;

    /** Zoom menu will not be available */
    public static final int CHAPTER_ZOOM_MENU_OFF = 0;

    /** Zoom menu will be available */
    public static final int CHAPTER_ZOOM_MENU_ON = 1;

    //private static final int INDEX_FILENAME_STRING_LENGTH = 48;
    //private static final String BINDING_FILENAME = "\\BINDING.HTML";
    //private static final String BOOKMETADATA_FILENAME = "\\BOOKMETADATA.HTML.GZ"; 
    //private static final String ORDER_CONFIG_FILENAME = "\\ORDER.CFG";
    //private static final String BOOK_DEFAULT_SORT_ORDER = FORMATTED_TITLE + " ASC";
    //private static final String HISTORY_DEFAULT_SORT_ORDER = CREATE_DATETIME + " DESC";
    //private static final String BOOKMARK_DEFAULT_SORT_ORDER = HISTORY_TITLE + " ASC";
    
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter/#", CHAPTER);
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter", CHAPTERS);
        sUriMatcher.addURI(AUTHORITY, "ybk/book/#", BOOK);
        sUriMatcher.addURI(AUTHORITY, "ybk/book", BOOKS);
        sUriMatcher.addURI(AUTHORITY, "ybk/history/#", HISTORY);
        sUriMatcher.addURI(AUTHORITY, "ybk/history", HISTORIES);
        sUriMatcher.addURI(AUTHORITY, "ybk/bookmark/#", BOOKMARK);
        sUriMatcher.addURI(AUTHORITY, "ybk/bookmark", BOOKMARKS);
        sUriMatcher.addURI(AUTHORITY, "ybk/back/#", BACK);
    }

    private HashMap<Uri, File> mTempImgFiles = new HashMap<Uri, File>();

    /*private static class DatabaseHelper extends SQLiteOpenHelper {
        Context mContext;

        DatabaseHelper(final Context context) {

            super(context, DATABASE_NAME, null, DATABASE_VERSION);

            mContext = context;
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " (" + _ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT," + FILE_NAME
                    + " TEXT UNIQUE," + BINDING_TEXT + " TEXT," + BOOK_TITLE
                    + " TEXT," + FORMATTED_TITLE + " TEXT," + SHORT_TITLE
                    + " TEXT," + METADATA + " TEXT" + "); ");

            db.execSQL("CREATE UNIQUE INDEX " + BOOK_TABLE_NAME + "_"
                    + FILE_NAME + "_index ON " + BOOK_TABLE_NAME + " ("
                    + FILE_NAME + "); ");

            db.execSQL("CREATE TABLE " + CHAPTER_TABLE_NAME + " (" + _ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT," + BOOK_ID
                    + " INTEGER ," + FILE_NAME + " TEXT," + CHAPTER_OFFSET
                    + " INTEGER," + CHAPTER_LENGTH + " INTEGER,"
                    + CHAPTER_ORDER_NAME + " TEXT," + CHAPTER_ORDER_NUMBER
                    + " INTEGER," + CHAPTER_NAVBAR_TITLE + " TEXT,"
                    + CHAPTER_HISTORY_TITLE + " TEXT," + CHAPTER_NAV_FILE
                    + " INTEGER," + CHAPTER_ZOOM_PICTURE + " INTEGER,"
                    + " FOREIGN KEY (" + BOOK_ID + ") REFERENCES "
                    + BOOK_TABLE_NAME + " (" + _ID + ")" + " ON DELETE CASCADE"
                    + "); ");

            db.execSQL("CREATE INDEX " + CHAPTER_TABLE_NAME + "_" + BOOK_ID
                    + "_index ON " + CHAPTER_TABLE_NAME + " (" + BOOK_ID
                    + "); ");

            db.execSQL("CREATE TABLE " + HISTORY_TABLE_NAME + " (" + _ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT," + BOOK_ID
                    + " INTEGER NOT NULL," + CHAPTER_NAME + " TEXT NOT NULL,"
                    + CREATE_DATETIME + " TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + SCROLL_POS + " INTEGER," + HISTORY_TITLE + " TEXT,"
                    + BOOKMARK_NUMBER + " INTEGER, " + " FOREIGN KEY ("
                    + BOOK_ID + ") REFERENCES " + BOOK_TABLE_NAME + " (" + _ID
                    + ")" + " ON DELETE CASCADE" + "); ");

             db.execSQL("CREATE INDEX " + HISTORY_TABLE_NAME + "_" +
             BOOKMARK_NUMBER + "_index ON " + HISTORY_TABLE_NAME + " (" +
             BOOKMARK_NUMBER + "); ");

            // turns out they allow the cascade and foreign keys, but don't
            // enforce them!
             db.execSQL("CREATE TRIGGER fkd_chapters_books_id \n" +
             "BEFORE DELETE ON " + BOOK_TABLE_NAME +
             " FOR EACH ROW BEGIN DELETE FROM " + CHAPTER_TABLE_NAME +
             " WHERE " + BOOK_ID + " = OLD." + _ID + "; END;");
             
             db.execSQL("CREATE TRIGGER fkd_chapters_history_id \n" +
             "BEFORE DELETE ON " + BOOK_TABLE_NAME +
             " FOR EACH ROW BEGIN DELETE FROM " + HISTORY_TABLE_NAME +
             " WHERE " + BOOK_ID + " = OLD." + _ID + "; END;");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {

            // Log.w(TAG, "Upgrading database from version " + oldVersion +
            // " to "
            // + newVersion + ", which will destroy all old data");

            NotificationManager nm = (NotificationManager) mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            Util.sendNotification(mContext, "Rebuilding library...",
                    R.drawable.ebooksmall, "Reveal Database Updated", nm,
                    Main.mNotifId++, Main.class);

            db.execSQL("DROP TABLE IF EXISTS " + CHAPTER_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE_NAME);

            onCreate(db);
        }
    }
*/
    //private DatabaseHelper mOpenHelper;

    private SharedPreferences mSharedPref;
    //private String mHistoryEntryAmount; 
    //private String mBookmarkEntryAmount; 
    
    /**
     * @see {@link ContentProvider.delete(Uri uri, String selection, String[]
     *      selectionArgs)}
     */
    @Override
    public int delete(final Uri uri, final String selection,
            final String[] selectionArgs) {
        /*final String DELETE_ALL = "1";
        String selectionString = selection;
        int recordsAffected = 0;

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        
        if (selectionArgs != null) {
            Log.w(TAG,"selectionArgs are not yet supported.  They will not be used.");
        }

        int match = sUriMatcher.match(uri);

        switch (match) {
        case BOOK:
            String bookId = uri.getPathSegments().get(2);

            db.beginTransaction();
            try {
                try {
                    db.delete(CHAPTER_TABLE_NAME, BOOK_ID
                            + "="
                            + bookId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), null);

                    db.delete(HISTORY_TABLE_NAME, BOOK_ID
                            + "="
                            + bookId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), null);

                    db.delete(YbkProvider.BOOK_TABLE_NAME, _ID
                            + "="
                            + bookId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), null);

                } catch (SQLiteException sqle) {
                    Log.i(TAG, "Could not delete book with id " + bookId
                            + "or its chapters and histories", sqle);
                }

                db.setTransactionSuccessful();
                getContext().getContentResolver().notifyChange(uri, null);
            } finally {
                db.endTransaction();
            }
            break;

        case BOOKS:
            if (selection == null) {
                selectionString = DELETE_ALL;
            }

            db.beginTransaction();
            try {
                try {
                    recordsAffected = db.delete(YbkProvider.BOOK_TABLE_NAME,
                            selectionString, null);
                } catch (SQLiteException sqle) {
                    Log.i(TAG, YbkProvider.BOOK_TABLE_NAME
                            + " probably doesn't exist.", sqle);
                }
                db.setTransactionSuccessful();
                getContext().getContentResolver().notifyChange(uri, null);
            } finally {
                db.endTransaction();
            }

            if (bookRows > recordsAffected) {
                Log.w(TAG, "Not all the books could be deleted");
            }
            break;

        case HISTORY:

            String histId = uri.getPathSegments().get(2);

            if (Integer.parseInt(histId) == GET_LAST_HISTORY) {
                db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE "
                        + CREATE_DATETIME + " = (SELECT MAX(" + CREATE_DATETIME
                        + ") FROM " + HISTORY_TABLE_NAME + ");");
            } else {
                try {
                    recordsAffected = db.delete(HISTORY_TABLE_NAME, _ID
                            + "="
                            + histId
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), null);
                } catch (SQLiteException sqle) {
                    Log.i(TAG, HISTORY_TABLE_NAME + " probably doesn't exist.",
                            sqle);
                }
            }

            getContext().getContentResolver().notifyChange(uri, null);

            break;

        case HISTORIES:

            Cursor histCurs = db.rawQuery("SELECT " + CREATE_DATETIME
                    + " FROM " + HISTORY_TABLE_NAME + " ORDER BY "
                    + CREATE_DATETIME + " ASC LIMIT ?",
                    new String[] { mHistoryEntryAmount });

            try {
                if (histCurs.getCount() == Integer
                        .parseInt(mHistoryEntryAmount)) {
                    histCurs.moveToFirst();
                    String oldestDate = histCurs.getString(0);
                    recordsAffected = db.delete(HISTORY_TABLE_NAME,
                            CREATE_DATETIME + " < ?",
                            new String[] { oldestDate });
                }
            } finally {
                histCurs.close();
            }

            getContext().getContentResolver().notifyChange(uri, null);

            break;

        case BOOKMARK:

            String bmkId = uri.getPathSegments().get(2);

            try {
                recordsAffected = db.delete(HISTORY_TABLE_NAME, BOOKMARK_NUMBER
                        + "="
                        + bmkId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                                + ')' : ""), null);
            } catch (SQLiteException sqle) {
                Log.i(TAG, HISTORY_TABLE_NAME + " probably doesn't exist.",
                        sqle);
            }

            if (recordsAffected > 1) {
                // nothing good can come from throwing an exception here, just
                // log it
                // throw new
                // InconsistentContentException("There was more than one bookmark with a number of "
                // + bmkId);
                Log.w(TAG, "There was more than one bookmark with a number of "
                        + bmkId);
            } else if (recordsAffected == 0) {
                Log
                        .w(TAG,
                                "Tried to delete a bookmark with a non-existant number");
            }

            getContext().getContentResolver().notifyChange(uri, null);

            break;
            
        case BOOKMARKS:  

         case BOOKMARKS:
         
         Cursor bmkCurs = db.rawQuery("SELECT " + CREATE_DATETIME + " FROM " +
         HISTORY_TABLE_NAME + " ORDER BY " + CREATE_DATETIME + " ASC LIMIT ?"
         , new String[] {mHistoryEntryAmount});
         
         
         try { if (histCurs.getCount() ==
         Integer.parseInt(mHistoryEntryAmount)) { histCurs.moveToFirst();
         String oldestDate = histCurs.getString(0); recordsAffected =
         db.delete(HISTORY_TABLE_NAME, CREATE_DATETIME + " < ?", new String[]
         {oldestDate}); } } finally { histCurs.close(); }
         
         getContext().getContentResolver().notifyChange(uri, null);
         
         break;

        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        return recordsAffected;*/
        
        Log.e(TAG, "YbkProvider does not support deletion.");
        
        return 0;
    }

    /**
     * Get the mime-type of a particular URI.
     * 
     * @param uri
     *            The URI for which to get a mime-type.
     * @return The mime-type.
     */
    @Override
    public String getType(final Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case CHAPTERS:
            return CHAPTER_CONTENT_TYPE;

        case CHAPTER:
            return CHAPTER_CONTENT_ITEM_TYPE;

        case BOOKS:
            return BOOK_CONTENT_TYPE;

        case BOOK:
            return BOOK_CONTENT_ITEM_TYPE;

        case HISTORIES:
            return HISTORY_CONTENT_TYPE;

        case HISTORY:
            return HISTORY_CONTENT_ITEM_TYPE;

        case BOOKMARKS:
            return BOOKMARK_CONTENT_TYPE;

        case BOOKMARK:
            return BOOKMARK_CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Insert records into the content provider.
     */
    @Override
    public Uri insert(final Uri uri, final ContentValues initialValues) {
        /*ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId;
        switch (sUriMatcher.match(uri)) {
        case CHAPTERS:
            if (values.containsKey(YbkProvider.BOOK_ID) == false
                    || values.containsKey(YbkProvider.FILE_NAME) == false
                    || values.containsKey(YbkProvider.CHAPTER_OFFSET) == false
                    || values.containsKey(YbkProvider.CHAPTER_LENGTH) == false
                    || values.containsKey(YbkProvider.CHAPTER_ORDER_NAME) == false
                    || values.containsKey(YbkProvider.CHAPTER_ORDER_NUMBER) == false
                    || values.containsKey(YbkProvider.CHAPTER_NAVBAR_TITLE) == false
                    || values.containsKey(YbkProvider.CHAPTER_HISTORY_TITLE) == false) {
                throw new IllegalArgumentException(
                        "One of the following parameters were not passed while adding a chapter: \n"
                                + YbkProvider.BOOK_ID + " ,"
                                + YbkProvider.FILE_NAME + " ,"
                                + YbkProvider.CHAPTER_OFFSET + " ,"
                                + YbkProvider.CHAPTER_LENGTH + " ,"
                                + YbkProvider.CHAPTER_ORDER_NAME + " ,"
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

            rowId = db.insert(CHAPTER_TABLE_NAME,
                    YbkProvider.CHAPTER_ORDER_NAME, values);
            if (rowId > 0) {
                Uri chapUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(chapUri, null);
                return chapUri;
            }

            break;

        case BOOKS:
            if (values.containsKey(FILE_NAME) == false) {
                throw new IllegalArgumentException(
                        "File name was not passed in");
            }

            rowId = populateBook(values.getAsString(FILE_NAME));

            Uri bookUri = ContentUris.withAppendedId(Uri.withAppendedPath(
                    CONTENT_URI, "book"), rowId);
            getContext().getContentResolver().notifyChange(bookUri, null);
            return bookUri;

        case HISTORIES:
            if (values.containsKey(BOOK_ID) == false
                    || values.containsKey(CHAPTER_NAME) == false
                    || values.containsKey(SCROLL_POS) == false
                    || values.containsKey(HISTORY_TITLE) == false) {
                throw new IllegalArgumentException(
                        "One of the following parameters were not passed while adding a history: \n"
                                + BOOK_ID + " ," + CHAPTER_NAME + " ,"
                                + SCROLL_POS + " ," + HISTORY_TITLE);
            }

            Log.d(TAG, "Values: " + values);

            try {
                rowId = db.insert(HISTORY_TABLE_NAME, CHAPTER_NAME, values);
            } catch (SQLiteConstraintException sce) {
                rowId = 0;
            }

            if (rowId > 0) {
                Uri histUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(histUri, null);
                return histUri;
            } else {
                Log.w(TAG, "History was not saved for: " + values);
            }

            break;

        case BOOKMARKS:
            if (values.containsKey(BOOK_ID) == false
                    || values.containsKey(CHAPTER_NAME) == false
                    || values.containsKey(SCROLL_POS) == false
                    || values.containsKey(HISTORY_TITLE) == false) {
                throw new IllegalArgumentException(
                        "One of the following parameters were not passed while adding a bookmark: \n"
                                + BOOK_ID + " ," + CHAPTER_NAME + " ,"
                                + SCROLL_POS + " ," + HISTORY_TITLE);
            }

            Cursor maxBMCurs = db.rawQuery("SELECT max(" + BOOKMARK_NUMBER
                    + ") FROM " + HISTORY_TABLE_NAME, null);

            int maxBM;
            if (maxBMCurs.moveToFirst()) {
                maxBM = maxBMCurs.getInt(0);
            } else {
                maxBM = 1;
            }
            maxBMCurs.close();

            values.put(BOOKMARK_NUMBER, maxBM + 1);

            rowId = db.insert(HISTORY_TABLE_NAME, CHAPTER_NAME, values);
            if (rowId > 0) {
                Uri histUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(histUri, null);
                return histUri;
            }

            break;

        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        throw new SQLException("Failed to insert row into " + uri);*/
        
        Log.e(TAG, "The YbkProvider does not support inserts");
        
        return null;
    }

    @Override
    public boolean onCreate() {
        //mOpenHelper = new DatabaseHelper(getContext());

        //mHistoryEntryAmount = mSharedPref.getString("default_history_entry_amount", "30");
        //mBookmarkEntryAmount = mSharedPref.getString("default_bookmark_entry_amount", "20");
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection,
            final String selection, final String[] selectionArgs,
            String sortOrder) {

        /*String orderBy = null;
        String where = null;
        String limit = null;

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case CHAPTERS:
            qb.setTables(CHAPTER_TABLE_NAME);
            if (selection != null && selectionArgs != null) {
                qb.appendWhere(selection);
            }

            break;

        case CHAPTER:
            qb.setTables(CHAPTER_TABLE_NAME);
            String chapId = uri.getPathSegments().get(2);
            where = _ID + "=" + chapId;
            break;

        case BOOKS:
            qb.setTables(BOOK_TABLE_NAME);
            // If no sort order is specified use the default
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = BOOK_DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            if (selection != null) {
                qb.appendWhere(selection);
            }

            break;

        case BOOK:
            qb.setTables(BOOK_TABLE_NAME);
            orderBy = null;
            String bookId = uri.getPathSegments().get(2);
            qb.appendWhere(_ID + "=" + bookId);
            break;

        case HISTORIES:
            qb.setTables(HISTORY_TABLE_NAME);

            // If no sort order is specified use the default
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = HISTORY_DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }

            qb.appendWhere(BOOKMARK_NUMBER + " IS NULL ");

            if (selection != null) {
                qb.appendWhere(selection);
            }

            limit = mHistoryEntryAmount;

            break;

        case HISTORY:
            String histId = uri.getPathSegments().get(2);
            String query;
            if (Integer.parseInt(histId) == GET_LAST_HISTORY) {

                query = "SELECT b." + FILE_NAME + ", " + CHAPTER_NAME + ", "
                        + SCROLL_POS + " FROM " + HISTORY_TABLE_NAME
                        + " AS h, " + BOOK_TABLE_NAME + " AS b " + " WHERE b."
                        + _ID + "=h." + BOOK_ID + " AND " + BOOKMARK_NUMBER
                        + " IS NULL " + " ORDER BY " + CREATE_DATETIME
                        + " DESC LIMIT 1";

            } else {
                query = "SELECT h." + BOOK_ID + ", b." + FILE_NAME + ", "
                        + CHAPTER_NAME + ", " + SCROLL_POS + ", "
                        + HISTORY_TITLE + " " + " FROM " + HISTORY_TABLE_NAME
                        + " AS h, " + BOOK_TABLE_NAME + " AS b " + " WHERE b."
                        + _ID + "=h." + BOOK_ID + " AND h." + _ID + "="
                        + histId;
            }

            Cursor c = db.rawQuery(query, null);

            // Tell the cursor what uri to watch, so it knows when its source
            // data changes
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;

        case BOOKMARKS:
            // Must use a rawQuery so the bookmark_name field can be aliased to
            // _id

            String bmQuery = "SELECT " + BOOK_ID + ", " + BOOKMARK_NUMBER
                    + " AS _id, " + CHAPTER_NAME + ", " + SCROLL_POS + ", "
                    + HISTORY_TITLE + " " + " FROM " + HISTORY_TABLE_NAME
                    + " WHERE " + BOOKMARK_NUMBER + " IS NOT NULL ";

            if (null == sortOrder) {
                sortOrder = BOOKMARK_DEFAULT_SORT_ORDER;
            }

            bmQuery += " ORDER BY " + sortOrder + " LIMIT "
                    + mBookmarkEntryAmount;

            Cursor bmksCurs = db.rawQuery(bmQuery, null);

            // Tell the cursor what uri to watch, so it knows when its source
            // data changes
            bmksCurs.setNotificationUri(getContext().getContentResolver(), uri);
            return bmksCurs;

        case BOOKMARK:
            String bmkId = uri.getPathSegments().get(2);

            Cursor bmkCurs = db.rawQuery("SELECT h." + _ID + ", h." + BOOK_ID
                    + ", b." + FILE_NAME + ", " + CHAPTER_NAME + ", "
                    + SCROLL_POS + ", " + HISTORY_TITLE + " " + " FROM "
                    + HISTORY_TABLE_NAME + " AS h, " + BOOK_TABLE_NAME
                    + " AS b " + " WHERE b." + _ID + "=h." + BOOK_ID
                    + " AND h." + BOOKMARK_NUMBER + "=" + bmkId, null);

            // Tell the cursor what uri to watch, so it knows when its source
            // data changes
            bmkCurs.setNotificationUri(getContext().getContentResolver(), uri);
            return bmkCurs;

        case BACK:
            String backId = uri.getPathSegments().get(2);

            Cursor backCurs = db.rawQuery("SELECT b." + FILE_NAME + ", "
                    + CHAPTER_NAME + ", " + SCROLL_POS + " FROM "
                    + HISTORY_TABLE_NAME + " AS h, " + BOOK_TABLE_NAME
                    + " AS b " + " WHERE b." + _ID + "=h." + BOOK_ID + " AND "
                    + BOOKMARK_NUMBER + " IS NULL " + " ORDER BY "
                    + CREATE_DATETIME + " DESC LIMIT 1 OFFSET " + backId, null);

            // Tell the cursor what uri to watch, so it knows when its source
            // data changes
            backCurs.setNotificationUri(getContext().getContentResolver(), uri);
            return backCurs;

        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Get the database and run the query
        Cursor c = qb.query(db, projection, where, selectionArgs, null, null,
                orderBy, limit);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;*/
        Log.e(TAG, "The YbkProvider does not support queries.");
        
        return null;
    }

    /**
     * This is only supported for bookmarks.
     */
    @Override
    public int update(final Uri uri, final ContentValues values,
            final String selection, final String[] selectionArgs) {
        /*SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
        case BOOKMARK:
            String bmkId = uri.getPathSegments().get(2);
            count = db.update(HISTORY_TABLE_NAME, values, BOOKMARK_NUMBER
                    + "="
                    + bmkId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection
                            + ')' : ""), selectionArgs);

            break;
        default:
            throw new IllegalArgumentException("Unsupported URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;*/
        Log.e(TAG, "The YbkProvider does not support updates.");
        
        return 0;
    }

    /**
     * Get and save the book information into the database.
     * 
     * @param fileName
     *            The file name of the book.
     * @return The id of the book that was saved into the database.
     */
    /*private long populateBook(final String fileName) {

        long bookId = 0;

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        RandomAccessFile file = null;

        try {
            file = new RandomAccessFile(fileName, "r");
        } catch (FileNotFoundException fnfe) {
            // throw new IllegalStateException("Couldn't find the YBK file",
            // fnfe);
            // treat this consistently with how we treat all the rest of the
            // exceptions
            Log.e(TAG, "Couldn't find the YBK file: " + fnfe.getMessage());
            bookId = 0;
            return bookId;
        }

        // Need save book with minimal data so we can get the record id
        ContentValues values = new ContentValues();
        values.put(YbkProvider.FILE_NAME, fileName);

        db.beginTransaction();
        try {
            bookId = db.insert(BOOK_TABLE_NAME, METADATA, values);

            if (bookId == -1) {
                // we'll assume the fileName is already in the db and continue
                Log
                        .w(
                                TAG,
                                "Unable to insert book ("
                                        + fileName
                                        + ") into the database.  Must already be in the database.");
            }

            try {
                populateChapters(file, bookId);
            } catch (IOException ioe) {
                Log
                        .e(
                                TAG,
                                fileName + "'s chapters could not be populated",
                                ioe);
            }

            values.clear();
            try {
                String bindingText = readBindingFile(file, bookId);
                if (bindingText != null) {
                    values.put(BINDING_TEXT, bindingText);
                    String title = Util
                            .getBookTitleFromBindingText(bindingText);
                    if (title == Util.NO_TITLE) {
                        throw new IllegalStateException("Book (" + fileName
                                + ") was not inserted. (bookId: " + bookId
                                + ")");
                    }
                    values.put(BOOK_TITLE, title);
                    values.put(SHORT_TITLE, Util
                            .getBookShortTitleFromBindingText(bindingText));
                    values.put(FORMATTED_TITLE, Util.formatTitle(title));
                }
                values.put(METADATA, readMetaData(file, bookId));

                int count = db.update(BOOK_TABLE_NAME, values, _ID + "="
                        + bookId, null);
                if (count != 1) {
                    throw new IllegalStateException("Book (" + fileName
                            + ") was not updated. (bookId: " + bookId + ")");
                }
                populateOrder(readOrderCfg(file, bookId), bookId);

                db.setTransactionSuccessful();
            } catch (IOException ioe) {
                Log.e(TAG, "Could not update the book. " + ioe.getMessage());
                bookId = 0;
            } catch (IllegalStateException ise) {
                Log.e(TAG, "Book may be corrupt. " + ise.getMessage());
                bookId = 0;
            }

        } finally {

            db.endTransaction();
        }

        return bookId;
    }
*/        
    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML.
     * @throws IOException
     *             If there is a problem reading the Binding file.
     * @throws InvalidFileFormatException
     *             if there is no Binding internal file found.
     */
    /*private String readBindingFile(final RandomAccessFile file, final long bookId) 
    throws IOException, InvalidFileFormatException {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = db.query(CHAPTER_TABLE_NAME,
                new String[] { YbkProvider._ID }, "lower(" + FILE_NAME
                        + ")=lower(?) AND " + BOOK_ID + "=?", new String[] {
                        BINDING_FILENAME, Long.toString(bookId) }, null, null,
                null);

        try {
            int count = c.getCount();
            // Log.d(TAG, "Cursor record count is: " + count);

            if (count == 1) {
                c.moveToFirst();
                fileText = readInternalFile(file, bookId, c.getInt(0));
            }
        } finally {
            c.close();
        }

        return fileText;
    }*/

    /**
     * 
     * @param file
     * @param bookFileName
     * @param chapterName
     * @return
     * @throws IOException
     */
    public byte[] readInternalBinaryFile(final RandomAccessFile file,
            final String bookFileName, final String chapterName)
            throws IOException {

        int offset = 0;
        int len = 0;
        byte[] bytes = null;
        
        YbkDAO ybkDao = YbkDAO.getInstance(getContext());
        
        Book book = ybkDao.getBook(bookFileName);
        Chapter chap = ybkDao.getChapter(book.id, chapterName);
        
        if (chap != null) {
            offset = chap.offset;
            len = chap.length;
        
        
            bytes = new byte[len];
            file.seek(offset);
            int amountRead = file.read(bytes);
            if (amountRead < len) {
                throw new InvalidFileFormatException(
                        "Couldn't read all of " + bookFileName + ".");
            }
        }

        return bytes;
    }

    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param dataInput
     *            The input stream that is the YanceyBook.
     * @param bookId
     *            The id of the book record in the database.
     * @param chapterId
     *            The id of the chapter record in the database.
     * @return The text of the chapter.
     * @throws IOException
     *             If the chapter cannot be read.
     */
    /*private String readInternalFile(final RandomAccessFile file, 
            final long bookId, final int chapterId) 
    throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;

        Cursor c = query(ContentUris.withAppendedId(Uri.withAppendedPath(
                CONTENT_URI, "chapter"), chapterId), new String[] {
                YbkProvider.CHAPTER_LENGTH, YbkProvider.CHAPTER_OFFSET,
                YbkProvider.FILE_NAME }, YbkProvider.BOOK_ID + "=" + bookId,
                null, null);

        try {
            if (c.getCount() > 0) {
                c.moveToFirst();

                offset = c.getInt(c
                        .getColumnIndexOrThrow(YbkProvider.CHAPTER_OFFSET));
                len = c.getInt(c
                        .getColumnIndexOrThrow(YbkProvider.CHAPTER_LENGTH));
                String iFilename = c.getString(c
                        .getColumnIndexOrThrow(YbkProvider.FILE_NAME));

                byte[] text = new byte[len];
                file.seek(offset);
                int amountRead = file.read(text);
                if (amountRead < len) {
                    throw new InvalidFileFormatException(
                            "Couldn't read all of " + iFilename + ".");
                }

                if (iFilename.toLowerCase().endsWith(".gz")) {
                    fileText = Util.decompressGzip(text);
                } else {
                    fileText = new String(text, "ISO_8859-1");
                }

            } else {
                throw new InvalidFileFormatException(
                        "The chapter could not be found.");
            }
        } finally {
            c.close();
        }

        return fileText;
    }*/

    /**
     * Return the uncompressed contents of Book Metadata internal file as a
     * String or null if the YBK file doesn't contain one.
     * 
     * @return The uncompressed contents of the Book Metadata file.
     * @throws IOException
     *             if there is a problem reading the Book Metadata file.
     */
    /*private String readMetaData(final RandomAccessFile file, final long bookId) throws IOException {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = db.query(CHAPTER_TABLE_NAME,
                new String[] { YbkProvider._ID }, "lower(" + FILE_NAME
                        + ")=lower(?)", new String[] { BOOKMETADATA_FILENAME },
                null, null, null);

        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                fileText = readInternalFile(file, bookId, c.getInt(0));
            }
        } finally {
            c.close();
        }

        return fileText;
    }*/

    /**
     * Read in the Order Config information which tells us in what order the
     * chapters appear in a YanceyBook.
     * 
     * @param dataInput
     *            The input stream that is the YanceyBook.
     * @param bookId
     *            The id of the book record in the database.
     * @return The text of the order config &quot;chapter&quot;. Returns null if
     *         there is no Order Config chapter in the file.
     * @throws IOException
     *             If the YBK cannot be read.
     */
    /*private String readOrderCfg(final RandomAccessFile file, final long bookId) {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = db.query(CHAPTER_TABLE_NAME, new String[] { _ID }, "lower("
                + FILE_NAME + ")=lower(?) AND " + BOOK_ID + "=?", new String[] {
                ORDER_CONFIG_FILENAME, Long.toString(bookId) }, null, null,
                null);

        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                fileText = readInternalFile(file, bookId, c.getInt(c
                        .getColumnIndex(_ID)));
            }
        } finally {
            c.close();
        }

        return fileText;
    }*/

    /**
     * Analyze the YBK file and save file contents data for later reference.
     * 
     * @throws IOException
     *             If the YBK file is not readable.
     */
    /*private void populateChapters(final RandomAccessFile file, final long bookId) 
    throws IOException {
        String iFileName = "";
        int iBookOffset = 0;
        int iBookLength = 0;

        String orderString = readOrderCfg(file, bookId);
        String[] orders = null;
        if (orderString != null) {
            orders = orderString.split(",");
        }

        int indexLength = Util.readVBInt(file);
        // Log.d(TAG,"Index Length: " + indexLength);

        byte[] indexArray = new byte[indexLength];

        if (file.read(indexArray) < indexLength) {
            throw new InvalidFileFormatException(
                    "Index Length is greater than length of file.");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            db.delete(CHAPTER_TABLE_NAME, BOOK_ID + "=" + bookId, null);
        } catch (SQLiteException sqle) {
            Log.i(TAG,
                    "There might not be a table named " + CHAPTER_TABLE_NAME,
                    sqle);
        }

        ContentValues values = new ContentValues();

        // Read the index information into the internalFiles list
        int pos = 0;

        // Read the index and create chapter records
        while (pos < indexLength) {

            StringBuffer fileNameSBuf = new StringBuffer();

            byte b;
            int fileNameStartPos = pos;

            while ((b = indexArray[pos++]) != 0 && pos < indexLength) {
                fileNameSBuf.append((char) b);
            }

            iFileName = fileNameSBuf.toString();

            pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;

            iBookOffset = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
            pos += 4;

            iBookLength = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
            pos += 4;

            values.clear();

            if (orders != null) {
                int orderNumber = Arrays.binarySearch(orders, iFileName,
                        new Comparator<String>() {

                            public int compare(String arg0, String arg1) {
                                return arg0.compareToIgnoreCase(arg1);
                            }

                        });

                if (orderNumber >= 0) {
                    values.put(YbkProvider.CHAPTER_ORDER_NUMBER, orderNumber);
                }
            }
            values.put(YbkProvider.FILE_NAME, iFileName);
            values.put(YbkProvider.CHAPTER_OFFSET, iBookOffset);
            values.put(YbkProvider.CHAPTER_LENGTH, iBookLength);
            values.put(YbkProvider.BOOK_ID, bookId);

            db.insert(YbkProvider.CHAPTER_TABLE_NAME, YbkProvider.FILE_NAME,
                    values);

        }

    }*/

    /**
     * Open a file and return a ParcelFileDescriptor reference to it.
     * 
     * @param uri
     *            The URI which refers to the file.
     * @param mode
     *            The mode in which to open the file.
     * @return The ParcelFileDescriptor which refers to the file.
     * @throws FileNotFoundException
     *             If the file cannot be accessed.
     * @see {@link ContentProvider.openFile(Uri, String)}
     */
    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String mode)
            throws FileNotFoundException {
        final int BUFFER_SIZE = 8096;
        // Log.d(TAG,"In openFile. URI is: " + uri.toString());

        HashMap<Uri, File> tempImgFiles = mTempImgFiles;
        File outFile = null;

        String strUri = uri.toString();
        String fileExt = strUri.substring(strUri.lastIndexOf("."));

        if (".jpg .gif".contains(fileExt)) {
            if (tempImgFiles.containsKey(uri)) {
                outFile = tempImgFiles.get(uri);
            } else {
                String strCUri = CONTENT_URI.toString();
                int cUriLength = strCUri.length();
                String uriFileName = strUri.substring(cUriLength);

                String[] fileParts = uriFileName.split("/");
                String tempFileName = "";
                for (int i = 1; i < fileParts.length; i++) {
                    tempFileName += fileParts[i] + "_";
                }
                tempFileName = tempFileName.substring(0, tempFileName.length()-1);
                
                String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, 
                        Settings.DEFAULT_EBOOK_DIRECTORY);
                
                outFile = new File(libDir + "images/", tempFileName);
                outFile.deleteOnExit();

                if (!outFile.exists()) {
                    HashMap<String, String> chapterMap = Util
                            .getFileNameChapterFromUri(strUri, libDir, false);
                    String fileName = chapterMap.get("book");
                    String chapter = chapterMap.get("chapter");
                    RandomAccessFile file = new RandomAccessFile(fileName, "r");

                    try {
                        byte[] contents = readInternalBinaryFile(file, fileName, chapter);
                        if (contents != null) {
                            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile),BUFFER_SIZE);
                            out.write(contents);
                            out.flush();
                        }
                    } catch (IOException e) {
                        throw new FileNotFoundException(
                                "Could not write internal file to temp file. "
                                        + e.getMessage() + " " + e.getCause());
                    }
                }
                tempImgFiles.put(uri, outFile);

            }
        } else {
            Log.w(TAG, "openFile was called for non-image URI: " + uri);
        }

        int m = ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.equalsIgnoreCase("rw"))
            m = ParcelFileDescriptor.MODE_READ_WRITE;

        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(outFile, m);

        return pfd;
   }
    
   /**
    * Save the order configuration into the database.
    * 
    * @param orderString A comma-delimited list of abbreviated chapter names.
    */
   /*private void populateOrder(final String orderString, long bookId) {
       if (null != orderString) {
            String[] orders = orderString.split(",");

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            for (int i = 0, orderLen = orders.length; i < orderLen; i++) {
                values.put(CHAPTER_ORDER_NUMBER, i);

                String chapter = orders[i].toLowerCase();
                if (chapter.indexOf(".html") == -1) {
                    chapter += ".html";
                }
                int recUpdated = db.update(CHAPTER_TABLE_NAME, values, 
                        BOOK_ID + "=? AND lower(" + FILE_NAME + ") like '%\\" + chapter.replace("'", "''") + "%'",
                        new String[] {Long.toString(bookId)});
                
                if (recUpdated != 1) {
                    Log.e(TAG, "Order.cfg appears to contain a reference to a non-existent chapter.\n" +
                    		"Records updated for " + chapter + " should be 1, Is: " + recUpdated);
                }
                
                values.clear();
            }
        }
    }*/
}
