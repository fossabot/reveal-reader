/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;

import jdbm.RecordManager;

/**
 * This class defines objects which store data about book chapters.
 * 
 * @author Jack C. Holt
 * 
 */
public class Chapter extends JDBMObject implements Serializable {
    private static final long serialVersionUID = 3372143287542286011L;

    public long id;
    public long bookId;
    public String fileName;
    public int offset;
    public int length;
    public String orderName = null;
    public int orderNumber = 0;
    public String navbarTitle = null;
    public String historyTitle = null;
    public int navFile = 0;
    public int zoomPicture = 0;

    public String toString() {
        return id + ":" + bookId + ":" + fileName + ":" + offset + ":" + length + ":" + orderName + ":" + orderNumber
                + ":" + navbarTitle + ":" + historyTitle + ":" + navFile + ":" + zoomPicture;
    }

    /**
     * Load a chapter from db by recID
     * 
     * @param db
     * @param recID
     * @return the chapter
     * @throws IOException
     */
    protected static Chapter load(RecordManager db, long recID) throws IOException {
        return (Chapter) JDBMObject.load(db, recID);
    }

}
