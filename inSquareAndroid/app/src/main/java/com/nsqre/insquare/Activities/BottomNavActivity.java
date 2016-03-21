package com.nsqre.insquare.Activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.R;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarFragment;

public class BottomNavActivity extends AppCompatActivity
{
    private enum TABS
    {
        MAP, RECENT, PROFILE, OTHER
    }

    private BottomBar mBottomBar;

    private static final String TAG = "BottomNavActivity";
    private static final String TAB_MAP = "Mappa";
    private static final String TAB_PROFILE = "Profilo";
    private static final String TAB_RECENT = "Recenti";
    private static final String TAB_OTHER = "Altro";

    TabLayout tabLayout;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_nav);

        mBottomBar = BottomBar.attach(findViewById(R.id.bottom_nav_content_frame), savedInstanceState);
        mBottomBar.noNavBarGoodness();

        mBottomBar.setFragmentItems(getSupportFragmentManager(),
                R.id.bottom_nav_content_frame,
                new BottomBarFragment(new MapFragment(), R.drawable.bottom_bar_map, TAB_MAP),
                new BottomBarFragment(new BlankFragment(), R.drawable.bottom_bar_recent, TAB_RECENT),
                new BottomBarFragment(new BlankFragment(), R.drawable.bottom_bar_profile, TAB_PROFILE),
                new BottomBarFragment(new BlankFragment(), R.drawable.bottom_bar_more, TAB_OTHER)
        );

        mBottomBar.mapColorForTab(0, ContextCompat.getColor(this, R.color.colorPrimary));
        mBottomBar.mapColorForTab(1, ContextCompat.getColor(this, R.color.colorAccent));
        mBottomBar.mapColorForTab(2, ContextCompat.getColor(this, R.color.colorAccentLight));
        mBottomBar.mapColorForTab(3, ContextCompat.getColor(this, R.color.colorAccentLightest));
    }

    private Fragment selectFragmentFromName(String tabName)
    {
        Fragment f = null;
        switch (tabName)
        {
            case TAB_MAP:
                return new MapFragment();
            case TAB_RECENT:
                return new RecentSquaresFragment();
        }

        return f;
    }
}
