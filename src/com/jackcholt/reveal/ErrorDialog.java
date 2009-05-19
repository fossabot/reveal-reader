package com.jackcholt.reveal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

/**
 * ErrorDialog for informing users of something that happening
 *
 * @author Dave Packham
 * @author Shon Vella
 */

public class ErrorDialog extends Activity {

    private static final String INFO = "info";

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Change DEBUG to "0" in Global.java when building a RELEASE Version for the GOOGLE APP MARKET
        // This allows for real usage stats and end user error reporting
        if (Global.DEBUG == 0) {
            // Release Key for use of the END USERS
            FlurryAgent.onStartSession(this, "BLRRZRSNYZ446QUWKSP4");
        } else {
            // Development key for use of the DEVELOPMENT TEAM
            FlurryAgent.onStartSession(this, "VYRRJFNLNSTCVKBF73UP");
        }
        FlurryAgent.onEvent("ErrorDialog");
        setContentView(R.layout.error_dialog);

        final CheckBox exitCheckBox = (CheckBox) findViewById(R.id.exit_reveal_btn);

        final String info = getIntent().getStringExtra(INFO);
        if (info == null) {
            finish();
            return;
        }

        final TextView confirmTextView = (TextView) findViewById(R.id.confirm_error_send_stacktrace);
        confirmTextView.setText(info);

        final Button sendButton = (Button) findViewById(R.id.send_error_btn);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                if (exitCheckBox.isChecked()) {
                    ReportError.reportErrorToWebsite("UNCAUGHT_EXCEPTION_" + info);
                    shutdown();
                } else {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info);
                }
            }
        });

        Button dontSendButton = (Button) findViewById(R.id.dont_send_error_btn);
        dontSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                if (exitCheckBox.isChecked()) {
                    shutdown();                }
            }
        });
        FlurryAgent.onEvent("ErrorDialog");
        setTitle(R.string.error_title);
    }

    /**
     * Launch the serious error dialog (actually an activity)
     *
     * @param _this
     *            the source context
     * @param t
     *            the exception
     * @param strings
     *            option extra information items
     * @return
     */
    public static void start(Context _this, Throwable t, String... strings) {
        // build the info string
        StringBuilder sb = new StringBuilder();
        if (strings != null && strings.length > 0) {
            for (String string : strings) {
                sb.append(string);
                sb.append('\n');
            }
        }
        if (t != null) {
            sb.append("stacktrace:\n");
            sb.append(Util.getStackTrace(t));
        }
        final String info = sb.toString();

        Intent intent = new Intent(_this, ErrorDialog.class);
        intent.putExtra(INFO, info);
        _this.startActivity(intent);
    }

    public void shutdown() {
        // try to shut down some things gracefully
        YbkService.stop(ErrorDialog.this);
        System.runFinalizersOnExit(true);
        // schedule actual shutdown request on a background thread to give the service a chance to stop on the
        // foreground thread
        new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                System.exit(-1);
            }
        }).start();
    }

}