package com.jackcholt.reveal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
	private static final int UPDATE_TOKEN = 12; // random number
	private static String mDownloadServer = "http://www.thecoffeys.net/ebooks/default.asp?action=download&ID=";
	private NotificationManager mNotifMgr;
	private int mNotifId = 0;
	private static final String TAG = "Reveal TitleBrowser";
	private SimpleCursorAdapter mAdapter;
	private Stack<Uri> mBreadCrumb;
	private Cursor mListCursor;
	private QueryHandler mQueryHandler;
	private boolean mDownloadSuccess;
	private URL mDownloadUrl = null;
	private URL mFileLocation = null;
	private SharedPreferences mSharedPref;
	private boolean mBusy = false;
	private boolean BOOLshowFullScreen;

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

		// Change DEBUG to "0" in Global.java when building a RELEASE Version
		// for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(Main.getMainApplication(), "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(Main.getMainApplication(), "C9D5YMTMI5SPPTE8S4S4");
		}

		FlurryAgent.onEvent("TitleBrowser");

		Map<String, String> flurryMap = new HashMap<String, String>();
		flurryMap.put("eBook Downloaded", "eBookname");

		mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		mBreadCrumb = new Stack<Uri>();

		mDownloadSuccess = false;

		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);

		if (BOOLshowFullScreen) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		setContentView(R.layout.browser_main);

		mQueryHandler = new QueryHandler(this);

		// If this database doesn't have anything to display, let's load it from
		// the embedded file

		mListCursor = managedQuery(Uri.withAppendedPath(TitleProvider.CONTENT_URI, "category"),
				new String[] { TitleProvider.Categories._ID }, null, null, null);

		if (mListCursor.getCount() == 0) {
			Toast.makeText(this, R.string.checking_ebooks, Toast.LENGTH_SHORT).show();

			setProgressBarIndeterminateVisibility(true);
			mBusy = true;
			mQueryHandler.startUpdate(UPDATE_TOKEN, null, Uri.withAppendedPath(
					TitleProvider.CONTENT_URI, "updatefile"), null, null, null);
		}

		// setup current location in stack
		Object lastStack = getLastNonConfigurationInstance();

		if (lastStack != null) {
			for (Object uri : (Stack<?>) lastStack) {
				mBreadCrumb.push((Uri) uri);
			}
		} else {
			Uri categoryUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "categoryparent");
			Uri rootCategories = ContentUris.withAppendedId(categoryUri, 0);
			mBreadCrumb.push(rootCategories);
		}

		updateScreen();

	}

	/** Called when the activity is going away. */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mBreadCrumb;
	}

	protected void updateScreen() {
		Uri currentUri = mBreadCrumb.peek();

		String[] projection;
		String[] from;
		int[] to = new int[] { R.id.book_name };

		if (currentUri.toString().contains("/category")) {
			projection = new String[] { TitleProvider.Categories.NAME, TitleProvider.Categories._ID };
			from = new String[] { TitleProvider.Categories.NAME };
		} else {
			projection = new String[] { TitleProvider.Titles.BOOKNAME, TitleProvider.Titles._ID };
			from = new String[] { TitleProvider.Titles.BOOKNAME };
		}

		mListCursor = managedQuery(currentUri, projection, null, null, null);

		Log.d(TAG, "currentUri/mListCursor.getCount(): " + currentUri + " / "
				+ mListCursor.getCount());

		mAdapter = new SimpleCursorAdapter(this, R.layout.browser_list_item, mListCursor, from, to);
		setListAdapter(mAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean success = super.onCreateOptionsMenu(menu);

		menu.add(Menu.NONE, UPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(
				android.R.drawable.ic_menu_share);

		return success;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case UPDATE_ID:
			Toast.makeText(this, R.string.checking_ebooks, Toast.LENGTH_SHORT).show();

			setProgressBarIndeterminateVisibility(true);
			mBusy = true;
			mQueryHandler.startUpdate(UPDATE_TOKEN, null, Uri.withAppendedPath(
					TitleProvider.CONTENT_URI, "update"), null, null, null);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (mBreadCrumb.size() < 3) {
			Uri baseUri;

			if (mBreadCrumb.size() == 2) {
				baseUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "titlecategory");
			} else {
				baseUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "categoryparent");
			}

			Uri lookupUri = ContentUris.withAppendedId(baseUri, id);
			mBreadCrumb.push(lookupUri);

			updateScreen();
		} else {
			if (mBusy) {
				Toast.makeText(this, R.string.ebook_download_busy, Toast.LENGTH_LONG).show();
				FlurryAgent.onError("TitleBrowser", "Download Busy", "WARNING");
			} else {
				TitleDialog dialog = new TitleDialog(this, id);
				dialog.show();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mBreadCrumb.size() > 1) {
				mBreadCrumb.pop();

				updateScreen();
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
			FlurryAgent.onError("TitleBrowser", "Download New Catalog", "INFO");
			// establish data connection
			Uri categoryUri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "categoryparent");
			Uri rootCategories = ContentUris.withAppendedId(categoryUri, 0);

			// create adapters for view
			mBreadCrumb.clear();
			mBreadCrumb.push(rootCategories);

			updateScreen();

			setProgressBarIndeterminateVisibility(false);
			mBusy = false;

			Toast.makeText(mContext, R.string.ebook_update_complete, Toast.LENGTH_SHORT).show();
		}
	}

	private class TitleDialog extends Dialog {

		public TitleDialog(final Context context, long titleId) {
			super(context);

			setContentView(R.layout.browser_title);

			// Get the title information
			Uri uri = Uri.withAppendedPath(TitleProvider.CONTENT_URI, "title/" + titleId);
			String[] projection = new String[] { TitleProvider.Titles.BOOKNAME,
					TitleProvider.Titles.SIZE, TitleProvider.Titles.DESCRIPTION,
					TitleProvider.Titles.UPDATED, TitleProvider.Titles.URL,
					TitleProvider.Titles.SOURCE_ID };

			Cursor cursor = managedQuery(uri, projection, null, null, null);

			if (cursor.moveToNext()) {
				setTitle(R.string.title_browser_name);

				TextView information = (TextView) findViewById(R.id.title_information);
				if (information != null) {
					String name = cursor.getString(0);
					String size = cursor.getString(1);
					String description = cursor.getString(2);
					String updated = cursor.getString(3);

					try {
						mFileLocation = new URL(cursor.getString(4));
						mDownloadUrl = new URL(mDownloadServer + cursor.getString(5));
					} catch (MalformedURLException e) {
						Toast.makeText(context, R.string.ebook_download_failed_url,
								Toast.LENGTH_SHORT).show();

						dismiss();
					}

					if (name != null) {
						information.append(name + "\n\n");
					}
					if (size != null) {
						information.append("Size: " + size + " KB\n");
					}
					if (updated != null) {
						information.append("Updated: " + updated + "\n");
					}
					if (description != null) {
						information.append("Description: " + description + "\n");
					}
					// Create a map and add the name of the downloaded eBook to
					// it
					Map<String, String> flurryMap = new HashMap<String, String>();
					flurryMap.put("eBook Downloaded", name);
					FlurryAgent.onEvent("TitleBrowser", flurryMap);
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
					if (mFileLocation != null) {
						Util.sendNotification(context, (String) getResources().getText(
								R.string.ebook_download_started), R.drawable.ebooksmall,
								"Reveal Online eBook Download", mNotifMgr, mNotifId++, Main.class);
						Toast
								.makeText(context, R.string.ebook_download_started,
										Toast.LENGTH_SHORT).show();

						Thread t = new Thread() {
							public void run() {
								Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
								String mLibraryDir = mSharedPref.getString("default_ebook_dir",
										"/sdcard/reveal/ebooks/");
								mDownloadSuccess = Util.fetchAndLoadTitle(mFileLocation,
										mDownloadUrl, mLibraryDir, context);
								mHandler.post(mUpdateResults);
							}
						};
						t.start();

					} else {
						Toast.makeText(context, R.string.ebook_download_failed_type,
								Toast.LENGTH_SHORT).show();
					}

					dismiss();
				}
			});
		}
	}

	private void downloadComplete() {
		mDownloadUrl = null;

		if (mDownloadSuccess) {
			Util.sendNotification(this, (String) getResources().getText(
					R.string.ebook_download_complete), R.drawable.ebooksmall,
					"Reveal Online eBook Download", mNotifMgr, mNotifId++, Main.class);
		} else {
			Util.sendNotification(this, (String) getResources().getText(
					R.string.ebook_download_failed), R.drawable.ebooksmall,
					"Reveal Online eBook Download", mNotifMgr, mNotifId++, TitleBrowser.class);
		}
		setProgressBarIndeterminateVisibility(false);
		mBusy = false;
	}
}