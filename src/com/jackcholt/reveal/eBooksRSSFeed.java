package com.jackcholt.reveal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.widget.ArrayAdapter;


/**
 * eBooksRSSFeed is for notifying people of new eBooks available for download  :)
 * 
 * by Dave Packham
 */

public class eBooksRSSFeed extends ListActivity {
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.ebook_rss_feed);
		GradientDrawable grad = new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {
				Color.GRAY, Color.WHITE });
		try {
			String xml = getXml("http://www.thecoffeys.net/ebooks/rss.asp");
			this.getWindow().setBackgroundDrawable(grad);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xml);
			String[] items = { "hi" };
			setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
					items));
		} catch (Exception e) {
		}
	}

	public String getXml(String strURL) {
		StringBuffer xml = null;
		URL url = null;
		URLConnection conn = null;
		BufferedReader in = null;
		try {
			url = new URL(strURL);
			conn = url.openConnection();
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			xml = new StringBuffer();
			while ((line = in.readLine()) != null) {
				xml.append(line);
			}
		} catch (Exception e) {
		} finally {
		}

		return xml.toString();
	}
}