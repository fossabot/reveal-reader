package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.TopDocs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;
import com.nullwire.trace.ExceptionHandler;

public class SearchView extends ListActivity {
    private static final String TAG = "reveal.Search";

    public static int mNotifId = 1;

    private final Handler mHandler = new Handler();
    
    private String mSearchString = "";
    
    private YbkSearcher mSearcher;

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
        startActivity(
                new Intent(this, YbkViewActivity.class)
                    .putExtra(YbkDAO.FILENAME, ((Book) item).fileName));
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

    private class Search extends AsyncTask<String, Void, TopDocs> {

        @Override
        protected TopDocs doInBackground(String... params) {
            if (mSearcher != null) {
                try {
                    return mSearcher.search(params[0]);
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(TopDocs topdocs) {
            super.onPostExecute(topdocs);
            setListAdapter(new TopDocsAdapter(topdocs));
        }

    }
    
    private class TopDocsAdapter extends BaseAdapter {
        TopDocs topdocs;
        
        public TopDocsAdapter(TopDocs topdocs) {
            this.topdocs = topdocs;
        }

        public int getCount() {
            return topdocs.scoreDocs.length;
        }

        public Object getItem(int position) {
        }

        public long getItemId(int position) {
            return topdocs.scoreDocs[position].doc;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
    }
}
