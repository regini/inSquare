package com.nsqre.insquare.Activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.FavSquaresFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.ProfileFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.R;

public class BottomNavActivity extends AppCompatActivity
{
    private enum TABS
    {
        MAP, RECENT, PROFILE, OTHER
    }

    private static final String TAG = "BottomNavActivity";

    AHBottomNavigation bottomNavigation;
    FrameLayout mainContentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav);

        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_nav_bar);

        setupBottomNavigation();

        mainContentFrame = (FrameLayout) findViewById(R.id.bottom_nav_content_frame);

        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, MapFragment.newInstance()).commit();
    }

    private void setupBottomNavigation() {

        final String mapTabName = getString(R.string.bottom_nav_tab_map);
        final String profileTabName = getString(R.string.bottom_nav_tab_profile);
        final String favTabName = getString(R.string.bottom_nav_tab_favs);
        final String recentsTabName = getString(R.string.bottom_nav_tab_recent);
        final String otherTabName = getString(R.string.bottom_nav_tab_other);

        AHBottomNavigationItem mapItem = new AHBottomNavigationItem(mapTabName, R.drawable.bottom_bar_map);
        AHBottomNavigationItem profileItem = new AHBottomNavigationItem(profileTabName, R.drawable.bottom_bar_profile);
        AHBottomNavigationItem favsItem = new AHBottomNavigationItem(favTabName, R.drawable.heart_white);
        AHBottomNavigationItem recentsItem = new AHBottomNavigationItem(recentsTabName, R.drawable.bottom_bar_recent);
        AHBottomNavigationItem othersItem = new AHBottomNavigationItem(otherTabName, R.drawable.bottom_bar_more);

        bottomNavigation.addItem(mapItem);
        bottomNavigation.addItem(profileItem);
        bottomNavigation.addItem(favsItem);
        bottomNavigation.addItem(recentsItem);
        bottomNavigation.addItem(othersItem);

        bottomNavigation.setColored(false);
        // Set background color
        bottomNavigation.setDefaultBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        bottomNavigation.setAccentColor(ContextCompat.getColor(getApplicationContext(), R.color.white));

        bottomNavigation.setOnTabSelectedListener(
                new AHBottomNavigation.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(int position, boolean wasSelected) {
                        Fragment f;

                        // position dipende dall'ordine di inserimento tramite bottomNavigation#addItem()
                        switch (position) {
                            case 0: // Selezionata Mappa
                                f = MapFragment.newInstance();
                                break;
                            case 1: // Selezionato Profilo
                                f = ProfileFragment.newInstance();
                                break;
                            case 2:
                                f = FavSquaresFragment.newInstance();
                                break;
                            case 3: // Selezionato Recenti
                                f = RecentSquaresFragment.newInstance();
                                break;
                            case 4: // Selezionato Altro
                                f = new BlankFragment();
                                break;
                            default:
                                f = new BlankFragment();
                        }
                        getSupportFragmentManager().beginTransaction().replace(R.id.bottom_nav_content_frame, f).commit();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }
}
