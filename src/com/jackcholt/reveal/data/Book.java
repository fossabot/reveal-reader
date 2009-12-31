/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.Serializable;

/**
 * This class defines objects which store data about books.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class Book implements Serializable {
    private static final long serialVersionUID = -1L;

    public String fileName;
    public String title;
    public String shortTitle;
    public String charset;

    public String toString() {
        return title;
    }

}
