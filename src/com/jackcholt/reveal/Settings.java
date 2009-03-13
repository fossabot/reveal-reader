package com.jackcholt.reveal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.flurry.android.FlurryAgent;


public class Settings extends PreferenceActivity { 

    public static final String EBOOK_DIRECTORY_KEY = "default_ebook_dir";
    public static final String EBOOK_DIR_CHANGED = "ebook_dir_changed";
	public static final String PREFS_NAME = "com.jackcholt.reveal_preferences";
	
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);
        
        FlurryAgent.onEvent("SettingScreen");
        
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(final SharedPreferences sharedPref, final String key) {
                    if (key.equals(EBOOK_DIRECTORY_KEY)) {
                        String ebookDir = sharedPref.getString(EBOOK_DIRECTORY_KEY, "/sdcard/reveal/ebooks/");
                        
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
                }
            });
  }

    /*@Override
	public void onResume() {
		super.onResume();
		
		

	}
*/    

	@Override
    protected void onStop() {
        super.onStop();
   
        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        //editor.putBoolean("showSplashScreen", showSplashScreen);

        // Don't forget to commit your edits!!!
        editor.commit();

    }
        
/*  @Override
    protected void onPause() {
        super.onPause();
        
        //revertPrefs();
        // Unregister the listener whenever a key changes
        //getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        
    }
*/
    



}
