package com.nsqre.insquare.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.nsqre.insquare.User.InSquareProfile;

public class SplashActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener
{

    public static final String SHOW_TUTORIAL_KEY = "tutorial";
    private static final String TAG = "SplashActivity";
    private Intent nextScreen;
    private GoogleApiClient gApiClient;
    public boolean isFacebookInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext(),
                new FacebookSdk.InitializeCallback() {
                    @Override
                    public void onInitialized() {
                        Log.d(TAG, "onInitialized: I've been ");
                        isFacebookInit = true;
                    }
                });
        InSquareProfile.getInstance(getApplicationContext());

        OptionalPendingResult<GoogleSignInResult> googleSignedIn = initGoogle();

        while(!isFacebookInit)
        {
            Log.d(TAG, "onCreate: init'ing");
        }

        if(googleSignedIn.isDone() || isFacebookSignedIn())
        {
            nextScreen = new Intent(SplashActivity.this, BottomNavActivity.class);
        }else
        {
            nextScreen = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(nextScreen);
    }

    private OptionalPendingResult<GoogleSignInResult> initGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        gApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(gApiClient);

        return opr;
    }

    private boolean isFacebookSignedIn() {
        return AccessToken.getCurrentAccessToken() != null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: the connection with Google failed!");
    }
}
