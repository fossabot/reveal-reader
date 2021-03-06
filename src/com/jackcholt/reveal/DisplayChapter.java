package com.jackcholt.reveal;

/**
 * Class for holding data and behavior for the currently displaying chapter in YbkViewActivity and its children. 
 * @author Jack C. Holt
 */
public class DisplayChapter {
    private String bookFileName;
    private String chapFileName;
    private int scrollYPos = 0;
    private String fragment;
    private int chapOrderNbr = 0;
    private String navFile = "1";
    private String verse;
    private String title; 

    public void setBookFileName(String bookFileName) {
        this.bookFileName = bookFileName;
    }

    public String getBookFileName() {
        if (null == bookFileName) {
            Log.i("DisplayChapter.getBookFileName()", "Book filename is null.");
        }
        return bookFileName;
    }

    public void setChapFileName(String chapFileName) {
        this.chapFileName = chapFileName;
    }

    public String getChapFileName() {
        if (null == chapFileName) {
            Log.i("CurrentChapter.getChapFileName()", "Chapter filename is null.");
        }
        return chapFileName;
    }

    public void setScrollYPos(int scrollYPos) {
        this.scrollYPos = scrollYPos;
    }

    public int getScrollYPos() {
        return scrollYPos;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getFragment() {
        return fragment;
    }

    public void setChapOrderNbr(int chapOrderNbr) {
        this.chapOrderNbr = chapOrderNbr;
    }

    public int getChapOrderNbr() {
        return chapOrderNbr;
    }

    public void setNavFile(String navFile) {
        this.navFile = navFile;
    }

    public String getNavFile() {
        return navFile;
    }

    public String getVerse() {
        return verse;
    }

    public void setVerse(String verse) {
        this.verse = verse;
    }

    public void setTitle(Object title) {
        if (title instanceof String) {
            this.title = (String) title;
        }
    }

    public String getTitle() {
        return title;
    }
}