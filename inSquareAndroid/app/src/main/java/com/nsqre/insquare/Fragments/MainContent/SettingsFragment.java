package com.nsqre.insquare.Fragments.MainContent;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * This is the fragment that show the user's Profile. In it you can find information about the user:
 * his name, his photo and the lists of squares created and favoured
 */
public class SettingsFragment extends Fragment implements
        InSquareProfile.InSquareProfileListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks
{

    private static SettingsFragment instance;

    private static final String TAG = "SettingsFragment";

    private CircleImageView userAvatar;
    private TextView username;

    private TextView facebookConnectButton;
    private TextView googleConnectButton;
    private TextView inSquareDisconnectButton;

    private TextView feedbackSendButton;

    private InSquareProfile profile;

    private String gAccessToken;
    private GoogleApiClient gApiClient;
    private GoogleSignInOptions gSo;
    public static final int RC_SIGN_IN = 9001;

    private CallbackManager fbCallbackManager;
    private String fbAccessToken;

    private TextView tutorialButton;

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

        gSo = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();

        if(gApiClient == null) {
            gApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                    .enableAutoManage(getActivity(), this)
                    .addConnectionCallbacks(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gSo)
                    .build();
        }

        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        fbCallbackManager = CallbackManager.Factory.create();

        profile = InSquareProfile.getInstance(getActivity().getApplicationContext());
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

        setupTutorialButton(v);

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
        inSquareDisconnectButton = (TextView) layout.findViewById(R.id.settings_insquare_disconnect);
        inSquareDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context c = getContext();
                if(InSquareProfile.isFacebookConnected()) {
                    LoginManager.getInstance().logOut();
                }
                InSquareProfile.clearProfileCredentials(c);
            }
        });

        facebookConnectButton = (TextView) layout.findViewById(R.id.settings_facebook_connect);

        if(InSquareProfile.isFacebookConnected())
        {
            facebookConnectButton.setText("Disconnetti da " + InSquareProfile.facebookName);

            facebookConnectButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context c = getContext();
                            LoginManager.getInstance().logOut();
                            InSquareProfile.clearFacebookCredentials(c);
                            facebookConnectButton.setText(R.string.settings_connect_facebook);

                            if(!InSquareProfile.isGoogleConnected())
                            {
                                InSquareProfile.clearProfileCredentials(c);
                            }
                        }
                    }
            );

        } else {
            facebookConnectButton.setText(R.string.settings_connect_facebook);

            LoginManager.getInstance().registerCallback(fbCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "Success Login");
                            requestFacebookData();  //fa la post
                            //fbLoginButton.setText(R.string.fb_logout_string);
                        }

                        @Override
                        public void onCancel() {
                            Log.d(TAG, "Facebook Login canceled");
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            Log.d(TAG, "onError:\n" + exception.toString());
                            CharSequence text = getString(R.string.connFail);
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(getActivity().getApplicationContext(), text, duration);
                            toast.show();
                        }
                    });

            facebookConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LoginManager.getInstance().logInWithReadPermissions(SettingsFragment.this, Arrays.asList("public_profile", "email", "user_friends"));
                }
            });
        }

        googleConnectButton = (TextView) layout.findViewById(R.id.settings_google_connect);
        if(InSquareProfile.isGoogleConnected())
        {
            googleConnectButton.setText("Disconnetti da " + InSquareProfile.googleName);

            googleConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context c = getContext();
                    InSquareProfile.clearGoogleCredentials(c);
                    if(!InSquareProfile.isFacebookConnected()) {
                        InSquareProfile.clearProfileCredentials(c);
                    }
                }
            });
        } else {
            googleConnectButton.setText("Connetti con Google+");

            googleConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(gApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }
            });
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            googleSignInResult(result); //dalla post parte l'app
        } else {
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void googleSignInResult(GoogleSignInResult result) {

        Log.d("Google" + TAG, "Success? " + result.isSuccess() + "\nStatus Code: " + result.getStatus().getStatusCode());

        if (result.isSuccess())
        {
            GoogleSignInAccount acct = result.getSignInAccount();
            gAccessToken = acct.getIdToken();

            Log.d(TAG, "Login was a success: " + acct.getDisplayName() + ": " + acct.getEmail());
            Log.d(TAG, "Token is: " + acct.getIdToken());

            googlePatchRequest();
        } else { //connessione fallita
            CharSequence text = getString(R.string.connFail);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), text, duration);
            toast.show();
        }
    }

    private void requestFacebookData()
    {
        fbAccessToken = AccessToken.getCurrentAccessToken().getToken();
        facebookPatchRequest();
    }

    private void googlePatchRequest() {
        final String serviceName = "google";

        VolleyManager.getInstance().patchLoginToken(serviceName, gAccessToken,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto - POST Request
                    }

                    @Override
                    public void responsePOST(Object object) {

                    }

                    @Override
                    public void responsePATCH(Object object) {
                        if (object == null) {
                            Toast.makeText(getActivity(), "Qualcosa non ha funzionato con il token di " + serviceName, Toast.LENGTH_SHORT).show();
                        } else {
                            String serverResponse = (String) object;
                            try {
                                JSONObject jsonObject = new JSONObject(serverResponse);
                                profile.googleEmail = jsonObject.getString("googleEmail");
                                profile.googleToken = jsonObject.getString("googleToken");
                                profile.googleName = jsonObject.getString("googleName");
                                profile.googleId = jsonObject.getString("googleId");
                                profile.save(getActivity().getApplicationContext());
                                googleConnectButton.setText("Disconnetti da " + InSquareProfile.googleName);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto - POST Request
                    }
                });
    }

    private void facebookPatchRequest() {
        final String serviceName = "facebook";

        VolleyManager.getInstance().patchLoginToken(serviceName, fbAccessToken,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto - POST Request
                    }

                    @Override
                    public void responsePOST(Object object) {

                    }

                    @Override
                    public void responsePATCH(Object object) {
                        if (object == null) {
                            Toast.makeText(getActivity(), "Qualcosa non ha funzionato con il token di " + serviceName, Toast.LENGTH_SHORT).show();
                        } else {
                            String serverResponse = (String) object;
                            try {
                                JSONObject jsonObject = new JSONObject(serverResponse);
                                profile.facebookEmail = jsonObject.getString("facebookEmail");
                                profile.facebookToken = jsonObject.getString("facebookToken");
                                profile.facebookName = jsonObject.getString("facebookName");
                                profile.facebookId = jsonObject.getString("facebookId");
                                profile.save(getActivity().getApplicationContext());
                                facebookConnectButton.setText("Disconnetti da " + InSquareProfile.facebookName);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto - POST Request
                    }
                });
    }

    private void setupTutorialButton(View layout)
    {
        tutorialButton = (TextView) layout.findViewById(R.id.settings_tutorial);
        tutorialButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: tutorialbutton");
                        //InSquareProfile profile = InSquareProfile.getInstance(getContext());
                        InSquareProfile.setShowTutorial(true, getContext());
                        Log.d(TAG, "onClick: showtutorial è " + InSquareProfile.getShowTutorial());
                        //setta showmap tutorial a false
                        BottomNavActivity madre = (BottomNavActivity) getContext();
                        Snackbar.make(madre.coordinatorLayout, "Rivedrai il tutorial riavviando l'app", Snackbar.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * TODO ??
     */
    @Override
    public void onStart() {
        super.onStart();
        if (gApiClient != null)
            gApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gApiClient != null && gApiClient.isConnected()) {
            gApiClient.stopAutoManage(getActivity());
            gApiClient.disconnect();
        }
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(connectionResult.hasResolution())
        {
            try
            {
                connectionResult.startResolutionForResult(getActivity(), 9000);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }else
        {
            Log.d(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
        Log.d(TAG, "Error on connection!\n" + connectionResult.getErrorMessage());
    }
}
