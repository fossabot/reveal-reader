package com.jackcholt.revel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;


public class Main extends ListActivity {
    public final static int DISPLAYMODE_ABSOLUTE = 0;
    public final static int DISPLAYMODE_RELATIVE = 1;
    
    private int mDisplayMode = DISPLAYMODE_RELATIVE;

    private static final int HISTORY_ID = Menu.FIRST;
    private static final int BOOKMARK_ID = Menu.FIRST + 1;
    private static final int SETTINGS_ID = Menu.FIRST + 2;
    private static final int REFRESH_LIB_ID = Menu.FIRST + 3;
    
    
    private static final int ACTIVITY_SETTINGS = 0;
    
    private SharedPreferences mSharedPref;
    private boolean mShowSplashScreen;
    private String mLibraryDir;
    
    private File mCurrentDirectory = new File("/sdcard/"); 
    private ArrayList<IconifiedText> mDirectoryEntries = new ArrayList<IconifiedText>();
    //private Map<String,YbkFileReader> ybkReaders = new HashMap<String,YbkFileReader>();
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setLibraryDir(mSharedPref.getString("library_dir", "/sdcard/"));
        browseToRoot(false);
        
        
        /*setListAdapter(new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_1, mStrings));*/
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Set preferences from Setting screen
                
        setShowSplashScreen(mSharedPref.getBoolean("show_splash_screen", true));
        setLibraryDir(mSharedPref.getString("library_dir", "/sdcard/"));
        setDisplayMode(mSharedPref.getInt("filebrowser_display_mode", DISPLAYMODE_RELATIVE));
        
        mCurrentDirectory = new File(getLibraryDir());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, HISTORY_ID, Menu.NONE, R.string.menu_history)
            .setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(Menu.NONE, BOOKMARK_ID, Menu.NONE,  R.string.menu_bookmark)
            .setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, SETTINGS_ID, Menu.NONE,  R.string.menu_settings)
            .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, REFRESH_LIB_ID, Menu.NONE,  R.string.menu_refresh_library);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        switch(item.getItemId()) {
        case REFRESH_LIB_ID:
            browseToRoot(false);
            return true;
        case SETTINGS_ID:
            Intent intent = new Intent(this, Settings.class);
            startActivityForResult(intent, ACTIVITY_SETTINGS);
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * This function browses to the
     * root-directory of the file-system.
     */
    private void browseToRoot(final boolean showFolders) {
         browseTo(new File(getLibraryDir()), showFolders);
    }
    
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
        /*if(mDisplayMode == DISPLAYMODE_RELATIVE) 
            this.setTitle(aDirectory.getAbsolutePath() + " :: " +
                getString(R.string.app_name));*/
        
        if (aDirectory.isDirectory()){
             this.mCurrentDirectory = aDirectory;
             fill(aDirectory.listFiles(), showFolders);
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
    private void fill(final File[] files, final boolean showFolders) {
        mDirectoryEntries.clear();
        
        // add the ".." == 'Up one level'
              
        if(showFolders && !mCurrentDirectory.getParent().equalsIgnoreCase(getLibraryDir()))
             mDirectoryEntries.add(new IconifiedText(getString(R.string.up_one_level),
                     getResources().getDrawable(R.drawable.uponelevel),null));
        
        Drawable currentIcon = null;
        for (File currentFile : files) {
            String bookTitle = null;
            YbkTitleReader ybk = null;
            String filePath = "";
            
            if (showFolders && currentFile.isDirectory()) {
                  currentIcon = getResources().getDrawable(R.drawable.folder);
             } else { 
                 String fileName = currentFile.getName();
                 currentIcon = null;
                 
                 
                /* Determine the Icon to be used,
                 * depending on the FileEndings defined in:
                 * res/values/fileendings.xml. */
                if(checkEndsWithInStringArray(fileName, getResources().
                                    getStringArray(R.array.fileEndingWebText))) {
                     currentIcon = getResources().getDrawable(R.drawable.webtext);
                } else if(checkEndsWithInStringArray(fileName, getResources().
                                    getStringArray(R.array.fileEndingYbk))) {
                     currentIcon = getResources().getDrawable(R.drawable.ywd);
                     try {
                         filePath = mCurrentDirectory + "/" + fileName;
                         ybk = new YbkTitleReader(filePath);
                     } catch (IOException ioe) {
                         Log.w("revel", "Could not create a file reader for '" + fileName + "'.");
                         continue;
                     }
                     
                     bookTitle = Util.formatTitle(ybk.getBookTitle());
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
                    }
                    
                    mDirectoryEntries.add(new IconifiedText(bookTitle,currentIcon, filePath));
                    
                    //ContentValues values = new ContentValues();
                    //Uri uri = getContentResolver().insert(YbkProvider.BOOK_CONTENT_URI, values);
                    break;
                }
            }
        }
        
        Collections.sort(this.mDirectoryEntries);
        
        IconifiedTextListAdapter itla = new IconifiedTextListAdapter(this);
        itla.setListItems(mDirectoryEntries);   

        this.setListAdapter(itla); 
        
    }

    
    /**
     * @return the showSplashScreen
     */
    public final boolean isShowSplashScreen() {
        return mShowSplashScreen;
    }

    /**
     * @param showSplashScreen the showSplashScreen to set
     */
    public final void setShowSplashScreen(final boolean showSplashScreen) {
        mShowSplashScreen = showSplashScreen;
    }

    
    /**
     * @return the mLibraryDir
     */
    public final String getLibraryDir() {
        return mLibraryDir;
    }
    

    /**
     * @param libraryDir the mLibraryDir to set
     */
    public final void setLibraryDir(final String libraryDir) {
        mLibraryDir = libraryDir;
    }

    /** Checks whether checkItsEnd ends with
     * one of the Strings from fileEndings */
    private boolean checkEndsWithInStringArray(final String checkItsEnd,
                        final String[] fileEndings){
         for(String aEnd : fileEndings){
              if(checkItsEnd.endsWith(aEnd))
                   return true;
         }
         return false;
    }

    /**
     * @return the mDisplayMode
     */
    public final int getDisplayMode() {
        return mDisplayMode;
    }

    /**
     * @param displayMode the mDisplayMode to set
     */
    public final void setDisplayMode(final int displayMode) {
        mDisplayMode = displayMode;
    } 

    @Override
    protected void onListItemClick(final ListView listView, final View view, 
            final int selectionRowId, final long id) {
        //super.onListItemClick(listView, view, selectionRowId, id);
        
        String filePath = mDirectoryEntries.get(selectionRowId).getFilePath();
         
        /*if(selectedFileText.equals(getString(R.drawable.uponelevel))) {
            this.upOneLevel();
        } else {*/
         
        if (filePath != null) {
             // Start the activity to view the file.
             
             Intent intent = new Intent(this, YbkViewActivity.class);
             intent.putExtra(YbkViewActivity.KEY_FILEPATH, filePath);
             startActivity(intent);
                 
        }
        
    } 
}
