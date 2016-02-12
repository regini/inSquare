package com.nsqre.insquare.Fragments.Helpers;/* Created by umbertosonnino on 9/2/16  */

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;


public class MapWrapperLayout extends FrameLayout {

    private static final String TAG = "MapWrapperLayout";

    public MapWrapperLayout(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "dispatchTouchEvent: DOOOOWN!");
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "dispatchTouchEvent: uuuuuuP!");
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
