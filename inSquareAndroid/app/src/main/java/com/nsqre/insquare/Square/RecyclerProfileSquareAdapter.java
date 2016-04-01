package com.nsqre.insquare.Square;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;

/**
 * Created by Umberto Sonnino on 29/03/2016.
 */
public class RecyclerProfileSquareAdapter extends RecyclerView.Adapter {

    private static final String TAG = "SquareAdapter";
    public static final String NOTIFICATION_MAP = "NOTIFICATION_MAP";
    private Context context;
    private ArrayList<Square> squaresArrayList;
    int i = 0;

    int[] backgroundColors = new int[]{
            R.color.md_amber_A100,
            R.color.md_orange_A100,
            R.color.colorAccentDark,
            R.color.md_purple_A100,
            R.color.md_deep_purple_A200,
            R.color.md_blue_100,
            R.color.md_teal_A400
    };

    public RecyclerProfileSquareAdapter(Context c, ArrayList<Square> squares) {
        this.context = c;
        this.squaresArrayList = squares;
    }

    @Override
    public SquareViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.square_card_profile, parent, false);
        SquareViewHolder squareViewHolder = new SquareViewHolder(v);
        return squareViewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final SquareViewHolder castHolder = (SquareViewHolder) holder;

        final Square listItem = this.squaresArrayList.get(position);

        setupHeart(castHolder, listItem);

        String squareName = listItem.getName();
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(listItem.formatTime());
        // Per sottolineare l'inizio
        String description = listItem.getDescription();
        if(description.length() > 0)
        {
            castHolder.squareDescription.setText("\t\t\t\t" + listItem.getDescription());
        }else
        {
            castHolder.middleSection.setVisibility(View.GONE);
        }
        setupLeftSection(castHolder, squareName);

        castHolder.lowerSectionViews.setText("Vista " + listItem.getViews() + " volte");
        castHolder.lowerSectionFavs.setText("Seguita da " + listItem.getViews() + " persone");
        castHolder.editButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                }
        );
        castHolder.trashButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                }
        );
    }

    private void setupLeftSection(SquareViewHolder castHolder, String squareName) {
        int position = castHolder.getAdapterPosition()%(backgroundColors.length);

        castHolder.squareInitials.setTextColor(
                ContextCompat.getColor(context, backgroundColors[position])
        );

        String initials = setupInitials(squareName);
        castHolder.squareInitials.setText(initials);
    }

    private String setupInitials(String words) {
        String[] division = words.split("\\s+");

        if(division.length <= 1)
        {
            return words.substring(0,1).toUpperCase();
        }
        else if(division.length == 2)
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase();
        }
        else
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase() + division[2].substring(0, 1).toUpperCase();
        }
    }

    private void setupHeart(final SquareViewHolder castHolder, final Square listItem) {
        if(InSquareProfile.isFav(listItem.getId())){
//            castHolder.squareFav.setImageResource(R.drawable.heart_black);
            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
        }

        castHolder.squareFav.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(InSquareProfile.isFav(listItem.getId()))
                        {
                            favouriteSquare(Request.Method.DELETE, listItem);
//                            castHolder.squareFav.setImageResource(R.drawable.heart_border_black);
                            castHolder.squareFav.setImageResource(R.drawable.like_96);
                        }else
                        {
                            favouriteSquare(Request.Method.POST, listItem);
//                            castHolder.squareFav.setImageResource(R.drawable.heart_black);
                            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
                        }
                    }
                }
        );
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return this.squaresArrayList.size();
    }

    private void favouriteSquare(final int method, final Square square) {

        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        }else
                        {
                            notifyDataSetChanged();
                            InSquareProfile.addFav(square);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responseDELETE - non sono riuscito ad rimuovere il fav " + square.toString());
                        }else
                        {
                            notifyDataSetChanged();
                            InSquareProfile.removeFav(square.getId());
                        }
                    }
                });
    }

    public void removeCard(SquareViewHolder viewholder) {
        Log.d(TAG, "removeCard: I'm swiping at position " + viewholder.getAdapterPosition());

        viewholder.squareCardBackground.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));

    }

    public static class SquareViewHolder extends RecyclerView.ViewHolder {

        LinearLayout squareCardBackground;
        CardView squareCardView;
        TextView squareInitials;
        TextView squareName;
        TextView squareActivity;
        TextView squareDescription;
        ImageView squareFav;
        ImageView expandArrow;

        RelativeLayout middleSection;

        LinearLayout lowerSectionExpanded;
        TextView lowerSectionFavs;
        TextView lowerSectionViews;

        ImageButton trashButton;
        ImageButton editButton;
        ImageView unexpandArrow;

        public SquareViewHolder(View itemView) {
            super(itemView);

            squareCardBackground = (LinearLayout) itemView.findViewById(R.id.cardview_profile_row);

            squareCardView = (CardView) itemView.findViewById(R.id.cardview_profile_square);
            squareName = (TextView) itemView.findViewById(R.id.cardview_profile_square_name);
            squareActivity = (TextView) itemView.findViewById(R.id.cardview_profile_square_last_activity);
            squareInitials = (TextView) itemView.findViewById(R.id.cardview_profile_square_initials);
            squareDescription = (TextView) itemView.findViewById(R.id.cardview_profile_description_text);

            squareFav = (ImageView) itemView.findViewById(R.id.cardview_profile_square_heart);

            middleSection = (RelativeLayout) itemView.findViewById(R.id.cardview_profile_middle_section);

            expandArrow  = (ImageView) itemView.findViewById(R.id.cardview_profile_expand_button);
            expandArrow.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            expandArrow.setVisibility(View.GONE);
                            lowerSectionExpanded.setVisibility(View.VISIBLE);
                        }
                    }
            );

            lowerSectionExpanded = (LinearLayout) itemView.findViewById(R.id.cardview_profile_lower_section_expanded);
            lowerSectionExpanded.setVisibility(View.GONE);

            lowerSectionFavs = (TextView) itemView.findViewById(R.id.lower_section_square_favourites);
            lowerSectionViews = (TextView) itemView.findViewById(R.id.lower_section_square_views);
            trashButton = (ImageButton) itemView.findViewById(R.id.lower_section_trash_button);
            editButton  = (ImageButton) itemView.findViewById(R.id.lower_section_edit_button);
            unexpandArrow = (ImageView) itemView.findViewById(R.id.lower_section_unexpand_button);
            unexpandArrow.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            expandArrow.setVisibility(View.VISIBLE);
                            lowerSectionExpanded.setVisibility(View.GONE);
                        }
                    }
            );

        }

    }
}
