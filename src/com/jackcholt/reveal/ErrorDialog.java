package com.jackcholt.reveal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;


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
        setContentView(R.layout.dialog_error);

        final CheckBox exitCheckBox = (CheckBox) findViewById(R.id.exit_reveal_btn);
        final CheckBox sendEmailCheckBox = (CheckBox) findViewById(R.id.send_email_btn);

        final String info = getIntent().getStringExtra(INFO);
        if (info == null) {
            finish();
            return;
        }

        findConfirmTextView().setText(info);

        findSendButton().setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                if (exitCheckBox.isChecked()) {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info, false);
                    shutdown();
                } else if (sendEmailCheckBox.isChecked()) {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info, true);
                } else if (sendEmailCheckBox.isChecked() && (exitCheckBox.isChecked())) {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info, true);
                    shutdown();
                } else if (!sendEmailCheckBox.isChecked() && (!exitCheckBox.isChecked())) {
                    ReportError.reportError("UNCAUGHT_EXCEPTION_" + info, false);
                }
            }
        });

        findDontSendButton().setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
                if (exitCheckBox.isChecked()) {
                    shutdown();
                }
            }
        });
        setTitle(R.string.unexpected_error_title);
    }

    private Button findSendButton() {
        return (Button) findViewById(R.id.send_error_btn);
    }

    private TextView findConfirmTextView() {
        return (TextView) findViewById(R.id.confirm_error_send_stacktrace);
    }

    private Button findDontSendButton() {
        return (Button) findViewById(R.id.dont_send_error_btn);
    }

    /**
     * Launch the serious error dialog (actually an activity)
     * 
     * @param _this the source context
     * @param t the exception
     * @param strings option extra information items
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

    @Override
    protected void onStart() {
        try {
            super.onStart();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        try {
            super.onStop();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }
}