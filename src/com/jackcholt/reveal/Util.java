package com.jackcholt.reveal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;

/**
 * The purpose of this class is to hold general purpose methods.
 * 
 * @author Jack C. Holt, Dave Packham and others
 *
 */
public class Util {
    private static final String TAG = "Util"; 
	//private static final int DIALOG_DELETE = 1;
	//private static final int DIALOG_RENAME = 2;
	//private File mContextFile = new File("");

	public static final String NO_TITLE = "no_book_title";
	
	
	/**
	 * Dave Packham
	 * Check for network connectivity before trying to go to the net and hanging :)
	 * hitting F8 in the emulator will turn network on/off
	 */
	@SuppressWarnings("static-access")
	public static boolean isNetworkUp(Context _this) {
		boolean networkUpOrNot;
		
		ConnectivityManager connectivityManager  = (ConnectivityManager)_this.getSystemService(_this.CONNECTIVITY_SERVICE); 
		//NetworkInfo netinfo = connectivityManager .getActiveNetworkInfo(); 
		//NetworkInfo activeNetInfo = connectivityManager .getActiveNetworkInfo();
		NetworkInfo mobNetInfo = connectivityManager .getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
			if(mobNetInfo.getState() == NetworkInfo.State.CONNECTED){ 
				networkUpOrNot = true;    
		}  else {
				networkUpOrNot = false;    
	        	//Toast.makeText(_this, "Internet not available,  Please enable your preferred network", Toast.LENGTH_LONG).show();
	  
        }
		
		 	 
		return networkUpOrNot;
	}
    
	
	
