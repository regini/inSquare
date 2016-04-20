package com.nsqre.insquare.Fragments.CreateSquare;


import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nsqre.insquare.Activities.CreateIntroActivity;
import com.nsqre.insquare.R;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReviewCreateFragment extends Fragment {

    private static final String TAG = "ReviewCreateFragment";
    private static ReviewCreateFragment instance;

    View containerView;
    public String id, latitude, longitude;
    public TextView squareName, squareDescription;

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
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: I'm inflating this guy");
        final ChooseCreateFragment.SQUARE_TYPE requestType = ((CreateIntroActivity)getActivity()).squareType;

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

    public void setupShopLowerSection(String description, String likeCount, String website, String phone, String streetName, List<String> hours)
    {
        squareDescription = (TextView) containerView.findViewById(R.id.review_description_text);
        TextView squareLikes = (TextView) containerView.findViewById(R.id.review_like_number);
        TextView squareWebsite = (TextView) containerView.findViewById(R.id.review_website);
        TextView squarePhone = (TextView) containerView.findViewById(R.id.review_phone);
        TextView squareStreet = (TextView) containerView.findViewById(R.id.review_street_name);

        LinearLayout hoursList = (LinearLayout) containerView.findViewById(R.id.review_list_hours);

        if(hours.isEmpty())
        {
            hoursList.setVisibility(View.GONE);
        }else {
            hoursList.setVisibility(View.VISIBLE);
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

    private void potentialEmptySection(TextView v, String value)
    {
        if(value.isEmpty()){
            ((LinearLayout)v.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)v.getParent()).setVisibility(View.VISIBLE);
        }

        v.setText(value);
    }

    public void setupShopTopSection(String name, String priceRange)
    {
        TextView squareInitials = (TextView) containerView.findViewById(R.id.review_square_initials);
        squareName = (TextView) containerView.findViewById(R.id.review_square_name);
        TextView squarePrice = (TextView) containerView.findViewById(R.id.review_square_price_range);

        squareName.setText(name);

        potentialEmptySection(squarePrice, priceRange);

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

    public void setupEventLowerSection(String description, String time, String street)
    {
        squareDescription = (TextView) containerView.findViewById(R.id.review_description_text);
        TextView squareLikes = (TextView) containerView.findViewById(R.id.review_like_number);
        TextView squareWebsite = (TextView) containerView.findViewById(R.id.review_website);
        TextView squarePhone = (TextView) containerView.findViewById(R.id.review_phone);
        TextView squareStreet = (TextView) containerView.findViewById(R.id.review_street_name);

        // Setup delle sezioni vuote
        potentialEmptySection(squareLikes, "");
        potentialEmptySection(squarePhone, "");

        potentialEmptySection(squareStreet, street);
        potentialEmptySection(squareWebsite, "www.facebook.com/events/" + this.id);
        potentialEmptySection(squareDescription, description);

        LinearLayout timeSection = (LinearLayout) containerView.findViewById(R.id.review_list_hours);
        if(time.isEmpty())
        {
            timeSection.setVisibility(View.GONE);
        }else {
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

    public void setupEventTopSection(String name)
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
}
