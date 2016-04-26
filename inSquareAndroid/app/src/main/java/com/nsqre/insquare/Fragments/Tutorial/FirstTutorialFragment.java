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
public class FirstTutorialFragment extends LogoFragment
{
    private static FirstTutorialFragment instance;
    private static final String TAG = "FirstTutorialFragment";

    public static FirstTutorialFragment newInstance() {

        if(instance == null)
        {
            instance = new FirstTutorialFragment();
        }

        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_first_tutorial, container, false);

        logo = (ImageView) v.findViewById(R.id.first_tutorial_logo);

        return v;
    }


}
