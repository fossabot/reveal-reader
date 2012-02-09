package com.jackcholt.reveal;

import android.app.Activity;

/**
 * Checks for updates to the MOTD and displays them
 * 
 * by Dave Packham, Shon Vella
 */

public class MOTDDialog extends PopupDialogBase {
    private final static String PREFIX = "MOTD";
    private final static String URL = "http://revealreader.thepackhams.com/revealMOTD.xml";

    public MOTDDialog(Activity parent) {
        super(parent, parent.getResources().getString(R.string.motd_title), URL, PREFIX);
    }

    public static void create(final Activity parent) {
        new MOTDDialog(parent);
    }
}
