package com.jackcholt.revel;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.jackcholt.revel.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class TitleBrowser extends ListActivity {
	private static final String mSourceURL = "http://www.thecoffeys.net/ebooks/xmlbooks.asp";

	private ArrayList<HashMap<String, Object>> mData;
	private SimpleAdapter adapter;
	private Stack<ArrayList<HashMap<String, Object>>> mBreadCrumb;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //create array for the list
        mData = new ArrayList<HashMap<String, Object>>();
        mBreadCrumb = new Stack<ArrayList<HashMap<String, Object>>>();
        loadList();
        
        //create adapters for view
        mBreadCrumb.push(mData);
        adapter = new SimpleAdapter(this, mData, R.layout.browser_list_item, new String[] { "BookName" }, new int[] { android.R.id.text1 });
        
        //bind array
        setListAdapter(adapter);
    }
    
	@SuppressWarnings("unchecked")
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	HashMap<String, Object> selected = (HashMap<String, Object>) l.getItemAtPosition(position);
    	Object submenu = selected.get("children");
    	
    	if (submenu != null)
    	{
    		mBreadCrumb.push((ArrayList<HashMap<String, Object>>) submenu);
    		adapter = new SimpleAdapter(this, (ArrayList<HashMap<String, Object>>) submenu, R.layout.browser_list_item, new String[] { "BookName" }, new int[] { android.R.id.text1 });
    		setListAdapter(adapter);
    	}
    	else
    	{
    		//TODO: show a screen just for this title with more info with download button
    		System.out.println("Download!");
    	}
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
    	if (keyCode == KeyEvent.KEYCODE_BACK)
    	{
	        if (mBreadCrumb.size() > 1)
	        {
	        	mBreadCrumb.pop();
	        	adapter = new SimpleAdapter(this, mBreadCrumb.peek(), R.layout.browser_list_item, new String[] { "BookName" }, new int[] { android.R.id.text1 });
	        	setListAdapter(adapter);
	        }
	        else
	        {
	        	finish();
	        }
    	}
        
        return false;
    }
    
    private void loadList()
    {
    	try
    	{
    		URL bookUrl = new URL(mSourceURL);
    		
    		SAXParserFactory factory = SAXParserFactory.newInstance();
    		SAXParser parser = factory.newSAXParser();
    		
    		XMLReader reader = parser.getXMLReader();
    		
    		reader.setContentHandler(new ParserHandler());
    		
    		reader.parse(new InputSource(bookUrl.openStream()));
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    private class ParserHandler extends DefaultHandler {
		private HashMap<String, Object> mCurrentHash;
		private String mCurrentTag;
		private int mCount;
		private String mCategory;
		
		private static final String mRootTag = "YanCEyWareBooks";
		private static final String mBookTag = "Book";
		private static final String mBookNameTag = "BookName";
		private static final String mChildrenTag = "children";
		private static final String mCategoryTag = "Category";

		public void startElement(String namespaceURI, String tagName, String qName, Attributes attributes) throws SAXException 
		{
			if (tagName.equals(mBookTag))
			{
				mCurrentHash = new HashMap<String, Object>();
			}
			else if (!tagName.equals(mRootTag))
			{
				mCurrentTag = tagName;
			}
		}
		
		public void endElement(String namespaceURI, String tagName, String qName) throws SAXException 
		{
			if (tagName.equals(mBookTag))
			{
				insertNode(mData);
			}
			else if (!tagName.equals(mRootTag))
			{
				mCurrentTag = null;
			}
		}
		
		public void characters(char inCharacters[], int start, int length)
		{
			if (mCurrentHash != null && mCurrentTag != null)
			{
				mCurrentHash.put(mCurrentTag, new String(inCharacters, start, length));
			}
		}
		
		@SuppressWarnings("unchecked")
		private void insertNode(ArrayList<HashMap<String, Object>> list)
		{
			mCount = 1;
			
			mCategory = (String) mCurrentHash.get(mCategoryTag + mCount++);
			HashMap<String, Object> currentNode = null;
			
			while (mCount < 4 && mCategory != null && !mCategory.equals(""))
			{
				currentNode = getNode(mCategory, list);
				
				if (currentNode != null)
				{
					list = (ArrayList<HashMap<String, Object>>) currentNode.get(mChildrenTag);
				}
				else
				{
					HashMap<String, Object> newCategory = new HashMap<String, Object>(2, 1);
					{
						newCategory.put(mBookNameTag, mCategory);
						newCategory.put(mChildrenTag, new ArrayList<HashMap<String, Object>>());
					}
					
					list.add(newCategory);
					list = (ArrayList<HashMap<String, Object>>) newCategory.get(mChildrenTag);
				}
				
				mCategory = (String) mCurrentHash.get(mCategoryTag + mCount++);
			}
			
			list.add(mCurrentHash);
			
		}
		
		private HashMap<String, Object> getNode(String inName, ArrayList<HashMap<String, Object>> list)
		{
			HashMap<String, Object> node = null;
			
			for (HashMap<String, Object> current : list)
			{
				Object name = current.get(mBookNameTag);
				if (name != null && name instanceof String)
				{
					if ( ((String) name).equals(inName) )
					{
						node = current;
						break;
					}
				}
			}
			
			return node;
		}
	}
}