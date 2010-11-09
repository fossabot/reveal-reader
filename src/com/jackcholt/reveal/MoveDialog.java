package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.jackcholt.reveal.data.YbkDAO;

public class MoveDialog extends ListActivity {
    private final String TAG = "MoveDialog";
    public static final String ADD_FOLDER = "add_folder";
    public static final String MOVE_TO_FOLDER = "move_to_folder";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.dialog_move);
            registerForContextMenu(getListView());

            List<String> folderList = new ArrayList<String>(YbkDAO.getInstance(this).getFolderMap().keySet());
            String currentFolder = getIntent().getStringExtra("currentFolder");
            if (currentFolder != null && currentFolder.length() != 0) {
                folderList.remove(currentFolder);
                folderList.add(0, getResources().getString(R.string.top_level_folder));
            }

            setListAdapter(new IconicAdapter(this, folderList));

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

    private class IconicAdapter extends ArrayAdapter<String> {
        IconicAdapter(Context context, List<String> list) {
            super(context, R.layout.folder_list_row, list);
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.folder_list_row, null);
            }

            TextView label = (TextView) row.findViewById(R.id.label);
            ImageView icon = (ImageView) row.findViewById(R.id.icon);

            String item = getItem(position);

            String labelText = item.toString();
            label.setText(labelText);

            if (labelText.equals(getResources().getString(R.string.top_level_folder))) {
                icon.setImageResource(R.drawable.home24);
            } else {
                icon.setImageResource(R.drawable.folder24y);
            }
            return row;
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
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

}
