package com.jackcholt.reveal.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.garret.perst.FieldIndex;
import org.garret.perst.GenericIndex;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Storage;
import org.garret.perst.StorageError;
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

    // private static volatile YbkDAO mSelf = null;

    private SharedPreferences mSharedPref;

    private static final String TAG = "YbkDAO";

    public static final String ID = "id";

    public static final int GET_LAST_HISTORY = 0;

    public static final String FROM_HISTORY = "from history";

    public static final String BOOKMARK_NUMBER = "bookmarkNumber";

    public static final boolean useSerializableTransactions = false;

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

    private static YbkDAO instance = null;

    /**
     * Allow the user to get the one and only instance of the YbkDAO.
     * 
     * @return The YbkDAO singleton.
     * @throws StorageException
     */
    public static YbkDAO getInstance(final Context ctx) throws StorageException {
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
     * @throws StorageException
     */
    private YbkDAO(final Context ctx) throws StorageException {
        try {
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
            String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

            mDb = StorageFactory.getInstance().createStorage();
            if (useSerializableTransactions) {
                // have to use alternate btree implementation for serializable
                // transactions to work
                // transactions to work correctly
                mDb.setProperty("perst.alternative.btree", Boolean.TRUE);
            }
            mDb.open(libDir + DATABASE_NAME, PAGE_POOL_SIZE);
            Log.d(TAG, "Opened the database");
        } catch (StorageError se) {
            throw new StorageException(se);
        }
    }

    /**
     * Get the root object of the database.
     * 
     * @param db
     *            The database for which we want the root object.
     * @return The root object.
     * @throws StorageException
     */
    private YbkRoot getRoot(final Storage db) throws StorageException {
        try {
            YbkRoot root = (YbkRoot) db.getRoot();

            if (root == null) {
                startTransaction();
                boolean done = false;
                try {
                    root = new YbkRoot(db);
                    db.setRoot(root);
                    done = true;
                } finally {
                    endTransaction(done);
                }
            }
            return root;
        } catch (StorageError se) {
            throw new StorageException(se);
        }
    }

    /**
     * Get a list of book titles.
     * 
     * @return The list of book titles as a field index or null if couldn't get
     *         list.
     * @throws StorageException
     */
    public List<Book> getBookTitles() throws StorageException {
        FieldIndex<Book> bookTitleIndex = getRoot(mDb).bookTitleIndex;
        if (useSerializableTransactions)
            bookTitleIndex.sharedLock();
        try {
            return new ArrayList<Book>(bookTitleIndex);
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                bookTitleIndex.unlock();
        }
    }

    /**
     * Get a list of books.
     * 
     * @return The list of book titles as a field index or null if couldn't get
     *         list.
     * @throws StorageException
     */
    public List<Book> getBooks() throws StorageException {
        FieldIndex<Book> bookIdIndex = getRoot(mDb).bookIdIndex;
        if (useSerializableTransactions)
            bookIdIndex.sharedLock();
        try {
            return new ArrayList<Book>(bookIdIndex);
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                bookIdIndex.unlock();
        }
    }

    /**
     * Get a List for history records that are bookmarks sorted by title.
     * 
     * @return
     * @throws StorageException
     */
    public List<History> getBookmarks() throws StorageException {
        FieldIndex<History> historyTitleIndex = getRoot(mDb).historyTitleIndex;
        if (useSerializableTransactions)
            historyTitleIndex.sharedLock();
        try {
            return new ArrayList<History>(historyTitleIndex);
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                historyTitleIndex.unlock();
        }
    }

    /**
     * Get an Iterator for history records that are bookmarks sorted by title.
     * 
     * @return
     * @throws StorageException
     */
    public Iterator<History> getBookmarkIterator() throws StorageException {
        return getBookmarks().iterator();
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
     * @throws StorageException
     */
    public long insertBook(final String fileName, final String bindingText, final String title,
            final String shortTitle, final String metaData, final List<Chapter> chapters) throws StorageException {

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

        YbkRoot root = getRoot(mDb);
        startTransaction();
        boolean success = false;
        try {
            if (useSerializableTransactions) {
                root.bookIdIndex.exclusiveLock();
                root.bookFilenameIndex.exclusiveLock();
                root.bookTitleIndex.exclusiveLock();
            }
            // Persistence-by-reachability causes objects to become persistent
            // once
            // they are referred to by a persistent object.
            success = root.bookIdIndex.put(book) && root.bookFilenameIndex.put(book)
                    && (book.formattedTitle == null || root.bookTitleIndex.put(book))
                    && insertChapters(root, id, chapters);
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            endTransaction(success);
            if (!success) {
                Log.e(TAG, "Could not insert " + fileName);
                id = 0;
            }
        }
        return id;

    }

    /**
     * Insert a list of chapters (must already be within a transaction)
     * 
     * @param chapters
     *            the list of chapters
     * @return true if successful
     * @throws StorageException
     */
    private boolean insertChapters(YbkRoot root, long bookId, List<Chapter> chapters) {
        if (useSerializableTransactions) {
            root.chapterBookIdIndex.exclusiveLock();
            root.chapterIdIndex.exclusiveLock();
            root.chapterNameIndex.exclusiveLock();
            root.chapterOrderNbrIndex.exclusiveLock();
        }

        int i = 0;
        for (Chapter chap : chapters) {
            chap.bookId = bookId;
            boolean success = root.chapterBookIdIndex.put(chap) && root.chapterIdIndex.put(chap)
                    && root.chapterNameIndex.put(chap)
                    && (chap.orderNumber == 0 || root.chapterOrderNbrIndex.put(chap));
            i++;
            if (!success) {
                Log.e(TAG, "Could not insert chapter (" + i + "): " + chap);
                return false;
            }
        }
        return true;
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
     * @throws StorageException
     */
    public boolean insertHistory(final long bookId, final String title, final String chapterName, final int scrollYPos)
            throws StorageException {

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
     * @throws StorageException
     */
    public boolean insertHistory(final long bookId, final String title, final String chapterName, final int scrollYPos,
            final int bookmarkNumber) throws StorageException {
        boolean success;

        History hist = new History();
        hist.bookId = bookId;
        hist.bookmarkNumber = bookmarkNumber;
        hist.chapterName = chapterName;
        hist.scrollYPos = scrollYPos;
        hist.title = title;

        YbkRoot root = getRoot(mDb);

        // Persistence-by-reachability causes objects to become persistent once
        // they are referred to by a persistent object.
        startTransaction();
        boolean b1 = false;
        boolean b2 = false;
        boolean b3 = true;
        try {
            b1 = root.historyIdIndex.put(hist);
            b2 = root.historyTitleIndex.put(hist);
            b3 = true;

            if (bookmarkNumber != 0) {
                b3 = root.historyBookmarkNumberIndex.put(hist);
            }
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            success = b1 && b2 && b3;
            endTransaction(success);
        }
        return success;
    }

    /**
     * Remove all histories that have a timestamp earlier than the milliseconds
     * passed in.
     * 
     * @throws StorageException
     */
    public void deleteHistories() throws StorageException {
        SharedPreferences sharedPref = mSharedPref;
        int histToKeep = sharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY, Settings.DEFAULT_HISTORY_ENTRY_AMOUNT);

        List<History> historyList = getHistoryList();
        if (historyList.size() > histToKeep) {
            List<History> delHistList = historyList.subList(histToKeep, historyList.size());
            YbkRoot root = getRoot(mDb);

            if (delHistList.size() != 0) {
                startTransaction();
                try {
                    if (useSerializableTransactions) {
                        root.historyIdIndex.exclusiveLock();
                        root.historyTitleIndex.exclusiveLock();
                    }
                    for (int i = 0; i < delHistList.size(); i++) {
                        History hist = delHistList.get(i);
                        root.historyIdIndex.removeKey(hist);
                        root.historyTitleIndex.removeKey(hist);
                        hist.deallocate();
                    }
                } catch (StorageError se) {
                    throw new StorageException(se);
                } finally {
                    // why would we need this?
                    // root.modify();
                    endTransaction(true);
                }
            }
        }
    }

    /**
     * Delete book from db based on filename.
     * 
     * @param fileName
     *            The absolute file path of the book.
     * @return True if the book was deleted.
     * @throws StorageException
     */
    public boolean deleteBook(final String fileName) throws StorageException {
        Book book = getRoot(mDb).bookFilenameIndex.get(new Key(fileName));
        return book != null ? deleteBook(book) : null;
    }

    /**
     * Remove the book from the database.
     * 
     * @param book
     *            The book to be deleted.
     * @return True if the book was deleted.
     * @throws StorageException
     */
    public boolean deleteBook(final Book book) throws StorageException {
        YbkRoot root = getRoot(mDb);

        startTransaction();
        boolean done = false;
        try {
            if (useSerializableTransactions) {
                root.historyIdIndex.exclusiveLock();
                root.historyTitleIndex.exclusiveLock();
            }
            done = root.bookFilenameIndex.remove(book) && root.bookIdIndex.remove(book)
                    && (book.title == null || root.bookTitleIndex.remove(book)) && deleteChapters(book.id);
            book.deallocate();
            // why?
            // root.modify();
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            endTransaction(done);
        }
        return done;
    }

    /**
     * Remove a book's chapters from the database. This is private because it is
     * assumed to be within a transaction
     * 
     * @param bookId
     *            The id of the book whose chapters are to be deleted.
     * @return True if the book chapters were deleted.
     * @throws StorageException
     */
    private boolean deleteChapters(final long bookId) throws StorageException {
        YbkRoot root = getRoot(mDb);
        boolean success = true;

        if (useSerializableTransactions) {
            root.chapterBookIdIndex.exclusiveLock();
            root.chapterIdIndex.exclusiveLock();
            root.chapterNameIndex.exclusiveLock();
            root.chapterOrderNbrIndex.exclusiveLock();
        }
        Chapter[] chapters = (Chapter[]) root.chapterBookIdIndex.get(new Key(bookId), null);

        for (int i = 0, size = chapters.length; i < size; i++) {
            if (root.chapterBookIdIndex.remove(chapters[i]) && root.chapterIdIndex.remove(chapters[i])
                    && root.chapterNameIndex.remove(chapters[i])
                    && (chapters[i].orderNumber == 0 || root.chapterOrderNbrIndex.remove(chapters[i]))) {
                // do nothing
            } else {
                success = false;
                break;
            }
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
     * @throws StorageException
     */
    public boolean toggleBookActivity(final Book book) throws StorageException {
        boolean done = false;
        startTransaction();
        try {
            book.active ^= true;
            book.modify();
            done = true;
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            endTransaction(done);
        }

        return done;

    }

    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId
     *            The key of the book to get.
     * @return The book object identified by bookId.
     * @throws StorageException
     */
    public Book getBook(final long bookId) throws StorageException {
        FieldIndex<Book> bookIdIndex = getRoot(mDb).bookIdIndex;
        if (useSerializableTransactions)
            bookIdIndex.sharedLock();
        try {
            return bookIdIndex.get(new Key(bookId));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                bookIdIndex.unlock();
        }
    }

    /**
     * Get the history item identified by histId.
     * 
     * @param histId
     *            The key of the history to get.
     * @return The history object identified by histId.
     * @throws StorageException
     */
    public History getHistory(final long histId) throws StorageException {
        FieldIndex<History> historyIdIndex = getRoot(mDb).historyIdIndex;
        if (useSerializableTransactions)
            historyIdIndex.sharedLock();
        try {
            return historyIdIndex.get(new Key(histId));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                historyIdIndex.unlock();
        }
    }

    /**
     * Get a bookmark by bookmarkNumber.
     * 
     * @param bmId
     *            the bookmark id.
     * @return The History object that contains the bookmark.
     * @throws StorageException
     */
    public History getBookmark(final int bmId) throws StorageException {
        FieldIndex<History> historyBookmarkNumberIndex = getRoot(mDb).historyBookmarkNumberIndex;
        if (useSerializableTransactions)
            historyBookmarkNumberIndex.sharedLock();
        try {
            return historyBookmarkNumberIndex.get(new Key(bmId));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                historyBookmarkNumberIndex.unlock();
        }
    }

    /**
     * Get a list if Histories sorted from newest to oldest for use with
     * ArrayAdapter for showing histories in a ListActivity.
     * 
     * @return the List of History objects.
     * @throws StorageException
     */
    public List<History> getHistoryList() throws StorageException {
        FieldIndex<History> historyIdIndex = getRoot(mDb).historyIdIndex;
        if (useSerializableTransactions)
            historyIdIndex.sharedLock();
        try {
            IterableIterator<History> iter = historyIdIndex.iterator(null, null, GenericIndex.DESCENT_ORDER);

            List<History> histList = new ArrayList<History>();

            int maxHistories = mSharedPref.getInt(Settings.HISTORY_ENTRY_AMOUNT_KEY,
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
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                historyIdIndex.unlock();
        }
    }

    /**
     * Get list of bookmarks.
     * 
     * @return
     * @throws StorageException
     */
    public List<History> getBookmarkList() throws StorageException {
        FieldIndex<History> historyBookmarkNumberIndex = getRoot(mDb).historyBookmarkNumberIndex;
        if (useSerializableTransactions)
            historyBookmarkNumberIndex.sharedLock();
        try {
            return new ArrayList<History>(historyBookmarkNumberIndex);
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            historyBookmarkNumberIndex.unlock();
        }
    }

    /**
     * Get the last bookmark in the list.
     * 
     * @return The highest numbered bookmark.
     * @throws StorageException
     */
    public int getMaxBookmarkNumber() throws StorageException {
        int bmNbr = 1;
        FieldIndex<History> historyBookmarkNumberIndex = getRoot(mDb).historyBookmarkNumberIndex;
        if (useSerializableTransactions)
            historyBookmarkNumberIndex.sharedLock();
        try {
            int indexSize = historyBookmarkNumberIndex.size();
            if (indexSize > 0) {
                History hist = historyBookmarkNumberIndex.getAt(indexSize - 1);
                bmNbr = hist.bookmarkNumber + 1;
            }

            return bmNbr;
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                historyBookmarkNumberIndex.unlock();
        }

    }

    /**
     * Get a book object identified by the fileName.
     * 
     * @param fileName
     *            the filename we're looking for.
     * @return A Book object identified by the passed in filename.
     * @throws StorageException
     */
    public Book getBook(final String fileName) throws StorageException {
        FieldIndex<Book> bookFilenameIndex = getRoot(mDb).bookFilenameIndex;
        if (useSerializableTransactions)
            bookFilenameIndex.sharedLock();
        try {
            return bookFilenameIndex.get(new Key(fileName.toLowerCase()));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                bookFilenameIndex.unlock();
        }
    }

    /**
     * Get a chapter object identified by the fileName.
     * 
     * @param fileName
     *            the name of the internal chapter file.
     * @return the chapter
     * @throws StorageException
     */
    public Chapter getChapter(final long bookId, final String fileName) throws StorageException {
        FieldIndex<Chapter> chapterNameIndex = getRoot(mDb).chapterNameIndex;
        chapterNameIndex.sharedLock();
        try {
            return chapterNameIndex.get(new Key(new Object[] { bookId, fileName.toLowerCase() }));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            chapterNameIndex.unlock();
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
     * @throws StorageException
     */
    public Chapter getChapter(final long bookId, final int orderId) throws StorageException {
        FieldIndex<Chapter> chapterOrderNbrIndex = getRoot(mDb).chapterOrderNbrIndex;
        if (useSerializableTransactions)
            chapterOrderNbrIndex.sharedLock();
        try {
            return chapterOrderNbrIndex.get(new Key(new Object[] { bookId, orderId }));
        } catch (StorageError se) {
            throw new StorageException(se);
        } finally {
            if (useSerializableTransactions)
                chapterOrderNbrIndex.unlock();
        }
    }

    /**
     * Check whether a chapter exists in a book.
     * 
     * @param bookId
     *            The id of the book.
     * @param fileName
     *            The name of the chapter (or internal file) that we're checking
     *            for.
     * @return True if the book has a chapter of that name, false otherwise.
     * @throws StorageException
     */
    public boolean chapterExists(final long bookId, final String fileName) throws StorageException {
        return (null != getChapter(bookId, fileName)) || (null != getChapter(bookId, fileName + ".gz"));
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

    public void startTransaction() throws StorageException {
        try {
            mDb.beginThreadTransaction(useSerializableTransactions ? Storage.SERIALIZABLE_TRANSACTION
                    : Storage.EXCLUSIVE_TRANSACTION);
        } catch (StorageError se) {
            throw new StorageException(se);

        }
    }

    public void endTransaction(boolean commit) throws StorageException {
        if (commit) {
            try {
                mDb.endThreadTransaction();
            } catch (StorageError se) {
                throw new StorageException(se);

            }
        } else {
            try {
                mDb.rollbackThreadTransaction();
                if (useSerializableTransactions) {

                    // it appears that the indexes get left in a funny state
                    // after rollback of a serialized thread transaction, so
                    // reload them.
                    YbkRoot root = getRoot(mDb);
                    root.bookFilenameIndex.load();
                    root.bookIdIndex.load();
                    root.bookTitleIndex.load();
                    root.chapterBookIdIndex.load();
                    root.chapterIdIndex.load();
                    root.chapterNameIndex.load();
                    root.chapterOrderNbrIndex.load();
                    root.historyBookmarkNumberIndex.load();
                    root.historyIdIndex.load();
                    root.historyTitleIndex.load();
                }
            } catch (StorageError se) {
                throw new StorageException(se);
            }
        }
    }

    /**
     * Get the history at the current position in the list of histories that the
     * back button has taken us to.
     * 
     * @param historyPos
     *            The position in the history list that we're getting the
     *            history at.
     * @throws StorageException
     */
    public History getPreviousHistory(int historyPos) throws StorageException {
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
