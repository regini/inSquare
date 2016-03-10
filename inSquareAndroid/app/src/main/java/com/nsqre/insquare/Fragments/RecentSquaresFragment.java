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
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.SquareAdapter;

public class RecentSquaresFragment extends Fragment implements InSquareProfile.InSquareProfileListener {

    private static final String TAG = "RecentSquaresFragment";

    private MapActivity rootActivity;
    private ListView listRecent;
    private SquareAdapter adapterRecents;

    public RecentSquaresFragment() {
        // Required empty public constructor
    }

    public static RecentSquaresFragment newInstance() {
        RecentSquaresFragment fragment = new RecentSquaresFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootActivity = (MapActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recent_squares, container, false);
        listRecent = (ListView) v.findViewById(R.id.squares_recents);
        adapterRecents = new SquareAdapter(rootActivity, InSquareProfile.getRecentSquaresList());
        listRecent.setAdapter(adapterRecents);
        return v;
    }

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
        listRecent.setAdapter(adapterRecents);
    }
}
