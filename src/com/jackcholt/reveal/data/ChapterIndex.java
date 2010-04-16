package com.jackcholt.reveal.data;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.jackcholt.reveal.YbkFileReader;

/**
 * Chapter index structures for Ybk file.
 * 
 * @author Shon Vella
 */
public class ChapterIndex implements Serializable {
    private static final long serialVersionUID = -1L;

    public long[] hashIndex;
    public short[] orderIndex;
    public transient SoftReference<Chapter[]> chaptersRef;
    public String charset = YbkFileReader.DEFAULT_YBK_CHARSET;

    /**
     * Default constructor (the one that deserialization will use)
     */
    public ChapterIndex() {
    }

    /**
     * Create chapter index from the chapter list and the orderString.
     * 
     * @param chapters list of chapters in the order in which they appear in the YBK index
     * @param bookChannel open file channel into the YBK file
     * @param orderString the text from the YBK order.cfg file
     * @param charset the character set to use to turn bytes into characters
     */
    public ChapterIndex(Chapter[] chapters, String orderString, String charset) {
        this.charset = charset;
        this.orderIndex = buildOrderIndex(orderString, chapters);
        this.hashIndex = buildHashIndex(chapters);
    }

    /**
     * Build the order index
     * 
     * @param orderString
     * @param chapters
     * @return
     */
    private short[] buildOrderIndex(final String orderString, final Chapter[] chapters) {
        short[] orderList;
        if (null != orderString) {
            int order = 0;
            orderList = new short[chapters.length];
            Chapter cmpChapter = new Chapter();

            StringTokenizer tokenizer = new StringTokenizer(orderString, ",");
            Chapter[] orderChapters = chapters.clone();
            Arrays.sort(orderChapters, Chapter.orderNameComparator);
            while (tokenizer.hasMoreTokens()) {
                cmpChapter.orderName = tokenizer.nextToken().toLowerCase();
                int chapterIndex = Arrays.binarySearch(orderChapters, cmpChapter, Chapter.orderNameComparator);
                if (chapterIndex >= 0) {
                    int originalIndex = orderChapters[chapterIndex].orderNumber;
                    if (originalIndex < 0) {
                        originalIndex = -originalIndex - 1;
                        orderList[order++] = (short) originalIndex;
                        orderChapters[chapterIndex].orderNumber = order;
                    }
                }
            }
            if (order == chapters.length) {
            } else {
                short tmp[] = new short[order];
                System.arraycopy(orderList, 0, tmp, 0, order);
            }

        } else {
            orderList = new short[0];
        }
        return orderList;
    }

    /**
     * Build the hash index
     * 
     * @param chapters
     * @return the hash index
     */
    private long[] buildHashIndex(final Chapter[] chapters) {
        int chapterCount = chapters.length;
        long hashList[] = new long[chapterCount];
        for (int i = 0; i < chapterCount; i++) {
            Chapter chapter = chapters[i];
            // strip the sign bit to keep things simpler
            int hash = chapter.fileName.hashCode() & 0x7FFFFFFF;
            int orderNumber = chapter.orderNumber;
            if (orderNumber < 0)
                orderNumber = 0;
            // hashkey has the hash in the high order 32 bits, the order number in the next 16 bits.
            // and the index into the chapters in the low order 16 bits.
            // NOTE THAT THIS MEANS WE CANNOT SUPPORT MORE THAN 64K
            long key = (((long) hash) << 32) | (orderNumber << 16) | i;
            hashList[i] = key;
        }
        // sort so we can binary search against the hashes
        Arrays.sort(hashList);
        return hashList;
    }

    /**
     * Get the chapter for fileName.
     * 
     * @param fileName the filename
     * @return the chapter or null if not found
     * @throws IOException
     */
    public Chapter getChapter(FileChannel bookChannel, String fileName) throws IOException {
        Chapter chapter = null;
        fileName = fileName.toLowerCase();
        // strip the sign bit to keep things simpler
        int hash = fileName.hashCode() & 0x7FFFFFFF;
        // hashkey has the hash in the high order 32 bits
        // leave the low order bits at 0
        long key = (((long) hash) << 32);
        int index = Arrays.binarySearch(hashIndex, key);

        // we should almost never get a direct hit because the low order bits won't match, but fortunately binarySearch
        // will point us to where we need to start scanning
        if (index < 0) {
            index = -index - 1;
        }

        // check until we find a match or the hash doesn't match
        for (; index < hashIndex.length; index++) {
            long checkHash = hashIndex[index];
            if ((checkHash >>> 32) != hash)
                break;
            int checkIndex = ((int) checkHash) & 0xFFFF;
            Chapter checkChapter = getChapterByIndex(bookChannel, checkIndex);
            if (checkChapter != null) {
                // inject the order number
                checkChapter.orderNumber = ((int) checkHash) >>> 16;
            } else {
                // couldn't read chapter, so bail
                break;
            }
            if (checkChapter.fileName.equals(fileName)) {
                chapter = checkChapter;
                break;
            }
        }
        return chapter;
    }

    /**
     * Gets a chapter by orderId.
     * 
     * @param orderId the chapter order id
     * @return the chapter or null if not found
     * @throws IOException
     */
    public Chapter getChapterByOrder(FileChannel bookChannel, int orderId) throws IOException {
        if (orderIndex == null || orderId <= 0 || orderId > orderIndex.length) {
            return null;
        }
        // orderId is 1 based, but orderIndex is 0 based, so adjust
        int index = orderIndex[orderId - 1] & 0xFFFF;
        Chapter chapter = getChapterByIndex(bookChannel, index);
        if (chapter != null) {
            // inject orderId in case we just loaded chapter entry
            chapter.orderNumber = orderId;
        }
        return chapter;
    }

    /**
     * Gets a chapter by index.
     * 
     * @param index the chapter index
     * @return the chapter or null if not found
     * @throws IOException
     */
    public Chapter getChapterByIndex(FileChannel bookChannel, int index) throws IOException {
        Chapter chapter = null;
        Chapter chapters[] = null;

        if (chaptersRef == null || (chapters = chaptersRef.get()) == null) {
            chapters = new Chapter[hashIndex.length];
            chaptersRef = new SoftReference<Chapter[]>(chapters);
        }

        if (index >= 0 && index < chapters.length) {
            chapter = chapters[index];
            if (chapter == null)
                chapter = fetchChapter(bookChannel, index);
        }
        return chapter;
    }

    /**
     * Read a chapter entry in from the ybk index
     * 
     * @param checkIndex the index into the index
     * @return the chapter entry
     * @throws IOException
     */
    private Chapter fetchChapter(FileChannel bookChannel, int checkIndex) throws IOException {
        int fileOffset = (checkIndex * YbkFileReader.INDEX_RECORD_LENGTH) + 4;
        ByteBuffer chapterBuf = ByteBuffer.wrap(new byte[YbkFileReader.INDEX_RECORD_LENGTH]);
        if (bookChannel.read(chapterBuf, (long) fileOffset) != YbkFileReader.INDEX_RECORD_LENGTH) {
            throw new EOFException();
        }
        return Chapter.fromYbkIndex(chapterBuf.array(), 0, charset);
    }
}
