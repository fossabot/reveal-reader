package com.jackcholt.reveal;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.WindowManager;
import android.widget.Toast;

public class Settings extends PreferenceActivity {

    public static final String DEFAULT_EBOOK_DIRECTORY = Environment.getExternalStorageDirectory().toString()
            + "/reveal/ebooks/";
    public static final String EBOOK_DIRECTORY_KEY = "default_ebook_dir";
    public static final String KEEP_SCREEN_ON_KEY = "keep_screen_on";
    public static final String EBOOK_DIR_CHANGED = "ebook_dir_changed";
    public static final String PREFS_NAME = "com.jackcholt.reveal_preferences";
    public static final String HISTORY_ENTRY_AMOUNT_KEY = "history_entry_amount";
    public static final int DEFAULT_HISTORY_ENTRY_AMOUNT = 30;
    public static final String BOOKMARK_ENTRY_AMOUNT_KEY = "bookmark_entry_amount";
    public static final int DEFAULT_BOOKMARK_ENTRY_AMOUNT = 20;
    public static final String EBOOK_FONT_SIZE_KEY = "default_font_size";
    public static final String DEFAULT_EBOOK_FONT_SIZE = "18";
    protected static final String TAG = "Settings";
    final Context prefContext = this;
    private Intent returnIntent;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            returnIntent = new Intent(getBaseContext(), Main.class);
            // Load the XML preferences file
            addPreferencesFromResource(R.xml.preferences);

            // always return an OK result
            setResult(RESULT_OK, returnIntent);

            ((EditTextPreference) findPreference(EBOOK_DIRECTORY_KEY))
                    .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (((String) newValue).startsWith(Environment.getExternalStorageDirectory().toString())) {
                                returnIntent.putExtra(EBOOK_DIR_CHANGED, true);
                                return true;
                            }

                            Toast.makeText(prefContext, getResources().getString(R.string.ebook_dir_invalid, //
                                    Environment.getExternalStorageDirectory().toString()), Toast.LENGTH_LONG).show();

                            return false;
                        }
                    });

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        public void onSharedPreferenceChanged(final SharedPreferences sharedPref, final String key) {
                            Log.d(TAG, "In onSharedPreferenceChanged");
                            try {
                                if (key.equals(EBOOK_DIRECTORY_KEY)) {
                                    changeEbookDir(sharedPref);
                                    return;
                                }

                                if (key.equals(KEEP_SCREEN_ON_KEY)) {
                                    if (sharedPref.getBoolean("keep_screen_on", false)) {
                                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                    } else {
                                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                    }
                                    return;
                                }

                                return;
                            } catch (RuntimeException rte) {
                                Util.unexpectedError(Settings.this, rte);
                            } catch (Error e) {
                                Util.unexpectedError(Settings.this, e);
                            }
                        }

                        private void changeEbookDir(final SharedPreferences sharedPref) {
                            String ebookDir = sharedPref.getString(EBOOK_DIRECTORY_KEY, DEFAULT_EBOOK_DIRECTORY);
                            ebookDir += ebookDir.endsWith(File.separator) ? "" : File.separator;

                            if (!ebookDir.startsWith(Environment.getExternalStorageDirectory().toString())) {
                                ebookDir = Environment.getExternalStorageDirectory().toString()
                                        + (ebookDir.startsWith(File.separator) ? "" : File.separator) + ebookDir;

                                Log.d(TAG, "default ebook directory changed to: " + ebookDir);
                            }

                            // if the ebook directory changed, recreate
                            Util.createDefaultDirs(getBaseContext());
                            setResult(RESULT_OK, returnIntent.putExtra(EBOOK_DIR_CHANGED, true));

                            Editor edit = sharedPref.edit();
                            edit.putString(EBOOK_DIRECTORY_KEY, ebookDir);
                            edit.commit();
                        }
                    });
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
            // Save user preferences. We need an Editor object to make changes. All objects are from
            // android.context.Context
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            // Don't forget to commit your edits!!!
            editor.commit();
            setResult(RESULT_OK, returnIntent);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
