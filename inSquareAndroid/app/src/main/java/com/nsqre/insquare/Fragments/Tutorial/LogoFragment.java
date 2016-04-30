package com.nsqre.insquare.Fragments.Tutorial;/* Created by umbertosonnino on 26/4/16  */

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * It's the Abstract class that handles the Fragment with a big logo on top, a title and a content below the title.
 * It also handles the animation of the corresponding fragment.
 */
public abstract class LogoFragment extends Fragment {

    protected ImageView logo;
    protected TextView title, content;

    public void animateLogo()
    {
        ViewCompat.animate(logo)
                .scaleY(1).scaleX(1)
                .setStartDelay(300)
                .setDuration(300)
                .start();
    }

    public void animateText()
    {
        ViewCompat.animate(title)
                .scaleY(1).scaleX(1)
                .setStartDelay(150)
                .setDuration(300)
                .start();

        ViewCompat.animate(content)
                .scaleY(1).scaleX(1)
                .setStartDelay(150)
                .setDuration(300)
                .start();
    }
}
