package com.jackcholt.reveal;

import java.io.File;
import java.io.FileFilter;
import java.util.Vector;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
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
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;


public class Main extends ListActivity {
    public final static int DISPLAYMODE_ABSOLUTE = 0;
    public final static int DISPLAYMODE_RELATIVE = 1;
    
    private static final int HISTORY_ID = Menu.FIRST;
    //private static final int BOOKMARK_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
    private static final int BROWSER_ID = Menu.FIRST + 4;
    private static final int REVELUPDATE_ID = Menu.FIRST + 5;
    private static final int ABOUT_ID = Menu.FIRST + 6;
    
    private static final int ACTIVITY_SETTINGS = 0;
    private static final int LIBRARY_NOT_CREATED = 0;
    
    private SharedPreferences mSharedPref;
    //private boolean mShowSplashScreen;
    private String mLibraryDir;
    private Uri mBookUri= Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book");
    private File mCurrentDirectory = new File("/sdcard/reveal/ebooks/"); 
    //private ArrayList<IconifiedText> mDirectoryEntries = new ArrayList<IconifiedText>();
    private Cursor mListCursor; 
    private ContentResolver mContRes; 

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        mContRes = getContentResolver(); 
        
        //Check here an XML file stored on our website for new version info
        //Just some info for online update.  its not really going to toast us to death..  :)
        //more like debugging.  
        //Toast.makeText(this, "Checking for new Version Online", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Version you are running SVN rev " + Global.SVN_VERSION, Toast.LENGTH_SHORT).show();
       
        //Actually go ONLINE and check...  duhhhh
        //UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
        //Toast.makeText(this, "Version available online rev " + Global.NEW_VERSION, Toast.LENGTH_SHORT).show();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLibraryDir = mSharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        
        createDefaultDirs();
        
        refreshLibrary();
        
