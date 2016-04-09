package com.nsqre.insquare.Fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class SettingsFragment extends Fragment implements
        InSquareProfile.InSquareProfileListener
{

    private static SettingsFragment instance;

    private static final String TAG = "SettingsFragment";

    private CircleImageView userAvatar;
    private TextView username;

    private TextView facebookConnectButton;
    private TextView googleConnectButton;

    private TextView feedbackSendButton;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        if(instance == null){
            instance = new SettingsFragment();
        }
        return instance;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    /**
     * Initialized the view of this fragment setting the lists of favourite and owned squares
     * and the profile image(downloading it if not saved in the local storage)
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The view created
     * @see DownloadImageTask
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        setupProfile(v);

        setupLoginButtons(v);

        setupFeedbackButton(v);

        return v;
    }

    private void setupFeedbackButton(View layout)
    {
        feedbackSendButton = (TextView) layout.findViewById(R.id.settings_send_feedback);
        feedbackSendButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Inizializza un Dialog per l'invio del feedback
                        final Dialog d = new Dialog(getContext());
                        d.setContentView(R.layout.dialog_feedback);
                        d.setTitle("Feedback");
                        d.setCancelable(true);
                        d.show();

                        final EditText feedbackEditText = (EditText) d.findViewById(R.id.dialog_feedbacktext);

                        final String feedback = feedbackEditText.getText().toString().trim();
                        final String activity = this.getClass().getSimpleName();

                        Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                        confirm.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                VolleyManager.getInstance().postFeedback(
                                        feedback,
                                        InSquareProfile.getUserId(),
                                        activity,
                                        new VolleyManager.VolleyResponseListener() {
                                            @Override
                                            public void responseGET(Object object) {
                                                // Vuoto - POST Request
                                            }

                                            @Override
                                            public void responsePOST(Object object) {
                                                if (object == null) {
                                                    Toast.makeText(getContext(), "Non sono riuscito ad inviare il feedback", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(getContext(), "Feedback inviato con successo!", Toast.LENGTH_SHORT).show();
                                                    d.dismiss();
                                                }
                                            }

                                            @Override
                                            public void responsePATCH(Object object) {
                                                // Vuoto - POST Request
                                            }

                                            @Override
                                            public void responseDELETE(Object object) {
                                                // Vuoto - POST Request
                                            }
                                        }
                                );
                            }
                        });
                    }
                }
        );
    }

    private void setupProfile(View layout) {
        userAvatar = (CircleImageView) layout.findViewById(R.id.settings_top_user_avatar);

        Bitmap bitmap = InSquareProfile.loadProfileImageFromStorage(getContext());
        if (bitmap == null) {
            if (!InSquareProfile.getPictureUrl().equals(""))
                new DownloadImageTask(userAvatar, getContext()).execute(InSquareProfile.getPictureUrl());
        } else {
            userAvatar.setImageBitmap(bitmap);
        }

        username = (TextView) layout.findViewById(R.id.settings_top_username);
        username.setText(InSquareProfile.getUsername());
    }

    private void setupLoginButtons(View layout)
    {
        View.OnClickListener WIP = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomNavActivity madre = (BottomNavActivity) getContext();
                Snackbar.make(madre.coordinatorLayout, "Ci stiamo lavorando!", Snackbar.LENGTH_SHORT).show();
            }
        };

        facebookConnectButton = (TextView) layout.findViewById(R.id.settings_facebook_connect);
        facebookConnectButton.setOnClickListener(WIP);

        if(InSquareProfile.isFacebookConnected())
        {
            facebookConnectButton.setText("Disconnetti da " + InSquareProfile.facebookName);

            /*facebookConnectButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context c = getContext();
                            Toast.makeText(getContext(), "Ci stiamo lavorando!", Toast.LENGTH_SHORT).show();
                            LoginManager.getInstance().logOut();
                            InSquareProfile.clearFacebookCredentials(c);
                            facebookConnectButton.setText(R.string.settings_connect_facebook);

                            if(!InSquareProfile.isGoogleConnected())
                            {
                                Intent intent = new Intent(c, LoginActivity.class);
                                startActivity(intent);
                                InSquareProfile.clearProfileCredentials(c);
                            }
                        }
                    }
            );*/

        }

        googleConnectButton = (TextView) layout.findViewById(R.id.settings_google_connect);
        if(InSquareProfile.isGoogleConnected())
        {
            googleConnectButton.setText("Disconnetti da " + InSquareProfile.googleName);
        }
        googleConnectButton.setOnClickListener(WIP);
    }

    /**
     * TODO ??
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        InSquareProfile.removeListener(this);
    }

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged!");
    }

    @Override
    public void onFavChanged() {
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged!");
    }
}
