package com.jackcholt.reveal;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.flurry.android.FlurryAgent;

/**
 * A class to do all the work of reading and accessing YBK files.
 * 
 * @author Jack C. Holt - jackcholt@gmail.com
 */
public class YbkTitleReader {
    private static final int INDEX_FILENAME_STRING_LENGTH = 48;
    private static final String BINDING_FILENAME = "\\BINDING.HTML";
    private File mFile;
    private DataInputStream mDataInput;
    private int mIndexLength;
    private String mBookTitle = "Couldn't get the title of this book";

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
    public YbkTitleReader(final File file) throws FileNotFoundException,
            IOException {
        mFile = file;

        initDataStream();

        populateFileData();
    }

    /**
     * Construct a new YbkFileReader on the given file named
     * <code>filename</code>. If the <code>filename</code> specified cannot be
     * found, throw a FileNotFoundException.
     * 
     * @param fileName
     *            an absolute or relative path specifying the file to open.
     * @throws FileNotFoundException
     *             if the filename cannot be opened for reading.
     */
    public YbkTitleReader(final String fileName) throws FileNotFoundException,
            IOException {
        this(new File(fileName));
    }

    /**
     * Return the title of the book.
     * 
     * @return The book title.
     */
    public String getBookTitle() {
        return mBookTitle;
    }

    private void initDataStream() throws FileNotFoundException {
        mDataInput = new DataInputStream(new BufferedInputStream(
                new FileInputStream(mFile)));
        mDataInput.mark(Integer.MAX_VALUE);
    }

    /**
     * Analyze the YBK file and save file contents data for later reference.
     * 
     * @throws IOException
     *             If the YBK file is not readable.
     */
    private void populateFileData() throws IOException {
        DataInputStream dataInput = mDataInput;
        // mIndexLength = Util.readVBInt(dataInput);
        int indexLength = mIndexLength;
        // Creating eBook file data for re-use
        // Toast.makeText(this, "Checking for new eBooks...",
        // Toast.LENGTH_SHORT).show();
        Log.d("reveal", "Index Length: " + indexLength);

        byte[] indexArray = new byte[indexLength];

        if (dataInput.read(indexArray) < indexLength) {
            FlurryAgent.onError("YbkTitleReader",
                    "Index Lenght Greater than length of file", "WARNING");
            throw new InvalidFileFormatException(
                    "Index Length is greater than length of file.");
        }

        // Read the index information into the internalFiles list
        int pos = 0;
        while (pos < indexLength) {

            StringBuffer fileNameSBuf = new StringBuffer();

            byte b;
            int fileNameStartPos = pos;

            while ((b = indexArray[pos++]) != 0) {
                fileNameSBuf.append((char) b);
            }

            String iFilename = fileNameSBuf.toString();

            if (iFilename.equalsIgnoreCase(BINDING_FILENAME)) {

                pos = fileNameStartPos + INDEX_FILENAME_STRING_LENGTH;

                int offset = Util.readVBInt(Util
                        .makeVBIntArray(indexArray, pos));
                pos += 4;

                int len = Util.readVBInt(Util.makeVBIntArray(indexArray, pos));
                pos += 4;

                try {
                    dataInput.reset();
                } catch (IOException ioe) {
                    Log.w("YbkFileReader",
                            "YBK file's DataInputStream had to be closed and reopened. "
                                    + ioe.getMessage());
                    FlurryAgent
                            .onError(
                                    "YbkTitleReader",
                                    "YBK file's DataInputStream had to be closed and reopened",
                                    "WARNING");
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

                String bindingText = readBindingFile(offset, len);
                mBookTitle = Util.getBookTitleFromBindingText(bindingText);

                break;
            }
        }
    }

    /**
     * Return the contents of BINDING.HTML internal file as a String.
     * 
     * @return The contents of BINDING.HTML.
     * @throws IOException
     *             If there is a problem reading the Binding file.
     * @throws InvalidFileFormatException
     *             if there is no Binding internal file found.
     */
    public String readBindingFile(final int offset, final int len)
            throws IOException, InvalidFileFormatException {
        DataInputStream dataInput = mDataInput;

        try {
            dataInput.reset();
        } catch (IOException ioe) {
            FlurryAgent.onError("YbkTitleReader",
                    "YBK file's DataInputStream had to be closed and reopened",
                    "WARNING");
            Log.w("YbkTitleReader",
                    "YBK file's DataInputStream had to be closed and reopened. "
                            + ioe.getMessage());
            dataInput.close();
            initDataStream();
        }

        byte[] text = new byte[len];
        dataInput.skipBytes(offset);
        int amountRead = dataInput.read(text);
        if (amountRead < len) {
            throw new InvalidFileFormatException(
                    "Couldn't read all of binding file.");
        }

        String fileText = new String(text);

        if (null == fileText) {
            FlurryAgent.onError("YbkTitleReader",
                    "The YBK file contains no binding.html", "WARNING");
            throw new InvalidFileFormatException(
                    "The YBK file contains no binding.html");
        }

        return fileText;
    }

}