        refreshBookList();
    }
    
    private void createDefaultDirs() {
        // Create the /sdcard/reveal and eBooks dir if they don't exist
        // Notify of creation and maybe put people directly into the TitleBrowser if they don't have ANY ybk's
         File revealdir = new File("/sdcard/reveal");
        if (!revealdir.mkdirs()) {
             Log.e(Global.TAG, "Create dir in sdcard failed");
        }
        File ebooksdir = new File(mLibraryDir);
        if (!ebooksdir.mkdirs()) {
             Log.e(Global.TAG, "Create dir in sdcard failed");
             //Jump to TTitleBrowser and download files
        }
    }
    
    /**
     * Refresh the eBook directory.
     */
    private void refreshLibrary() {
        ContentResolver contRes = mContRes;
        Uri bookUri = mBookUri;
        String strLibDir = mLibraryDir;
        
        // get a list of files from the database
        // Notify that we are getting current list of eBooks
        Toast.makeText(this, "Getting eBook list", Toast.LENGTH_SHORT).show();
            
        Cursor fileCursor = contRes.query(bookUri, 
                new String[] {YbkProvider.FILE_NAME,YbkProvider._ID}, null, null,
                YbkProvider.FILE_NAME + " ASC");
        
        startManagingCursor(fileCursor);
        
        if (fileCursor.getCount() == 0) {
            Log.w(Global.TAG, "eBook database has no valid YBK files");
            Toast.makeText(this, "eBook database has no valid YBK files", 
                    Toast.LENGTH_SHORT).show();
        }
        
        // get a list of files from the library directory
        File libraryDir = new File(strLibDir);
        if (!libraryDir.exists()) {
            if (!libraryDir.mkdirs()) {
                showDialog(LIBRARY_NOT_CREATED);
            }
        }
        
        File[] ybkFiles = libraryDir.listFiles(new YbkFilter());

        if (ybkFiles != null) {
            Vector<Integer> notFoundInDir = new Vector<Integer>();
            
            // add books that are not in the database
            // Notify that we are getting NEW list of eBooks
            Toast.makeText(this, "Updating eBook list", Toast.LENGTH_SHORT).show();
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
                    ContentValues values = new ContentValues();
                    values.put(YbkProvider.FILE_NAME, dirFilename);
                    contRes.insert(bookUri, values);
                }
            }
            
            // remove the books from the database if they are not in the directory
            fileCursor.moveToFirst();
            while(!fileCursor.isAfterLast()) {
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
                    String bookId = fileCursor.getString(fileCursor.getColumnIndexOrThrow(YbkProvider._ID));
                    contRes.delete(bookUri, YbkProvider._ID + "=?" , new String[] {bookId});
                }
                
                fileCursor.moveToNext();
            }
    
    
            for(Integer id : notFoundInDir) {
                contRes.delete(ContentUris.withAppendedId(bookUri, id), null, null);
            }

        }
            
        // no longer need the fileCursor
        stopManagingCursor(fileCursor);
        fileCursor.close();
        
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
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Set preferences from Setting screen
          
        SharedPreferences sharedPref = mSharedPref;
        //mShowSplashScreen = sharedPref.getBoolean("show_splash_screen", true);
        String libDir = mLibraryDir = sharedPref.getString("default_ebook_dir", "/sdcard/reveal/ebooks/");
        //mDisplayMode = sharedPref.getInt("filebrowser_display_mode", DISPLAYMODE_RELATIVE);
        
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
            refreshLibrary();
            refreshBookList();
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
        	UpdateChecker.checkForNewerVersion(Global.SVN_VERSION);
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
    
    /**
     * 
     * @param files
     */
    /*private void fill(final File[] files, final boolean showFolders) {
        mDirectoryEntries.clear();
        
        // create the book uri
        Uri bookUri = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book");
        int recordsDeleted = getContentResolver().delete(bookUri, null, null);
        Log.i(Global.TAG, recordsDeleted + " books cleaned out of the " 
                + YbkProvider.BOOK_TABLE_NAME + " table.");
        
        // add the ".." == 'Up one level'
              
        if(showFolders && !mCurrentDirectory.getParent().equalsIgnoreCase(mLibraryDir)) {
             mDirectoryEntries.add(new IconifiedText(getString(R.string.up_one_level),
                     getResources().getDrawable(R.drawable.uponelevel),null));
        }
        
        Drawable currentIcon = null;
        for (int i=0; i < files.length; i++) {
            File currentFile = files[i];
            String bookTitle = null;
            //YbkFileReader ybk = null;
            String filePath = "";
            
            if (showFolders && currentFile.isDirectory()) {
                  currentIcon = getResources().getDrawable(R.drawable.folder);
             } else { 
                 String fileName = currentFile.getName();
                 currentIcon = null;
                 
                 
                 Determine the Icon to be used,
                 * depending on the FileEndings defined in:
                 * res/values/fileendings.xml. 
                if(checkEndsWithInStringArray(fileName, getResources().
                                    getStringArray(R.array.fileEndingWebText))) {
                     currentIcon = getResources().getDrawable(R.drawable.webtext);
                } else if(checkEndsWithInStringArray(fileName, getResources().
                                    getStringArray(R.array.fileEndingYbk))) {
                     currentIcon = getResources().getDrawable(R.drawable.ywd);
                     //try {
                         filePath = mCurrentDirectory + "/" + fileName;
                         //ybk = new YbkFileReader(filePath);
                     } catch (IOException ioe) {
                         Log.w("reveal", "Could not create a file reader for '" + fileName + "'.");
                         continue;
                     }
                     
                     Cursor c = getContentResolver().query(bookUri, 
                             new String[] {YbkProvider.BOOK_TITLE}, 
                             YbkProvider.FILE_NAME + "='" + filePath + "'", null, null);
                     
                     // Try getting it from the database
                     if (c.getCount() > 0) {
                         c.moveToFirst();        
                         bookTitle = Util.formatTitle(c.getString(
                                 c.getColumnIndexOrThrow(YbkProvider.BOOK_TITLE)));
                     } else {
                         throw new IllegalStateException("Book exists in the library but couldn't get book title");
                     }
                             
                } else if(checkEndsWithInStringArray(fileName, getResources().
                        getStringArray(R.array.fileEndingText))){
                     currentIcon = getResources().getDrawable(R.drawable.text);
                }
            }
        
             
            if (null != currentIcon) { // only include the non-filtered files
                switch(mDisplayMode){
                case DISPLAYMODE_ABSOLUTE:
                    mDirectoryEntries.add(new IconifiedText(currentFile.getPath(), 
                                  currentIcon, filePath)); 
                    break;
                case DISPLAYMODE_RELATIVE: // On relative Mode, we have to add the current-path to the beginning
                    if (bookTitle == null) {
                        int currentPathStringLength = this.mCurrentDirectory.getAbsolutePath().length();
                        bookTitle = currentFile.getAbsolutePath()
                                   .substring(currentPathStringLength);
                    } else {
                        ContentValues values = new ContentValues();
                        values.put(YbkProvider.FILE_NAME, filePath);
                        values.put(YbkProvider.BOOK_TITLE, bookTitle);
                        //values.put(YbkProvider.SHORT_TITLE, ybk.getBookShortTitle());
                        //values.put(YbkProvider.METADATA, ybk.getBookMetaData());
                        //values.put(YbkProvider.BINDING_TEXT, ybk.getBindingText());                        
                        Uri uri = getContentResolver().insert(
                                bookUri, values);
                    }
                    
                    mDirectoryEntries.add(new IconifiedText(bookTitle, currentIcon, filePath));
                    
                    break;
                }
            }
        }
        
        Collections.sort(this.mDirectoryEntries);
        
        IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
        itla.setListItems(mDirectoryEntries);   

        this.setListAdapter(itla); 
        
    }*/

    /** Checks whether checkItsEnd ends with
     * one of the Strings from fileEndings */
    /*private boolean checkEndsWithInStringArray(final String checkItsEnd,
                        final String[] fileEndings){
         for(String aEnd : fileEndings){
              if(checkItsEnd.endsWith(aEnd))
                   return true;
         }
         return false;
    }*/

    @Override
    protected void onListItemClick(final ListView listView, final View view, 
            
            final int selectionRowId, final long id) {
        
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
}
