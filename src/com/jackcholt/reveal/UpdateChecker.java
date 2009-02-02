package com.jackcholt.reveal;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

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
      URL url =
          new URL("http://androidstuff.thepackhams.com/revealVersion.xml");
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
      return version;
      
    }
  }

  public static  void checkForNewerVersion(int currentVersion) {
	  Global.NEW_VERSION = getLatestVersionCode();
    if (Global.NEW_VERSION > currentVersion) {
    	//Download a new REV of this cool CODE
    	
    }
  }




}
