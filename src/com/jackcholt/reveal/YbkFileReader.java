package com.jackcholt.reveal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

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
    /** Zoom menu will be available  */
    public static final int CHAPTER_ZOOM_MENU_ON = 1;
    
    private static final int INDEX_FILENAME_STRING_LENGTH = 48;
    private static final String BINDING_FILENAME = "\\BINDING.HTML";
    private static final String BOOKMETADATA_FILENAME = "\\BOOKMETADATA.HTML"; 
    private static final String ORDER_CONFIG_FILENAME = "\\ORDER.CFG";
    private static final int FROM_INTERNAL = 1;
    private static final int FROM_DB = 2;
    private RandomAccessFile mFile;
    private String mFilename;
    //private DataInputStream mDataInput;
    private int mIndexLength;
    private ArrayList<InternalFile> mInternalFiles = new ArrayList<InternalFile>();
    private String mBindingText = "No Binding Text";
    private String mBookTitle = "Couldn't get the title of this book";
    private String mBookShortTitle = "No Short Title";
    private String mBookMetaData = null;     
    private ArrayList<Order> mOrderList = new ArrayList<Order>();
    //private String mCurrentChapterOrderName = null;
    //private int mCurrentChapterOrderNumber = -1;
    //private String mChapterNavBarTitle = "No Title";
    //private String mChapterHistoryTitle = "No Title";
    //private int mChapterNavFile = CHAPTER_TYPE_SETTINGS;
    //private int mChapterZoomPicture = CHAPTER_ZOOM_MENU_OFF;
    //private SharedPreferences mSharedPref; 
    //private YbkDAO mYbkDao;
    private long mBookId = -1;  
    private Context mCtx; 
    
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
        
        InternalFile(final String newFileName, final int ybkOffset, 
                final int ybkLen) {
            fileName = newFileName;
            offset = ybkOffset;
            len = ybkLen;
        }
        
    }
    
    /**
     * Construct a new YbkFileReader on the given File <code>file</code>. If the 
     * <code>file</code> specified cannot be found, throw a FileNotFoundException.
     * 
     * @param file a File to be opened for reading characters from.
     * @throws FileNotFoundException if the file cannot be opened for 
     * reading. 
     */
    public YbkFileReader(final Context ctx, final RandomAccessFile file) 
    throws FileNotFoundException, IOException {
        mFile = file;
        mCtx = ctx;
        //populateFileData();
        
        //mSharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    /**
     * Construct a new YbkFileReader on the given file named <code>filename</code>. 
     * If the <code>filename</code> specified cannot be found, 
     * throw a FileNotFoundException.
     * 
     * @param fileName an absolute or relative path specifying the file to open.
     * @throws FileNotFoundException if the filename cannot be opened for 
     * reading. 
     */
    public YbkFileReader(final Context ctx, final String fileName) 
    throws FileNotFoundException, IOException {
        this(ctx, new RandomAccessFile(fileName, "r"));
        
        mFilename = fileName;
        
        YbkDAO ybkDao = YbkDAO.getInstance(ctx);
        Book book = ybkDao.getBook(fileName);
        if (book != null) {
            mBookId = book.id;
        }
    }
        
    /**
     * Analyze the YBK file and save file contents data for later reference.
     * @throws IOException If the YBK file is not readable.
     */
    private void populateFileData() throws IOException {
        RandomAccessFile file = mFile;
        mIndexLength = Util.readVBInt(file);
        
        byte[] indexArray = new byte[mIndexLength];
        
        file.readFully(indexArray);
        
        // Read the index information into the internalFiles list
        int pos = 0;
        
        while (pos < mIndexLength) {
            InternalFile iFile = new InternalFile();
            
            StringBuffer fileNameSBuf = new StringBuffer();
            
            byte b;
            int fileNameStartPos = pos;

            while((b = indexArray[pos++]) != 0 && pos < mIndexLength) {
                fileNameSBuf.append((char) b);                
            }
            
            iFile.fileName = fileNameSBuf.toString();
            
            pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;
            
            iFile.offset = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
            pos += 4;
            
            iFile.len = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
            pos += 4;
            
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
    }
    
    private class Order {
        public String chapter;
        public int order;
        
        public Order(String newChapter, int newOrder) {
            chapter =  newChapter;
            order = newOrder;
        }
    }
    
    private void populateOrder(final String orderString) {
        if (null != orderString) {
            Scanner orderScan = new Scanner(orderString).useDelimiter(",");
            
             
            ArrayList<Order> orderList = new ArrayList<Order>();
            
            int order = 1;
            while(orderScan.hasNext()) {
                Order ord = new Order(orderScan.next(), order++);
                orderList.add(ord);
            }
            mOrderList = orderList;
        }
    }

    /**
     * Get and save the book information into the database.
     * 
     * @param fileName The file name of the book.
     * @return The id of the book that was saved into the database.
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    public long populateBook() throws IOException {
        String fileName = mFilename;
        boolean success = true;
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        populateFileData();
        
        String bindingText = readBindingFile(FROM_INTERNAL);
        String bookTitle = null;
        String shortTitle = null;
        
        if (bindingText != null) {
            bookTitle = Util.getBookTitleFromBindingText(bindingText);
            shortTitle = Util.getBookShortTitleFromBindingText(bindingText);
        }
        
        long bookId = mBookId = ybkDao.insertBook(fileName, bindingText, bookTitle, 
                shortTitle, mBookMetaData);
        
        if (bookId == 0) {
            // we'll assume the fileName is already in the db and continue
            Log.w(TAG, "Unable to insert book (" + 
                    fileName + ") into the database.  Must already be in the database.");
        } else {
       
            List<InternalFile> ifList = mInternalFiles;
            ArrayList<Order> orderList = mOrderList;
            
            
            Order[] orderArray = orderList.toArray(new Order[orderList.size()]);
            
            Comparator orderComp = new Comparator<Order>() {

                public int compare(Order arg0, Order arg1) {
                    return arg0.chapter.compareToIgnoreCase(arg1.chapter);
                }
                
            };
            
            Arrays.sort(orderArray,orderComp);
            
            for(int i=0, chapAmount = ifList.size(); i < chapAmount; i++) {
                Integer orderNbr = null;
                InternalFile iFile = ifList.get(i);
                if (iFile.fileName.length() == 0) {
                    continue;
                }
                
                if (orderList != null) {
                    
                    String iFileOrderString = "";
                    String[] iFileOrderParts = iFile.fileName.toLowerCase().split("\\\\");
                    int partLength = iFileOrderParts.length;
                    
                    if (partLength > 2) {
                        for(int k = 2; k < partLength; k++) {
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
                
                if (!ybkDao.insertChapter(bookId, iFile.fileName, null, iFile.len, 
                        null, null, iFile.offset, null, orderNbr, null)) {
                
                    success = false;
                    break;
                }
                
            }
            
            if (success) {
                ybkDao.commit();
            } else {
                ybkDao.rollback();
                bookId = 0;
                Log.e(TAG, "The book and all its chapters could not be inserted.");
            }
            
        }
        
        return bookId;
    }

    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML.  Returns null if there was an EOFException
     * while reading the file.
     * @throws IOException If there is a problem reading the Binding file. 
     */
    public String readBindingFile() 
    throws IOException {
        return readBindingFile(FROM_DB);
    }

    public String readBindingFile(final int source) throws IOException {
        String fileText = readInternalFile(BINDING_FILENAME, source);
        
        if (null == fileText) {
            Log.e(TAG,"The YBK file contains no binding.html");
        }
        
        return fileText;
        
    }
/*    public String readInternalFile(final String iFilename) throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;
        
        RandomAccessFile file = mFile;
        
        ArrayList<InternalFile> internalFiles = mInternalFiles;
        
        int ifLength = internalFiles.size();
        for(int i=0; i < ifLength; i++) {
            InternalFile iFile = internalFiles.get(i);
            if (iFile.getFileName().equalsIgnoreCase(iFilename)) {
                offset = iFile.getYbkOffset();
                len = iFile.getYbkLen();
                     
                byte[] text = new byte[len];
                file.seek(offset);
                int amountRead = file.read(text);
                if (amountRead < len) {
                    throw new InvalidFileFormatException(
                            "Couldn't read all of " + iFilename + ".");
                }
                
                if (iFilename.toLowerCase().endsWith(".gz")) {
                    fileText = Util.decompressGzip(text);
                } else {
                    fileText = new String(text, "ISO_8859-1");
                }
                
                break;
            }
        }
        return fileText;
    }
*/    
    public String readInternalFile(final String chapName) throws IOException {
        return readInternalFile(chapName, FROM_DB);
    }
    
    Comparator iFileComp = new Comparator() {

        public int compare(Object arg0, Object arg1) {
            InternalFile if0 = (InternalFile) arg0;
            InternalFile if1 = (InternalFile) arg1;
            
            return if0.fileName.compareToIgnoreCase(if1.fileName);
        }
        
    };
    
    /**
     * The brains behind reading YBK file chapters (or internal files).
     * 
     * @param dataInput The input stream that is the YanceyBook.
     * @param bookId The id of the book record in the database.
     * @param chapterId The id of the chapter record in the database.
     * @return The text of the chapter.
     * @throws IOException If the chapter cannot be read.
     */
    @SuppressWarnings("unchecked")
    public String readInternalFile(String chapName, final int source) 
    throws IOException {
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
                offset = mInternalFiles.get(index).offset;
                len = mInternalFiles.get(index).len;
                fileFound = true;
            } else {
                chapName += ".gz";
                iFile.fileName = chapName;
                index = Arrays.binarySearch(ifArray, iFile, iFileComp);
                if (index > -1) {
                    offset = mInternalFiles.get(index).offset;
                    len = mInternalFiles.get(index).len;
                    fileFound = true;
                } 
            }
        }
        
        if (fileFound) {
            byte[] text = new byte[len];
            file.seek(offset);
            int amountRead = file.read(text);
            if (amountRead < len) {
                throw new InvalidFileFormatException(
                        "Couldn't read all of " + chapName + ".");
            }
            
            
            if (chapName.toLowerCase().endsWith(".gz")) {
                fileText = Util.decompressGzip(text);
            } else {
                fileText = new String(text, "ISO_8859-1");
            }
        } 
                        
        return fileText;
    }

    /**
     * Return the uncompressed contents of Book Metadata internal file as a 
     * String or null if the YBK file doesn't contain one.
     * 
     * @return The uncompressed contents of the Book Metadata file.
     * @throws IOException if there is a problem reading the Book Metadata file. 
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
     * Analyze the YBK file and save file contents data for later reference.
     * 
     * @param bookId The id of the book these chapters belong in.
     * @throws InvalidFileFormatException If the YBK file is not readable.
     */
    public boolean populateChapters(final long bookId) 
    throws IOException {
        String iFileName = "";
        int iBookOffset = 0;
        int iBookLength = 0;
        boolean success = true;
        
        RandomAccessFile file = mFile;
        YbkDAO ybkDao = YbkDAO.getInstance(mCtx);
        
        String orderString = readOrderCfg(FROM_INTERNAL);
        String[] orders = null;
        if (orderString != null) {
            orders = orderString.split(",");
        }

        int indexLength = Util.readVBInt(file);
        //Log.d(TAG,"Index Length: " + indexLength);
        
        byte[] indexArray = new byte[indexLength];
        
        if (file.read(indexArray) < indexLength) {
            throw new InvalidFileFormatException("Index Length is greater than length of file.");
        } 

        if (!ybkDao.deleteChapters(bookId)) {
            Log.e(TAG, "Could not delete the chapters for book with id " + bookId);
        } else {
            
            // Read the index information into the internalFiles list
            int pos = 0;
            
            // Read the index and create chapter records
            while (pos < indexLength) {
                Integer orderNumber = null;
                
                StringBuffer fileNameSBuf = new StringBuffer();
                
                byte b;
                int fileNameStartPos = pos;
    
                while((b = indexArray[pos++]) != 0 && pos < indexLength) {
                    fileNameSBuf.append((char) b);                
                }
                
                iFileName = fileNameSBuf.toString();
                
                pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;
                
                iBookOffset = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
                pos += 4;
                
                iBookLength = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
                pos += 4;
                
                if (orders != null) {
                     int orderNbr = Arrays.binarySearch(orders, iFileName, new Comparator<String>() {
        
                        public int compare(String arg0, String arg1) {
                            return arg0.compareToIgnoreCase(arg1);
                        }
                        
                    });
                    
                    if (orderNbr >= 0) {
                        orderNumber = orderNbr;            
                    }
                }
                
                ybkDao.insertChapter(bookId, iFileName, null, iBookLength, null, 
                        null, iBookOffset, null, orderNumber, null);
                
            }
        }
        return success;
    }

    /**
     * 
     * @param file
     * @param bookFileName
     * @param chapterName
     * @return
     * @throws IOException
     */
    public byte[] readInternalBinaryFile(final String chapterName) 
    throws IOException {
        
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
                throw new IOException(
                        "Couldn't read all of " + chapterName + ".");
            }
        }
        
        return bytes;
    }
}

