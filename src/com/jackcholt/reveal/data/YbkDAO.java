package com.jackcholt.reveal.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.garret.perst.FieldIndex;
import org.garret.perst.GenericIndex;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;

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
 * 
 */
public class YbkDAO {
    private static String DATABASE_NAME = "reveal_ybk.pdb";

    private static final int PAGE_POOL_SIZE = 512 * 1024;

    private Storage mDb;

    //private static volatile YbkDAO mSelf = null;

    // Add the volatile modifier to make sure that changes to mCtx are atomic 
    private static volatile Context mCtx;

    private SharedPreferences mSharedPref;

    private static final String TAG = "YbkDAO";

    public static final String ID = "id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    /**
     * Is the chapter a navigation chapter? Data type: INTEGER. Use
     * {@link CHAPTER_TYPE_NO_NAV} and {@link CHAPTER_TYPE_NAV} to set values.
     */
    public static final String CHAPTER_NAV_FILE = "nav_file";

    /**
     * Should the user be able to zoom the page? Data type: INTEGER. Used when
     * the chapter contains a picture. Use {@link CHAPTER_ZOOM_MENU_OFF} and
     * {@link CHAPTER_ZOOM_MENU_ON} to set values.
     */
    public static final String CHAPTER_ZOOM_PICTURE = "zoom_picture";

    /**
     * Allow the user to get the one and only instance of the YbkDAO.
     * 
     * @param ctx
     *            The Android instance.
     * @return The YbkDAO singleton.
     */
    public static YbkDAO getInstance(final Context ctx) {
        mCtx = ctx;
        return YbkDaoHolder.instance;
    }

    private static class YbkDaoHolder {
        static private YbkDAO instance = new YbkDAO(mCtx);
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
     * @param ctx
     *            The Android context.
     */
    private YbkDAO(final Context ctx) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                Settings.DEFAULT_EBOOK_DIRECTORY);

