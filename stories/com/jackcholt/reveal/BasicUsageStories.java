package com.jackcholt.reveal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.googlecode.autoandroid.positron.PositronAPI;
import com.googlecode.autoandroid.positron.junit4.TestCase;

public class BasicUsageStories extends TestCase {

    private final String PKG = "com.jackcholt.reveal";
    
    @Test
    public void shouldSeeMain() {
        startActivity(PKG, PKG + ".Main");
        pause();
        assertEquals("Main", stringAt("class.simpleName"));
    }
    
    @Test
    public void callSettings() {
        startActivity(PKG, PKG + ".Main");
        pause();
        assertEquals("Main", stringAt("class.simpleName"));
        resume();
        menu(PositronAPI.Menu.FIRST + 2);
        pause();
        assertEquals("Settings", stringAt("class.simpleName"));
    }
    
    @Test
    public void shouldSeeMenu() {
        startActivity(PKG, PKG + ".Main");
        sendKey(PositronAPI.Motion.ACTION_DOWN,PositronAPI.Key.MENU);
        pause();
        
    }
    
    @Test
    public void shouldSeeShowSplashScreen() {
        startActivity(PKG, PKG + ".Settings");
        pause();
        assertEquals("Settings", stringAt("class.simpleName"));
    
    }
}
