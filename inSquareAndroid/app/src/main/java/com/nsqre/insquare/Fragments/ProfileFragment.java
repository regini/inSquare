package com.nsqre.insquare.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.ImageConverter;
import com.nsqre.insquare.Utilities.Square;
import com.nsqre.insquare.Utilities.SquareAdapter;

import java.util.ArrayList;

public class ProfileFragment extends Fragment implements
        TabLayout.OnTabSelectedListener,
        InSquareProfile.InSquareProfileListener
{


    private static final String TAG = "ProfileFragment";

    private static final String TAB_OWNED = "Create";
    private static final String TAB_FAVOURITE = "Preferite";

    private ListView squaresList;
    private SquareAdapter adapterOwned, adapterFavourite;
    private ImageView profileImage;
    private TextView username, emptyText;
    private TabLayout tabLayout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        InSquareProfile.addListener(this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        //TODO sostituire con un placeholder del profilo
        //setta il placeholder, mentre attende il download dell'immagine
    }

    @Override
    public void onResume() {
        super.onResume();
        if(tabLayout != null) {
            onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
        }

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("event");
            Log.d(TAG, "Got message: " + message);
            if("deletion".equals(intent.getStringExtra("action"))) {
                String squareId = intent.getExtras().getString("squareId");
                if(InSquareProfile.isOwned(squareId)) {
                    InSquareProfile.removeOwned(squareId);
                }
                if(InSquareProfile.isFav(squareId)) {
                    InSquareProfile.removeFav(squareId);
                }
                if(InSquareProfile.isRecent(squareId)) {
                    InSquareProfile.removeRecent(squareId);
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        squaresList = (ListView) v.findViewById(R.id.squares_list);
        profileImage = (ImageView) v.findViewById(R.id.user_avatar);
        username = (TextView) v.findViewById(R.id.userName);
        tabLayout = (TabLayout) v.findViewById(R.id.profile_tab_layout);
        emptyText = (TextView) v.findViewById(R.id.profile_text_empty);

        Bitmap icon = BitmapFactory.decodeResource(rootActivity.getResources(),
                R.drawable.logo_icon_96);
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(icon, 100);
        profileImage.setImageBitmap(circularBitmap);

        adapterOwned = new SquareAdapter(getActivity(), InSquareProfile.getOwnedSquaresList());
        adapterFavourite = new SquareAdapter(getActivity(), InSquareProfile.getFavouriteSquaresList());

        if(!InSquareProfile.getPictureUrl().equals(""))
            new DownloadImageTask(profileImage).execute(InSquareProfile.getPictureUrl());
        username.setText(InSquareProfile.getUsername());

        setupTabLayout();

        return v;
    }

    private void setupTabLayout() {
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        TabLayout.Tab favouritesTab = tabLayout.newTab().setText(TAB_FAVOURITE);
        tabLayout.addTab(favouritesTab, 0); // Il numero specifica dove
        tabLayout.addTab(tabLayout.newTab().setText(TAB_OWNED), 1);
        tabLayout.setOnTabSelectedListener(this);

        onTabSelected(favouritesTab);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        InSquareProfile.removeListener(this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabSelected: I've selected " + tab.getText());

        if (tab.getText() == TAB_OWNED) {
            fillTab(InSquareProfile.getOwnedSquaresList(), adapterOwned, getString(R.string.profile_empty_owned));
        }

        if(tab.getText() == TAB_FAVOURITE) {
            fillTab(InSquareProfile.getFavouriteSquaresList(), adapterFavourite, getString(R.string.profile_empty_favourite));
        }
        adapterFavourite.notifyDataSetChanged();
        adapterOwned.notifyDataSetChanged();
    }

    private void fillTab(ArrayList<Square> list, SquareAdapter listAdapter, String message)
    {
        if(list.isEmpty())
        {
            squaresList.setVisibility(View.INVISIBLE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(message);
        }else
        {
            squaresList.setVisibility(View.VISIBLE);
            squaresList.setAdapter(listAdapter);
            emptyText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged!");
        adapterOwned = new SquareAdapter(getActivity(), InSquareProfile.getOwnedSquaresList());
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
    }

    @Override
    public void onFavChanged() {
        Log.d(TAG, "onFavChanged!");
        adapterFavourite = new SquareAdapter(getActivity(), InSquareProfile.getFavouriteSquaresList());
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
