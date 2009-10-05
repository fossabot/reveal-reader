package com.jackcholt.reveal;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jackcholt.reveal.data.Book;
import com.jackcholt.reveal.data.Chapter;
import com.jackcholt.reveal.data.ChapterIndex;
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

    private RandomAccessFile mFileIO;
    private FileChannel mChannel;

    private String mFilename;
    private File mFile;
    private Book mBook;

    private Context mCtx;

    private String mCharset = DEFAULT_YBK_CHARSET;

    private String mTitle;

    private String mShortTitle;

    private ChapterIndex mChapterIndex;

    private int mInUseCount = 0;

    private boolean mClosed = false;

    private static final int READER_CACHE_SIZE = 10;

    private static List<YbkFileReader> readerCache = new ArrayList<YbkFileReader>(READER_CACHE_SIZE);

    private static synchronized YbkFileReader getReaderFromCache(String fileName) {
        int cacheCount = readerCache.size();
        for (int i = cacheCount - 1; i >= 0; i--) {
            YbkFileReader reader = readerCache.get(i);
            if (reader.mFilename.equalsIgnoreCase(fileName)) {
                if (i != cacheCount - 1) {
                    readerCache.remove(i);
                    readerCache.add(reader);
                }
                return reader;
            }
        }
        return null;
    }

    private static synchronized void cacheReader(YbkFileReader reader) {
        while (readerCache.size() >= READER_CACHE_SIZE) {
            YbkFileReader oldReader = readerCache.remove(0);
            oldReader.close();
        }
        readerCache.add(reader);
    }

    private static synchronized void uncacheReader(String fileName) {
        int cacheCount = readerCache.size();
        for (int i = cacheCount - 1; i >= 0; i--) {
            YbkFileReader reader = readerCache.get(i);
            if (reader.mFilename.equalsIgnoreCase(fileName)) {
                readerCache.remove(i);
                reader.close();
            }
        }
    }

    /**
     * Get a YbkFileReader on the given file named <code>filename</code>. If the <code>filename</code> specified cannot
     * be found, throw a FileNotFoundException. Construct a new YbkFileReader on the given file named
     * <code>filename</code>. If the <code>filename</code> specified cannot be found, throw a FileNotFoundException.
     * 
     * @param context
     * @param fileName
     *            the filename (relative to the library directory)
     * @throws FileNotFoundException
     *             if the filename cannot be opened for reading.
     */
    public static synchronized YbkFileReader getReader(final Context ctx, final String fileName)
            throws FileNotFoundException, IOException {
        YbkFileReader reader = getReaderFromCache(fileName);
        if (reader == null) {
            reader = new YbkFileReader(ctx, fileName);
            cacheReader(reader);
        }
        reader.use();
        return reader;
    }

    /**
     * Closes and uncaches reader for given filename.
     * 
     * @param fileName
     */
    public static synchronized void closeReader(String fileName) {
        uncacheReader(fileName);
    }

    /**
     * Add a book.
     * 
     * @param context
     * @param fileName
     *            the filename (relative to the library directory)
     * @param charset
     *            the character set to use
     * @return the reader used to add the book (must be unused by the caller
     * @throws FileNotFoundException
     *             if the filename cannot be opened for reading.
     */
    public static synchronized YbkFileReader addBook(final Context ctx, final String fileName, String charset)
            throws FileNotFoundException, IOException {
        // clean up the existing book if any
        closeReader(fileName);
        YbkDAO ybkDao = YbkDAO.getInstance(ctx);
        ybkDao.deleteBook(fileName);

        YbkFileReader reader = new YbkFileReader(ctx, fileName);
        reader.mCharset = charset;
        try {
            reader.populateBook();
        } finally {
            if (reader.mBook == null) {
                reader.close();
            }
        }
        cacheReader(reader);
        reader.use();
        return reader;
    }

    /**
     * Closes and uncaches reader for given filename.
     * 
     * @param fileName
     */
    public static synchronized void closeAllReaders() {
        for (YbkFileReader reader : readerCache) {
            reader.close();
        }
        readerCache.clear();
    }

    /**
     * Construct a new YbkFileReader on the given file named <code>filename</code>. If the <code>filename</code>
     * specified cannot be found, throw a FileNotFoundException. Construct a new YbkFileReader on the given file named
     * <code>filename</code>. If the <code>filename</code> specified cannot be found, throw a FileNotFoundException.
     * 
     * @param context
     * @param fileName
     *            the filename (relative to the library directory)
     * @param charset
     *            the character set to use
     * @throws FileNotFoundException
     *             if the filename cannot be opened for reading.
     */
    private YbkFileReader(final Context ctx, final String fileName) throws FileNotFoundException, IOException {
        final String libDir = PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);

        mCtx = ctx;
        mFilename = fileName;
        mFile = new File(libDir, fileName);
        mCharset = DEFAULT_YBK_CHARSET;
        mFileIO = new RandomAccessFile(mFile, "r");
        mChannel = mFileIO.getChannel();

        mBook = YbkDAO.getInstance(ctx).getBook(fileName);
        if (mBook != null) {
            if (mBook.charset != null) {
                mCharset = mBook.charset;
            }
            mChapterIndex = YbkDAO.getInstance(ctx).getChapterIndex(fileName);
        }
    }

    /**
     * Increment use count.
     * 
     */
    public synchronized void use() {
        mInUseCount++;
    }

    /**
     * Decrement use count.
     * 
     */
    public synchronized void unuse() {
        if (--mInUseCount <= 0) {
            mInUseCount = 0;
            if (mClosed) {
                close();
            }
        }
    }

    /**
     * Closes the reader.
     * 
     * @throws IOException
     * 
     */
    private synchronized void close() {
        mClosed = true;
        if (mInUseCount <= 0) {
            if (mChannel != null) {
                try {
                    mChannel.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close ybk channel: " + Util.getStackTrace(e));
                }
                mChannel = null;
            }
            if (mFileIO != null) {
                try {
                    mFileIO.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close ybk file: " + Util.getStackTrace(e));
                }
                mFileIO = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        mInUseCount = 0;
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

            Chapter chapters[] = new Chapter[chapterCount];

            Chapter orderChapter = null;
            Chapter bindingChapter = null;

            // Read the index information
            for (int i = 0; i < chapterCount; i++) {
                int pos = i * INDEX_RECORD_LENGTH;

                Chapter chapter = chapters[i] = Chapter.fromYbkIndex(arrayBuf, pos, mCharset);
                // temporarily add negative order number so we can tell the original order
                // but distinguish it from a real order number later on
                chapter.orderNumber = -1 - i;

                // check that the offsets could possibly be legal
                if (chapter.offset < indexLength + 4 || chapter.length < 0
                        || chapter.offset + chapter.length > fileLength) {
                    Log.e(TAG, this.mFilename + ": Internal file " + chapter.fileName + " is missing or incomplete.");
                } else if (chapter.fileName.startsWith(BINDING_FILENAME)) {
                    bindingChapter = chapter;
                } else if (chapter.fileName.startsWith(ORDER_CONFIG_FILENAME)) {
                    orderChapter = chapter;
                }

            }

            if (bindingChapter != null) {
                String bindingText = readInternalFile(bindingChapter.offset, bindingChapter.length);
                Log.d(TAG, "Binding text: " + bindingText);
                
                String bookTitle = null;
                String shortTitle = null;

                if (bindingText != null) {
                    bookTitle = Util.getBookTitleFromBindingText(bindingText);
                    if (bookTitle.length() == 0) {
                        bookTitle = new File(mFilename).getName();
                    }
                    shortTitle = Util.getBookShortTitleFromBindingText(bindingText);
                    if (shortTitle.length() == 0) {
                        Log.d(TAG,"Using the backup method of determining shortTitle");
                        shortTitle = new File(mFilename).getName().replaceFirst("(?s)\\..*", "");
                    }
                    Log.d(TAG, "shortTitle: " + shortTitle);
                }
                mTitle = bookTitle;
                mShortTitle = shortTitle;
            }
            
            String orderString = (orderChapter != null) ? readInternalFile(orderChapter.offset, orderChapter.length)
                    : Util.EMPTY_STRING;
            mChapterIndex = new ChapterIndex(chapters, orderString, mCharset);

        } catch (IllegalArgumentException iae) {
            throw new InvalidFileFormatException("Index is damaged or incomplete.");
        }
    }

    /**
     * Get and save the book information into the database.
     * 
     * @param fileName
     *            The file name of the book.
     * @return the added book objects
     * @throws IOException
     */
    public Book populateBook() throws IOException {
        String fileName = mFilename;
        populateFileData();
        mBook = YbkDAO.getInstance(mCtx).insertBook(fileName, mCharset, mTitle, mShortTitle, mChapterIndex);
        return mBook;
    }

    /**
     * Gets a chapter object.
     * 
     * @param chapName
     *            the name of the chapter
     * @return the chapter object, or null if it doesn't exist.
     */
    public Chapter getChapter(String chapName) {
        Chapter chap = null;
        try {
            chap = mChapterIndex.getChapter(mChannel, chapName);
            if (chap == null) {
                chap = mChapterIndex.getChapter(mChannel, chapName + ".gz");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chap;
    }

    /**
     * Tests for the existence of a chapter.
     * 
     * @param chapName
     *            the name of the chapter
     * @return true if the chapter exists.
     */
    public boolean chapterExists(String chapName) throws IOException {
        return getChapter(chapName) != null;
    }

    /**
     * Gets a chapter object by orderId.
     * 
     * @param orderId
     *            the chapter order id
     * @return the chapter object, or null if it doesn't exist.
     */
    public Chapter getChapterByOrder(int orderId) {
        Chapter chapter = null;
        try {
            chapter = mChapterIndex.getChapterByOrder(mChannel, orderId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chapter;
    }

    /**
     * Gets a chapter object by index.
     * 
     * @param index
     *            the chapter index
     * @return the chapter object, or null if it doesn't exist.
     */
    public Chapter getChapterByIndex(int index) {
        Chapter chapter = null;
        try {
            chapter = mChapterIndex.getChapterByIndex(mChannel, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chapter;
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
    public String readInternalFile(final String chapName) throws IOException {

        Chapter chap = getChapter(chapName);

        return (null == chap) ? null : readInternalFile(chap.offset, chap.length);
    }

    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param offset
     *            the bytes offset into the ybk where the chapter starts
     * @param length
     *            the byte length of the chapter
     * @return The text of the chapter.
     * @throws IOException
     *             If the chapter cannot be read.
     */
    public String readInternalFile(int offset, int length) throws IOException {
        byte[] buf = readChunk(offset, length);
        return (hasGzipHeader(length, buf)) ? Util.decompressGzip(buf, mCharset) : new String(buf, mCharset);
    }

    private boolean hasGzipHeader(int length, byte[] buf) {
        return length > 1 && buf[0] == 31 && buf[1] == (byte) 139;
    }

    /**
     * Return the uncompressed contents of Book Metadata internal file as a String or null if the YBK file doesn't
     * contain one.
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
        Chapter chap = mChapterIndex.getChapter(mChannel, chapterName);
        byte bytes[] = null;
        if (chap != null) {
            bytes = readChunk(chap.offset, chap.length);
        }
        return bytes;
    }

    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param offset
     *            the bytes offset into the ybk where the chunk starts
     * @param length
     *            the byte length of the chunk
     * @return the chunk
     * @throws IOException
     *             If the chapter cannot be read.
     */
    private byte[] readChunk(int offset, int length) throws IOException {
        byte[] arrayBuf = new byte[length];
        ByteBuffer buf = ByteBuffer.wrap(arrayBuf);
        int amountRead = mChannel.read(buf, offset);
        if (amountRead < length) {
            throw new EOFException("Was not able to read as many bytes the length parameter specified.");
        }
        return arrayBuf;
    }

    /**
     * Get the Book object associated with this reader.
     * 
     * @return the book
     */
    public Book getBook() {
        return mBook;
    }
}
