/*
 * NotesDbAdapter.java -- Thanks to Google for providing the template we used.
 */

package com.jackcholt.reveal;

import com.jackcholt.reveal.data.Note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NotesDbAdapter {

    public static final String KEY_BODY = "body";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_BOOK_ID = "book_id";
    public static final String KEY_CHAPTER_ID = "chapter_id";
    public static final String KEY_VERSE_START_POS = "verse_start_pos";
    public static final String KEY_CHAPTER_VERSE = "chapter_verse";

    private static final String TAG = "NotesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "reveal_annot.db";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 1;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ROWID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_BOOK_ID + " INTEGER NOT NULL, " + KEY_CHAPTER_ID
            + " INTEGER NOT NULL, " + KEY_VERSE_START_POS + " INTEGER NOT NULL, " + KEY_BODY + " TEXT NOT NULL, " 
            + KEY_CHAPTER_VERSE + " TEXT NOT NULL);";

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be opened/created
     * 
     * @param ctx
     *            the Context within which to work
     */
    public NotesDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new instance of the database. If it cannot be
     * created, throw an exception to signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    public NotesDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    /**
     * Create a new note using the title and body provided. If the note is successfully created return the new rowId for
     * that note, otherwise return a -1 to indicate failure.
     * 
     * @param title
     *            the title of the note
     * @param body
     *            the body of the note
     * @return rowId or -1 if failed
     */
    public long createNote(final Note note) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_BOOK_ID, note.getBookId());
        initialValues.put(KEY_BODY, note.getBody());
        initialValues.put(KEY_CHAPTER_ID, note.getChapterId());
        initialValues.put(KEY_VERSE_START_POS, note.getVerseStartPos());

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId
     *            id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllNotes() {

        return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_BOOK_ID, KEY_CHAPTER_ID, KEY_VERSE_START_POS,
                KEY_BODY }, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId
     *            id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException
     *             if note could not be found/retrieved
     */
    public Cursor fetchNote(long rowId) throws SQLException {

        Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_BOOK_ID, KEY_CHAPTER_ID,
                KEY_VERSE_START_POS, KEY_BODY }, KEY_ROWID + "=" + rowId, null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;

    }

    /**
     * Update the note using the details provided.
     * 
     * @param note
     *            The note to update.
     * @return true if the note was successfully updated, false otherwise.
     * @throws IllegalArgumentException
     *             if the Note object has a null id.
     */
    public boolean updateNote(final Note note) throws IllegalArgumentException {
        if (note.getId() == null) {
            throw new IllegalArgumentException("When updating a note, id cannot be null");
        }

        ContentValues args = new ContentValues();
        args.put(KEY_BOOK_ID, note.getBookId());
        args.put(KEY_CHAPTER_ID, note.getChapterId());
        args.put(KEY_VERSE_START_POS, note.getVerseStartPos());
        args.put(KEY_BODY, note.getBody());

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + note.getId(), null) > 0;
    }
}
