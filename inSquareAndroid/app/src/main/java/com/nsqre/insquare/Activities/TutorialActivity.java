package com.nsqre.insquare.Activities;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.Tutorial.FirstTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.FourthTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.LogoFragment;
import com.nsqre.insquare.Fragments.Tutorial.SecondTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.ThirdTutorialFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.pixelcan.inkpageindicator.InkPageIndicator;

/**
 * Shows the tutorial of the app to the user
 */
public class TutorialActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private static final String TAG = "TutorialActivity";

    private ViewPager vpager;
    private Button skipButton, endButton;
    ImageButton nextButton;
    private TutorialPagerAdapter pagerAdapter;
    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(vpager.getCurrentItem() == 0) {
            this.onPageSelected(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        skipButton = (Button) findViewById(R.id.review_button_skip);
        View.OnClickListener endClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InSquareProfile.setShowTutorial(false, getApplicationContext());
                finish();
            }
        };
        skipButton.setOnClickListener( endClickListener );
        endButton = (Button) findViewById(R.id.review_button_fine);
        endButton.setOnClickListener(endClickListener);

        nextButton = (ImageButton) findViewById(R.id.review_button_next);

        vpager = (ViewPager) findViewById(R.id.review_viewpager);
        pagerAdapter = new TutorialPagerAdapter(getSupportFragmentManager());
        vpager.setAdapter(pagerAdapter);

        vpager.addOnPageChangeListener(this);

        InkPageIndicator indicators = (InkPageIndicator) findViewById(R.id.review_inkindicator);
        indicators.setViewPager(vpager);
    }

    /**
     * manages the scrolling through the pages of the tutorial, setting the right colors and the button's text
     *
     * @see #changeStatusBarColor(int, float, int)
     */
    @Override
    public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
        int nextColorPosition = position + 1;
        if (nextColorPosition >= pagerAdapter.getCount()) {
            nextColorPosition %= pagerAdapter.getCount();
        }
        if (position < (vpager.getAdapter().getCount() - 1)) {
            skipButton.setVisibility(View.VISIBLE);
            endButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            vpager.setCurrentItem(position+1, true);
                        }
                    }
            );

            int pageColor = getPageColor(position);
            vpager.setBackgroundColor((Integer) argbEvaluator.evaluate(positionOffset, pageColor, getPageColor(nextColorPosition)));
            ((FrameLayout) vpager.getParent()).setBackgroundColor((Integer) argbEvaluator.evaluate(positionOffset, pageColor, getPageColor(nextColorPosition)));
            changeStatusBarColor(position, positionOffset, nextColorPosition);
        } else if (position == pagerAdapter.getCount() - 1) {
            skipButton.setVisibility(View.GONE);
            endButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.GONE);

            int pageColor = getPageColor(position);
            vpager.setBackgroundColor(getPageColor(pageColor));
            ((FrameLayout) vpager.getParent()).setBackgroundColor(pageColor);
            changeStatusBarColor(position, positionOffset, nextColorPosition);
            if (pagerAdapter.getItem(position).getView() != null) {
                pagerAdapter.getItem(position).getView().setAlpha(1 - positionOffset);
            }
        }
    }

    /**
     * sets the status bar color
     *
     * @see #getStatusBarColor(int)
     */
    private void changeStatusBarColor(int position, float positionOffset, int nextColorPosition) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            int color = getStatusBarColor(position);
            window.setStatusBarColor((Integer) argbEvaluator.evaluate(positionOffset, color, getPageColor(nextColorPosition)));
        }
    }

    /**
     * This method chooses the color to set to the status bar, using the actual position on the tutorial
     *
     * @param position The page of the tutorial selected
     * @return The color to set
     */
    private int getStatusBarColor(int position) {
        switch (position) {
            default:
            case 0:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_blue_grey_900);
            case 1:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_light_green_900);
            case 2:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_blue_900);
            case 3:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_red_900);
        }
    }

    /**
     * This method chooses the color to set to the page, using the actual position on the tutorial
     *
     * @param position The page of the tutorial selected
     * @return The color to set
     */
    private int getPageColor(int position) {
        switch (position) {
            case 0:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_blue_grey_700);
            case 1:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_light_green_700);
            case 2:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_blue_700);
            case 3:
                return ContextCompat.getColor(getApplicationContext(), R.color.md_red_700);
            default:
                return Color.TRANSPARENT;
        }
    }

    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected: " + position);
        LogoFragment visible = (LogoFragment) pagerAdapter.getItem(position);
        visible.animateLogo();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * Manages the back press event. If the user is not in the first page of the tutorial, it will scroll one page back
     */
    @Override
    public void onBackPressed() {
        int position = vpager.getCurrentItem();
        if (position == 0) {
            super.onBackPressed();
        } else {
            --position;
            vpager.setCurrentItem(position, true);
        }
    }

    /**
     * The adapter for the pages of the tutorial
     */
    public static class TutorialPagerAdapter extends FragmentPagerAdapter {

        private static final int NUM_ITEMS = 4;

        public TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Returns the fragment to show
         *
         * @param position The page the user is on
         * @return A Tutorial fragment
         * @see com.nsqre.insquare.Fragments.Tutorial
         */
        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return FirstTutorialFragment.newInstance();
                case 1:
                    return SecondTutorialFragment.newInstance();
                case 2:
                    return ThirdTutorialFragment.newInstance();
                case 3:
                    return FourthTutorialFragment.newInstance();
                default:
                    return new BlankFragment();
            }

        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }
    }
}
