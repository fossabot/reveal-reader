package com.jackcholt.reveal;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jackcholt.reveal.Log;
import com.jackcholt.reveal.Util;
import com.jackcholt.reveal.YbkFileReader;

public class TestYbkFileReader {
    YbkFileReader ybkRdr;
    
    @Before
    public void setUp() throws Exception {
        ybkRdr = new YbkFileReader("c:\\pogp.ybk");
    }

    @After
    public void tearDown() throws Exception {
        ybkRdr = null;
    }

    @Test
    public void testGetBookTitle() {
        assertTrue(ybkRdr.getBookTitle().startsWith("P<small>"));
    }
    
    @Test
    public void testGetBindingText() {
        String bindingText = ybkRdr.getBindingText();
        
        Log.d("testGetBindingText", bindingText);
    }
    
    @Test
    public void testGetBookMetaData() {
        Log.i("TestYbkReader", "Book meta data: " + ybkRdr.getBookMetaData());
    }

    @Test
    public void testGetBindingFile() throws IOException {
        String text = ybkRdr.readBindingFile();
        
        assertTrue(text.startsWith("<a href=\"PoGP.html\">"));
        Log.d("reveal", text);
    }

    @Test
    public void testGetOrderCfg() throws IOException {
        List<String> orderList = ybkRdr.getOrderList();
        
        for (String order : orderList) {
            Log.d("testGetOrderCfg", "Chapter name: " + order);
        }
    }

    @Test
    public void testGetChapters() throws IOException {
        String text = ybkRdr.readInternalFile("\\POGP\\MOSES\\6.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6: " + text);
        
        text = ybkRdr.readInternalFile("\\POGP\\MOSES\\6_.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6 footnotes: " + text);
    }
    
    @Test
    public void testGetNextChapter() {
        String text = ybkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);

        text = ybkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);
    }

    @Test
    public void testGetPrevChapter() {
        ybkRdr.readNextChapter();        
        ybkRdr.readNextChapter();
        String text = ybkRdr.readPrevChapter();
        
        Log.d("testGetPrevChapter", text);
    }

    @Test
    public void testGetPrevChapterNoPrev() {
        String text = ybkRdr.readPrevChapter();

        assertNull(text);
    }

    @Test
    public void testGetChapter() {
        String text = ybkRdr.readChapter("\\pogp\\abr\\1.html");
        
        Log.d("testGetChapter", text);
    }

    @Test
    public void testGetFacsimile() {
        String text = ybkRdr.readChapter("\\pogp\\abr\\fac_3.html");
        
        Log.d("testGetFacsimile", text);
    }

    @Test
    public void testGetImage() throws IOException {
        byte[] image = ybkRdr.readImage("pogp/images/facsimile3.gif");
        
        assertNotNull(image);
        assertTrue(new String(image).startsWith("GIF89", 0));
    }

    @Test
    public void testGetFormattedTitle() {
        assertTrue(!Util.formatTitle(ybkRdr.getBookTitle()).contains("<"));
    }

    @Test
    public void testGetATOTC() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\atotc.ybk");
        String text = ybkRdr.readNextChapter();
        
        Log.d("testGetATOTC", text);
    }

    @Test
    public void testGetFirstFile() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\atotc.ybk");
        String text = ybkRdr.readChapter("\\ATOTC.HTML");
        
        Log.d("testGetChapter", text);
    }

    @Test
    public void testGetInternalFile() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\sh.ybk");
        String contents = ybkRdr.readChapter("\\sh\\tg\\f");
        Log.d("testGetInternalFile", "Topical Guide F: " + contents);
    }

    @Test
    public void testGetTG_F_() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\sh.ybk");
        String contents = ybkRdr.readChapter("\\sh\\tg\\f_");
        Log.d("testGetInternalFile", "Topical Guide F notes: " + contents);
    }

    @Test
    public void testGetTG_F() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\sh.ybk");
        String contents = ybkRdr.readChapter("\\sh\\tg\\f");
        Log.d("testGetInternalFile", "Topical Guide F: " + contents);
    }

    @Test
    public void testGetBM() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\sh.ybk");
        String contents = ybkRdr.readChapter("\\sh\\bm");
        Log.d("testGetInternalFile", "Bible Maps: " + contents);
    }

    @Test
    public void testGetSH() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\sh.ybk");
        String contents = ybkRdr.readChapter("\\sh.html.gz");
        Log.d("testGetInternalFile", "Bible Maps: " + contents);
    }

    @Test
    public void testGetTotPotCJS() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\js.ybk");
        String contents = ybkRdr.readChapter("\\js\\5");
        Log.d("testGetTotPotCJS", "JS Repentance: " + contents);
    }
}