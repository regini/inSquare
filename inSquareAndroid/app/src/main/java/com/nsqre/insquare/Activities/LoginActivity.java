package com.nsqre.insquare.Activities;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.gson.Gson;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity
    implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{

    private static final String TAG = "LoginActivity";

    // Facebook Login
    private LoginButton fbLoginButton;
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
//    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 211;
    // ============

    private Button goChatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        fbCallbackManager = CallbackManager.Factory.create();

        fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);
        // Permessi da richiedere durante il login
        fbLoginButton.setReadPermissions("public_profile", "email", "user_friends");

        // Controlla che un utente si sia gia' loggato
        fbTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if(AccessToken.getCurrentAccessToken() != null)
                {
                    Log.d(TAG, "onCurrentAccessTokenChanged: Facebook already logged in!");
                    startInSquare();
                }
            }
        };

        gLoginButton = (Button) findViewById(R.id.google_login_button);

        fbLoginButton.registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(AccessToken.getCurrentAccessToken() != null)
                {
                    requestData();
                }
            }

            @Override
            public void onCancel() {
                Log.d(TAG + "Facebook", "Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException e) {
                Log.d(TAG + "Facebook", e.toString());
            }
        });

        // setup delle opzioni di google+
        gSo = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestIdToken(getString(R.string.server_client_id))
                .requestEmail()
                .build();


        // builda il client e prova a chiamare startActivityForResult, se il login è stato già fatto precedentemente
        // il codice è quello corretto e l'app passa subito a fabactivity(ma tanto non lo fa perchè la post non funziona)
        gApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gSo)
                .build();
        gApiClient.connect();

        // Se il login e' gia' in cache, fai partire le chat
        if(getGoogleSignIn().isDone())
        {
            startInSquare();
            gLoginButton.setText(R.string.google_logout_string);
        }

        // Non avendo ancora effettuato il Login con Google
        gLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(gApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        // accesso alla chat
        // bottone che invia il token a facebook e ti fa entrare nella fabactivity
        goChatButton = (Button) findViewById(R.id.chat_now_button);

        goChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        goChatButton.setText(error.toString());
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
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Ritorno dal login di Google+
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            startInSquare();
        } else {
            // Ritorno dal Login di Facebook
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
            startInSquare();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //FACEBOOK
        try {
            fbAccessToken = AccessToken.getCurrentAccessToken().getToken(); //SE UTENTE HA GIà LOGGATO UN'ALTRA VOLTA, RITROVA IL TOKEN.
            Log.d("token", fbAccessToken);
        }
        catch (Exception e) {
            Log.d("token", e.toString());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG + " Google", "Client connesso con successo!");
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

    private void googleDialogSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(gApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void startInSquare()
    {
        Intent i = new Intent(LoginActivity.this, MapActivity.class);
        startActivity(i);
    }

    //metodo che elabora il json preso dalle post, crea l'oggetto user e va a fabactivity
    private void json2login(String jsonUser) {
        User user;
        Gson gson = new Gson();
        user = gson.fromJson(jsonUser, User.class);
        Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        intent.putExtra("CURRENT_USER", user);
        startActivity(intent);
    }

    //quando il login a g+ va a buon fine esegue questo, dove c'è la post(che da errore 500)
    private void handleSignInResult(GoogleSignInResult result) {

        Log.d("Google" + TAG, "Success? " + result.isSuccess() + "\nStatus Code: " + result.getStatus().getStatusCode());

        if (result.isSuccess())
        {

            GoogleSignInAccount acct = result.getSignInAccount();
            gAccessToken = acct.getIdToken();

            Log.d(TAG, "Login was a success: " + acct.getDisplayName() + ": " + acct.getEmail());
            Log.d(TAG, "Token is: " + acct.getIdToken());

            gLoginButton.setText(R.string.google_logout_string);

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
    }

    private void requestData()
    {
        // Creazione di una nuova richiesta al grafo di Facebook per le informazioni necessarie
        GraphRequest graphRequest = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.d(TAG, "Hello Facebook!" + response.toString());

                try
                {
                    String nome = object.getString("name");
                    String email = object.getString("email");
                    String gender = object.getString("gender");

                    Log.d(TAG, "Name: " + nome
                                + " email: " + email
                                + " Gender: " + gender);

                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "id,name,gender,email,picture");
        graphRequest.setParameters(params);
        graphRequest.executeAsync();
    }

    private OptionalPendingResult<GoogleSignInResult> getGoogleSignIn()
    {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(gApiClient);

        return opr;
    }
}
