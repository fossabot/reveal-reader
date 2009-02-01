package com.jackcholt.reveal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
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
	
	private SimpleCursorAdapter mAdapter;
	private Stack<Uri> mBreadCrumb;
	private Cursor mListCursor;
	private QueryHandler mQueryHandler;
	private URL mDownloadUrl;
	SharedPreferences mSharedPref;
	
	final Handler mHandler = new Handler();
	
	/** Used after return from downloading new titles **/
	final Runnable mUpdateResults = new Runnable() {
        public void run() {
            downloadComplete();
        }
    };

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBreadCrumb = new Stack<Uri>();

		setContentView(R.layout.browser_main);
		
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
		mAdapter = new SimpleCursorAdapter(this, R.layout.browser_list_item,
				mListCursor, new String[] { TitleProvider.Categories.NAME },
				new int[] { R.id.book_name });

		setListAdapter(mAdapter);
		
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
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

			mAdapter = new SimpleCursorAdapter(this, R.layout.browser_list_item,
					mListCursor, from, new int[] { R.id.book_name });

			setListAdapter(mAdapter);

			mBreadCrumb.push(lookupUri);
		} else {
			TitleDialog dialog = new TitleDialog(this, id);
			dialog.show();
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
				mAdapter = new SimpleCursorAdapter(this,
						R.layout.browser_list_item, mListCursor,
						new String[] { TitleProvider.Categories.NAME },
						new int[] { R.id.book_name });
				setListAdapter(mAdapter);
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
	
	private class TitleDialog extends Dialog {

		public TitleDialog(Context context, long titleId) {
			super(context);
			
			setContentView(R.layout.browser_title);
			
			//Get the title information
			Uri uri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "title/"
					+ titleId);
			String[] projection = new String[] { TitleProvider.Titles.BOOKNAME,
					TitleProvider.Titles.SIZE, TitleProvider.Titles.DESCRIPTION,
					TitleProvider.Titles.UPDATED, TitleProvider.Titles.URL};
			
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			
			if (cursor.moveToNext()) {
				setTitle(R.string.title_browser_name);
				
				TextView information = (TextView) findViewById(R.id.title_information);
				if(information != null) {
					String name = cursor.getString(0);
					String size = cursor.getString(1);
					String description = cursor.getString(2);
					String updated = cursor.getString(3);
					try {
						mDownloadUrl = new URL(cursor.getString(4));
					} catch (MalformedURLException e) {
						Log.w(this.getClass().getName(), e.getMessage());
					}
					
					if (name != null){
						information.append(name + "\n\n");
					}
					if (size != null){
						information.append("Size: " + size + " KB\n");
					}
					if (updated != null){
						information.append("Updated: " + updated + "\n");
					}
					if (description != null){
						information.append("Description: " + description + "\n");
					}
				}
			}
			
			cursor.close();
			
			Button cancel = (Button) findViewById(R.id.title_cancel_button);
			cancel.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					dismiss();
				}
			});
			
			Button download = (Button) findViewById(R.id.title_download_button);
			download.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					final byte[] buffer = new byte[255];
					
					if (mDownloadUrl != null && !mDownloadUrl.getFile().endsWith("exe")) {
						
						Thread t = new Thread() {
							public void run() {
								try {
									ZipInputStream zip = new ZipInputStream(
											mDownloadUrl.openStream());
									ZipEntry entry = zip.getNextEntry();

									String libDir = mSharedPref.getString(
											"default_ebook_dir",
											"/sdcard/reveal/ebooks/");
									FileOutputStream out = new FileOutputStream(
											libDir + entry.getName());

									int bytesRead = 0;
									while (-1 != (bytesRead = zip.read(buffer,
											0, 255))) {
										out.write(buffer, 0, bytesRead);
									}

									zip.close();
									out.flush();
									out.close();

									// add this book to the list
									Uri bookUri = Uri.withAppendedPath(
											YbkProvider.CONTENT_URI, "book");
									ContentValues values = new ContentValues();
									values.put(YbkProvider.FILE_NAME, libDir
											+ entry.getName());
									getContentResolver()
											.insert(bookUri, values);

								} catch (IOException e) {
									e.printStackTrace();
								}

								mHandler.post(mUpdateResults);
							}
						};
						t.start();

					} else {
						//handle I can't download this notification
					}
					
					dismiss();
				}
			});
		}
	}
	
	private void downloadComplete() {
		mDownloadUrl = null;
		
		Toast.makeText(this, R.string.ebook_download_complete, Toast.LENGTH_SHORT).show();
	}
}