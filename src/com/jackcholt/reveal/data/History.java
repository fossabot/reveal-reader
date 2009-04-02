/**
 * 
 */
package com.jackcholt.reveal.data;

import org.garret.perst.Persistent;

/**
 * This class defines objects which store data about history and bookmarks.
 * 
 * @author Jack C. Holt
 *
 */
public class History extends Persistent {
    public long id  = System.currentTimeMillis();
    public long bookId;
    public String chapterName;
    public int scrollYPos;
    public String title;
    public Integer bookmarkNumber = null;
    
    public String toString() {
        return title;
    }
    
    
}
