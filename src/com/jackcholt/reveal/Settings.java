package com.jackcholt.reveal;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //setContentView(R.layout.settings);
        
        // Load the XML preferences file
        addPreferencesFromResource(R.xml.preferences);
        
        // Restore preferences
        /*SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean showSplashScreen = settings.getBoolean("showSplashScreen", true);
        setShowSplashScreen(showSplashScreen);*/
    }
    
    /*@Override
    protected void onStop() {
        super.onStop();
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("showSplashScreen", isShowSplashScreen());
    }*/
        
    @Override
    protected void onResume() {
        super.onResume();
   
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        
    }

    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, 
            final String key) {
        // Let's do something when my counter preference value changes
        /*if (key.equals(KEY_MY_PREFERENCE)) {
            Toast.makeText(this, "Thanks! You increased my count to "
                    + sharedPreferences.getInt(key, 0), Toast.LENGTH_SHORT).show();
        }*/
    }

}
