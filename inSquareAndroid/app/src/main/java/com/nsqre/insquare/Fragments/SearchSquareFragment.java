package com.nsqre.insquare.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.SquareAdapter;

/**
 * This fragment show the recent squares used by the user. The recent squares are those in which he wrote at least a message
 */
public class SearchSquareFragment extends Fragment implements InSquareProfile.InSquareProfileListener {

    private static final String TAG = "SearchSquaresFragment";

    private MapActivity rootActivity;
    private ListView listSearched;
    private SquareAdapter adapterRecents;

    public SearchSquareFragment() {
        // Required empty public constructor
    }

    public static SearchSquareFragment newInstance() {
        SearchSquareFragment fragment = new SearchSquareFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootActivity = (MapActivity) getActivity();
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
        View v = inflater.inflate(R.layout.fragment_search_squares, container, false);
        listSearched = (ListView) v.findViewById(R.id.squares_search);
        adapterRecents = new SquareAdapter(rootActivity, InSquareProfile.getRecentSquaresList());
        listSearched.setAdapter(adapterRecents);
        return v;
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
    }

    @Override
    public void onResume() {
        super.onResume();
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
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);
        InSquareProfile.removeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: I've just paused!");
    }

    @Override
    public void onOwnedChanged() {

    }

    @Override
    public void onFavChanged() {

    }

    @Override
    public void onRecentChanged() {
        adapterRecents = new SquareAdapter(rootActivity, InSquareProfile.getRecentSquaresList());
        listSearched.setAdapter(adapterRecents);
    }
}
