package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 20/4/16  */

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class NoSwipesViewPager extends ViewPager {

    private boolean enabled;

    public NoSwipesViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(this.enabled) {
            return super.onTouchEvent(ev);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(this.enabled) {
            return super.onInterceptTouchEvent(ev);
        }

        return false;
    }

    public void setPagingEnabled(boolean pagingEnabled)
    {
        this.enabled = pagingEnabled;
    }
}
