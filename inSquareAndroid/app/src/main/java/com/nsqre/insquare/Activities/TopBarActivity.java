package com.nsqre.insquare.Activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.nsqre.insquare.Fragments.BlankFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.R;

import java.util.ArrayList;
import java.util.List;

public class TopBarActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 4;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_bar);
        toolbar = (Toolbar) findViewById(R.id.top_bar_toolbar);
        setSupportActionBar(toolbar);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.top_bar_viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.top_bar_tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();

        tabLayout.getTabAt(1).select();
    }

    private void setupTabIcons()
    {
        tabLayout.getTabAt(0).setIcon(R.drawable.bottom_bar_more);
        tabLayout.getTabAt(1).setIcon(R.drawable.bottom_bar_map);
        tabLayout.getTabAt(2).setIcon(R.drawable.bottom_bar_profile);
        tabLayout.getTabAt(3).setIcon(R.drawable.bottom_bar_recent);

    }

    private void setupViewPager(ViewPager pager)
    {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new BlankFragment(), "Altro");
        adapter.addFragment(new MapFragment(), "Mappa");
        adapter.addFragment(new BlankFragment(), "Profilo");
        adapter.addFragment(new BlankFragment(), "Recenti");

        pager.setAdapter(adapter);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter
    {

        final private List<Fragment> fragmentList = new ArrayList<>();
        final private List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment newFragment, String fragmentName)
        {
            this.fragmentList.add(newFragment);
            this.fragmentTitleList.add(fragmentName);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }
}
