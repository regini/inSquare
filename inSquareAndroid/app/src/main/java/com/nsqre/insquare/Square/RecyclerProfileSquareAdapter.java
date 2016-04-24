package com.nsqre.insquare.Square;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.widget.Toast;

import com.android.volley.Request;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DialogHandler;
import com.nsqre.insquare.Utilities.REST.VolleyManager;
import com.nsqre.insquare.Utilities.SquareType;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Umberto Sonnino on 29/03/2016.
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

        setupLeftSection(castHolder, squareName);

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

                        Snackbar.make(madre.coordinatorLayout, castHolder.lowerSectionViews.getText() + " visite", Snackbar.LENGTH_SHORT).show();

                        return true;
                    }
                }
        );

        if(!listItem.getFacebookEventId().isEmpty())
        {
            setupFacebookSection(castHolder, SquareType.TYPE_EVENT, listItem);
        }else if(!listItem.getFacebookPageId().isEmpty())
        {
            setupFacebookSection(castHolder, SquareType.TYPE_SHOP, listItem);
        }

    }

    private void setupTopSection(final SquareViewHolder castHolder, final Square square) {
        String squareName = square.getName();
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(square.formatTime());
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

        builder.setTitle("Attenzione!")
                .setMessage("Tutti i messaggi associati a " + listItem.getName().toString().trim() + " andranno perduti.");
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
                                    Snackbar.make(madre.coordinatorLayout, "Cancellazione avvenuta con successo!", Snackbar.LENGTH_SHORT).show();
                                    squaresArrayList.remove(position);
                                    notifyItemRemoved(position);
                                } else {
                                    Log.d(TAG, "responseDELETE: c'e' stato un problema con la cancellazione");
                                    Snackbar.make(madre.coordinatorLayout, "C'e' stato un problema con la cancellazione!", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
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
    private void setupLeftSection(SquareViewHolder castHolder, String squareName) {
        int position = castHolder.getAdapterPosition()%(BottomNavActivity.backgroundColors.length);

        ColorStateList circleColor = ContextCompat.getColorStateList(context, BottomNavActivity.backgroundColors[position]);

        final Drawable originalDrawable = castHolder.squareInitials.getBackground();
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTintList(wrappedDrawable, circleColor);
        castHolder.squareInitials.setBackground(wrappedDrawable);

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
                            castHolder.squareFav.setImageResource(R.drawable.heart_border_black);
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

    private void setupFacebookSection(final SquareViewHolder castHolder, SquareType typeShop, final Square listItem)
    {

        switch (typeShop)
        {
            case TYPE_EVENT:
                Log.d(TAG, "setupFacebookSection: EVENTO");
                downloadAndFillEventDetails(castHolder, listItem);
                break;
            case TYPE_SHOP:
                Log.d(TAG, "setupFacebookSection: PAGINA!");
                downloadAndFillePageDetails(castHolder, listItem);
                break;
        }
    }

    private void downloadAndFillePageDetails(final SquareViewHolder castHolder, Square listItem) {
        Bundle requestParams = new Bundle();
        requestParams.putString("fields", "fan_count,price_range,hours,phone,location,website");

        Log.d(TAG, "downloadAndFillePageDetails: " + listItem.getFacebookPageId());

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/v2.6/" + listItem.getFacebookPageId(),
                requestParams,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject object = response.getJSONObject();
                        Log.d(TAG, "onCompleted: page details ====\n" + response.toString());

                        if(object == null)
                        {
                            Toast.makeText(context, "Facebook ha fallito il download dei dati", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {

                            String likes,price, phone, website;
                            String street;
                            likes = price = phone = website = street = "";
                            JSONObject location, hours;

                            if(object.has("fan_count")) {
                                likes = object.getString("fan_count").trim();
                            }
                            if(object.has("price_range")) {
                                price = object.getString("price_range").trim();
                            }
                            if(object.has("phone")) {
                                phone = object.getString("phone").trim();
                            }
                            if(object.has("website")) {
                                website = object.getString("website").trim();
                            }

                            // Location
                            if(object.has("location")) {
                                location = object.getJSONObject("location");
                                street = location.getString("street").trim();
                            }

                            List<String> hoursList = new ArrayList<>();

                            // Hours
                            if(object.has("hours")) {
                                hours = object.getJSONObject("hours");
                                Iterator<String> keys = hours.keys();

                                while (keys.hasNext()) {
                                    String listValue = "";

                                    String keyDay = keys.next();
                                    String valueOpen = hours.getString(keyDay);

                                    String keyDayOpen = keyDay.split("_")[0];
                                    String capitalized = keyDayOpen.substring(0, 1).toUpperCase() + keyDayOpen.substring(1);

                                    listValue += capitalized + ": " + valueOpen;

                                    if (keys.hasNext()) {
                                        // Ha anche una controparte per la chiusura
                                        String valueClose = hours.getString(keys.next());
                                        listValue += (" - " + valueClose);
                                    }
                                    hoursList.add(listValue.trim());
                                }
                            }

                            castHolder.facebookSection.setVisibility(View.VISIBLE);
                            potentialEmptySection(castHolder.facebookLikeCount, likes);
                            potentialEmptySection(castHolder.facebookPhone, phone);
                            potentialEmptySection(castHolder.facebookStreetName, street);
                            potentialEmptySection(castHolder.facebookWebsite, website);
                            potentialEmptySection(castHolder.facebookPrice, price);

                            if(hoursList.isEmpty())
                            {
                                ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.GONE);
                            }else {
                                ((LinearLayout)castHolder.facebookHoursList.getParent()).setVisibility(View.VISIBLE);
                                // Puliamo il layout e prepariamolo ad essere riempito
                                castHolder.facebookHoursList.removeAllViews();
                                for(String s: hoursList)
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
                                    castHolder.facebookHoursList.addView(tv);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void downloadAndFillEventDetails(final SquareViewHolder castHolder, final Square listItem) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + listItem.getFacebookEventId(),
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        Log.d(TAG, "onCompleted: event details ====\n" + response.toString());

                        JSONObject object = response.getJSONObject();

                        if(object == null)
                        {
                            Toast.makeText(context, "Facebook ha fallito il download dei dati", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {

                            String street;
                            street = "";
                            String startTime, endTime, finalTime;
                            // TODO String picture;
                            JSONObject location;

                            Date date;
                            SimpleDateFormat incomingFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                            SimpleDateFormat outgoingStartFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");
                            SimpleDateFormat outgoingEndFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");

                            Date endDate;
                            SimpleDateFormat serverEndFormat = new SimpleDateFormat("yyyy-MM-dd");

                            {
                                String jsonTime= object.getString("start_time");
                                // Il parsing mi restituisce un oggetto Data
                                date = incomingFormat.parse(jsonTime);
                                // Il format ritorna una stringa con il formato specificato nel costruttore
                                startTime = outgoingStartFormat.format(date);
                                Log.d(TAG, "onCompleted: " + startTime);
                            }

                            if(object.has("end_time"))
                            {
                                String jsonTime = object.getString("end_time");
                                date = incomingFormat.parse(jsonTime);
                                endTime = outgoingEndFormat.format(date);

                                endDate = serverEndFormat.parse(jsonTime);

                                String startIncipit = startTime.substring(0, startTime.indexOf(" at "));
                                String endIncipit = endTime.substring(0,endTime.indexOf(" at "));

                                if(startIncipit.equals(endIncipit))
                                {
                                    int stringLength = endTime.length();
                                    endTime = endTime.substring(stringLength-4, stringLength);
                                }

                                finalTime = startTime + " to " + endTime;
                            }else
                            {
                                endTime = "";
                                finalTime = startTime;
                            }

                            if(object.has("place"))
                            {
                                JSONObject place = object.getJSONObject("place");
                                if(place.has("location")){
                                    location = place.getJSONObject("location");
                                    street = location.getString("street").trim();
                                }
                            }

                            String website = "www.facebook.com/events/" + listItem.getFacebookEventId();

                            castHolder.facebookSection.setVisibility(View.VISIBLE);
                            potentialEmptySection(castHolder.facebookLikeCount, "");
                            potentialEmptySection(castHolder.facebookPhone, "");
                            potentialEmptySection(castHolder.facebookStreetName, street);
                            potentialEmptySection(castHolder.facebookWebsite, website);
                            potentialEmptySection(castHolder.facebookPrice, "");

                            if(finalTime.isEmpty())
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
                                tv.setText(finalTime);
                                castHolder.facebookHoursList.addView(tv);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }).executeAsync();
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
