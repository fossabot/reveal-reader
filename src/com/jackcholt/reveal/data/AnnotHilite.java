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
public class AnnotHilite implements Serializable {

    private static final long serialVersionUID = -2388452770454940860L;

    public String note;
    public int color;
    public int verse;
    public String bookFilename;
    public String chapterFilename;
}
