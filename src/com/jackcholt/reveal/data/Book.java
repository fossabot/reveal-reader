/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;

import android.text.Html;

import jdbm.RecordManager;

/**
 * This class defines objects which store data about books.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class Book extends JDBMObject implements Serializable {
    private static final long serialVersionUID = -8714483712431825041L;

    public long id;
    public String fileName;
    public String bindingText;
    public String title;
    public String formattedTitle;
    public String shortTitle;
    public String metaData;
    public boolean active;

    public String toString() {
        return formattedTitle.contains("&#") ? Html.fromHtml(formattedTitle).toString() : formattedTitle;
    }

    /**
     * Load a book from db by recID
     * 
     * @param db
     * @param recID
     * @return the book
     * @throws IOException
     */
    protected static Book load(RecordManager db, long recID) throws IOException {
        return (Book) JDBMObject.load(db, recID);
    }

}
