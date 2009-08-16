package com.jackcholt.reveal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.Chapter;
import com.jackcholt.reveal.data.YbkDAO;

/**
 * A class to do all the work of reading and accessing YBK files.
 * 
 * @author Jack C. Holt - jackcholt@gmail.com
 */
public class YbkFileReader {
    private static final String TAG = "YbkFileReader";

    /** Non navigation chapter */
    public static final int CHAPTER_TYPE_NONNAV = 0;

    /** Navigation chapter */
    public static final int CHAPTER_TYPE_NAV = 1;

    /** All links open according to Popup view settings */
    public static final int CHAPTER_TYPE_SETTINGS = 2;

    /** Zoom menu will not be available */
    public static final int CHAPTER_ZOOM_MENU_OFF = 0;

    /** Zoom menu will be available */
    public static final int CHAPTER_ZOOM_MENU_ON = 1;

    public static final String DEFAULT_YBK_CHARSET = "ISO-8859-1";

    private static final int INDEX_FILENAME_STRING_LENGTH = 48;

    private static final String BINDING_FILENAME = "\\BINDING.HTML";

    private static final String BOOKMETADATA_FILENAME = "\\BOOKMETADATA.HTML";

    private static final String ORDER_CONFIG_FILENAME = "\\ORDER.CFG";

    private static final int FROM_INTERNAL = 1;

    private static final int FROM_DB = 2;

    private RandomAccessFile mFile;

    private String mFilename;

    // private DataInputStream mDataInput;
    private int mIndexLength;

    private ArrayList<InternalFile> mInternalFiles = new ArrayList<InternalFile>();

    private String mBindingText = "No Binding Text";

    @SuppressWarnings("unused")
    private String mBookTitle = "Couldn't get the title of this book";

    @SuppressWarnings("unused")
    private String mBookShortTitle = "No Short Title";

    private String mBookMetaData = null;

    private ArrayList<Order> mOrderList = new ArrayList<Order>();

    // private String mCurrentChapterOrderName = null;
    // private int mCurrentChapterOrderNumber = -1;
    // private String mChapterNavBarTitle = "No Title";
    // private String mChapterHistoryTitle = "No Title";
    // private int mChapterNavFile = CHAPTER_TYPE_SETTINGS;
    // private int mChapterZoomPicture = CHAPTER_ZOOM_MENU_OFF;
    // private SharedPreferences mSharedPref;
    // private YbkDAO mYbkDao;
    private long mBookId = -1;

    private Context mCtx;

    private String mCharset = DEFAULT_YBK_CHARSET;

    /**
     * A class to act as a structure for holding information about the chapters
     * held inside a YBK ebook.
     * 
     */
    private class InternalFile {
        public String fileName;

        public int offset;

        public int len;

        InternalFile() {
            // do nothing
        }

        // InternalFile(final String newFileName, final int ybkOffset, final int
        // ybkLen) {
        // fileName = newFileName;
        // offset = ybkOffset;
        // len = ybkLen;
        // }
    }

    /**
     * Construct a new YbkFileReader on the given File <code>file</code>. If the
     * <code>file</code> specified cannot be found, throw a
     * FileNotFoundException.
     * 
     * @param file
     *            a File to be opened for reading characters from.
     * @throws FileNotFoundException
     *             if the file cannot be opened for reading.
     */
    private YbkFileReader(final Context ctx, final RandomAccessFile file, String charset) throws FileNotFoundException,
            IOException {
        mFile = file;
        mCtx = ctx;
        mCharset = charset == null ? DEFAULT_YBK_CHARSET : charset;
    }

