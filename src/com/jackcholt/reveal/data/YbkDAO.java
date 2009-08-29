package com.jackcholt.reveal.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jackcholt.reveal.Settings;
import com.jackcholt.reveal.Util;

/**
 * A class for managing all the database accesses for the OODB and other data
 * related logic.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class YbkDAO {
    private SharedPreferences mSharedPref;
    private Stack<History> backStack = new Stack<History>();

    private static final String TAG = "YbkDAO";

    public static final String DATA_DIR = "data";
    public static final String BOOKS_FILE = "books.dat";
    public static final String HISTORY_FILE = "history.dat";
    public static final String BOOKMARKS_FILE = "bookmarks.dat";
    public static final String CHAPTER_EXT = ".chp";
    private static final int CHAPTER_CACHE_SIZE = 5;

    public static final String ID = "id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    private static YbkDAO instance = null;

    private ArrayList<History> historyList;
    private ArrayList<History> bookmarkList;

    private ArrayList<Book> bookList;
    private Map<String, ChapterDetails>chapterCache;
    private List<String>chapterCacheLRU;

    private File dataDirFile;
    
    private static class ChapterDetails implements Serializable {
        private static final long serialVersionUID = 1L;
        Chapter chapters[];
        int order[];
    }

    /**
     * Allow the user to get the one and only instance of the YbkDAO.
     * 
     * @return The YbkDAO singleton.
     */
    public synchronized static YbkDAO getInstance(final Context ctx) {
        if (instance == null) {
            instance = new YbkDAO(ctx);
        }
        return instance;
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
    private YbkDAO(final Context ctx) {
        open(ctx);
    }

    /**
     * Open the db.
     * 
     */
    public void open(Context ctx) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
        File libDirFile = new File(libDir);
        dataDirFile = new File(libDirFile, DATA_DIR);
        dataDirFile.mkdirs();

        bookList = getStoredBookList();
        historyList = getStoredHistoryList();
        bookmarkList = getStoredBookmarkList();
        chapterCache = new TreeMap<String, ChapterDetails>(String.CASE_INSENSITIVE_ORDER);
        chapterCacheLRU = new ArrayList<String>(CHAPTER_CACHE_SIZE);
    }

    /**
     * Get a list of book titles.
     * 
     * @return The (possibly empty) list of book titles as a field index.
     */
    public List<Book> getBookTitles() {
        synchronized (bookList) {
            List<Book> books = new ArrayList<Book>(bookList.size());
            for (Book book : bookList) {
                if (book.title != null) {
                    books.add(book);
                }
            }
            return books;
        }
    }

    /**
     * Get a list of books.
     * 
     * @return The list of books
     */
    public List<Book> getBooks() {
        return Collections.unmodifiableList(bookList);
    }

    /**
     * Get a List for history records that are bookmarks sorted by title.
     * 
     * @return
     */
    public List<History> getBookmarks() {
        synchronized (bookmarkList) {
            List<History> bookmarks = new ArrayList<History>(bookmarkList);
            Collections.sort(bookmarks, bookmarkTitleComparator);
            return bookmarks;
        }
    }

    private static final Comparator<History> bookmarkTitleComparator = new Comparator<History>() {

        public int compare(History hist1, History hist2) {
            return String.CASE_INSENSITIVE_ORDER.compare(hist1.title, hist2.title);
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
    public long insertBook(final String fileName, final String charset, final String title, final String shortTitle,
            final Chapter chapters[], int order[]) throws IOException {
        synchronized (bookList) {
            deleteBook(fileName);
            Log.d(TAG, "Inserting book: " + fileName);
            storeChapterDetails(fileName, chapters, order);
            long id = Util.getUniqueTimeStamp();
            Book book = new Book();
            book.id = id;
            book.fileName = fileName;
            book.charset = charset;
            book.active = true;
            book.title = title;
            book.shortTitle = shortTitle;
            bookList.add(book);
            Collections.sort(bookList, bookTitleComparator);
            storeBookList();
            return id;
        }
    }

    private static final Comparator<Book> bookTitleComparator = new Comparator<Book>() {

        public int compare(Book book1, Book book2) {
            String title1 = book1.title != null ? book1.title : "\uffff";
            String title2 = book2.title != null ? book2.title : "\uffff";

            return String.CASE_INSENSITIVE_ORDER.compare(title1, title2);
        }

    };

    /**
     * Convenience method to save a history item but not a bookmark (no
     * bookmarkNumber).
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
     */
    public boolean insertHistory(final long bookId, final String title, final String chapterName, final int scrollYPos) {
        return insertHistory(bookId, title, chapterName, scrollYPos, 0);
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
        if (bookmarkNumber == 0) {
            synchronized (historyList) {
                // find and remove duplicate history entries
                Iterator<History> it = historyList.iterator();
                while (it.hasNext()) {
                    History h = it.next();
                    if ((h.bookId == bookId) && h.chapterName.equalsIgnoreCase(chapterNameNoGz)) {
                        it.remove();
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
            success = true;
        }

        return success;
    }

    public boolean deleteBookmark(History hist) {
        synchronized (bookmarkList) {
            return bookmarkList.remove(hist);
        }
    }

    /**
     * Remove all histories beyond the maximum.
     * 
     */
    public void deleteHistories() {
        SharedPreferences sharedPref = mSharedPref;
        int histToKeep = sharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);
        int stackToKeep = histToKeep * 2;

        synchronized (historyList) {
            while (historyList.size() > histToKeep) {
                historyList.remove(historyList.size() - 1);
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
     */
    public boolean deleteBook(final String fileName) {
        synchronized (bookList) {
            uncacheChapterDetails(fileName);
            Iterator<Book> it = bookList.iterator();
            while (it.hasNext()) {
                Book book = it.next();
                if (book.fileName.equalsIgnoreCase(fileName)) {
                    it.remove();
                    deleteBookHistories(book.id);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the book from the database.
     * 
     * @param book
     *            The book to be deleted.
     * @return True if the book was deleted.
     */
    public boolean deleteBook(final Book book) {
        synchronized (bookList) {
            return bookList.remove(book);
        }
    }

    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId
     *            The key of the book to get.
     * @return The book object identified by bookId.
     */
    public Book getBook(final long bookId) {
        synchronized (bookList) {
            for (Book book : bookList) {
                if (book.id == bookId) {
                    return book;
                }
            }
        }
        return null;
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
        synchronized (bookList) {
            for (Book book : bookList) {
                if (book.fileName.equalsIgnoreCase(fileName)) {
                    return book;
                }
            }
        }
        return null;
    }

    /**
     * Get the history item identified by histId.
     * 
     * @param histId
     *            The key of the history to get.
     * @return The history object identified by histId.
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
     * Get a list if Histories sorted from newest to oldest for use with
     * ArrayAdapter for showing histories in a ListActivity.
     * 
     * @return the List of History objects.
     * @throws IOException
     */
    public List<History> getHistoryList() throws IOException {
        int maxHistories = mSharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);
        return getHistoryList(maxHistories);
    }

    /**
     * Delete all the histories/bookmarks associated with a book that is being
     * deleted
     * 
     * @param bookId
     */
    private void deleteBookHistories(long bookId) {
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
     * Get a chapter object identified by the fileName.
     *
     * @param bookId the id of the book
     * @param fileName
     *            the name of the internal chapter file.
     * @return the chapter
     * @throws IOException
     */
    public Chapter getChapter(final long bookId, final String fileName) throws IOException {
        Book book = getBook(bookId);
        return book == null ? null : getChapter(book, fileName);
    }
    
    /**
     * Get a chapter object identified by the fileName.
     * 
     * @param book the book
     * @param fileName
     *            the name of the internal chapter file.
     * @return the chapter
     * @throws IOException
     */
    public Chapter getChapter(final Book book, final String fileName) throws IOException {
        Chapter cmpChapter = new Chapter();
        cmpChapter.fileName = fileName.toLowerCase();
        Chapter chapter = null;
        ChapterDetails chapterDetails = getChapterDetails(book.fileName);
        if (chapterDetails != null) {
            int chapterIndex = Arrays.binarySearch(chapterDetails.chapters, cmpChapter, Chapter.chapterNameComparator);
            if (chapterIndex < 0) {
                cmpChapter.fileName += ".gz";
                chapterIndex = Arrays.binarySearch(chapterDetails.chapters, cmpChapter, Chapter.chapterNameComparator);
            }
            if (chapterIndex >= 0)
                chapter = chapterDetails.chapters[chapterIndex];
        }
        return chapter;
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
        Chapter chapter = null;
        if (orderId > 0) {
            Book book = getBook(bookId);
            if (book != null) {
                ChapterDetails chapterDetails = getChapterDetails(book.fileName);
                if (chapterDetails != null) {
                    if (orderId <= chapterDetails.order.length) { 
                        chapter = chapterDetails.chapters[chapterDetails.order[orderId - 1]];
                    }
                }
            }
        }
        return chapter;
    }

    //
    // /**
    // * Get the next chapter in bookwalker order.
    // *
    // * @param bookId
    // * The id of the book that contains the chapter.
    // * @param prevFileName
    // * The previous chapter filename (or empty string).
    // * @return The next chapter in the book, or null if none left
    // * @throws IOException
    // */
    // public Chapter getNextBookWalkerChapter(final long bookId, final String
    // prevFileName) throws IOException {
    // try {
    // String startKey = makeKey(bookId, prevFileName.toLowerCase());
    // TupleBrowser<String, Long> browser =
    // root.chapterNameIndex.browse(startKey);
    // Tuple<String, Long> tuple = new Tuple<String, Long>();
    // while (browser.getNext(tuple)) {
    // if (tuple.getKey().equals(startKey))
    // continue;
    // Chapter chapter = Chapter.load(mDb, (Long) tuple.getValue());
    // if (chapter != null) {
    // if (chapter.bookId != bookId)
    // return null;
    // if (chapter.fileName.toLowerCase().contains(".html"))
    // return chapter;
    // } else {
    // Log.e(TAG, "Unable to load chapter record " + tuple.getValue() +
    // " for key " + tuple.getKey());
    // }
    // }
    // return null;
    // } catch (RuntimeException rte) {
    // throw new RTIOException(rte);
    // }
    // }
    //
    
    /**
     * Check whether a chapter exists in a book.
     * 
     * @param bookId
     *            The id of the book.
     * @param fileName
     *            The name of the chapter (or internal file) that we're checking
     *            for.
     * @return True if the book has a chapter of that name, false otherwise.
     * @throws IOException
     */
    public boolean chapterExists(final long bookId, final String fileName) throws IOException {
         return null != getChapter(bookId, fileName);
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

    /**
     * Gets a the stored history list (or creates a new one if it can't be read)
     * 
     * @return the history list
     */
    @SuppressWarnings("unchecked")
    private ArrayList<History> getStoredHistoryList() {
        ArrayList<History> historyList = null;
        try {
            historyList = (ArrayList<History>) load(HISTORY_FILE);
            if (historyList.size() != 0) {
                @SuppressWarnings("unused")
                History history = historyList.get(0);
                // perform a sanity check on the type
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load existing book list, creating a new one instead.");
        }
        if (historyList == null) {
            historyList = new ArrayList<History>();
        }
        return historyList;
    }

    /**
     * Stores the history list.
     * 
     * @throws IOException
     */
    public void storeHistoryList() {
        synchronized (historyList) {
            try {
                store(HISTORY_FILE, historyList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a the stored bookmark list (or creates a new one if it can't be
     * read)
     * 
     * @return the bookmark list
     */
    @SuppressWarnings("unchecked")
    private ArrayList<History> getStoredBookmarkList() {
        ArrayList<History> bookmarkList = null;
        try {
            bookmarkList = (ArrayList<History>) load(BOOKMARKS_FILE);
            if (bookmarkList.size() != 0) {
                @SuppressWarnings("unused")
                // perform a sanity check on the type
                History history = bookmarkList.get(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load existing book list, creating a new one instead.");
        }
        if (bookmarkList == null) {
            bookmarkList = new ArrayList<History>();
        }
        return bookmarkList;
    }

    /**
     * Store chapter related information
     * @param fileName  filename of the book
     * @param chapters  array of chapter information
     * @param order     order map
     * @throws IOException
     */
    private void storeChapterDetails(final String fileName, final Chapter chapters[], final int[] order)
            throws IOException {
        String baseFileName = new File(fileName).getName().replaceFirst("(?s)\\..*", "");
        ChapterDetails chapterDetails = new ChapterDetails();
        chapterDetails.chapters = chapters;
        chapterDetails.order = order;
        store(baseFileName + CHAPTER_EXT, chapterDetails);
//      cacheChapterDetails(fileName, chapterDetails);
    }

    /**
     * Gets the chapter details for a book
     * 
     * @fileName the book filename
     * @return the bookmark list
     */
    private ChapterDetails getStoredChapterDetails(String fileName) {
        String baseFileName = new File(fileName).getName().replaceFirst("(?s)\\..*", "");
        ChapterDetails chapterDetails = null;
        try {
            chapterDetails = (ChapterDetails) load(baseFileName + CHAPTER_EXT);
            cacheChapterDetails(fileName, chapterDetails);
        } catch (Exception e) {
            Log.e(TAG, "Unable to load chapter details for book " + fileName);
        }
        return chapterDetails;
    }

    /**
     * Stores the bookmark list.
     * 
     * @throws IOException
     */
    public void storeBookmarkList() {
        synchronized (bookmarkList) {
            try {
                store(BOOKMARKS_FILE, bookmarkList);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores the book list.
     * 
     * @throws IOException
     */
    private void storeBookList() throws IOException {
        synchronized (bookList) {
            store(BOOKS_FILE, bookList);
        }
    }

    /**
     * Gets a the stored book list (or creates a new one if it can't be read)
     * 
     * @return the book list
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Book> getStoredBookList() {
        ArrayList<Book> bookList = null;
        try {
            bookList = (ArrayList<Book>) load(BOOKS_FILE);
            if (bookList.size() != 0) {
                @SuppressWarnings("unused")
                Book book = bookList.get(0);
                // perform a sanity check on the type
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load existing book list, creating a new one instead.");
        }
        if (bookList == null) {
            bookList = new ArrayList<Book>();
        }
        return bookList;
    }

    /**
     * Store a serializable object into a file
     * 
     * @param filename
     *            the filename
     * @param serializeable
     *            the object
     * 
     * @throws IOException
     */
    private void store(String filename, Serializable object) throws IOException {
        File file = new File(dataDirFile, filename);
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        boolean finished = false;
        try {
            os.writeObject(object);
            finished = true;
        } finally {
            os.close();
            if (!finished)
                file.delete();
        }
    }

    /**
     * Load a serializable object from a file
     * 
     * @param filename
     *            the filename
     * @return the object
     * 
     */
    private Object load(String filename) throws IOException {
        File file = new File(dataDirFile, filename);
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
        try {
            try {
                return is.readObject();
            } catch (ClassNotFoundException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
        } finally {
            is.close();
        }
    }
    
    /**
     * Cache chapter details for a book.
     * 
     * @param fileName book filename
     * @param chapterDetails the chapter details
     */
    private void cacheChapterDetails(String fileName, ChapterDetails chapterDetails) {
        synchronized (chapterCache) {
            // make room in cache
            while (chapterCacheLRU.size() > CHAPTER_CACHE_SIZE) {
                chapterCache.remove(chapterCacheLRU.remove(0));
            }
            chapterCache.put(fileName, chapterDetails);
            chapterCacheLRU.add(fileName);
        }
    }

    /**
     * Uncache chapter details for a book.
     * 
     * @param fileName the book filename
     */
    private void uncacheChapterDetails(String fileName) {
        synchronized (chapterCache) {
            chapterCache.remove(fileName);
            chapterCacheLRU.remove(fileName);
        }
    }

    /**
     * Get chapter details for a book.
     * 
     * @param fileName the book filename
     */
    private ChapterDetails getChapterDetails(String fileName) {
        synchronized (chapterCache) {
            ChapterDetails chapterDetails = chapterCache.get(fileName);
            if (chapterDetails == null)
                chapterDetails = getStoredChapterDetails(fileName);
            return chapterDetails;
        }
    }


}
