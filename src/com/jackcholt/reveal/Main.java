package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;
import com.nullwire.trace.ExceptionHandler;

public class Main extends ListActivity implements OnGestureListener {

    private static final int HISTORY_ID = Menu.FIRST;

    private static final int BOOKMARK_ID = Menu.FIRST + 1;

    private static final int SETTINGS_ID = Menu.FIRST + 2;

    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;

    private static final int BROWSER_ID = Menu.FIRST + 4;

    private static final int HELP_ID = Menu.FIRST + 5;

    private static final int ABOUT_ID = Menu.FIRST + 6;

    private static final int REVELUPDATE_ID = Menu.FIRST + 7;

    @SuppressWarnings("unused")
    private static final int INSERT_ID = Menu.FIRST + 8;

    private static final int DELETE_ID = Menu.FIRST + 9;

    private static int mRefreshNotifId = 0;

    public static int mNotifId = 1;

    public static Main mApplication;

    private static final int ACTIVITY_SETTINGS = 0;

    private static final int LIBRARY_NOT_CREATED = 0;

    // private static final boolean DONT_ADD_BOOKS = false;
    private static final boolean ADD_BOOKS = true;

    // Gestures Stuff
    private NotificationManager mNotifMgr;

    @SuppressWarnings("unused")
    private GestureDetector gestureScanner;

    private SharedPreferences mSharedPref;

    private boolean BOOLshowSplashScreen;

    private boolean BOOLshowFullScreen;

    private final Handler mUpdateLibHandler = new Handler();

    private static boolean mUpdating = false;

    private List<Book> mBookTitleList;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Debug.startMethodTracing("reveal");

		// send an exception email via this URL
		//ExceptionHandler.register(this, "http://revealreader.thepackhams.com/exception.php");

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

		SharedPreferences sharedPref = mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		// Get the YanceyBook DAO
		// mYbkDao = YbkDAO.getInstance(this);

		BOOLshowFullScreen = sharedPref.getBoolean("show_fullscreen", false);

		if (BOOLshowFullScreen) {
			getWindow()
					.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		setContentView(R.layout.main);

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

		if (!configChanged) {
			// Check for SDcard presence
			// if we have one create the dirs and look fer ebooks
			if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				// Log.e(Global.TAG, "sdcard not installed");
				Toast.makeText(this, "You must have an SDCARD installed to use Reveal", Toast.LENGTH_LONG).show();
				return;
			} else {
				Util.createDefaultDirs(this);
				// updateBookList();

			}
		}

