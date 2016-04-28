package com.nsqre.insquare.Utilities.BottomSheetMenu;/* Created by umbertosonnino on 4/4/16  */

import android.support.annotation.DrawableRes;

/**
 * An item of the bottom navigation bar
 */
public class BottomSheetItem
{
    private int drawableRes;
    private String title;

    /**
     * Creates a BottomSheetItem object
     * @param drawable the icon of the item
     * @param title the name of the item
     */
    public BottomSheetItem(@DrawableRes int drawable, String title)
    {
        this.drawableRes = drawable;
        this.title = title;
    }

    public int getDrawableRes() {
        return drawableRes;
    }

    public String getTitle() {
        return title;
    }
}
