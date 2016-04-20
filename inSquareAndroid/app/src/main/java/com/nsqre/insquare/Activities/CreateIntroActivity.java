package com.nsqre.insquare.Activities;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;

import com.nsqre.insquare.Fragments.CreateSquare.ChooseCreateFragment;
import com.nsqre.insquare.Fragments.CreateSquare.ReviewCreateFragment;
import com.nsqre.insquare.Fragments.CreateSquare.SquareCreateFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.R;
import com.pixelcan.inkpageindicator.InkPageIndicator;

public class CreateIntroActivity extends AppCompatActivity {

    public static final String RESULT_SQUARE_NAME = "name";
    public static final String RESULT_SQUARE_DESCRIPTION = "description";
    public static final String RESULT_SQUARE_LATITUDE = "latitude";
    public static final String RESULT_SQUARE_LONGITUDE = "longitude";
    public static final String RESULT_SQUARE_TYPE = "type";
    public static final String RESULT_SQUARE_FACEBOOK_ID = "facebookId";
    public CreateSquarePagerAdapter pagerAdapter;
    private ViewPager vpager;

    public Button nextButton, previousButton;
    public String longitude, latitude;

    SquareCreateFragment eventCreationFragment;
    ReviewCreateFragment reviewFragment;

    public boolean isTypeChosen = false;
    public ChooseCreateFragment.SQUARE_TYPE squareType;
    private static final String TAG = "CreateIntroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crea_square);

        Bundle data = getIntent().getExtras();
        this.latitude = data.getString("latitude");
        this.longitude = data.getString("longitude");

        Log.d(TAG, "onCreate: Just received " + latitude + ", " + longitude);

        vpager = (ViewPager) findViewById(R.id.create_square_viewpager);
        // Create an adapter
        pagerAdapter = new CreateSquarePagerAdapter(getSupportFragmentManager());
        vpager.setAdapter(pagerAdapter);

        vpager.addOnPageChangeListener(
                new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        buttonsShowHide(position);
                    }

                    @Override
                    public void onPageSelected(int position) {
                        buttonsShowHide(position);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }

                    private void buttonsShowHide(int position)
                    {
                        switch (position)
                        {
                            case 0:
                                nextButton.setText("Avanti");
                                previousButton.setVisibility(View.GONE);
                                break;
                            case 1:
                                if(previousButton.getVisibility() == View.GONE)
                                {
                                    circularReveal(previousButton);
                                }
                                nextButton.setVisibility(View.GONE);
                                nextButton.setText("Avanti");
                                break;
                            case 2:
                                nextButton.setVisibility(View.VISIBLE);
                                nextButton.setText("Crea!");
                                break;
                        }
                    }
                }
        );

        InkPageIndicator indicators = (InkPageIndicator) findViewById(R.id.create_square_inkindicator);
        indicators.setViewPager(vpager);

        nextButton = (Button) findViewById(R.id.create_square_button_next);
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = vpager.getCurrentItem();
                        if(position < 2)
                        {
                            ++position;
                            vpager.setCurrentItem(position, true);
                        }else if(position == pagerAdapter.getCount()-1) // Sto all'ultima pagina e avanzo per creare la square
                        {
                            ReviewCreateFragment rcf = (ReviewCreateFragment) pagerAdapter.getItem(position);
                            if(!rcf.longitude.isEmpty() && !rcf.latitude.isEmpty())
                            {
                                String name = rcf.squareName.getText().toString();
                                String description = rcf.squareDescription.getText().toString();
                                String lat = rcf.latitude;
                                String lon = rcf.longitude;
                                String facebookId = rcf.id;

                                Intent resultIntent = new Intent();

                                resultIntent.putExtra(RESULT_SQUARE_NAME, name);
                                resultIntent.putExtra(RESULT_SQUARE_DESCRIPTION, description);
                                resultIntent.putExtra(RESULT_SQUARE_LATITUDE, lat);
                                resultIntent.putExtra(RESULT_SQUARE_LONGITUDE, lon);
                                switch (CreateIntroActivity.this.squareType)
                                {
                                    case TYPE_EVENT:

                                        // type = 1 e' il valore della Square-Evento sul server
                                        resultIntent.putExtra(RESULT_SQUARE_TYPE, "1");
                                        resultIntent.putExtra(RESULT_SQUARE_FACEBOOK_ID, facebookId);
                                        setResult(MapFragment.RESULT_SQUARE_FACEBOOK, resultIntent);

                                        break;
                                    case TYPE_SHOP:
                                        // type = 2 e' il valore della Square-Evento sul server
                                        resultIntent.putExtra("type", "2");
                                        resultIntent.putExtra(RESULT_SQUARE_FACEBOOK_ID, facebookId);
                                        setResult(MapFragment.RESULT_SQUARE_FACEBOOK, resultIntent);
                                        break;
                                    default:
                                        setResult(MapFragment.RESULT_SQUARE, resultIntent);

                                        break;

                                }

                                Log.d(TAG, "onClick: finishing this guy!");
                                finish();
                            }
                        }
                    }
                }
        );
        previousButton = (Button) findViewById(R.id.create_square_button_prev);
        previousButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = vpager.getCurrentItem();
                        if(position > 0)
                        {
                            --position;
                            vpager.setCurrentItem(position, true);
                        }
                    }
                }
        );
    }

    public static class CreateSquarePagerAdapter extends FragmentPagerAdapter
    {

        private static final int NUM_ITEMS = 3;

        public CreateSquarePagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {

            switch (position)
            {
                case 0:
                    return ChooseCreateFragment.newInstance();
                case 1:
                    return SquareCreateFragment.newInstance();
                case 2:
                    return ReviewCreateFragment.newInstance();
                default:
                    return null;
            }

        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
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

    public void squareTypeSelected(ChooseCreateFragment.SQUARE_TYPE placeType)
    {
        if(!isTypeChosen)
        {
            circularReveal(nextButton);
        }

        if(pagerAdapter.getCount() > 1) {
            this.squareType = placeType;
            Log.d(TAG, "squareTypeSelected: " + placeType.toString());
            this.isTypeChosen = true;
            SquareCreateFragment fragment = (SquareCreateFragment) pagerAdapter.getItem(1);
            fragment.setLayoutType();
        }else
        {
            Log.d(TAG, "squareTypeSelected: I'm having trouble with the next page?");
        }
    }

    @Override
    public void onBackPressed() {
        if(vpager.getCurrentItem() != 0)
        {
            previousButton.performClick();
        }else {
            super.onBackPressed();
        }
    }
}
