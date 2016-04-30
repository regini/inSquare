package com.nsqre.insquare.Square;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DialogHandler;
import com.nsqre.insquare.Utilities.REST.VolleyManager;
import com.nsqre.insquare.Utilities.SquareType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by Umberto Sonnino on 29/03/2016.
 * TODO Documentare
 */
public class RecyclerProfileSquareAdapter extends RecyclerView.Adapter {

    private static final int PENDING_REMOVAL_TIMEOUT = 5000; // 5 secs
    private static final String TAG = "SquareAdapter";
    private static final String NOTIFICATION_MAP = "NOTIFICATION_MAP";

    private Context context;
    private ArrayList<Square> squaresArrayList;

    private ArrayList<Square> squaresPendingRemoval;
    private Handler handler = new Handler();
    private HashMap<String, Runnable> pendingRunnables = new HashMap<>();

    int i = 0;


    public RecyclerProfileSquareAdapter(Context c, ArrayList<Square> squares) {
        this.context = c;
        this.squaresArrayList = squares;
        squaresPendingRemoval = new ArrayList<>();
    }

    public void setDataList(ArrayList<Square> data)
    {
        if(!listEquals(data, this.squaresArrayList)) {
            this.squaresArrayList = data;
            sortData();
        }
    }

    public void sortData() {
        Collections.sort(this.squaresArrayList);
        Collections.reverse(this.squaresArrayList);
        notifyDataSetChanged();
    }

