package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;
import android.view.View;
import android.widget.Button;


/**
 * AboutDialog for telling people who we be  :)
 * 
 * by Dave Packham
 */

public class AboutDialog extends Dialog {
        public AboutDialog(Context _this) {
                super(_this);
                setContentView(R.layout.about);

                Button close = (Button) findViewById(R.id.close_about_btn);
                close.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                                dismiss();
                        }
                });
                String title;
                //String escapedTitle = TextUtil.htmlEncode(title);
                try {
                	//String resultsTextFormat = getContext().getResources().getString(R.string.build_number);
                	//String resultsText = String.format(resultsTextFormat, Global.SVN_VERSION);
                	//CharSequence styledResults = Html.fromHtml(resultsText);
                	
                        title = getContext().getString(R.string.app_name) + 
                        " : " + getContext().getPackageManager().getPackageInfo
                        (getContext().getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
                                 
                        //Grab the Global updated version instead of a static one
                        title += String.format(" %d", Global.SVN_VERSION);
                        
                        setTitle(title);
         
                } catch (NameNotFoundException e) {
                        title = "Unknown version";
                }
                
        }

        public static AboutDialog create(Context _this) {
                AboutDialog dlg = new AboutDialog(_this);
                dlg.show();
                return dlg;
        }
}