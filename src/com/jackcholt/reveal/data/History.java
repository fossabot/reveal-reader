/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.IOException;
import java.io.Serializable;

import jdbm.RecordManager;

import com.jackcholt.reveal.Util;

/**
 * This class defines objects which store data about history and bookmarks.
 * 
 * @author Jack C. Holt
 * 
 */
public class History extends JDBMObject implements Serializable {
    private static final long serialVersionUID = 7208903236018191152L;

    // note that this is negative so that iteration will get newest first
    public long id = Util.getUniqueTimeStamp();
    public long bookId;
    public String chapterName;
    public int scrollYPos;
    public String title;
    public int bookmarkNumber = 0;

    public String toString() {
        return title;
    }

    /**
     * Load a history entry from db by recID
     * 
     * @param db
     * @param recID
     * @return the history entry
     * @throws IOException
     */
    protected static History load(RecordManager db, long recID) throws IOException {
        return (History) JDBMObject.load(db, recID);
    }

}
