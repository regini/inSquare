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
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.ImageConverter;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class ProfileFragment extends Fragment implements
        InSquareProfile.InSquareProfileListener
{

    private static ProfileFragment instance;

    private static final String TAG = "ProfileFragment";

    private static final String TAB_OWNED = "Create";
    private static final String TAB_FAVOURITE = "Preferite";

    private RecyclerView squaresRecyclerView;
    private RecyclerSquareAdapter adapterOwned;
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
        if(this.adapterOwned.getItemCount() == 0)
        {
            squaresRecyclerView.setVisibility(View.INVISIBLE);
            emptyText.setVisibility(View.VISIBLE);

            String message = getString(R.string.profile_empty_owned);
            emptyText.setText(message);
        }else
        {
            squaresRecyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.INVISIBLE);
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

        adapterOwned = new RecyclerSquareAdapter(getContext(), InSquareProfile.getOwnedSquaresList());
        squaresRecyclerView.setAdapter(adapterOwned);

        profileImage = (ImageView) v.findViewById(R.id.user_avatar);
        username = (TextView) v.findViewById(R.id.userName);
        emptyText = (TextView) v.findViewById(R.id.profile_text_empty);

        Bitmap icon = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.logo_icon_144);
        Bitmap circularBitmap = ImageConverter.getRoundedCornerBitmap(icon, 100);
        profileImage.setImageBitmap(circularBitmap);

        Bitmap bitmap = InSquareProfile.loadProfileImageFromStorage(getContext());
        if (bitmap == null) {
            if (!InSquareProfile.getPictureUrl().equals(""))
                new DownloadImageTask(profileImage, getContext()).execute(InSquareProfile.getPictureUrl());
        } else {
            profileImage.setImageBitmap(bitmap);
        }

        username.setText(InSquareProfile.getUsername());

        return v;
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
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged!");
        adapterOwned.notifyDataSetChanged();
    }

    @Override
    public void onFavChanged() {
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
