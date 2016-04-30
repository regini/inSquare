package com.nsqre.insquare.Square;/* Created by umbertosonnino on 6/4/16  */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.nsqre.insquare.R;

/**
 * unused
 */
public class RecyclerProfileSquareDecoration extends RecyclerView.ItemDecoration {

    private Drawable background;
    private Context context;

    public RecyclerProfileSquareDecoration(Context c)
    {
        this.context = c;
        int redColor = ContextCompat.getColor(context, R.color.colorAccent);
        background = new ColorDrawable(redColor);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if(parent.getItemAnimator().isRunning())
        {
            View comingDown = null;
            View goingUp = null;

            // Misure prefissate
            int left = 0;
            int right = parent.getWidth();
            // Misure da trovare
            int top = 0;
            int bottom = 0;

            int childCount = parent.getLayoutManager().getChildCount();
            for(int i = 0; i < childCount; i++)
            {
                View child = parent.getLayoutManager().getChildAt(i);
                if(child.getTranslationY() < 0)
                {
                    // E' la View che scende
                    comingDown = child;
                }else if(child.getTranslationY() > 0)
                {
                    if(goingUp == null)
                    {
                        goingUp = child;
                    }
                }
            }

            if(comingDown == null && goingUp == null)
            {
                top = (int) (comingDown.getBottom() + comingDown.getTranslationY());
                bottom = (int) (goingUp.getTop() + goingUp.getTranslationY());
            }else if(comingDown != null)
            {
                top = (int) (comingDown.getBottom() + comingDown.getTranslationY());
                bottom = comingDown.getBottom();
            }else if(goingUp != null)
            {
                top = goingUp.getTop();
                bottom = (int) (goingUp.getBottom() + goingUp.getTranslationY());
            }

            background.setBounds(left, top, right, bottom);
            background.draw(c);
        }

        super.onDraw(c, parent, state);
    }
}
