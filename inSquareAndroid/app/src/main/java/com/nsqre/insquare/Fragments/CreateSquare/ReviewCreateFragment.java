package com.nsqre.insquare.Fragments.CreateSquare;


import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nsqre.insquare.Activities.CreateSquareActivity;
import com.nsqre.insquare.R;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReviewCreateFragment extends Fragment {

    private static final String TAG = "ReviewCreateFragment";
    private static ReviewCreateFragment instance;

    private CreateSquareActivity father;

    View containerView;
    private String id, latitude, longitude;
    private TextView squareName, squareDescription;

    public ReviewCreateFragment() {
        // Required empty public constructor
    }

    public static ReviewCreateFragment newInstance() {

        if(instance == null)
        {
            instance = new ReviewCreateFragment();
        }

        return instance;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        this.father = (CreateSquareActivity) getActivity();

        // Inflate the layout for this fragment
        final ChooseCreateFragment.SQUARE_TYPE requestType = father.squareType;

        if(requestType != null)
        {
            switch (requestType) {
                case TYPE_PLACE:
                case TYPE_EVENT:
                    // TODO handle also these type of layouts!!
                case TYPE_SHOP:
                    break;
            }

        }
        containerView = inflater.inflate(R.layout.fragment_place_review, container, false);

        return containerView;
    }

    public void setupShopInfo(
            String name, String price,
            String description, String likeCount, String website, String phone, String street, List<String> hours,
            String facebookId, String latitude, String longitude
    )
    {
        father.setupResults(name, description, latitude, longitude, id, "");

        this.id = facebookId;
        this.latitude = latitude;
        this.longitude = longitude;

        this.setupShopTopSection(name, price);
        this.setupShopLowerSection(description, likeCount, website, phone, street, hours);
    }

    private void setupShopLowerSection(String description, String likeCount, String website, String phone, String streetName, List<String> hours)
    {
        squareDescription = (TextView) containerView.findViewById(R.id.review_description_text);
        TextView squareLikes = (TextView) containerView.findViewById(R.id.review_like_number);
        TextView squareWebsite = (TextView) containerView.findViewById(R.id.review_website);
        TextView squarePhone = (TextView) containerView.findViewById(R.id.review_phone);
        TextView squareStreet = (TextView) containerView.findViewById(R.id.review_street_name);

        LinearLayout hoursList = (LinearLayout) containerView.findViewById(R.id.review_list_hours);

        if(hours.isEmpty())
        {
            ((LinearLayout)hoursList.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)hoursList.getParent()).setVisibility(View.GONE);
            // Puliamo il layout e prepariamolo ad essere riempito
            hoursList.removeAllViews();
            for(String s: hours)
            {
                TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.simple_list_item, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                Resources r = getContext().getResources();
                int px = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8,
                        r.getDisplayMetrics()
                );
                params.setMargins(0, 0, px, px);
                tv.setLayoutParams(params);
                tv.setText(s);
                hoursList.addView(tv);
            }
        }

        potentialEmptySection(squareDescription, description);
        potentialEmptySection(squareLikes, likeCount);
        potentialEmptySection(squareWebsite, website);
        potentialEmptySection(squarePhone, phone);
        potentialEmptySection(squareStreet, streetName);

    }


    private void setupShopTopSection(String name, String priceRange)
    {
        TextView squareInitials = (TextView) containerView.findViewById(R.id.review_square_initials);
        squareName = (TextView) containerView.findViewById(R.id.review_square_name);
        TextView squarePrice = (TextView) containerView.findViewById(R.id.review_square_price_range);

        squareName.setText(name);

        squarePrice.setVisibility(View.GONE);

        String initials = "";
        String[] words = name.split("\\s+");
        if(words.length <= 1)
        {
            initials = name.substring(0,1).toUpperCase();
        }else if(words.length == 2)
        {
            initials = words[0].substring(0,1).toUpperCase() + words[1].substring(0,1).toUpperCase();
        }else {
            initials = words[0].substring(0,1).toUpperCase() + words[1].substring(0,1).toUpperCase() + words[2].substring(0,1).toUpperCase();

        }

        squareInitials.setText(initials);

    }

    public void setupEventInfo(
            String name, String description, String time, String street, String website,
            String facebookId, String latitude, String longitude,
            String expireString)
    {
        father.setupResults(name, description, latitude, longitude, facebookId, expireString);

        // Setup delle sezioni vuote
        TextView squareLikes = (TextView) containerView.findViewById(R.id.review_like_number);
        potentialEmptySection(squareLikes, "");
        TextView squarePhone = (TextView) containerView.findViewById(R.id.review_phone);
        potentialEmptySection(squarePhone, "");

        this.id = facebookId;
        this.latitude = latitude;
        this.longitude = longitude;
        setupEventTopSection(name);
        setupEventLowerSection(description, time, street, website);
    }

    private void setupEventLowerSection(String description, String time, String street, String website)
    {
        squareDescription = (TextView) containerView.findViewById(R.id.review_description_text);

        TextView squareWebsite = (TextView) containerView.findViewById(R.id.review_website);
        TextView squareStreet = (TextView) containerView.findViewById(R.id.review_street_name);

        potentialEmptySection(squareStreet, street);

        potentialEmptySection(squareWebsite, website);
        potentialEmptySection(squareDescription, description);

        LinearLayout timeSection = (LinearLayout) containerView.findViewById(R.id.review_list_hours);
        if(time.isEmpty())
        {
            ((LinearLayout)timeSection.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)timeSection.getParent()).setVisibility(View.VISIBLE);
            timeSection.removeAllViews();
            TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.simple_list_item, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Resources r = getContext().getResources();
            int px = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8,
                    r.getDisplayMetrics()
            );
            params.setMargins(0, 0, px, px);
            tv.setLayoutParams(params);
            tv.setText(time);
            timeSection.addView(tv);
        }

    }

    private void setupEventTopSection(String name)
    {
        TextView squareInitials =  (TextView) containerView.findViewById(R.id.review_square_initials);
        squareName = (TextView) containerView.findViewById(R.id.review_square_name);
        TextView squarePrice = (TextView) containerView.findViewById(R.id.review_square_price_range);

        squareName.setText(name);

        // No Price Range per gli eventi
        squarePrice.setVisibility(View.GONE);

        String initials = "";
        String[] words = name.split("\\s+");
        if(words.length <= 1)
        {
            initials = name.substring(0,1).toUpperCase();
        }else if(words.length == 2)
        {
            initials = words[0].substring(0,1).toUpperCase() + words[1].substring(0,1).toUpperCase();
        }else {
            initials = words[0].substring(0,1).toUpperCase() + words[1].substring(0,1).toUpperCase() + words[2].substring(0,1).toUpperCase();
            initials = initials.replaceAll("[^\\p{L}\\p{Z}]","");
        }

        squareInitials.setText(initials);
    }


    private void potentialEmptySection(TextView v, String value)
    {
        if(value.isEmpty()){
            ((LinearLayout)v.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)v.getParent()).setVisibility(View.VISIBLE);
        }

        v.setText(value);
    }
}
