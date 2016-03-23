package com.nsqre.insquare.Activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

public class MainActivity extends AppCompatActivity
    implements TabLayout.OnTabSelectedListener
{
    private enum TABS
    {
        MAP, RECENT, PROFILE
    }
    private static final String TAG = "MainActivity";
    private static final String TAB_MAP = "Mappa";
    private static final String TAB_PROFILE = "Profile";
    private static final String TAB_RECENT = "Recenti";

    TabLayout tabLayout;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupTabLayout();

        Fragment f = selectFragmentFromName(TAB_MAP);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_content_frame,f).commit();
    }

    private void setupToolbar()
    {
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
    }

    private void setupTabLayout()
    {
        tabLayout = (TabLayout) findViewById(R.id.main_tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addTab(tabLayout.newTab().setText(TAB_MAP));
//        tabLayout.addTab(tabLayout.newTab().setText(TAB_PROFILE));
        tabLayout.addTab(tabLayout.newTab().setText(TAB_RECENT));
        tabLayout.setOnTabSelectedListener(this);
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabSelected: I've selected " + tab.getText());

        Fragment f = selectFragmentFromName(tab.getText().toString());

        getSupportFragmentManager()
                .beginTransaction().replace(R.id.main_content_frame, f).commit();
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

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
}
