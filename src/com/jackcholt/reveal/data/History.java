/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.Serializable;

import com.jackcholt.reveal.Util;

/**
 * This class defines objects which store data about history and bookmarks.
 * 
 * @author Jack C. Holt
 * 
 */
public class History implements Serializable {
    private static final long serialVersionUID = -1L;

    public long id = Util.getUniqueTimeStamp();
    // public long bookId;
    public String bookFileName;
    public String chapterName;
    public int scrollYPos;
    public String title;
    public int bookmarkNumber = 0;

    public String toString() {
        return title;
    }

}
