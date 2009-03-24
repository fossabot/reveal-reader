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
    public int id;
    public int bookId;
    public String fileName;
    public int offset;
    public int length;
    public String orderName;
    public int orderNumber;
    public String navbarTitle;
    public String historyTitle;
    public int navFile;
    public int zoomPicture;
    
    public String toString() {
        return id + ":" + bookId + ":" + fileName + ":" + offset + 
        ":" + length + ":" + orderName + ":" + orderNumber + ":" + navbarTitle + 
        ":" + historyTitle + ":" + navFile + ":" + zoomPicture;
    }
    
    
}
