package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

public class Main extends ListActivity implements OnGestureListener {
	
    //public final static int DISPLAYMODE_ABSOLUTE = 0;
    //public final static int DISPLAYMODE_RELATIVE = 1;
    
    private static final int HISTORY_ID = Menu.FIRST;
    //private static final int BOOKMARK_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
    private static final int BROWSER_ID = Menu.FIRST + 4;
    private static final int REVELUPDATE_ID = Menu.FIRST + 5;
    private static final int ABOUT_ID = Menu.FIRST + 6;
    private int mNotifId = 0;

    private static final int ACTIVITY_SETTINGS = 0;
    private static final int ACTIVITY_HISTORY = 1;
    private static final int LIBRARY_NOT_CREATED = 0;
    //private static final boolean DONT_ADD_BOOKS = false;
    private static final boolean ADD_BOOKS = true;
    
    //Gestures Stuff
    private NotificationManager mNotifMgr;
    private GestureDetector gestureScanner; 
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private SharedPreferences mSharedPref;
    private boolean BOOLshowSplashScreen;
    private boolean BOOLshowFullScreen;
    private String mLibraryDir;
    private File mImagesDir;
    private Uri mBookUri= Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book");
    private File mCurrentDirectory = new File("/sdcard/reveal/ebooks/"); 
    private final Handler mUpdateLibHandler = new Handler();
    private Cursor mListCursor; 
    private ContentResolver mContRes; 
    public int i = -1;
    private boolean mUpdating = false;
    
    private final Runnable mUpdateBookList = new Runnable() {
        public void run() {
            refreshBookList();
            
            mUpdating = false;
        }
    };
    
    
    /**
     * Updating the book list can be very time-consuming.  To preserve snappiness
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
	                refreshLibrary();
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

        mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);       
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", true);
    	
        if (BOOLshowFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE); 
        }
        
        setContentView(R.layout.main);
        mContRes = getContentResolver(); 
       
        //mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        if(!mLibraryDir.endsWith("/")) {
        	mLibraryDir = mLibraryDir + "/";
        }
        //To capture LONG_PRESS gestures
        gestureScanner = new GestureDetector(this); 
        registerForContextMenu(getListView());
                
        boolean configChanged = (getLastNonConfigurationInstance() != null);
        
        if (!configChanged) {
        	//mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        	BOOLshowSplashScreen = mSharedPref.getBoolean("show_splash_screen", true);
            
          	if (BOOLshowSplashScreen) { 
                Util.showSplashScreen(this);
            }
            //Actually go ONLINE and check...  duhhhh
            UpdateChecker.checkForNewerVersion(Global.SVN_VERSION, this);
        }
        
        refreshBookList();

        if (!configChanged) {
            //Check for SDcard presence
            //if we have one create the dirs and look fer ebooks
        	if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            	Log.e(Global.TAG, "sdcard not installed");
            	Toast.makeText(this, "You must have an SDCARD installed to use Reveal", Toast.LENGTH_LONG).show();
            } else {
            	createDefaultDirs();
            	updateBookList();
             }
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        return "configuration changed";
    }
    
    private void createDefaultDirs() {
        // Create the /sdcard/reveal and eBooks dir if they don't exist
        // Notify of creation and maybe put people directly into the TitleBrowser if they don't have ANY ybk's
    	//Must end in a SLASH
        File revealdir = new File("/sdcard/reveal/");
        if (!revealdir.exists()) {
        	 revealdir.mkdirs();
             Log.i(Global.TAG, "Create reveal dir on sdcard ok");
        }
        File ebooksdir = new File(mLibraryDir);
        if (!ebooksdir.exists()) {
        	 ebooksdir.mkdirs();
             Log.i(Global.TAG, "Create ebooks dir on sdcard ok");
        }

        mImagesDir = new File(mLibraryDir + "images/");
        if (!mImagesDir.exists()) {
             mImagesDir.mkdirs();
             Log.i(Global.TAG, "Create images dir on sdcard ok");
        }
    }
    
    /**
     * Convenience method to make calling refreshLibrary() without any 
     * parameters retaining its original behavior. 
     */
    private void refreshLibrary() {
        refreshLibrary(ADD_BOOKS);
    }
    
