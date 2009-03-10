package com.jackcholt.reveal;

import com.flurry.android.FlurryAgent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class Settings extends PreferenceActivity 
		implements OnSharedPreferenceChangeListener { 

	public static final String PREFS_NAME = "com.jackcholt.reveal_preferences";
	
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);
        FlurryAgent.onEvent("SettingScreen");
  }

    @Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);

	}
    
private void readPrefs() {
	Log.d(Global.TAG, "Settings prefs controls");
	//mShowSplashScreen.setChecked(mDisplayIcon);
}

private void revertPrefs() {
	Log.d(Global.TAG, "Reverting prefs");
	//SharedPreferences.Editor ed = Settings.getEditor(this);
	//ed.putBoolean(DISPLAY_DETAILS_KEY, mShowSplashScreen);
	//ed.commit();
}

private void savePrefs() {
	Log.d(Global.TAG, "Saving prefs");
	//SharedPreferences.Editor editor = Settings.getEditor(this);
	//editor.putBoolean(DISPLAY_DETAILS_KEY, mShowSplashScreenCheckbox.isChecked());
	//editor.commit();
}


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
        
  @Override
    protected void onPause() {
        super.onPause();
        
        revertPrefs();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        // Let's do something when my counter preference value changes
        /*if (key.equals(KEY_MY_PREFERENCE)) {
            Toast.makeText(this, "Thanks! You increased my count to "
                    + sharedPreferences.getInt(key, 0), Toast.LENGTH_SHORT).show();
        }*/
    }



}
