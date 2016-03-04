package com.nsqre.insquare.Fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
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
import com.nsqre.insquare.Utilities.SquareAdapter;

public class ProfileFragment extends Fragment implements TabLayout.OnTabSelectedListener {


    private static final String TAG = "ProfileFragment";

    private static final String TAB_OWNED = "Create";
    private static final String TAB_FAVOURITE = "Preferite";

    private ListView squaresList;
    private SquareAdapter adapterOwned, adapterFavourite;
    private ImageView profileImage;
    private InSquareProfile userProfile;
    private MapActivity rootActivity;
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
        rootActivity = (MapActivity) getActivity();
        userProfile = InSquareProfile.getInstance(rootActivity.getApplicationContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        //TODO sostituire con un placeholder del profilo
        //setta il placeholder, mentre attende il download dell'immagine
        if(tabLayout.getTabCount() == 0)
            setupTabLayout();
    }

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

        adapterOwned = new SquareAdapter(getActivity(), InSquareProfile.ownedSquaresList);
        adapterFavourite = new SquareAdapter(getActivity(), InSquareProfile.favouriteSquaresList);

        new DownloadImageTask(profileImage)
                .execute(userProfile.getPictureUrl());
        username.setText(userProfile.getUsername());


        return v;
    }

    private void setupTabLayout() {
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addTab(tabLayout.newTab().setText(TAB_FAVOURITE));
        tabLayout.addTab(tabLayout.newTab().setText(TAB_OWNED));
        tabLayout.setOnTabSelectedListener(this);

        //gestisce il caso in cui non ho piazze create
        //tablayout.gettab con l'index e poi .select(), non triggera onTabSelected
        if (!InSquareProfile.favouriteSquaresList.isEmpty()) {
//        if (!InSquareProfile.ownedSquaresList.isEmpty()) {
            squaresList.setAdapter(adapterFavourite);
//            squaresList.setAdapter(adapterOwned);
            emptyText.setVisibility(View.INVISIBLE);
        } else {
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(getString(R.string.profile_empty_owned));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: I've just paused!");
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "onTabSelected: I've selected " + tab.getText());

        adapterFavourite.notifyDataSetChanged();
        adapterOwned.notifyDataSetChanged();

        if (tab.getText() == TAB_OWNED) {
            if (!InSquareProfile.ownedSquaresList.isEmpty()) {
                squaresList.setAdapter(adapterOwned);
                emptyText.setVisibility(View.INVISIBLE);
            } else {
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText(getString(R.string.profile_empty_owned));
            }
        }

        if(tab.getText() == TAB_FAVOURITE) {
            if (!InSquareProfile.favouriteSquaresList.isEmpty()) {
                squaresList.setAdapter(adapterFavourite);
                emptyText.setVisibility(View.INVISIBLE);
            } else {
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText(getString(R.string.profile_empty_favourite));
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        onTabSelected(tab);
    }
}
