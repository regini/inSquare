package com.nsqre.insquare.Square;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;

/**
 * Created by Umberto Sonnino on 29/03/2016.
 */
public class RecyclerSquareAdapter extends RecyclerView.Adapter {

    private static final String TAG = "SquareAdapter";
    public static final String NOTIFICATION_MAP = "NOTIFICATION_MAP";
    private Context context;
    private ArrayList<Square> squaresArrayList;
    int i = 0;

    int[] backgroundColors = new int[]{
            R.color.md_amber_A100,
            R.color.md_orange_A100,
            R.color.colorAccentLightest,
            R.color.md_purple_A100,
            R.color.md_deep_purple_A200,
            R.color.md_blue_100,
            R.color.md_teal_A400
    };

    public RecyclerSquareAdapter(Context c, ArrayList<Square> squares) {
        this.context = c;
        this.squaresArrayList = squares;
    }

    @Override
    public SquareViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.square_card, parent, false);
        SquareViewHolder squareViewHolder = new SquareViewHolder(v);
        return squareViewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final SquareViewHolder castHolder = (SquareViewHolder) holder;

        final Square listItem = this.squaresArrayList.get(position);

        setupHeart(castHolder, listItem);

        setupNotifications(castHolder, listItem);

        String squareName = listItem.getName();
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(listItem.formatTime());

        setupLeftSection(castHolder, squareName);

        castHolder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(MapFragment.SQUARE_TAG, listItem);
                        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_MAP, Context.MODE_PRIVATE);
                        if (sharedPreferences.contains(listItem.getId())) {
                            sharedPreferences.edit().remove(listItem.getId()).apply();
                            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount", 0) - 1).apply();
                        }
                        context.startActivity(intent);
                    }
                }
        );
    }

    private void setupLeftSection(SquareViewHolder castHolder, String squareName) {
        int position = castHolder.getAdapterPosition()%(backgroundColors.length);

//        castHolder.verticalColoredBar.setBackgroundColor(
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
            castHolder.squareFav.setImageResource(R.drawable.heart_black);
        }

        castHolder.squareFav.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(InSquareProfile.isFav(listItem.getId()))
                        {
                            favouriteSquare(Request.Method.DELETE, listItem);
                            castHolder.squareFav.setImageResource(R.drawable.heart_border_black);
                        }else
                        {
                            favouriteSquare(Request.Method.POST, listItem);
                            castHolder.squareFav.setImageResource(R.drawable.heart_black);
                        }
                    }
                }
        );
    }

    private void setupNotifications(SquareViewHolder castHolder, Square listItem) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_MAP, Context.MODE_PRIVATE);

        int squaresNewMessages = sharedPreferences.getInt(listItem.getId(), 0);
        if (squaresNewMessages == 0) {
            castHolder.squareNotifications.setVisibility(View.INVISIBLE);
        } else {
//            castHolder.squareNotifications.setText(String.valueOf(squaresNewMessages));
            castHolder.squareNotifications.setVisibility(View.VISIBLE);
        }
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
//        View verticalColoredBar;
        TextView squareInitials;
        TextView squareName;
        TextView squareActivity;
        TextView squareNotifications;
        ImageView squareFav;

        public SquareViewHolder(View itemView) {
            super(itemView);

            squareCardBackground = (LinearLayout) itemView.findViewById(R.id.cardview_row);

            squareCardView = (CardView) itemView.findViewById(R.id.cardview_square);
            squareName = (TextView) itemView.findViewById(R.id.cardview_square_name);
            squareActivity = (TextView) itemView.findViewById(R.id.cardview_square_last_activity);
            squareNotifications = (TextView) itemView.findViewById(R.id.cardview_square_notification_counter);
            squareInitials = (TextView) itemView.findViewById(R.id.cardview_square_initials);
//            verticalColoredBar = itemView.findViewById(R.id.cardview_vertical_colored_bar);

            squareFav = (ImageView) itemView.findViewById(R.id.cardview_square_heart);

        }

    }
}
