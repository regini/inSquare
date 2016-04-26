package com.nsqre.insquare.Fragments.Tutorial;/* Created by umbertosonnino on 26/4/16  */

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.widget.ImageView;

public abstract class LogoFragment extends Fragment {

    protected ImageView logo;

    public void animateLogo()
    {
        ViewCompat.animate(logo)
                .scaleY(1).scaleX(1)
                .setStartDelay(800)
                .setDuration(500)
                .start();
    }
}
