package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;

public class Main extends ListActivity implements OnGestureListener {

	private static final int HISTORY_ID = Menu.FIRST;
	private static final int BOOKMARK_ID = Menu.FIRST + 1;
	private static final int SETTINGS_ID = Menu.FIRST + 2;
	private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
	private static final int BROWSER_ID = Menu.FIRST + 4;
	private static final int HELP_ID = Menu.FIRST + 5;
	private static final int ABOUT_ID = Menu.FIRST + 6;
	private static final int REVELUPDATE_ID = Menu.FIRST + 7;

	public static int mNotifId = 0;
	public static Main mApplication;

	private static final int ACTIVITY_SETTINGS = 0;
	private static final int LIBRARY_NOT_CREATED = 0;
	// private static final boolean DONT_ADD_BOOKS = false;
	private static final boolean ADD_BOOKS = true;

	// Gestures Stuff
	private NotificationManager mNotifMgr;

	@SuppressWarnings("unused")
	private GestureDetector gestureScanner;

	@SuppressWarnings("unused")
	private static final int INSERT_ID = Menu.FIRST + 8;

	private static final int DELETE_ID = Menu.FIRST + 9;

	private SharedPreferences mSharedPref;

	private boolean BOOLshowSplashScreen;

	private boolean BOOLshowFullScreen;

	private Uri mBookUri = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book");

	@SuppressWarnings("unused")
	private File mCurrentDirectory = new File("/sdcard/reveal/ebooks/");

	private final Handler mUpdateLibHandler = new Handler();

	private Cursor mListCursor;

	private ContentResolver mContRes;

	private static boolean mUpdating = false;

	private final Runnable mUpdateBookList = new Runnable() {
		public void run() {

			refreshBookList();

			mUpdating = false;
		}
	};

	/**
	 * Updating the book list can be very time-consuming. To preserve snappiness
	 * we're putting it in its own thread.
	 */
	protected void updateBookList() {
		// Fire off the thread to update the book database and populate the Book
		// Menu
		if (mUpdating) {
			Toast.makeText(this, R.string.update_in_progress, Toast.LENGTH_LONG).show();
		} else {
			mUpdating = true;
			Thread t = new Thread() {

				public void run() {
					// Try to tame this from stealing all the interface CPU
					Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
					Looper.prepare();
					String ebookDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
							"/sdcard/reveal/ebooks");
					refreshLibrary(ebookDir);
					mUpdateLibHandler.post(mUpdateBookList);

				}
			};

			t.start();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Debug.startMethodTracing("reveal");

		mApplication = this;

		// Change DEBUG to "0" in Global.java when building a RELEASE Version
		// for the GOOGLE APP MARKET
		// This allows for real usage stats and end user error reporting
		if (Global.DEBUG == 0) {
			// Release Key for use of the END USERS
			FlurryAgent.onStartSession(this, "BLRRZRSNYZ446QUWKSP4");
		} else {
			// Development key for use of the DEVELOPMENT TEAM
			FlurryAgent.onStartSession(this, "VYRRJFNLNSTCVKBF73UP");
		}
		FlurryAgent.onEvent("Main");

		mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);

		if (BOOLshowFullScreen) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		setContentView(R.layout.main);
		mContRes = getContentResolver();

		// To capture LONG_PRESS gestures
		gestureScanner = new GestureDetector(this);
		registerForContextMenu(getListView());

		boolean configChanged = (getLastNonConfigurationInstance() != null);

		if (!configChanged) {
			BOOLshowSplashScreen = mSharedPref.getBoolean("show_splash_screen", true);

			if (BOOLshowSplashScreen) {
				Util.showSplashScreen(this);
			}
		}

		// Is Network up or not?
		if (Util.isNetworkUp(this)) {
			// Actually go ONLINE and check... duhhhh
			UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
		}

		refreshBookList();

