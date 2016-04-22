package com.nsqre.insquare.Activities;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.Tutorial.FirstTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.FourthTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.SecondTutorialFragment;
import com.nsqre.insquare.Fragments.Tutorial.ThirdTutorialFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.pixelcan.inkpageindicator.InkPageIndicator;


public class TutorialActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private ViewPager vpager;
    private Button skipButton;
    private TutorialPagerAdapter pagerAdapter;
    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        skipButton = (Button) findViewById(R.id.review_button_skip);
        skipButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        InSquareProfile.setShowTutorial(false, getApplicationContext());
                        finish();
                    }
                }
        );


        vpager = (ViewPager) findViewById(R.id.review_viewpager);
        pagerAdapter = new TutorialPagerAdapter(getSupportFragmentManager());
        vpager.setAdapter(pagerAdapter);

        vpager.addOnPageChangeListener(this);

        InkPageIndicator indicators = (InkPageIndicator) findViewById(R.id.review_inkindicator);
        indicators.setViewPager(vpager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int nextColorPosition = position + 1;
        if (nextColorPosition >= pagerAdapter.getCount()) {
            nextColorPosition %= pagerAdapter.getCount();
        }
        if (position < (vpager.getAdapter().getCount() - 1)) {
            skipButton.setText(R.string.tutorial_salta);

            int pageColor = getPageColor(position);
            vpager.setBackgroundColor((Integer) argbEvaluator.evaluate(positionOffset, pageColor, getPageColor(nextColorPosition)));
            ((FrameLayout)vpager.getParent()).setBackgroundColor((Integer) argbEvaluator.evaluate(positionOffset, pageColor, getPageColor(nextColorPosition)));
            changeStatusBarColor(position, positionOffset, nextColorPosition);
        } else if (position == pagerAdapter.getCount() - 1) {
            skipButton.setText(R.string.tutorial_fine);

            int pageColor = getPageColor(position);
            vpager.setBackgroundColor(getPageColor(pageColor));
            ((FrameLayout)vpager.getParent()).setBackgroundColor(pageColor);
            changeStatusBarColor(position, positionOffset, nextColorPosition);
            if (pagerAdapter.getItem(position).getView() != null) {
                pagerAdapter.getItem(position).getView().setAlpha(1 - positionOffset);
            }
        }
    }

    private void changeStatusBarColor(int position, float positionOffset, int nextColorPosition)
    {
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


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Animator circularReveal(View v)
    {
        int cx = v.getMeasuredWidth() / 2;
        int cy = v.getMeasuredHeight() / 2;

        int finalRadius = Math.max(v.getWidth(), v.getHeight()) / 2;

        Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
        v.setVisibility(View.VISIBLE);
        return anim;

    }

    private int getStatusBarColor(int position)
    {
        switch (position)
        {
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

    private int getPageColor(int position)
    {
        switch (position)
        {
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

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onBackPressed() {
        int position = vpager.getCurrentItem();
        if(position == 0)
        {
            super.onBackPressed();
        }else
        {
            --position;
            vpager.setCurrentItem(position, true);
        }
    }

    public static class TutorialPagerAdapter extends FragmentPagerAdapter
    {

        private static final int NUM_ITEMS = 4;

        public TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {

            switch (position)
            {
                case 0:
                    return new FirstTutorialFragment();
                case 1:
                    return new SecondTutorialFragment();
                case 2:
                    return new ThirdTutorialFragment();
                case 3:
                    return new FourthTutorialFragment();
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
