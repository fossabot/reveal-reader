/**
 * 
 */
package com.jackcholt.reveal.data;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Comparator;

import com.jackcholt.reveal.Util;
import com.jackcholt.reveal.YbkFileReader;

/**
 * This class defines objects which store data about book chapters.
 * 
 * @author Jack C. Holt
 * 
 */
public class Chapter implements Serializable {
    private static final long serialVersionUID = -1L;

    public String fileName;
    public int offset;
    public int length;
    public int orderNumber = 0;
    public transient String orderName;

    public String toString() {
        return fileName + ":" + offset + ":" + length + ":" + orderNumber;
    }
    
    public static Chapter fromYbkIndex(byte buf[], int offset, String charset) {
        Chapter chapter = new Chapter();
        int i = offset;
        int end = i + YbkFileReader.INDEX_POS_OFFSET;
        while (i < end && buf[i++] != 0)
            ;
        try {
            i--;
            chapter.fileName = new String(buf, offset, i - offset, charset).toLowerCase();
        } catch (UnsupportedEncodingException e) {
            chapter.fileName = new String(buf, offset, i - offset).toLowerCase();
        }
        chapter.orderName = getOrderName(chapter.fileName);
        
        i = offset + YbkFileReader.INDEX_POS_OFFSET;
        chapter.offset = Util.readVBInt(buf, offset + YbkFileReader.INDEX_POS_OFFSET);
        chapter.length = Util.readVBInt(buf, offset + YbkFileReader.INDEX_LENGTH_OFFSET);
        return chapter;
    }
    
    public static final Comparator<Chapter> chapterNameComparator = new Comparator<Chapter>() {
        public int compare(Chapter c1, Chapter c2) {
            return c1.fileName.compareTo(c2.fileName);
        }
        
    };

    public static final Comparator<Chapter> orderNameComparator = new Comparator<Chapter>() {
        public int compare(Chapter c1, Chapter c2) {
            return c1.orderName.compareTo(c2.orderName);
        }
        
    };
    
    public static String getOrderName(String fileName) {
        if (fileName.indexOf('\\', 1) != -1) {
            return fileName.replaceFirst("^\\\\[^\\\\]+\\\\([^\\.]*).*$", "$1").toLowerCase();
        } else {
            return Util.EMPTY_STRING;
        }
            
    }

}
