package com.jackcholt.reveal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.XMLReader;

import android.app.Activity;
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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.YbkDAO;

/**
 * The purpose of this class is to hold general purpose methods.
 * 
 * @author Jack C. Holt, Dave Packham and others
 * 
 */
public class Util {
    private static final String TMP_EXTENSION = ".tmp";
    private static final String TAG = "Util";
    public static final String NO_TITLE = "no_book_title";
    public static final String EMPTY_STRING = new String();
    private static SharedPreferences mSharedPref;

    /**
     * Dave Packham Check for network connectivity before trying to go to the net and hanging :) hitting F8 in the
     * emulator will turn network on/off
     */
    public static boolean isNetworkUp(Context _this) {
        boolean networkUp;

        ConnectivityManager connectivityManager = (ConnectivityManager) _this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mobNetInfo.getState() == NetworkInfo.State.CONNECTED
                || wifiNetInfo.getState() == NetworkInfo.State.CONNECTED) {
            networkUp = true;
        } else {
            networkUp = false;
        }

        return networkUp;
    }

    /**
     * Remove HTML, surrounding quotes and Title case a book title.
     * 
     * @param title
     *            The unformatted title.
     * @return The formatted title.
     */
    public static final String formatTitle(final String title) {
        StringBuffer sb = new StringBuffer();
        // remove html tags and convert character references, and convert to
        // lower case
        String plainTitle = Html.fromHtml(title).toString().toLowerCase();
        Scanner scan = new Scanner(plainTitle);

        while (scan.hasNext()) {
            String word = scan.next();
            if (!"of and on about above over under ".contains(word + " ")) {
                if (word.length() == 1) {
                    word = word.toUpperCase();
                } else {
                    int capLength = 1;

                    if ("abcdefghijklmnopqrstuvwxyz".indexOf(word.charAt(0)) == -1) {
                        // if the word starts with a special character,
                        // capitalize the
                        // actual first letter
                        capLength = 2;
                    }
                    word = word.substring(0, capLength).toUpperCase() + word.substring(capLength, word.length());
                }
            }

            sb.append(word + " ");
        }

        return sb.toString().trim();
    }

    /**
     * Parses the binding text from BINDING.HTML to get the Book Title.
     * 
     * @param binding
     *            The binding text
     * @return The title of the book.
     */
    public static final String getBookTitleFromBindingText(String binding) {
        return formatTitle(binding);
    }

    /**
     * Parses the binding text from BINDING.HTML to get the Book Title.
     * 
     * @param binding
     *            The binding text
     * @return The title of the book.
     */
    public static final String getBookShortTitleFromBindingText(String binding) {
        // Compose the regex per http://www.martinfowler.com/bliki/ComposedRegex.html
        final String start = "^";
        final String caseInsensSingleLineFlags = "(?is)";
        final String oneOrMoreSpaces = "\\s+";
        final String singleOrDoubleQuote = "['\"]";
        final String oneOrNoBangs = "!?";
        final String shortTitleGroup = "(.+)";
        final String period = "\\.";
        final String zeroOrMoreChars = ".*";
        final String firstGroup = "$1";

        // parse binding text to populate book title
        String bookShortTitle = Html.fromHtml(  // handle character references
                binding.replaceAll(start + caseInsensSingleLineFlags + zeroOrMoreChars + "<a" + oneOrMoreSpaces
                        + "href=" + singleOrDoubleQuote + oneOrNoBangs + shortTitleGroup + period + zeroOrMoreChars
                        + ">" + zeroOrMoreChars, firstGroup)).toString();

        return bookShortTitle;
    }

    /**
     * Uncompress a GZip file that has been converted to a byte array.
     * 
     * @param buf
     *            The byte array that contains the GZip file contents.
     * @return The uncompressed String. Returns null if there was an IOException.
     */
    public static final String decompressGzip(final byte[] buf, String encoding) {
        StringBuilder decomp = null;

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            GZIPInputStream zip = new GZIPInputStream(bis);
            final int BUF_SIZE = 255;
            decomp = new StringBuilder(BUF_SIZE);
            byte[] newBuf = new byte[BUF_SIZE];

            int bytesRead = 0;
            while (-1 != (bytesRead = zip.read(newBuf, 0, BUF_SIZE))) {
                decomp.append(new String(newBuf, encoding).substring(0, bytesRead));
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error decompressing file: " + ioe.getMessage());
            return null;
        }

        return decomp.toString();
    }

    /**
     * Make an array of ints from the next four bytes in the byte array <code>ba</code> starting at position
     * <code>pos</code> in <code>ba</code>.
     * 
     * @param ba
     *            The byte array to read from.
     * @param pos
     *            The position in <code>ba</code> to start from.
     * @return An array of four bytes which are in least to greatest significance order.
     * @throws IOException
     *             When the DataInputStream &quot;is&quot; cannot be read from.
     */
    public static final int[] makeVBIntArray(final byte[] ba, final int pos) throws IOException {
        int[] iArray = new int[4];

        if (pos > ba.length) {
            throw new IllegalArgumentException("The pos parameter is larger than the size of the byte array.");
        }

        // Need to use some bit manipulation to make the bytes be treated as
        // unsigned
        iArray[0] = (0x000000FF & (int) ba[pos]);
        iArray[1] = (0x000000FF & (int) ba[pos + 1]);
        iArray[2] = (0x000000FF & (int) ba[pos + 2]);
        iArray[3] = (0x000000FF & (int) ba[pos + 3]);

        return iArray;
    }

    /**
     * Make an array of ints from the next four bytes in the DataInputStream.
     * 
     * @param is
     *            the InputStream from which to read.
     * @return An array of four bytes which are in least to greatest significance order.
     * @throws IOException
     *             When the DataInputStream &quot;is&quot; cannot be read from.
     */
    public static final int[] makeVBIntArray(final RandomAccessFile is) throws IOException {
        int[] iArray = new int[4];

        iArray[0] = (0x000000FF & (int) is.readByte());
        iArray[1] = (0x000000FF & (int) is.readByte());
        iArray[2] = (0x000000FF & (int) is.readByte());
        iArray[3] = (0x000000FF & (int) is.readByte());

        return iArray;
    }

    /**
     * Read in the four bytes of VB Long as stored in the YBK file. VB Longs are stored as bytes in least significant
     * byte to most significant byte order.
     * 
     * @param is
     *            The DataInputStream to read from.
     * @return The numeric value of the four bytes.
     * @throws IOException
     *             If the input stream is not readable.
     */
    public static final int readVBInt(RandomAccessFile is) throws IOException {
        return readVBInt(makeVBIntArray(is));
    }

    /**
     * Read in the four bytes of VB Long as stored in the YBK file. VB Longs are stored as bytes in least significant
     * byte (LSB) &quot;little endian&quot; order.
     * 
     * @param bytes
     *            byte array to read from.
     * @return The numeric value of the four bytes.
     */
    public static final int readVBInt(final int[] bytes) {
        int i = bytes[0];
        i += bytes[1] << 8;
        i += bytes[2] << 16;
        i += bytes[3] << 24;

        return i;
    }
    
    /**
     * Read in the four bytes of VB Long as stored in the YBK file. VB Longs are stored as bytes in least significant
     * byte (LSB) &quot;little endian&quot; order.
     * 
     * @param bytes
     *            byte array to read from.
     * @return The numeric value of the four bytes.
     */
    public static final int readVBInt(final byte[] bytes, int off) {
        int i = ((int)bytes[off++]) & 0xFF;
        i += (((int)bytes[off++]) & 0xFF) << 8;
        i += (((int)bytes[off++]) & 0xFF) << 16;
        i += (((int)bytes[off++]) & 0xFF) << 24;
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

        String style = "<style>" + "._showpicture {" + (showPicture ? "display:inline;" : "display:none") + "}"
                + "._hidepicture {" + (showPicture ? "display:none;" : "display:inline") + "}"
                + "._showtoc {display:inline}" + "._hidetoc {display:none}" + ".ah {"
                + (showAH ? "display:inline;" : "display:none") + "}" + "</style>";

        // Log.d(TAG, "style: " + style);

        return "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" + style
                + "</head><body>" + content + "</body></html>";
    }

    public static final HashMap<String, String> getFileNameChapterFromUri(final String uri, final String libDir,
            final boolean isGzipped) {

        HashMap<String, String> map = new HashMap<String, String>();

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
     * @param text
     *            The text to shorten.
     * @param length
     *            The maximum length of the string to return.
     * @return The tail end of the <code>text</code> passed in if it is longer than <code>length</code>. The entire
     *         <code>text</code> passed if it is shorter than <code>length</code>.
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
     * Process ifbook tags to not show links to books that don't exist in the ebook directory. Remove ifbook tags to
     * clean up the HTML.
     * 
     * @param content
     *            HTML to process.
     * @param contRes
     *            Reference to the environment in which we are working.
     * @param libDir
     *            The directory which contains our ebooks.
     * @return The processed content.
     * @throws IOException
     */
    public static String processIfbook(final String content, final Context ctx, final String libDir) throws IOException {

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
                            // Log.d(TAG, "Appending: " +
                            // oldContent.substring(gtPos + 1, elsePos));
                        } else {
                            newContent.append(oldContent.substring(elsePos + bookName.length() + 11, endPos));
                        }

                        // remove just-parsed <ifbook> tag structure so we can
                        // find the next
                        oldContent.delete(0, endPos + bookName.length() + 10);
                        oldLowerContent.delete(0, endPos + bookName.length() + 10);
                    }
                }
            }

            // remove just-parsed <ifbook> tag so we can find the next
            if (!fullIfBookFound) {
                oldContent.delete(0, 8);
                oldLowerContent.delete(0, 8);
            }

        }

        // copy the remaining content over
        newContent.append(oldContent);

        return newContent.toString();
    }

    /**
     * Convert ahtags into span tags using &quot;ah&quot; as the class and making the id &quot;ah&quot; appended by the
     * number of the ahtag.
     * 
     * @param content
     *            The content containing the ahtags to convert.
     * @return The converted content.
     */
    public static String convertAhtag(final String content) {
        String fixedContent = content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>",
                "<span class=\"ah\" id=\"ah$1\">$2</span>");

        // Log.d(TAG, "Fixed Content" + fixedContent);

        return fixedContent;
        /*
         * StringBuilder newContent = new StringBuilder();
         * 
         * // Use this to get the actual content StringBuilder oldContent = new StringBuilder(content);
         * 
         * // Use this for case-insensitive comparison StringBuilder oldLowerContent = new
         * StringBuilder(content.toLowerCase()); int pos = 0;
         * 
         * while ((pos = oldLowerContent.indexOf("<ahtag num=")) != -1) { boolean fullAhtagFound = false;
         * 
         * // copy text before <ahtag> tag to new content and remove from old newContent.append(oldContent.substring(0,
         * pos)); oldContent.delete(0, pos); oldLowerContent.delete(0, pos);
         * 
         * int gtPos = oldContent.indexOf(">"); if (gtPos != -1) {
         * 
         * // grab the number by skipping the beginning of the ahtag tag String number = oldContent.substring(11,
         * gtPos);
         * 
         * int endPos = oldLowerContent.indexOf("</ahtag>"); if (endPos != -1 && endPos > gtPos) {
         * 
         * fullAhtagFound = true;
         * 
         * 
         * 
         * 
         * 
         * 
         * newContent.append("<span class=\"ah\" id=\"ah").append(number).append( "\">");
         * newContent.append(oldContent.substring(gtPos + 1, endPos)); //Log.d(TAG, "Appending: " +
         * oldContent.substring(gtPos + 1, endPos)); newContent.append("</span>");
         * 
         * //Log.d(TAG, newContent.substring(newContent.length() - 200, newContent.length()+1));
         * 
         * // remove just-parsed <ahtag> tag structure so we can find the next oldContent.delete(0, endPos + 8);
         * oldLowerContent.delete(0, endPos + 8); } }
         * 
         * // remove just-parsed <ahtag> tag so we can find the next if (!fullAhtagFound) { oldContent.delete(0,11);
         * oldLowerContent.delete(0,11); }
         * 
         * }
         * 
         * // copy the remaining content over newContent.append(oldContent);
         * 
         * return newContent.toString();
         */
    }

    /**
     * Convert ifvar tags into span tags using &quot;ah&quot; as the class and making the id &quot;ah&quot; appended by
     * the number of the ahtag.
     * 
     * @param content
     *            The content containing the ahtags to convert.
     * @return The converted content.
     * @throws InvalidFileFormatException
     *             If content is in the wrong format.
     */
    public static String convertIfvar(final String content) throws InvalidFileFormatException {
        /*
         * String findString = "<ifvar=([a-zA-Z0-9]+)>(.+)" + "<[aA]\\s+href=['\"]\\+\\1=0['\"]>(.+)</[aA]>(.+)" +
         * "<elsevar=\\1>(.+)<[aA]\\s+href=['\"]\\+\\1=1['\"]>" + "(.+)</[aA]>(.+)<endvar=\\1>";
         * 
         * Log.d(TAG, "findString: " + findString);
         * 
         * String replaceString = "<span class=\"_show$1\">$2<a href=\"javascript:hideSpan('$1')\">" +
         * "$3</a>$4</span><span class=\"_hide$1\">$5<a href=\"javascript:showSpan('$1')\">$6</a>$7</span>" ;
         * 
         * Log.d(TAG, "replaceString: " + replaceString);
         * 
         * String fixedContent = content.replaceAll(findString, replaceString);
         * 
         * Log.d(TAG, "fixedContent: " + fixedContent);
         * 
         * return fixedContent;
         */

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
                        StringBuilder showLowerText = new StringBuilder(oldContent.substring(gtPos + 1, elsePos)
                                .toLowerCase());
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
                                newShowText.append(
                                        showText.substring(closeAnchorPos + 1, closeAnchorPos + endAnchorPos)).append(
                                        "</a>");

                                showText.delete(0, closeAnchorPos + endAnchorPos + 4);
                                showLowerText.delete(0, closeAnchorPos + endAnchorPos + 4);

                                newShowText.append(showText);
                            }
                        }

                        newContent.append(newShowText);
                        // Log.d(TAG, "Appending: " + newShowText);

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
                                newHideText.append(
                                        hideText.substring(closeAnchorPos + 1, closeAnchorPos + endAnchorPos)).append(
                                        "</a>");

                                hideText.delete(0, closeAnchorPos + endAnchorPos + 4);
                                hideLowerText.delete(0, closeAnchorPos + endAnchorPos + 4);

                                newHideText.append(hideText);
                            }
                        }

                        newContent.append(newHideText);
                        newContent.append("</span>");

                        // remove just-parsed <ifvar> tag structure so we can
                        // find the next
                        oldContent.delete(0, endPos + variable.length() + 9);
                        oldLowerContent.delete(0, endPos + variable.length() + 9);
                    }
                }
            }

            // remove just-parsed <ifvar> tag so we can find the next
            if (!fullIfvarFound) {
                oldContent.delete(0, 7);
                oldLowerContent.delete(0, 7);
            }

        }

        // copy the remaining content over
        newContent.append(oldContent);

        return newContent.toString();
    }

    /**
     * Download and install title into library. Used by the title browser thread.
     * 
     * @param fileLocation
     *            Url of target file
     * @param downloadUrl
     *            Url from which we are downloading
     * @param libDir
     *            library directory
     * @param context
     *            the caller's context
     * @return list of file paths to add to library
     * @throws IOException
     *             if download fails
     */
    public static List<String> fetchTitle(final File fileName, final URL downloadUrl, final String libDir,
            final Context context) throws IOException {

        boolean success = false;

        YbkDAO ybkDao = YbkDAO.getInstance(context);

        final byte[] buffer = new byte[255];

        ArrayList<File> files = new ArrayList<File>();
        ArrayList<String> downloaded = new ArrayList<String>();

        File libDirFile = new File(libDir);
        String filename = fileName.getName();
        FileOutputStream out = null;

        try { // assume we have a zip file first

            ZipInputStream zip = new ZipInputStream(downloadUrl.openStream());

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                // unpack all the files
                File file = new File(libDirFile, entry.getName());

                // check to see if they already have this title
                // if (file.exists() && !shouldDownload(context, file)) {
                if (file.exists()) {
                    file.delete();
                    ybkDao.deleteBook(entry.getName());

                }

                file = new File(libDirFile, entry.getName() + TMP_EXTENSION);
                out = new FileOutputStream(file);
                files.add(file);
                try {
                    int bytesRead = 0;
                    while (-1 != (bytesRead = zip.read(buffer, 0, 255))) {
                        out.write(buffer, 0, bytesRead);
                    }
                } finally {
                    out.close();
                }
            }

            zip.close();

            success = true;

        } catch (IOException e) { // non-zip attempt
            BufferedInputStream in = new BufferedInputStream(downloadUrl.openStream());
            try {
                File file = new File(libDirFile, filename);

                // if (file.exists() && !shouldDownload(context, file)) {
                if (file.exists()) {
                    file.delete();
                    ybkDao.deleteBook(filename);
                }
                file = new File(libDirFile, filename);
                out = new FileOutputStream(file);
                files.add(file);

                int bytesRead = 0;
                try {
                    while (-1 != (bytesRead = in.read(buffer, 0, 255))) {
                        out.write(buffer, 0, bytesRead);
                    }
                } finally {
                    out.close();
                }

                success = true;
            } catch (IOException e2) {
                Log.w(TAG, "Unable to process file " + fileName);
                throw new IOException("Unrecognized file type");
            } finally {
                in.close();
            }
        } finally {
            for (File file : files) {
                if (success) {

                    // rename from tmp
                    String realNameString = file.getAbsolutePath();
                    realNameString = realNameString.substring(0, realNameString.lastIndexOf(TMP_EXTENSION));
                    File realName = new File(realNameString);
                    file.renameTo(realName);
                    downloaded.add(new File(realNameString).getName());
                } else {
                    // delete partially downloaded files
                    file.delete();
                }
            }
        }
        return downloaded;
    }

    /**
     * This should ask the user whether they want to overwrite the title in question... It's causing crashes because it
     * is called from a new thread. This may be fixed or we may just scrap it.
     * 
     * @param context
     * @param file
     * @return
     */
    // @SuppressWarnings("unused")
    // private static boolean shouldDownload(final Context context, final File
    // file) {
    // new AlertDialog.Builder(context).setTitle(
    // R.string.ebook_exists_still_download).setPositiveButton(
    // R.string.alert_dialog_ok,
    // new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int whichButton) {
    // file.delete();
    // YbkService.requestRemoveBook(context, file.getAbsolutePath());
    // }
    // }).setNegativeButton(R.string.cancel,
    // new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int whichButton) {
    // /* Do absolutely nothing */
    // }
    // }).create();
    //
    // return !file.exists();
    // }
    public static void showSplashScreen(Context _this) {
        boolean mShowSplashScreen = true;
        // Toast Splash with image :)
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
            // refreshList();
            Toast.makeText(_this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(_this, R.string.error_deleting_file, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Convenience method to send a notification that autocancels.
     * 
     * @see sendNotification(Context,String,int,String,int,NotificationManager,Class ,boolean)
     */
    public static void sendNotification(final Context ctx, final String text, final int iconId, final String title,
            NotificationManager notifMgr, final int notifId, final Class<?> classToStart) {

        sendNotification(ctx, text, iconId, title, notifId, notifMgr, classToStart, true);
    }

    /**
     * Encapsulation of the code needed to send a notification.
     * 
     * @param ctx
     *            The context in which this notification is being sent. Usually the Activity.
     * @param text
     *            The text of the notification.
     * @param iconId
     *            The id of icon to use in the notification.
     * @param title
     *            The header title of the notification.
     * @param notifId
     *            The number you would like to use to identify this notification.
     * @param notifMgr
     *            The NotificationManager to send the notification through.
     * @param classToStart
     *            The class to start when the notification is tapped on.
     * @param autoCancel
     *            True if the notification should automatically disappear from the queue when tapped on.
     */
    public static void sendNotification(final Context ctx, final String text, final int iconId, final String title,
            final int notifId, final NotificationManager notifMgr, final Class<?> classToStart, final boolean autoCancel) {
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, classToStart), 0);

        Notification notif = new Notification(iconId, text, System.currentTimeMillis());

        if (autoCancel) {
            notif.flags = notif.flags | Notification.FLAG_AUTO_CANCEL;
        }

        notif.setLatestEventInfo(ctx, title, text, contentIntent);

        notifMgr.notify(notifId, notif);
    }

    /**
     * Create the file directories if they don't exist.
     * 
     * @param ctx
     *            The context in which we are running.
     */
    public static void createDefaultDirs(final Context ctx) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strRevealDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

        if (!strRevealDir.startsWith("/sdcard/")) {
            String strRevealDirTemp = strRevealDir;

            if (!strRevealDir.startsWith("/")) {
                strRevealDir = "/sdcard/" + strRevealDirTemp;
            } else {
                strRevealDir = "/sdcard" + strRevealDirTemp;
            }
        }

        File revealdir = new File(strRevealDir);

        if (!revealdir.exists()) {
            revealdir.mkdirs();
            // Log.i(Global.TAG, "Create reveal dir on sdcard ok");
        }

        File imagesDir = new File(strRevealDir + "images/");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
            // Log.i(Global.TAG, "Create images dir on sdcard ok");
        }
    }

    /**
     * Displays an error message and optionally the associated exception that caused it in an alert dialog
     * 
     * @param ctx
     *            context
     * @param t
     *            exception (can be null)
     * @param messageFormat
     *            the message format string
     * @param messageArgs
     *            (optional) arguments to the message format string
     */
    public static void displayError(final Context ctx, final Throwable t, final String messageFormat,
            final Object... messageArgs) {
        final Activity activity = ctx instanceof Activity ? (Activity) ctx : Main.getMainApplication();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                String message = messageFormat == null ? "" : MessageFormat.format(messageFormat, messageArgs);
                if (t != null) {
                    message += "\n\n" + t;
                }

                LayoutInflater factory = LayoutInflater.from(activity);
                final View contentView = factory.inflate(R.layout.view_display_error, null);
                final TextView messageView = (TextView) contentView.findViewById(R.id.display_error_msg);
                messageView.setText(message);

                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle(ctx.getResources().getString(R.string.error_title));
                builder.setView(contentView);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked OK so do some stuff */
                    }
                });
                builder.show();
            }
        });
    }

    private volatile static long lastTimeStamp = System.currentTimeMillis();

    /**
     * Get a timestamp that is different than any other that we have seen since the process started.
     * 
     * @return unique timestamp
     */
    public static synchronized long getUniqueTimeStamp() {
        long timeStamp = System.currentTimeMillis();
        if (timeStamp <= lastTimeStamp)
            timeStamp = lastTimeStamp + 1;
        lastTimeStamp = timeStamp;
        return timeStamp;
    }

    /**
     * Gets the stack trace from a thrown object as a string
     * 
     * @param t
     *            the thrown object
     * @return the stack trace string
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Handle unexpected errors and runtime exceptions
     * 
     * @param t
     *            the exception
     */
    public static void unexpectedError(final Context ctx, final Throwable t, final String... strings) {
        Activity activity;

        Log.e(TAG, Util.getStackTrace(t));

        if (ctx instanceof Activity) {
            activity = (Activity) ctx;
        } else {
            activity = Main.getMainApplication();
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                ErrorDialog.start(ctx, t, strings);
            }

        });
    }

    /**
     * Look up the book name based on the ybk file name.
     * 
     * @param ctx
     *            the context
     * @param name
     *            the ybk file name (without the path)
     * @return the book name if found, name if not found
     */
    public static String lookupBookName(Context ctx, String name) {
        String bookName = name.replaceAll(".*/", "");

        try {
            URL searchUrl = new URL(TitleBrowser.TITLE_LOOKUP_URL
                    + bookName.replaceAll(".ybk$", "").replaceAll("&", "&amp;"));

            URLConnection connection = searchUrl.openConnection();
            connection.setConnectTimeout(TitleBrowser.POPULATE_TIMEOUT);

            InputStream in = connection.getInputStream();

            int length = 0;
            byte[] buffer = new byte[1024];

            length = in.read(buffer);

            if (length > 0) {
                bookName = new String(buffer, 0, length);
            }

            in.close();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return bookName;
    }

    /**
     * Delete files that match a pattern.
     * 
     * @param dir
     *            directory
     * @param pattern
     *            regular expression
     */
    public static void deleteFiles(File dir, String pattern) {
        Pattern filter = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (filter.matcher(file.getName()).matches()) {
                    if (!file.delete())
                        file.deleteOnExit();
                }
            }
        }
    }

    /**
     * Wrapper for String.substring(start) that returns a substring that is not dependent on the buffer of the original
     * and therefore doesn't keep the larger buffer from being garbage collected.
     * 
     * @param string
     * @param start
     * @return the substring
     */
    public static String independentSubstring(String string, int start) {
        return new String(string.substring(start).toCharArray());
    }

    /**
     * Wrapper for String.substring(start, end) that returns a substring that is not dependent on the buffer of the
     * original and therefore doesn't keep the larger buffer from being garbage collected.
     * 
     * @param string
     * @param start
     * @param end
     * @return the substring
     */
    public static String independentSubstring(String string, int start, int end) {
        return new String(string.substring(start, end).toCharArray());
    }

    private static Object htmlSchema = null;

    /**
     * Gets an instance of the TagSoup Html Parser that is built-in to Android Framework, but not directly exposed. @
     * return Html Parser
     */
    @SuppressWarnings("unchecked")
    public static XMLReader getHtmlSAXParser() {
        try {
            if (htmlSchema == null) {
                Class<?> htmlSchemaClass = Class.forName("org.ccil.cowan.tagsoup.HTMLSchema");
                htmlSchema = htmlSchemaClass.newInstance();
                // TODO - need to add in definitions for the custom elements
                // found in a ybk
            }
            Class<? extends XMLReader> htmlParserClass = (Class<? extends XMLReader>) Class
                    .forName("org.ccil.cowan.tagsoup.Parser");
            XMLReader parser = htmlParserClass.newInstance();
            parser.setProperty("http://www.ccil.org/~cowan/tagsoup/properties/schema", htmlSchema);
            return parser;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start flurry session.
     * 
     * @param context
     */
    public static void startFlurrySession(Context context) {
        boolean BOOLdisableAnalytics;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        BOOLdisableAnalytics = mSharedPref.getBoolean("disable_analytics", false);

        if (Global.DEBUG == 0) {
            // Release Key for use of the END USERS
            if (BOOLdisableAnalytics) {
                FlurryAgent.onStartSession(context, "BLRRZRSNYZ446QUWKSP4");
                FlurryAgent.setReportLocation(false);
                FlurryAgent.onEvent("LocationDisabled");
            } else {
                FlurryAgent.onStartSession(context, "BLRRZRSNYZ446QUWKSP4");
                FlurryAgent.onEvent("LocationEnabled");
            }
        } else {
            // Development key for use of the DEVELOPMENT TEAM
            if (BOOLdisableAnalytics) {
                FlurryAgent.onStartSession(context, "VYRRJFNLNSTCVKBF73UP");
                FlurryAgent.setReportLocation(false);
            } else {
                FlurryAgent.onStartSession(context, "VYRRJFNLNSTCVKBF73UP");
            }
        }
    }

}
