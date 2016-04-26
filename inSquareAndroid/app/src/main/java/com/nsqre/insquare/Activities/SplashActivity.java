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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        InSquareProfile.getInstance(getApplicationContext());

        if(isFacebookSignedIn() || isGoogleSignedIn())
        {
            Log.d(TAG, "onCreate: vado alla mappa!");
            nextScreen = new Intent(this, BottomNavActivity.class);

        }else
        {
            nextScreen = new Intent(this, LoginActivity.class);
            if(getIntent().getExtras() != null) {
                if(getIntent().getExtras().getInt("map") == 0) {
                    nextScreen.putExtra("map", getIntent().getIntExtra("map",0));
                }
                else if(getIntent().getStringExtra("squareId") != null) {
                    nextScreen.putExtra("s  quareId", getIntent().getStringExtra("squareId"));
                }
                getIntent().getExtras().clear();
            }
        }

        startActivity(nextScreen);

    }

    private boolean isGoogleSignedIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        gApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(gApiClient);
        return opr.isDone();
    }

    private boolean isFacebookSignedIn() {
        return AccessToken.getCurrentAccessToken() != null;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: the connection with Google failed!");
    }
}
