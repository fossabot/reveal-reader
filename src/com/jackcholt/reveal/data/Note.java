package com.jackcholt.reveal.data;

public class Note {
    private Long id = null;
    private long bookId;
    private long chapterId;
    private long verseStartPos;
    private String body;
    private String chapterVerse;

    public Note(Long id, long bookId, long chapterId, long verseStartPos, String body, String chapterVerse) {
        this.id = id;
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.verseStartPos = verseStartPos;
        this.body = body;
        this.chapterVerse = chapterVerse;
    }
    
    /**
     * Constructor for creating a new Note.
     * @see Note(Long, long, long, long, String);
     */
    public Note(long bookId, long chapterId, long verseStartPos, String body, String chapterVerse) {
        this(null, bookId, chapterId, verseStartPos, body, chapterVerse);
    }
    
    /**
     * @return the id
     */
    public final Long getId() {
        return id;
    }

    /**
     * @return the chapterVerse
     */
    public final String getChapterVerse() {
        return chapterVerse;
    }

    /**
     * @return the bookId
     */
    public final long getBookId() {
        return bookId;
    }

    /**
     * @return the chapterId
     */
    public final long getChapterId() {
        return chapterId;
    }

    /**
     * @return the verseStartPos
     */
    public final long getVerseStartPos() {
        return verseStartPos;
    }

    /**
     * @return the body
     */
    public final String getBody() {
        return body;
    }

}
