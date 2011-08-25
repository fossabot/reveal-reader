package com.jackcholt.reveal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jackcholt.reveal.R.style;
import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.AnnotHilite;
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
    public static final String DOWNLOAD_MIRROR = "http://revealreader.thepackhams.com/ebooks/";
    public static final String EMPTY_STRING = new String();
    public static final String NIGHT_MODE_STYLE = "body {background-color:black;color:#b3b3b3;} "
            + "hr{border:1px solid #564129} a:link{color:#6699cc} a:visited{color:#336699}" + "a:active{color:#3399ff}";

    public static void displayToastMessage(String message) {
        Toast.makeText(Main.getMainApplication(), message, Toast.LENGTH_LONG).show();
    }

    public static int getTheme(SharedPreferences prefs) {
        return prefs.getBoolean("enable_night_mode", false) ? style.Theme_NightMode : style.Theme_DayMode;
    }

    public static void setTheme(SharedPreferences prefs, Activity view) {
        view.setTheme(getTheme(prefs));
    }

    /**
     * Check for network connectivity before trying to go to the net and hanging :) hitting F8 in the emulator will turn
     * network on/off.
     * 
     * @author Dave Packham
     * @author Jack C. Holt
     */
    public static boolean areNetworksUp(Context context) {

        if (!isNetworkUp(context, ConnectivityManager.TYPE_MOBILE)
                && !isNetworkUp(context, ConnectivityManager.TYPE_WIFI)) {
            return false;
        }

        /*
         * just because the network transport layer is up doesn't mean we have an actual connection to the internet. if
         * the user does not have a data plan with their provider, for example the network layer will report up even
         * though we can't connect to the internet
         */
        InputStream streamVersion = getVersionInputStream();
        if (null == streamVersion) {
            return false;
        }
        try {
            return (canGetVersionManifest(streamVersion));
        } finally {
            try {
                if (null != streamVersion)
                    streamVersion.close();
            } catch (IOException e) {
            }
        }
    }

    private static boolean canGetVersionManifest(InputStream streamVersion) {
        boolean succeeded = false;
        try {
            succeeded = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(streamVersion)
                    .getElementsByTagName("manifest").getLength() > 0;
        } catch (SAXException e) {
        } catch (IOException e) {
        } catch (ParserConfigurationException e) {
        } catch (FactoryConfigurationError e) {
        } catch (DOMException e) {
        }
        return succeeded;
    }

    private static InputStream getVersionInputStream() {
        InputStream streamVersion = null;
        URL urlVersion = null;
        try {
            urlVersion = new URL("http://revealreader.thepackhams.com/revealVersion.xml?ClientVer="
                    + Global.SVN_VERSION);
        } catch (MalformedURLException e) {
            assert false : "We should never get here since we hardcoded the URL";
        }
        if (urlVersion != null) {
            try {
                URLConnection cnVersion = urlVersion.openConnection();
                cnVersion.setReadTimeout(10000);
                cnVersion.setConnectTimeout(10000);
                cnVersion.setDefaultUseCaches(false);
                cnVersion.connect();
                streamVersion = cnVersion.getInputStream();
            } catch (IOException ioe) {
            }
        }
        return streamVersion;
    }

    private static boolean isNetworkUp(Context context, int netType) {
        if (null == context) {
            throw new IllegalArgumentException("Context is null");
        }

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == connMgr || null == connMgr.getNetworkInfo(netType)) {
            return false;
        }

        return connMgr.getNetworkInfo(netType).getState() == NetworkInfo.State.CONNECTED;
    }

    /**
     * Remove HTML, surrounding quotes and Title case a book title.
     * 
     * @param title The unformatted title.
     * @return The formatted title.
     */
    public static final String formatTitle(final String title) {
        StringBuffer sb = new StringBuffer();
        // remove html tags and convert character references, and convert to lower case
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
                        // if the word starts with a special character, capitalize the actual first letter
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
     * @param binding The binding text
     * @return The title of the book.
     */
    public static final String getBookTitleFromBindingText(String binding) {
        return formatTitle(binding);
    }

    /**
     * Parses the binding text from BINDING.HTML to get the Book Title.
     * 
     * @param binding The binding text
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
        final String htmExt = "\\.htm";
        final String zeroOrMoreChars = ".*";
        final String firstGroup = "$1";

        return Html.fromHtml(
                binding.replaceAll(start + caseInsensSingleLineFlags + zeroOrMoreChars + "<a" + oneOrMoreSpaces
                        + "href=" + singleOrDoubleQuote + oneOrNoBangs + shortTitleGroup + htmExt + zeroOrMoreChars
                        + ">" + zeroOrMoreChars, firstGroup)).toString();
    }

    /**
     * Uncompress a GZip file that has been converted to a byte array.
     * 
     * @param buf The byte array that contains the GZip file contents.
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
     * @param ba The byte array to read from.
     * @param pos The position in <code>ba</code> to start from.
     * @return An array of four bytes which are in least to greatest significance order.
     * @throws IOException When the DataInputStream &quot;is&quot; cannot be read from.
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
     * @param is the InputStream from which to read.
     * @return An array of four bytes which are in least to greatest significance order.
     * @throws IOException When the DataInputStream &quot;is&quot; cannot be read from.
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
     * @param is The DataInputStream to read from.
     * @return The numeric value of the four bytes.
     * @throws IOException If the input stream is not readable.
     */
    public static final int readVBInt(RandomAccessFile is) throws IOException {
        return readVBInt(makeVBIntArray(is));
    }

    /**
     * Read in the four bytes of VB Long as stored in the YBK file. VB Longs are stored as bytes in least significant
     * byte (LSB) &quot;little endian&quot; order.
     * 
     * @param bytes byte array to read from.
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
     * @param bytes byte array to read from.
     * @return The numeric value of the four bytes.
     */
    public static final int readVBInt(final byte[] bytes, int off) {
        int i = ((int) bytes[off++]) & 0xFF;
        i += (((int) bytes[off++]) & 0xFF) << 8;
        i += (((int) bytes[off++]) & 0xFF) << 16;
        i += (((int) bytes[off++]) & 0xFF) << 24;
        return i;
    }

    // java script that implements the ifvar show/hide functionality
    private static final String IFVAR_SCRIPT = "function chcss(c,e,v) {" + //
            " var ss = document.styleSheets;" + //
            " for (var S = 0; S < ss.length; S++) {" + //
            "  for (var R = 0; R < ss[S].rules.length; R++) {" + //
            "   var r = ss[S].rules[R];" + //
            "   if (r.selectorText == c) {" + //
            "    if(r.style[e]) {" + //
            "     r.style[e] = v;" + //
            "     return;" + //
            "    }" + //
            "   }" + //
            "  }" + //
            " }" + //
            "}" + //
            "function showSpan(v) {" + //
            " chcss('._show' + v, 'display', 'inline');" + //
            " chcss('._hide' + v, 'display', 'none');" + //
            "}" + //
            "function hideSpan(v) {" + //
            " chcss('._show' + v, 'display', 'none');" + //
            " chcss('._hide' + v, 'display', 'inline');" + //
            "}";

    /**
     * Show the annotation mark and highlight all verses in a chapter for which such have been defined.
     * 
     * @param content the chapter that will have highlighting inserted.
     * @param ahList All the highlighting and annotation for the chapter contained in <code>content</code>.
     * @param _this The context in which to display a Toast if necessary.
     * @return The modified content.
     */
    public static final String annotHiliteContent(final String content, final ArrayList<AnnotHilite> ahList,
            Context _this) {
        if (null == ahList) {
            return null;
        }

        StringBuilder newContent = new StringBuilder();

        int verseStartPos = 0;
        int verseEndPos = 0;
        Matcher endMatcher = Pattern.compile("(?is)<br.*|</p.*").matcher(content);
        
        for (AnnotHilite ahItem : ahList) {
            final int startPos = verseEndPos;
            Matcher startMatcher = Pattern.compile(getVerseAnchorTagRegExp(ahItem.verse)).matcher(content);
            if (startMatcher.find(startPos)) {
                verseStartPos = startMatcher.start();

                if (endMatcher.find(verseStartPos)) {
                    verseEndPos = endMatcher.start();
                }
            }

            // append the new text.
            if (!isVersePosValid(verseStartPos, verseEndPos, startPos, content.length())) {
                if (null != _this) {
                    Toast.makeText(_this, R.string.cannot_hilite, Toast.LENGTH_LONG).show();
                }
                return content;
            }
            
            String matchGroup;
            try {
                matchGroup = endMatcher.group();
            } catch (IllegalStateException ise) {
                Log.w(TAG, "No verse seperator tags were found (<br/> or </p>)");
                newContent.append(content.subSequence(startPos, verseEndPos));
                continue;
            }
            newContent.append(content.substring(startPos, verseStartPos)).append(
                    annotHiliteVerse(content.substring(verseStartPos, verseEndPos), ahItem, matchGroup
                            .toLowerCase().contains("</p")));
        }

        return newContent.append(content.substring(verseEndPos)).toString();
    }

    private static boolean isVersePosValid(int verseStartPos, int verseEndPos, int startPos, int contentLen) {
        return startPos >= 0 && startPos <= verseStartPos && verseStartPos <= verseEndPos && verseEndPos <= contentLen;
    }

    /**
     * Find and set the verse to be highlighted and insert an image indicator if there is an associated note.
     * 
     * @param content The content of the chapter that contains the verse to be highlighted and annotated.
     * @param ah The AnnotHilite object that contains the annotation and highlight data.
     * @return The chapter with the verse highlighted.
     */
    public static StringBuilder annotHiliteVerse(String content, final AnnotHilite ah, boolean endsWithPara) {
        if (ah.color == Color.TRANSPARENT && ah.note.length() == 0) {
            return new StringBuilder(content);
        }

        String annot = (ah.note.length() > 0) ? " <img src='file:///android_asset/note.png'/> " : "";
        String colorHex = Integer.toHexString(ah.color);
        String hiliteDivStart = "";
        String hiliteDivEnd = "";
        if (colorHex.length() > 2) {
            hiliteDivStart = (ah.color == Color.TRANSPARENT) ? "" : "<div style=\"background:#" + colorHex.substring(2)
                    + ";color:black;" + (endsWithPara ? "" : "margin-top:-1.2em;") + "margin-bottom:-1.2em;\">";
            hiliteDivEnd = (ah.color == Color.TRANSPARENT) ? "" : "</div>";
        }

        return new StringBuilder().append(hiliteDivStart).append(annot).append(content).append(hiliteDivEnd);
    }

    private static String getVerseAnchorTagRegExp(int verse) {
        return "(?is)<a\\s+href=\"@" + Integer.toString(verse) + ",\\d+,\\d+\">.*";
    }

    public static final String htmlize(final String text, final SharedPreferences sharedPref, String navFile) {
        if (text == null) {
            throw new IllegalArgumentException("No text was passed.");
        }

        boolean showPicture = sharedPref.getBoolean("show_pictures", true);
        boolean showAH = sharedPref.getBoolean("show_ah", false);
        boolean nightMode = sharedPref.getBoolean("enable_night_mode", false);
        boolean touchable = sharedPref.getBoolean("make_touchable", false);

        String content = text;
        int pos = content.indexOf("<end>");

        if (pos != -1) {
            content = content.substring(pos + 5);
        }

        String style = "<style>" //
                + "._showpicture {" + (showPicture ? "display:inline;" : "display:none") + "} "
                + "._hidepicture {"
                + (showPicture ? "display:none;" : "display:inline") + "} "
                + " ._showcontents {display:inline}"
                + " ._hidecontents {display:none} .ah {" + (showAH ? "display:inline;" : "display:none")
                + "}"
                + (nightMode ? NIGHT_MODE_STYLE : "");

        /*
         * if navFile is not "0" convert all link anchor tags to block level entities with a larger font that resembles
         * list view entries.
         */
        if (touchable && !navFile.equals("0")) {
            style += "a[href]{display:block;width:100%;text-decoration:none;font-size:150%;background:#eee;"
                    + "margin-bottom:.2em;padding:.2em;}";
        }
        style += "</style>";

        String scripts = "<script type='text/javascript'>";
        if (content.indexOf("class=\"_show") != -1) {
            scripts += IFVAR_SCRIPT;
        }
        scripts += "</script>";

        return "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" + style + scripts
                + "</head><body>" + content + "</body></html>";
    }

    public static final HashMap<String, String> getFileNameChapterFromUri(final String uri, final boolean isGzipped) {

        HashMap<String, String> map = new HashMap<String, String>();

        int ContentUriLength = YbkProvider.CONTENT_URI.toString().length();
        String dataString = uri.substring(ContentUriLength + 1).replace("%20", " ");

        String[] urlParts = dataString.split("/");

        String book = urlParts[0] + ".ybk";

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
     * @param content HTML to process.
     * @param contRes Reference to the environment in which we are working.
     * @return The processed content.
     * @throws IOException
     */
    public static String processIfbook(final String content, final Context ctx) throws IOException {

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

                        Book book = ybkDao.getBook(bookName + ".ybk");
                        if (book != null) {
                            newContent.append(oldContent.substring(gtPos + 1, elsePos));
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
                oldContent.delete(0, 8);
                oldLowerContent.delete(0, 8);
            }
        }
        // copy the remaining content over
        return newContent.append(oldContent).toString();
    }

    /**
     * Convert ahtags into span tags using &quot;ah&quot; as the class and making the id &quot;ah&quot; appended by the
     * number of the ahtag.
     * 
     * @param content The content containing the ahtags to convert.
     * @return The converted content.
     */
    /*
     * public static String convertAhtag(final String content) { String fixedContent =
     * content.replaceAll("<ahtag num=(\\d+)>(.+)</ahtag>", "<span class=\"ah\" id=\"ah$1\">$2</span>");
     * 
     * return fixedContent;
     * 
     * }
     */

    /**
     * Convert ifvar tags into span tags using &quot;ah&quot; as the class and making the id &quot;ah&quot; appended by
     * the number of the ahtag.
     * 
     * @param content The content containing the ahtags to convert.
     * @return The converted content.
     * @throws InvalidFileFormatException If content is in the wrong format.
     */
    public static String convertIfvar(final String content) throws InvalidFileFormatException {
        final String flagCaseInsensitiveSingleLine = "(?is)";
        final String spanNameCapturingGroup = "(\\w+)";
        final String lazyOneOrMoreAnyCharacterCapturingGroup = "(.+?)";
        final String lazyZeroOrMoreAnyCharacterCapturingGroup = "(.*?)";
        final String oneOrMoreSpaceCharacter = "\\s+";
        final String singleOrDoubleQuote = "['\"]";
        final String plusSign = "[+]";
        final String spanName = "\\1"; // using a back reference

        String findString = flagCaseInsensitiveSingleLine + //
                "<ifvar=" + spanNameCapturingGroup + ">" + //
                lazyZeroOrMoreAnyCharacterCapturingGroup + //
                "<a" + //
                oneOrMoreSpaceCharacter + //
                "href=" + singleOrDoubleQuote + plusSign + spanName + "=0" + singleOrDoubleQuote + ">" + //
                lazyZeroOrMoreAnyCharacterCapturingGroup + //
                "</a>" + lazyOneOrMoreAnyCharacterCapturingGroup + //
                "<elsevar=" + spanName + ">" + //
                lazyZeroOrMoreAnyCharacterCapturingGroup + //
                "<a" + //
                oneOrMoreSpaceCharacter + //
                "href=" + singleOrDoubleQuote + plusSign + spanName + "=1" + singleOrDoubleQuote + ">" + //
                lazyOneOrMoreAnyCharacterCapturingGroup + //
                "</a>" + //
                lazyZeroOrMoreAnyCharacterCapturingGroup + //
                "<endvar=" + spanName + ">";

        // Log.d(TAG, "findString: " + findString);

        String fixedString = content
                .replaceAll(
                        findString,
                        "<span class=\"_show$1\">$2<a href=\"javascript:hideSpan('$1')\">"
                                + "$3</a>$4</span><span class=\"_hide$1\">$5<a href=\"javascript:showSpan('$1')\">$6</a>$7</span>");

        // Log.d(TAG, "fixedString: " + fixedString);

        return fixedString;

    }

    /**
     * Download and install title into library. Used by the title browser thread.
     * 
     * @param fileName Target file name
     * @param libDir library directory
     * @param context the caller's context
     * @param callbacks
     * @return list of file paths to add to library
     * @throws IOException if download fails
     */
    public static List<String> fetchTitle(final File fileName, final String libDir, final Context context,
            Completion... callbacks) throws IOException {

        boolean success = false;
        boolean isZip = false;

        ArrayList<File> files = new ArrayList<File>();

        File tempFile = new File(libDir, fileName.getName() + TMP_EXTENSION);
        FileOutputStream out = null;
        InputStream in = null;

        // Get the file that was referred to us
        final byte[] buffer = new byte[512];
        try {
            URLConnection connection = new URL(DOWNLOAD_MIRROR
                    + URLEncoder.encode(fileName.getName(), "UTF-8").replace("+", "%20")).openConnection();
            // set timeouts for 5 minutes. This will give generous time to deal with network and server glitches
            // but won't cause us to block forever
            connection.setConnectTimeout(300000);
            connection.setReadTimeout(300000);

            if (!hasEnoughDiskspace(connection)) {
                Toast.makeText(context, R.string.sdcard_full, Toast.LENGTH_LONG);
                return null;
            }

            in = connection.getInputStream();
            if (in == null) {
                // getInputStream isn't suppose to return null, but we sometimes get a null pointer exception later on
                // that could only happen if it does. Best guess is that it happens with HTTP responses that don't
                // actually have content, but by throwing an exception with the response message we might be able to
                // diagnose what is going on.
                throw new FileNotFoundException(((HttpURLConnection) connection).getResponseMessage());
            }
            Log.d(TAG,
                    "download from " + DOWNLOAD_MIRROR
                            + URLEncoder.encode(fileName.getName(), "UTF-8").replace("+", "%20"));

            out = new FileOutputStream(tempFile);

            int bytesRead = 0;
            long totalBytesRead = 0;
            while (-1 != (bytesRead = in.read(buffer, 0, buffer.length))) {
                if ((0 == totalBytesRead) && hasZipHeader(buffer)) {
                    isZip = true;
                }
                totalBytesRead += bytesRead;
                out.write(buffer, 0, bytesRead);
                for (Completion callback : callbacks) {
                    callback.completed(
                            true,
                            BigInteger.valueOf(totalBytesRead * 100).divide(
                                    BigInteger.valueOf(connection.getContentLength()))
                                    + "%");
                }
            }
            success = true;
        } catch (IOException ioe) {
            ReportError.reportError("eBook error: " + fileName.getName() + ".\n" + ioe.getMessage(), false);
            throw ioe;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (!success && tempFile.exists()) {
                tempFile.delete();
            }
        }

        if (!tempFile.exists()) {
            return new ArrayList<String>();
        }

        if (isZip) {
            ZipInputStream zip = new ZipInputStream(new FileInputStream(tempFile));
            try {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String entryName = entry.getName();

                    // unpack ybk files only
                    if (!entryName.endsWith(".ybk")) {
                        continue;
                    }
                    if (entryName.contains("/")) {
                        entryName = entryName.substring(entryName.lastIndexOf('/'));
                    }

                    File file = new File(libDir + entryName);

                    // check to see if they already have this title
                    if (file.exists()) {
                        file.delete();
                        YbkDAO.getInstance(context).deleteBook(entryName);
                    }

                    file = new File(libDir + entryName + TMP_EXTENSION);
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
            } catch (IOException ioe) {
                Log.w(TAG, ioe.toString());
                throw ioe;
            } finally {
                zip.close();
            }
        } else {
            files.add(tempFile);
        }

        ArrayList<String> downloaded = new ArrayList<String>();
        for (File file : files) {
            // rename from tmp
            String realNameString = file.getAbsolutePath();
            realNameString = realNameString.substring(0, realNameString.lastIndexOf(TMP_EXTENSION));
            File realName = new File(realNameString);
            file.renameTo(realName);
            downloaded.add(realName.getName());
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }

        return downloaded;
    }

    private static boolean hasEnoughDiskspace(URLConnection connection) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }

        String extDir = Environment.getExternalStorageDirectory().getName();
        
        if (TextUtils.isEmpty(extDir)) {
            return false;
        }

        if (!extDir.startsWith("/")) {
            extDir = "/" + extDir;
        }
        
        StatFs statFs;
        try {
            statFs = new StatFs(extDir);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("The directory " + extDir + " is invalid.", iae);
        }

        return BigInteger
                .valueOf(connection.getContentLength())
                .multiply(BigInteger.valueOf(2))
                .compareTo(
                        BigInteger.valueOf(statFs.getAvailableBlocks()).multiply(
                                BigInteger.valueOf(statFs.getBlockSize()))) == -1;
    }

    private static boolean hasZipHeader(byte[] buffer) {
        return buffer[0] == 0x50 && buffer[1] == 0x4b && buffer[2] == 0x03 && buffer[3] == 0x04;
    }

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
     * @param ctx The context in which this notification is being sent. Usually the Activity.
     * @param text The text of the notification.
     * @param iconId The id of icon to use in the notification.
     * @param title The header title of the notification.
     * @param notifId The number you would like to use to identify this notification.
     * @param notifMgr The NotificationManager to send the notification through.
     * @param classToStart The class to start when the notification is tapped on.
     * @param autoCancel True if the notification should automatically disappear from the queue when tapped on.
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
     * @param ctx The context in which we are running.
     */
    public static void createDefaultDirs(final Context ctx) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strRevealDir = sharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

        if (!strRevealDir.startsWith(Environment.getExternalStorageDirectory().toString())) {
            String strRevealDirTemp = strRevealDir;

            if (!strRevealDir.startsWith("/")) {
                strRevealDir = Environment.getExternalStorageDirectory().toString() + "/" + strRevealDirTemp;
            } else {
                strRevealDir = Environment.getExternalStorageDirectory().toString() + strRevealDirTemp;
            }
        }

        File revealdir = new File(strRevealDir);

        if (!revealdir.exists()) {
            revealdir.mkdirs();
            // Log.i(Global.TAG, "Create reveal dir on sdcard ok");
        }

        File imagesDir = new File(strRevealDir + ".images/");
        File thumbsDir = new File(strRevealDir + ".thumbnails/");
        if (!imagesDir.exists() || !thumbsDir.exists()) {
            imagesDir.mkdirs();
            thumbsDir.mkdirs();
            // Log.i(Global.TAG, "Create images dir on sdcard ok");
        }
    }

    /**
     * Displays an error message and optionally the associated exception that caused it in an alert dialog
     * 
     * @param ctx context
     * @param t exception (can be null)
     * @param messageFormat the message format string
     * @param messageArgs (optional) arguments to the message format string
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
     * @param t the thrown object
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
     * @param t the exception
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
     * @param ctx the context
     * @param name the ybk file name (without the path)
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
     * @param dir directory
     * @param pattern regular expression
     */
    public static void deleteFiles(File dir, String pattern) {
        File[] files = dir.listFiles();

        if (null == files) {
            return;
        }

        Pattern filter = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        for (File file : files) {
            if (filter.matcher(file.getName()).matches() && !file.delete()) {
                file.deleteOnExit();
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

    public static void thumbOnlineUpdate(final String eBookName) {

        Thread t = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                Bitmap bmImg;
                HttpURLConnection connection = null;
                try {
                    if (Util.areNetworksUp(Main.getMainApplication())) {
                        URL myFileUrl = new URL("http://revealreader.thepackhams.com/ebooks/thumbnails/" + eBookName
                                + ".jpg");
                        connection = (HttpURLConnection) myFileUrl.openConnection();

                        connection.setConnectTimeout(300000);
                        connection.setReadTimeout(300000);
                        connection.setDoInput(true);
                        connection.connect();

                        InputStream is = connection.getInputStream();

                        if (is == null) {
                            // getInputStream isn't suppose to return null, but we sometimes getting null pointer
                            // exception
                            // later on
                            // that could only happen if it does. Best guess is that it happens with HTTP responses that
                            // don't
                            // actually have content, but by throwing an exception with the response message we might be
                            // able to
                            // diagnose what is going on.
                            throw new FileNotFoundException(((HttpURLConnection) connection).getResponseMessage());
                        }
                        Log.d(TAG, "download from " + myFileUrl);

                        bmImg = BitmapFactory.decodeStream(is);

                        if (bmImg != null) {
                            byte[] b;
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bmImg.compress(Bitmap.CompressFormat.PNG, 75, bytes);
                            b = bytes.toByteArray();

                            File myFile = new File(Environment.getExternalStorageDirectory().toString()
                                    + "/reveal/ebooks/.thumbnails/" + eBookName + ".jpg");
                            myFile.createNewFile();
                            OutputStream filoutputStream = new FileOutputStream(myFile);
                            filoutputStream.write(b);
                            filoutputStream.flush();
                            filoutputStream.close();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("file", "not created");
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                        connection = null;
                    }
                }
            }
        };
        t.start();
    }

    /**
     * <p>
     * Capitalizes all the delimiter separated words in a String. Only the first letter of each word is changed. To
     * convert the rest of each word to lowercase at the same time, use {@link #capitalizeFully(String, char[])}.
     * </p>
     * 
     * <p>
     * The delimiters represent a set of characters understood to separate words. The first string character and the
     * first non-delimiter character after a delimiter will be capitalized.
     * </p>
     * 
     * <p>
     * A <code>null</code> input String returns <code>null</code>. Capitalization uses the unicode title case, normally
     * equivalent to upper case.
     * </p>
     * 
     * <pre>
     * WordUtils.capitalize(null, *)            = null
     * WordUtils.capitalize("", *)              = ""
     * WordUtils.capitalize(*, new char[0])     = *
     * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
     * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
     * </pre>
     * 
     * Shamefully ripped off from org.apache.commons.lang.WordUtils.
     * 
     * @param str the String to capitalize, may be null
     * @param delimiters set of characters to determine capitalization, null means whitespace
     * @return capitalized String, <code>null</code> if null String input
     * @see #uncapitalize(String)
     * @see #capitalizeFully(String)
     */
    public static String capitalize(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuilder buffer = new StringBuilder(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /**
     * Is the character a delimiter.
     * 
     * @param ch the character to check
     * @param delimiters the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (null == delimiters) {
            return Character.isWhitespace(ch);
        }
        for (int i = 0, isize = delimiters.length; i < isize; i++) {
            if (ch == delimiters[i]) {
                return true;
            }
        }
        return false;
    }
}
