package com.jackcholt.reveal;

import java.io.File;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class FileManagerPreference extends EditTextPreference {
    private Context mContext;
    private final String TAG = "FileManagerPreference";

    public FileManagerPreference(Context context) {
        super(context);
        mContext = context;
    }

    public FileManagerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FileManagerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onClick() {
        if (!(mContext instanceof Settings)) {
            Log.e(TAG, "Context is not the Settings class.");
            return;
        }

        try {
            ((Settings) mContext).startActivityForResult(
                    new Intent("org.openintents.action.PICK_DIRECTORY")
                            .putExtra("org.openintents.extra.TITLE",
                                    mContext.getResources().getString(R.string.choose_lib_folder))
                            .putExtra("org.openintents.extra.BUTTON_TEXT",
                                    mContext.getResources().getString(R.string.use_folder))
                            .setData(Uri.parse(buildLibDir())), Main.ACTIVITY_SETTINGS);
        } catch (ActivityNotFoundException anfe) {
            Main.displayToastMessage("To change your library folder, you must first install the OI File Manager.");
            Log.e(TAG, "OI File Manager is not installed? " + anfe.getMessage());
        }
    }

    private String buildLibDir() {
        Log.d(TAG, "getText() returns '" + getText() + "'");
        return "file://" + ((null == getText()) ? Settings.DEFAULT_EBOOK_DIRECTORY : getText());
    }
    
    /*@Override
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();
        
        //mText = text;
        
        persistString(text);
        
        final boolean isBlocking = shouldDisableDependents(); 
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }*/
}
