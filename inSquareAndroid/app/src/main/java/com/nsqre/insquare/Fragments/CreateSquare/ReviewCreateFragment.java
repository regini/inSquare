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
import com.nsqre.insquare.Utilities.SquareType;

import java.util.List;

/**
 * This is the last Fragment in the Square Creation process.
 * It contains all the informations that will be added to InSquare, this the creator can review what it's doing and eventually go back to change it.
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
        final SquareType requestType = father.squareType;

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

    /**
     * Firstly the upper activity is informed of all the relevant information that will be sent back to the calling activity.
     * Then the sections are set up in order to display all the available information to review.
     * @see #setupShopTopSection(String, String)
     * @see #setupShopLowerSection(String, String, String, String, String, List)
     * @param name
     * @param price
     * @param description
     * @param likeCount
     * @param website
     * @param phone
     * @param street
     * @param hours
     * @param facebookId
     * @param latitude
     * @param longitude
     */
    public void setupShopInfo(
            String name, String price,
            String description, String likeCount, String website, String phone, String street, List<String> hours,
            String facebookId, String latitude, String longitude
    )
    {
        father.setupResults(name, description, latitude, longitude, facebookId, "");

        this.id = facebookId;
        this.latitude = latitude;
        this.longitude = longitude;

        this.setupShopTopSection(name, price);
        this.setupShopLowerSection(description, likeCount, website, phone, street, hours);
        if(likeCount.isEmpty() && website.isEmpty() && phone.isEmpty() && street.isEmpty() && hours.isEmpty()){
            showDetailsSection(false);
        }else {
            showDetailsSection(true);
        }
    }

    /**
     * The lower section of the card is setup with the relevant information regarding this Square:
     * description, likes, website, phone number, street name and opening hours
     * @param description
     * @param likeCount
     * @param website
     * @param phone
     * @param streetName
     * @param hours
     */
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
            ((LinearLayout)hoursList.getParent()).setVisibility(View.VISIBLE);
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

    /**
     * The top part of the Shop contains the name and the price range (from Facebook) associated with this shop.
     * @param name
     * @param priceRange
     */
    private void setupShopTopSection(String name, String priceRange)
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

    /**
     * Firstly the information regarding this event is passed back to the Activity in order to have the information to be sent back to the one that lunached it.
     * Then it it delegates the information-filling process to two other methods
     * @see #setupEventTopSection(String, String)
     * @see #setupEventLowerSection(String, String, String)
     * N.B. An event does not have Likes and a Phone number
     * @param name
     * @param description
     * @param time
     * @param street
     * @param website
     * @param facebookId
     * @param latitude
     * @param longitude
     * @param expireString
     */
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
        setupEventTopSection(name,description);
        setupEventLowerSection(time, street, website);
        if(time.isEmpty() && street.isEmpty() && website.isEmpty())
        {
            showDetailsSection(false);
        }else {
            showDetailsSection(true);
        }
    }

    /**
     * The event lower section is filled with the relevant information provided
     * @param time
     * @param street
     * @param website
     */
    private void setupEventLowerSection(String time, String street, String website)
    {
        TextView squareWebsite = (TextView) containerView.findViewById(R.id.review_website);
        TextView squareStreet = (TextView) containerView.findViewById(R.id.review_street_name);

        potentialEmptySection(squareStreet, street);
        potentialEmptySection(squareWebsite, website);

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

    /**
     * It fills the top part of the CardView with the main information regarding this event
     * @param name
     * @param description
     */
    private void setupEventTopSection(String name, String description)
    {
        setupPlaceMainInfo(name, description);
    }

    /**
     * Check if the LinearLayout containing the TextView is empty.
     * If it hide it. Otherwise show it.
     * @param v the TextView contained in the LinearLayout
     * @param value the String value to be set in the TextView
     */
    private void potentialEmptySection(TextView v, String value)
    {
        if(value.isEmpty()){
            ((LinearLayout)v.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)v.getParent()).setVisibility(View.VISIBLE);
        }

        v.setText(value);
    }

    /**
     * It shows or hides the details section.
     * If an event or a page is created from Facebook this section will most likely have fields.
     * Otherwise it's better to hide it.
     * @param show
     */
    private void showDetailsSection(boolean show)
    {
        LinearLayout details = (LinearLayout) containerView.findViewById(R.id.review_details);

        if(show)
        {
            details.setVisibility(View.VISIBLE);
        }else {
            details.setVisibility(View.GONE);
        }
    }

    /**
     * The review card is filled with the information regarding this place, taken from the previous fragment.
     * @param name
     * @param description
     * @param squareLatitude
     * @param squareLongitude
     */
    public void setupPlaceInfo(String name, String description, String squareLatitude, String squareLongitude) {
        father.setupResults(name, description, squareLatitude, squareLongitude, "", "");
        this.latitude = squareLatitude;
        this.longitude = squareLongitude;

        setupPlaceMainInfo(name, description);
        // Nascondiamo tutto il resto
        showDetailsSection(false);
    }

    /**
     * The top part of the cardview is filled with the relevant information.
     * The Main Section is always visible since it contains all the basic information regarding the Square being created
     * @param name
     * @param description
     */
    private void setupPlaceMainInfo(String name, String description) {
        TextView squareInitials =  (TextView) containerView.findViewById(R.id.review_square_initials);
        squareName = (TextView) containerView.findViewById(R.id.review_square_name);
        TextView squarePrice = (TextView) containerView.findViewById(R.id.review_square_price_range);
        // No Price Range per gli eventi e luoghi
        potentialEmptySection(squarePrice, "");

        squareName.setText(name);


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

        squareDescription = (TextView) containerView.findViewById(R.id.review_description_text);
        potentialEmptySection(squareDescription, description);
    }
}
