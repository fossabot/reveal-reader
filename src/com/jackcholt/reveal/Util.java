package com.jackcholt.reveal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;


public class Util {
    private static final String TAG = "Util"; 
    
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
    

    public static final String htmlize(final String text, final SharedPreferences sharedPref) {
        if (text == null) {
            throw new IllegalStateException("No text was passed.");
        }
        
        boolean showPicture = sharedPref.getBoolean("show_pictures", true);
        boolean showAH = sharedPref.getBoolean("show_ah", false);
        
        String content = text;
        int pos = content.indexOf("<end>");
        
        if (pos != -1) {
            content = content.substring(pos + 5);
        }
        
        String style = 
            "<style>" +
            "._showpicture {" + (showPicture ? "display:inline;" : "display:none") + "}" +
            "._hidepicture {"+ (showPicture ? "display:none;" : "display:inline") + "}" +
            "._showtoc {display:inline}" +
            "._hidetoc {display:none}" +
            ".ah {" + (showAH ? "display:inline;" : "display:none") + "}" +
            "</style>";
        
        Log.d(TAG, "style: " + style);
        
        return "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" 
        + style + "</head><body>" + content + "</body></html>";
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
 	
    /**
     * Process ifbook tags to not show links to books that don't exist in the ebook 
     * directory.  Remove ifbook tags to clean up the HTML.
     *  
     * @param content HTML to process.
     * @param contRes Reference to the environment in which we are working.
     * @param libDir The directory which contains our ebooks.
     * @return The processed content.
     */
    public static String processIfbook(final String content, 
            final ContentResolver contRes, final String libDir) {
        StringBuilder newContent = new StringBuilder();

        // Use this to get the actual content
        StringBuilder oldContent = new StringBuilder(content);
        
        // Use this for case-insensitive comparison
        StringBuilder oldLowerContent = new StringBuilder(content.toLowerCase());
        int pos = 0;
        
        while ((pos = oldLowerContent.indexOf("<ifbook=")) != -1) {
            boolean fullIfBookFound = false;
            
            // copy text before <ifbook> tag to new content and remove from old
            newContent.append(oldContent.substring(0, pos));
            oldContent.delete(0, pos);
            oldLowerContent.delete(0, pos);
            
            int gtPos = oldContent.indexOf(">");
            if (gtPos != -1) {
                
                // grab the bookname by skipping the beginning of the ifbook tag
                String bookName = oldContent.substring(8, gtPos);
                String lowerBookName = bookName.toLowerCase();
                
                int elsePos = oldLowerContent.indexOf("<elsebook=" + lowerBookName + ">");
                if (elsePos != -1 && elsePos > gtPos) {
                
                    int endPos = oldLowerContent.indexOf("<endbook=" + lowerBookName + ">");
                    if (endPos != -1 && endPos > elsePos) {
                        
                        fullIfBookFound = true;
                        
                        Cursor c = contRes.query(Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book"), 
                                new String[] {YbkProvider.FILE_NAME}, "lower(" + YbkProvider.FILE_NAME + ") = lower(?)", 
                                new String[] {libDir + bookName + ".ybk"}, null);
                        
                        int count = c.getCount();
                        if (count == 1) {
                            newContent.append(oldContent.substring(gtPos + 1, elsePos));
                            Log.d(TAG, "Appending: " + oldContent.substring(gtPos + 1, elsePos));
                        } else if (count == 0) {
                            newContent.append(oldContent.substring(elsePos + bookName.length() + 11, endPos));
                        } else {
                            throw new IllegalStateException("More than one record for the same book");
                        }
                        
                        //Log.d(TAG, newContent.substring(newContent.length() - 200, newContent.length()+1));
                        
                        // remove just-parsed <ifbook> tag structure so we can find the next
                        oldContent.delete(0, endPos + bookName.length() + 10);
                        oldLowerContent.delete(0, endPos + bookName.length() + 10);
                    }
                }
            } 
            
            // remove just-parsed <ifbook> tag so we can find the next
            if (!fullIfBookFound) {
                oldContent.delete(0,8);
                oldLowerContent.delete(0,8);
            }
            
        }
        
        // copy the remaining content over
        newContent.append(oldContent);
        
        return newContent.toString();
    }
    
    /**
     * Convert ahtags into span tags using &quot;ah&quot; as the class and making
     * the id &quot;ah&quot; appended by the number of the ahtag.
     * 
     * @param content The content containing the ahtags to convert.
     * @return The converted content.
     */
    public static String convertAhtag(final String content) {
        StringBuilder newContent = new StringBuilder();

        // Use this to get the actual content
        StringBuilder oldContent = new StringBuilder(content);
        
        // Use this for case-insensitive comparison
        StringBuilder oldLowerContent = new StringBuilder(content.toLowerCase());
        int pos = 0;
        
        while ((pos = oldLowerContent.indexOf("<ahtag num=")) != -1) {
            boolean fullAhtagFound = false;
            
            // copy text before <ahtag> tag to new content and remove from old
            newContent.append(oldContent.substring(0, pos));
            oldContent.delete(0, pos);
            oldLowerContent.delete(0, pos);
            
            int gtPos = oldContent.indexOf(">");
            if (gtPos != -1) {
                
                // grab the number by skipping the beginning of the ahtag tag
                String number = oldContent.substring(11, gtPos);
                
                int endPos = oldLowerContent.indexOf("</ahtag>");
                if (endPos != -1 && endPos > gtPos) {
                    
                    fullAhtagFound = true;
                    
                    newContent.append("<span class=\"ah\" id=\"ah").append(number).append("\">");
                    newContent.append(oldContent.substring(gtPos + 1, endPos));
                    Log.d(TAG, "Appending: " + oldContent.substring(gtPos + 1, endPos));
                    newContent.append("</span>");
                    
                    //Log.d(TAG, newContent.substring(newContent.length() - 200, newContent.length()+1));
                    
                    // remove just-parsed <ahtag> tag structure so we can find the next
                    oldContent.delete(0, endPos + 8);
                    oldLowerContent.delete(0, endPos + 8);
                }
            } 
            
            // remove just-parsed <ahtag> tag so we can find the next
            if (!fullAhtagFound) {
                oldContent.delete(0,11);
                oldLowerContent.delete(0,11);
            }
            
        }
        
        // copy the remaining content over
        newContent.append(oldContent);
        
        return newContent.toString();

    }

    /**
     * Convert ifvar tags into span tags using &quot;ah&quot; as the class and making
     * the id &quot;ah&quot; appended by the number of the ahtag.
     * 
     * @param content The content containing the ahtags to convert.
     * @return The converted content.
     */
    public static String convertIfvar(final String content) {
        StringBuilder newContent = new StringBuilder();

        // Use this to get the actual content
        StringBuilder oldContent = new StringBuilder(content);
        
        // Use this for case-insensitive comparison
        StringBuilder oldLowerContent = new StringBuilder(content.toLowerCase());
        int pos = 0;
        
        while ((pos = oldLowerContent.indexOf("<ifvar=")) != -1) {
            boolean fullIfvarFound = false;
            
            // copy text before <ifvar> tag to new content and remove from old
            newContent.append(oldContent.substring(0, pos));
            oldContent.delete(0, pos);
            oldLowerContent.delete(0, pos);
            
            int gtPos = oldContent.indexOf(">");
            if (gtPos != -1) {
                
                // grab the variable by skipping the beginning of the ifvar tag
                String variable = oldContent.substring(7, gtPos);
                String lowerVariable = variable.toLowerCase();
                
                int elsePos = oldLowerContent.indexOf("<elsevar=" + lowerVariable + ">");
                if (elsePos != -1 && elsePos > gtPos) {
                
                    int endPos = oldLowerContent.indexOf("<endvar=" + lowerVariable + ">");
                    if (endPos != -1 && endPos > elsePos) {
                        
                        fullIfvarFound = true;
                        
                        newContent.append("<span class=\"_show").append(variable).append("\">");

                        StringBuilder showText = new StringBuilder(oldContent.substring(gtPos + 1, elsePos));
                        StringBuilder showLowerText = new StringBuilder(oldContent.substring(gtPos + 1, elsePos).toLowerCase());
                        StringBuilder newShowText = new StringBuilder();
                            
                        int varPos = showLowerText.indexOf("+" + variable);
                        if (varPos != -1) {
                            int anchorPos = showLowerText.substring(0, varPos).lastIndexOf("<a");
                            if (anchorPos != -1) {
                                newShowText.append(showText.substring(0, anchorPos));
                                
                                showText.delete(0, anchorPos);
                                showLowerText.delete(0, anchorPos);

                                int closeAnchorPos = showLowerText.indexOf(">");
                                int endAnchorPos = 0;
                                if (closeAnchorPos != -1) {
                                    endAnchorPos = showLowerText.substring(closeAnchorPos).indexOf("</a>");
                                    if (endAnchorPos == -1) {
                                        throw new InvalidFileFormatException("Show anchor tag is not properly closed");
                                    }
                                }
                                
                                newShowText.append("<a href=\"javascript:hideSpan('").append(variable).append("')\">");
                                newShowText.append(showText.substring(closeAnchorPos + 1, closeAnchorPos + endAnchorPos)).append("</a>");
                                
                                showText.delete(0, closeAnchorPos + endAnchorPos + 4);
                                showLowerText.delete(0, closeAnchorPos + endAnchorPos + 4);
                                
                                newShowText.append(showText);
                            }
                        }
                        
                        newContent.append(newShowText);
                        Log.d(TAG, "Appending: " + newShowText);
                        
                        oldContent.delete(0, elsePos + variable.length() + 10);
                        oldLowerContent.delete(0, elsePos + variable.length() + 10);
                        
                        newContent.append("</span><span class=\"_hide").append(variable).append("\">");
                        
                        endPos = oldLowerContent.indexOf("<endvar=" + lowerVariable + ">");
                        if (endPos == -1) {
                            throw new InvalidFileFormatException("Endvar tag now missing");
                        }
                            
                        StringBuilder hideText = new StringBuilder(oldContent.substring(0, endPos));
                        StringBuilder hideLowerText = new StringBuilder(oldContent.substring(0, endPos).toLowerCase());
                        StringBuilder newHideText = new StringBuilder();
                            
                        varPos = hideLowerText.indexOf("+" + variable);
                        if (varPos != -1) {
                            int anchorPos = hideLowerText.substring(0, varPos).lastIndexOf("<a");
                            if (anchorPos != -1) {
                                newHideText.append(hideText.substring(0, anchorPos));
                                
                                hideText.delete(0, anchorPos);
                                hideLowerText.delete(0, anchorPos);

                                int closeAnchorPos = hideLowerText.indexOf(">");
                                int endAnchorPos = 0;
                                if (closeAnchorPos != -1) {
                                    endAnchorPos = hideLowerText.substring(closeAnchorPos).indexOf("</a>");
                                    if (endAnchorPos == -1) {
                                        throw new InvalidFileFormatException("Hide anchor tag is not properly closed");
                                    }
                                }
                                
                                newHideText.append("<a href=\"javascript:showSpan('").append(variable).append("')\">");
                                newHideText.append(hideText.substring(closeAnchorPos + 1, closeAnchorPos + endAnchorPos)).append("</a>");
                                
                                hideText.delete(0, closeAnchorPos + endAnchorPos + 4);
                                hideLowerText.delete(0,closeAnchorPos +  endAnchorPos + 4);
                                
                                newHideText.append(hideText);
                            }
                        }
                        
                        newContent.append(newHideText);
                        newContent.append("</span>");
                        
                        // remove just-parsed <ifvar> tag structure so we can find the next
                        oldContent.delete(0, endPos + variable.length() + 9);
                        oldLowerContent.delete(0, endPos + variable.length() + 9);
                    }
                }
            } 
            
            // remove just-parsed <ifvar> tag so we can find the next
            if (!fullIfvarFound) {
                oldContent.delete(0,7);
                oldLowerContent.delete(0,7);
            }
            
        }
        
        // copy the remaining content over
        newContent.append(oldContent);
        
        return newContent.toString();
    }
 
    /**
	 * Download and install title into library. Used by the title browser
	 * thread.
	 * 
	 * @param downloadUrl
	 *            Url from which we are downloading
	 * @return
	 */
	public static boolean fetchAndLoadTitle(URL fileLocation, URL downloadUrl,
			String libDir, ContentResolver resolver) {
		boolean success = false;

		final byte[] buffer = new byte[255];

		String filePath = null;

		try {
			FileOutputStream out = null;

			if (fileLocation.getFile().endsWith("zip")
					|| fileLocation.getFile().contains("?")) {
				ZipInputStream zip = new ZipInputStream(downloadUrl
						.openStream());
				ZipEntry entry = zip.getNextEntry();

				filePath = libDir + entry.getName();

				out = new FileOutputStream(filePath);

				int bytesRead = 0;
				while (-1 != (bytesRead = zip.read(buffer, 0, 255))) {
					out.write(buffer, 0, bytesRead);
				}

				zip.close();
			} else if (fileLocation.getFile().endsWith("ybk")) {
				BufferedInputStream in = new BufferedInputStream(downloadUrl
						.openStream());

				filePath = libDir + fileLocation.getFile();

				out = new FileOutputStream(filePath);

				int bytesRead = 0;
				while (-1 != (bytesRead = in.read(buffer, 0, 255))) {
					out.write(buffer, 0, bytesRead);
				}

				in.close();
			} else {
				Log.w(resolver.getClass().getName(), "Unable to process file "
						+ fileLocation.getFile());
			}

			if (out != null) {
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			Log.w(resolver.getClass().getName(), e.getMessage());
		}

		// add this book to the list
		Uri bookUri = Uri.withAppendedPath(YbkProvider.CONTENT_URI, "book");
		ContentValues values = new ContentValues();
		values.put(YbkProvider.FILE_NAME, filePath);
		resolver.insert(bookUri, values);
		success = true;

		return success;
	}
}

