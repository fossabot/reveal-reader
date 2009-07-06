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
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.webkit.WebView;

import com.flurry.android.FlurryAgent;

/**
 * Checks for updates to the MOTD and display them
 * 
 * by Dave Packham
 */


public class MOTDDialog extends Dialog {
    public MOTDDialog(Context _this) {
        super(_this);

        FlurryAgent.onEvent("MOTD");
        setContentView(R.layout.dialog_motd);
        String title;
        title = "Reveal Online MOTD";
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
            e3.printStackTrace();
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
        String versionStr = manifestNodeList.item(0).getAttributes().getNamedItem("android:versionCode")
                .getNodeValue();
        int version = Integer.parseInt(versionStr);

        String messageStr = manifestNodeList.item(0).getAttributes().getNamedItem("android:MOTDmessage")
                .getNodeValue();

        try {
            Editor e = Main.getMainApplication().getPreferences(Context.MODE_PRIVATE).edit();
            e.putString("motd", messageStr);
            e.putInt("motd_version", version);
            e.commit();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        
        } finally {
            Global.NEW_VERSION = version;
        }
        
        WebView wv = (WebView) findViewById(R.id.motdView);
        wv.clearCache(true);
        wv.getSettings().setJavaScriptEnabled(true);
        if (Util.isNetworkUp(_this)) {
            wv.loadData(messageStr, "text/html", "utf-8");
        } else {
            wv.loadData("Cannot get online help.  Your network is currently down.", "text/plain", "utf-8");
        }

        show();
    }

    public static MOTDDialog create(Context _this) {
        // Change DEBUG to "0" in Global.java when building a RELEASE Version
        // for the GOOGLE APP MARKET
        // This allows for real usage stats and end user error reporting
        if (Global.DEBUG == 0) {
            // Release Key for use of the END USERS
            FlurryAgent.onStartSession(_this, "BLRRZRSNYZ446QUWKSP4");
        } else {
            // Development key for use of the DEVELOPMENT TEAM
            FlurryAgent.onStartSession(_this, "VYRRJFNLNSTCVKBF73UP");
        }
        FlurryAgent.onEvent("MOTDDialog");
        
        MOTDDialog dlg = new MOTDDialog(_this);
        return dlg;
    }

    /** Called when the activity is going away. */
    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(Main.getMainApplication());
    }
}
