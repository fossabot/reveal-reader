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

    //private EditText mBodyTextField;
    //private TextView mChapterVerseField;
    private Long mRowId;
    private NotesDbAdapter mDbHelper;
    private String mBookFileName;
    private String mChapterName;
    private long mVerseStartPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.note_edit);

        mRowId = (null == savedInstanceState || savedInstanceState.getLong(NotesDbAdapter.KEY_ROWID) == 0) ? null
                : savedInstanceState.getLong(NotesDbAdapter.KEY_ROWID);

        if (null == mRowId) {
            mRowId = (getIntent().getExtras() != null && getIntent().getExtras().getLong(NotesDbAdapter.KEY_ROWID) != 0) ? getIntent()
                    .getExtras().getLong(NotesDbAdapter.KEY_ROWID)
                    : null;
        }

        populateFields();

        findConfirmButton().setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }

        });
    }

    private EditText findBodyTextField() {
        return (EditText) findViewById(R.id.body);
    }

    private TextView findChapterVerseField() {
        return (TextView) findViewById(R.id.chapter_verse);
    }

    private Button findConfirmButton() {
        return (Button) findViewById(R.id.confirm);
    }

    private void populateFields() {
        if (mRowId == null)
            return;

        Cursor noteCursor = mDbHelper.fetchNoteCursor(mRowId);
        startManagingCursor(noteCursor);

        findBodyTextField().setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)));
        findChapterVerseField().setText(noteCursor.getString(noteCursor
                .getColumnIndexOrThrow(NotesDbAdapter.KEY_CHAPTER_VERSE)));

        mBookFileName = noteCursor.getString(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_BOOK_FILENAME));
        mChapterName = noteCursor.getString(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_CHAPTER_NAME));
        mVerseStartPos = noteCursor.getLong(noteCursor.getColumnIndexOrThrow(NotesDbAdapter.KEY_VERSE_START_POS));
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
            long id = mDbHelper.createNote(new Note(mBookFileName, mChapterName, mVerseStartPos, findBodyTextField()
                    .getText().toString(), findChapterVerseField().getText().toString()));
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateNote(new Note(mRowId, mBookFileName, mChapterName, mVerseStartPos, findBodyTextField().getText()
                    .toString(), findChapterVerseField().getText().toString()));
        }
    }
}
