package com.jackcholt.reveal.search;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import android.content.Context;
import android.preference.PreferenceManager;

import com.jackcholt.reveal.Log;
import com.jackcholt.reveal.Settings;

/**
 * 
 * @author holtja
 *
 */
public class LuceneManager {
    private static final String LUCENE_DIRECTORY_FILENAME = "lucene_directory.dat";
    private static FSDirectory dir = null;
    private static final String TAG = LuceneManager.class.getSimpleName();
    private static IndexSearcher indexSearcher = null;
    private static IndexReader indexReader = null;
    
    QueryParser parser = new QueryParser(Version.LUCENE_33, "verseText", new StandardAnalyzer(Version.LUCENE_33));
    /**
     * Must be called before using {@link getDirectory()} to make sure that the Lucene Directory can be accessed.  This 
     * gives the developer a means of testing whether the Lucene Directory can be opened without having to catch an
     * exception. This initializes the connection to the Directory only once so it be called multiple times with no 
     * adverse affect. 
     * @param ctx The android application context. 
     * @return false if the Lucene directory cannot be opened, true otherwise.
     */
    public static boolean isDirReady(Context ctx) {
        if (null != dir) {
            return true;
        }

        String libDir = PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
        if (!libDir.endsWith("/")) {
            libDir += "/";
        }
        
        try {
            File dataDir = new File(libDir + "data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            dir = FSDirectory.open(new File(libDir + "data/" + LUCENE_DIRECTORY_FILENAME));
            indexSearcher = new IndexSearcher(dir);
            indexReader = IndexReader.open(dir);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Could open the Lucene directory file. " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the Lucene Directory.  This should not be called unless {@link isDirReady(Context)} returns true. 
     * @return the file-system-based Lucene directory.
     */
    public static Directory getDirectory() {
        if (null == dir) {
            throw new IllegalStateException("You must call isDirReady() at lease once before calling this.");
        }
        
        return dir;
    }
}
