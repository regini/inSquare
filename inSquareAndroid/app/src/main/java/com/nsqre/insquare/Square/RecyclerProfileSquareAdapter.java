package com.nsqre.insquare.Square;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DialogHandler;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;
import java.util.HashMap;

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
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(listItem.formatTime());
        // Per sottolineare l'inizio
        String description = listItem.getDescription().trim();
        if(description.length() > 0)
        {
            castHolder.squareDescription.setText(listItem.getDescription());
        }else
        {
            castHolder.middleSection.setVisibility(View.GONE);
        }
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
//                        Toast message = Toast.makeText(context, castHolder.lowerSectionViews.getText() + " visite", Toast.LENGTH_SHORT);
//                        int xOffset = (int) (v.getX() + 1.5 * v.getWidth());
//                        int yOffset = (int) (array[1] - 1.5 * v.getHeight());
//                        message.setGravity(Gravity.TOP | Gravity.LEFT, xOffset, yOffset);
//                        message.show();


                        return true;
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

        RelativeLayout middleSection;

        LinearLayout lowerSectionExpanded;
        TextView lowerSectionViews;

        Button editButton;

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

            lowerSectionExpanded = (LinearLayout) itemView.findViewById(R.id.cardview_profile_lower_section_expanded);

            heartFavs = (TextView) itemView.findViewById(R.id.carview_profile_heart_favorites);
            lowerSectionViews = (TextView) itemView.findViewById(R.id.lower_section_square_views);
            editButton  = (Button) itemView.findViewById(R.id.lower_section_edit_button);

        }

    }
}
