package com.jackcholt.reveal;

import android.app.Activity;

/**
 * RevNotesDialog for online "Whats New in this Version" system
 * 
 * by Dave Packham, Shon Vella
 */

public class RevNotesDialog extends PopupDialogBase {
    private final static String PREFIX = "REV";
    private final static String URL = "http://revealreader.thepackhams.com/revNotes/rev" + Global.SVN_VERSION + ".xml";
    
    public RevNotesDialog(Activity parent) {
        super(parent, parent.getResources().getString(R.string.version_notes_title), URL, PREFIX);
    }

    public static void create(final Activity parent) {
       new RevNotesDialog(parent);
    }
}
