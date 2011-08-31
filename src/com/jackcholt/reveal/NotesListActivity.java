package com.jackcholt.reveal;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.jackcholt.reveal.data.AnnotHilite;
import com.jackcholt.reveal.data.YbkDAO;

public class NotesListActivity extends ListActivity {
    private static final int COLOR_GRAY = Color.rgb(10, 10, 10);
    private static final int COLOR_LIGHT_GRAY = Color.rgb(80, 80, 80);

    private ArrayList<AnnotHilite> mNotesList;
    private LayoutInflater mLayoutInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNotesList = YbkDAO.getInstance(getApplicationContext()).getAnnotHilites();
        mLayoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.notes_list);
        setListAdapter(new NotesAdapter());
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        AnnotHilite ah = mNotesList.get(position);
        setResult(
                RESULT_OK,
                new Intent().putExtra(YbkDAO.FILENAME, ah.bookFilename)
                        .putExtra(YbkDAO.CHAPTER_FILENAME, ah.chapterFilename).putExtra(YbkDAO.VERSE, Integer.toString(ah.verse)));
        finish();
    }

    private class NotesAdapter extends BaseAdapter {

        public int getCount() {
            if (null == mNotesList) {
                return 0;
            }
            return mNotesList.size();
        }

        public Object getItem(int position) {
            if (null == mNotesList) {
                throw new IllegalStateException("The notes list is empty");
            }
            if (position > mNotesList.size() || position < 0) {
                throw new IllegalArgumentException("Index of requested item (" + position + ") is out of range");
            }
            return mNotesList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TwoLineListItem view = (null == convertView) ? createView(parent) : (TwoLineListItem) convertView;
            bindView(view, (AnnotHilite) getItem(position));
            return view;
        }

        /**
         * Bind notes with a two-line list item view for display.
         * 
         * @param view A two-line list item view.
         * @param note A search result to bind with the view.
         */
        private void bindView(TwoLineListItem view, AnnotHilite note) {
            TextView ref = view.getText1();
            ref.setText(formatReference(note));
            ref.setTextColor(COLOR_GRAY);

            TextView annot = view.getText2();
            annot.setText(note.note);
            annot.setTextColor(COLOR_LIGHT_GRAY);
            annot.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }

        /**
         * Create a new two-line list item view for displaying notes.
         * 
         * @param parent The parent view group to which the new view will belong.
         * @return A new two-line list item view.
         */
        private TwoLineListItem createView(ViewGroup parent) {
            return (TwoLineListItem) mLayoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        private String formatReference(AnnotHilite ah) {
            String chapFilename = ah.chapterFilename.replace('\\', '/');
            if (-1 != chapFilename.lastIndexOf('.')) {
                // get rid of trailing file extension
                chapFilename = chapFilename.substring(0, chapFilename.lastIndexOf('.'));
            }
            String[] tokens = chapFilename.split("/");
            String ref = tokens[1].toUpperCase() + " -";
            for (int index = 2; index < tokens.length; index++) {
                ref += " " + Util.capitalize(tokens[index], " -".toCharArray());
            }

            return ref + ":" + ah.verse;
        }
    }
}
