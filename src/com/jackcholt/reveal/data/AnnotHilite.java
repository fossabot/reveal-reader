/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.Serializable;

import android.graphics.Color;

/**
 * This class defines objects which store data about Annotations and Highlights.
 * 
 * @author Jack C. Holt
 * @author Shon Vella
 * 
 */
public class AnnotHilite implements Serializable {

    private static final long serialVersionUID = -2388452770454940860L;

    public String note = "";
    public int color = Color.TRANSPARENT;
    public int verse = 0;
    public String bookFilename = "";
    public String chapterFilename = "";
}
