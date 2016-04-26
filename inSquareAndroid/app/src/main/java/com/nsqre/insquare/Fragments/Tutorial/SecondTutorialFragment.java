package com.nsqre.insquare.Fragments.Tutorial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nsqre.insquare.R;

/**
 */
public class SecondTutorialFragment extends LogoFragment
{
    private static SecondTutorialFragment instance;
    private static final String TAG = "SecondTutorialFragment";

    public static SecondTutorialFragment newInstance() {

        if(instance == null)
        {
            instance = new SecondTutorialFragment();
        }

        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_second_tutorial, container, false);

        logo = (ImageView) v.findViewById(R.id.second_tutorial_logo);
        title = (TextView) v.findViewById(R.id.second_tutorial_title);
        content = (TextView) v.findViewById(R.id.second_tutorial_content);

        return v;
    }
}
