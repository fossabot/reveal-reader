package com.jackcholt.reveal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class Util {
    
    /**
     * Remove HTML and Titlecase a book title.
     * 
     * @param title The unformatted title.
     * @return The formatted title.
     */
    public static final String formatTitle(final String title) {
        StringBuffer sb = new StringBuffer();
        Scanner scan = new Scanner(title.toLowerCase().replaceAll("<[^>]*(small|b|)[^<]*>", ""));
        
        while(scan.hasNext()) {
            String word = scan.next();
            if(!"of and on about above over under ".contains(word + " ")) {
                word = word.substring(0,1).toUpperCase() + word.substring(1,word.length());
            }
            
            sb.append(word + " ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Parses the binding text from BINDING.HTML to get the Book Title.
     * 
     * @param binding The binding text
     * @return The title of the book.
     */
    public static final String getBookTitleFromBindingText(String binding) {
        // parse binding text to populate book title
        String bookTitle = "No book title";

        int bindingPos = binding.toLowerCase().indexOf("<a");
        
        if (bindingPos != -1) {
            binding = binding.substring(bindingPos);
            bindingPos = binding.toLowerCase().indexOf("href");
        }

        if (bindingPos != -1) {
            binding = binding.substring(bindingPos);
            bindingPos = binding.toLowerCase().indexOf(">");
        }

        if (bindingPos != -1) {
            binding = binding.substring(bindingPos + 1);
            bindingPos = binding.toLowerCase().indexOf("</a>");
        }
        
        if (bindingPos != -1) {
            bookTitle = binding.substring(0, bindingPos);
        }
        
        return bookTitle;
    }

    /**
     * Parses the binding text from BINDING.HTML to get the Book Title.
     * 
     * @param binding The binding text
     * @return The title of the book.
     */
    public static final String getBookShortTitleFromBindingText(String binding) {
        // parse binding text to populate book title
        String bookShortTitle = "No book short title";

        int bindingPos = binding.toLowerCase().indexOf("<a");
        
        if (bindingPos != -1) {
            binding = binding.substring(bindingPos);
            bindingPos = binding.toLowerCase().indexOf("href");
        }

        if (bindingPos != -1) {
            binding = binding.substring(bindingPos);
            bindingPos = binding.toLowerCase().indexOf("\"");
        }

        if (bindingPos != -1) {
            binding = binding.substring(bindingPos + 1);
            bindingPos = binding.toLowerCase().indexOf(".");
        }
        
        if (bindingPos != -1) {
            bookShortTitle = binding.substring(0, bindingPos);
        }
        
        return bookShortTitle;
    }

    /**
     * Uncompress a GZip file that has been converted to a byte array.
     * 
     * @param buf The byte array that contains the GZip file contents.
     * @return The uncompressed String.
     * @throws IOException If there is a problem reading the byte array. 
     */
    public static final String decompressGzip(final byte[] buf) throws IOException {
        
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        GZIPInputStream zip = new GZIPInputStream(bis);
        final int BUF_SIZE = 255;
        StringBuffer decomp = new StringBuffer();
        byte[] newBuf = new byte[BUF_SIZE];
        
        int bytesRead = 0;
        while (-1 != (bytesRead = zip.read(newBuf, 0, BUF_SIZE))) { 
            decomp.append(new String(newBuf).substring(0, bytesRead));
        }
    
        return decomp.toString();
    }

    /**
     * Make an array of ints from the next four bytes in the byte array <code>ba</code>
     * starting at position <code>pos</code> in <code>ba</code>.
     * 
     * @param ba The byte array to read from.
     * @param pos The position in <code>ba</code> to start from.
     * @return An array of four bytes which are in least to greatest 
     * significance order.
     * @throws IOException When the DataInputStream &quot;is&quot; cannot be read
     * from. 
     */
    public static final int[] makeVBIntArray(final byte[] ba, final int pos) throws IOException {
        int[] iArray = new int[4];
        
        if (pos > ba.length) {
            throw new IllegalArgumentException("The pos parameter is larger than the size of the byte array.");
        }
        
        // Need to use some bit manipulation to make the bytes be treated as unsigned
        iArray[0] = (0x000000FF & (int)ba[pos]);
        iArray[1] = (0x000000FF & (int)ba[pos + 1]);
        iArray[2] = (0x000000FF & (int)ba[pos + 2]);
        iArray[3] = (0x000000FF & (int)ba[pos + 3]);
        
        return iArray;
    }
    
    /**
     * Make an array of ints from the next four bytes in the DataInputStream.
     * 
     * @param is the InputStream from which to read.
     * @return An array of four bytes which are in least to greatest 
     * significance order.
     * @throws IOException When the DataInputStream &quot;is&quot; cannot be read
     * from. 
     */
    public static final int[] makeVBIntArray(final RandomAccessFile is) throws IOException {
        int[] iArray = new int[4];
        
        iArray[0] = (0x000000FF & (int)is.readByte());
        iArray[1] = (0x000000FF & (int)is.readByte());
        iArray[2] = (0x000000FF & (int)is.readByte());
        iArray[3] = (0x000000FF & (int)is.readByte());
        
        return iArray;
    }

    /**
     * Read in the four bytes of VB Long as stored in the YBK file.  VB Longs 
     * are stored as bytes in least significant byte to most significant byte 
     * order.
     *  
     * @param is The DataInputStream to read from.
     * @return The numeric value of the four bytes.
     * @throws IOException If the input stream is not readable.
     */
    public static final int readVBInt(RandomAccessFile is) throws IOException {
        return readVBInt(makeVBIntArray(is));
    }
    
    /**
     * Read in the four bytes of VB Long as stored in the YBK file.  VB Longs 
     * are stored as bytes in least significant byte (LSB) &quot;little 
     * endian&quot; order.
     *  
     * @param bytes byte array to read from.
     * @return The numeric value of the four bytes.
     * @throws IOException If the input stream is not readable.
     */
    public static final int readVBInt(final int[] bytes) throws IOException {
        int i = bytes[0];
        i += bytes[1] << 8;
        i += bytes[2] << 16;
        i += bytes[3] << 24;
        
        return i;
    }
    

    public static final String htmlize(final String text) {
        if (text == null) {
            throw new IllegalStateException("No text was passed.");
        }
        String content = text;
        int pos = content.indexOf("<end>");
        
        if (pos != -1) {
            content = content.substring(pos + 5);
        }
        return "<html><body>" + content + "</body></html>";
    }

    public static final HashMap<String,String> getFileNameChapterFromUri(final String uri, 
            final String libDir, final boolean isGzipped) {
        
        HashMap<String, String> map = new HashMap<String,String>();
        
        int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
        String dataString = uri.substring(ContentUriLength + 1).replace("%20", " ");
        
        String[] urlParts = dataString.split("/");
        
        String book = libDir + urlParts[0] + ".ybk";
                
        map.put("book", book);
        
        String chapter = "";
        for (int i = 0; i < urlParts.length; i++) {
           chapter += "\\" + urlParts[i];
        }
                
        if (isGzipped) {
            chapter += ".gz";
        }
        
        map.put("chapter", chapter);
        
        return map;
    }
    
    public static boolean isInteger(final String num) {
        try {
            Integer.parseInt(num);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /** 
     * Return the tail end of the text.
     * 
     * @param text The text to shorten.
     * @param length The maximum length of the string to return.
     * @return The tail end of the <code>text</code> passed in if it is longer 
     * than <code>length</code>. The entire <code>text</code> passed if it is 
     * shorter than <code>length</code>.
     */
    public static String tail(final String text, final int length) {
        int start = 0;
        int textLength = text.length();
        
        if (textLength > length) {
            start = textLength - length;
        }
        
        return text.substring(start);
    }
 	

}
