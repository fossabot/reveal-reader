package com.jackcholt.reveal;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class IconifiedTextListAdapter extends BaseAdapter {

    /** Remember our context so we can use it when constructing views. */
    private Context mContext;

    private List<IconifiedText> mItems = new ArrayList<IconifiedText>();

    public IconifiedTextListAdapter(final Context context) { 
        mContext = context;
    }

    /**
     * Add a new item to the list of IconifiedText.
     * 
     * @param it The IconifiedText to add.
     */
    public void addItem(final IconifiedText it) { mItems.add(it); }

    /**
     * Replace the list of IconifiedText.
     * 
     * @param lit The new list of IconifiedText.
     */
    public void setListItems(final List<IconifiedText> lit) { mItems = lit; }

    /** @return The number of items in the */
    public int getCount() { return mItems.size(); }

    /**
     * Get the IconifiedText at the passed position in the list.
     * 
     * @param position The position in the list to get the IconifiedText from.
     */
    public Object getItem(final int position) { return mItems.get(position); }

    /**
     * Are all items the list enabled? Always returns false.
     * 
     * @return false.
     */
    @Override
    public boolean areAllItemsEnabled() { return false; }

    /**
     * Is the item at the passed position enabled?
     * 
     * @param position The position at which to check.
     * @return true or false.
     */
    @Override
    public boolean isEnabled(final int position) {
        try {
            return mItems.get(position).isEnabled();
        } catch (IndexOutOfBoundsException aioobe){
            return super.isEnabled(position);
        }
    }

    /** Use the array index as a unique id. */
    public long getItemId(final int position) {
        return position;
    }

     /** @param convertView The old view to overwrite, if one is passed
      * @returns a IconifiedTextView that holds wraps around an IconifiedText */
     public View getView(int position, View convertView, ViewGroup parent) {
          IconifiedTextView btv;
          
          if (convertView == null) {
               btv = new IconifiedTextView(mContext, mItems.get(position));
          } else { // Reuse/Overwrite the View passed
               // We are assuming(!) that it is castable!
               btv = (IconifiedTextView) convertView;
               btv.setText(mItems.get(position).getText());
               btv.setIcon(mItems.get(position).getIcon());
          }
          
          return btv;
     }
}