    /**
     * Refresh the eBook directory.
     * 
     * @param addNewBooks If true, run the code that will add new books to the
     * database as well as the code that removes missing books from the database 
     * (which runs regardless).
     */
    private void refreshLibrary(final boolean addNewBooks) {
        boolean neededRefreshing = false;
        ContentResolver contRes = mContRes;
        Uri bookUri = mBookUri;
        String strLibDir = mLibraryDir;
        Cursor fileCursor = null;
        
        // get a list of files from the database
        // Notify that we are getting current list of eBooks
        Log.i(Global.TAG,"Getting the list of books in the database");
   
        fileCursor = contRes.query(bookUri, 
                new String[] {YbkProvider.FILE_NAME,YbkProvider._ID}, null, null,
                YbkProvider.FILE_NAME + " ASC");
        
        startManagingCursor(fileCursor);
        
        if (fileCursor.getCount() == 0) {
            Log.w(Global.TAG, "eBook database has no valid YBK files");
        }
        
        // get a list of files from the library directory
        File libraryDir = new File(strLibDir);
        if (!libraryDir.exists()) {
            if (!libraryDir.mkdirs()) {
                
                // Send a notice that the ebook library folder couldn't be created
                Util.sendNotification(this, (String) getResources().getText(LIBRARY_NOT_CREATED), 
                        android.R.drawable.stat_sys_warning, "Couldn't make eBook Library", 
                        mNotifMgr, mNotifId++, Main.class);
                
            }
        }
        
        File[] ybkFiles = libraryDir.listFiles(new YbkFilter());

        if (ybkFiles != null && addNewBooks) {
            // add books that are not in the database
            // Notify that we are getting NEW list of eBooks
            Log.i(Global.TAG, "Updating eBook List from " + libraryDir);
            
            for(int i=0, dirListLen=ybkFiles.length; i < dirListLen; i++) {
                String dirFilename = ybkFiles[i].getAbsolutePath();
                Log.d(Global.TAG, "dirFilename: " + dirFilename);
                
                boolean fileFoundInDb = false;
                
                fileCursor.moveToFirst();
                while(!fileCursor.isAfterLast()) {
                    
                    String dbFilename = fileCursor.getString(fileCursor.getColumnIndexOrThrow(YbkProvider.FILE_NAME));
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
                            android.R.drawable.stat_notify_more, "Reveal Library Refresh", 
                            mNotifMgr, mNotifId++, Main.class);
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
                        mListCursor = mContRes.query(mBookUri, new String[] {YbkProvider.FORMATTED_TITLE, YbkProvider._ID}, 
                                YbkProvider.BINDING_TEXT + " is not null", null,
                                " LOWER(" + YbkProvider.FORMATTED_TITLE + ") ASC");
    
                        Util.sendNotification(this, "Added '" + bookName + "' to the library", 
                                android.R.drawable.stat_notify_more, "Reveal Library Refresh", 
                                mNotifMgr, mNotifId++, Main.class);
                    } else {
                        Util.sendNotification(this, "Could not add '" + bookName + "'. Bad file?", 
                                android.R.drawable.stat_sys_warning, "Reveal Library Refresh", 
                                mNotifMgr, mNotifId++, Main.class);                        
                    }

                }
            }            
            
        }
        
        Log.i(Global.TAG, "Removing Books from the database which are not in directory");
        
        // remove the books from the database if they are not in the directory
        int fileIndex = 0;
        fileCursor.moveToFirst();
        while(!fileCursor.isAfterLast()) {
            fileIndex++;
            String dbFilename = fileCursor.getString(fileCursor.getColumnIndexOrThrow(YbkProvider.FILE_NAME));
            
            boolean fileFoundInDir = false;
            for(int i = 0, dirListLen = ybkFiles.length; i < dirListLen; i++) {
                String dirFilename = ybkFiles[i].getAbsolutePath();
                if (dirFilename.equalsIgnoreCase(dbFilename)) {
                    fileFoundInDir = true;
                    break;
                } 
            }
            
            if (!fileFoundInDir) {
                neededRefreshing = true;
                String bookId = fileCursor.getString(fileCursor.getColumnIndexOrThrow(YbkProvider._ID));
                Uri deleteUri = ContentUris.withAppendedId(bookUri, Long.parseLong(bookId));
                contRes.delete(deleteUri, null , null);
                
                
            }
            
            fileCursor.moveToNext();
        }
            
        // no longer need the fileCursor
        stopManagingCursor(fileCursor);
        fileCursor.close();
        
        if (neededRefreshing) {

            Util.sendNotification(this, "Refreshing of library complete.", 
                    android.R.drawable.stat_notify_more, "Reveal Library Refresh", 
                    mNotifMgr, mNotifId++, Main.class);

        }
        
    }
    
    /**
     * Refresh the list of books in the main list.
     */
    private void refreshBookList() {
        mListCursor = mContRes.query(mBookUri, new String[] {YbkProvider.FORMATTED_TITLE, YbkProvider._ID}, 
                YbkProvider.BINDING_TEXT + " is not null", null,
                " LOWER(" + YbkProvider.FORMATTED_TITLE + ") ASC");
        
        startManagingCursor(mListCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{YbkProvider.FORMATTED_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.bookText};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter bookAdapter = 
                new SimpleCursorAdapter(this, R.layout.book_list_row, mListCursor, from, to);
        
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
    
    //do stuff with the Gestures we capture.
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case DELETE_ID:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            //Write Delete from DB Helper
            //DeleteFileHere(info.id);
            refreshBookList();
            return true;
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.really_delete);
	}
    
    @Override
    public void onResume() {
        super.onResume();
        
        setProgressBarIndeterminateVisibility(false);
        
        // Set preferences from Setting screen
        SharedPreferences sharedPref = mSharedPref;
        
        String libDir = mLibraryDir = sharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        if(!mLibraryDir.endsWith("/")) {
        	mLibraryDir = mLibraryDir + "/";
        }
          
        mCurrentDirectory = new File(libDir);
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history)
            .setIcon(android.R.drawable.ic_menu_recent_history);
        //menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE,  R.string.menu_bookmark)
        //    .setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE,  R.string.menu_settings)
            .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE,  R.string.menu_refresh_library)
        	.setIcon(android.R.drawable.ic_menu_rotate);        
        menu.add(Menu.NONE, BROWSER_ID, Menu.NONE,  R.string.menu_browser)
        	.setIcon(android.R.drawable.ic_menu_set_as);        
        menu.add(Menu.NONE, REVELUPDATE_ID, Menu.NONE,  R.string.menu_update)
        	.setIcon(android.R.drawable.ic_menu_share);
        menu.add(Menu.NONE, ABOUT_ID, Menu.NONE,  R.string.menu_about)
    	.setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        switch(item.getItemId()) {
        case REFRESH_LIB_ID:
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
        	UpdateChecker.checkForNewerVersion(Global.SVN_VERSION, this);
        	return true;
        	
        case ABOUT_ID: 
        	AboutDialog.create(this);
        	return true;
        
        case HISTORY_ID: 
        	startActivity(new Intent(this, HistoryDialog.class));
        	return true;
        	

        }
       
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        
    }
    
    /**
     * This function browses to the
     * root-directory of the file-system.
     */
    /*private void browseToRoot(final boolean showFolders) {
         browseTo(new File(mLibraryDir), showFolders);
    }*/
    
    /**
     * This function browses up one level
     * according to the field: mCurrentDirectory
     */
    @SuppressWarnings("unused")
    private void upOneLevel(){
         if(this.mCurrentDirectory.getParent() != null)
              this.browseTo(this.mCurrentDirectory.getParentFile(), true);
    } 
    
    private void browseTo(final File aDirectory, final boolean showFolders) {
        
        if (aDirectory.isDirectory()){
             this.mCurrentDirectory = aDirectory;
             //fill(aDirectory.listFiles(), showFolders);
        } else {
             OnClickListener okButtonListener = new OnClickListener() {
                  // @Override
                 public void onClick(DialogInterface arg0, int arg1) {
                     // Lets start an intent to View the file, that was clicked...
                     String path = aDirectory.getAbsolutePath();
                     Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW,
                               Uri.parse("file://" + path));
                     
                     startActivity(myIntent); 
                 }
             };
     
             OnClickListener cancelButtonListener = new OnClickListener(){
                 // @Override
                 public void onClick(DialogInterface arg0, int arg1) {
                     // Do nothing
                 }
             };
     
             AlertDialog.Builder builder = new AlertDialog.Builder(this);
             builder.setMessage("Do you want to open that file?\n"
                     + aDirectory.getName());
             builder.setNegativeButton("Cancel", cancelButtonListener);
             builder.setPositiveButton("OK", okButtonListener);
             builder.show();
        }
    }

    @Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        
        setProgressBarIndeterminateVisibility(true);        
        
        Log.d(Global.TAG, "selectionRowId/id: " + selectionRowId + "/" + id);
        
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
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.library_not_created)
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked OK so do some stuff */
                }
            })
            .create();
        }
        return null;
    }

	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
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
		// TODO Auto-generated method stub
		
	}
}
