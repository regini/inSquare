package com.nsqre.insquare.Activities;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.nsqre.insquare.Fragments.CreateSquare.ChooseCreateFragment;
import com.nsqre.insquare.Fragments.CreateSquare.ReviewCreateFragment;
import com.nsqre.insquare.Fragments.CreateSquare.SquareCreateFragment;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.NoSwipesViewPager;
import com.nsqre.insquare.Utilities.SquareType;
import com.pixelcan.inkpageindicator.InkPageIndicator;

import java.util.ArrayList;

/**
 * The Activity which gives the user the possibility to create his own Square
 */
public class CreateSquareActivity extends AppCompatActivity {

    public static final String RESULT_SQUARE_NAME = "name";
    public static final String RESULT_SQUARE_DESCRIPTION = "description";
    public static final String RESULT_SQUARE_LATITUDE = "latitude";
    public static final String RESULT_SQUARE_LONGITUDE = "longitude";
    public static final String RESULT_SQUARE_TYPE = "type";
    public static final String RESULT_SQUARE_FACEBOOK_ID = "facebookId";
    public static final String RESULT_EXPIRE_TIME = "expireTime";
    public CreateSquarePagerAdapter pagerAdapter;
    public NoSwipesViewPager vpager;

    public Button nextButton, previousButton;
    public String mapLongitude, mapLatitude;

    SquareCreateFragment eventCreationFragment;
    ReviewCreateFragment reviewFragment;

    public boolean isTypeChosen = false;
    public SquareType squareType;
    private static final String TAG = "CreateSquareActivity";

    // DATI DA RITORNARE ALLA MAPPA PER LA CREAZIONE
    private String resultName, resultDescription, resultLat, resultLon, resultFacebookId, resultExpireTime;

    /**
     * TODO documentare
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.logo_icon_144);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(getString(R.string.app_name),
                            icon, Color.parseColor("#D32F2F"));
            setTaskDescription(taskDesc);
        }

        setContentView(R.layout.activity_crea_square);

        resultName = resultDescription = resultFacebookId = resultLat = resultLon = resultExpireTime = "";

        Bundle data = getIntent().getExtras();
        this.mapLatitude = data.getString("latitude");
        this.mapLongitude = data.getString("longitude");

        Log.d(TAG, "onCreate: Just received " + mapLatitude + ", " + mapLongitude);

        vpager = (NoSwipesViewPager) findViewById(R.id.create_square_viewpager);
        // Create an adapter
        pagerAdapter = new CreateSquarePagerAdapter(getSupportFragmentManager());
        vpager.setAdapter(pagerAdapter);
        vpager.setPagingEnabled(false);

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
                                SquareCreateFragment create = (SquareCreateFragment) pagerAdapter.getItem(position);
                                if(create.getInsertedName().isEmpty())
                                    nextButton.setVisibility(View.GONE);
                                else
                                    nextButton.setVisibility(View.VISIBLE);
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

                        switch (position)
                        {
                            case 1:
                                SquareCreateFragment create = (SquareCreateFragment) pagerAdapter.getItem(position);
                                ReviewCreateFragment review = (ReviewCreateFragment) pagerAdapter.getItem(position+1);

                                String name = create.getInsertedName();
                                String descr = create.getInsertedDescription();

                                if(squareType == SquareType.TYPE_EVENT)
                                {
                                    String expireTime = create.getExpireTime();
                                    String eventTime = create.getEventTime();
                                    review.setupEventInfo(name, descr, eventTime,
                                            "", // Nessun nome della strada
                                            "", // Nessun Sito
                                            "", // Nessun Id Facebook
                                            mapLatitude, mapLongitude, expireTime);
                                }else if(squareType == SquareType.TYPE_SHOP)
                                {
                                    review.setupShopInfo(
                                            name,
                                            "", // Nessuna fascia di prezzo via input
                                            descr,
                                            "", // Nessun LikeCount via input
                                            "", // Nessun sito via input
                                            "", // Nessun telefono via input
                                            "", // Nessuna strada via input
                                            new ArrayList<String>(), // Lista vuota degli orari
                                            "", // Nessun ID Facebook
                                            mapLatitude, mapLongitude // Premendo il tasto, non prendo i dati da Facebook

                                    );
                                }else if(squareType == SquareType.TYPE_PLACE)
                                {
                                    review.setupPlaceInfo(name, descr, mapLatitude, mapLongitude);
                                }

                                ++position;
                                vpager.setCurrentItem(position, true);

                                break;
                            case 2:
                                // Dati necessari per tornare alla mappa
                                Intent resultIntent = new Intent();

                                resultIntent.putExtra(RESULT_SQUARE_NAME, resultName);
                                resultIntent.putExtra(RESULT_SQUARE_DESCRIPTION, resultDescription);
                                resultIntent.putExtra(RESULT_SQUARE_LATITUDE, resultLat);
                                resultIntent.putExtra(RESULT_SQUARE_LONGITUDE, resultLon);

                                if(CreateSquareActivity.this.squareType == SquareType.TYPE_EVENT)
                                {
                                    // type = 1 e' il valore della Square-Evento sul server
                                    resultIntent.putExtra(RESULT_SQUARE_TYPE, "1");
                                    resultIntent.putExtra(RESULT_EXPIRE_TIME, resultExpireTime);
                                    resultIntent.putExtra(RESULT_SQUARE_FACEBOOK_ID, resultFacebookId);
                                    setResult(MapFragment.RESULT_SQUARE_FACEBOOK, resultIntent);
                                }else if(CreateSquareActivity.this.squareType == SquareType.TYPE_SHOP)
                                {
                                    // type = 2 e' il valore della Square-Shop sul server
                                    resultIntent.putExtra(RESULT_SQUARE_TYPE, "2");
                                    resultIntent.putExtra(RESULT_SQUARE_FACEBOOK_ID, resultFacebookId);
                                    setResult(MapFragment.RESULT_SQUARE_FACEBOOK, resultIntent);
                                }else
                                {
                                    setResult(MapFragment.RESULT_SQUARE, resultIntent);
                                }
                                finish();
                                break;
                            case 0:
                            default:
                                ++position;
                                vpager.setCurrentItem(position, true);
                                break;
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
                            if(position == 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    Window window = getWindow();
                                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                                    window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccentDark));
                                }
                            }
                        }
                    }
                }
        );
    }

    /**
     * TODO documentare
     */
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

    public void circularReveal(View v)
    {
        int cx = v.getMeasuredWidth() / 2;
        int cy = v.getMeasuredHeight() / 2;

        int finalRadius = Math.max(v.getWidth(), v.getHeight()) / 2;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
           ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
        }
        v.setVisibility(View.VISIBLE);

    }

    public void setupResults(String name, String description, String lat, String lon, String id, String time)
    {
        this.resultName = name;
        this.resultDescription = description;
        this.resultLat = lat;
        this.resultLon = lon;
        this.resultFacebookId = id;
        this.resultExpireTime = time;
    }

    /**
     * TODO documentare
     */
    public void squareTypeSelected(SquareType placeType)
    {
        if(!isTypeChosen)
        {
            circularReveal(nextButton);
        }

        if(pagerAdapter.getCount() > 1) {
            this.squareType = placeType;
            this.isTypeChosen = true;
            SquareCreateFragment fragment = (SquareCreateFragment) pagerAdapter.getItem(1);
            fragment.setLayoutType();
        }else
        {
            Log.d(TAG, "squareTypeSelected: I'm having trouble with the next page?");
        }
    }

    /**
     * TODO documentare
     */
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