		refreshBookList();
	}

	/** Called when the activity is going away. */
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
		// Debug.stopMethodTracing();

		// Remove any references so it can be garbage collected.
		// mYbkDao = null;
	}

	private final Runnable mUpdateBookList = new Runnable() {
		public void run() {
            /*
             * mBookTitleList = YbkDAO.getInstance(getBaseContext())
             * .getBookTitles().getList(null, null);
             */
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
					refreshLibrary(mSharedPref
							.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY));
					// refreshBookList();

					mUpdateLibHandler.post(mUpdateBookList);
				}
			};

			t.start();
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

		// get a list of files from the database
		// Notify that we are getting current list of eBooks
		// Log.i(Global.TAG,"Getting the list of books in the database");

		YbkDAO ybkDao = YbkDAO.getInstance(this);
		Iterator<Book> fileIterator = ybkDao.getBookTitles().iterator();

		if (!fileIterator.hasNext()) {
			Log.w(Global.TAG, "eBook database has no valid YBK files");
		}

		// get a list of files from the library directory
		File libraryDir = new File(strLibDir);
		if (!libraryDir.exists()) {
			if (!libraryDir.mkdirs()) {

				// Send a notice that the ebook library folder couldn't be
				// created
				Util.sendNotification(this, (String) getResources().getText(R.string.library_not_created),
						android.R.drawable.stat_sys_warning, "Couldn't make eBook Library", mNotifMgr, mNotifId++,
						Main.class);

			}
		}

		File[] ybkFiles = libraryDir.listFiles(new YbkFilter());

		if (ybkFiles != null && addNewBooks) {
			// add books that are not in the database
			// Notify that we are getting NEW list of eBooks
			Log.i(Global.TAG, "Updating eBook List from " + libraryDir);

			for (int i = 0, dirListLen = ybkFiles.length; i < dirListLen; i++) {
				String dirFilename = ybkFiles[i].getAbsolutePath();
				Log.d(Global.TAG, "dirFilename: " + dirFilename);

				boolean fileFoundInDb = false;

				fileIterator = ybkDao.getBooks().iterator();
				while (fileIterator.hasNext()) {
					Book book = fileIterator.next();

					if (dirFilename.equalsIgnoreCase(book.fileName)) {
						fileFoundInDb = true;
						break;
					}

				}

				if (!fileFoundInDb) {
					if (!neededRefreshing) {
						// if the neededRefreshing flag is not set yet
						Util.sendNotification(this, "Refreshing the library", R.drawable.ebooksmall,
								"Reveal Library Refresh", mNotifMgr, mRefreshNotifId, Main.class);
					}

					int lastSlashPos = dirFilename.lastIndexOf('/');
					int lastDotPos = dirFilename.lastIndexOf('.');
					String bookName = dirFilename;
					if (lastSlashPos != -1 && lastDotPos != -1) {
						bookName = dirFilename.substring(lastSlashPos + 1, lastDotPos);
					}

					neededRefreshing = true;

					try {
						// Create an object for reading a ybk file;
						YbkFileReader ybkRdr = new YbkFileReader(this, dirFilename);
						// Tell the YbkFileReader to populate the book info into
						// the database;
						ybkRdr.populateBook();

						// refreshBookList();
						mBookTitleList = ybkDao.getBookTitles().getList(null, null);

						/*
						 * List<Book> data =
						 * ybkDao.getBookTitles().getList(null, null);
						 * 
						 * // Now create a simple adapter and set it to display
						 * mBookAdapter = new ArrayAdapter<Book>(this,
						 * R.layout.book_list_row, data);
						 */

						Util.sendNotification(this, "Added '" + bookName + "' to the library", R.drawable.ebooksmall,
								"Reveal Library Refresh", mNotifMgr, mRefreshNotifId, Main.class);
					} catch (IOException ioe) {
						Util.sendNotification(this, "Could not add '" + bookName + "'. Bad file?",
								android.R.drawable.stat_sys_warning, "Reveal Library Refresh", mNotifMgr, mNotifId++,
								Main.class);
						ReportError.reportError("BAD_EBOOK_FILE_" + bookName);
					}

				}
			}

		}

		// Log.i(Global.TAG,
		// "Removing Books from the database which are not in directory");

		// remove the books from the database if they are not in the directory
		// int fileIndex = 0;
		// restart the file Iterator
		fileIterator = ybkDao.getBookTitles().iterator();
		while (fileIterator.hasNext()) {
			// fileIndex++;
			Book book = fileIterator.next();
			String dbFilename = book.fileName;

			boolean fileFoundInDir = false;
			for (int i = 0, dirListLen = ybkFiles.length; i < dirListLen; i++) {
				String dirFilename = ybkFiles[i].getAbsolutePath();
				if (dirFilename.equalsIgnoreCase(dbFilename)) {
					fileFoundInDir = true;
					break;
				}
			}

			if (!fileFoundInDir) {
				neededRefreshing = true;
				ybkDao.deleteBook(book);

			}

		}

		if (neededRefreshing) {

			Util.sendNotification(this, "Refreshing of library complete.", R.drawable.ebooksmall,
					"Reveal Library Refresh", mNotifMgr, mRefreshNotifId, Main.class);

		}

	}

	/**
	 * Refresh the list of books in the main list.
	 */
	private void refreshBookList() {

		YbkDAO ybkDao = YbkDAO.getInstance(this);

		mBookTitleList = ybkDao.getBookTitles().getList(null, null);

		// Now create a simple adapter and set it to display
		ArrayAdapter<Book> bookAdapter = new ArrayAdapter<Book>(this, R.layout.book_list_row, mBookTitleList);

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

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
			long bookId = menuInfo.id;
			YbkDAO ybkDAO = YbkDAO.getInstance(this);
			Book book = ybkDAO.getBook(bookId);

			String fileName = book.fileName;
			File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}
			ybkDAO.deleteBook(book);
			refreshBookList();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		DeleteEbookDialog.create(this, DELETE_ID);
	}

	@Override
	public void onResume() {
		super.onResume();

		setProgressBarIndeterminateVisibility(false);

		// Set preferences from Setting screen
		SharedPreferences sharedPref = mSharedPref;

		String libDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

		if (!libDir.endsWith("/")) {
			libDir = libDir + "/";
		}

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history).setIcon(
				android.R.drawable.ic_menu_recent_history);
		menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE, R.string.menu_bookmark).setIcon(android.R.drawable.ic_input_get);
		menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE, R.string.menu_refresh_library).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(Menu.NONE, BROWSER_ID, Menu.NONE, R.string.menu_browser).setIcon(android.R.drawable.ic_menu_set_as);
		menu.add(Menu.NONE, HELP_ID, Menu.NONE, R.string.menu_help).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE, R.string.menu_settings).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE, R.string.menu_update).setIcon(android.R.drawable.ic_menu_share);

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
			Toast.makeText(this, R.string.checking_for_new_version_online, Toast.LENGTH_SHORT).show();
			UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
			return true;

		case ABOUT_ID:
			AboutDialog.create(this);
			return true;

		case HELP_ID:
			HelpDialog.create(this);
			return true;

		case HISTORY_ID:
			startActivityForResult(new Intent(this, HistoryDialog.class), YbkViewActivity.CALL_HISTORY);
			return true;

		case BOOKMARK_ID:
			Intent bmIntent = new Intent(this, BookmarkDialog.class);
			bmIntent.putExtra("fromMain", true);
			startActivityForResult(bmIntent, YbkViewActivity.CALL_BOOKMARK);
			return true;

		case DELETE_ID:
			AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
			long bookId = menuInfo.id;
			YbkDAO ybkDAO = YbkDAO.getInstance(this);
			Book book = ybkDAO.getBook(bookId);
			String fileName = book.fileName;

			if (fileName != null) {
				File file = new File(fileName);
				if (file.exists()) {
					file.delete();
				}
				refreshBookList();
			}
			ybkDAO.deleteBook(book);
			return true;

		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onListItemClick(final ListView listView, final View view, final int selectionRowId, final long id) {

		setProgressBarIndeterminateVisibility(true);

		// Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
		Book book = (Book) listView.getItemAtPosition(selectionRowId);
		Intent intent = new Intent(this, YbkViewActivity.class);
		intent.putExtra(YbkDAO.ID, book.id);
		startActivity(intent);

	}

	/**
	 * Used to configure any dialog boxes created by this Activity
	 */
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case LIBRARY_NOT_CREATED:
			return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(
					R.string.library_not_created).setPositiveButton(R.string.alert_dialog_ok,
					new DialogInterface.OnClickListener() {
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
		Intent intent;

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case YbkViewActivity.CALL_HISTORY:
			case YbkViewActivity.CALL_BOOKMARK:
				setProgressBarIndeterminateVisibility(true);

				extras = data.getExtras();
				histId = extras.getLong(YbkDAO.ID);

				intent = new Intent(this, YbkViewActivity.class);
				intent.putExtra(YbkDAO.ID, histId);
				intent.putExtra(YbkDAO.FROM_HISTORY, true);
				startActivity(intent);
				break;

			case ACTIVITY_SETTINGS:
				extras = data.getExtras();
				boolean libDirChanged = extras.getBoolean(Settings.EBOOK_DIR_CHANGED);

				if (libDirChanged) {

					String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
							Settings.DEFAULT_EBOOK_DIRECTORY);

					refreshLibrary(libDir, ADD_BOOKS);
					refreshBookList();
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	// used to give access to "this" in threads and other places
	// DKP
	public static Main getMainApplication() {
		return mApplication;
	}

}
