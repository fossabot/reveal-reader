package com.jackcholt.reveal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity that allows Main to reload itself when necessary.
 * 
 * @author shon
 *
 */
public class ReloadMainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, Main.class));
        finish();
    }
}
