package com.nsqre.insquare.Fragments.Tutorial;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.cleveroad.slidingtutorial.PageFragment;
import com.cleveroad.slidingtutorial.SimplePagerFragment;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.LoginActivity;
import com.nsqre.insquare.Fragments.Tutorial.FirstTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.FourthTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.SecondTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.ThirdTutorialFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;

/**
 */
public class TutorialFragment extends SimplePagerFragment {

    private static final String TAG = "TutorialFragment";

    @Override
    protected int getPagesCount() {
        return 4;
    }

    @Override
    protected PageFragment getPage(int position) {
        if (position == 0)
            return new FirstTutorialFragment();
        if (position == 1)
            return new SecondTutorialFragment();
        if (position == 2)
            return new ThirdTutorialFragment();
        if (position == 3)
            return new FourthTutorialFragment();
        throw new IllegalArgumentException("Unknown position: " + position);
    }

    @ColorInt
    @Override
    protected int getPageColor(int position) {
        if (position == 0)
            return ContextCompat.getColor(getContext(), R.color.colorAccentDark);
        if (position == 1)
            return ContextCompat.getColor(getContext(), android.R.color.holo_green_dark);
        if (position == 2)
            return ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark);
        if (position == 3)
            return ContextCompat.getColor(getContext(), android.R.color.holo_orange_dark);
        if (position == 4)
            return ContextCompat.getColor(getContext(), android.R.color.holo_purple);
        if (position == 5)
            return ContextCompat.getColor(getContext(), android.R.color.darker_gray);
        return Color.TRANSPARENT;
    }

    @Override
    protected boolean isInfiniteScrollEnabled() {
        return false;
    }

    @Override
    protected boolean onSkipButtonClicked(View skipButton) {
        InSquareProfile.setShowTutorial(false, getContext());
        Log.d(TAG, "onSkipButtonClicked: " + InSquareProfile.getShowTutorial());
        removeFragmentFromScreen();
        if (getActivity().getClass() == LoginActivity.class) {
            LoginActivity loginActivity = (LoginActivity) getContext();
            loginActivity.launchLoginProcedure();
        } else {  //sono nel fragment settings
            BottomNavActivity bottomNavActivity = (BottomNavActivity) getActivity();
            bottomNavActivity.findViewById(R.id.bottom_nav_bar).setVisibility(View.VISIBLE);
        }
        return true;
    }

    @Override
    public void onPageSelected(int position) {
        if (!isInfiniteScrollEnabled() && position == getPagesCount()) {
            InSquareProfile.setShowTutorial(false, getContext());
            Log.d(TAG, "onPageSelected: " + InSquareProfile.getShowTutorial());
            removeFragmentFromScreen();

            if (getActivity().getClass() == LoginActivity.class) {
                LoginActivity loginActivity = (LoginActivity) getContext();
                loginActivity.launchLoginProcedure();
            } else {  //sono nel fragment settings
                BottomNavActivity bottomNavActivity = (BottomNavActivity) getActivity();
                bottomNavActivity.findViewById(R.id.bottom_nav_bar).setVisibility(View.VISIBLE);
            }
        }
    }

    private void removeFragmentFromScreen() {
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();
    }
}