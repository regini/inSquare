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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerSquareAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.ImageConverter;

import java.util.ArrayList;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class ProfileFragment extends Fragment implements
        TabLayout.OnTabSelectedListener,
        InSquareProfile.InSquareProfileListener
{

    private static ProfileFragment instance;

    private static final String TAG = "ProfileFragment";

    private static final String TAB_OWNED = "Create";
    private static final String TAB_FAVOURITE = "Preferite";

    private RecyclerView squaresRecyclerView;
    private RecyclerSquareAdapter adapterOwned, adapterFavourite;
//    private SquareAdapter adapterOwned, adapterFavourite;
    private ImageView profileImage;
    private TextView username, emptyText;
    private TabLayout tabLayout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        if(instance == null){
            instance = new ProfileFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * TODO ??
     */
    @Override
    public void onStart() {
        super.onStart();
        InSquareProfile.addListener(this);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        InSquareProfile.downloadAllSquares();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(tabLayout != null) {
            onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
        }
    }

    /**
     * TODO ???
     */
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

    /**
     * Initialized the view of this fragment setting the lists of favourite and owned squares
     * and the profile image(downloading it if not saved in the local storage)
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The view created
     * @see DownloadImageTask
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        squaresRecyclerView = (RecyclerView) v.findViewById(R.id.profile_squares_recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        squaresRecyclerView.setLayoutManager(linearLayoutManager);

        profileImage = (ImageView) v.findViewById(R.id.user_avatar);
        username = (TextView) v.findViewById(R.id.userName);
        tabLayout = (TabLayout) v.findViewById(R.id.profile_tab_layout);
        emptyText = (TextView) v.findViewById(R.id.profile_text_empty);


        Bitmap icon = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.logo_icon_144);
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(icon, 100);
        profileImage.setImageBitmap(circularBitmap);

        adapterOwned = new RecyclerSquareAdapter(getContext(), InSquareProfile.getOwnedSquaresList());
        adapterFavourite = new RecyclerSquareAdapter(getContext(), InSquareProfile.getFavouriteSquaresList());
//        adapterOwned = new SquareAdapter(getContext(), InSquareProfile.getOwnedSquaresList());
//        adapterFavourite = new SquareAdapter(getContext(), InSquareProfile.getFavouriteSquaresList());

        Bitmap bitmap = InSquareProfile.loadProfileImageFromStorage(getContext());
        if (bitmap == null) {
            if (!InSquareProfile.getPictureUrl().equals(""))
                new DownloadImageTask(profileImage, getContext()).execute(InSquareProfile.getPictureUrl());
        } else {
            profileImage.setImageBitmap(bitmap);
        }

        username.setText(InSquareProfile.getUsername());

        setupTabLayout();

        return v;
    }

    /**
     * Sets up the TabLayout
     */
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

    /**
     * Switches to the tab the user selected, checking if the lists have changed
     * @param tab The tab selected
     */
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

    /**
     * Fills the listAdapter with the list of squares, if the list is empty it shows the message
     * @param list The list of squares to show
     * @param listAdapter The adapter which manages the list of squares
     * @param message The message shown if the list is empty
     */
    private void fillTab(ArrayList<Square> list, RecyclerSquareAdapter listAdapter, String message)
    {
        if(list.isEmpty())
        {
            squaresRecyclerView.setVisibility(View.INVISIBLE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText(message);
        }else
        {
            squaresRecyclerView.setVisibility(View.VISIBLE);
            squaresRecyclerView.setAdapter(listAdapter);
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
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
    }

    @Override
    public void onFavChanged() {
        Log.d(TAG, "onFavChanged!");
        onTabSelected(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()));
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
