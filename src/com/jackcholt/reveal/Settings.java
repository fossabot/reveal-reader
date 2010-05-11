package com.jackcholt.reveal;

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
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

public class Settings extends PreferenceActivity {

    public static final String DEFAULT_EBOOK_DIRECTORY = Environment.getExternalStorageDirectory() + "/reveal/ebooks/";
    public static final String EBOOK_DIRECTORY_KEY = "default_ebook_dir";
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

            Util.startFlurrySession(this);
            FlurryAgent.onEvent("SettingScreen");

            EditTextPreference defaultEbookDir = (EditTextPreference) findPreference("default_ebook_dir");
            defaultEbookDir.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((String) newValue).startsWith(Environment.getExternalStorageDirectory().toString())) {
                        returnIntent.putExtra(EBOOK_DIR_CHANGED, true);
                        return true;
                    }

                    String ebookDirBadMsg = getResources().getString(R.string.ebook_dir_invalid,
                            Environment.getExternalStorageDirectory().toString());

                    Toast.makeText(prefContext, ebookDirBadMsg, Toast.LENGTH_LONG).show();

                    return false;
                }
            });

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        public void onSharedPreferenceChanged(final SharedPreferences sharedPref, final String key) {
                            Log.d(TAG, "In onSharedPreferenceChanged");
                            try {
                                if (!key.equals(EBOOK_DIRECTORY_KEY)) {
                                    return;
                                }

                                String ebookDir = sharedPref.getString(EBOOK_DIRECTORY_KEY, DEFAULT_EBOOK_DIRECTORY);

                                if (!ebookDir.startsWith(Environment.getExternalStorageDirectory().toString())) {
                                    String ebookDirTemp = ebookDir;

                                    if (!ebookDir.startsWith("/")) {
                                        ebookDir = Environment.getExternalStorageDirectory().toString() + "/" + ebookDirTemp;
                                    } else {
                                        ebookDir = Environment.getExternalStorageDirectory().toString() + ebookDirTemp;
                                    }
                                    Editor edit = sharedPref.edit();
                                    edit.putString(EBOOK_DIRECTORY_KEY, ebookDir);
                                    edit.commit();

                                    Log.d(TAG, "default ebook directory changed to: " + ebookDir);

                                    // exit here to avoid recursiveness
                                    return;
                                }

                                if (!ebookDir.endsWith("/")) {
                                    ebookDir += "/";
                                    Editor edit = sharedPref.edit();
                                    edit.putString(EBOOK_DIRECTORY_KEY, ebookDir);
                                    edit.commit();

                                    // exit here to avoid recursiveness
                                    return;
                                }

                                // if the ebook directory changed, recreate
                                Util.createDefaultDirs(getBaseContext());
                                setResult(RESULT_OK, returnIntent.putExtra(EBOOK_DIR_CHANGED, true));

                            } catch (RuntimeException rte) {
                                Util.unexpectedError(Settings.this, rte);
                            } catch (Error e) {
                                Util.unexpectedError(Settings.this, e);
                            }
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
            // Save user preferences. We need an Editor object to make changes. All objects are from
            // android.context.Context
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            // Don't forget to commit your edits!!!
            editor.commit();
            setResult(RESULT_OK, returnIntent);
            FlurryAgent.onEndSession(this);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}
