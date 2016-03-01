package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
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
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.gson.Gson;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;
import com.nsqre.insquare.Utilities.RegistrationIntentService;
import com.nsqre.insquare.Utilities.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity
    implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{

    private static final String TAG = "LoginActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private User user;
    private InSquareProfile profile;

    // Facebook Login
    private Button fbLoginButton;
    private CallbackManager fbCallbackManager;
    private String fbUserId;
    private String fbAccessToken;
    private AccessTokenTracker fbTokenTracker;
    // ==============

    // Google Login
    private String gAccessToken;
    private GoogleApiClient gApiClient;
    private GoogleSignInOptions gSo;
    private static final int RC_SIGN_IN = 9001;
    private Button gLoginButton;
    // ============

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());


        // Profilo statico perche' non puo' cambiare.
        // Singleton perche' cosi non puo' essere duplicato
        profile = InSquareProfile.getInstance(getApplicationContext());

        Log.d("DATILOGIN", "is: " + profile.hasLoginData());
        Log.d("NETWORK", "is "+ isNetworkAvailable());
        if (profile.hasLoginData() && isNetworkAvailable()) {
            launchInSquare();
        }

        fbCallbackManager = CallbackManager.Factory.create();

        //chiamato quando c'è un successo(o fallimento) della connessione a fb
        LoginManager.getInstance().registerCallback(fbCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("Success", "Login");
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
                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                        toast.show();
                    }
                });

        gLoginButton = (Button) findViewById(R.id.google_login_button);
        fbLoginButton = (Button) findViewById(R.id.fb_login_button);
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

        // Se il login e' gia' stato effettuato, fai le post
        if(isGoogleSignedIn()) {
            Log.d(TAG, "Google is already logged in!");
            //gLoginButton.setText(R.string.google_logout_string);
            googlePostRequest();
        } else if(isFacebookSignedIn())
        {
            Log.d(TAG, "onCreate: Facebook is already logged in!");
            //fbLoginButton.setText(R.string.fb_logout_string);
            facebookPostRequest();
        }

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
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

    //metodo che elabora il json preso dalle post, crea l'oggetto user e va @chatActivity
    private void json2login(String jsonUser) {
        Gson gson = new Gson();
        user = gson.fromJson(jsonUser, User.class);
        Log.d(TAG, "json2login: " + user);
        profile.userId = user.getId();
        profile.username = user.getName();
        profile.email = user.getEmail();
        profile.pictureUrl = user.getPicture();
        profile.save(getApplicationContext());
        launchInSquare();
    }

    //metodo che crea l'intent alla map activity
    private void launchInSquare() {
        Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        startActivity(intent);
    }

    private void facebookPostRequest() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
        String url = "http://recapp-insquare.rhcloud.com/auth/facebook/token";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ServerResponse", response);
                        json2login(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ServerResponse", error.toString());
            }
        }) {
            //TOKEN messo nei parametri della query
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("access_token", fbAccessToken);

                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void googlePostRequest() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
        String url = "http://recapp-insquare.rhcloud.com/auth/google/token";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("google+response", "Response is: " + response);
                        json2login(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("GOOGLE+Response", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("access_token", gAccessToken);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    //quando il login a g+ va a buon fine esegue questo, dove c'è la post
    private void googleSignInResult(GoogleSignInResult result) {

        Log.d("Google" + TAG, "Success? " + result.isSuccess() + "\nStatus Code: " + result.getStatus().getStatusCode());

        if (result.isSuccess())
        {
            GoogleSignInAccount acct = result.getSignInAccount();
            gAccessToken = acct.getIdToken();

            Log.d(TAG, "Login was a success: " + acct.getDisplayName() + ": " + acct.getEmail());
            Log.d(TAG, "Token is: " + acct.getIdToken());

            //gLoginButton.setText(R.string.google_logout_string);

            profile.googleEmail = acct.getEmail();
            profile.googleToken = acct.getIdToken();
            profile.googleName = acct.getDisplayName();
            profile.googleId = acct.getId();
            profile.save(getApplicationContext());

            googlePostRequest();
        } else { //connessione fallita
            CharSequence text = getString(R.string.connFail);
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }

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

    private boolean isGoogleSignedIn()
    {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(gApiClient);
        boolean result = opr.isDone();
        if(result)
            gAccessToken = opr.get().getSignInAccount().getIdToken();

        return result;
    }

    private boolean isFacebookSignedIn()
    {
        // Controlla che Facebook sia gia' loggato
        try {
            AccessToken token = AccessToken.getCurrentAccessToken();
            if(token != null)
            {
                fbAccessToken = token.getToken();
                Log.d("token", fbAccessToken);
                return true;
            }

            return false;
        }
        catch (Exception e) {
            Log.d("token", e.toString());
        }
        return false;
    }

    //Controllo della disponibilità della connessione
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //FEEDBACK STUFF
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.instfeedback:
                // [START feedback_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Feedback")
                        .build());
                // [END feedback_event]
                final Dialog d = new Dialog(this);
                d.setContentView(R.layout.dialog_feedback);
                d.setTitle("Feedback");
                d.setCancelable(true);
                d.show();

                final EditText feedbackText = (EditText) d.findViewById(R.id.dialog_feedbacktext);
                Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String feedback = feedbackText.getText().toString();
                        final String activity = this.getClass().getSimpleName();
                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                        String url = "http://recapp-insquare.rhcloud.com/feedback";

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("VOLLEY", "ServerResponse: " + response);
                                        CharSequence text = getString(R.string.thanks_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("VOLLEY", error.toString());
                                CharSequence text = getString(R.string.error_feedback);
                                int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                toast.show();
                            }
                        }) {
                            @Override
                            protected Map<String, String> getParams() {
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("feedback", feedback);
                                if (user != null)
                                    params.put("username", user.getId());
                                params.put("activity", activity);
                                return params;
                            }
                        };
                        queue.add(stringRequest);
                        d.dismiss();
                    }
                });
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
