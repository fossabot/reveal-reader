package com.jackcholt.reveal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Process;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.PopDialogCheck;
import com.jackcholt.reveal.data.PopDialogDAO;

/**
 * Checks for updates to the MOTD and display them
 * 
 * by Dave Packham, Shon Vella
 */

public class PopupDialogBase extends Dialog {
    private static final String TAG = "PopupDialogBase";
    private static final String ANDROID_NAMESPACE="http://schemas.android.com/apk/res/android";
    
    int mMessageId;
    String mMessage;
    String mPrefix;
    String mMessageURL;
    Activity mParent;
    

    public PopupDialogBase(final Activity parent, final String title, String messageURL, String prefix) {
        super(parent);
        mParent = parent;
        mMessageURL = messageURL;
        mPrefix = prefix;

        FlurryAgent.onEvent(prefix);
        setContentView(R.layout.dialog_dismissable);
        setTitle(title);

        Button close = (Button) findViewById(R.id.close_about_btn);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
        final WebView wv = (WebView) findViewById(R.id.msgView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);

        Thread t = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                if (fetchMessage()) {
                    parent.runOnUiThread(new SafeRunnable() {
                        @Override
                        public void protectedRun() {
                            wv.loadData(mMessage, "text/html", "utf-8");
                            show();
                        }
                    });
                }
            }
        };
        t.start();

    }
    
    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        final CheckBox checkBox = (CheckBox) findViewById(R.id.dismiss_popup_id);

        if (checkBox.isChecked()) {
            PopDialogDAO dao = PopDialogDAO.getInstance(Main.getMainApplication(), PopDialogCheck.DATABASE_NAME,
                    PopDialogCheck.TABLE_CREATE, PopDialogCheck.DATABASE_TABLE, PopDialogCheck.DATABASE_VERSION);

            if (dao != null) {
                ContentValues values = new ContentValues();
                values.put(PopDialogCheck.COL_DIALOGNAME, mPrefix + mMessageId);
                values.put(PopDialogCheck.COL_DISMISSED, "1");
                dao.insert(PopDialogCheck.DATABASE_TABLE, values);
            }

        }
    }

    /**
     * Get the message of the day from the web.
     * 
     * @return The message of the day HTML if available and not already shown, null otherwise
     */
    private boolean fetchMessage() {
        try {
            URLConnection cnVersion = null;
            URL urlVersion = new URL(mMessageURL);
            cnVersion = urlVersion.openConnection();
                cnVersion.setReadTimeout(10000);
                cnVersion.setConnectTimeout(10000);
                cnVersion.setDefaultUseCaches(false);
                cnVersion.connect();
                InputStream streamVersion = cnVersion.getInputStream();
                Document manifestDoc = null;
                try {
                    manifestDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(streamVersion);
                } finally {
                    streamVersion.close();
                }
            NodeList manifestNodeList = manifestDoc.getElementsByTagName("manifest");

            // What version of MOTD is this.
            String messageIdStr = ((Element)manifestNodeList.item(0)).getAttribute("android:" + mPrefix + "Number");
            
            // FIXIT - this is only needed until the different formats standardize on capitalization
            if (messageIdStr.length() == 0) {
                messageIdStr = ((Element)manifestNodeList.item(0)).getAttribute("android:" + mPrefix + "number");
            }
            
            int messageId = Integer.parseInt(messageIdStr);

            // The actual MOTD HTML code to display
            String message = ((Element)manifestNodeList.item(0)).getAttribute("android:" + mPrefix + "message");

            Editor e = Main.getMainApplication().getPreferences(Context.MODE_PRIVATE).edit();
            e.putString(mPrefix, message);
            e.putInt(mPrefix + "_version", messageId);
            e.commit();
            // Check to see if the DB contains this dialog version already dismissed
            // Then don't display

            PopDialogDAO dao = PopDialogDAO.getInstance(mParent, PopDialogCheck.DATABASE_NAME, PopDialogCheck.TABLE_CREATE,
                    PopDialogCheck.DATABASE_TABLE, PopDialogCheck.DATABASE_VERSION);
            if (!dao.isMyDialogDismissed(mPrefix + messageId)) {
                mMessage = message;
                mMessageId = messageId;
                return true;
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot retrieve for message text. " + ioe);
        } catch (ParserConfigurationException pce) {
            Log.e(TAG, "Cannot parse message text: " + pce);
        } catch (FactoryConfigurationError fce) {
            Log.e(TAG, "Cannot parse message text: " + fce.getMessage());
        } catch (SAXException se) {
            Log.e(TAG, "Cannot parse message text: " + se.getMessage());
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Cannot parse message number: " + nfe);
        }
        return false;
    }

}
