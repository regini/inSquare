package com.nsqre.insquare.Fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

public class ProfileFragment extends Fragment {


    private static final String TAG = "ProfileFragment";

    private ListView ownedSquares, favouriteSquares;
    private SquareAdapter adapterOwned, adapterFavourite;
    private ImageView profileImage;
    private InSquareProfile userProfile;
    private MapActivity rootActivity;
    private TextView username;
    private TextView textOwnedSquares, textFavouriteSquares;

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
        Bitmap icon = BitmapFactory.decodeResource(rootActivity.getResources(),
                R.drawable.logo_icon_96);
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(icon, 100);
        profileImage.setImageBitmap(circularBitmap);


        if (InSquareProfile.ownedSquaresList.isEmpty()) {
            adapterOwned = new SquareAdapter(getActivity(), InSquareProfile.ownedSquaresList);
            ownedSquares.setAdapter(adapterOwned);
        } else {
            textOwnedSquares.setVisibility(View.INVISIBLE);
        }
        if (!InSquareProfile.favouriteSquaresList.isEmpty()) {
            adapterFavourite = new SquareAdapter(getActivity(), InSquareProfile.favouriteSquaresList);
            favouriteSquares.setAdapter(adapterFavourite);
        } else {
            textFavouriteSquares.setVisibility(View.INVISIBLE);
        }
        new DownloadImageTask(profileImage)
                .execute(userProfile.getPictureUrl());
        username.setText(userProfile.getUsername());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        ownedSquares = (ListView) v.findViewById(R.id.squares_owned);
        favouriteSquares = (ListView) v.findViewById(R.id.squares_preferred);
        profileImage = (ImageView) v.findViewById(R.id.user_avatar);
        username = (TextView) v.findViewById(R.id.userName);
        textOwnedSquares = (TextView) v.findViewById(R.id.text_owned_squares);
        textFavouriteSquares = (TextView) v.findViewById(R.id.text_favourite_squares);
        return v;
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
}
