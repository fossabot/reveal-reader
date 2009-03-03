package com.jackcholt.reveal;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;
import android.view.View;
import android.widget.Button;

public class AboutDialog extends Dialog {
        public AboutDialog(Context context) {
                super(context);
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
                                 
                        //String.format("This is a string %s, this is a decimal %d", R.string.helpdialog_no_ebooks, Global.SVN_VERSION);
                        
                        setTitle(title);
         
                } catch (NameNotFoundException e) {
                        title = "Unknown version";
                }
                
        }

        public static AboutDialog create(Context context) {
                AboutDialog dlg = new AboutDialog(context);
                dlg.show();
                return dlg;
        }
}