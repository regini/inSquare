package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 8/4/16  */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerProfileSquareAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.Date;

/**
 * TODO Documentare
 */
public class DialogHandler {

    public static final String SQUARE_TYPE = "SQUARE_TYPE";
    public static final String MAP_SCREENSHOT = "MAP_SCREENSHOT";

    public static void handleMuteRequest(
            final Context where,
            final View snackbarContainer,
            final String TAG,
            final String squareId
    )
    {
        final CharSequence options[] = new CharSequence[]{
                where.getString(R.string.dialog_mute_off), //0
                where.getString(R.string.dialog_mute_1h), //1
                where.getString(R.string.dialog_mute_8h), //2
                where.getString(R.string.dialog_mute_2d),//3
                where.getString(R.string.dialog_mute_on)//4
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(where);
        builder.setTitle(R.string.dialog_mute_title);
        builder.setItems(options,
                new DialogInterface.OnClickListener() {

                    boolean valid = true;

                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        Snackbar showResult = Snackbar.make(snackbarContainer, "", Snackbar.LENGTH_LONG);
                        Log.d(TAG, "onCheckedChanged: " + which + " " + options[which]);
                        if (which == 0) {
                            showResult.setText(R.string.dialog_mute_result_off);
                        } else {
                            showResult.setText(R.string.dialog_mute_result_on);
                        }

                        showResult.setAction(R.string.dialog_mute_cancel,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        valid = false;
                                    }
                                });
                        showResult.setCallback(
                                new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);
                                        if(valid) {
                                            SharedPreferences sharedPreferences = where.getSharedPreferences("NOTIFICATION_MUTE_MAP", where.MODE_PRIVATE);

                                            if(sharedPreferences.contains(squareId)){
                                                sharedPreferences.edit().remove(squareId).apply();
                                            }

                                            long expireTime = 0;

                                            long minutes = 1000 * 60;
                                            long hours = minutes * 60;
                                            long days = hours * 24;

                                            long years = 365 * days;

                                            switch (which)
                                            {
                                                case 0:
                                                    expireTime = new Date().getTime() - hours;
                                                    break;
                                                case 1:
                                                    expireTime = new Date().getTime() + hours;
                                                    break;
                                                case 2:
                                                    expireTime = new Date().getTime() + 8 * hours;
                                                    break;
                                                case 3:
                                                    expireTime = new Date().getTime() + 2 * days;
                                                    break;
                                                case 4:
                                                    expireTime = new Date().getTime() + 10 * years;
                                                    break;
                                            }
                                            sharedPreferences.edit().putString(squareId, Long.toString(expireTime)).apply();
/*
                                            VolleyManager.getInstance().muteSquare(
                                                    InSquareProfile.getUserId(),
                                                    squareId,
                                                    which,
                                                    new VolleyManager.VolleyResponseListener() {
                                                        @Override
                                                        public void responseGET(Object object) {
                                                            // Rimane vuota
                                                        }

                                                        @Override
                                                        public void responsePOST(Object object) {
                                                            // Rimane vuota
                                                        }

                                                        @Override
                                                        public void responsePATCH(Object object) {
                                                            boolean muteResponse = (boolean) object;
                                                            if(muteResponse)
                                                            {

                                                            }else
                                                            {

                                                            }
                                                        }

                                                        @Override
                                                        public void responseDELETE(Object object) {
                                                            // Rimane vuota
                                                        }
                                                    }
                                            );*/
                                        }
                                        else {
                                            Log.d(TAG, "onDismissed: well, I've been undone!");
                                        }
                                    }
                                }
                        );

                        showResult.show();
                    }
                });

        builder.show();
    }

    public static void handleProfileEdit(
            final Square element,
            final RecyclerProfileSquareAdapter.SquareViewHolder squareViewHolder,
            final View parentLayout,
            final String TAG
    ) {
        String oldDescription = element.getDescription().trim();
        String oldName = element.getName().trim();

        final Context dialogContext = parentLayout.getContext();

        final Dialog editDialog = new Dialog(dialogContext);
        editDialog.setContentView(R.layout.dialog_edit_square);
        editDialog.setCancelable(true);
        editDialog.show();

        final EditText nameEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_name_text);

        ((TextInputLayout)nameEditText.getParent()).setHint(dialogContext.getString(R.string.dialog_edit_name_hint));
        nameEditText.setText(oldName);

        final EditText descriptionEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_description_text);
        if(!oldDescription.isEmpty())
        {
            ((TextInputLayout)descriptionEditText.getParent()).setHint(dialogContext.getString(R.string.dialog_edit_descr_hint));
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
//                            Toast.makeText(where, "Il nome non può essere vuoto!", Toast.LENGTH_SHORT).show();
//                            Snackbar.make(parentLayout, "Il nome non può essere vuoto", Snackbar.LENGTH_SHORT).show();
                            ((TextInputLayout)nameEditText.getParent()).setErrorEnabled(true);
                            ((TextInputLayout)nameEditText.getParent()).setError(dialogContext.getString(R.string.dialog_edit_name_empty_error));
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
                                            squareViewHolder.squareDescription.setText(newDescription);
                                            element.setName(newName);
                                            element.setDescription(newDescription);

                                            if (newDescription.isEmpty()) {
                                                squareViewHolder.squareDescription.setVisibility(View.GONE);
                                            } else {
                                                squareViewHolder.squareDescription.setVisibility(View.VISIBLE);
                                            }
//                                            Toast.makeText(where, "Modificata con successo!", Toast.LENGTH_SHORT).show();
                                            Snackbar.make(parentLayout, R.string.dialog_edit_success, Snackbar.LENGTH_SHORT).show();
                                            editDialog.dismiss();
                                        } else {
                                            // Errore
//                                            Toast.makeText(where, "Ho avuto un problema con la connessione. Riprova..?", Toast.LENGTH_SHORT).show();
                                            Snackbar.make(parentLayout, R.string.dialog_edit_fail, Snackbar.LENGTH_SHORT).show();
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



}
