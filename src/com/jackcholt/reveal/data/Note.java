package com.jackcholt.reveal.data;

public class Note {
    private Long id = null;
    private String bookFileName;
    private String chapterName;
    private long verseStartPos;
    private String body;
    private String chapterVerse;

    public Note(Long id, String bookFileName, String chapterName, long verseStartPos, String body, String chapterVerse) {
        this.id = id;
        this.bookFileName = bookFileName;
        this.chapterName = chapterName;
        this.verseStartPos = verseStartPos;
        this.body = body;
        this.chapterVerse = chapterVerse;
    }

    /**
     * Constructor for creating a new Note.
     * 
     * @see Note(Long, long, long, long, String);
     */
    public Note(String bookFileName, String chapterName, long verseStartPos, String body, String chapterVerse) {
        this(null, bookFileName, chapterName, verseStartPos, body, chapterVerse);
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
    public final String getBookFileName() {
        return bookFileName;
    }

    /**
     * @return the chapterId
     */
    public final String getChapterName() {
        return chapterName;
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
