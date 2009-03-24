package com.jackcholt.reveal.data;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.garret.perst.FieldIndex;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;

import android.content.Context;
import android.util.Log;

import com.jackcholt.reveal.Util;
import com.jackcholt.reveal.YbkFileReader;

/**
 * A class for managing all the database accesses for the Perst OODB.
 * 
 * @author Jack C. Holt
 *
 */
public class YbkDAO {
    public static String DATABASE_NAME = "reveal_ybk.pdb";
    private static final int PAGE_POOL_SIZE = 2*1024*1024;
    private Storage mDb;
    private static final String TAG = "YbkDAO";
    public static final String ID = "id";
    
    public YbkDAO(final Context ctx, final String libDir) {
        mDb = StorageFactory.getInstance().createStorage();
        mDb.open(libDir + DATABASE_NAME, PAGE_POOL_SIZE);
        
    }
    
    /**
     * Get the root object of the database.
     * 
     * @param db The database for which we want the root object.
     * @return The root object.
     */
    private YbkRoot getRoot(final Storage db) {
        YbkRoot root = (YbkRoot)db.getRoot();
        
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
     * Insert a book into the database. 
     * @param fileName The name of the file that contains the book.
     * @return The record id of the book.
     */
    public long insertBook(final String fileName) {
        YbkFileReader ybkRdr;
        
        long recordId = 0;
        
        try {
            ybkRdr = new YbkFileReader(fileName);
            String bindingText = ybkRdr.getBindingText();
            
            recordId = insertBook(fileName, bindingText, 
                    Util.getBookTitleFromBindingText(bindingText), 
                    Util.getBookShortTitleFromBindingText(bindingText), 
                    ybkRdr.getBookMetaData());

        } catch (FileNotFoundException e) {
            Log.e(TAG, "No such file exists. " + fileName + 
                    " was not inserted into the library. " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Miscellaneous I/O error. " + fileName + 
                    " was not inserted into the library. " + e.getMessage());
        }
        
        return recordId;
    }
    
    /**
     * Insert a book into the database.
     *  
     * @param fileName The name of the file that contains the book.
     * @param bindingText The text that describes the book.
     * @param title The title of the book.
     * @param shortTitle the abbreviated title.
     * @param metaData Descriptive information. 
     */
    public long insertBook(final String fileName, final String bindingText, 
            final String title, final String shortTitle, final String metaData) {
        
        long id = System.currentTimeMillis();
        
        Book book = new Book();
        book.id = id;
        book.fileName = fileName;
        book.active = true;
        book.bindingText = bindingText;
        book.formattedTitle = Util.formatTitle(title);
        book.title = title;
        book.shortTitle = shortTitle;
        book.metaData = metaData;
        
        YbkRoot root = getRoot(mDb);
        
        // Persistence-by-reachability causes objects to become persistent once
        // they are referred to by a persistent object.
        if (root.bookIdIndex.put(book)
                && root.bookTitleIndex.put(book)
                && root.bookFilenameIndex.put(book)) {
            
            mDb.commit();
            return id;
            
        } else {
        
            // one of the puts failed
            mDb.rollback();
            Log.e(TAG, "Could not insert " + fileName + ". It already exists in the database.");
            return 0;
        }
        
    }
    
    /**
     * Remove the book from the database.
     * 
     * @param book The book to be deleted.
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
     * Change the book from active to inactive or vice versa.
     * 
     * @param book The book to change the active state of.
     * @return True if the Book is already in the database indexes and the update
     * occurred successfully. 
     */
    public boolean toggleBookActivity(final Book book) {
        YbkRoot root = getRoot(mDb);
        
        book.active = !book.active;
        
        boolean result = (null == root.bookFilenameIndex.set(book) 
                || null == root.bookIdIndex.set(book)
                || null == root.bookTitleIndex.set(book));
            
        root.modify();
        mDb.commit();
        
        return result;
        
    }
    
    /**
     * Get the book object identified by bookId.
     * 
     * @param bookId The key of the book to get.
     * @return The book object identified by bookId.
     */
    public Book getBook(long bookId) {
        return getRoot(mDb).bookIdIndex.get(new Key(bookId));
    }
    
    /**
     * clean up the object.
     */
    public void finalize() throws Throwable {
        try {
            mDb.close();
        } finally {
            super.finalize();
        }
        
    }
}
