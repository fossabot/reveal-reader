package com.jackcholt.reveal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.Version;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import com.jackcholt.reveal.data.Chapter;

public class YbkSearcher {

    private static final String TAG = "YbkSearcher";
    private IndexSearcher mIndexSearcher;
    private MultiReader mMultiReader;
   
    static SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(Main.getMainApplication());

    // reusing fields is supposed to speed up indexing per Lucene docs
    public YbkSearcher() throws IOException {
        File indexDirs[] = new File(sharedPrefs.getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY)).listFiles(new FilenameFilter() {                    
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(YbkIndexer.INDEX_EXT);
                    }
                });
        if (indexDirs == null || indexDirs.length == 0) {
            throw new FileNotFoundException("No indexes found");
        }
        
        IndexReader readers[] = new IndexReader[indexDirs.length];
        for (int i = 0; i < indexDirs.length; i++) {
            File indexDir = indexDirs[i];
            FSDirectory fsDirectory = FSDirectory.open(indexDir, NoLockFactory.getNoLockFactory());
            readers[i] = IndexReader.open(fsDirectory);
        }
        mMultiReader = new MultiReader(readers, true);
        mIndexSearcher = new IndexSearcher(mMultiReader);
    }

    /**
     * Close the searcher.
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        mIndexSearcher.close();
        mMultiReader.close();
    }
    
    public TopDocs search(String searchString) throws ParseException, IOException {
        QueryParser queryParser = new QueryParser(Version.LUCENE_35, YbkIndexer.CONTENT_FIELDNAME, new StandardAnalyzer(
                    Version.LUCENE_35));
        Query query = queryParser.parse(searchString);
       return mIndexSearcher.search(query, 100);
    }

}
