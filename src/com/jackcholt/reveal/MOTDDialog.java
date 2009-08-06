package com.jackcholt.reveal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
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
 * by Dave Packham
 */

public class MOTDDialog extends Dialog {
    private static final String TAG = "MOTDDialog";
    String MOTDNumberStr;
    static int MOTDNumberInt;
    String MOTDmessage;

    public MOTDDialog(Context _this) {
        super(_this);

        FlurryAgent.onEvent("MOTD");
        setContentView(R.layout.dialog_dismissable);
        
        Button close = (Button) findViewById(R.id.close_about_btn);
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });

        String title;
        title = "Reveal Online Message";
        setTitle(title);

        URLConnection cnVersion = null;
        URL urlVersion = null;

        try {
            urlVersion = new URL("http://revealreader.thepackhams.com/revealMOTD.xml");
        } catch (MalformedURLException e5) {
            e5.printStackTrace();
        }
        try {
            cnVersion = urlVersion.openConnection();
        } catch (IOException e4) {
            e4.printStackTrace();
        }
        cnVersion.setReadTimeout(10000);
        cnVersion.setConnectTimeout(10000);
        cnVersion.setDefaultUseCaches(false);
        try {
            cnVersion.connect();
        } catch (IOException e3) {
            Log.e(TAG, "Cannot connect to the source for MOTD text. " + e3.getMessage());
            this.dismiss();
            return;
        }

        InputStream streamVersion = null;
        try {
            streamVersion = cnVersion.getInputStream();
        } catch (IOException e3) {
            e3.printStackTrace();
        }

        // Proceed parsing the info
        DocumentBuilder docBuild = null;
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e2) {
            e2.printStackTrace();
        } catch (FactoryConfigurationError e2) {
            e2.printStackTrace();
        }
        Document manifestDoc = null;
        try {
            manifestDoc = docBuild.parse(streamVersion);
        } catch (SAXException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        NodeList manifestNodeList = manifestDoc.getElementsByTagName("manifest");

        // What version of MOTD is this.
        MOTDNumberStr = manifestNodeList.item(0).getAttributes().getNamedItem("android:MOTDNumber").getNodeValue();
        MOTDNumberInt = Integer.parseInt(MOTDNumberStr);

        // The actual MOTD HTML code to display
        MOTDmessage = manifestNodeList.item(0).getAttributes().getNamedItem("android:MOTDmessage").getNodeValue();

        try {
            Editor e = Main.getMainApplication().getPreferences(Context.MODE_PRIVATE).edit();
            e.putString("motd", MOTDmessage);
            e.putInt("motd_version", MOTDNumberInt);
            e.commit();

        } catch (Exception e) {
            System.out.println(e.getMessage());

        } finally {
        }

        WebView wv = (WebView) findViewById(R.id.motdView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);
        if (Util.isNetworkUp(_this)) {
            wv.loadData(MOTDmessage, "text/html", "utf-8");
        } else {
            wv.loadData("Cannot get online help.  Your network is currently down.", "text/plain", "utf-8");
        }

        // Check to see if the DB contains this dialog version already dismissed
        // Then don't display
        PopDialogDAO dao = PopDialogDAO.getInstance(_this, PopDialogCheck.DATABASE_NAME, PopDialogCheck.TABLE_CREATE,
                PopDialogCheck.DATABASE_TABLE, PopDialogCheck.DATABASE_VERSION);

        boolean showme = dao.isMyDialogDismissed("MOTDDialog" + MOTDNumberInt);

        if (!showme) {
            show();
        }
    }

    public static void create(final Context _this) {
        FlurryAgent.onEvent("MOTDDialog");

        // Thread t = new Thread() {
        // public void run() {
        // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        // Looper.prepare();

        @SuppressWarnings("unused")
        MOTDDialog dlg = new MOTDDialog(_this);
        // }
        // };
        // t.start();
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
                values.put(PopDialogCheck.COL_DIALOGNAME, "MOTDDialog" + MOTDNumberInt);
                values.put(PopDialogCheck.COL_DISMISSED, "1");
                dao.insert(PopDialogCheck.DATABASE_TABLE, values);
            }

        }
    }
}
