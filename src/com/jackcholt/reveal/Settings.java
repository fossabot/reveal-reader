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
        super.onCreate(savedInstanceState);
        
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);
        
        FlurryAgent.onEvent("SettingScreen");
        
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(final SharedPreferences sharedPref, final String key) {
                    if (key.equals(EBOOK_DIRECTORY_KEY)) {
                        String ebookDir = sharedPref.getString(EBOOK_DIRECTORY_KEY, 
                                DEFAULT_EBOOK_DIRECTORY);

                        if (!ebookDir.startsWith("/sdcard")) {
                	    	String ebookDirTemp = ebookDir;

                	    	if(!ebookDir.startsWith("/")){
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
        FlurryAgent.onEndSession();

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
