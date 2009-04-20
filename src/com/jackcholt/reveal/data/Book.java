/**
 * 
 */
package com.jackcholt.reveal.data;

import org.garret.perst.Persistent;

/**
 * This class defines objects which store data about books.
 * 
 * @author Jack C. Holt
 *
 */
public class Book extends Persistent {
    public long id;
    public String fileName;
    public String bindingText;
    public String title;
    public String formattedTitle;
    public String shortTitle;
    public String metaData;
    public boolean active;
    
    public String toString() {
        return formattedTitle;
    }
    
    
}