    /**
     * Construct a new YbkFileReader on the given file named
     * <code>filename</code>. If the <code>filename</code> specified cannot be
     * found, throw a FileNotFoundException. Construct a new YbkFileReader on
     * the given file named <code>filename</code>. If the <code>filename</code>
     * specified cannot be found, throw a FileNotFoundException.
     * 
     * @param fileName
     *            an absolute or relative path specifying the file to open.
     * @throws FileNotFoundException
     *             if the filename cannot be opened for reading.
     */
    public YbkFileReader(final Context ctx, final String fileName, String charset) throws FileNotFoundException,
            IOException {
        this(ctx, new RandomAccessFile(fileName, "r"), charset);

        mFilename = fileName;

        YbkDAO ybkDao = YbkDAO.getInstance(ctx);
        boolean noException = false;
        Book book;
        try {
            book = ybkDao.getBook(fileName);
            noException = true;
        } finally {
            if (!noException) {
                mFile.close();
                mFile = null;
            }
        }
        if (book != null) {
            mBookId = book.id;
            if (book.charset != null) {
                mCharset = book.charset;
            }
        }
    }

    /**
     * Closes the reader.
     * 
     * @throws IOException
     * 
     */
    public void close() {
        if (mFile != null) {
            try {
                mFile.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close ybk file: " + Util.getStackTrace(e));
            }
            mFile = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Analyze the YBK file and save file contents data for later reference.
     * 
     * @throws IOException
     *             If the YBK file is not readable.
     */
    private void populateFileData() throws IOException {
        RandomAccessFile file = mFile;
        long fileLength = file.getChannel().size();
        if (fileLength < 4) {
            throw new InvalidFileFormatException("Index is damaged or incomplete.");
        }

        try {
            mIndexLength = Util.readVBInt(file);

            if (mIndexLength > fileLength - file.getFilePointer()) {
                throw new InvalidFileFormatException("Index is damaged or incomplete.");
            }

            byte[] indexArray = new byte[mIndexLength];

            file.readFully(indexArray);

            // Read the index information into the internalFiles list
            int pos = 0;

            while (pos < mIndexLength) {
                InternalFile iFile = new InternalFile();

                int fileNameStartPos = pos;

                // NOTE - there are character sets for which testing for 0 won't
                // work, but at least for the time being
                // there aren't any YBK's that use them and Yancey software
                // would choke on them too.
                while (indexArray[pos++] != 0 && pos < mIndexLength)
                    ;
                int fileNameLength = (pos - fileNameStartPos) - 1;
                iFile.fileName = new String(indexArray, fileNameStartPos, fileNameLength, mCharset);

                pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;

                iFile.offset = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
                pos += 4;

                iFile.len = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
                pos += 4;
                if (iFile.offset < mIndexLength || iFile.len < 0 || iFile.offset + iFile.len > fileLength) {
                    Log.e(TAG, this.mFilename + ": Internal file " + iFile.fileName + " is missing or incomplete.");
                    continue;
                }

                // Add the internal file into the list
                mInternalFiles.add(iFile);
            }

            mBindingText = readBindingFile(FROM_INTERNAL);
            if (mBindingText != null) {
                mBookTitle = Util.getBookTitleFromBindingText(mBindingText);
                mBookShortTitle = Util.getBookShortTitleFromBindingText(mBindingText);
                mBookMetaData = readMetaData(FROM_INTERNAL);
                populateOrder(readOrderCfg(FROM_INTERNAL));
            }
        } catch (IllegalArgumentException iae) {
            throw new InvalidFileFormatException("Index is damaged or incomplete.");
        }
    }

    private class Order {
        public String chapter;

        public int order;

        public Order(String newChapter, int newOrder) {
            chapter = newChapter;
            order = newOrder;
        }
    }

    private void populateOrder(final String orderString) {
        ArrayList<Order> orderList = new ArrayList<Order>();
        int order = 1;

        if (null != orderString) {
            // for reasons not completely understood, Scanner was very, very,
            // slow on large strings, so try using StringTokenizer instead (sv)
            // Scanner orderScan = new Scanner(orderString).useDelimiter(",");
            // while (orderScan.hasNext()) {
            // Order ord = new Order(orderScan.next(), order++);
            // orderList.add(ord);
            // }

            StringTokenizer tokenizer = new StringTokenizer(orderString, ",");
            while (tokenizer.hasMoreTokens()) {
                orderList.add(new Order(tokenizer.nextToken(), order++));
            }
            mOrderList = orderList;
        }
    }

    /**
     * Get and save the book information into the database.
     * 
     * @param fileName
     *            The file name of the book.
     * @return The id of the book that was saved into the database.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public long populateBook() throws IOException {
        String fileName = mFilename;
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        populateFileData();

        String bindingText = readBindingFile(FROM_INTERNAL);
        String bookTitle = null;
        String shortTitle = null;

        if (bindingText != null) {
            bookTitle = Util.getBookTitleFromBindingText(bindingText);
            shortTitle = Util.getBookShortTitleFromBindingText(bindingText);
        }

        List<Chapter> chapters = new ArrayList();
        List<InternalFile> ifList = mInternalFiles;
        ArrayList<Order> orderList = mOrderList;

        Order[] orderArray = orderList.toArray(new Order[orderList.size()]);

        // big books cause a memory squeeze - don't need these any more
        mOrderList = orderList = null;

        Comparator orderComp = new Comparator<Order>() {

            public int compare(Order arg0, Order arg1) {
                return arg0.chapter.compareToIgnoreCase(arg1.chapter);
            }

        };

        Arrays.sort(orderArray, orderComp);

        for (int i = 0, chapAmount = ifList.size(); i < chapAmount; i++) {
            Integer orderNbr = null;
            InternalFile iFile = ifList.get(i);
            if (iFile.fileName.length() == 0) {
                continue;
            }

            if (orderArray != null) {

                String iFileOrderString = "";
                String[] iFileOrderParts = iFile.fileName.toLowerCase().split("\\\\");
                int partLength = iFileOrderParts.length;

                if (partLength > 2) {
                    for (int k = 2; k < partLength; k++) {
                        iFileOrderString += iFileOrderParts[k] + "\\";
                    }

                    if (iFileOrderString.length() > 0) {
                        iFileOrderString = iFileOrderString.substring(0, iFileOrderString.length() - 1);
                    }

                } else {
                    Log.d(TAG, "Internal File Name: " + iFile.fileName);
                    iFileOrderString = iFile.fileName.toLowerCase().substring(1);
                }

                int dotPos = iFileOrderString.indexOf(".");
                if (dotPos != -1) {
                    iFileOrderString = iFileOrderString.substring(0, dotPos);
                    int orderNumber = Arrays.binarySearch(orderArray, new Order(iFileOrderString, 0), orderComp);

                    if (orderNumber >= 0) {
                        orderNbr = orderArray[orderNumber].order;
                    }
                } else {
                    Log.w(TAG, "Chapter is missing file extension. '" + iFileOrderString + "'");
                }

            }

            long id = Util.getUniqueTimeStamp();
            Chapter chap = new Chapter();
            chap.id = id;
            chap.fileName = iFile.fileName;
            chap.length = iFile.len;
            chap.offset = iFile.offset;
            if (orderNbr != null)
                chap.orderNumber = orderNbr;
            chapters.add(chap);
            chap = null;
        }

        // big books cause a memory squeeze - don't need these any more
        ifList = mInternalFiles = null;
        bindingText = null;
        orderList = null;

        long bookId = 0;
        try {
            bookId = mBookId = ybkDao.insertBook(fileName, mCharset, bindingText, bookTitle, shortTitle, mBookMetaData,
                    chapters);
        } finally {
            if (bookId == 0) {
                // we'll assume the fileName is already in the db and continue
                Log.w(TAG, "Unable to insert book (" + fileName + ") into the database.");
            }
        }
        return bookId;
    }

    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML. Returns null if there was an
     *         EOFException while reading the file.
     * @throws IOException
     *             If there is a problem reading the Binding file.
     */
    public String readBindingFile() throws IOException {
        return readBindingFile(FROM_DB);
    }

    public String readBindingFile(final int source) throws IOException {
        String fileText = readInternalFile(BINDING_FILENAME, source);

        if (null == fileText) {
            Log.e(TAG, "The YBK file contains no binding.html");
        }

        return fileText;

    }

    public String readInternalFile(final String chapName) throws IOException {
        return readInternalFile(chapName, FROM_DB);
    }

    Comparator<Object> iFileComp = new Comparator<Object>() {

        public int compare(Object arg0, Object arg1) {
            InternalFile if0 = (InternalFile) arg0;
            InternalFile if1 = (InternalFile) arg1;

            return if0.fileName.compareToIgnoreCase(if1.fileName);
        }

    };

    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param dataInput
     *            The input stream that is the YanceyBook.
     * @param bookId
     *            The id of the book record in the database.
     * @param chapterId
     *            The id of the chapter record in the database.
     * @return The text of the chapter.
     * @throws IOException
     *             If the chapter cannot be read.
     */
    public String readInternalFile(String chapName, final int source) throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;
        boolean fileFound = false;

        RandomAccessFile file = mFile;
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        Chapter chap;

        if (source == FROM_DB) {
            chap = ybkDao.getChapter(mBookId, chapName);
            if (chap == null) {
                chapName += ".gz";
                chap = ybkDao.getChapter(mBookId, chapName);
            }

            if (chap != null) {
                offset = chap.offset;
                len = chap.length;
                fileFound = true;
            }
        } else {

            InternalFile iFile = new InternalFile();
            iFile.fileName = chapName;
            Object[] ifArray = mInternalFiles.toArray();
            Arrays.sort(ifArray, iFileComp);
            int index = Arrays.binarySearch(ifArray, iFile, iFileComp);

            if (index >= 0) {
                InternalFile matchedFile = (InternalFile)ifArray[index];
                offset = matchedFile.offset;
                len = matchedFile.len;
                fileFound = true;
            } else {
                chapName += ".gz";
                iFile.fileName = chapName;
                index = Arrays.binarySearch(ifArray, iFile, iFileComp);
                if (index > -1) {
                    InternalFile matchedFile = (InternalFile)ifArray[index];
                    offset = matchedFile.offset;
                    len = matchedFile.len;
                    fileFound = true;
                }
            }
        }

        if (fileFound) {
            byte[] text = new byte[len];
            file.seek(offset);
            int amountRead = file.read(text);
            if (amountRead < len) {
                throw new InvalidFileFormatException("Couldn't read all of " + chapName + ".");
            }

            if (chapName.toLowerCase().endsWith(".gz") ||
            // also check for GZ header since apparently some books in the wild don't correctly label the BINDING.HTML when it is gz compressed. 
                    (len > 1 && text[0] == 31 && text[1] == (byte) 139)) {
                fileText = Util.decompressGzip(text, mCharset);
            } else {
                fileText = new String(text, mCharset);
            }
        }

        return fileText;
    }

    /**
     * Return the uncompressed contents of Book Metadata internal file as a
     * String or null if the YBK file doesn't contain one.
     * 
     * @return The uncompressed contents of the Book Metadata file.
     * @throws IOException
     *             if there is a problem reading the Book Metadata file.
     */
    private String readMetaData(final int source) throws IOException {
        return readInternalFile(BOOKMETADATA_FILENAME, source);
    }

    private String readOrderCfg(final int source) throws IOException {
        return readInternalFile(ORDER_CONFIG_FILENAME, source);
    }

    /**
     * @return the filename
     */
    public final String getFilename() {
        return mFilename;
    }

    /**
     * 
     * @param file
     * @param bookFileName
     * @param chapterName
     * @return
     * @throws IOException
     */
    public byte[] readInternalBinaryFile(final String chapterName) throws IOException {

        long bookId = mBookId;
        RandomAccessFile file = mFile;
        int offset = 0;
        int len = 0;
        byte[] bytes = null;

        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);

        Chapter chap = ybkDao.getChapter(bookId, chapterName);
        if (chap != null) {
            offset = chap.offset;
            len = chap.length;

            bytes = new byte[len];
            file.seek(offset);
            int amountRead = file.read(bytes);
            if (amountRead < len) {
                throw new IOException("Couldn't read all of " + chapterName + ".");
            }
        }

        return bytes;
    }
}
