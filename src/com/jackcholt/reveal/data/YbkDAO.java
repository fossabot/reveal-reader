package com.jackcholt.reveal.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jackcholt.reveal.Main;
import com.jackcholt.reveal.Settings;
import com.jackcholt.reveal.Util;
import com.jackcholt.reveal.YbkService;

/**
 * A class for managing all the database accesses for the OODB and other data related logic.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class YbkDAO {
    private static String DATABASE_NAME = "reveal_ybk";

    private RecordManager mDb;

    private SharedPreferences mSharedPref;

    private Stack<History> backStack = new Stack<History>();

    private static final String TAG = "YbkDAO";

    public static final String ID = "id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    // public static final String CACHE_SIZE = "1000";
    public static final String CACHE_SIZE = "100";

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

    private Object writeGate = new Object();

    private List<History> historyList;
    private List<History> bookmarkList;

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
     * @throws IOException
     */
    private YbkDAO(final Context ctx) throws IOException {
        try {
            open(ctx);
        } catch (IOException ioe) {
            recreate(ctx);
        }
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

        // Apparently this option is experimental and has bugs, but could help
        // performance a lot
        // options.put(RecordManagerOptions.BUFFERED_INSTALLS, "true");

        // Apparently this option is not yet implemented
        // options.put(RecordManagerOptions.THREAD_SAFE, "true");

        options.put(RecordManagerOptions.CACHE_SIZE, CACHE_SIZE);

        // undocumented option that lets shrinks the memory usage down
        // to something we can live with on android
        options.put("jdbm.RecordFile.cleanMRUCapacity", CACHE_SIZE);

        // soft ref cache may help keep from running out of memory
        // options.put(RecordManagerOptions.CACHE_TYPE,
        // RecordManagerOptions.SOFT_REF_CACHE);

        mDb = RecordManagerFactory.createRecordManager(dbFile.getAbsolutePath(), options);
        Log.d(TAG, "Opened the database");
        root = YbkRoot.load(mDb);
        historyList = getStoredHistoryList();
        bookmarkList = getStoredBookmarkList();
    }

    /**
     * Close the db.
     * 
     * @throws IOException
     */
    private void close() throws IOException {
        if (mDb != null)
            mDb.close();
        mDb = null;
        root = null;
    }

    /**
     * Reopen the db. Primarily needed when the library directory changes.
     * 
     * @param ctx
     * @throws IOException
     */
    public void reopen(Context ctx) throws IOException {
        try {
            synchronized (writeGate) {
                close();
                open(ctx);
            }
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Recreate the db.
     * 
     * @param main
     * @throws IOException
     */
    private void recreate(Context ctx) throws IOException {
        // TODO - should we at least try to preserve the bookmarks?
        try {
            synchronized (writeGate) {
                if (mDb != null) {
                    try {
                        mDb.close();

                    } catch (Throwable t) {
                        // ignore this, and try to delete anyway
                        Log.e(TAG, t.toString());
                    }
                    mDb = null;
                    root = null;
                }
                String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
                File libDirFile = new File(libDir);
                File dbFile = new File(libDirFile, DATABASE_NAME + ".db");
                File lgFile = new File(libDirFile, DATABASE_NAME + ".lg");
                dbFile.delete();
                lgFile.delete();
                open(ctx);
            }
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
    public List<Book> getBookTitles() throws IOException {
        try {
            List<Book> books = new ArrayList<Book>();
            TupleBrowser<String, Long> browser = root.bookTitleIndex.browse();
            Tuple<String, Long> tuple = new Tuple<String, Long>();
            while (browser.getNext(tuple)) {
                Book book = Book.load(mDb, (Long) tuple.getValue());
                if (book != null) {
                    books.add(book);
                } else {
                    Log.e(TAG, "Unable to load book record " + tuple.getValue() + " for key " + tuple.getKey());
                }
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
    public List<Book> getBooks() throws IOException {
        try {
            List<Book> books = new ArrayList<Book>();
            TupleBrowser<Long, Long> browser = root.bookIdIndex.browse();
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (browser.getNext(tuple)) {
                Book book = Book.load(mDb, (Long) tuple.getValue());
                if (book != null) {
                    books.add(book);
                } else {
                    Log.e(TAG, "Unable to load book record " + tuple.getValue() + " for key " + tuple.getKey());
                }
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
    public List<History> getBookmarks() throws IOException {
        synchronized (bookmarkList) {
            List<History> bookmarks = new ArrayList<History>(bookmarkList);
            Collections.sort(bookmarks, bookmarkTitleComparator);
            return bookmarks;
        }
    }

    private static final Comparator<History> bookmarkTitleComparator = new Comparator<History>() {

        public int compare(History hist1, History hist2) {
            return hist1.title.compareTo(hist2.title);
        }

    };

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
    public long insertBook(final String fileName, final String charset, final String bindingText, final String title,
            final String shortTitle, final String metaData, final List<Chapter> chapters) throws IOException {
        // Debug.startMethodTracing("profiler", 20 * 1024 * 1024);
        synchronized (writeGate) {

            try {
                Log.d(TAG, "Inserting book: " + fileName);
                long id = Util.getUniqueTimeStamp();
                Book book = new Book();
                book.id = id;
                book.fileName = fileName.toLowerCase();
                book.charset = charset;
                book.active = true;
                book.bindingText = bindingText;
                book.formattedTitle = title == null ? null : Util.formatTitle(title);
                if (book.formattedTitle == Util.NO_TITLE) {
                    // use filename instead
                    book.formattedTitle = new File(fileName).getName();
                }
                book.title = title;
                book.shortTitle = shortTitle;
                book.metaData = metaData;

                boolean done = false;
                try {
                    book.create(mDb);
                    // make sure the book is put into the title index last so it
                    // won't show up in the list prematurely
                    done = insertChapters(id, chapters) && root.bookIdIndex.put(book.id, book)
                            && root.bookFilenameIndex.put(book.fileName, book);
                    if (done && book.formattedTitle != null) {
                        done = root.bookTitleIndex.put(book.formattedTitle, book);
                        if (!done) {
                            // almost certainly a duplicate book title - so tack on filename to try to make it unique
                            book.formattedTitle += " (" + new File(fileName).getName() + ")";
                            book.update(mDb);
                            done = root.bookTitleIndex.put(book.formattedTitle, book);
                        }
                    }
                } finally {
                    endTransaction(done);
                    if (!done) {
                        Log.e(TAG, "Could not insert book: " + fileName);
                        id = 0;
                    } else {
                        Log.i(TAG, "Inserted book: " + fileName);
                    }
                }
                // Debug.stopMethodTracing();
                return id;
            } catch (RuntimeException rte) {
                throw new RTIOException(rte);
            }
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
        int count = chapters.size();
        for (int i = 0; i < count; i++) {
            Chapter chap = chapters.get(i);
            // free up memory asap because we are running out inserting large
            // books
            chapters.set(i, null);
            chap.bookId = bookId;
            chap.create(mDb);
            boolean inserted = root.chapterNameIndex.put(makeKey(chap.bookId, chap.fileName.toLowerCase()), chap)
                    && (chap.orderNumber == 0 || root.chapterOrderNbrIndex.put(makeKey(chap.bookId, chap.orderNumber),
                            chap));
            if (!inserted) {
                Log.e(TAG, "Failed to insert chapter '" + chap.fileName + "'. Possible duplicate name?");
            }
            chap = null;
        }

        // free up the memory because big books can cause a memory squeeze
        chapters.clear();
        if (chapters instanceof ArrayList) {
            ((ArrayList<Chapter>) chapters).trimToSize();
        }
        return true;
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
     * @return True if the insert succeeded, False otherwise.
     * @throws IOException
     */
    public boolean insertHistory(final long bookId, final String title, final String chapterName, final int scrollYPos)
            throws IOException {
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
    public boolean insertHistory(final long bookId, final String title, final String chapterName, final int scrollYPos,
            final int bookmarkNumber) {
        String chapterNameNoGz = chapterName.replaceFirst("(?i)\\.gz$", "");
        History hist = new History();
        hist.bookId = bookId;
        hist.bookmarkNumber = bookmarkNumber;
        hist.chapterName = chapterNameNoGz;
        hist.scrollYPos = scrollYPos;
        hist.title = title;
        YbkService.requestAddHistory(Main.getMainApplication(), hist);
        if (bookmarkNumber == 0) {
            synchronized (historyList) {
                // find and remove duplicate history entries
                Iterator<History> it = historyList.iterator();
                while (it.hasNext()) {
                    History h = it.next();
                    if (h.bookId == hist.bookId && h.chapterName.equals(chapterNameNoGz)) {
                        it.remove();
                        YbkService.requestRemoveHistory(Main.getMainApplication(), h);
                    }
                }
                historyList.add(0, hist);
                backStack.push(hist);
                Log.d(TAG, "Added " + hist.chapterName + " to backStack");
            }
        } else {
            synchronized (bookmarkList) {
                bookmarkList.add(hist);
            }
        }
        return true;
    }

    public boolean updateHistory(final long histId, final long bookId, final String chapName, final int scrollYPos) {
        boolean success = false;
        History hist = getHistory(histId);

        if (hist != null) {
            hist.scrollYPos = scrollYPos;
            hist.bookId = bookId;
            hist.chapterName = chapName;
            YbkService.requestUpdateHistory(Main.getMainApplication(), hist);
            success = true;
        }

        return success;
    }

    /**
     * Insert a history (should only be called by YbkService).
     * 
     * @param hist
     *            the history to insert
     * @return True if the insert succeeded, False otherwise.
     * @throws IOException
     */
    public boolean insertHistory(final History hist) throws IOException {
        synchronized (writeGate) {
            try {
                boolean done = false;
                try {
                    hist.create(mDb);
                    done = root.historyIdIndex.put(hist.id, hist)
                            && (hist.bookmarkNumber == 0 || root.historyBookmarkNumberIndex.put(
                                    (long) hist.bookmarkNumber, hist));
                } finally {
                    endTransaction(done);
                }
                return done;
            } catch (RuntimeException rte) {
                throw new RTIOException(rte);
            }
        }
    }

    /**
     * Update a history; usually a bookmark (This should only be called by YbkService).
     * 
     * @param hist
     *            the history to update.
     * @return True if the insert succeeded, False otherwise.
     * @throws IOException
     */
    public boolean updateHistory(final History hist) throws IOException {
        synchronized (writeGate) {
            try {
                boolean done = false;
                try {
                    hist.update(mDb);
                    done = true;
                } finally {
                    endTransaction(done);
                }
                return done;
            } catch (RuntimeException rte) {
                throw new RTIOException(rte);
            }
        }
    }

    /**
     * Delete a history (should only be called by YbkService).
     * 
     * @param hist
     *            the history to delete
     * @throws IOException
     */
    public void deleteHistory(History hist) throws IOException {
        synchronized (writeGate) {
            try {
                History removedHist = root.historyIdIndex.remove(hist.id);
                if (removedHist != null) {
                    removedHist.delete(mDb);
                }
            } catch (RuntimeException rte) {
                throw new RTIOException(rte);
            } finally {
                endTransaction(true);
            }
        }
    }

    /**
     * Delete a bookmark (should only be called by YbkService).
     * 
     * @param bm
     *            The bookmark to delete.
     * @throws IOException
     */
    public void deleteBookmark(History bm) throws IOException {
        synchronized (writeGate) {
            try {
                History removedBm = root.historyBookmarkNumberIndex.remove((long) bm.bookmarkNumber);
                if (removedBm != null) {
                    removedBm.delete(mDb);
                    List<History> newBookmarkList = new ArrayList<History>();
                    for (History bookmark : bookmarkList) {
                        if (bookmark.id != removedBm.id) {
                            newBookmarkList.add(bookmark);
                        }
                    }
                    bookmarkList = newBookmarkList;

                    Log.d(TAG, "Deleted a bookmark");
                }
            } catch (RuntimeException rte) {
                throw new RTIOException(rte);
            } finally {
                endTransaction(true);
            }
        }
    }

    /**
     * Remove all histories beyond the maximum.
     * 
     * @throws IOException
     */
    public void deleteHistories() {
        SharedPreferences sharedPref = mSharedPref;
        int histToKeep = sharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);
        int stackToKeep = histToKeep * 2;

        synchronized (historyList) {
            while (historyList.size() > histToKeep) {
                History hist = historyList.remove(historyList.size() - 1);
                YbkService.requestRemoveHistory(Main.getMainApplication(), hist);
            }
            while (backStack.size() > stackToKeep) {
                backStack.remove(0);
            }
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
    public boolean deleteBook(final String fileName) throws IOException {
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
    public boolean deleteBook(final Book book) throws IOException {
        synchronized (writeGate) {
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
    public boolean toggleBookActivity(final Book book) throws IOException {
        synchronized (writeGate) {

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
    }

    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId
     *            The key of the book to get.
     * @return The book object identified by bookId.
     * @throws IOException
     */
    public Book getBook(final long bookId) throws IOException {
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
    public History getHistory(final long histId) {
        synchronized (historyList) {
            for (History hist : historyList) {
                if (hist.id == histId) {
                    return hist;
                }
            }
            for (History hist : bookmarkList) {
                if (hist.id == histId) {
                    return hist;
                }
            }
            return null;
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
    public History getBookmark(final int bmId) {
        synchronized (bookmarkList) {
            for (History hist : bookmarkList) {
                if (hist.bookmarkNumber == bmId) {
                    return hist;
                }
            }
            return null;
        }
    }

    /**
     * Get a list if Histories sorted from newest to oldest for use with ArrayAdapter for showing histories in a
     * ListActivity.
     * 
     * @return the List of History objects.
     * @throws IOException
     */
    public List<History> getHistoryList() throws IOException {
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
            if (hist != null && hist.bookId == bookId) {
                histList.add(hist);
            }
        }
        for (History hist : histList) {
            root.historyIdIndex.remove(hist.id);
            if (hist.bookmarkNumber != 0)
                root.historyBookmarkNumberIndex.remove((long) hist.bookmarkNumber);
            hist.delete(mDb);
        }
        for (List<History> list : new List[] { historyList, bookmarkList }) {
            synchronized (list) {
                Iterator<History> it = list.iterator();
                while (it.hasNext()) {
                    History hist = it.next();
                    if (hist.bookId == bookId) {
                        it.remove();
                    }
                }
            }
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
    private synchronized List<History> getHistoryList(int maxHistories) {
        synchronized (historyList) {
            ArrayList<History> retList = new ArrayList<History>(maxHistories);
            for (int i = 0; i < maxHistories && i < historyList.size(); i++) {
                retList.add(historyList.get(i));
            }
            return retList;
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
    private synchronized List<History> getStoredHistoryList() throws IOException {
        try {
            List<History> histList = new ArrayList<History>();
            int histCount = 0;
            // null means start at the end
            TupleBrowser<Long, Long> browser = root.historyIdIndex.browse(null);
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (browser.getPrevious(tuple)) {
                History hist = History.load(mDb, tuple.getValue());
                if (hist != null && hist.bookmarkNumber == 0) {
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
     * Get a list if Histories sorted from newest to oldest
     * 
     * @param maxHistories
     *            the maximum number of histories to return
     * @return the List of History objects.
     * @throws IOException
     */
    private synchronized List<History> getStoredBookmarkList() throws IOException {
        try {
            List<History> bookmarks = new ArrayList<History>();
            TupleBrowser<Long, Long> browser = root.historyBookmarkNumberIndex.browse();
            Tuple<Long, Long> tuple = new Tuple<Long, Long>();
            while (browser.getNext(tuple)) {
                History bookmark = History.load(mDb, tuple.getValue());
                if (bookmark != null) {
                    bookmarks.add(bookmark);
                } else {
                    Log.w(TAG, "There is a null bookmark object in the historyBookmarkNumberIndex.");
                }
            }
            return bookmarks;
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
    public List<History> getBookmarkList() {
        synchronized (bookmarkList) {
            return new ArrayList<History>(bookmarkList);
        }
    }

    /**
     * Get the last bookmark in the list.
     * 
     * @return The highest numbered bookmark.
     * @throws IOException
     */
    public int getMaxBookmarkNumber() throws IOException {
        synchronized (bookmarkList) {
            int bmNbr = 1;
            for (History hist : bookmarkList) {
                if (hist.bookmarkNumber > bmNbr) {
                    bmNbr = hist.bookmarkNumber;
                }
            }
            return bmNbr + 1;
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
    public Book getBook(final String fileName) throws IOException {
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
    public Chapter getChapter(final long bookId, final String fileName) throws IOException {
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
    public Chapter getChapter(final long bookId, final int orderId) throws IOException {
        try {
            return root.chapterOrderNbrIndex.get(makeKey(bookId, orderId));
        } catch (RuntimeException rte) {
            throw new RTIOException(rte);
        }
    }

    /**
     * Get the next chapter in bookwalker order.
     * 
     * @param bookId
     *            The id of the book that contains the chapter.
     * @param prevFileName
     *            The previous chapter filename (or empty string).
     * @return The next chapter in the book, or null if none left
     * @throws IOException
     */
    public Chapter getNextBookWalkerChapter(final long bookId, final String prevFileName) throws IOException {
        try {
            String startKey = makeKey(bookId, prevFileName.toLowerCase());
            TupleBrowser<String, Long> browser = root.chapterNameIndex.browse(startKey);
            Tuple<String, Long> tuple = new Tuple<String, Long>();
            while (browser.getNext(tuple)) {
                if (tuple.getKey().equals(startKey))
                    continue;
                Chapter chapter = Chapter.load(mDb, (Long) tuple.getValue());
                if (chapter != null) {
                    if (chapter.bookId != bookId)
                        return null;
                    if (chapter.fileName.toLowerCase().contains(".html"))
                        return chapter;
                } else {
                    Log.e(TAG, "Unable to load chapter record " + tuple.getValue() + " for key " + tuple.getKey());
                }
            }
            return null;
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
    public boolean chapterExists(final long bookId, final String fileName) throws IOException {
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
    public History getPreviousHistory(int historyPos) throws IOException {
        History hist = null;
        synchronized (historyList) {
            if (historyPos >= 0) {
                if (historyList.size() - 1 >= historyPos) {
                    hist = historyList.get(historyPos);
                }
            }
        }
        return hist;
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

    /**
     * Pop the the most recent History off the stack and return it.
     * 
     * @return The most recent History. If the stack is empty, return null.
     */
    public History popBackStack() {
        History hist = null;

        try {
            hist = backStack.pop();
        } catch (EmptyStackException ese) {
            // do nothing
        }

        return hist;
    }

    /**
     * Clears all Histories off the stack.
     * 
     */
    public void clearBackStack() {
        backStack.clear();
    }

}