		if (!configChanged) {
			// Check for SDcard presence
			// if we have one create the dirs and look fer ebooks
			if (!android.os.Environment.getExternalStorageState().equals(
					android.os.Environment.MEDIA_MOUNTED)) {
				// Log.e(Global.TAG, "sdcard not installed");
				Toast.makeText(this, "You must have an SDCARD installed to use Reveal",
						Toast.LENGTH_LONG).show();
			} else {
				Util.createDefaultDirs(this);

				// Lets not do this on every onCreate. if users have a bad ebook
				// they wont be able to use reveal at all if we do.

				// updateBookList();
			}
		}
	}

	/** Called when the activity is going away. */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
		// Debug.stopMethodTracing();

	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();

			long bookId = menuInfo.id;

			ContentResolver res = getContentResolver();

			Uri thisBookUri = ContentUris.withAppendedId(mBookUri, bookId);

			Cursor bookCurs = managedQuery(thisBookUri, new String[] { YbkProvider.FILE_NAME },
					null, null, null);

			String fileName = bookCurs.moveToFirst() ? bookCurs.getString(0) : null;

			File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}

			res.delete(ContentUris.withAppendedId(mBookUri, bookId), null, null);

			refreshBookList();

			return true;
		default:
			return super.onContextItemSelected(item);
		}

	}

	/**
	 * Convenience method to make calling refreshLibrary() without any
	 * parameters retaining its original behavior.
	 */
	private void refreshLibrary(final String strLibDir) {
		refreshLibrary(strLibDir, ADD_BOOKS);
	}

	/**
	 * Refresh the eBook directory.
	 * 
	 * @param strLibDir
	 *            the path to the library directory.
	 * @param addNewBooks
	 *            If true, run the code that will add new books to the database
	 *            as well as the code that removes missing books from the
	 *            database (which runs regardless).
	 */
	private void refreshLibrary(final String strLibDir, final boolean addNewBooks) {
		boolean neededRefreshing = false;
		ContentResolver contRes = mContRes;
		Uri bookUri = mBookUri;
		Cursor fileCursor = null;

		// get a list of files from the database
		// Notify that we are getting current list of eBooks
		// Log.i(Global.TAG,"Getting the list of books in the database");

		fileCursor = managedQuery(bookUri, new String[] { YbkProvider.FILE_NAME, YbkProvider._ID },
				null, null, YbkProvider.FILE_NAME + " ASC");

		if (fileCursor.getCount() == 0) {
			Log.w(Global.TAG, "eBook database has no valid YBK files");
		}

		// get a list of files from the library directory
		File libraryDir = new File(strLibDir);
		if (!libraryDir.exists()) {
			if (!libraryDir.mkdirs()) {

				// Send a notice that the ebook library folder couldn't be
				// created
				Util.sendNotification(this, (String) getResources().getText(
						R.string.library_not_created), android.R.drawable.stat_sys_warning,
						"Couldn't make eBook Library", mNotifMgr, mNotifId++, Main.class);

			}
		}

		File[] ybkFiles = libraryDir.listFiles(new YbkFilter());

		if (ybkFiles != null && addNewBooks) {
			// add books that are not in the database
			// Notify that we are getting NEW list of eBooks
			// Log.i(Global.TAG, "Updating eBook List from " + libraryDir);

			for (int i = 0, dirListLen = ybkFiles.length; i < dirListLen; i++) {
				String dirFilename = ybkFiles[i].getAbsolutePath();
				// Log.d(Global.TAG, "dirFilename: " + dirFilename);

				boolean fileFoundInDb = false;

				fileCursor.moveToFirst();
				while (!fileCursor.isAfterLast()) {

					String dbFilename = fileCursor.getString(fileCursor
							.getColumnIndexOrThrow(YbkProvider.FILE_NAME));
					if (dirFilename.equalsIgnoreCase(dbFilename)) {
						fileFoundInDb = true;
						break;
					}

					fileCursor.moveToNext();
				}

				if (!fileFoundInDb) {
					if (!neededRefreshing) {
						// if the neededRefreshing flag is not set yet
						Util.sendNotification(this, "Refreshing the library",
								R.drawable.ebooksmall, "Reveal Library Refresh", mNotifMgr,
								mNotifId++, Main.class);
					}

					int lastSlashPos = dirFilename.lastIndexOf('/');
					int lastDotPos = dirFilename.lastIndexOf('.');
					String bookName = dirFilename;
					if (lastSlashPos != -1 && lastDotPos != -1) {
						bookName = dirFilename.substring(lastSlashPos + 1, lastDotPos);
					}

					neededRefreshing = true;
					ContentValues values = new ContentValues();
					values.put(YbkProvider.FILE_NAME, dirFilename);
					Uri uri = contRes.insert(bookUri, values);

					String bookId = uri.getLastPathSegment();

					if (Integer.parseInt(bookId) > 0) {
						mListCursor = managedQuery(mBookUri, new String[] {
								YbkProvider.FORMATTED_TITLE, YbkProvider._ID },
								YbkProvider.BINDING_TEXT + " is not null", null, " LOWER("
										+ YbkProvider.FORMATTED_TITLE + ") ASC");

						Util.sendNotification(this, "Added '" + bookName + "' to the library",
								R.drawable.ebooksmall, "Reveal Library Refresh", mNotifMgr,
								mNotifId++, Main.class);
					} else {
						Util.sendNotification(this, "Could not add '" + bookName + "'. Bad file?",
								android.R.drawable.stat_sys_warning, "Reveal Library Refresh",
								mNotifMgr, mNotifId++, Main.class);

						// Tell the user that we had a eBook Error and we want
						// to notify the devs'

						// Builder builder = new AlertDialog.Builder(this);
						// builder.setTitle("eBook Error");
						// builder.setIcon(R.drawable.reveal);
						// builder.setMessage("Send Report??");
						// builder.setPositiveButton("YES", null);
						// builder.setNegativeButton("NO", null);
						// builder.show();

						ReportError.reportError("BAD_EBOOK_FILE_" + bookName);
					}
				}
			}
		}

		// Log.i(Global.TAG,
		// "Removing Books from the database which are not in directory");

		// remove the books from the database if they are not in the directory
		int fileIndex = 0;
		fileCursor.moveToFirst();
		while (!fileCursor.isAfterLast()) {
			fileIndex++;
			String dbFilename = fileCursor.getString(fileCursor
					.getColumnIndexOrThrow(YbkProvider.FILE_NAME));

			boolean fileFoundInDir = false;
			for (int i = 0, dirListLen = ybkFiles.length; i < dirListLen; i++) {
				String dirFilename = ybkFiles[i].getAbsolutePath();
				if (dirFilename.equalsIgnoreCase(dbFilename)) {
					fileFoundInDir = true;
					break;
				}
			}
			// remove files that are 0 byte size
			// File f = new File (dbFilename);
			// if (f.length() == 0) {
			// f.delete();
			// Util.sendNotification(this, "Removed Corrupt eBook",
			// R.drawable.ebooksmall, "Removed Corrupt eBook",
			// mNotifMgr, mNotifId++, Main.class);
			// }

			if (!fileFoundInDir) {
				neededRefreshing = true;
				String bookId = fileCursor.getString(fileCursor
						.getColumnIndexOrThrow(YbkProvider._ID));
				Uri deleteUri = ContentUris.withAppendedId(bookUri, Long.parseLong(bookId));
				contRes.delete(deleteUri, null, null);

			}

			fileCursor.moveToNext();
		}

		// no longer need the fileCursor
		// stopManagingCursor(fileCursor);
		// fileCursor.close();

		if (neededRefreshing) {

			Util.sendNotification(this, "Refreshing of library complete.", R.drawable.ebooksmall,
					"Reveal Library Refresh", mNotifMgr, mNotifId++, Main.class);

		}

	}

	/**
	 * Refresh the list of books in the main list.
	 */
	private void refreshBookList() {
		mListCursor = managedQuery(mBookUri, new String[] { YbkProvider.FORMATTED_TITLE,
				YbkProvider._ID }, YbkProvider.BINDING_TEXT + " is not null", null, " LOWER("
				+ YbkProvider.FORMATTED_TITLE + ") ASC");

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { YbkProvider.FORMATTED_TITLE };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.bookText };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter bookAdapter = new SimpleCursorAdapter(this, R.layout.book_list_row,
				mListCursor, from, to);

		setListAdapter(bookAdapter);

	}

	/**
	 * Class for filtering non-YBK files out of a list of files
	 */
	private class YbkFilter implements FileFilter {
		public boolean accept(File file) {
			return file.getName().endsWith(".ybk");
		}
	}

	// on main menu long press we go here to do stuff like delete
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// AdapterContextMenuInfo info = (AdapterContextMenuInfo)
		// item.getMenuInfo();
		// Write Delete from DB Helper
		// DeleteFileHere(info.id);
		menu.add(0, DELETE_ID, 0, R.string.really_delete);
	}

	@Override
	public void onResume() {
		super.onResume();

		setProgressBarIndeterminateVisibility(false);

		// Set preferences from Setting screen
		SharedPreferences sharedPref = mSharedPref;

		String libDir = sharedPref
				.getString(Settings.EBOOK_DIRECTORY_KEY, "/sdcard/reveal/ebooks/");
		if (!libDir.endsWith("/")) {
			libDir = libDir + "/";
		}

		mCurrentDirectory = new File(libDir);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history).setIcon(
				android.R.drawable.ic_menu_recent_history);
		menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE, R.string.menu_bookmark).setIcon(
				android.R.drawable.ic_menu_compass);
		menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE, R.string.menu_refresh_library).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(Menu.NONE, BROWSER_ID, Menu.NONE, R.string.menu_browser).setIcon(
				android.R.drawable.ic_menu_set_as);
		menu.add(Menu.NONE, HELP_ID, Menu.NONE, R.string.menu_help).setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(
				android.R.drawable.ic_menu_share);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH_LIB_ID:
			RefreshDialog.create(this);
			updateBookList();
			return true;

		case SETTINGS_ID:
			Intent intent = new Intent(this, Settings.class);
			startActivityForResult(intent, ACTIVITY_SETTINGS);
			return true;

		case BROWSER_ID:
			Intent browserIntent = new Intent(this, TitleBrowser.class);
			startActivity(browserIntent);
			return true;

		case REVELUPDATE_ID:
			Toast.makeText(this, R.string.checking_for_new_version_online, Toast.LENGTH_SHORT)
					.show();
			UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
			return true;

		case ABOUT_ID:
			AboutDialog.create(this);
			return true;

		case HELP_ID:
			HelpDialog.create(this);
			return true;

		case HISTORY_ID:
			startActivityForResult(new Intent(this, HistoryDialog.class),
					YbkViewActivity.CALL_HISTORY);
			return true;
		case DELETE_ID:
			AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();

			long bookId = menuInfo.id;

			ContentResolver res = getContentResolver();

			Uri thisBookUri = ContentUris.withAppendedId(mBookUri, bookId);

			Cursor bookCurs = managedQuery(thisBookUri, new String[] { YbkProvider.FILE_NAME },
					null, null, null);

			String fileName = bookCurs.moveToFirst() ? bookCurs.getString(0) : null;

			if (fileName != null) {
				File file = new File(fileName);
				if (file.exists()) {
					file.delete();
				}

				res.delete(thisBookUri, null, null);

				refreshBookList();

			}

			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onListItemClick(final ListView listView, final View view,
			final int selectionRowId, final long id) {

		setProgressBarIndeterminateVisibility(true);

		// Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);

		Intent intent = new Intent(this, YbkViewActivity.class);
		intent.putExtra(YbkProvider._ID, id);
		startActivity(intent);

	}

	/**
	 * Used to configure any dialog boxes created by this Activity
	 */
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case LIBRARY_NOT_CREATED:
			return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.library_not_created).setPositiveButton(
							R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									/* User clicked OK so do some stuff */
								}
							}).create();
		}
		return null;
	}

	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub

	}

	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onLongPress(MotionEvent arg0) {
		Log.e(Global.TAG, "ONLONGPRESS");

	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Bundle extras;
		long histId;

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case YbkViewActivity.CALL_HISTORY:
				setProgressBarIndeterminateVisibility(true);

				extras = data.getExtras();
				histId = extras.getLong(YbkProvider._ID);

				Cursor histCurs = managedQuery(ContentUris.withAppendedId(Uri.withAppendedPath(
						YbkProvider.CONTENT_URI, "history"), histId), null, null, null, null);

				if (histCurs.moveToFirst()) {
					long bookId = histCurs.getLong(histCurs.getColumnIndex(YbkProvider.BOOK_ID));
					Intent intent = new Intent(this, YbkViewActivity.class);
					intent.putExtra(YbkProvider._ID, bookId);
					intent.putExtra(YbkProvider.FROM_HISTORY, true);
					startActivity(intent);
				} else {
					Log.e(Global.TAG, "Couldn't load chapter from history");
				}

				break;
			case YbkViewActivity.CALL_BOOKMARK:
				setProgressBarIndeterminateVisibility(true);

				extras = data.getExtras();
				long bmId = extras.getLong(YbkProvider.BOOKMARK_NUMBER);

				Cursor bmCurs = managedQuery(ContentUris.withAppendedId(Uri.withAppendedPath(
						YbkProvider.CONTENT_URI, "bookmark"), bmId), null, null, null, null);

				if (bmCurs.moveToFirst()) {
					histId = bmCurs.getLong(bmCurs.getColumnIndex(YbkProvider._ID));
					Intent intent = new Intent(this, YbkViewActivity.class);
					intent.putExtra(YbkProvider._ID, histId);
					intent.putExtra(YbkProvider.FROM_HISTORY, true);
					startActivity(intent);
				} else {
					Log.e(Global.TAG, "Couldn't load chapter from bookmarks");
				}
				break;

			case ACTIVITY_SETTINGS:
				extras = data.getExtras();
				boolean libDirChanged = extras.getBoolean(Settings.EBOOK_DIR_CHANGED);

				if (libDirChanged) {
					updateBookList();
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public static Main getMainApplication() {
		return mApplication;
	}

}
