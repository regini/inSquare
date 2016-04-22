package com.nsqre.insquare.Fragments.MainContent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerSquareAdapter;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.ImageConverter;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * This fragment show the recent squares used by the user. The recent squares are those in which he wrote at least a message
 */
public class FavSquaresFragment extends Fragment implements InSquareProfile.InSquareProfileListener {

    private static FavSquaresFragment instance;
    private static final String TAG = "FavSquaresFragment";

//    private ListView listRecent;
    private RecyclerView recyclerListFavs;
    private RecyclerSquareAdapter adapterFavs;
    private Toolbar toolbar;
    private TextView favEmptyText;

    public FavSquaresFragment() {
        // Required empty public constructor
    }

    public static FavSquaresFragment newInstance() {

        if(instance == null)
        {
            instance = new FavSquaresFragment();
        }

        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Create and initializes the main view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The view of this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recent_squares, container, false);
//        listRecent = (ListView) v.findViewById(R.id.squares_recents);
        recyclerListFavs = (RecyclerView) v.findViewById(R.id.recyclerview_squares_recents);
        recyclerListFavs.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerListFavs.setLayoutManager(linearLayoutManager);

        adapterFavs = new RecyclerSquareAdapter(getActivity(), InSquareProfile.getFavouriteSquaresList());
        recyclerListFavs.setAdapter(adapterFavs);


        favEmptyText = (TextView) v.findViewById(R.id.recents_text_empty);

        setupToolbar(v);

        return v;
    }

    private void setupToolbar(View v) {
        toolbar = (Toolbar) v.findViewById(R.id.recents_toolbar);
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        parentActivity.setSupportActionBar(toolbar);

        CircleImageView profileImage = (CircleImageView) v.findViewById(R.id.recents_profile_image);
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

        TextView title = (TextView) v.findViewById(R.id.recents_title);
        title.setText(getString(R.string.bottom_nav_tab_favs));
    }

    /**
     * TODO ???
     */
    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        InSquareProfile.addListener(this);
        InSquareProfile.downloadAllSquares();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(this.adapterFavs.getItemCount() == 0)
        {
            recyclerListFavs.setVisibility(View.INVISIBLE);
            favEmptyText.setVisibility(View.VISIBLE);

            String message = getString(R.string.profile_empty_favourite);
            favEmptyText.setText(message);
        }else
        {
            recyclerListFavs.setVisibility(View.VISIBLE);
            favEmptyText.setVisibility(View.INVISIBLE);
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

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);
        InSquareProfile.removeListener(this);
    }

    @Override
    public void onOwnedChanged() {

    }

    @Override
    public void onFavChanged() {
        Log.d(TAG, "onFavChanged: something has changed in these favorites!");
        adapterFavs.notifyDataSetChanged();
    }

    @Override
    public void onRecentChanged() {
    }
}
