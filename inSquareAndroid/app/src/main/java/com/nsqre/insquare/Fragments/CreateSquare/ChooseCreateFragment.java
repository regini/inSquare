package com.nsqre.insquare.Fragments.CreateSquare;


import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nsqre.insquare.Activities.CreateSquareActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.SquareType;

/**
 * A Fragment that gives the user the possibility to choose which kind of square he wants to create
 */
public class ChooseCreateFragment extends Fragment {

    private static ChooseCreateFragment instance;

    CreateSquareActivity upperActivity;
    FrameLayout choicePlace, choiceEvent, choiceShop;
    ImageView overlayPlace, overlayEvent, overlayShop;

    TextView textPlace, textEvent, textShop;

    public ChooseCreateFragment() {
        // Required empty public constructor
    }

    public static ChooseCreateFragment newInstance() {

        if(instance == null)
        {
            instance = new ChooseCreateFragment();
        }

        return instance;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Sets up the layout and manages the click on every choice
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_create_choose, container, false);

        upperActivity = (CreateSquareActivity)getActivity();

        choicePlace = (FrameLayout) v.findViewById(R.id.create_square_choice_place);
        choiceEvent = (FrameLayout) v.findViewById(R.id.create_square_choice_event);
        choiceShop = (FrameLayout) v.findViewById(R.id.create_square_choice_shop);

        overlayPlace = (ImageView) v.findViewById(R.id.create_square_choice_place_overlay);
        overlayEvent = (ImageView) v.findViewById(R.id.create_square_choice_event_overlay);
        overlayShop = (ImageView) v.findViewById(R.id.create_square_choice_shop_overlay);

        Drawable placeOverlayBackground = overlayPlace.getBackground();
        Drawable eventOverlayBackground = overlayEvent.getBackground();
        Drawable shopOverlayBackground = overlayShop.getBackground();

        textPlace = (TextView) v.findViewById(R.id.create_square_choice_place_text);
        textEvent = (TextView) v.findViewById(R.id.create_square_choice_event_text);
        textShop = (TextView) v.findViewById(R.id.create_square_choice_shop_text);

        choicePlace.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upperActivity.squareTypeSelected(SquareType.TYPE_PLACE);
                        enhanceText(textPlace);
                        resetText(textEvent);
                        resetText(textShop);
                        upperActivity.nextButton.performClick();

                    }
        });

        choiceEvent.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        upperActivity.squareTypeSelected(SquareType.TYPE_EVENT);
                        enhanceText(textEvent);
                        resetText(textPlace);
                        resetText(textShop);
                        upperActivity.nextButton.performClick();
                    }
                }
        );

        choiceShop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upperActivity.squareTypeSelected(SquareType.TYPE_SHOP);
                        enhanceText(textShop);
                        resetText(textPlace);
                        resetText(textEvent);
                        upperActivity.nextButton.performClick();
                    }
                }
        );

        return v;
    }


    /**
     * Enhances the text of a text view
     * @param v the text view
     */
    private void enhanceText(TextView v)
    {
        v.setShadowLayer(3f, 0, 0, Color.WHITE);
        v.setPaintFlags(textPlace.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG );
    }

    /**
     * Reset the text of a text view
     * @param v the text view
     */
    private void resetText(TextView v)
    {
        v.setShadowLayer(1f, 1,1, Color.BLACK);
        v.setPaintFlags(0);
    }
}
