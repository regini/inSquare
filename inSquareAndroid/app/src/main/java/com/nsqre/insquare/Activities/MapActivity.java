package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener
{

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

    private Tracker mTracker;

    private SearchView searchView;
    private SimpleCursorAdapter mSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();


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
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
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
        inflater.inflate(R.menu.activity_map_actions, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search_squares_action);

        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getString(R.string.hint_cerca_squares));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        AutoCompleteTextView searchTextView =
                (AutoCompleteTextView) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchTextView.setHintTextColor(ContextCompat.getColor(this, R.color.light_grey));
        searchTextView.setTextColor(ContextCompat.getColor(this, R.color.white));

        ArrayAdapter<Address> adapter = new ArrayAdapter<Address>(this, R.layout.drop_down_entry);
        searchTextView.setAdapter(adapter);

        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search_squares_action:
                // [START search_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Search")
                        .build());
                // [END search_event]

                Log.d(TAG, "I've just initiated search");
                break;
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(linearLayout.getVisibility() == View.VISIBLE)
        {
            linearLayout.startAnimation(animationDown);
            linearLayout.setVisibility(View.GONE);
            return;
        }else
        {
            // Termina l'activity quando viene premuto BACK
            this.finishAffinity();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "onQueryTextSubmit: Currently looking for: " + query);
        searchLocationName(query.trim());
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, "onQueryTextChange: just written:" + newText);
        return false;
    }

    private void searchLocationName(String placeName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            MapFragment mFrag = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);
            Location l = mFrag.mCurrentLocation;
            LatLng northeast = calculateOffset(l, 8000);
            LatLng southwest = calculateOffset(l, -8000);

            List<Address> results = geocoder.getFromLocationName(placeName,
                    5, // num results
                    southwest.latitude, //double lowerLeftLatitude,
                    southwest.longitude, //double lowerLeftLongitude,
                    northeast.latitude, //double upperRightLatitude,
                    northeast.longitude //double upperRightLongitude
            );


            if(!results.isEmpty())
            {
                Address first = results.get(0);
                LatLng pos = new LatLng(first.getLatitude(), first.getLongitude());
                mFrag.mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLng(pos),
                        400,
                        null
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LatLng calculateOffset(Location position, float offset)
    {
        final int RADIUS = 6378137;
        float diff_lat = offset/RADIUS;
        double diff_lon = offset/(RADIUS * Math.cos(Math.PI * position.getLatitude()/180));

        double lat = position.getLatitude() + diff_lat*180/Math.PI;
        double lon = position.getLongitude() + diff_lon*180/Math.PI;

        return new LatLng(lat, lon);
    }
}
