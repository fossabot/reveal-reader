package com.jackcholt.reveal;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

    public class Splash extends Activity {

        private final int SPLASH_DISPLAY_LENGTH = 5000;
        
        /** called when the activity is first created */
        @Override
        public void onCreate( Bundle icicle ){
             super.onCreate( icicle );
             setContentView( R.layout.dialog_about );
             

             /* New Handler to start the Menu-Activity
              * and close this splash creen after some secods
              */
             new Handler().postDelayed(new Runnable(){
                  public void run(){
                       /*Create an Intent that will start the Menu.Activity */
                       Intent mainIntent = new Intent( Splash.this, Main.class );
                       Splash.this.startActivity( mainIntent );
                       Splash.this.finish();
                  }
             }, SPLASH_DISPLAY_LENGTH );
        }
   }
    