/**
 * 
 */
package com.jackcholt.reveal.data;

import org.garret.perst.FieldIndex;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;

/**
 * This class acts as the Root object of a Perst OODB.
 * 
 * @author Jack C. Holt
 *
 */
public class YbkRoot extends Persistent {
    // Index for the Book object on its primary field
    public FieldIndex<Book> bookIdIndex;
    // Index for the Book object on the title field
    public FieldIndex<Book> bookTitleIndex;
    // Index for the Book object on the fileName field
    public FieldIndex<Book> bookFilenameIndex;    
    // Index for the Chapter object on its primary field
    public FieldIndex<Chapter> chapterIdIndex;
    // Index for the Chapter object on the bookId field
    public FieldIndex<Chapter> chapterBookIdIndex;
    // Index for the Chapter object on the bookId field
    public FieldIndex<Chapter> chapterOrderNbrIndex;
    public FieldIndex<Chapter> chapterNameIndex;
    // Index for the History object on its primary field
    public FieldIndex<History> historyIdIndex;
    public FieldIndex<History> historyBookmarkNumberIndex;
    public FieldIndex<History> historyTitleIndex;
    
    
    public YbkRoot(final Storage db) {
        super(db);
        
        bookIdIndex = db.<Book>createFieldIndex(Book.class, "id", true);
        bookTitleIndex = db.<Book>createFieldIndex(Book.class, "formattedTitle", true, true);
        bookFilenameIndex = db.<Book>createFieldIndex(Book.class, "fileName", true, true);
        chapterIdIndex = db.<Chapter>createFieldIndex(Chapter.class, "id", true);
        chapterBookIdIndex = db.<Chapter>createFieldIndex(Chapter.class, "bookId", false);
        chapterOrderNbrIndex = db.<Chapter>createFieldIndex(Chapter.class, 
                new String[] {"bookId", "orderNumber"}, false); 
        chapterNameIndex = db.<Chapter>createFieldIndex(Chapter.class, 
                new String[] {"bookId","fileName"}, true, true);
        historyIdIndex = db.<History>createFieldIndex(History.class, "id", true);
        historyBookmarkNumberIndex = db.<History>createFieldIndex(History.class, 
                "bookmarkNumber", true);
        historyTitleIndex = db.<History>createFieldIndex(History.class, "title", false, true);
    }
    

    public YbkRoot() {}
    
}
