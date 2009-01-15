package com.jackcholt.revel;

import java.util.Stack;

import android.app.ListActivity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * List activity to show categories and titles under those categories
 * 
 * @author jwiggins
 * 
 */
public class TitleBrowser extends ListActivity {

	private static final int UPDATE_ID = Menu.FIRST;
	
	private SimpleCursorAdapter adapter;
	private Stack<Uri> mBreadCrumb;
	private Cursor mListCursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBreadCrumb = new Stack<Uri>();

		// setContentView(R.layout.browser_main);
		
		//inform the users we are checking for new ebooks.  so we dont appear locked up
		Toast.makeText(this, "Checking for new eBooks", Toast.LENGTH_SHORT).show();
		
		// establish data connection
		Uri categoryUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI,
				"categoryparent");
		Uri rootCategories = ContentUris.withAppendedId(categoryUri, 0);
		mListCursor = managedQuery(rootCategories, new String[] {
				TitleProvider.Categories.NAME, TitleProvider.Categories._ID },
				null, null, null);

		// create adapters for view
		mBreadCrumb.push(rootCategories);
		adapter = new SimpleCursorAdapter(this, R.layout.browser_list_item,
				mListCursor, new String[] { TitleProvider.Categories.NAME },
				new int[] { android.R.id.text1 });

		setListAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean success = super.onCreateOptionsMenu(menu);
		
		menu.add(Menu.NONE, UPDATE_ID, Menu.NONE, R.string.menu_update);
		
		return success;
	}
	
	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch(item.getItemId()) {
		case UPDATE_ID:
			getContentResolver().update(
					Uri.withAppendedPath(TitleProvider.CONTENT_URI, "update"),
					null, null, null);
			return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (mBreadCrumb.size() < 3) {
			Uri baseUri;
			String[] projection;
			String[] from;

			if (mBreadCrumb.size() == 2) {
				baseUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI,
						"titlecategory");
				projection = new String[] { TitleProvider.Titles.BOOKNAME,
						TitleProvider.Titles._ID };
				from = new String[] { TitleProvider.Titles.BOOKNAME };
			} else {
				baseUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI,
						"categoryparent");
				projection = new String[] { TitleProvider.Categories.NAME,
						TitleProvider.Categories._ID };
				from = new String[] { TitleProvider.Categories.NAME };
			}

			Uri lookupUri = ContentUris.withAppendedId(baseUri, id);
			mListCursor = managedQuery(lookupUri, projection, null, null, null);

			adapter = new SimpleCursorAdapter(this, R.layout.browser_list_item,
					mListCursor, from, new int[] { android.R.id.text1 });

			setListAdapter(adapter);

			mBreadCrumb.push(lookupUri);
		} else {
			Log.d(this.getPackageName(), "Load display for title " + id);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mBreadCrumb.size() > 1) {
				mBreadCrumb.pop();

				mListCursor = managedQuery(mBreadCrumb.peek(), new String[] {
						TitleProvider.Categories.NAME,
						TitleProvider.Categories._ID }, null, null, null);
				adapter = new SimpleCursorAdapter(this,
						R.layout.browser_list_item, mListCursor,
						new String[] { TitleProvider.Categories.NAME },
						new int[] { android.R.id.text1 });
				setListAdapter(adapter);
			} else {
				finish();
			}
		}

		return false;
	}
}