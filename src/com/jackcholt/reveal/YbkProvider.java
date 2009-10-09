package com.jackcholt.reveal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

public class YbkProvider extends ContentProvider {
    public static final String KEY_MIMETYPE = "mimetype";

    public static final String AUTHORITY = "com.jackcholt.reveal";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/ybk");

    public static final int BOOK = 0;

    public static final int BOOKS = 1;

    public static final int CHAPTER = 2;

    public static final int CHAPTERS = 3;

    public static final int HISTORY = 4;

    public static final int HISTORIES = 5;

    public static final int BOOKMARK = 6;

    public static final int BOOKMARKS = 7;

    public static final int BACK = 8;

    public static final String TAG = "YbkProvider";

    /**
     * Is the chapter a navigation chapter? Data type: INTEGER. Use
     * {@link CHAPTER_TYPE_NO_NAV} and {@link CHAPTER_TYPE_NAV} to set values.
     */
    public static final String CHAPTER_NAV_FILE = "nav_file";

    /**
     * Should the user be able to zoom the page? Data type: INTEGER. Used when
     * the chapter contains a picture. Use {@link CHAPTER_ZOOM_MENU_OFF} and
     * {@link CHAPTER_ZOOM_MENU_ON} to set values.
     */
    public static final String CHAPTER_ZOOM_PICTURE = "zoom_picture";

    public static final String BOOK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.book";

    public static final String BOOK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.book";

    public static final String CHAPTER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.chapter";

    public static final String CHAPTER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.chapter";

    public static final String HISTORY_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.history";

    public static final String HISTORY_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.history";

    public static final String BOOKMARK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.bookmark";

    public static final String BOOKMARK_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.ybk.bookmark";

