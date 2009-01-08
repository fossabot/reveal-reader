package com.jackcholt.revel;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
    private static final String BOOKMETADATA_FILENAME = "\\BOOKMETADATA.HTML.GZ"; 
    private static final String ORDER_CONFIG_FILENAME = "\\ORDER.CFG";
    private File mFile;
    private DataInputStream mDataInput;
    private int mIndexLength;
    private ArrayList<InternalFile> mInternalFiles = new ArrayList<InternalFile>();
    private String mBindingText = "No Binding Text";
    private String mBookTitle = "Couldn't get the title of this book";
    private String mBookShortTitle = "No Short Title";
    private String mBookMetaData = null;     
    private ArrayList<String> mOrderList = new ArrayList<String>();
    private String mCurrentChapterOrderName = null;
    private int mCurrentChapterOrderNumber = -1;
    private String mChapterNavBarTitle = "No Title";
    private String mChapterHistoryTitle = "No Title";
    private int mChapterNavFile = CHAPTER_TYPE_SETTINGS;
    private int mChapterZoomPicture = CHAPTER_ZOOM_MENU_OFF;
    //private Context context = null;
    /**
     * A class to act as a structure for holding information about the chapters 
     * held inside a YBK ebook.
     * 
     */
    private class InternalFile {
        private String mFileName;
        private int mYbkOffset;
        private int mYbkLen;

        InternalFile() {
            // do nothing
        }
        
        InternalFile(final String fileName, final int ybkOffset, 
                final int ybkLen) {
            mFileName = fileName;
            mYbkOffset = ybkOffset;
            mYbkLen = ybkLen;
        }
        
        /**
         * @return the fileName
         */
        final String getFileName() {
            return mFileName;
        }
        
        /**
         * @return the ybkLen
         */
        final int getYbkLen() {
            return mYbkLen;
        }
        
        /**
         * @return the ybkOffset
         */
        final int getYbkOffset() {
            return mYbkOffset;
        }
        
        /**
         * @param fileName the fileName to set
         */
        final void setFileName(final String fileName) {
            mFileName = fileName;
        }
        
        /**
         * @param ybkLen the ybkLen to set
         */
        final void setYbkLen(final int ybkLen) {
            mYbkLen = ybkLen;
        }
        
        /**
         * @param ybkOffset the ybkOffset to set
         */
        final void setYbkOffset(final int ybkOffset) {
            mYbkOffset = ybkOffset;
        }
    }
    
    /**
     * Construct a new 
     * YbkFileReader on the given File <code>file</code>. If the 
     * <code>file</code> specified cannot be found, throw a FileNotFoundException.
     * 
     * @param file a File to be opened for reading characters from.
     * @throws FileNotFoundException if the file cannot be opened for 
     * reading. 
     */
    public YbkFileReader(final File file) 
    throws FileNotFoundException, IOException {
        mFile = file;
        
        initDataStream();
        
        populateFileData();
    }

    /**
     * Construct a new 
     * YbkFileReader on the given file named <code>filename</code>. If the 
     * <code>filename</code> specified cannot be found, throw a FileNotFoundException.
     * 
     * @param fileName an absolute or relative path specifying the file to open.
     * @throws FileNotFoundException if the filename cannot be opened for 
     * reading. 
     */
    public YbkFileReader(final String fileName) 
    throws FileNotFoundException, IOException {
        this(new File(fileName));
    }
    