        mDb = StorageFactory.getInstance().createStorage();
        mDb.open(libDir + DATABASE_NAME, PAGE_POOL_SIZE);
        Log.d(TAG, "Opened the database");
    }

    /**
     * Get the root object of the database.
     * 
     * @param db
     *            The database for which we want the root object.
     * @return The root object.
     */
    private YbkRoot getRoot(final Storage db) {
        YbkRoot root = (YbkRoot) db.getRoot();

        if (root == null) {
            root = new YbkRoot(db);
            db.setRoot(root);
        }

        return root;
    }

    /**
     * Get a list of book titles.
     * 
     * @return The list of book titles as a field index.
     */
    public FieldIndex<Book> getBookTitles() {
        return getRoot(mDb).bookTitleIndex;
    }

    /**
     * Get an Iterator for history records that are bookmarks sorted by title.
     * 
     * @return
     */
    public Iterator<History> getBookmarkIterator() {
        Iterator<History> iter = getRoot(mDb).historyTitleIndex.iterator();
        List<History> bmList = new ArrayList<History>();

        while (iter.hasNext()) {
            History hist = iter.next();
            if (hist.bookmarkNumber != 0) {
                bmList.add(hist);
            }
        }

        return bmList.iterator();
    }

    /**
     * Insert a book into the database.
     * 
     * @param fileName
     *            The name of the file that contains the book.
     * @return The record id of the book.
     */
    /*
     * public long insertBook(final String fileName) { YbkFileReader ybkRdr;
     * 
     * long recordId = 0;
     * 
     * try { ybkRdr = new YbkFileReader(fileName); String bindingText =
     * ybkRdr.getBindingText();
     * 
     * recordId = insertBook(fileName, bindingText,
     * Util.getBookTitleFromBindingText(bindingText),
     * Util.getBookShortTitleFromBindingText(bindingText),
     * ybkRdr.getBookMetaData());
     * 
     * } catch (FileNotFoundException e) { Log.e(TAG, "No such file exists. " +
     * fileName + " was not inserted into the library. " + e.getMessage()); }
     * catch (IOException e) { Log.e(TAG, "Miscellaneous I/O error. " + fileName
     * + " was not inserted into the library. " + e.getMessage()); }
     * 
     * return recordId; }
     */

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
     */
    public long insertBook(final String fileName, final String bindingText,
            final String title, final String shortTitle, final String metaData) {

        long id = System.currentTimeMillis();

        Book book = new Book();
        book.id = id;
        book.fileName = fileName.toLowerCase();
        book.active = true;
        book.bindingText = bindingText;
        book.formattedTitle = Util.formatTitle(title);
        book.title = title;
        book.shortTitle = shortTitle;
        book.metaData = metaData;

        YbkRoot root = getRoot(mDb);

        // Persistence-by-reachability causes objects to become persistent once
        // they are referred to by a persistent object.
        if (root.bookIdIndex.put(book) && root.bookTitleIndex.put(book)
                && root.bookFilenameIndex.put(book)) {

            mDb.commit();
            return id;

        } else {

            // one of the puts failed
            mDb.rollback();
            Log.e(TAG, "Could not insert " + fileName
                    + ". It already exists in the database.");
            return 0;
        }

    }

    public boolean insertChapter(final long bookId, final String fileName,
            final String historyTitle, final Integer length,
            final String navbarTitle, final Integer navFile, final int offset,
            final String orderName, final Integer orderNumber,
            final Integer zoomPicture) {

        long id = System.currentTimeMillis();

        Chapter chap = new Chapter();
        chap.id = id;
        chap.bookId = bookId;
        chap.fileName = fileName;
        chap.historyTitle = historyTitle;
        chap.length = length;
        chap.navbarTitle = navbarTitle;
        if (navFile != null)
            chap.navFile = navFile;
        chap.offset = offset;
        chap.orderName = orderName;
        if (orderNumber != null)
            chap.orderNumber = orderNumber;
        if (zoomPicture != null)
            chap.zoomPicture = zoomPicture;

        YbkRoot root = getRoot(mDb);

        // Persistence-by-reachability causes objects to become persistent once
        // they are referred to by a persistent object.
        boolean b1 = root.chapterBookIdIndex.put(chap);
        boolean b2 = root.chapterIdIndex.put(chap);
        boolean b3 = root.chapterNameIndex.put(chap);
        boolean b4 = true;
        if (chap.orderNumber != 0) {
            b4 = root.chapterOrderNbrIndex.put(chap);
        }

        return (b1 && b2 && b3 && b4);

    }

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
     * @return
     */
    public boolean insertHistory(final long bookId, final String title,
            final String chapterName, final int scrollYPos) {

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
    public boolean insertHistory(final long bookId, final String title,
            final String chapterName, final int scrollYPos,
            final int bookmarkNumber) {
        boolean success = true;

        History hist = new History();
        hist.bookId = bookId;
        hist.bookmarkNumber = bookmarkNumber;
        hist.chapterName = chapterName;
        hist.scrollYPos = scrollYPos;
        hist.title = title;

        YbkRoot root = getRoot(mDb);

        // Persistence-by-reachability causes objects to become persistent once
        // they are referred to by a persistent object.
        boolean b1 = root.historyIdIndex.put(hist);
        boolean b2 = root.historyTitleIndex.put(hist);
        boolean b3 = true;

        if (bookmarkNumber != 0) {
            b3 = root.historyBookmarkNumberIndex.put(hist);
        }

        if (b1 && b2 && b3) {
            mDb.commit();
        } else {
            mDb.rollback();
            success = false;
        }

        return success;
    }

    /**
     * Remove all histories that have a timestamp earlier than the milliseconds
     * passed in.
     */
    public void deleteHistories() {
        SharedPreferences sharedPref = mSharedPref;
        int histToKeep = sharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY,
                Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);

        List<History> historyList = getHistoryList();
        if (historyList.size() > histToKeep) {
            List<History> delHistList = historyList.subList(histToKeep,
                    historyList.size());
            YbkRoot root = getRoot(mDb);

            for (int i = 0; i < delHistList.size(); i++) {
                History hist = delHistList.get(i);
                root.historyIdIndex.removeKey(hist);
                root.historyTitleIndex.removeKey(hist);
                hist.deallocate();
            }
            root.modify();
            mDb.commit();
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
        Book book = getRoot(mDb).bookFilenameIndex.get(new Key(fileName));

        return deleteBook(book);
    }

    /**
     * Remove the book from the database.
     * 
     * @param book
     *            The book to be deleted.
     * @return True if the book was deleted
     */
    public boolean deleteBook(final Book book) {
        YbkRoot root = getRoot(mDb);

        if (root.bookFilenameIndex.remove(book)
                && root.bookIdIndex.remove(book)
                && root.bookTitleIndex.remove(book)) {
            book.deallocate();
            root.modify();
            mDb.commit();
            return true;
        } else {
            mDb.rollback();
            return false;
        }

    }

    /**
     * Remove the book from the database.
     * 
     * @param book
     *            The book to be deleted.
     * @return True if the book was deleted
     */
    public boolean deleteChapters(final long bookId) {
        YbkRoot root = getRoot(mDb);
        boolean success = true;

        Chapter[] chapters = (Chapter[]) root.chapterBookIdIndex.get(new Key(
                bookId), null);

        for (int i = 0, size = chapters.length; i < size; i++) {
            if (root.chapterBookIdIndex.remove(chapters[i])
                    && root.chapterIdIndex.remove(chapters[i])
                    && root.chapterNameIndex.remove(chapters[i])
                    && root.chapterOrderNbrIndex.remove(chapters[i])) {
                // do nothing
            } else {
                success = false;
                break;
            }
        }

        if (success) {
            for (int i = 0, size = chapters.length; i < size; i++) {
                chapters[i].deallocate();
            }
            root.modify();
            mDb.commit();
        } else {
            mDb.rollback();
        }

        return success;
    }

    /**
     * Change the book from active to inactive or vice versa.
     * 
     * @param book
     *            The book to change the active state of.
     * @return True if the Book is already in the database indexes and the
     *         update occurred successfully.
     */
    public boolean toggleBookActivity(final Book book) {
        YbkRoot root = getRoot(mDb);

        book.active = !book.active;

        boolean result = (null == root.bookFilenameIndex.set(book)
                || null == root.bookIdIndex.set(book) || null == root.bookTitleIndex
                .set(book));

        root.modify();
        mDb.commit();

        return result;

    }

    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId
     *            The key of the book to get.
     * @return The book object identified by bookId.
     */
    public Book getBook(final long bookId) {
        return getRoot(mDb).bookIdIndex.get(new Key(bookId));
    }

    /**
     * Get the history item identified by histId.
     * 
     * @param histId
     *            The key of the history to get.
     * @return The history object identified by histId.
     */
    public History getHistory(final long histId) {
        // History hist = new History();
        // hist.id = histId;
        return getRoot(mDb).historyIdIndex.get(new Key(histId));
    }

    /**
     * Get a bookmark by bookmarkNumber.
     * 
     * @param bmId
     *            the bookmark id.
     * @return The History object that contains the bookmark.
     */
    public History getBookmark(final int bmId) {
        return getRoot(mDb).historyBookmarkNumberIndex.get(new Key(bmId));
    }

    /**
     * Get a list if Histories sorted from newest to oldest for use with
     * ArrayAdapter for showing histories in a ListActivity.
     * 
     * @return the List of History objects.
     */
    public List<History> getHistoryList() {
        IterableIterator<History> iter = getRoot(mDb).historyIdIndex.iterator(
                null, null, GenericIndex.DESCENT_ORDER);

        List<History> histList = new ArrayList<History>();

        int maxHistories = mSharedPref.getInt(
                Settings.HISTORY_ENTRY_AMOUNT_KEY,
                Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);

        int histCount = 0;
        while (iter.hasNext() && histCount < maxHistories) {
            History hist = iter.next();
            if (hist.bookmarkNumber == 0) {
                histList.add(hist);
                histCount++;
            }
        }

        return histList;
    }

    public List<History> getBookmarkList() {
        FieldIndex<History> historyBookmarkNumberIndex = getRoot(mDb).historyBookmarkNumberIndex;
        History[] array = historyBookmarkNumberIndex
                .toArray(new History[historyBookmarkNumberIndex.size()]);

        return Arrays.asList(array);

    }

    /**
     * Get the last bookmark in the list.
     * 
     * @return The highest numbered bookmark.
     */
    public int getMaxBookmarkNumber() {
        int bmNbr = 1;
        FieldIndex<History> bmNbrIndex = getRoot(mDb).historyBookmarkNumberIndex;

        int indexSize = bmNbrIndex.size();
        if (indexSize > 0) {
            History hist = bmNbrIndex.getAt(indexSize - 1);
            bmNbr = hist.bookmarkNumber + 1;
        }

        return bmNbr;
    }

    /**
     * Get a book object identified by the fileName.
     * 
     * @param fileName
     *            the filename we're looking for.
     * @return A Book object identified by the passed in filename.
     */
    public Book getBook(final String fileName) {
        return getRoot(mDb).bookFilenameIndex.get(new Key(fileName
                .toLowerCase()));
    }

    /**
     * Get a chapter object identified by the fileName.
     * 
     * @param fileName
     *            the name of the internal chapter file.
     * @return the chapter
     */
    public Chapter getChapter(final long bookId, final String fileName) {
        return getRoot(mDb).chapterNameIndex.get(new Key(new Object[] { bookId,
                fileName.toLowerCase() }));
    }

    /**
     * Get chapter by book id and order id.
     * 
     * @param bookId
     *            The id of the book that contains the chapter.
     * @param orderId
     *            The number of the chapter in the order of chapters.
     * @return The chapter we're look for.
     */
    public Chapter getChapter(final long bookId, final int orderId) {
        return getRoot(mDb).chapterOrderNbrIndex.get(new Key(new Object[] {
                bookId, orderId }));
    }

    /**
     * clean up the object.
     */
    public void finalize() throws Throwable {
        try {
            if (mDb.isOpened()) {
                mDb.close();
                Log.d(TAG, "Closed the database in finalize()");
            }
        } finally {
            super.finalize();
        }

    }

    public void commit() {
        mDb.commit();
    }

    public void rollback() {
        mDb.rollback();
    }

    /**
     * Get the history at the current position in the list of histories that the
     * back button has taken us to.
     * 
     * @param historyPos
     *            The position in the history list that we're getting the
     *            history at.
     */
    public History getPreviousHistory(int historyPos) {
        History hist = null;

        if (historyPos >= 0) {
            List<History> historyList = getHistoryList();
            if (historyList.size() - 1 >= historyPos) {
                hist = historyList.get(historyPos);
            }
        }
        return hist;
    }
}
