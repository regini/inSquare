package com.nsqre.insquare.Square;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.design.widget.TextInputLayout;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final String NOTIFICATION_MAP = "NOTIFICATION_MAP";
    private static final String INDENTATION = "\t\t\t\t";

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
        String description = listItem.getDescription().trim();
        if(description.length() > 0)
        {
            castHolder.squareDescription.setText(INDENTATION + listItem.getDescription());
        }else
        {
            castHolder.middleSection.setVisibility(View.GONE);
        }
        setupLeftSection(castHolder, squareName);

        castHolder.lowerSectionViews.setText("" + listItem.getViews());
        castHolder.lowerSectionFavs.setText("" + listItem.getFavouredBy());
        castHolder.editButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleEdit(listItem, castHolder);
                    }
                }
        );
        castHolder.trashButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleDelete(listItem, castHolder);
                    }
                }
        );
    }

    private void handleDelete(final Square listItem, SquareViewHolder squareViewHolder) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);

        builder.setTitle("Attenzione!")
                .setMessage("Tutti i messaggi associati a " + listItem.getName().toString().trim() + " andranno perduti.");
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener()
                {
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
                                if(response) {
                                    Log.d(TAG, "responseDELETE: sono riuscito a eliminare correttamente!");
                                    Toast.makeText(context, "Cancellazione avvenuta con successo!", Toast.LENGTH_SHORT).show();
                                    squaresArrayList.remove(listItem);
                                    notifyDataSetChanged();
                                }else {
                                    Log.d(TAG, "responseDELETE: c'e' stato un problema con la cancellazione");
                                    Toast.makeText(context, "C'e' stato un problema con la cancellazione!", Toast.LENGTH_SHORT).show();
                                }
                            }
                });
            }
        });
        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }


    private void handleEdit(final Square element, final SquareViewHolder squareViewHolder) {
        String oldDescription = element.getDescription().trim();
        String oldName = element.getName().trim();

        final Dialog editDialog = new Dialog(context);
        editDialog.setContentView(R.layout.dialog_edit_square);
        editDialog.setCancelable(true);
        editDialog.show();

        final EditText nameEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_name_text);

        ((TextInputLayout)nameEditText.getParent()).setHint("Modifica il nome");
        nameEditText.setText(oldName);

        final EditText descriptionEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_description_text);
        if(!oldDescription.isEmpty())
        {
            ((TextInputLayout)descriptionEditText.getParent()).setHint("Modifica la descrizione");
            descriptionEditText.setText(oldDescription);
        }

        final Button okButton = (Button) editDialog.findViewById(R.id.dialog_edit_ok_button);
        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String newDescription = descriptionEditText.getText().toString().trim();
                        final String newName = nameEditText.getText().toString().trim();

                        if (newName.isEmpty()) {
                            Toast.makeText(context, "Il nome non può essere vuoto!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.d(TAG, "onClick: stai tentando di modificare la descrizione:\n" + newDescription);
                        Log.d(TAG, "onClick: stai tentando di modificare il nome:\n" + newName);
                        VolleyManager.getInstance().patchDescription(newName, newDescription, element.getId(), InSquareProfile.getUserId(),
                                new VolleyManager.VolleyResponseListener() {
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
                                        boolean response = (boolean) object;
                                        if (response) {
                                            // Tutto OK!
                                            Log.d(TAG, "responsePATCH: sono riuscito a patchare correttamente!");
                                            squareViewHolder.squareName.setText(newName);
                                            squareViewHolder.squareDescription.setText(INDENTATION + newDescription);
                                            element.setName(newName);
                                            element.setDescription(newDescription);

                                            if (newDescription.isEmpty()) {
                                                squareViewHolder.middleSection.setVisibility(View.GONE);
                                            } else {
                                                squareViewHolder.middleSection.setVisibility(View.VISIBLE);
                                            }

                                            Toast.makeText(context, "Modificata con successo!", Toast.LENGTH_SHORT).show();
                                            editDialog.dismiss();
                                        } else {
                                            // Errore
                                            Toast.makeText(context, "Ho avuto un problema con la connessione. Riprova..?", Toast.LENGTH_SHORT).show();
                                            editDialog.dismiss();
                                        }
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Lasciare vuoto
                                    }
                                });
                    }
                }
        );

    }

    /*
        Cambia il colore del cerchio nella lista a seconda della posizione
        Utilizza DrawableCompat per retrocompatibilità
     */
    private void setupLeftSection(SquareViewHolder castHolder, String squareName) {
        int position = castHolder.getAdapterPosition()%(backgroundColors.length);

        ColorStateList circleColor = ContextCompat.getColorStateList(context, backgroundColors[position]);

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

    public static class SquareViewHolder extends RecyclerView.ViewHolder {

        LinearLayout squareCardBackground;
        CardView squareCardView;
        TextView squareInitials;
        TextView squareName;
        TextView squareActivity;
        TextView squareDescription;
        ImageView squareFav;

        RelativeLayout middleSection;

        LinearLayout lowerSectionExpanded;
        TextView lowerSectionFavs;
        TextView lowerSectionViews;

        ImageButton trashButton;
        ImageButton editButton;

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

            lowerSectionFavs = (TextView) itemView.findViewById(R.id.lower_section_square_favourites);
            lowerSectionViews = (TextView) itemView.findViewById(R.id.lower_section_square_views);
            trashButton = (ImageButton) itemView.findViewById(R.id.lower_section_trash_button);
            editButton  = (ImageButton) itemView.findViewById(R.id.lower_section_edit_button);

        }

    }
}
