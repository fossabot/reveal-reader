package com.jackcholt.reveal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestYbkFileReader {
    YbkFileReader ybkRdr;
    
    @Before
    public void setUp() throws Exception {
        //ybkRdr = new YbkFileReader("c:\\pogp.ybk");
    }

    @After
    public void tearDown() throws Exception {
        ybkRdr = null;
    }

    /*@Test
    public void testGetBookTitle() {
        assertTrue(ybkRdr.getBookTitle().startsWith("P<small>"));
    }*/
    
    @Test
    public void testGetBindingText() {
        String bindingText = ybkRdr.getBindingText();
        
        Log.d("testGetBindingText", bindingText);
    }
    
    /*@Test
    public void testGetBookMetaData() {
        Log.i("TestYbkReader", "Book meta data: " + ybkRdr.getBookMetaData());
    }*/

    @Test
    public void testGetBindingFile() throws IOException {
        String text = ybkRdr.readBindingFile();
        
        assertTrue(text.startsWith("<a href=\"PoGP.html\">"));
        Log.d("reveal", text);
    }

    /*@Test
    public void testGetOrderCfg() throws IOException {
        List<String> orderList = ybkRdr.getOrderList();
        
        for (String order : orderList) {
            Log.d("testGetOrderCfg", "Chapter name: " + order);
        }
    }*/

    @Test
    public void testGetChapters() throws IOException {
        String text = ybkRdr.readInternalFile("\\POGP\\MOSES\\6.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6: " + text);
        
        text = ybkRdr.readInternalFile("\\POGP\\MOSES\\6_.HTML.GZ");
        Log.d("TestGetChapters", "Moses 6 footnotes: " + text);
    }
    
    /*@Test
    public void testGetNextChapter() {
        String text = ybkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);

        text = ybkRdr.readNextChapter();
        
        Log.d("testGetNextChapter", text);
    }*/

    /*@Test
    public void testGetPrevChapter() {
        ybkRdr.readNextChapter();        
        ybkRdr.readNextChapter();
        String text = ybkRdr.readPrevChapter();
        
        Log.d("testGetPrevChapter", text);
    }*/

    /*@Test
    public void testGetPrevChapterNoPrev() {
        String text = ybkRdr.readPrevChapter();

        assertNull(text);
    }*/

    /*@Test
    public void testGetChapter() {
        String text = ybkRdr.readChapter("\\pogp\\abr\\1.html");
        
        Log.d("testGetChapter", text);
    }*/

    /*@Test
    public void testGetFacsimile() {
        String text = ybkRdr.readChapter("\\pogp\\abr\\fac_3.html");
        
        Log.d("testGetFacsimile", text);
    }*/

    @Test
    public void testGetImage() throws IOException {
        byte[] image = ybkRdr.readImage("pogp/images/facsimile3.gif");
        
        assertNotNull(image);
        assertTrue(new String(image).startsWith("GIF89", 0));
    }

    /*@Test
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
    
    @Test
    public void testConvertAhtag() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\js.ybk");
        String contents = ybkRdr.readChapter("\\js\\5");
        Log.d("testConvertAhTag", "JS Repentance: " + contents);
        contents = Util.convertAhtag(contents);
        Log.d("testConvertAhTag", "JS Repentance: " + contents);
    }

    @Test
    public void testConvertIfvar() throws IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\js.ybk");
        String contents = ybkRdr.readChapter("\\js\\5");
        contents = Util.convertIfvar(contents);
        Log.d("testConvertIfvar", "JS Repentance: " + contents);
    }
    
    @Test
    public void testJS() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\js.ybk");
        String content = ybkRdr.readChapter("\\js.html");
        content = Util.convertAhtag(content);
        content = Util.convertIfvar(content);
        //content = Util.htmlize(content, PreferenceManager.getDefaultSharedPreferences(new MockContext()));
        
        Log.d("testJS", "JS: " + content);
    }

    @Test
    public void testNTSG() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\nt study guide.ybk");
        String content = ybkRdr.readChapter("\\nt study guide.html").replace('“', '"').replace('”', '"');
        assertNotNull(content);
        
        Log.d("testNTSG", "NTSG: " + content);
    }

    @Test
    public void testHymnsAlt314() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\Hymns_alt.ybk");
        String content = ybkRdr.readChapter("\\Hymns\\For Women.html");
        assertNotNull(content);
        
        Log.d("testHymnsAlt314", "314: " + content);
    }

    @Test
    public void testGAPK_JS() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\gapk.ybk");
        String content = ybkRdr.readChapter("\\gapk\\400.html");
        assertNotNull(content);
        
        Log.d("testGAPK_JS", "JS: " + content);
    }

    @Test
    public void testDaCSG_8_DC13() throws FileNotFoundException, IOException {
        ybkRdr = new YbkFileReader("c:\\documents and settings\\holtja\\my documents\\my ebooks\\DaCSG.ybk");
        String content = ybkRdr.readChapter("\\dacsg\\10.html");
        assertNotNull(content);
        
        Log.d("testDaCSG_8_DC13", "DaCSG: " + content);
    }

*/
}
