package com.jackcholt.reveal;

import java.util.Stack;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
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
	private static final int UPDATE_TOKEN = 12; //random number
	
	private SimpleCursorAdapter adapter;
	private Stack<Uri> mBreadCrumb;
	private Cursor mListCursor;
	private QueryHandler mQueryHandler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBreadCrumb = new Stack<Uri>();

		// setContentView(R.layout.browser_main);
		
		mQueryHandler = new QueryHandler(this);
		
		// If this database doesn't have anything to display, let's load it from
		// the embedded file
		mListCursor = getContentResolver().query(
					Uri.withAppendedPath(TitleProvider.CONTENT_URI,	"category"),
					new String[] { TitleProvider.Categories._ID }, null, null, null);

		if (mListCursor.getCount() == 0) {
			Toast.makeText(this, R.string.checking_ebooks, Toast.LENGTH_SHORT).show();
			
			mQueryHandler.startUpdate(UPDATE_TOKEN, null,
					Uri.withAppendedPath(TitleProvider.CONTENT_URI, "updatefile"),
					null, null, null);
		}

		mListCursor.close();
		
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
		
		menu.add(Menu.NONE, UPDATE_ID, Menu.NONE, R.string.menu_update)
				.setIcon(android.R.drawable.ic_menu_share);
		
		return success;
	}
	
	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch(item.getItemId()) {
		case UPDATE_ID:
			Toast.makeText(this, R.string.checking_ebooks, Toast.LENGTH_SHORT).show();
			
			mQueryHandler.startUpdate(UPDATE_TOKEN, null,
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
	
	private final class QueryHandler extends AsyncQueryHandler {
		private Context mContext;
		
		public QueryHandler(Context context) {
            super(context.getContentResolver());
            mContext = context;
        }

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			super.onUpdateComplete(token, cookie, result);
			
			Toast.makeText(mContext, R.string.ebook_update_complete, Toast.LENGTH_SHORT).show();
		}
	}
}