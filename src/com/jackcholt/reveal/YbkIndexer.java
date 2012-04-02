package com.jackcholt.reveal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.Version;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import com.jackcholt.reveal.data.Chapter;

public class YbkIndexer {

    private static final String TAG = "YbkIndexer";
    public static final String INDEX_EXT = ".lucene";

    public static final String FILE_FIELDNAME = "file";
    public static final String CHAPTER_FIELDNAME = "chapter";
    public static final String CONTENT_FIELDNAME = "content";

    public static final Map<String, YbkIndexer> cachedIndexers = new TreeMap<String, YbkIndexer>(
            String.CASE_INSENSITIVE_ORDER);

    private IndexWriter mIndexWriter;
    private String mBookname;
    private YbkFileReader mReader;
    
    static SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(Main.getMainApplication());

    // reusing fields is supposed to speed up indexing per Lucene docs
    private final Field fileField = new Field(FILE_FIELDNAME, "", Store.YES, Index.NOT_ANALYZED_NO_NORMS);
    private final Field chapterField = new Field(CHAPTER_FIELDNAME, "", Store.YES, Index.NO);
    private final Field contentField = new Field(CONTENT_FIELDNAME, "", Store.NO, Index.ANALYZED_NO_NORMS);

    private YbkIndexer(Context ctx, String bookName) throws IOException {
        mBookname = bookName;
        File indexDir = new File(sharedPrefs.getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY), bookName + INDEX_EXT);
        FSDirectory fsDirectory;
        fsDirectory = FSDirectory.open(indexDir, NoLockFactory.getNoLockFactory());
        try {
            fsDirectory.clearLock("write.lock");
            IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LUCENE_35, new StandardAnalyzer(
                    Version.LUCENE_35));
            mIndexWriter = new IndexWriter(fsDirectory, writerConfig);
        } catch (IOException ioe) {
            fsDirectory.close();
            Util.deleteDir(indexDir);
            throw ioe;
        }
        mReader = YbkFileReader.getReader(ctx, bookName);
    }
    
    public static File getIndexFile(String bookName) {
        return new File (new File(sharedPrefs.getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY), "data"), bookName + INDEX_EXT);
    }

    /**
     * Get the appropriate content analyzer for the language
     * 
     * @param lang
     *            the language
     * @return the content analyzer
     */
    public static Analyzer getAnalyzer(String lang) {
        // TODO need to actually select an appropriate analyzer for the language
        // but since at this point we don't have language information we'll have
        // to
        // figure that out before we worry too much about how to select an
        // appropriate analyzer
        return new StandardAnalyzer(Version.LUCENE_35);
    }

    /**
     * Get or create an indexer for a book
     * @param ctx
     * @param bookName
     * @return the indexer
     */
    public static synchronized YbkIndexer getYbkIndexer(Context ctx, String bookName) {
        YbkIndexer indexer = cachedIndexers.get(bookName);
        if (indexer == null) {
            try {
                indexer = new YbkIndexer(ctx, bookName);
            } catch (IOException e) {
                Log.e(TAG, "Could not create index for " + bookName + ": " + Util.getStackTrace(e));
            }
            cachedIndexers.put(bookName, indexer);
        }
        return indexer;
    }

    public void commit() throws IOException {
        mIndexWriter.commit();
    }

    public void close() throws IOException {
        synchronized(cachedIndexers) {
            mReader.unuse();
            cachedIndexers.remove(mBookname);
            commit();
            Directory dir = mIndexWriter.getDirectory();
            mIndexWriter.close();
            dir.close();
        }
    }

    synchronized public static void removeBook(String bookName) {
        try {
            synchronized(cachedIndexers) {
                YbkIndexer indexer = cachedIndexers.remove(bookName);
                if (indexer != null) {
                    indexer.mIndexWriter.rollback();
                    indexer.mIndexWriter.close();
                    indexer.mIndexWriter.getDirectory().close();
                    indexer.mReader.unuse();
                }
            }
            Util.deleteDir(getIndexFile(bookName));
        } catch (IOException e) {
            Log.e(TAG, "Unable to remove index for book: " + bookName + Util.getStackTrace(e));
        }
    }


    /**
     * Adds a chapter from a book to the index.
     * 
     * @param ctx
     *            android context
     * @param reader
     *            the ybk reader
     * @param chapter
     *            the chapter object
     * @throws IOException 
     */
    public void addChapter(Chapter chapter) throws IOException {
        Log.d(TAG, "Adding chapter " + chapter.fileName + " to index");
        String chapterText = Html.fromHtml(mReader.readInternalFile(chapter.fileName)).toString();
        Document doc = new Document();
        fileField.setValue(mBookname);
        doc.add(fileField);
        chapterField.setValue(chapter.fileName);
        doc.add(chapterField);
        contentField.setValue(chapterText);
        doc.add(contentField);
        mIndexWriter.addDocument(doc);
    }

    /**
     * Adds a chapter from a book to the index.
     * 
     * @param ctx
     *            android context
     * @param fileName
     *            the filename
     * @return true if should proceed to next chapter
     * @throws IOException 
     */
    public boolean addChapter(int chapterIndex) throws IOException {
        Chapter chapter = mReader.getChapterByIndex(chapterIndex);
        if (chapter != null) {
            if (chapter.fileName.matches("(?i).*\\.html(\\.gz)?")) {
                addChapter(chapter);
            }
            return true;
        } else {
            return false;
        }
    }
}
