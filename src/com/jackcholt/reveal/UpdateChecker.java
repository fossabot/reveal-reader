package com.jackcholt.reveal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

/**
 * Checks for updates to the program
 * 
 * by Dave Packham
 */

public class UpdateChecker {
	public static String marketId;

	public static int getLatestVersionCode() {
		int version = 0;

		try {
			// Get the XML update Version to prompt user to get a new Update from the market
			FlurryAgent.onEvent("UpdateCheck");
			URLConnection cnVersion;
			URL urlVersion = new URL(
					"http://revealreader.thepackhams.com/revealVersion.xml?ClientVer="
							+ Global.SVN_VERSION);
			cnVersion = urlVersion.openConnection();
			cnVersion.setReadTimeout(10000);
			cnVersion.setConnectTimeout(10000);
			cnVersion.setDefaultUseCaches(false);
			cnVersion.connect();
			InputStream streamVersion = cnVersion.getInputStream();

			// Proceed parsing the info
			DocumentBuilder docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document manifestDoc = docBuild.parse(streamVersion);
			NodeList manifestNodeList = manifestDoc.getElementsByTagName("manifest");
			String versionStr = manifestNodeList.item(0).getAttributes().getNamedItem(
					"android:versionCode").getNodeValue();
			version = Integer.parseInt(versionStr);
			// NodeList clcNodeList = manifestDoc.getElementsByTagName("clc");
			// marketId = clcNodeList.item(0).getAttributes().getNamedItem(
			// "marketId").getNodeValue();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			Global.NEW_VERSION = version;
		}
		return version;
	}

	public static void checkForNewerVersion(final Activity activity, final int currentVersion) {
		// Check here an XML file stored on our website for new version info

		Thread t = new Thread() {
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				Looper.prepare();
				Global.NEW_VERSION = getLatestVersionCode();
		        if (Global.NEW_VERSION > currentVersion) {
		            // Tell user to Download a new REV of this cool CODE from the Market
		            // Only Toast if there IS an update
		            activity.runOnUiThread(new Runnable() {

                        public void run() {
                            Toast.makeText(Main.getMainApplication(), R.string.update_available, Toast.LENGTH_LONG)
                            .show();
                        }		                
		            });
		        }
			}
		};
		t.start();

	}

}
