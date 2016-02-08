package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    private LinearLayout linearLayout;
    private FloatingActionButton mapFab;
    private float startTouchY;
    private Animation animationUp, animationDown;

    // Variabili per l'inizializzazione della Chat
    public static final String SQUARE_ID_TAG = "SQUARE_URL";
    public static final String SQUARE_NAME_TAG = "SQUARE_NAME";

    private String mSquareId;
    private String mSquareName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapFab = (FloatingActionButton) findViewById(R.id.map_fab);
        mapFab.setVisibility(View.GONE);

        mapFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, ChatActivity.class);

                intent.putExtra(SQUARE_ID_TAG, mSquareId);
                intent.putExtra(SQUARE_NAME_TAG, mSquareName);
                startActivity(intent);
            }
        });

        linearLayout = (LinearLayout) findViewById(R.id.slider_ll);
        linearLayout.setVisibility(View.GONE);
        animationUp = AnimationUtils.loadAnimation(this, R.anim.anim_up);
        animationDown = AnimationUtils.loadAnimation(this, R.anim.anim_down);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                startTouchY = event.getY();
            case MotionEvent.ACTION_UP:
                float endTouchY = event.getY();

                if(endTouchY < startTouchY)
                {
                    Log.d(TAG, "Moving Up");
//                    linearLayout.setVisibility(View.VISIBLE);
//                    linearLayout.startAnimation(animationUp);
                }else {
                    Log.d(TAG, "Moving Down!");
//                    linearLayout.setVisibility(View.GONE);
//                    linearLayout.startAnimation(animationDown);
                }
        }

        return super.onTouchEvent(event);
    }

    public void setSquareName(String squareName) {
        this.mSquareName = squareName;
    }

    public void setSquareId(String mSquareId) {
        this.mSquareId = mSquareId;
    }

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
                        RequestQueue queue = Volley.newRequestQueue(MapActivity.this);
                        String url = "http://recapp-insquare.rhcloud.com/feedback";

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("VOLLEY","ServerResponse: "+response);
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
                                params.put("username", InSquareProfile.getInstance(getApplicationContext()).getUserId());
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

}