    public static final String BACK_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.ybk.back";

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

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter/#", CHAPTER);
        sUriMatcher.addURI(AUTHORITY, "ybk/chapter", CHAPTERS);
        sUriMatcher.addURI(AUTHORITY, "ybk/book/#", BOOK);
        sUriMatcher.addURI(AUTHORITY, "ybk/book", BOOKS);
        sUriMatcher.addURI(AUTHORITY, "ybk/history/#", HISTORY);
        sUriMatcher.addURI(AUTHORITY, "ybk/history", HISTORIES);
        sUriMatcher.addURI(AUTHORITY, "ybk/bookmark/#", BOOKMARK);
        sUriMatcher.addURI(AUTHORITY, "ybk/bookmark", BOOKMARKS);
        sUriMatcher.addURI(AUTHORITY, "ybk/back/#", BACK);
    }

    private HashMap<Uri, ImgFileInfo> mTempImgFiles = new HashMap<Uri, ImgFileInfo>();

    private SharedPreferences mSharedPref;

    /**
     * @see {@link ContentProvider.delete(Uri uri, String selection, String[]
     *      selectionArgs)}
     */
    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {

        Log.e(TAG, "YbkProvider does not support deletion.");

        return 0;
    }

    /**
     * Get the mime-type of a particular URI.
     * 
     * @param uri
     *            The URI for which to get a mime-type.
     * @return The mime-type.
     */
    @Override
    public String getType(final Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case CHAPTERS:
            return CHAPTER_CONTENT_TYPE;

        case CHAPTER:
            return CHAPTER_CONTENT_ITEM_TYPE;

        case BOOKS:
            return BOOK_CONTENT_TYPE;

        case BOOK:
            return BOOK_CONTENT_ITEM_TYPE;

        case HISTORIES:
            return HISTORY_CONTENT_TYPE;

        case HISTORY:
            return HISTORY_CONTENT_ITEM_TYPE;

        case BOOKMARKS:
            return BOOKMARK_CONTENT_TYPE;

        case BOOKMARK:
            return BOOKMARK_CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Insert records into the content provider.
     */
    @Override
    public Uri insert(final Uri uri, final ContentValues initialValues) {

        Log.e(TAG, "The YbkProvider does not support inserts");

        return null;
    }

    @Override
    public boolean onCreate() {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        // clean up leftover image files
        String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY, Settings.DEFAULT_EBOOK_DIRECTORY);
        Util.deleteFiles(new File(libDir, "images"), ".*");
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
            String sortOrder) {

        Log.e(TAG, "The YbkProvider does not support queries.");

        return null;
    }

    /**
     * This is only supported for bookmarks.
     */
    @Override
    public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        Log.e(TAG, "The YbkProvider does not support updates.");

        return 0;
    }

    /**
     * Open a file and return a ParcelFileDescriptor reference to it.
     * 
     * @param uri
     *            The URI which refers to the file.
     * @param mode
     *            The mode in which to open the file.
     * @return The ParcelFileDescriptor which refers to the file.
     * @throws FileNotFoundException
     *             If the file cannot be accessed.
     * @see {@link ContentProvider.openFile(Uri, String)}
     */
    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String mode) throws FileNotFoundException {
        final int BUFFER_SIZE = 8096;
        synchronized (mTempImgFiles) {
            ParcelFileDescriptor pfd = null;

            // Log.d(TAG,"In openFile. URI is: " + uri.toString());

            HashMap<Uri, ImgFileInfo> tempImgFiles = mTempImgFiles;
            ImgFileInfo info;
            File outFile;
            File outThumbnail;

            String strUri = uri.toString();
            String fileExt = strUri.substring(strUri.lastIndexOf("."));

            if (".jpg .gif".contains(fileExt)) {
                if (tempImgFiles.containsKey(uri)) {
                    info = tempImgFiles.get(uri);
                    info.use();
                    outFile = info.file;
                } else {
                    String strCUri = CONTENT_URI.toString();
                    int cUriLength = strCUri.length();
                    String uriFileName = strUri.substring(cUriLength);

                    String[] fileParts = uriFileName.split("/");
                    String tempFileName = "";
                    for (int i = 1; i < fileParts.length; i++) {
                        tempFileName += fileParts[i] + "_";
                    }
                    String tempThumbFileName = "";
                    tempThumbFileName += fileParts[1] + fileExt;

                    tempFileName = tempFileName.substring(0, tempFileName.length() - 1);

                    String libDir = mSharedPref.getString(Settings.EBOOK_DIRECTORY_KEY,
                            Settings.DEFAULT_EBOOK_DIRECTORY);

                    outFile = new File(libDir + "images/", tempFileName);
                    outThumbnail = new File(libDir + "thumbnails/", tempThumbFileName);

                    if (!outFile.exists()) {
                        HashMap<String, String> chapterMap = Util.getFileNameChapterFromUri(strUri, false);
                        String fileName = chapterMap.get("book");
                        String chapter = chapterMap.get("chapter");
                        YbkFileReader ybkRdr = null;
                        try {
                            ybkRdr = YbkFileReader.getReader(Main.getMainApplication(), new File(fileName).getName());
                            byte[] contents = ybkRdr.readInternalBinaryFile(chapter);
                            if (contents != null) {
                                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile),
                                        BUFFER_SIZE);
                                BufferedOutputStream outThumb = new BufferedOutputStream(new FileOutputStream(
                                        outThumbnail), BUFFER_SIZE);
                                try {
                                    out.write(contents);
                                    out.flush();
                                    outThumb.write(contents);
                                    outThumb.flush();

                                } finally {
                                    out.close();
                                    outThumb.close();
                                }
                            } else {
                                throw new FileNotFoundException("Couldn't read internal image file.");
                            }
                        } catch (IOException e) {
                            throw new FileNotFoundException("Could not write internal file to temp file."
                                    + e.getMessage() + " " + e.getCause());
                        } finally {
                            if (ybkRdr != null) {
                                ybkRdr.unuse();
                                ybkRdr = null;
                            }
                        }
                    }
                    info = new ImgFileInfo(uri, outFile);
                    tempImgFiles.put(uri, info);
                }

                int m = ParcelFileDescriptor.MODE_READ_ONLY;
                if (mode.equalsIgnoreCase("rw"))
                    m = ParcelFileDescriptor.MODE_READ_WRITE;

                pfd = new ParcelFileDescriptorWrapper(info, ParcelFileDescriptor.open(outFile, m));
            } else {
                Log.w(TAG, "openFile was called for non-image URI: " + uri);
            }

            return pfd;
        }
    }

    private class ImgFileInfo {
        File file;
        int useCount = 1;
        @SuppressWarnings("unused")
        Uri uri;

        ImgFileInfo(Uri uri, File file) {
            this.uri = uri;
            this.file = file;
        }

        int use() {
            synchronized (mTempImgFiles) {
                return ++useCount;
            }
        }

        int unuse() {
            synchronized (mTempImgFiles) {
                if (--useCount <= 0) {
                    // mTempImgFiles.remove(uri);
                    // file.delete();
                }
                return useCount;
            }
        }
    }

    private static class ParcelFileDescriptorWrapper extends ParcelFileDescriptor {
        ImgFileInfo info;

        public ParcelFileDescriptorWrapper(ImgFileInfo info, ParcelFileDescriptor descriptor) {
            super(descriptor);
            this.info = info;
        }

        @Override
        public void close() throws IOException {
            super.close();
            info.unuse();
        }

    }

}
