package com.jackcholt.reveal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.flurry.android.FlurryAgent;

public class Settings extends PreferenceActivity {

    public static final String DEFAULT_EBOOK_DIRECTORY = "/sdcard/reveal/ebooks/";
    public static final String EBOOK_DIRECTORY_KEY = "default_ebook_dir";
    public static final String EBOOK_DIR_CHANGED = "ebook_dir_changed";
    public static final String PREFS_NAME = "com.jackcholt.reveal_preferences";
    public static final String HISTORY_ENTRY_AMOUNT_KEY = "history_entry_amount";
    public static final int DEFAULT_HISTORY_ENTRY_AMOUNT = 30;
    public static final String BOOKMARK_ENTRY_AMOUNT_KEY = "bookmark_entry_amount";
    public static final int DEFAULT_BOOKMARK_ENTRY_AMOUNT = 20;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            // Change DEBUG to "0" in Global.java when building a RELEASE Version
            // for the GOOGLE APP MARKET
            // This allows for real usage stats and end user error reporting
            if (Global.DEBUG == 0) {
                // Release Key for use of the END USERS
                FlurryAgent.onStartSession(this, "BLRRZRSNYZ446QUWKSP4");
            } else {
                // Development key for use of the DEVELOPMENT TEAM
                FlurryAgent.onStartSession(this, "VYRRJFNLNSTCVKBF73UP");
            }

            // Load the XML preferences file
            addPreferencesFromResource(R.xml.preferences);

            FlurryAgent.onEvent("SettingScreen");

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        public void onSharedPreferenceChanged(final SharedPreferences sharedPref, final String key) {
                            try {
                                if (key.equals(EBOOK_DIRECTORY_KEY)) {
                                    String ebookDir = sharedPref
                                            .getString(EBOOK_DIRECTORY_KEY, DEFAULT_EBOOK_DIRECTORY);

                                    if (!ebookDir.startsWith("/sdcard")) {
                                        String ebookDirTemp = ebookDir;

                                        if (!ebookDir.startsWith("/")) {
                                            ebookDir = "/sdcard/" + ebookDirTemp;
                                        } else {
                                            ebookDir = "/sdcard" + ebookDirTemp;

                                        }
                                        Editor edit = sharedPref.edit();
                                        edit.putString(EBOOK_DIRECTORY_KEY, ebookDir);
                                        edit.commit();

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
                                    Intent intent = new Intent(getBaseContext(), Main.class);
                                    intent.putExtra(EBOOK_DIR_CHANGED, true);
                                    setResult(RESULT_OK, intent);
                                }
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

    /*
     * @Override public void onResume() { super.onResume();
     * 
     * 
     * 
     * }
     */

    @Override
    protected void onStop() {
        super.onStop();

        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        // editor.putBoolean("showSplashScreen", showSplashScreen);

        // Don't forget to commit your edits!!!
        editor.commit();
        FlurryAgent.onEndSession(Main.getMainApplication());

    }

    /*
     * @Override protected void onPause() { super.onPause();
     * 
     * //revertPrefs(); // Unregister the listener whenever a key changes
     * //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
     * 
     * }
     */

}
