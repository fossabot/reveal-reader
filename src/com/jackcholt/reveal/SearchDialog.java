package com.jackcholt.reveal;

import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class SearchDialog  {
    private final String TAG = "SearchDialog";

    public static void searchPrompt(Activity parentView) {
        LayoutInflater factory = LayoutInflater.from(parentView);
        final View textEntryView = factory.inflate(R.layout.dialog_search, null);
        final EditText et = (EditText) textEntryView.findViewById(R.id.searchString);

        new AlertDialog.Builder(parentView)
                .setTitle(R.string.title_search).setView(textEntryView)
                .setPositiveButton(R.string.title_search, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            YbkSearcher searcher = new YbkSearcher();
                            String topDoc = searcher.search(et.getText().toString());
                        } catch (IOException ioe) {
                            // TODO Auto-generated catch block
                            ioe.printStackTrace();
                        } catch (ParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).create().show();
    }
}
