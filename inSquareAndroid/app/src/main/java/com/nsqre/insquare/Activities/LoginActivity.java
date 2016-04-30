package com.nsqre.insquare.Activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.User.User;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import io.fabric.sdk.android.Fabric;

/**
 * This is the first activity the user will see. This activity deals with the login, taking tokens from Facebook or Google,
 * sending them to the back-end and elaborating the InSquareProfile
 * @see InSquareProfile
 */
public class LoginActivity extends AppCompatActivity
    implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{

    private static final String TAG = "LoginActivity";
    public static final int ITEM_DELAY = 300;

    private User user;
    private InSquareProfile profile;

    // Facebook Login
    private Button fbLoginButton;
    private CallbackManager fbCallbackManager;
    private String fbUserId;
    /**
     * The token received from Facebook
     */
    private String fbAccessToken;
    private AccessTokenTracker fbTokenTracker;
    // ==============

    // Google Login
    /**
     * The token received from Google
     */
    private String gAccessToken;
    private GoogleApiClient gApiClient;
    private GoogleSignInOptions gSo;
    private static final int RC_SIGN_IN = 9001;
    private Button gLoginButton;
    // ============

    private Tracker mTracker;

    /**
     * The OnCreate method of LoginActivity deals with the initialization of Google Analytics, the InSquareProfile data stored locally
     * and if they're not present gives the user the possibility to login via Facebook or Google
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_login);

        // Profilo statico perché non deve cambiare.
        // Singleton -> non puo' essere duplicato
        profile = InSquareProfile.getInstance(getApplicationContext());

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.logo_icon_144);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(getString(R.string.app_name),
                            icon, Color.parseColor("#D32F2F"));
            setTaskDescription(taskDesc);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }

        initLoginButtons();
    }

    /**
     * This method manages the authentication of the user
     * @see #launchInSquare()
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        ViewGroup container = (ViewGroup) findViewById(R.id.login_container);

        for(int i = 0; i < container.getChildCount(); i++)
        {
            View v = container.getChildAt(i);
            ViewPropertyAnimatorCompat viewAnimator;

            if (!(v instanceof Button) && !(v instanceof ImageView)) {
                viewAnimator = ViewCompat.animate(v)
                        .translationY(50).alpha(1)
                        .setStartDelay((ITEM_DELAY * i) + 500)
                        .setDuration(1000);
            } else {
                viewAnimator = ViewCompat.animate(v)
                        .scaleY(1).scaleX(1)
                        .setStartDelay((ITEM_DELAY * i) + 500)
                        .setDuration(500);
            }

            viewAnimator.setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    public void initLoginButtons() {

        fbCallbackManager = CallbackManager.Factory.create();

       /* if (profile.hasLoginData() && isNetworkAvailable()) {
            Log.d(TAG, "onCreate: haslogindata & networkavailable");
            launchInSquare();
            return;
        }else */

        if(!isNetworkAvailable()) {
            Toast.makeText(LoginActivity.this, R.string.login_no_connection, Toast.LENGTH_SHORT).show();
        }

        // Callback al ritorno da una richiesta a facebook
        LoginManager.getInstance().registerCallback(fbCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "Success Login");
                        requestFacebookData();  //fa la post
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
                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                        toast.show();
                    }
                });

        gLoginButton = (Button) findViewById(R.id.login_google_button);
        fbLoginButton = (Button) findViewById(R.id.login_fb_button);
        // Permessi da richiedere durante il login
        fbLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "email", "user_friends"));
            }
        });

        // setup delle opzioni di google+
        gSo = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();


        // builda il client e prova a chiamare startActivityForResult, se il login è stato già fatto precedentemente
        // il codice è quello corretto
        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gSo)
                .build();
        gApiClient.connect();

        gLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(gApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Ritorno dal login di Google+
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            googleSignInResult(result); //dalla post parte l'app
        } else {
            // Ritorno dal Login di Facebook
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        if(gApiClient != null)
        {
            gApiClient.connect();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onStop() {
        if (gApiClient != null && gApiClient.isConnected()) {
            gApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.hasResolution())
        {
            try
            {
                connectionResult.startResolutionForResult(this, 9000);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }else
        {
            Log.d(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
        Log.d(TAG, "Error on connection!\n" + connectionResult.getErrorMessage());
    }

    /**
     * This method initializes the values of the InSquareProfile
     * @param jsonUser the json string that represents the user
     * @see InSquareProfile
     */
    private void json2login(String jsonUser) {
        Gson gson = new Gson();
        user = gson.fromJson(jsonUser, User.class);
        Log.d(TAG, "json2login: " + user);
        profile.userId = user.getId();
        profile.username = user.getName();
        profile.email = user.getEmail();
        profile.pictureUrl = user.getPicture();
        if("undefined?sz=200".equals(profile.pictureUrl)){
            profile.pictureUrl = getString(R.string.avatarURL);
        }

        profile.save(getApplicationContext());

        launchInSquare();
    }

    /**
     * This method is called after the login is considered successful. It creates an intent to BottomNavActivity to open the map
     * or the ProfileFragment, depending on the extras that are put in it.
     * @see BottomNavActivity
     */
    private void launchInSquare() {
        Intent intent;
        if(InSquareProfile.showTutorial()) {
            intent = new Intent(this, TutorialActivity.class);
        }else {
            intent = new Intent(this, BottomNavActivity.class);
        }
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }

    /**
     * This method creates a POST request to the backend to manage Facebook login
     * The backend answers with data that are used in json2login
     * @see #json2login(String)
     */
    private void facebookPostRequest() {

        final String serviceName = "facebook";

        VolleyManager.getInstance(getApplicationContext()).postLoginToken(serviceName, fbAccessToken,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto - POST Request
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            Toast.makeText(LoginActivity.this, getString(R.string.login_token_error) + serviceName, Toast.LENGTH_SHORT).show();
                        } else {
                            String serverResponse = (String) object;
                            json2login(serverResponse);
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
                });
    }

    /**
     * This method creates a POST request to the backend to manage Google login
     * The backend answers with data that are used in json2login
     * @see #json2login(String)
     */
    private void googlePostRequest() {
        final String serviceName = "google";

        VolleyManager.getInstance(getApplicationContext()).postLoginToken(serviceName, gAccessToken,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto - POST Request
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            Toast.makeText(LoginActivity.this, getString(R.string.login_token_error) + serviceName, Toast.LENGTH_SHORT).show();
                        } else {
                            String serverResponse = (String) object;
                            json2login(serverResponse);
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
                });
    }

    /**
     * This method manages the result of the Google Authentication calling googlePostRequest() in case of success
     * @see #googlePostRequest()
     */
    private void googleSignInResult(GoogleSignInResult result) {

        Log.d("Google" + TAG, "Success? " + result.isSuccess() + "\nStatus Code: " + result.getStatus().getStatusCode());

        if (result.isSuccess())
        {
            GoogleSignInAccount acct = result.getSignInAccount();
            gAccessToken = acct.getIdToken();

//            Log.d(TAG, "Login was a success: " + acct.getDisplayName() + ": " + acct.getEmail());
//            Log.d(TAG, "Token is: " + acct.getIdToken());

            profile.googleEmail = acct.getEmail();
            profile.googleToken = acct.getIdToken();
            profile.googleName = acct.getDisplayName();
            profile.googleId = acct.getId();
            profile.save(getApplicationContext());

            googlePostRequest();
        } else {
            //connessione fallita
            CharSequence text = getString(R.string.connFail);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }

    /**
     * This method manages Facebook's authentication calling facebookPostRequest() in case of success
     * @see #facebookPostRequest()
     */
    private void requestFacebookData()
    {
        // Creazione di una nuova richiesta al grafo di Facebook per le informazioni necessarie
        GraphRequest graphRequest = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.d(TAG, "Hello Facebook!" + response.toString());

                try {
                    String nome = object.getString("name");
                    String email = object.getString("email");
                    String gender = object.getString("gender");
                    String id = object.getString("id");

                    AccessToken myToken = AccessToken.getCurrentAccessToken();

                    fbAccessToken = AccessToken.getCurrentAccessToken().getToken();

                    profile.facebookName = nome;
                    profile.facebookEmail = email;
                    profile.facebookId = id;
                    profile.facebookToken = fbAccessToken;

                    profile.save(getApplicationContext());

                    Log.d(TAG, "Name: " + nome
                            + " email: " + email
                            + " Gender: " + gender
                            + " ID: " + id);

                    facebookPostRequest();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "id,name,gender,email,picture");
        graphRequest.setParameters(params);
        graphRequest.executeAsync();
    }

    /**
     * This method checks if the network is currently available
     * @return true if the Network is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