    /**
     * Remove HTML, surrounding quotes and Titlecase a book title.
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
                if (word.length() == 1) {
                	word = word.toUpperCase();
                } else {
	                int capLength = 1;
	                
	                if ("abcdefghijklmnopqrstuvwxyz".indexOf(word.charAt(0)) == -1) {
	                    // if the word starts with a special character, capitalize the 
	                    // actual first letter
	                    capLength = 2;
	                }
	                word = word.substring(0,capLength).toUpperCase() + word.substring(capLength,word.length());
                }
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
        String bookTitle = NO_TITLE;

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
     * @return The uncompressed String. Returns null if there was an IOException.
     */
    public static final String decompressGzip(final byte[] buf) {
        StringBuilder decomp = null;
        
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            GZIPInputStream zip = new GZIPInputStream(bis);
            final int BUF_SIZE = 255;
            decomp = new StringBuilder(BUF_SIZE);
            byte[] newBuf = new byte[BUF_SIZE];
            
            int bytesRead = 0;
            while (-1 != (bytesRead = zip.read(newBuf, 0, BUF_SIZE))) { 
                decomp.append(new String(newBuf, "ISO_8859-1").substring(0, bytesRead));
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error decompressing file: " + ioe.getMessage());
            return null;
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
            throw new IllegalArgumentException("No text was passed.");
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
        
        //Log.d(TAG, "style: " + style);
        
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
     * @throws InconsistentContentException 
     */
    public static String processIfbook(final String content, 
            final Context ctx, final String libDir) {
        
        YbkDAO ybkDao = YbkDAO.getInstance(ctx);
        
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
                        
                        Book book = ybkDao.getBook(libDir + bookName + ".ybk");
                        
                        if (book != null) {
                            newContent.append(oldContent.substring(gtPos + 1, elsePos));
                            //Log.d(TAG, "Appending: " + oldContent.substring(gtPos + 1, elsePos));
                        } else {
                            newContent.append(oldContent.substring(elsePos + bookName.length() + 11, endPos));
                        } 
                        
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
        String fixedContent = content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>", 
                "<span class=\"ah\" id=\"ah$1\">$2</span>");
        
        //Log.d(TAG, "Fixed Content"  + fixedContent);
        
        return fixedContent;
/*        StringBuilder newContent = new StringBuilder();

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
                    //Log.d(TAG, "Appending: " + oldContent.substring(gtPos + 1, endPos));
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
*/
    }

    /**
     * Convert ifvar tags into span tags using &quot;ah&quot; as the class and making
     * the id &quot;ah&quot; appended by the number of the ahtag.
     * 
     * @param content The content containing the ahtags to convert.
     * @return The converted content.
     * @throws InvalidFileFormatException If content is in the wrong format.
     */
    public static String convertIfvar(final String content) throws InvalidFileFormatException {
        /*String findString = "<ifvar=([a-zA-Z0-9]+)>(.+)" +
        		"<[aA]\\s+href=['\"]\\+\\1=0['\"]>(.+)</[aA]>(.+)" +
        		"<elsevar=\\1>(.+)<[aA]\\s+href=['\"]\\+\\1=1['\"]>" +
        		"(.+)</[aA]>(.+)<endvar=\\1>";
        
        Log.d(TAG, "findString: " + findString);
        
        String replaceString = "<span class=\"_show$1\">$2<a href=\"javascript:hideSpan('$1')\">" +
            "$3</a>$4</span><span class=\"_hide$1\">$5<a href=\"javascript:showSpan('$1')\">$6</a>$7</span>";
        
        Log.d(TAG, "replaceString: " + replaceString);

        String fixedContent = content.replaceAll(findString, replaceString);
        
        Log.d(TAG, "fixedContent: " + fixedContent);

        return fixedContent;*/
        
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
                        //Log.d(TAG, "Appending: " + newShowText);
                        
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
	 * @return true if the file was downloaded.
     * @throws IOException When unable to read a downloaded file.
	 */
	public static boolean fetchAndLoadTitle(final URL fileLocation,
			final URL downloadUrl, final String libDir, final Context context) throws IOException {
		
		boolean success = false;

		YbkDAO ybkDao = YbkDAO.getInstance(context);
		
		final byte[] buffer = new byte[255];

		ArrayList<File> files = new ArrayList<File>();

		try {
			FileOutputStream out = null;

			if (fileLocation.getFile().endsWith("zip")
					|| fileLocation.getFile().contains("?")) {
				ZipInputStream zip = new ZipInputStream(downloadUrl
						.openStream());

				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					// unpack all the files
					File file = new File(libDir + entry.getName());

					// check to see if they already have this title
					// if (file.exists() && !shouldDownload(context, file)) {
					if (file.exists()) {
						file.delete();
						ybkDao.deleteBook(file.getAbsolutePath());
						
					}

					out = new FileOutputStream(file);

					int bytesRead = 0;
					while (-1 != (bytesRead = zip.read(buffer, 0, 255))) {
						out.write(buffer, 0, bytesRead);
					}
					
					files.add(file);
				}
				zip.close();
			} else if (fileLocation.getFile().endsWith("ybk")) {
				BufferedInputStream in = new BufferedInputStream(downloadUrl
						.openStream());

				File file = new File(libDir + fileLocation.getFile());

				// if (file.exists() && !shouldDownload(context, file)) {
				if (file.exists()) {
					file.delete();
					ybkDao.deleteBook(file.getAbsolutePath());
				}
				out = new FileOutputStream(file);

				int bytesRead = 0;
				while (-1 != (bytesRead = in.read(buffer, 0, 255))) {
					out.write(buffer, 0, bytesRead);
				}
				
				files.add(file);

				in.close();
			} else {
				Log.w(TAG, "Unable to process file "
						+ fileLocation.getFile());
			}

			if (out != null) {
				out.flush();
				out.close();
			}
			
			success = true;
		} catch (IOException e) {
			Log.w(TAG, e.getMessage());
		}

		// add this book to the list
		if (success) {
			for (File file : files) {
				// The file was properly downloaded
				
                // Create an object for reading a ybk file;
                YbkFileReader ybkRdr = new YbkFileReader(context, file.getAbsolutePath());
                // Tell the YbkFileReader to populate the book info into the database;
                ybkRdr.populateBook();

			}
		} 

		return success;
	}
	
	/**
	 * This should ask the user whether they want to overwrite the title in
	 * question... It's causing crashes because it is called from a new thread.
	 * This may be fixed or we may just scrap it.
	 * 
	 * @param context
	 * @param file
	 * @return
	 */
	@SuppressWarnings("unused")
	private static boolean shouldDownload(final Context context, final File file) {
		new AlertDialog.Builder(context).setTitle(
				R.string.ebook_exists_still_download).setPositiveButton(
				R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						file.delete();
	                    YbkDAO ybkDao = YbkDAO.getInstance(context);
						ybkDao.deleteBook(file.getAbsolutePath());
					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						/* Do absolutely nothing */
					}
				}).create();

