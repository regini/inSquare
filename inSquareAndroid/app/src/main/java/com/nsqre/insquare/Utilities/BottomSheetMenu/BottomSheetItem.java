package com.nsqre.insquare.Utilities.BottomSheetMenu;/* Created by umbertosonnino on 4/4/16  */

import android.support.annotation.DrawableRes;

public class BottomSheetItem
{
    private int drawableRes;
    private String title;

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
