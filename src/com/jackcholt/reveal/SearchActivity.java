package com.jackcholt.reveal;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;

public class SearchActivity extends ListActivity {
    private static final String TAG = "reveal.Search";

    public static int mNotifId = 1;

    private String mSearchString = "";

    private YbkSearcher mSearcher;
    
    private ImageButton mButton;
    
    private EditText mSearchStringEdit;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            if (savedInstanceState != null) {
                mSearchString = savedInstanceState.getString("mSearchString");
            }

            setContentView(R.layout.search);

            if (mSearchString.length() > 0) {
                findViewById(R.id.searchString);
            }
            mSearcher = new YbkSearcher();
            mButton = ((ImageButton)findViewById(R.id.searchButton));
            mSearchStringEdit = ((EditText)findViewById(R.id.searchString));
            
            mButton.setOnClickListener(new View.OnClickListener() {
                
                public void onClick(View v) {
                    search();
                }
            });
            
            mSearchStringEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    search();
                    return true;
                }
            });
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        } catch (IOException e) {
            // what to do???
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    protected void onDestroy() {
        mSearcher = null;
        super.onDestroy();
    }

    protected void openItem(Object item) {
        setProgressBarIndeterminateVisibility(true);
        startActivity(new Intent(this, YbkViewActivity.class).putExtra(YbkDAO.FILENAME, ((Book) item).fileName));
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            setContentView(R.layout.search);
            setProgressBarIndeterminateVisibility(false);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        try {
            openItem(listView.getItemAtPosition(selectionRowId));
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mSearchString", mSearchString);
    }

    private void search() {
        String searchString = ((EditText)findViewById(R.id.searchString)).getText().toString();
        if (searchString.length() > 0) {
            new SearchTask().execute(searchString);
        }
    }
    private class SearchTask extends AsyncTask<String, Void, List<Map<String, String>>> {

        @Override
        protected List<Map<String, String>> doInBackground(String... params) {
            if (mSearcher != null) {
                try {
                    return mSearcher.search(params[0]);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Map<String,String>> results) {
            super.onPostExecute(results);
            SimpleAdapter adapter = new SimpleAdapter(
                    SearchActivity.this,
                    results,
                    android.R.layout.simple_list_item_1,
                    new String[] {YbkIndexer.TITLE_FIELDNAME},
                    new int[] { android.R.id.text1 });
            setListAdapter(adapter);
        }
    }
}