		return !file.exists();
	}
	
	public static  void showSplashScreen(Context _this) {
		boolean mShowSplashScreen = true;
    //Toast Splash with image  :)
	    if (mShowSplashScreen) {
		   Toast toast = new Toast(_this);
		   LinearLayout lay = new LinearLayout(_this);
		   lay.setOrientation(LinearLayout.HORIZONTAL);
		   ImageView view = new ImageView(_this);
		   view.setImageResource(R.drawable.splash);
		   lay.addView(view);
		   toast.setView(lay);
		   toast.setDuration(Toast.LENGTH_LONG); 
		   toast.show();
	   }
	}	
	
	public void deleteFileOrFolder(File file, Context _this) {
		
		if (file.delete()) {
			// Delete was successful.
			//refreshList();
				Toast.makeText(_this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
		}
		else {
				Toast.makeText(_this, R.string.error_deleting_file, Toast.LENGTH_SHORT).show();
		}
	}
	
	
	/**
	 * Convenience method to send a notification that autocancels.
	 * @see sendNotification(Context,String,int,String,int,NotificationManager,Class,boolean)
	 */
	public static void sendNotification(final Context ctx, final String text, 
	        final int iconId, final String title, NotificationManager notifMgr, 
	        final int notifId, final Class<?> classToStart) {
	    
	    sendNotification(ctx, text, iconId, title, notifId, notifMgr, classToStart, 
	            true);
	}
	
	/**
	 * Encapsulation of the code needed to send a notification.  
	 * @param ctx The context in which this notification is being sent.  Usually 
	 * the Activity.
	 * @param text The text of the notification.
	 * @param iconId The id of icon to use in the notification.
	 * @param title The header title of the notification.
	 * @param notifId The number you would like to use to identify this notification.
	 * @param notifMgr The NotificationManager to send the notification through.
	 * @param classToStart The class to start when the notification is tapped on.
	 * @param autoCancel True if the notification should automatically disappear 
	 * from the queue when tapped on.
	 */
	public static void sendNotification(final Context ctx, final String text, 
	        final int iconId, final String title, final int notifId, 
	        final NotificationManager notifMgr, final Class<?> classToStart, 
	        final boolean autoCancel) {
	    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, classToStart), 0);
	    
        Notification notif = new Notification(iconId, text,
                System.currentTimeMillis());
        
        if (autoCancel) {
            notif.flags = notif.flags | Notification.FLAG_AUTO_CANCEL;
        }
        
        notif.setLatestEventInfo(ctx, title, text, contentIntent);
        
        notifMgr.notify(notifId, notif);
	}
	
	/**
	 * Create the file directories if they don't exist.
	 * 
	 * @param ctx The context in which we are running.
	 */
	public static void createDefaultDirs(final Context ctx) {
	    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
	    String strRevealDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
	    
	    if (!strRevealDir.startsWith("/sdcard/")) {
	    	String strRevealDirTemp = strRevealDir;

	    	if(!strRevealDir.startsWith("/")){
		    	strRevealDir = "/sdcard/" + strRevealDirTemp;
	    	} else {
		    	strRevealDir = "/sdcard" + strRevealDirTemp;
	    	}
	    }
	    
	    File revealdir = new File(strRevealDir);
	    
        if (!revealdir.exists()) {
             revealdir.mkdirs();
             //Log.i(Global.TAG, "Create reveal dir on sdcard ok");
        }
        
        File imagesDir = new File(strRevealDir + "images/");
        if (!imagesDir.exists()) {
             imagesDir.mkdirs();
             //Log.i(Global.TAG, "Create images dir on sdcard ok");
        }
    }

}

