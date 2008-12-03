package com.jackcholt.revel;

import android.graphics.drawable.Drawable;

public class IconifiedText implements Comparable<IconifiedText> {
   
     private String mText = "";
     private Drawable mIcon;
     private boolean mEnabled = true;
     private String mFilePath;

     public IconifiedText(final String text, final Drawable bullet, final String filePath) {
          mIcon = bullet;
          mText = text;
          mFilePath = filePath;
     }
     
     public boolean isEnabled() {
          return mEnabled;
     }
     
     public void setEnabled(final boolean enabled) {
          mEnabled = enabled;
     }
     
     public String getText() {
          return mText;
     }
     
     public void setText(final String text) {
          mText = text;
     }
     
     public void setIcon(final Drawable icon) {
          mIcon = icon;
     }
     
     public Drawable getIcon() {
          return mIcon;
     }

     /** Make IconifiedText comparable by its name */
     public int compareTo(final IconifiedText other) {
          if(mText != null) {
               return mText.toLowerCase()
               .compareTo(other.getText().toLowerCase());
          } else {
               throw new IllegalArgumentException();
          }
     }

    /**
     * @return the filePath
     */
    public final String getFilePath() {
        return mFilePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public final void setFilePath(final String filePath) {
        mFilePath = filePath;
    }
     

}