//    public YbkFileReader(Context ctx, final int id) {
//        
//        Cursor c = ctx.getContentResolver().query(
//                ContentUris.withAppendedId(YbkProvider.BOOK_CONTENT_URI, id), 
//                new String[] {YbkProvider.FILE_NAME}, null, null, null);
//        
//        c.moveToFirst();
//        String fileName = c.getString(0);
//        
//        this(fileName);
//        
//    }
    
    /**
     * Return the title of the book.
     * 
     * @return The book title.
     */
    public String getBookTitle() {
        return mBookTitle;
    }
    
    /**
     * @return the mBookMetaData
     */
    public final String getBookMetaData() {
        return mBookMetaData;
    }

    private void initDataStream() throws FileNotFoundException {
        mDataInput = new DataInputStream(new BufferedInputStream(new FileInputStream (mFile)));
        mDataInput.mark(Integer.MAX_VALUE);
    }
    
    /**
     * Analyze the YBK file and save file contents data for later reference.
     * @throws IOException If the YBK file is not readable.
     */
    private void populateFileData() throws IOException {
        mIndexLength = Util.readVBInt(mDataInput);
        Log.d("revel","Index Length: " + mIndexLength);
        
        byte[] indexArray = new byte[mIndexLength];
        
        if (mDataInput.read(indexArray) < mIndexLength) {
            throw new IllegalStateException("Index Length is greater than length of file.");
        }
        
        // Read the index information into the internalFiles list
        int pos = 0;
        
        while (pos < mIndexLength) {
            InternalFile iFile = new InternalFile();
            
            StringBuffer fileNameSBuf = new StringBuffer();
            
            byte b;
            int fileNameStartPos = pos;

            while((b = indexArray[pos++]) != 0) {
                fileNameSBuf.append((char) b);                
            }
            
            iFile.setFileName(fileNameSBuf.toString());
            
            pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;
            
            iFile.setYbkOffset(Util.readVBInt(Util.makeVBIntArray(indexArray, pos)));
            pos += 4;
            
            iFile.setYbkLen(Util.readVBInt(Util.makeVBIntArray(indexArray, pos)));
            pos += 4;
            
            // Add the internal file into the list
            mInternalFiles.add(iFile);
            
            
        }
        
        mBindingText = readBindingFile();
        if (mBindingText != null) {
            mBookTitle = Util.getBookTitleFromBindingText(mBindingText);
            mBookShortTitle = Util.getBookShortTitleFromBindingText(mBindingText);
            mBookMetaData = readMetaData();
            populateOrder(readOrderCfg());
        }
    }
    
    private void populateOrder(final String orderString) {
        if (null != orderString) {
            Scanner orderScan = new Scanner(orderString).useDelimiter(",");
            
            ArrayList<String> orderList = mOrderList;
            
            while(orderScan.hasNext()) {
                orderList.add(orderScan.next());
            }
        }
    }

    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML.
     * @throws IOException If there is a problem reading the Binding file.
     * @throws InvalidFileFormatException if there is no Binding internal file 
     * found. 
     */
    public String readBindingFile() 
    throws IOException, InvalidFileFormatException {
        String fileText = readInternalFile(BINDING_FILENAME);
        
        if (null == fileText) {
            Log.w(TAG,"The YBK file contains no binding.html");
        }
        
        return fileText;
    }

    public String readInternalFile(final String iFilename) throws IOException {
        String fileText = null;
        int offset = 0;
        int len = 0;
        
        DataInputStream dataInput = mDataInput;
        
        ArrayList<InternalFile> internalFiles = mInternalFiles;
        
        for(InternalFile iFile : internalFiles) {
            if (iFile.getFileName().equalsIgnoreCase(iFilename)) {
                offset = iFile.getYbkOffset();
                len = iFile.getYbkLen();
        
                try {
                    dataInput.reset();
                } catch (IOException ioe) {
                    Log.w("YbkFileReader", "YBK file's DataInputStream had to be closed and reopened. " 
                            + ioe.getMessage());
                    dataInput.close();
                    initDataStream();
                }
             
                byte[] text = new byte[len];
                dataInput.skipBytes(offset);
                int amountRead = dataInput.read(text);
                if (amountRead < len) {
                    throw new InvalidFileFormatException(
                            "Couldn't read all of " + iFilename + ".");
                }
                
                if (iFilename.toLowerCase().endsWith(".gz")) {
                    fileText = Util.decompressGzip(text);
                } else {
                    fileText = new String(text);
                }
                
                break;
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
    private String readMetaData() throws IOException {
        return readInternalFile(BOOKMETADATA_FILENAME);
    }
    
    private String readOrderCfg() throws IOException {
        return readInternalFile(ORDER_CONFIG_FILENAME);
    }

    /**
     * @param bookMetaData the mBookMetaData to set
     */
    public final void setMBookMetaData(String bookMetaData) {
        mBookMetaData = bookMetaData;
    }

    
    /**
     * Get the list order info.
     * 
     * @return the orderList
     */
    public final List<String> getOrderList() {
        return mOrderList;
    }

    /**
     * Return the text of the next chapter and set the current chapter to the 
     * chapter returned.
     *  
     * @return Contents of the next chapter or <code>null</code> if there is no next chapter.
     * @throws IOException When the next chapter cannot be read.
     */
    public String readNextChapter() {
        String chapter = null;
        
        ArrayList<String> orderList = mOrderList;
        int orderListSize = orderList.size();
        if (orderListSize > 0) {
            if (mCurrentChapterOrderNumber < orderListSize) {
                mCurrentChapterOrderNumber++;
                
                String currentChapterOrderName = mCurrentChapterOrderName = orderList.get(mCurrentChapterOrderNumber);
                if (!currentChapterOrderName.toLowerCase().endsWith(".html")) {
                    currentChapterOrderName += ".html";
                }

                String chapterName = "\\" + mBookShortTitle + "\\" + currentChapterOrderName + ".gz";
          
                try {
                    chapter = readInternalFile(chapterName);
                } catch (IOException ioe) {
                    Log.e("revel", "Chapter " + chapterName + " could not be read. " 
                            + ioe.getMessage());
                }
                
            } 
        }
        
        setChapterData(chapter);
        
        return chapter;
    }

    /**
     * Return the text of the previous chapter and set the current chapter to the 
     * chapter returned.
     *  
     * @return Contents of the next chapter or <code>null</code> if there is no 
     * previous chapter
     * @throws IOException When the previous chapter cannot be read.
     */
    public String readPrevChapter() {
        String chapter = null;
        
        ArrayList<String> orderList = mOrderList;

        if (orderList.size() > 0) {
            if (mCurrentChapterOrderNumber > 0) {
                mCurrentChapterOrderNumber--;
                
                String currentChapterOrderName = mCurrentChapterOrderName = orderList.get(mCurrentChapterOrderNumber);
                if (!currentChapterOrderName.toLowerCase().endsWith(".html")) {
                    currentChapterOrderName += ".html";
                }
          
                String chapterName = "\\" + mBookShortTitle + "\\" + currentChapterOrderName + ".gz";
                
                try {
                    chapter = readInternalFile(chapterName);
                } catch (IOException ioe) {
                    Log.e("revel", "Chapter " + chapterName + " could not be read. " 
                            + ioe.getMessage());
                }
            } 
        }
        
        return chapter;
    }

    /**
     * @return the currentChapterOrderName
     */
    public final String getCurrentChapterOrderName() {
        return mCurrentChapterOrderName;
    }

    /**
     * @return the currentChapterOrderNumber
     */
    public final int getCurrentChapterOrderNumber() {
        return mCurrentChapterOrderNumber;
    }

    /**
     * @return the mBindingText
     */
    public final String getBindingText() {
        return mBindingText;
    }
    
    /**
     * Get the contents of an image file from within the YBK file.
     * 
     * @param imageFileName The filename of the image.
     * @return The bytes which make up the image.
     * @throws IOException if the image file cannot be read.
     */
    public byte[] readImage(final String imageFileName) throws IOException {
        byte[] image = null;
        int offset = 0;
        int len = 0;
        String fileName = "\\" + imageFileName;
        fileName = fileName.replace("/", "\\");
        
        ArrayList<InternalFile> internalFiles = mInternalFiles;
        for(InternalFile iFile : internalFiles) {
            if (iFile.getFileName().equalsIgnoreCase(fileName)) {
                offset = iFile.getYbkOffset();
                len = iFile.getYbkLen();
        
                DataInputStream dataInput = mDataInput;
                try {
                    dataInput.reset();
                } catch (IOException ioe) {
                    Log.w("YbkFileReader", "YBK file's DataInputStream had to be closed and reopened. " 
                            + ioe.getMessage());
                    dataInput.close();
                    initDataStream();
                }
             
                image = new byte[len];
                dataInput.skipBytes(offset);
                int amountRead = dataInput.read(image);
                if (amountRead < len) {
                    throw new InvalidFileFormatException(
                            "Couldn't read all of " + imageFileName + ".");
                }
                                
                break;
            }
        }
        return image;
        
    }
    
    public String readChapter(final String chapterName) {
        String text = null;
        String pChapterName = chapterName;
        
        String bookShortTitle = mBookShortTitle;
        if (!pChapterName.toLowerCase().startsWith("\\" + bookShortTitle.toLowerCase() , 0)) {
            throw new IllegalArgumentException(
                    "chapterName does not start with the book folder.");
        }
        
        if (!pChapterName.toLowerCase().endsWith(".html")) {
            pChapterName += ".html";
        }
  
        try {
            text = readInternalFile(pChapterName + ".gz");
        } catch (IOException ioe) {
            Log.e("revel", "Chapter " + chapterName + " could not be read. " 
                    + ioe.getMessage());
        }
        
        // Update the current chapter information
        String indexName4Search = chapterName.substring(("\\" + bookShortTitle + "\\").length());
        int dotIndex = indexName4Search.indexOf(".");
        if (dotIndex != -1) {
            indexName4Search = indexName4Search.substring(0, dotIndex);
            
            ArrayList<String> orderList = mOrderList;
            int orderListSize = orderList.size();
            for (int i = 0; i < orderListSize; i++) {
                if (orderList.get(i).toLowerCase().indexOf(indexName4Search.toLowerCase()) != -1 ) {
                    mCurrentChapterOrderNumber = i;
                    mCurrentChapterOrderName = orderList.get(i);
                    break;
                }
            }
        }
        return text;
        
    }

    private void setChapterData(final String chapter) {
        String chapterHistoryTitle = "No Title";
        String chapterNavBarTitle = "No Title";
        int chapterNavFile = CHAPTER_TYPE_SETTINGS;
        int chapterZoomPicture = CHAPTER_ZOOM_MENU_OFF;
        
        if (null != chapter) {
            int pos = chapter.toLowerCase().indexOf("<ln>");
            
            if (pos != -1) {
                pos += 4;
                int endPos = chapter.indexOf("<", pos);
                if (endPos != -1) {
                    chapterNavBarTitle = chapter.substring(pos, endPos);
                }
            }

            pos = chapter.toLowerCase().indexOf("<fn>");
            
            if (pos != -1) {
                pos += 4;
                int endPos = chapter.indexOf("<", pos);
                if (endPos != -1) {
                    chapterHistoryTitle = chapter.substring(pos, endPos);
                }
            }

            pos = chapter.toLowerCase().indexOf("<nf>");
            
            if (pos != -1) {
                pos += 4;
                int endPos = chapter.indexOf("<", pos);
                if (endPos != -1) {
                    chapterNavFile = Integer.parseInt(chapter.substring(pos, endPos));
                }
            }

            pos = chapter.toLowerCase().indexOf("<zp>");
            
            if (pos != -1) {
                pos += 4;
                int endPos = chapter.indexOf("<", pos);
                if (endPos != -1) {
                    chapterZoomPicture = Integer.parseInt(chapter.substring(pos, endPos));
                }
            }
        }
        
        mChapterHistoryTitle = chapterHistoryTitle;
        mChapterNavBarTitle = chapterNavBarTitle;
        mChapterNavFile = chapterNavFile;
        mChapterZoomPicture = chapterZoomPicture;
    }

    /**
     * @return the mChapterNavBarTitle
     */
    public final String getChapterNavBarTitle() {
        return mChapterNavBarTitle;
    }

    /**
     * @param chapterNavBarTitle the mChapterNavBarTitle to set
     */
    public final void setChapterNavBarTitle(String chapterNavBarTitle) {
        mChapterNavBarTitle = chapterNavBarTitle;
    }

    /**
     * @return the mChapterHistoryTitle
     */
    public final String getChapterHistoryTitle() {
        return mChapterHistoryTitle;
    }

    /**
     * @param chapterHistoryTitle the mChapterHistoryTitle to set
     */
    public final void setChapterHistoryTitle(String chapterHistoryTitle) {
        mChapterHistoryTitle = chapterHistoryTitle;
    }

    /**
     * @return the mChapterNavFile
     */
    public final int getChapterNavFile() {
        return mChapterNavFile;
    }

    /**
     * @param chapterNavFile the mChapterNavFile to set
     */
    public final void setChapterNavFile(int chapterNavFile) {
        mChapterNavFile = chapterNavFile;
    }

    /**
     * @return the mChapterZoomPicture
     */
    public final int getChapterZoomPicture() {
        return mChapterZoomPicture;
    }

    /**
     * @param chapterZoomPicture the mChapterZoomPicture to set
     */
    public final void setChapterZoomPicture(int chapterZoomPicture) {
        mChapterZoomPicture = chapterZoomPicture;
    }

    /**
     * @return the mBookShortTitle
     */
    public final String getBookShortTitle() {
        return mBookShortTitle;
    }
}
