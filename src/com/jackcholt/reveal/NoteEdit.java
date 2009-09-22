package com.jackcholt.reveal;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jackcholt.reveal.data.Note;

public class NoteEdit extends Activity {

    private EditText mBodyText;
    private TextView mChapterVerse;
    private Long mRowId;
    private NotesDbAdapter mDbHelper;
    private long mBookId;
    private long mChapterId;
    private long mVerseStartPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.note_edit); 

        mChapterVerse = (TextView) findViewById(R.id.chapter_verse);
        mBodyText = (EditText) findViewById(R.id.body);

        Button confirmButton = (Button) findViewById(R.id.confirm);

        if (null == savedInstanceState) {
            mRowId = null;
        } else {
            mRowId = savedInstanceState.getLong(NotesDbAdapter.KEY_ROWID);
        }

        if (null == mRowId) {
            Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(NotesDbAdapter.KEY_ROWID) : null;
        }

        populateFields();

        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }

        });
    }

    private void populateFields() {
        if (mRowId != null) {
            Cursor noteCursor = mDbHelper.fetchNote(mRowId);
            startManagingCursor(noteCursor);

            mBodyText.setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)));
            mChapterVerse.setText(noteCursor.getString(noteCursor
                    .getColumnIndexOrThrow(NotesDbAdapter.KEY_CHAPTER_VERSE)));

            mBookId = noteCursor.getLong(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_BOOK_ID));
            mChapterId = noteCursor.getLong(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_CHAPTER_ID));
            mVerseStartPos = noteCursor.getLong(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_VERSE_START_POS));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(NotesDbAdapter.KEY_ROWID, mRowId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    private void saveState() {

        if (null == mRowId) {
            long id = mDbHelper.createNote(new Note(mBookId, mChapterId, mVerseStartPos,
                    mBodyText.getText().toString(), mChapterVerse.getText().toString()));
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateNote(new Note(mRowId, mBookId, mChapterId, mVerseStartPos, mBodyText.getText().toString(),
                    mChapterVerse.getText().toString()));
        }
    }
}
