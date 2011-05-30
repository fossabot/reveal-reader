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

    @Override
    public boolean equals(Object o) {
        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(o instanceof AnnotHilite)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        AnnotHilite ah2 = (AnnotHilite) o;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        return bookFilename.equals(ah2.bookFilename) && chapterFilename.equals(ah2.chapterFilename)
                && verse == ah2.verse;
    }

}
