package com.jackcholt.revel;

import static org.junit.Assert.*;

import org.junit.Test;

import com.googlecode.autoandroid.positron.PositronAPI;
import com.googlecode.autoandroid.positron.junit4.TestCase;

public class BasicUsageStories extends TestCase {

    private final String PKG = "com.jackcholt.revel";
    
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
