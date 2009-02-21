package com.jackcholt.reveal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class YbkProvider extends ContentProvider {
    public static final String KEY_MIMETYPE = "mimetype";
    public static final String AUTHORITY = "com.jackcholt.reveal";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/ybk");
    public static final int BOOK = 0;
    public static final int BOOKS = 1;
    public static final int CHAPTER = 2;
    public static final int CHAPTERS = 3;
    public static final int ORDER = 4;
    public static final int ORDERS = 5;
    public static final String TAG = "YbkProvider";
    public static final String BOOK_TABLE_NAME = "books";
    public static final String DATABASE_NAME = "reveal_ybk.db";
    public static final int DATABASE_VERSION = 9;
    /** Unique id. Data type: INTEGER */
    public static final String _ID = "_id";
    public static final String BINDING_TEXT = "binding_text";
    public static final String BOOK_TITLE = "book_title";
    public static final String SHORT_TITLE = "short_title";
    public static final String FORMATTED_TITLE = "formatted_title";
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
    public static final String ORDER_TABLE_NAME = "orders";
    /** Foreign key to the chapters table. Data type: INTEGER */
    public static final String CHAPTER_ID = "chapter_id";
    public static final String BOOK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.book";
    public static final String BOOK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.book";
    public static final String CHAPTER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.chapter";
    public static final String CHAPTER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.chapter";
    public static final String ORDER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.order";
    public static final String ORDER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.order";
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
    private static final String BOOK_DEFAULT_SORT_ORDER = FORMATTED_TITLE + " ASC";
    
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter/#", CHAPTER);
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter", CHAPTERS);
        sUriMatcher.addURI(AUTHORITY, "ybk/book/#", BOOK);
        sUriMatcher.addURI(AUTHORITY, "ybk/book", BOOKS);
        sUriMatcher.addURI(AUTHORITY, "ybk/order/#", ORDER);
        sUriMatcher.addURI(AUTHORITY, "ybk/order", ORDERS);
    }
    
    private HashMap<Uri, File> mTempImgFiles = new HashMap<Uri, File>();
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }  

        @Override
        
        public void onCreate(final SQLiteDatabase db) {
         	db.execSQL("CREATE TABLE " + BOOK_TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + FILE_NAME + " TEXT UNIQUE,"
                    + BINDING_TEXT + " TEXT,"
                    + BOOK_TITLE + " TEXT," 
                    + FORMATTED_TITLE + " TEXT,"
                    + SHORT_TITLE + " TEXT,"
                    + METADATA + " TEXT"
                    + "); ");
            
            db.execSQL("CREATE TABLE " + CHAPTER_TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + BOOK_ID + " INTEGER,"
                    + FILE_NAME + " TEXT,"
                    + CHAPTER_OFFSET + " INTEGER,"
                    + CHAPTER_LENGTH + " INTEGER,"
                    + CHAPTER_ORDER_NAME + " TEXT,"
                    + CHAPTER_ORDER_NUMBER + " INTEGER,"
                    + CHAPTER_NAVBAR_TITLE + " TEXT," 
                    + CHAPTER_HISTORY_TITLE + " TEXT," 
                    + CHAPTER_NAV_FILE + " INTEGER,"
                    + CHAPTER_ZOOM_PICTURE + " INTEGER,"
                    + " FOREIGN KEY (" + BOOK_ID + ") REFERENCES "
                    + BOOK_TABLE_NAME + " (" + _ID + ")"
                    + " ON DELETE CASCADE"
                    + "); ");
            
            //turns out they allow the cascade and foreign keys, but don't
            // enforce them!
            db.execSQL("CREATE TRIGGER fkd_chapters_books_id \n"
            		+ "BEFORE DELETE ON " + BOOK_TABLE_NAME
            		+ " FOR EACH ROW BEGIN DELETE FROM "
            		+ CHAPTER_TABLE_NAME + " WHERE "
            		+ BOOK_ID + " = OLD." + _ID + "; END;");
            
           /*db.execSQL("CREATE TABLE " + ORDER_TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + BOOK_ID + " INTEGER, "
                    + CHAPTER_ID + " INTEGER, "
                    + CHAPTER_ORDER_NUMBER + " INTEGER,"
                    + FILE_NAME + " TEXT"
                    + "); "
                    
            );*/
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, 
                final int newVersion) {

            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            
            db.execSQL("DROP TABLE IF EXISTS " + ORDER_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + CHAPTER_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + BOOK_TABLE_NAME);
            
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;
    private String mLibraryDir = "/sdcard/reveal/ebooks/";
    private SharedPreferences mSharedPref; 
    
    /**
     * @see {@link ContentProvider.delete(Uri uri, String selection, String[] selectionArgs)}
     */
    @Override
    public int delete(final Uri uri, final String selection, 
            final String[] selectionArgs) {
        final String DELETE_ALL = "1";
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
                try {recordsAffected = db.delete(YbkProvider.BOOK_TABLE_NAME, YbkProvider._ID + "=" + bookId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), null);
                }
                catch (SQLiteException sqle) {
                    Log.i(TAG,YbkProvider.BOOK_TABLE_NAME + " probably doesn't exist.", sqle);
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

            Cursor c = db.rawQuery("SELECT count(*) FROM " + BOOK_TABLE_NAME, null);
            c.moveToFirst();
            int bookRows = c.getInt(0);
            c.close();
            
            db.beginTransaction();
            try {
                try {recordsAffected = db.delete(YbkProvider.BOOK_TABLE_NAME, selectionString, null);}
                catch (SQLiteException sqle) {
                    Log.i(TAG,YbkProvider.BOOK_TABLE_NAME + " probably doesn't exist.", sqle);
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
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        
        
        return recordsAffected;
    }

    /**
     * Get the mime-type of a particular URI.
     * 
     * @param uri The URI for which to get a mime-type.
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

        case ORDERS:
            return ORDER_CONTENT_TYPE;

        case ORDER:
            return ORDER_CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Insert records into the content provider.
     */
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
                throw new IllegalArgumentException("One of the following parameters were not passed while adding a chapter: \n"
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
            if (values.containsKey(FILE_NAME) == false) {
                throw new IllegalArgumentException("File name was not passed in");
            }

            rowId = populateBook(values.getAsString(FILE_NAME));
            
            if (rowId > 0) {
                Uri bookUri = ContentUris.withAppendedId(
                        Uri.withAppendedPath(CONTENT_URI, "book"), rowId);
                getContext().getContentResolver().notifyChange(bookUri, null);
                return bookUri;
            }
            
            break;
        case ORDERS:
            if (values.containsKey(YbkProvider.FILE_NAME) == false
                    || values.containsKey(YbkProvider.CHAPTER_ID) == false
                    || values.containsKey(YbkProvider.BOOK_ID) == false) {
                throw new IllegalArgumentException("One of the following parameters were not passed while adding an order: \n"
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
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, 
            final String selection, final String[] selectionArgs, 
            final String sortOrder) {

        String orderBy = null;
        String where = null;
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
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection,  where, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }
    /**
     * Updating of this content provider through this public interface is not 
     * supported.
     * 
     * @deprecated This method does not do anything.
     */
    @Override
    public int update(final Uri uri, final ContentValues values, 
            final String selection, final String[] selectionArgs) {
        Log.w(TAG, "Update is not supported.");
        return 0;
    }

    /**
     * Get and save the book information into the database.
     * 
     * @param fileName The file name of the book.
     * @return The id of the book that was saved into the database.
     */
    private long populateBook(final String fileName) {
      
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        RandomAccessFile file = null;
        
        try {
            file = new RandomAccessFile(fileName, "r");
        } catch (FileNotFoundException fnfe) {
            throw new IllegalStateException("Couldn't find the YBK file", fnfe);
        }
        
        // Need save book with minimal data so we can get the record id
        ContentValues values = new ContentValues();
        values.put(YbkProvider.FILE_NAME, fileName);

        long bookId = db.insert(BOOK_TABLE_NAME, METADATA, values);
        
        if (bookId == -1) {
            throw new IllegalStateException("Unable to insert book (" + 
                    fileName + ") into the database.");
        }
        
        try {
            populateChapters(file, bookId);
        } catch (IOException ioe) {
            Log.e(TAG, fileName + "'s chapters could not be populated", ioe);
        }
        
        values.clear();
        try {
            String bindingText = readBindingFile(file, bookId);
            if (bindingText != null) {
                values.put(BINDING_TEXT, bindingText);
                String title = Util.getBookTitleFromBindingText(bindingText);
                values.put(BOOK_TITLE, title);
                values.put(SHORT_TITLE,Util.getBookShortTitleFromBindingText(bindingText));
                values.put(FORMATTED_TITLE,Util.formatTitle(title));
            }
            values.put(METADATA, readMetaData(file, bookId));
            
            int count = db.update(BOOK_TABLE_NAME, values, _ID + "=" + bookId, null);
            if (count != 1) {
                throw new IllegalStateException("Book (" + fileName + ") was not updated. (bookId: " + bookId + ")");
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not update the book");
        }

        populateOrder(readOrderCfg(file, bookId), bookId);
        
        return bookId;
    }
        
    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML.
     * @throws IOException If there is a problem reading the Binding file.
     * @throws InvalidFileFormatException if there is no Binding internal file 
     * found. 
     */
    public String readBindingFile(final RandomAccessFile file, final long bookId) 
    throws IOException, InvalidFileFormatException {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        
        Cursor c = db.query(CHAPTER_TABLE_NAME, new String[] {YbkProvider._ID}, 
                "lower(" + FILE_NAME + ")=lower(?) AND " + BOOK_ID + "=?" , 
                new String[] {BINDING_FILENAME, Long.toString(bookId)}, null, null, null);
        
        try {
            int count = c.getCount();
            Log.d(TAG, "Cursor record count is: " + count);
            
            if (count == 1) {
                c.moveToFirst();
                fileText = readInternalFile(file, bookId, c.getInt(0));
            }
        } finally {
            c.close();
        }
        
        return fileText;
    }
    
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
        
        String bookId = "";
        int offset = 0;
        int len = 0;
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.query(BOOK_TABLE_NAME, new String[] {_ID}, "lower(" + FILE_NAME + ")=?", 
                new String[] {bookFileName.toLowerCase()}, null, null, null);
        
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                bookId = c.getString(0);
            }
            
            c = db.query(CHAPTER_TABLE_NAME, new String[] {CHAPTER_OFFSET, CHAPTER_LENGTH}, 
                    "lower(" + FILE_NAME + ")=? AND BOOK_ID =?", 
                    new String[] {chapterName.toLowerCase(), bookId}, null, null, null);
            
            if (c.getCount() == 1) {
                c.moveToFirst();
                offset = c.getInt(c.getColumnIndexOrThrow(CHAPTER_OFFSET));
                len = c.getInt(c.getColumnIndexOrThrow(CHAPTER_LENGTH));
            }
        } finally {
            c.close();
        }
        
        byte[] bytes = new byte[len];
        file.seek(offset);
        int amountRead = file.read(bytes);
        if (amountRead < len) {
            throw new InvalidFileFormatException(
                    "Couldn't read all of " + bookFileName + ".");
        }
        
        return bytes;
    }
    
    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param dataInput The input stream that is the YanceyBook.
     * @param bookId The id of the book record in the database.
     * @param chapterId The id of the chapter record in the database.
     * @return The text of the chapter.
     * @throws IOException If the chapter cannot be read.
     */
    public String readInternalFile(final RandomAccessFile file, 
            final long bookId, final int chapterId) 
    throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;
        
        Cursor c = query(ContentUris.withAppendedId(Uri.withAppendedPath(CONTENT_URI, "chapter"), chapterId), 
                new String[] {YbkProvider.CHAPTER_LENGTH, YbkProvider.CHAPTER_OFFSET, YbkProvider.FILE_NAME}, 
                YbkProvider.BOOK_ID + "=" + bookId, null, null);
        
        try {
            if ( c.getCount() > 0) {
                c.moveToFirst();
                
                offset = c.getInt(c.getColumnIndexOrThrow(YbkProvider.CHAPTER_OFFSET));
                len = c.getInt(c.getColumnIndexOrThrow(YbkProvider.CHAPTER_LENGTH));
                String iFilename = c.getString(c.getColumnIndexOrThrow(YbkProvider.FILE_NAME));
                
             
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
                    fileText = new String(text);
                }
                        
            } else {
                throw new IllegalStateException("The chapter could not be found.");
            }
        } finally {
            c.close();
        }
        
        return fileText;
    }
    
    /**
     * Return the uncompressed contents of Book Metadata internal file as a 
     * String or null if the YBK file doesn't contain one.
     * 
     * @return The uncompressed contents of the Book Metadata file.
     * @throws IOException if there is a problem reading the Book Metadata file. 
     */
    private String readMetaData(final RandomAccessFile file, final long bookId) throws IOException {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        
        Cursor c = db.query(CHAPTER_TABLE_NAME, new String[] {YbkProvider._ID}, 
                "lower(" + FILE_NAME + ")=lower(?)" , new String[] {BOOKMETADATA_FILENAME}, null, null, null);
        
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                fileText = readInternalFile(file, bookId, c.getInt(0));
            }
        } finally {
            c.close();
        }
        
        return fileText;
    }
    
    /**
     * Read in the Order Config information which tells us in what order the 
     * chapters appear in a YanceyBook.
     * 
     * @param dataInput The input stream that is the YanceyBook.
     * @param bookId The id of the book record in the database.
     * @return The text of the order config &quot;chapter&quot;. Returns null if 
     * there is no Order Config chapter in the file.
     * @throws IOException If the YBK cannot be read.
     */
    private String readOrderCfg(final RandomAccessFile file, final long bookId) {
        String fileText = null;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        
        Cursor c = db.query(CHAPTER_TABLE_NAME, new String[] {_ID}, 
                "lower(" + FILE_NAME + ")=lower(?) AND " + BOOK_ID + "=?" , 
                new String[] {ORDER_CONFIG_FILENAME, Long.toString(bookId)}, null, null, null);
        
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                try {
                    fileText = readInternalFile(file, bookId, c.getInt(0));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        } finally {
            c.close();
        }
        
        return fileText;
    }

    /**
     * Analyze the YBK file and save file contents data for later reference.
     * @throws IOException If the YBK file is not readable.
     */
    private void populateChapters(final RandomAccessFile file, final long bookId) 
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
        Log.d(TAG,"Index Length: " + indexLength);
        
        byte[] indexArray = new byte[indexLength];
        
        if (file.read(indexArray) < indexLength) {
            throw new IllegalStateException("Index Length is greater than length of file.");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            db.delete(CHAPTER_TABLE_NAME, BOOK_ID + "=" + bookId, null);
        } catch (SQLiteException sqle) {
            Log.i(TAG,"There might not be a table named " + CHAPTER_TABLE_NAME, sqle);
        }
        
        ContentValues values = new ContentValues();

        // Read the index information into the internalFiles list
        int pos = 0;
        
        // Read the index and create chapter records
        while (pos < indexLength) {
            
            StringBuffer fileNameSBuf = new StringBuffer();
            
            byte b;
            int fileNameStartPos = pos;

            while((b = indexArray[pos++]) != 0) {
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
                int orderNumber = Arrays.binarySearch(orders, iFileName, new Comparator<String>() {
    
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
            
            db.insert(YbkProvider.CHAPTER_TABLE_NAME, YbkProvider.FILE_NAME, values);
            
        }
        
    }
    
    /*private void populateOrder(final String orderString) {
        if (null != orderString) {
            Scanner orderScan = new Scanner(orderString).useDelimiter(",");
            
            ArrayList<String> orderList = mOrderList;
            
            while(orderScan.hasNext()) {
                orderList.add(orderScan.next());
            }
        }
    }*/

    /**
     * Open a file and return a ParcelFileDescriptor reference to it.
     * 
     *  @param uri The URI which refers to the file.
     *  @param mode The mode in which to open the file.
     *  @return The ParcelFileDescriptor which refers to the file.
     *  @throws FileNotFoundException If the file cannot be accessed.
     *  @see {@link ContentProvider.openFile(Uri, String)}
     */
    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String mode) 
    throws FileNotFoundException {
        final int BUFFER_SIZE = 8096;
        Log.d(TAG,"In openFile. URI is: " + uri.toString());
        
        HashMap<Uri, File> tempImgFiles = mTempImgFiles;
        File f = null;
        
        String strUri = uri.toString();
        String fileExt = strUri.substring(strUri.lastIndexOf("."));
        
        if (".jpg .gif".contains(fileExt)) {
            if (tempImgFiles.containsKey(uri)) {
                f = tempImgFiles.get(uri);
            } else {
                try {
                    f = File.createTempFile("reveal_img", fileExt, null);
                    f.deleteOnExit();
                } catch (IOException ioe) {
                    throw new FileNotFoundException("Could not create a temporary file. " 
                            + ioe.getMessage() + " " + ioe.getCause());
                }
                
                HashMap<String,String> chapterMap = Util.getFileNameChapterFromUri(strUri, mLibraryDir, false);
                String fileName = chapterMap.get("book");
                String chapter = chapterMap.get("chapter");
                RandomAccessFile file = new RandomAccessFile(fileName, "r");
                
                try {
                    byte[] contents = readInternalBinaryFile(file, fileName, chapter);
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f),BUFFER_SIZE);
                    out.write(contents);
                    out.flush();
                } catch (IOException e) {
                    throw new FileNotFoundException("Could not write internal file to temp file. " 
                            + e.getMessage() + " " + e.getCause());
                }
                
                tempImgFiles.put(uri, f);
            }
        } else {
            Log.i(TAG, "openFile was called for non-image URI: " + uri);
        }
        
        int m = ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.equalsIgnoreCase("rw"))
            m = ParcelFileDescriptor.MODE_READ_WRITE;
        
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f,m);
        
        return pfd;
   }
    
   /**
    * Save the order configuration into the database.
    * 
    * @param orderString A comma-delimited list of abbreviated chapter names.
    */
   private void populateOrder(final String orderString, long bookId) {
       if (null != orderString) {
            String[] orders = orderString.split(",");
            
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            for(int i = 0, orderLen = orders.length; i < orderLen; i++) {
                values.put(CHAPTER_ORDER_NUMBER, i);
                
                String chapter = orders[i].toLowerCase();
                if (chapter.indexOf(".html") == -1) {
                    chapter += ".html";
                }
                int recUpdated = db.update(CHAPTER_TABLE_NAME, values, 
                        BOOK_ID + "=? AND lower(" + FILE_NAME + ") like '%\\" + chapter.replace("'", "''") + "%'",
                        new String[] {Long.toString(bookId)});
                
                if (recUpdated != 1) {
                    Log.e(TAG, "Order.cfg appears to contain a reference to a non-existant chapter.\n" +
                    		"Records updated for " + chapter + " should be 1, Is: " + recUpdated);
                }
                
                values.clear();
            }
        }
    }
}
