package com.nsqre.insquare.Fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.Square;
import com.nsqre.insquare.Utilities.SquareAdapter;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    ListView ownedSquares, preferredSquares, recentSquares;
    SquareAdapter adapterOwned, adapterPreferred, adapterRecents;

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
    }

    @Override
    public void onStart() {
        super.onStart();
        //una lista creata al volo tanto per, poi da cambiare con risultati della get
        ArrayList<Square> squaresTemp = new ArrayList<>();
        squaresTemp.add(new Square("ema", "piazza pia", 0,0,"boh", "emanuele"));
        squaresTemp.add(new Square("ema", "piazza pia1", 0, 0, "boh", "emanuele"));
        squaresTemp.add(new Square("ema", "piazza pia2", 0, 0, "boh", "emanuele"));

        adapterOwned = new SquareAdapter(getActivity(), squaresTemp, getResources());
        ownedSquares.setAdapter(adapterOwned);
        adapterPreferred = new SquareAdapter(getActivity(), squaresTemp, getResources());
        preferredSquares.setAdapter(adapterPreferred);
        //adapterRecents = new SquareAdapter(getActivity(), squaresTemp, getResources());
        //recentSquares.setAdapter(adapterRecents);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        ownedSquares = (ListView) v.findViewById(R.id.squares_owned);
        preferredSquares = (ListView) v.findViewById(R.id.squares_preferred);
        //recentSquares = (ListView) v.findViewById(R.id.squares_recents);
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

}
