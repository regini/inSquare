package com.nsqre.insquare.Fragments.Tutorial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nsqre.insquare.R;

/**
 */
public class ThirdTutorialFragment extends LogoFragment
{
    private static ThirdTutorialFragment instance;
    private static final String TAG = "ThirdTutorialFragment";

    public static ThirdTutorialFragment newInstance() {

        if(instance == null)
        {
            instance = new ThirdTutorialFragment();
        }

        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_third_tutorial, container, false);

        logo = (ImageView) v.findViewById(R.id.third_tutorial_logo);

        return v;
    }
}