    private boolean listEquals(ArrayList<Square> first, ArrayList<Square> second) {
        if(first == null && second == null)
            return true;

        if(first == null && second != null
                || first != null && second == null
                || first.size() != second.size())
        {
            return false;
        }

        for (int i = 0; i < first.size(); i++) {
            if(!first.get(i).equals(second.get(i)))
            {
                return false;
            }
        }
        
        return true;
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

        final String squareName = listItem.getName();
        setupTopSection(castHolder, listItem);

        setupLeftSection(castHolder, listItem.getInitials());

        castHolder.lowerSectionViews.setText("" + listItem.getViews());
        castHolder.heartFavs.setText("" + listItem.getFavouredBy());
        castHolder.editButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new DialogHandler().handleProfileEdit(listItem, castHolder, ((BottomNavActivity)context).coordinatorLayout, TAG);
                    }
                }
        );

        ((LinearLayout)castHolder.lowerSectionViews.getParent()).setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        BottomNavActivity madre = (BottomNavActivity) context;

                        int[] array = new int[2];
                        v.getLocationOnScreen(array);

                        Snackbar.make(madre.coordinatorLayout, castHolder.lowerSectionViews.getText() + madre.getString(R.string.recycler_profile_adapter_visits), Snackbar.LENGTH_SHORT).show();

                        return true;
                    }
                }
        );

        if(listItem.isFacebookEvent)
        {
            setupFacebookSection(castHolder, SquareType.TYPE_EVENT, listItem);
        }else if(listItem.isFacebookPage)
        {
            setupFacebookSection(castHolder, SquareType.TYPE_SHOP, listItem);
        }

    }

    private void setupTopSection(final SquareViewHolder castHolder, final Square square) {
        String squareName = square.getName();
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(context.getString(R.string.square_last_message_incipit) + square.formatTime());
        String description = square.getDescription().trim();
        if(!description.isEmpty())
        {
            castHolder.squareDescription.setText(square.getDescription());
        }else
        {
            castHolder.squareDescription.setVisibility(View.GONE);
        }

        ((LinearLayout)castHolder.squareName.getParent().getParent()).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(MapFragment.SQUARE_TAG, square);
                        intent.putExtra(BottomNavActivity.INITIALS_TAG, castHolder.squareInitials.getText().toString());
                        int position = castHolder.getAdapterPosition() % (BottomNavActivity.backgroundColors.length);
                        intent.putExtra(BottomNavActivity.INITIALS_COLOR_TAG, BottomNavActivity.backgroundColors[position]);

                        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_MAP, Context.MODE_PRIVATE);
                        if (sharedPreferences.contains(square.getId())) {
                            sharedPreferences.edit().remove(square.getId()).apply();
                            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount", 0) - 1).apply();
                        }

                        BottomNavActivity madre = (BottomNavActivity) context;
                        Pair namePair = new Pair<>(v.findViewById(R.id.cardview_profile_square_name),
                                context.getString(R.string.transition_name_square_name));
                        Pair initialsPair = new Pair<>(v.findViewById(R.id.cardview_profile_left_section),
                                context.getString(R.string.transition_name_square_circle));
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                madre , namePair, initialsPair

                        );

                        ActivityCompat.startActivity(madre, intent, options.toBundle());
                    }
                }
        );
    }

    private void handleDelete(final Square listItem, final int position) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);

        builder.setTitle(context.getString(R.string.dialog_delete_title))
                .setMessage(context.getString(R.string.dialog_delete_message_p1) +
                        listItem.getName().toString().trim() + context.getString(R.string.dialog_delete_message_p2));
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String ownerId = InSquareProfile.getUserId();
                        final String squareId = listItem.getId();
                        VolleyManager.getInstance().deleteSquare(squareId, ownerId, new VolleyManager.VolleyResponseListener() {
                            @Override
                            public void responseGET(Object object) {
                                // Lasciare vuoto
                            }

                            @Override
                            public void responsePOST(Object object) {
                                // Lasciare vuoto
                            }

                            @Override
                            public void responsePATCH(Object object) {
                                // Lasciare vuoto
                            }

                            @Override
                            public void responseDELETE(Object object) {
                                boolean response = (boolean) object;
                                BottomNavActivity madre = (BottomNavActivity) context;

                                if (response) {
                                    Log.d(TAG, "responseDELETE: sono riuscito a eliminare correttamente!");
                                    Snackbar.make(madre.coordinatorLayout, R.string.dialog_delete_success, Snackbar.LENGTH_SHORT).show();
                                    squaresArrayList.remove(position);
                                    notifyItemRemoved(position);
                                } else {
                                    Log.d(TAG, "responseDELETE: c'e' stato un problema con la cancellazione");
                                    Snackbar.make(madre.coordinatorLayout, R.string.dialog_delete_fail, Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
        builder.setNegativeButton(context.getString(R.string.dialog_delete_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                squaresPendingRemoval.remove(listItem);
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    /*
        Cambia il colore del cerchio nella lista a seconda della posizione
        Utilizza DrawableCompat per retrocompatibilità
     */
    private void setupLeftSection(SquareViewHolder castHolder, String initials) {
        int position = castHolder.getAdapterPosition()%(BottomNavActivity.backgroundColors.length);

        ColorStateList circleColor = ContextCompat.getColorStateList(context, BottomNavActivity.backgroundColors[position]);

        final Drawable originalDrawable = castHolder.squareInitials.getBackground();
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTintList(wrappedDrawable, circleColor);
        castHolder.squareInitials.setBackground(wrappedDrawable);

        castHolder.squareInitials.setText(initials);
    }

    private void setupHeart(final SquareViewHolder castHolder, final Square listItem) {
        if(InSquareProfile.isFav(listItem.getId())){
            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
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
                            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
                        }
                    }
                }
        );
    }

    private void setupFacebookSection(final SquareViewHolder castHolder, SquareType typeShop, final Square listItem)
    {

        switch (typeShop)
        {
            case TYPE_EVENT:
                FacebookEventSquare fbEvent = (FacebookEventSquare) listItem;
                fillEventDetails(castHolder, fbEvent);
                break;
            case TYPE_SHOP:
                FacebookPageSquare fbPage = (FacebookPageSquare) listItem;
                fillPageDetails(castHolder, fbPage);
                break;
        }
    }

    private void fillPageDetails(final SquareViewHolder castHolder, FacebookPageSquare page) {

        castHolder.facebookSection.setVisibility(View.VISIBLE);
        potentialEmptySection(castHolder.facebookLikeCount, page.likeCount);
        potentialEmptySection(castHolder.facebookPhone, page.phoneNumber);
        potentialEmptySection(castHolder.facebookStreetName, page.street);
        potentialEmptySection(castHolder.facebookWebsite, page.website);
        potentialEmptySection(castHolder.facebookPrice, page.priceRange);

        if(page.hoursList.isEmpty())
        {
            ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.VISIBLE);
            // Puliamo il layout e prepariamolo ad essere riempito
            castHolder.facebookHoursList.removeAllViews();
            for(String s: page.hoursList)
            {
                TextView tv = (TextView) LayoutInflater.from(context).inflate(R.layout.simple_list_item, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                Resources r = context.getResources();
                int px = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8,
                        r.getDisplayMetrics()
                );
                params.setMargins(0, 0, px, px);
                tv.setLayoutParams(params);
                tv.setText(s);
                tv.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
                castHolder.facebookHoursList.addView(tv);
            }
        }
    }

    private void fillEventDetails(final SquareViewHolder castHolder, final FacebookEventSquare event) {

        Log.d(TAG, "fillEventDetails: " + event.toString());
        castHolder.facebookSection.setVisibility(View.VISIBLE);
        potentialEmptySection(castHolder.facebookLikeCount, "");
        potentialEmptySection(castHolder.facebookPhone, "");
        potentialEmptySection(castHolder.facebookStreetName, event.street);
        potentialEmptySection(castHolder.facebookWebsite, event.website);
        potentialEmptySection(castHolder.facebookPrice, "");

        if(event.time.isEmpty())
        {
            ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.GONE);
        }else {
            ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.VISIBLE);
            castHolder.facebookHoursList.removeAllViews();
            TextView tv = (TextView) LayoutInflater.from(context).inflate(R.layout.simple_list_item, null);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Resources r = context.getResources();
            int px = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8,
                    r.getDisplayMetrics()
            );
            params.setMargins(0, 0, px, px);
            tv.setLayoutParams(params);
            tv.setText(event.time);
            tv.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
            castHolder.facebookHoursList.addView(tv);
        }
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
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        } else {
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
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responseDELETE - non sono riuscito ad rimuovere il fav " + square.toString());
                        } else {
                            notifyDataSetChanged();
                            InSquareProfile.removeFav(square.getId());
                        }
                    }
                });
    }

    public void pendingRemoval(final int position)
    {
        Square toRemove = squaresArrayList.get(position);
//        Log.d(TAG, "pendingRemoval: " + toRemove.getName());
        if(!squaresPendingRemoval.contains(toRemove))
        {
            squaresPendingRemoval.add(toRemove);
            notifyItemChanged(position);
            handleDelete(toRemove, position);
            /*
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(position);
                }
            };

            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(toRemove.getId(), pendingRemovalRunnable);
            */
        }
    }

    public void remove(final int position)
    {
        Square item = squaresArrayList.get(position);
       /* if(squaresPendingRemoval.contains(item))
        {
            Log.d(TAG, "remove: I really want to remove " + item.getName());
            squaresPendingRemoval.remove(item);
        }*/
        if(squaresArrayList.contains(item))
        {
            squaresArrayList.remove(position);
            notifyItemChanged(position);
        }
    }

    public static class SquareViewHolder extends RecyclerView.ViewHolder {

        LinearLayout squareCardBackground;
        CardView squareCardView;
        TextView squareInitials;
        public TextView squareName;
        TextView squareActivity;
        public TextView squareDescription;
        ImageView squareFav;
        TextView heartFavs;

        LinearLayout lowerSectionExpanded;
        TextView lowerSectionViews;

        Button editButton;

        LinearLayout facebookSection;
        TextView facebookStreetName, facebookLikeCount, facebookWebsite, facebookPhone, facebookPrice;
        LinearLayout facebookHoursList;

        public SquareViewHolder(View itemView) {
            super(itemView);

            squareCardBackground = (LinearLayout) itemView.findViewById(R.id.cardview_profile_row);

            squareCardView = (CardView) itemView.findViewById(R.id.cardview_profile_square);
            squareName = (TextView) itemView.findViewById(R.id.cardview_profile_square_name);
            squareActivity = (TextView) itemView.findViewById(R.id.cardview_profile_square_last_activity);
            squareInitials = (TextView) itemView.findViewById(R.id.cardview_profile_square_initials);
            squareDescription = (TextView) itemView.findViewById(R.id.cardview_profile_description_text);

            squareFav = (ImageView) itemView.findViewById(R.id.cardview_profile_square_heart);

            lowerSectionExpanded = (LinearLayout) itemView.findViewById(R.id.cardview_profile_lower_section_expanded);

            heartFavs = (TextView) itemView.findViewById(R.id.carview_profile_heart_favorites);
            lowerSectionViews = (TextView) itemView.findViewById(R.id.lower_section_square_views);
            editButton  = (Button) itemView.findViewById(R.id.lower_section_edit_button);

            // Download da Facebook
            facebookSection = (LinearLayout) itemView.findViewById(R.id.cardview_profile_facebook_section);
            facebookStreetName = (TextView) itemView.findViewById(R.id.cardview_profile_facebook_street_name);
            facebookLikeCount = (TextView) itemView.findViewById(R.id.cardview_profile_facebook_like_number);
            facebookWebsite = (TextView) itemView.findViewById(R.id.cardview_profile_facebook_website);
            facebookPhone = (TextView) itemView.findViewById(R.id.cardview_profile_facebook_phone);
            facebookPrice = (TextView) itemView.findViewById(R.id.cardview_profile_facebook_price_range);

            facebookHoursList = (LinearLayout) itemView.findViewById(R.id.cardview_profile_facebook_list_hours);

        }

    }
}
