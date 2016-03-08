package com.nsqre.insquare.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.SquareAdapter;

public class RecentSquaresFragment extends Fragment {

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
