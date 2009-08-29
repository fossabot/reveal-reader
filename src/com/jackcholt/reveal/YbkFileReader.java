package com.jackcholt.reveal;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
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
 * @auther Shon Vella
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

    public static final int INDEX_FILENAME_STRING_LENGTH = 48;
    public static final int INDEX_POS_OFFSET = 48;
    public static final int INDEX_LENGTH_OFFSET = 52;
    public static final int INDEX_RECORD_LENGTH = 56;

    private static final String BINDING_FILENAME = "\\binding.html";
    private static final String BOOKMETADATA_FILENAME = "\\bookmetadata.html";
    private static final String ORDER_CONFIG_FILENAME = "\\order.cfg";

    private RandomAccessFile mFile;

    private FileChannel mChannel;

    private String mFilename;

    private long mBookId = -1;

    private Context mCtx;

    private String mCharset = DEFAULT_YBK_CHARSET;
    
    private String mTitle;
    
    private String mShortTitle;

    private Chapter mChapters[];

    private int mOrder[];

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
        mChannel = file.getChannel();
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
                close();
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
        if (mChannel != null) {
            try {
                mChannel.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close ybk channel: " + Util.getStackTrace(e));
            }
            mChannel = null;
        }
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
        long fileLength = mChannel.size();
        if (fileLength < 4) {
            throw new InvalidFileFormatException("Index is damaged or incomplete.");
        }

        try {
            byte arrayBuf[] = new byte[4];
            ByteBuffer buf = ByteBuffer.wrap(arrayBuf);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            mChannel.read(buf);
            buf.flip();
            int indexLength = buf.getInt(0);

            if (indexLength > fileLength - 4 || (indexLength % INDEX_RECORD_LENGTH != 0)) {
                throw new InvalidFileFormatException("Index is damaged or incomplete.");
            }

            int chapterCount = indexLength / INDEX_RECORD_LENGTH;
            
            arrayBuf = new byte[indexLength];
            buf = ByteBuffer.wrap(arrayBuf);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            mChannel.read(buf);
            buf.flip();

            Chapter chapters[] = mChapters = new Chapter[chapterCount];
            
            Chapter orderChapter = null;
            Chapter bindingChapter = null;

            // Read the index information
            for (int i = 0; i < chapterCount; i++) {
                int pos = i * INDEX_RECORD_LENGTH;
                
                Chapter chapter = chapters[i] = Chapter.fromYbkIndex(arrayBuf, pos, mCharset);
                
                // check that the offsets could possibly be legal
                if (chapter.offset < indexLength + 4 || chapter.length < 0 || chapter.offset + chapter.length > fileLength) {
                    Log.e(TAG, this.mFilename + ": Internal file " + chapter.fileName + " is missing or incomplete.");
                } else if (chapter.fileName.startsWith(BINDING_FILENAME)) {
                    bindingChapter = chapter;
                } else if (chapter.fileName.startsWith(ORDER_CONFIG_FILENAME)) {
                    orderChapter = chapter;
                }

            }
            
//            Arrays.sort(chapters, Chapter.chapterNameComparator);
            
            if (bindingChapter != null) {
                String bindingText = readInternalFile(bindingChapter.offset, bindingChapter.length);
                String bookTitle = null;
                String shortTitle = null;

                if (bindingText != null) {
                    bookTitle = Util.getBookTitleFromBindingText(bindingText);
                    if (bookTitle.length() == 0) {
                        bookTitle = new File(mFilename).getName();
                    }
                    shortTitle = Util.getBookShortTitleFromBindingText(bindingText);
                    if (shortTitle.length() == 0) {
                        shortTitle = new File(mFilename).getName().replaceFirst("(?s)\\..*", "");
                    }
                }
                mTitle = bookTitle;
                mShortTitle = shortTitle;
            }
            if (orderChapter != null)
                populateOrder(readInternalFile(orderChapter.offset, orderChapter.length), chapters);
            else
                mOrder = new int[0];
        } catch (IllegalArgumentException iae) {
            throw new InvalidFileFormatException("Index is damaged or incomplete.");
        }
    }
        
    private void populateOrder(final String orderString, Chapter[] chapters) {
        if (null != orderString) {
            int order = 0;
            int orderList[] = new int[chapters.length];
            Chapter cmpChapter = new Chapter();
            
            StringTokenizer tokenizer = new StringTokenizer(orderString, ",");
            while (tokenizer.hasMoreTokens()) {
                cmpChapter.orderName = tokenizer.nextToken().toLowerCase();
                int chapterIndex = Arrays.binarySearch(mChapters, cmpChapter, Chapter.orderNameComparator);
                if (chapterIndex >= 0) {
                    orderList[order++] = chapterIndex; 
                    mChapters[chapterIndex].orderNumber = order;
                }
            }
            if (order == mChapters.length) {
                mOrder = orderList;
            } else {
                mOrder = new int[order];
                System.arraycopy(orderList, 0, mOrder, 0, order);
            }
                
        } else {
            mOrder = new int[0];
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
    public long populateBook() throws IOException {
        String fileName = mFilename;
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        populateFileData();

        long bookId = 0;
        try {
            bookId = mBookId = ybkDao.insertBook(fileName, mCharset, mTitle, mShortTitle, mChapters, mOrder);
        } finally {
            if (bookId == 0) {
                // we'll assume the fileName is already in the db and continue
                Log.w(TAG, "Unable to insert book into the database.");
            }
        }
        return bookId;
    }

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
    public String readInternalFile(String chapName) throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;

        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        Chapter chap = ybkDao.getChapter(mBookId, chapName);
        if (chap != null) {
            offset = chap.offset;
            len = chap.length;
            fileText = readInternalFile(offset, len);
        }

        return fileText;
    }

    
    /**
     * The brains behind reading YBK file chapters (or internal files).
     *
     * @param offset the bytes offset into the ybk where the chapter starts
     * @param length the byte length of the chapter
     * @return The text of the chapter.
     * @throws IOException
     *             If the chapter cannot be read.
     */
    public String readInternalFile(int offset, int length) throws IOException {
        byte buf[] = readChunk(offset, length);
        String fileText;
        if (length > 1 && buf[0] == 31 && buf[1] == (byte) 139) {
            fileText = Util.decompressGzip(buf, mCharset);
        } else {
            fileText = new String(buf, mCharset);
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
    public String readMetaData() throws IOException {
        return readInternalFile(BOOKMETADATA_FILENAME);
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
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        Chapter chap = ybkDao.getChapter(mBookId, chapterName);
        byte bytes[] = null;
        if (chap != null) {
            bytes = readChunk(chap.offset, chap.length);
        }
        return bytes;
    }
    
    /**
     * The brains behind reading YBK file chapters (or internal files).
     *
     * @param offset the bytes offset into the ybk where the chunk starts
     * @param length the byte length of the chunk
     * @return the chunk
     * @throws IOException
     *             If the chapter cannot be read.
     */
    private byte [] readChunk(int offset, int length) throws IOException {
        byte arrayBuf[] = new byte[length];
        ByteBuffer buf = ByteBuffer.wrap(arrayBuf);
        int amountRead = mChannel.read(buf, offset);
        if (amountRead < length) {
            throw new EOFException();
        }
        return arrayBuf;
    }

}
