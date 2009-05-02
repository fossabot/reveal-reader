package com.jackcholt.reveal.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jackcholt.reveal.Settings;
import com.jackcholt.reveal.Util;

/**
 * A class for managing all the database accesses for the Perst OODB.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class YbkDAO {
    private static String DATABASE_NAME = "reveal_ybk";

    private RecordManager mDb;

    private SharedPreferences mSharedPref;

    private static final String TAG = "YbkDAO";

    public static final String ID = "id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    public static final boolean useJournaledTransactions = true;

    /**
     * Is the chapter a navigation chapter? Data type: INTEGER. Use {@link CHAPTER_TYPE_NO_NAV} and
     * {@link CHAPTER_TYPE_NAV} to set values.
     */
    public static final String CHAPTER_NAV_FILE = "nav_file";

    /**
     * Should the user be able to zoom the page? Data type: INTEGER. Used when the chapter contains a picture. Use
     * {@link CHAPTER_ZOOM_MENU_OFF} and {@link CHAPTER_ZOOM_MENU_ON} to set values.
     */
    public static final String CHAPTER_ZOOM_PICTURE = "zoom_picture";

    private static YbkDAO instance = null;

    private YbkRoot root = null;

    /**
     * Allow the user to get the one and only instance of the YbkDAO.
     * 
     * @return The YbkDAO singleton.
     * @throws IOException
     */
    public synchronized static YbkDAO getInstance(final Context ctx) throws IOException {
        try {
            if (instance == null) {
                instance = new YbkDAO(ctx);
            }
            return instance;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Disallow cloning to avoid getting around the singleton design pattern.
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("YbkDAO may not be cloned.");
    }

    /**
     * Make this a singleton by blocking direct access to the constructor.
     * 
     * 
     * 
     * @throws IOException
     */
    private YbkDAO(final Context ctx) throws IOException {
        open(ctx);
    }

    /**
     * Open the db.
     * 
     * @throws IOException
     */
    private void open(Context ctx) throws IOException {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
        File libDirFile = new File(libDir);
        File dbFile = new File(libDirFile, DATABASE_NAME);

        Properties options = new Properties();

        // Apparently this option is experimental and has bugs, but could help performance a lot
        // options.put(RecordManagerOptions.BUFFERED_INSTALLS, "true");
        // Apparently this option is not yet implemented
        // options.put(RecordManagerOptions.THREAD_SAFE, "true");
        if (!useJournaledTransactions)
            options.put(RecordManagerOptions.DISABLE_TRANSACTIONS, "true");
        mDb = RecordManagerFactory.createRecordManager(dbFile.getAbsolutePath(), options);
        Log.d(TAG, "Opened the database");
        root = YbkRoot.load(mDb);
    }

    /**
     * Close the db.
     * 
     * @throws IOException
     */
    private void close() throws IOException {
        if (mDb != null)
            mDb.close();
        root = null;
    }

    /**
     * Reopen the db. Primarily needed when the library directory changes.
     * 
     * @param ctx
     * @throws IOException
     */
    public synchronized void reopen(Context ctx) throws IOException {
        try {
            close();
            open(ctx);
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a list of book titles.
     * 
     * @return The (possibly empty) list of book titles as a field index.
     * @throws IOException
     */
    public synchronized List<Book> getBookTitles() throws IOException {
        try {
            List<Book> books = new ArrayList<Book>();
            TupleBrowser<String, Long> browser = root.bookTitleIndex.browse();
            Tuple<String, Long> tuple = new Tuple<String, Long>();
            while (browser.getNext(tuple)) {
                books.add(Book.load(mDb, (Long) tuple.getValue()));
            }
            return books;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a list of books.
     * 
     * @return The list of books
     * @throws IOException
     */
    public synchronized List<Book> getBooks() throws IOException {
        try {
            List<Book> books = new ArrayList<Book>();
            TupleBrowser<Long, Long> browser = root.bookIdIndex.browse();
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (browser.getNext(tuple)) {
                books.add(Book.load(mDb, tuple.getValue()));
            }
            return books;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a List for history records that are bookmarks sorted by title.
     * 
     * @return
     * @throws IOException
     */
    public synchronized List<History> getBookmarks() throws IOException {
        try {
            List<History> bookmarks = new ArrayList<History>();
            TupleBrowser<String, Long> browser = root.historyTitleIndex.browse();
            Tuple<String, Long> tuple = new Tuple<String, Long>();
            while (browser.getNext(tuple)) {
                bookmarks.add(History.load(mDb, tuple.getValue()));
            }
            return bookmarks;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Insert a book into the database.
     * 
     * @param fileName
     *            The name of the file that contains the book.
     * @param bindingText
     *            The text that describes the book.
     * @param title
     *            The title of the book.
     * @param shortTitle
     *            the abbreviated title.
     * @param metaData
     *            Descriptive information.
     * @param chapters
     *            the list of chapters
     * @throws IOException
     */
    public synchronized long insertBook(final String fileName, final String bindingText, final String title,
            final String shortTitle, final String metaData, final List<Chapter> chapters) throws IOException {
        try {
            Log.d(TAG, "Inserting book: " + fileName);
            long id = Util.getUniqueTimeStamp();
            Book book = new Book();
            book.id = id;
            book.fileName = fileName.toLowerCase();
            book.active = true;
            book.bindingText = bindingText;
            book.formattedTitle = title == null ? null : Util.formatTitle(title);
            book.title = title;
            book.shortTitle = shortTitle;
            book.metaData = metaData;

            boolean done = false;
            try {
                book.create(mDb);
                done = root.bookIdIndex.put(book.id, book) && root.bookFilenameIndex.put(book.fileName, book)
                        && (book.formattedTitle == null || root.bookTitleIndex.put(book.formattedTitle, book))
                        && insertChapters(id, chapters);
            } finally {
                endTransaction(done);
                if (!done) {
                    Log.e(TAG, "Could not insert book: " + fileName);
                    id = 0;
                } else {
                    Log.i(TAG, "Inserted book: " + fileName);
                }
            }
            return id;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Insert a list of chapters (should only be called from insertBook)
     * 
     * @param chapters
     *            the list of chapters
     * @return true if successful
     * @throws IOException
     */
    private boolean insertChapters(long bookId, List<Chapter> chapters) throws IOException {
        boolean done = false;
        for (Chapter chap : chapters) {
            chap.bookId = bookId;
            chap.create(mDb);
            done = root.chapterNameIndex.put(makeKey(chap.bookId, chap.fileName.toLowerCase()), chap)
                    && (chap.orderNumber == 0 || root.chapterOrderNbrIndex.put(makeKey(chap.bookId, chap.orderNumber),
                            chap));
        }
        return done;
    }

    /**
     * Convenience method to save a history item but not a bookmark (no bookmarkNumber).
     * 
     * @param bookId
     *            The id of the book that this is related to.
     * @param title
     *            The title to be shown in the history/bookmark list.
     * @param chapterName
     *            The chapter that was being read.
     * @param scrollPos
     *            The position in the chapter that was being read.
     * @return
     * @throws IOException
     */
    public synchronized boolean insertHistory(final long bookId, final String title, final String chapterName,
            final int scrollYPos) throws IOException {
        try {
            return insertHistory(bookId, title, chapterName, scrollYPos, 0);
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Save a new history/bookMark item.
     * 
     * @param bookId
     *            The id of the book that this is related to.
     * @param historyTitle
     *            The title to be shown in the history/bookmark list.
     * @param chapterName
     *            The chapter that was being read.
     * @param scrollPos
     *            The position in the chapter that was being read.
     * @param bookmarkNumber
     *            The number of the bookmark to save.
     * @return True if the insert succeeded, False otherwise.
     * @throws IOException
     */
    public synchronized boolean insertHistory(final long bookId, final String title, final String chapterName,
            final int scrollYPos, final int bookmarkNumber) throws IOException {
        try {
            History hist = new History();
            hist.bookId = bookId;
            hist.bookmarkNumber = bookmarkNumber;
            hist.chapterName = chapterName;
            hist.scrollYPos = scrollYPos;
            hist.title = title;

            boolean done = false;
            try {
                hist.create(mDb);
                done = root.historyIdIndex.put(hist.id, hist)
                        && root.historyTitleIndex.put(hist.title, hist)
                        && (bookmarkNumber == 0 || root.historyBookmarkNumberIndex
                                .put((long) hist.bookmarkNumber, hist));
            } finally {
                endTransaction(done);
            }
            return done;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Remove all histories that have a timestamp earlier than the milliseconds passed in.
     * 
     * @throws IOException
     */
    public synchronized void deleteHistories() throws IOException {
        try {
            SharedPreferences sharedPref = mSharedPref;
            int histToKeep = sharedPref
                    .getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);

            List<History> historyList = getHistoryList(Integer.MAX_VALUE);
            if (historyList.size() > histToKeep) {
                List<History> delHistList = historyList.subList(histToKeep, historyList.size());
                if (delHistList.size() != 0) {
                    try {
                        for (int i = 0; i < delHistList.size(); i++) {
                            History hist = delHistList.get(i);
                            root.historyIdIndex.remove(hist.id);
                            root.historyTitleIndex.remove(hist.title);
                            hist.delete(mDb);
                        }
                    } finally {
                        endTransaction(true);
                    }
                }
            }
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Delete book from db based on filename.
     * 
     * @param fileName
     *            The absolute file path of the book.
     * @return True if the book was deleted.
     * @throws IOException
     */
    public synchronized boolean deleteBook(final String fileName) throws IOException {
        try {
            Book book = root.bookFilenameIndex.get(fileName.toLowerCase());
            return book != null ? deleteBook(book) : false;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Remove the book from the database.
     * 
     * @param book
     *            The book to be deleted.
     * @return True if the book was deleted.
     * @throws IOException
     */
    public synchronized boolean deleteBook(final Book book) throws IOException {
        try {
            boolean done = false;
            try {
                root.bookFilenameIndex.remove(book.fileName);
                root.bookIdIndex.remove(book.id);
                if (book.formattedTitle != null)
                    root.bookTitleIndex.remove(book.formattedTitle);
                deleteChapters(book.id);
                deleteBookHistories(book.id);
                book.delete(mDb);
                done = true;
            } finally {
                endTransaction(done);
            }
            return done;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Remove a book's chapters from the database. This should only be called deleteBook()
     * 
     * @param bookId
     *            The id of the book whose chapters are to be deleted.
     * @return True if the book chapters were deleted.
     * @throws IOException
     */
    private void deleteChapters(final long bookId) throws IOException {
        List<Chapter> chaps = root.chapterNameIndex.getStartsWith(makeKey(bookId, ""));
        for (Chapter chap : chaps) {
            root.chapterNameIndex.remove(makeKey(chap.bookId, chap.fileName.toLowerCase()));
            if (chap.orderNumber != 0)
                root.chapterOrderNbrIndex.remove(makeKey(chap.bookId, chap.orderNumber));
            chap.delete(mDb);
        }
    }

    /**
     * Change the book from active to inactive or vice versa.
     * 
     * @param book
     *            The book to change the active state of.
     * @return True if the Book is already in the database indexes and the update occurred successfully.
     * @throws IOException
     */
    public synchronized boolean toggleBookActivity(final Book book) throws IOException {
        try {
            boolean done = false;
            try {
                book.active ^= true;
                book.update(mDb);
                done = true;
            } finally {
                endTransaction(done);
            }

            return done;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId
     *            The key of the book to get.
     * @return The book object identified by bookId.
     * @throws IOException
     */
    public synchronized Book getBook(final long bookId) throws IOException {
        try {
            return root.bookIdIndex.get(bookId);
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get the history item identified by histId.
     * 
     * @param histId
     *            The key of the history to get.
     * @return The history object identified by histId.
     * @throws IOException
     */
    public synchronized History getHistory(final long histId) throws IOException {
        try {
            return root.historyIdIndex.get(histId);
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a bookmark by bookmarkNumber.
     * 
     * @param bmId
     *            the bookmark id.
     * @return The History object that contains the bookmark.
     * @throws IOException
     */
    public synchronized History getBookmark(final int bmId) throws IOException {
        try {
            return root.historyBookmarkNumberIndex.get((long) bmId);
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a list if Histories sorted from newest to oldest for use with ArrayAdapter for showing histories in a
     * ListActivity.
     * 
     * @return the List of History objects.
     * @throws IOException
     */
    public synchronized List<History> getHistoryList() throws IOException {
        int maxHistories = mSharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);
        return getHistoryList(maxHistories);
    }

    /**
     * Delete all the histories/bookmarks associated with a book that is being deleted
     * 
     * @param bookId
     * @throws IOException 
     */
    private void deleteBookHistories(long bookId) throws IOException {
        List<History> histList = new ArrayList<History>();
        TupleBrowser<Long, Long> browser = root.historyIdIndex.browse();
        Tuple<Long, Long> tuple = new Tuple<Long, Long>();
        while (browser.getNext(tuple)) {
            History hist = History.load(mDb, tuple.getValue());
            if (hist.bookId == bookId) {
                histList.add(hist);
            }
        }
        for (History hist : histList) {
            root.historyIdIndex.remove(hist.id);
            root.historyTitleIndex.remove(hist.title);
            if (hist.bookmarkNumber != 0)
                root.historyBookmarkNumberIndex.remove((long)hist.bookmarkNumber);
            hist.delete(mDb);           
        }
    }
    /**
     * Get a list if Histories sorted from newest to oldest
     * 
     * @param maxHistories
     *            the maximum number of histories to return
     * @return the List of History objects.
     * @throws IOException
     */
    private synchronized List<History> getHistoryList(int maxHistories) throws IOException {
        try {
            List<History> histList = new ArrayList<History>();
            int histCount = 0;
            TupleBrowser<Long, Long> browser = root.historyIdIndex.browse(null); // null means
            // start at the
            // end
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (histCount < maxHistories && browser.getPrevious(tuple)) {
                History hist = History.load(mDb, tuple.getValue());
                if (hist.bookmarkNumber == 0) {
                    histList.add(hist);
                    histCount++;
                }
            }
            return histList;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get list of bookmarks.
     * 
     * @return
     * @throws IOException
     */
    public synchronized List<History> getBookmarkList() throws IOException {
        try {
            List<History> bookmarks = new ArrayList<History>();
            TupleBrowser<Long, Long> browser = root.historyBookmarkNumberIndex.browse();
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (browser.getNext(tuple)) {
                bookmarks.add(History.load(mDb, tuple.getValue()));
            }
            return bookmarks;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get the last bookmark in the list.
     * 
     * @return The highest numbered bookmark.
     * @throws IOException
     */
    public synchronized int getMaxBookmarkNumber() throws IOException {
        try {
            int bmNbr = 1;
            TupleBrowser<Long, Long> browser = root.historyBookmarkNumberIndex.browse(null); // null means start at the
            // end
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            if (browser.getPrevious(tuple)) {
                History hist = History.load(mDb, tuple.getValue());
                bmNbr = hist.bookmarkNumber + 1;
            }
            return bmNbr;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a book object identified by the fileName.
     * 
     * @param fileName
     *            the filename we're looking for.
     * @return A Book object identified by the passed in filename.
     * @throws IOException
     */
    public synchronized Book getBook(final String fileName) throws IOException {
        try {
            return root.bookFilenameIndex.get(fileName.toLowerCase());
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get a chapter object identified by the fileName.
     * 
     * @param fileName
     *            the name of the internal chapter file.
     * @return the chapter
     * @throws IOException
     */
    public synchronized Chapter getChapter(final long bookId, final String fileName) throws IOException {
        try {
            return root.chapterNameIndex.get(makeKey(bookId, fileName.toLowerCase()));
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get chapter by book id and order id.
     * 
     * @param bookId
     *            The id of the book that contains the chapter.
     * @param orderId
     *            The number of the chapter in the order of chapters.
     * @return The chapter we're look for.
     * @throws IOException
     */
    public synchronized Chapter getChapter(final long bookId, final int orderId) throws IOException {
        try {
            return root.chapterOrderNbrIndex.get(makeKey(bookId, orderId));
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Check whether a chapter exists in a book.
     * 
     * @param bookId
     *            The id of the book.
     * @param fileName
     *            The name of the chapter (or internal file) that we're checking for.
     * @return True if the book has a chapter of that name, false otherwise.
     * @throws IOException
     */
    public synchronized boolean chapterExists(final long bookId, final String fileName) throws IOException {
        try {
            return (null != getChapter(bookId, fileName)) || (null != getChapter(bookId, fileName + ".gz"));
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * clean up the object.
     */
    public void finalize() throws Throwable {
        try {
            if (mDb != null) {
                close();
                Log.d(TAG, "Closed the database in finalize()");
            }
        } finally {
            super.finalize();
        }
    }

    private void endTransaction(boolean commit) throws IOException {
        if (commit) {
            mDb.commit();
        } else {
            mDb.rollback();
            // indexes have to be reloaded after a rollback
            root = YbkRoot.load(mDb, root.recID);
        }
    }

    /**
     * Get the history at the current position in the list of histories that the back button has taken us to.
     * 
     * @param historyPos
     *            The position in the history list that we're getting the history at.
     * @throws IOException
     */
    public synchronized History getPreviousHistory(int historyPos) throws IOException {
        try {
            History hist = null;

            if (historyPos >= 0) {
                List<History> historyList = getHistoryList();
                if (historyList.size() - 1 >= historyPos) {
                    hist = historyList.get(historyPos);
                }
            }
            return hist;
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    String makeKey(Object key1, Object key2) {
        return key1.toString() + '\uFFFF' + key2.toString();
    }

    /**
     * Wrapper to rethrow runtime errors as IOExceptions because there are many runtime exception possibilities that can
     * arise from data corruption or inconsistency and we want to make sure that they are handled by the caller.
     * 
     * 
     */
    public static class RTIOException extends IOException {
        private static final long serialVersionUID = 7842382591982841568L;

        private RTIOException(RuntimeException rte) {
            super(rte.getMessage());
            initCause(rte);
            Log.e(TAG, Util.getStackTrace(rte));
        }

    }
}
