package com.nsqre.insquare.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cleveroad.slidingtutorial.PageFragment;
import com.cleveroad.slidingtutorial.TransformItem;
import com.nsqre.insquare.R;

/**
 */
public class FourthTutorialFragment extends PageFragment {

    @Override
    protected int getLayoutResId() {
        // layout id of fragment
        return R.layout.fragment_fourth_tutorial;
    }

    @Override
    protected TransformItem[] provideTransformItems() {
        // list of transformation items
        return new TransformItem[]{
                //new TransformItem(R.id.ivFirstImage, true, 20),
                new TransformItem(R.id.ivSecondImage, false, 6)
        };
    }
}
