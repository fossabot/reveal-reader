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
    public int id;
    public int bookId;
    public String name;
    public String createDateTime;
    public int scrollPos;
    public String title;
    public int bookmarkNumber;
    
    public String toString() {
        return id + ":" + bookId + ":" + name + ":" + createDateTime + 
        ":" + scrollPos + ":" + title + ":" + bookmarkNumber;
    }
    
    
}
