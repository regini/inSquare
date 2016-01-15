package com.nsqre.insquare.Activities;

import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.gson.Gson;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.User;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextView info;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private String userId;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        info = (TextView)findViewById(R.id.info);
        loginButton = (LoginButton)findViewById(R.id.login_button);

        callbackManager = CallbackManager.Factory.create();
        final Button loginfb = (Button) findViewById(R.id.buttonfb);

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                userId = loginResult.getAccessToken().getUserId();
                accessToken = loginResult.getAccessToken().getToken();
                info.setText(
                    "User ID: "
                    + userId
                    + "\n" +
                    "Auth Token: "
                    + accessToken
                );
            }

            @Override
            public void onCancel() {
                info.setText("Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException e) {
                info.setText(e.toString());
            }
        });

        loginfb.setOnClickListener(new View.OnClickListener() {
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
                                loginfb.setText("Response is: " + response);
                                User user = new User();
                                //// TODO: 15/01/2016  SOME JSON MAGIC MUST HAPPEN HERE
                                //Gson gson = new Gson();
                                //user = gson.fromJson(response, user.getClass());
                                Intent intent = new Intent(getApplicationContext(), FabActivity.class);
                                intent.putExtra("CURRENT_USER", user);
                                startActivity(intent);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loginfb.setText(error.toString());
                    }
                }) {
                    /*
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("userid", userId);
                        params.put("token", accessToken);

                        return params;
                    }
                    */
                    @Override
                    protected Map<String,String> getParams(){
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("access_token", accessToken);

                        return params;
                    }

                    /*
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("Content-Type","application/x-www-form-urlencoded");
                        return params;
                    }
                    */
                };

                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        accessToken = AccessToken.getCurrentAccessToken().getToken(); //SE UTENTE HA GIà LOGGATO UN'ALTRA VOLTA, RITROVA IL TOKEN.
        Log.d("token", accessToken);
    }
}
