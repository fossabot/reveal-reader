package com.jackcholt.reveal.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jackcholt.reveal.Settings;

/**
 * A class for managing all the database accesses for the OODB and other data related logic.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class YbkDAO {
    private SharedPreferences mSharedPref;
    private Stack<History> mBackStack = new Stack<History>();

    private static final String TAG = "reveal.YbkDAO";

    public static final String DATA_DIR = "data";
    public static final String BOOKS_FILE = "books.dat";
    public static final String HISTORY_FILE = "history.dat";
    public static final String BOOKMARKS_FILE = "bookmarks.dat";
    public static final String BACKSTACK_FILE = "backstack.dat";
    public static final String ANNOT_HILITE_FILE = "annotHilite.dat";
    public static final String FOLDER_FILE = "folders.dat";

    public static final String CHAPTER_EXT = ".chp";

    public static final String FILENAME = "FILENAME";
    public static final String HISTORY_ID = "HISTORY_ID";

    public static final int GET_LAST_HISTORY = 0;

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    public static final String NOTE = "com.jackcholt.reveal.YbkDAO.note";
    public static final String ANNOTHILITE = "com.jackcholt.reveal.YbkDAO.annot_hilite";
    public static final String COLOR = "com.jackcholt.reveal.YbkDAO.color";
    public static final String VERSE = "com.jackcholt.reveal.YbkDAO.verse";
    public static final String BOOK_FILENAME = "com.jackcholt.reveal.YbkDAO.book_filename";
    public static final String CHAPTER_FILENAME = "com.jackcholt.reveal.YbkDAO.chapter_filename";

    private static YbkDAO instance = null;

    private ArrayList<History> mHistoryList;
    private ArrayList<History> mBookmarkList;
    private ArrayList<History>[] mHistoryLists;
    private ArrayList<Book> mBookList;
    private ArrayList<AnnotHilite> mAnnotHiliteList;
    private TreeMap<String, SortedSet<String>> mFolderMap;

    private File mDataDirFile;

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
     */
    private YbkDAO(final Context ctx) {
        open(ctx);
    }

    /**
     * Open the db.
     * 
     */
    @SuppressWarnings("unchecked")
    public void open(Context ctx) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
        File libDirFile = new File(libDir);
        mDataDirFile = new File(libDirFile, DATA_DIR);
        mDataDirFile.mkdirs();

        mBookList = getStoredBookList();
        mHistoryList = getStoredHistoryList();
        mBookmarkList = getStoredBookmarkList();
        mHistoryLists = (ArrayList<History>[]) new ArrayList[] { mHistoryList, mBookmarkList };
        mAnnotHiliteList = getStoredAnnotHiliteList();
        mFolderMap = getStoredFolderMap();
    }

    /**
     * Get a list of book titles.
     * 
     * @return The (possibly empty) list of book titles as a list of Books.
     */
    public List<Book> getBookTitles() {
        synchronized (mBookList) {
            List<Book> books = new ArrayList<Book>(mBookList.size());
            for (Book book : mBookList) {
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
        return Collections.unmodifiableList(mBookList);
    }

    /**
     * Get a List for history records that are bookmarks sorted by title.
     * 
     * @return
     */
    public List<History> getBookmarks() {
        synchronized (mBookmarkList) {
            List<History> bookmarks = new ArrayList<History>(mBookmarkList);
            Collections.sort(bookmarks, bookmarkTitleComparator);
            return bookmarks;
        }
    }

    private static final Comparator<History> bookmarkTitleComparator = new Comparator<History>() {

        public int compare(History hist1, History hist2) {
            return String.CASE_INSENSITIVE_ORDER.compare(hist1.title, hist2.title);
        }

    };

    public void initAnnotHilites() {
        mAnnotHiliteList = new ArrayList<AnnotHilite>();
    }

    public ArrayList<AnnotHilite> getAnnotHilites() {
        synchronized (mAnnotHiliteList) {
            ArrayList<AnnotHilite> ah = new ArrayList<AnnotHilite>(mAnnotHiliteList);
            Collections.sort(ah, annotHiliteComparator);
            return ah;
        }
    }

    /**
     * Insert a new Annotation Highlight record into the database.
     * 
     * @param note the text of the annotation.
     * @param color the color to use to highlight the verse.
     * @param verse the verse to highlight.
     * @param bookFilename the filename of book that contains the verse to be highlighted/annotated.
     * @param chapterFilename the filename of the chapter that contains the verse to be highlighted/annotated.
     * @return the AnnotHilite object that was inserted.
     */
    public AnnotHilite insertAnnotHilite(final String note, final int color, final int verse,
            final String bookFilename, final String chapterFilename) {
        if (verse < 1 || null == bookFilename || null == chapterFilename) {
            throw new IllegalArgumentException(
                    "verse must be greater than 0, bookFilename and chapterFilename must not be null.\n"
                            + "verse/bookFilename/chapterFilename: " + verse + "/"
                            + (null == bookFilename ? "nil" : bookFilename) + "/"
                            + (null == chapterFilename ? "nil" : chapterFilename));
        }

        AnnotHilite ah = new AnnotHilite();
        ah.note = note;
        ah.color = color;
        ah.verse = verse;
        ah.bookFilename = bookFilename;
        ah.chapterFilename = chapterFilename;
        synchronized (mAnnotHiliteList) {
            Iterator<AnnotHilite> it = mAnnotHiliteList.iterator();
            while (it.hasNext()) {
                AnnotHilite h = it.next();
                if ((h.verse == verse && null != h.bookFilename && h.bookFilename.equalsIgnoreCase(bookFilename))
                        && null != h.chapterFilename && h.chapterFilename.equalsIgnoreCase(chapterFilename)) {
                    it.remove();
                }
            }
            mAnnotHiliteList.add(ah);
            Log.d(TAG, "added AnnotHilite");
            Collections.sort(mAnnotHiliteList, annotHiliteComparator);
            storeAnnotHiliteList();
        }
        return ah;
    }

    private static final Comparator<AnnotHilite> annotHiliteComparator = new Comparator<AnnotHilite>() {

        public int compare(AnnotHilite ah1, AnnotHilite ah2) {
            int compareFileVal = (ah1.bookFilename + ah1.chapterFilename).compareToIgnoreCase(ah2.bookFilename
                    + ah2.chapterFilename);

            if (compareFileVal != 0) {
                return compareFileVal;
            }

            return Integer.valueOf(ah1.verse).compareTo(ah2.verse);
        }
    };

    /**
     * Remove an AnnotHilite from the in-memory list and save the modified list to disk for persistence.
     * 
     * @param ah The AnnotHilite to delete.
     * @return the status of whether the removal was successful.
     */
    public boolean deleteAnnotHilite(AnnotHilite ah) {
        AnnotHilite ah2delete = null;
        synchronized (mAnnotHiliteList) {
            for(AnnotHilite ahItem : mAnnotHiliteList) {
                if (ahItem.equals(ah)) {
                    ah2delete = ahItem;
                    break;
                }
            }
            boolean success = mAnnotHiliteList.remove(ah2delete);
            storeAnnotHiliteList();
            return success;
        }
    }
    
    /**
     * Insert a book into the database.
     * 
     * @param fileName The name of the file that contains the book.
     * @param bindingText The text that describes the book.
     * @param title The title of the book.
     * @param shortTitle the abbreviated title.
     * @param metaData Descriptive information.
     * @param chapters the list of chapters
     * @return the Book
     * @throws IOException
     */
    public Book insertBook(final String fileName, final String charset, final String title, final String shortTitle,
            final ChapterIndex chapterIndex) throws IOException {
        synchronized (mBookList) {
            deleteBook(fileName);
            Log.d(TAG, "Inserting book: " + fileName);
            storeChapterIndex(fileName, chapterIndex);
            Book book = new Book();
            book.fileName = fileName;
            book.charset = charset;
            book.title = title;
            book.shortTitle = shortTitle;
            mBookList.add(book);
            Collections.sort(mBookList, bookTitleComparator);
            storeBookList();
            return book;
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
     * Save a new history/bookMark item.
     * 
     * @param bookFileName The filename of the book that this is related to.
     * @param historyTitle The title to be shown in the history/bookmark list.
     * @param chapterName The chapter that was being read.
     * @param scrollPos The position in the chapter that was being read.
     * @return True if the insert succeeded, False otherwise.
     */
    public boolean insertHistory(final String bookFileName, final String title, final String chapterName,
            final int scrollYPos) {
        String chapterNameNoGz = chapterName.replaceFirst("(?i)\\.gz$", "");
        History hist = new History();
        hist.bookFileName = bookFileName;
        hist.bookmarkNumber = 0;
        hist.chapterName = chapterNameNoGz;
        hist.scrollYPos = scrollYPos;
        hist.title = title;
        synchronized (mHistoryList) {
            // find and remove duplicate history entries
            Iterator<History> it = mHistoryList.iterator();
            while (it.hasNext()) {
                History h = it.next();
                if ((h.bookFileName.equalsIgnoreCase(bookFileName)) && h.chapterName.equalsIgnoreCase(chapterNameNoGz)) {
                    it.remove();
                }
            }
            mHistoryList.add(0, hist);
            deleteHistories();
            storeHistoryList();
        }
        return true;
    }

    public void addToBackStack(final String bookFileName, final String title, final String chapterName,
            final int scrollYPos) {
        History hist = new History();
        hist.bookFileName = bookFileName;
        hist.bookmarkNumber = 0;
        hist.chapterName = chapterName.replaceFirst("(?i)\\.gz$", "");
        hist.scrollYPos = scrollYPos;
        hist.title = title;
        synchronized (mHistoryList) {
            mBackStack.push(hist);
            Log.d(TAG, "Added " + hist.chapterName + " to backStack");
        }
    }

    /**
     * Save a new /bookMark item.
     * 
     * @param bookFileName The filename of the book that this is related to.
     * @param historyTitle The title to be shown in the bookmark list.
     * @param chapterName The chapter that was being read.
     * @param scrollPos The position in the chapter that was being read.
     * @param bookmarkNumber The number of the bookmark to save.
     * @return True if the insert succeeded, False otherwise.
     */
    public boolean insertBookmark(final String bookFileName, final String title, final String chapterName,
            final int scrollYPos, final int bookmarkNumber) {
        String chapterNameNoGz = chapterName.replaceFirst("(?i)\\.gz$", "");
        History hist = new History();
        hist.bookFileName = bookFileName;
        hist.bookmarkNumber = bookmarkNumber;
        hist.chapterName = chapterNameNoGz;
        hist.scrollYPos = scrollYPos;
        hist.title = title;
        synchronized (mBookmarkList) {
            mBookmarkList.add(hist);
            storeBookmarkList();
        }
        return true;
    }

    /**
     * Updates a bookmark entry
     * 
     * @param bmId
     * @param bookFileName
     * @param chapName
     * @param scrollYPos
     * @return
     */
    public boolean updateBookmark(final int bmId, final String bookFileName, final String chapName, final int scrollYPos) {
        boolean success = false;
        History hist = getBookmark(bmId);

        if (hist != null) {
            hist.scrollYPos = scrollYPos;
            hist.bookFileName = bookFileName;
            hist.chapterName = chapName;
            storeBookmarkList();
            success = true;
        }

        return success;
    }

    public boolean deleteBookmark(History hist) {
        synchronized (mBookmarkList) {
            boolean success = mBookmarkList.remove(hist);
            storeBookmarkList();
            return success;
        }
    }

    /**
     * Remove all histories beyond the maximum.
     * 
     */
    private void deleteHistories() {
        SharedPreferences sharedPref = mSharedPref;
        int histToKeep = sharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);
        int stackToKeep = histToKeep * 2;

        synchronized (mHistoryList) {
            while (mHistoryList.size() > histToKeep) {
                mHistoryList.remove(mHistoryList.size() - 1);
            }
            while (mBackStack.size() > stackToKeep) {
                mBackStack.remove(0);
            }
        }
    }

    /**
     * Delete book from db based on filename.
     * 
     * @param fileName The absolute file path of the book.
     * @return True if the book was deleted.
     */
    public boolean deleteBook(final String fileName) {
        synchronized (mBookList) {
            deleteChapterDetails(fileName);
            storeBookmarkList();
            storeHistoryList();
            Iterator<Book> it = mBookList.iterator();
            while (it.hasNext()) {
                Book book = it.next();
                if (book.fileName.equalsIgnoreCase(fileName)) {
                    it.remove();
                    storeBookList();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the book from the database.
     * 
     * @param book The book to be deleted.
     * @return True if the book was deleted.
     */
    public boolean deleteBook(final Book book) {
        synchronized (mBookList) {
            return mBookList.remove(book);
        }
    }

    /**
     * Get a book object identified by the fileName.
     * 
     * @param fileName the filename we're looking for.
     * @return A Book object identified by the passed in filename.
     * @throws IOException
     */
    public Book getBook(final String fileName) {
        synchronized (mBookList) {
            for (Book book : mBookList) {
                if (book.fileName.equalsIgnoreCase(fileName)) {
                    return book;
                }
            }
        }
        return null;
    }

    /**
     * Get a single {@link AnnotHilite} object identified by the parameters. If no AnnotHilite can be found, null is
     * returned.
     * 
     * @param verse
     * @param bookFilename
     * @param chapterFilename
     * @return
     */
    public AnnotHilite getAnnotHilite(int verse, String bookFilename, String chapterFilename) {
        Log.d(TAG, "verse/bookFilename/chapterFilename: " + verse + "/" + bookFilename + "/" + chapterFilename);

        synchronized (mAnnotHiliteList) {
            for (AnnotHilite ah : mAnnotHiliteList) {
                if (null == ah || null == ah.bookFilename || null == ah.chapterFilename) {
                    throw new IllegalStateException("Annotation Highlight object contains null values in primary key.");
                }
                if (ah.verse == verse && ah.bookFilename.equalsIgnoreCase(bookFilename)
                        && ah.chapterFilename.equalsIgnoreCase(chapterFilename)) {
                    return ah;
                }
            }
        }
        return null;
    }

    /**
     * Get the history item identified by histId.
     * 
     * @param histId The key of the history to get.
     * @return The history object identified by histId.
     */
    public History getHistory(final long histId) {
        History history = null;
        synchronized (mHistoryList) {
            synchronized (mBookmarkList) {
                for (List<History> list : mHistoryLists) {
                    for (History hist : list) {
                        if (hist.id == histId) {
                            if (getBook(hist.bookFileName) != null) {
                                // only return histories for which there is a book
                                history = hist;
                            }
                            break;
                        }
                    }
                }
            }
        }
        return history;
    }

    /**
     * Get a bookmark by bookmarkNumber.
     * 
     * @param bmId the bookmark id.
     * @return The History object that contains the bookmark.
     */
    public History getBookmark(final int bmId) {
        History history = null;
        synchronized (mBookmarkList) {
            for (History hist : mBookmarkList) {
                if (hist.bookmarkNumber == bmId) {
                    if (getBook(hist.bookFileName) != null) {
                        // only return histories for which there is a book
                        history = hist;
                    }
                    break;
                }
            }
            return history;
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
        synchronized (mHistoryList) {
            ArrayList<History> retList = new ArrayList<History>(mHistoryList.size());
            for (int i = 0; i < mHistoryList.size(); i++) {
                History hist = mHistoryList.get(i);
                if (getBook(hist.bookFileName) != null) {
                    // only return histories for which there is a book
                    retList.add(hist);
                }
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
        synchronized (mBookmarkList) {
            ArrayList<History> retList = new ArrayList<History>(mBookmarkList.size());
            for (int i = 0; i < mBookmarkList.size(); i++) {
                History hist = mBookmarkList.get(i);
                if (getBook(hist.bookFileName) != null) {
                    // only return bookmarks for which there is a book
                    retList.add(hist);
                }
            }
            return retList;
        }
    }

    /**
     * Get the last bookmark in the list.
     * 
     * @return The highest numbered bookmark.
     * @throws IOException
     */
    public int getMaxBookmarkNumber() throws IOException {
        synchronized (mBookmarkList) {
            int bmNbr = 1;
            for (History hist : mBookmarkList) {
                if (hist.bookmarkNumber > bmNbr) {
                    bmNbr = hist.bookmarkNumber;
                }
            }
            return bmNbr + 1;
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
            hist = mBackStack.pop();
        } catch (EmptyStackException ese) {
            // do nothing
        }

        return hist;
    }

    /**
     * Clears all Histories off the stack.
     */
    public void clearBackStack() {
        mBackStack.clear();
    }

    /**
     * Stores the history list.
     * 
     * @throws IOException
     */
    private void storeHistoryList() {
        synchronized (mHistoryList) {
            try {
                store(HISTORY_FILE, mHistoryList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                // perform a sanity check on the type
                historyList.get(0);
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
     * Gets a the stored history list (or creates a new one if it can't be read)
     * 
     * @return the history list
     */
    @SuppressWarnings("unchecked")
    public void loadStoredBackstack() {
        try {
            mBackStack = (Stack<History>) load(HISTORY_FILE);
            if (mBackStack.size() != 0) {
                // perform a sanity check on the type
                mBackStack.get(0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load existing book list, creating a new one instead.");
        }
        if (null == mBackStack) {
            mBackStack = new Stack<History>();
        }
    }

    /**
     * Stores the backstack.
     * 
     * @throws IOException
     */
    public void storeBackstack() {
        synchronized (mBackStack) {
            try {
                store(BACKSTACK_FILE, mBackStack);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a the stored bookmark list (or creates a new one if it can't be read).
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
     * Gets a the stored annotation highlight list (or creates a new one if it can't be read).
     * 
     * @return The annotation highlight list.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<AnnotHilite> getStoredAnnotHiliteList() {
        ArrayList<AnnotHilite> annotHiliteList = null;
        try {
            annotHiliteList = (ArrayList<AnnotHilite>) load(ANNOT_HILITE_FILE);
            if (annotHiliteList.size() != 0) {
                @SuppressWarnings("unused")
                // perform a sanity check on the type
                AnnotHilite ah = annotHiliteList.get(0);
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to load existing annotation highlight list, creating a new one instead.");
        }
        if (null == annotHiliteList) {
            annotHiliteList = new ArrayList<AnnotHilite>();
        }
        return annotHiliteList;
    }

    /**
     * Gets a the stored forld map (or creates a new one if it can't be read).
     * 
     * @return The folder map.
     */
    @SuppressWarnings("unchecked")
    private TreeMap<String, SortedSet<String>> getStoredFolderMap() {
        TreeMap<String, SortedSet<String>> folderMap = null;
        try {
            folderMap = (TreeMap<String, SortedSet<String>>) load(FOLDER_FILE);
            if (folderMap.size() != 0) {
                // perform a sanity check on the type
                try {
                    @SuppressWarnings("unused")
                    SortedSet<String> fm = folderMap.values().iterator().next();
                } catch (NoSuchElementException nse) {
                    // ignore - to be expected if no folders
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to load existing annotation highlight list, creating a new one instead.");
        }
        if (folderMap == null) {
            folderMap = new TreeMap<String, SortedSet<String>>(String.CASE_INSENSITIVE_ORDER);
        }
        return folderMap;
    }

    /**
     * Store chapter index
     * 
     * @param fileName filename of the book
     * @param chapters array of chapter information
     * @param order order map
     * @throws IOException
     */
    private void storeChapterIndex(final String fileName, final ChapterIndex chapterIndex) throws IOException {
        String baseFileName = new File(fileName).getName().replaceFirst("(?s)\\..*", "");
        store(baseFileName + CHAPTER_EXT, chapterIndex);
    }

    /**
     * Gets the chapter index for a book
     * 
     * @fileName the book filename
     * @return the chapter index
     */
    public ChapterIndex getChapterIndex(String fileName) {
        String baseFileName = new File(fileName).getName().replaceFirst("(?s)\\..*", "");
        ChapterIndex chapterIndex = null;
        try {
            chapterIndex = (ChapterIndex) load(baseFileName + CHAPTER_EXT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to load chapter details for book " + fileName, e);
        }
        return chapterIndex;
    }

    /**
     * Delete chapter details file for a book
     * 
     * @fileName the book filename
     * @return true if successful
     */
    private boolean deleteChapterDetails(String fileName) {
        String baseFileName = new File(fileName).getName().replaceFirst("(?s)\\..*", "");
        File file = new File(mDataDirFile, baseFileName + CHAPTER_EXT);
        return file.delete();
    }

    /**
     * Stores the bookmark list.
     * 
     * @throws IOException
     */
    private void storeBookmarkList() {
        synchronized (mBookmarkList) {
            try {
                store(BOOKMARKS_FILE, mBookmarkList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores the book list.
     * 
     */
    private void storeBookList() {
        synchronized (mBookList) {
            try {
                store(BOOKS_FILE, mBookList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void storeAnnotHiliteList() {
        synchronized (mAnnotHiliteList) {
            try {
                store(ANNOT_HILITE_FILE, mAnnotHiliteList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "stored annotHiliteList.  list size: " + mAnnotHiliteList.size());
    }

    private void storeFolderMap() {
        synchronized (mFolderMap) {
            try {
                store(FOLDER_FILE, mFolderMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "stored folderMap.  map size: " + mFolderMap.size());
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
            if (bookList.size() > 0) {
                bookList.get(0);
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
     * @param filename the filename
     * @param serializeable the object
     * 
     * @throws IOException
     */
    private void store(String filename, Serializable object) throws IOException {
        File file = new File(mDataDirFile, filename);
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file), 4096));
        boolean finished = false;
        try {
            os.writeObject(object);
            finished = true;
        } finally {
            os.close();
            if (!finished) {
                file.delete();
            }
        }
    }

    /**
     * Load a serializable object from a file
     * 
     * @param filename the filename
     * @return the object
     * 
     */
    private Object load(String filename) throws IOException {
        File file = new File(mDataDirFile, filename);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), 4096));
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

    public ArrayList<AnnotHilite> getChapterAnnotHilites(final String bookFilename, final String chapterFilename) {
        ArrayList<AnnotHilite> chapAnnotHilites = new ArrayList<AnnotHilite>();
        synchronized (mAnnotHiliteList) {
            for (AnnotHilite ah : mAnnotHiliteList) {
                if (ah.bookFilename.equalsIgnoreCase(bookFilename)
                        && ah.chapterFilename.equalsIgnoreCase(chapterFilename)) {
                    chapAnnotHilites.add(ah);
                }
            }
        }
        return chapAnnotHilites;
    }

    /**
     * Gets a read-only copy of the folder map.
     * 
     * @return the folder map
     */
    public SortedMap<String, SortedSet<String>> getFolderMap() {
        return Collections.unmodifiableSortedMap(mFolderMap);
    }

    /**
     * Move a book to a folder
     * 
     * @param folder the folder name
     * @param book
     */
    public void moveBookToFolder(String folder, String book) {
        synchronized (mFolderMap) {
            // remove from any other folder it might be in
            for (SortedSet<String> set : mFolderMap.values()) {
                set.remove(book);
            }
            // add to folder map if folder is non-null
            if (folder != null) {
                SortedSet<String> set = mFolderMap.get(folder);
                if (set == null) {
                    set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                    mFolderMap.put(folder, set);
                }
                set.add(book);
            }
            storeFolderMap();
        }
    }

    /**
     * Add a folder.
     * 
     * @param folder the folder name
     */
    public void addFolder(String folder) {
        synchronized (mFolderMap) {
            if (!mFolderMap.containsKey(folder)) {
                SortedSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                mFolderMap.put(folder, set);
                storeFolderMap();
            }
        }
    }

    /**
     * Remove a folder.
     * 
     * @param folder the folder name
     */
    public void removeFolder(String folder) {
        synchronized (mFolderMap) {
            if (mFolderMap.remove(folder) != null) {
                storeFolderMap();
            }
        }
    }

    /**
     * Remove a folder.
     * 
     * @param folder the folder name
     * @param newFolder the new folder name
     */
    public void renameFolder(String folder, String newFolder) {
        synchronized (mFolderMap) {
            if (!folder.equals(newFolder)) {
                SortedSet<String> folderBooks = mFolderMap.remove(folder);
                addFolder(newFolder);
                if (folderBooks != null) {
                    for (String book : folderBooks) {
                        moveBookToFolder(newFolder, book);
                    }
                }
                storeFolderMap();
            }
        }
    }

    /**
     * Get the folder that a book is in.
     * 
     * @param book the book name
     * @return the folder the book is in, or empty string if not in a folder
     */
    public String getBookFolder(String book) {
        synchronized (mFolderMap) {
            for (Map.Entry<String, SortedSet<String>> entry : mFolderMap.entrySet()) {
                if (entry.getValue().contains(book)) {
                    return entry.getKey();
                }
            }
        }
        return "";
    }

}
