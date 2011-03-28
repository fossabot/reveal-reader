package com.jackcholt.reveal;

import java.io.IOException;
import java.io.InputStream;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * LicenseDialog to meet legal requirements
 * 
 * by Shon Vella
 */

public class LicenseDialog extends Dialog {
    private static final String TAG = LicenseDialog.class.getSimpleName();

    public LicenseDialog(Context _this) {
        super(_this);
        setContentView(R.layout.dialog_license);

        ((Button) findViewById(R.id.close_about_btn)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
        setTitle(R.string.title_license);

        TextView licenseText = (TextView) findViewById(R.id.license_text);
        InputStream inputFile = _this.getResources().openRawResource(R.raw.license);
        try {
            byte buf[] = new byte[inputFile.available()];
            inputFile.read(buf);
            licenseText.setText(new String(buf, "UTF-8"));
        } catch (IOException e) {
            licenseText.setText("Unable to find license text");
        } finally {
            try {
                inputFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
        findViewById(R.id.content_layout).forceLayout();
    }

    public static LicenseDialog create(Context _this) {
        LicenseDialog dlg = new LicenseDialog(_this);
        
        try {
            dlg.show();
        } catch (WindowManager.BadTokenException bte) {
            // This gets thrown if the Activity associated with _this is not running. Do nothing
            Log.w(TAG, "Bad token associated with the context");
        }
        
        return dlg;
    }
}