package com.jackcholt.reveal;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.jackcholt.reveal.data.AnnotHilite;
import com.jackcholt.reveal.data.YbkDAO;

public class AnnotationDialog extends Activity {
    public static final String TAG = "ldssa.AnnotationDialog";
    
    public static final int RED_HILITE = Color.parseColor("#ffaaaa");
    public static final int PINK_HILITE = Color.parseColor("#ffcccc");
    public static final int BLUE_HILITE = Color.parseColor("#ccccff");
    public static final int GREEN_HILITE = Color.parseColor("#ccffcc");
    public static final int YELLOW_HILITE = Color.parseColor("#ffffcc");
    public static final int NO_HILITE = Color.TRANSPARENT;
    public static final HashMap<Integer, Integer> COLOR_MAP = new HashMap<Integer, Integer>();
    static {
        COLOR_MAP.put(R.id.radioButtonRed, RED_HILITE);
        COLOR_MAP.put(R.id.radioButtonPink, PINK_HILITE);
        COLOR_MAP.put(R.id.radioButtonBlue, BLUE_HILITE);
        COLOR_MAP.put(R.id.radioButtonGreen, GREEN_HILITE);
        COLOR_MAP.put(R.id.radioButtonYellow, YELLOW_HILITE);
        COLOR_MAP.put(R.id.radioButtonWhite, NO_HILITE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(android.R.style.Theme_Dialog);
        setContentView(R.layout.annotation_layout);

        final int verse = getIntent().getIntExtra(YbkDAO.VERSE, 0);
        final String bookFilename = getIntent().getStringExtra(YbkDAO.BOOK_FILENAME);
        final String chapterFilename = getIntent().getStringExtra(YbkDAO.CHAPTER_FILENAME);

        AnnotHilite ah = YbkDAO.getInstance(this).getAnnotHilite(verse, bookFilename, chapterFilename);

        if (ah != null) {
            findNoteField().setText(ah.note);
            int colorKey = getColorKeyByValue(ah.color);
            Log.d(TAG, "color key: " + colorKey);
            ((RadioButton) findViewById(colorKey)).setChecked(true);
        }

        findOkButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                setResult(RESULT_OK, new Intent(getBaseContext(), YbkViewActivity.class).putExtra(YbkDAO.VERSE, verse)
                        .putExtra(YbkDAO.BOOK_FILENAME, bookFilename)
                        .putExtra(YbkDAO.CHAPTER_FILENAME, chapterFilename).putExtra(YbkDAO.NOTE,
                                findNoteField().getText().toString()).putExtra(YbkDAO.COLOR,
                                COLOR_MAP.get(findHiliteGroup().getCheckedRadioButtonId())));
                finish();
            }
        });

        findCancelButton().setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                finish();
            }
        });
    }

    public int getColorKeyByValue(int color) {
        for (Integer key : COLOR_MAP.keySet()) {
            if (COLOR_MAP.get(key) == color) {
                return key;
            }
        }
        return -1;
    }

    private Button findOkButton() {
        return (Button) findViewById(R.id.annotOkButton);
    }

    private Button findCancelButton() {
        return (Button) findViewById(R.id.annotCancelButton);
    }

    private EditText findNoteField() {
        return (EditText) findViewById(R.id.editNote);
    }

    private RadioGroup findHiliteGroup() {
        return (RadioGroup) findViewById(R.id.radioGroupHiLite);
    }
}
