package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.YbkDAO;

public class MoveDialog extends ListActivity {
    private final String TAG = "MoveDialog";
    public static final String ADD_FOLDER = "add_folder";
    public static final String MOVE_TO_FOLDER = "move_to_folder";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Util.startFlurrySession(this);
            FlurryAgent.onEvent("MoveDialog");

            setContentView(R.layout.dialog_move);
            registerForContextMenu(getListView());

            List<String> folderList = new ArrayList<String>(YbkDAO.getInstance(this).getFolderMap().keySet());
            String currentFolder = getIntent().getStringExtra("currentFolder");
            if (currentFolder != null && currentFolder.length() != 0) {
                folderList.remove(currentFolder);
                folderList.add(0, getResources().getString(R.string.top_level_folder));
            }

            setListAdapter(new ArrayAdapter<String>(this, R.layout.history_list_row, folderList));

            findAddButton().setOnClickListener(new OnClickListener() {
                public void onClick(final View view) {
                    Log.d(TAG, "Adding a folder");
                    setResult(RESULT_OK, new Intent(getBaseContext(), YbkViewActivity.class).putExtra(ADD_FOLDER, true)
                            .putExtra("fileName", getIntent().getStringExtra("fileName")));
                    finish();
                }

            });
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

    }

    private Button findAddButton() {
        return (Button) findViewById(R.id.addFolderButton);
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {
        try {
            String folder = listView.getItemAtPosition(selectionRowId).toString();
            if (folder.equals(getResources().getString(R.string.top_level_folder))) {
                folder = "";
            }
            setResult(RESULT_OK, new Intent(this, YbkViewActivity.class).putExtra(MOVE_TO_FOLDER, folder).putExtra(
                    "fileName", getIntent().getStringExtra("fileName")));
            finish();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    protected void onStart() {
        try {
            Util.startFlurrySession(this);
            super.onStart();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
            FlurryAgent.onEndSession(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

}
