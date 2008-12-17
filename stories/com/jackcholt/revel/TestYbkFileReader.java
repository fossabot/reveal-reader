package com.jackcholt.revel;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestYbkFileReader {
    YbkFileReader YbkRdr;
    
    @Before
    public void setUp() throws Exception {
        YbkRdr = new YbkFileReader("c:\\pogp.ybk");
    }

    @After
    public void tearDown() throws Exception {
        YbkRdr = null;
    }

    @Test
    public void testGetBookTitle() {
        assertTrue(YbkRdr.getBookTitle().startsWith("P<small>"));
    }
    
    @Test
    public void testGetBindingText() {
        String bindingText = YbkRdr.getBindingText();
        
        Log.d("testGetBindingText", bindingText);
    }
    
    @Test
    public void testGetBookMetaData() {
        Log.i("TestYbkReader", "Book meta data: " + YbkRdr.getBookMetaData());
    }

    @Test
    public void testGetBindingFile() throws IOException {
        String text = YbkRdr.readBindingFile();
        
        assertTrue(text.startsWith("<a href=\"PoGP.html\">"));
        Log.d("revel", text);
    }

    @Test
    public void testGetOrderCfg() throws IOException {
        List<String> orderList = YbkRdr.getOrderList();
        
        for (String order : orderList) {
            Log.d("testGetOrderCfg", "Chapter name: " + order);
        }
    }

    @Test
    public void testGetChapters() throws IOException {
        String text = YbkRdr.readInternalFile("\\POGP\\MOSES\\6.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6: " + text);
        
        text = YbkRdr.readInternalFile("\\POGP\\MOSES\\6_.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6 footnotes: " + text);
    }
    
    @Test
    public void testGetNextChapter() {
        String text = YbkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);

        text = YbkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);
    }

    @Test
    public void testGetPrevChapter() {
        YbkRdr.readNextChapter();        
        YbkRdr.readNextChapter();
        String text = YbkRdr.readPrevChapter();
        
        Log.d("testGetPrevChapter", text);
    }

    @Test
    public void testGetPrevChapterNoPrev() {
        String text = YbkRdr.readPrevChapter();

        assertNull(text);
    }

    @Test
    public void testGetChapter() {
        String text = YbkRdr.readChapter("\\pogp\\abr\\1.html");
        
        Log.d("testGetChapter", text);
    }

    @Test
    public void testGetFacsimile() {
        String text = YbkRdr.readChapter("\\pogp\\abr\\fac_3.html");
        
        Log.d("testGetFacsimile", text);
    }

    @Test
    public void testGetImage() throws IOException {
        byte[] image = YbkRdr.readImage("pogp/images/facsimile3.gif");
        
        assertNotNull(image);
        assertTrue(new String(image).startsWith("GIF89", 0));
    }

    @Test
    public void testGetFormattedTitle() {
        assertTrue(!Util.formatTitle(YbkRdr.getBookTitle()).contains("<"));
    }

    @Test
    public void testGetATOTC() throws FileNotFoundException, IOException {
        YbkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\atotc.ybk");
        String text = YbkRdr.readNextChapter();
        
        Log.d("testGetATOTC", text);
    }

    @Test
    public void testGetFirstFile() throws FileNotFoundException, IOException {
        YbkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\atotc.ybk");
        String text = YbkRdr.readChapter("\\ATOTC.HTML");
        
        Log.d("testGetChapter", text);
    }


}
