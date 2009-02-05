package com.jackcholt.reveal;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestYbkTitleReader {
    YbkTitleReader YbkRdr;
    
    @Before
    public void setUp() throws Exception {
        YbkRdr = new YbkTitleReader("c:\\pogp.ybk");
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
    public void testGetFormattedTitle() {
        assertTrue(!Util.formatTitle(YbkRdr.getBookTitle()).contains("<"));
    }

}
