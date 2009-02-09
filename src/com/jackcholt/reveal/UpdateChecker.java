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

import android.content.Context;
import android.widget.Toast;

/**
 * Checks for updates to the game.
 */

public class UpdateChecker {
  public static String marketId;
  

  @SuppressWarnings("finally")
  public static int getLatestVersionCode() {
    int version = 0;
    try {
      URLConnection cn;
      URL url = new URL("http://androidstuff.thepackhams.com/revealVersion.xml?ClientVer=" + Global.SVN_VERSION);
      cn = url.openConnection();
      cn.connect();
      InputStream stream = cn.getInputStream();
      DocumentBuilder docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document manifestDoc = docBuild.parse(stream);
      NodeList manifestNodeList = manifestDoc.getElementsByTagName("manifest");
      String versionStr =
          manifestNodeList.item(0).getAttributes().getNamedItem("android:versionCode")
              .getNodeValue();
      version = Integer.parseInt(versionStr);
      NodeList clcNodeList = manifestDoc.getElementsByTagName("clc");
      marketId = clcNodeList.item(0).getAttributes().getNamedItem("marketId").getNodeValue();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FactoryConfigurationError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
    	Global.NEW_VERSION = version;
      return version;
    }
  }

  public static  void checkForNewerVersion(int currentVersion, Context _this) {
	  //Check here an XML file stored on our website for new version info
      Toast.makeText(_this, R.string.checking_for_new_version_online, Toast.LENGTH_SHORT).show();
      //Toast.makeText(_this, R.string.version_you_are_running + Global.SVN_VERSION, Toast.LENGTH_SHORT).show();
	  Global.NEW_VERSION = getLatestVersionCode();
    if (Global.NEW_VERSION > currentVersion) {
    	//Download a new REV of this cool CODE
        Toast.makeText(_this, R.string.update_available, Toast.LENGTH_LONG).show();
    }
  }




}
