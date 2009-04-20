/**
 * 
 */
package com.jackcholt.reveal.data;

import org.garret.perst.Persistent;

/**
 * This class defines objects which store data about book chapters.
 * 
 * @author Jack C. Holt
 *
 */
public class Chapter extends Persistent {
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
        return id + ":" + bookId + ":" + fileName + ":" + offset + 
        ":" + length + ":" + orderName + ":" + orderNumber + ":" + navbarTitle + 
        ":" + historyTitle + ":" + navFile + ":" + zoomPicture;
    }
    
    
}
