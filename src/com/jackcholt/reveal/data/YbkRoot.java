/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;

import jdbm.RecordManager;
import jdbm.helper.LongComparator;
import jdbm.helper.StringComparator;

/**
 * This class acts as the Root object of a Perst OODB.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
class YbkRoot extends JDBMObject implements Serializable {
    private static final long serialVersionUID = 6467674082445522784L;

    // Index for the Book object on its primary field
    public transient Index<Long, Book> bookIdIndex;
    public Long bookIdIndexID;
    // Index for the Book object on the title field
    public transient Index<String, Book> bookTitleIndex;
    public Long bookTitleIndexID;
    // Index for the Book object on the fileName field
    public transient Index<String, Book> bookFilenameIndex;
    public Long bookFilenameIndexID;
    // Index for the Chapter object on the order number and title fields
    public transient Index<String, Chapter> chapterOrderNbrIndex;
    public Long chapterOrderNbrIndexID;
    public transient Index<String, Chapter> chapterNameIndex;
    public Long chapterNameIndexID;
    // Index for the History object on its primary field
    public transient Index<Long, History> historyIdIndex;
    public Long historyIdIndexID;
    public transient Index<Long, History> historyBookmarkNumberIndex;
    public Long historyBookmarkNumberIndexID;
    public transient Index<String, History> historyTitleIndex;
    public Long historyTitleIndexID;

    /**
     * Creates a new root object and all of it's associated indices
     * 
     * @param db
     * @throws IOException
     */
    private YbkRoot(final RecordManager db) throws IOException {
        super();
        boolean done = false;
        try {
            LongComparator longComparator = new LongComparator();
            StringComparator stringComparator = new StringComparator();

            // create indexes
            bookIdIndex = new Index<Long, Book>(db, longComparator);
            bookTitleIndex = new Index<String, Book>(db, stringComparator);
            bookFilenameIndex = new Index<String, Book>(db, stringComparator);
            chapterOrderNbrIndex = new Index<String, Chapter>(db, stringComparator);
            chapterNameIndex = new Index<String, Chapter>(db, stringComparator);
            historyIdIndex = new Index<Long, History>(db, longComparator);
            historyBookmarkNumberIndex = new Index<Long, History>(db, longComparator);
            historyTitleIndex = new Index<String, History>(db, stringComparator);

            // and remember the record id's
            bookIdIndexID = bookIdIndex.getRecid();
            bookTitleIndexID = bookTitleIndex.getRecid();
            bookFilenameIndexID = bookFilenameIndex.getRecid();
            chapterOrderNbrIndexID = chapterOrderNbrIndex.getRecid();
            chapterNameIndexID = chapterNameIndex.getRecid();
            historyIdIndexID = historyIdIndex.getRecid();
            historyBookmarkNumberIndexID = historyBookmarkNumberIndex.getRecid();
            historyTitleIndexID = historyTitleIndex.getRecid();

            // and store this object and give it a name
            recID = db.insert(this);
            db.setNamedObject(YbkRoot.class.getName(), recID);
            done = true;
        } finally {
            if (done)
                db.commit();
            else
                db.rollback();
        }
    }

    /**
     * No-argument constructor so for deserialization.
     * 
     */
    public YbkRoot() {
    }

    /**
     * Load or create the root object
     * 
     * @param db
     * @return the root object
     * @throws IOException
     */
    protected static YbkRoot load(RecordManager db) throws IOException {
        long recID = db.getNamedObject(YbkRoot.class.getName());
        if (recID == 0) {
            return new YbkRoot(db);
        } else {
            return (YbkRoot) load(db, recID);
        }
    }

    /**
     * Load or create the root object
     * 
     * @param db
     * @return the root object
     * @throws IOException
     */
    protected static YbkRoot load(RecordManager db, long recID) throws IOException {
        YbkRoot root = (YbkRoot) JDBMObject.load(db, recID);
        root.loadIndexes(db);
        return root;
    }

    /**
     * Load the indexes
     * 
     * @param db
     * @throws IOException
     */
    private void loadIndexes(RecordManager db) throws IOException {
        bookIdIndex = new Index<Long, Book>(db, bookIdIndexID);
        bookTitleIndex = new Index<String, Book>(db, bookTitleIndexID);
        bookFilenameIndex = new Index<String, Book>(db, bookFilenameIndexID);
        chapterOrderNbrIndex = new Index<String, Chapter>(db, chapterOrderNbrIndexID);
        chapterNameIndex = new Index<String, Chapter>(db, chapterNameIndexID);
        historyIdIndex = new Index<Long, History>(db, historyIdIndexID);
        historyBookmarkNumberIndex = new Index<Long, History>(db, historyBookmarkNumberIndexID);
        historyTitleIndex = new Index<String, History>(db, historyTitleIndexID);
    }

}
