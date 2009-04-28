/**
 * 
 */
package com.jackcholt.reveal.data;

import org.garret.perst.Persistent;

import com.jackcholt.reveal.Util;

/**
 * This class defines objects which store data about history and bookmarks.
 * 
 * @author Jack C. Holt
 * 
 */
public class History extends Persistent {
    public long id = Util.getUniqueTimeStamp();
    public long bookId;
    public String chapterName;
    public int scrollYPos;
    public String title;
    public int bookmarkNumber = 0;

    public String toString() {
        return title;
    }

}
