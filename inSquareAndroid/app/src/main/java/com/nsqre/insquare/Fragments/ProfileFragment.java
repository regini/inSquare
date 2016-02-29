package com.nsqre.insquare.Fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

public class ProfileFragment extends Fragment {

    private ListView ownedSquares, favouriteSquares, recentSquares;
    private SquareAdapter adapterOwned, adapterFavourite, adapterRecents;
    private ImageView profileImage;
    private InSquareProfile userProfile;
    private MapActivity rootActivity;
    private TextView username;
    private ArrayList<Square> ownedSquaresList, favouriteSquaresList;
    private TextView textOwnedSquares, textFavouriteSquares;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
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

        ownedSquaresList = rootActivity.getOwnedSquaresList();
        favouriteSquaresList = rootActivity.getFavouriteSquaresList();
        if (!ownedSquaresList.isEmpty()) {
            adapterOwned = new SquareAdapter(getActivity(), ownedSquaresList);
            ownedSquares.setAdapter(adapterOwned);
        } else {
            textOwnedSquares.setVisibility(View.INVISIBLE);
        }
        if (!favouriteSquaresList.isEmpty()) {
            adapterFavourite = new SquareAdapter(getActivity(), favouriteSquaresList);
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
        //recentSquares = (ListView) v.findViewById(R.id.squares_recents);
        profileImage = (ImageView) v.findViewById(R.id.user_avatar);
        username = (TextView) v.findViewById(R.id.userName);
        textOwnedSquares = (TextView) v.findViewById(R.id.text_owned_squares);
        textFavouriteSquares = (TextView) v.findViewById(R.id.text_favourite_squares);
        return v;
    }

    public void resetAdapters() {
        ownedSquaresList = rootActivity.getOwnedSquaresList();
        favouriteSquaresList = rootActivity.getFavouriteSquaresList();
        ownedSquares.setAdapter(null);
        favouriteSquares.setAdapter(null);
        if (!ownedSquaresList.isEmpty()) {
            adapterOwned = new SquareAdapter(getActivity(), ownedSquaresList);
            ownedSquares.setAdapter(adapterOwned);
            textOwnedSquares.setVisibility(View.VISIBLE);
        } else {
            textOwnedSquares.setVisibility(View.INVISIBLE);
        }
        if (!favouriteSquaresList.isEmpty()) {
            adapterFavourite = new SquareAdapter(getActivity(), favouriteSquaresList);
            favouriteSquares.setAdapter(adapterFavourite);
            textFavouriteSquares.setVisibility(View.VISIBLE);
        } else {
            textFavouriteSquares.setVisibility(View.INVISIBLE);
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
}
