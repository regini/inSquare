package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nsqre.insquare.Fragments.MainMapFragment;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.ProfileFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.DrawerListAdapter;
import com.nsqre.insquare.Utilities.LocationServices;
import com.nsqre.insquare.Utilities.MyInstanceIDListenerService;
import com.nsqre.insquare.Utilities.NavItem;
import com.nsqre.insquare.Utilities.Square;
import com.nsqre.insquare.Utilities.SquareDeserializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener, InSquareProfile.InSquareProfileListener
{
    private static final String TAG = "MapActivity";

    private float startTouchY;

    private String mSquareId;
    private String mSquareName;

    private Tracker mTracker;

    private SearchView searchView;
    private SimpleCursorAdapter mSearchAdapter;

    // Hamburger Menu
    ListView mDrawerList;
    RelativeLayout mDrawerPane;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    // ==================
    // Fragments del menu
    private ProfileFragment profileFragment;
    private RecentSquaresFragment recentSquaresFragment;
    private MainMapFragment mainMapFragment;
    // ==================
    private ImageView drawerImage;
    private TextView drawerUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        //IMMAGINE
        drawerImage = (ImageView) findViewById(R.id.drawer_avatar);
        drawerUsername = (TextView) findViewById(R.id.drawer_userName);
        Bitmap bitmap = loadImageFromStorage();
        if (bitmap == null) {
            new DownloadImageTask(drawerImage, this).execute(InSquareProfile.getPictureUrl());
        } else {
             drawerImage.setImageBitmap(bitmap);
        }
        drawerUsername.setText(InSquareProfile.getUsername());

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        int recCount = 0;
        int profCount = 0;
        for(String id : sharedPreferences.getAll().keySet()) {
            if(InSquareProfile.isOwned(id)) {
                profCount += sharedPreferences.getInt(id, 0);
            } else if(InSquareProfile.isFav(id)) {
                profCount += sharedPreferences.getInt(id, 0);
            }
            if(InSquareProfile.isRecent(id)) {
                recCount += sharedPreferences.getInt(id, 0);
            }
        }

        mNavItems.add(new NavItem("Mappa", "Dai un'occhiata in giro", R.drawable.google_maps, 0));  // 0 fa scomparire il notification counter
        mNavItems.add(new NavItem("Squares recenti", "Non perderti un messaggio", R.drawable.google_circles_extended, recCount));
        mNavItems.add(new NavItem("Profilo", "Gestisci il tuo profilo", R.drawable.account_circle, profCount));

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        // Populate the Navigtion Drawer with options
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        mDrawerList = (ListView) findViewById(R.id.navList);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItemFromDrawer(position);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //crea il bottone toggle del drawer menu
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // C'è l'override di questo metodo, ma non serve a nulla
                // (teoricamente toglie dall'action bar ciò che non vogliamo far vedere quando l'hamburger è aperto
                invalidateOptionsMenu();
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "onDrawerClosed: " + getTitle());
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mainMapFragment = new MainMapFragment();
        recentSquaresFragment = new RecentSquaresFragment();
        profileFragment = new ProfileFragment();

        if(getIntent().getExtras() != null) {
            selectItemFromDrawer(getIntent().getExtras().getInt("profile"));
            getIntent().getExtras().clear();
        }
        else {
            selectItemFromDrawer(0);
        }

        Intent idService = new Intent(this, MyInstanceIDListenerService.class);
        startService(idService);
        Intent locationService = new Intent(this, LocationServices.class);
        startService(locationService);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getOwnedSquares();
        getFavouriteSquares();
        getRecentSquares();
        InSquareProfile.addListener(this);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("event");
            Log.d("receiver", "Got message: " + message);
            getOwnedSquares();
            getFavouriteSquares();
            getRecentSquares();
        }
    };

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

    public String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory,"profileImage.png");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    public Bitmap loadImageFromStorage()
    {
        Bitmap b = null;

        try {
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File f=new File(directory, "profileImage.png");
            b = BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return b;
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
//        inflater.inflate(R.menu.activity_map_actions, menu);
        inflater.inflate(R.menu.activity_main_actions, menu);

//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        MenuItem searchItem = menu.findItem(R.id.search_squares_action);
//
//        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
//        searchView.setQueryHint(getString(R.string.hint_cerca_squares));
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//
//        AutoCompleteTextView searchTextView =
//                (AutoCompleteTextView) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
//        searchTextView.setHintTextColor(ContextCompat.getColor(this, R.color.light_grey));
//        searchTextView.setTextColor(ContextCompat.getColor(this, R.color.white));
//
//        ArrayAdapter<Address> adapter = new ArrayAdapter<Address>(this, R.layout.drop_down_entry);
//        searchTextView.setAdapter(adapter);
//
//        searchView.setIconifiedByDefault(false);
//        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled
        // the nav drawer indicator touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {   //permette al toggle di aprire e chiudere il menu hamburger
            return true;
        }
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
                                        Log.d(TAG,"VOLLEY ServerResponse: "+response);
                                        CharSequence text = getString(R.string.thanks_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "VOLLEY " + error.toString());
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
                                params.put("username", InSquareProfile.getUserId());
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
    public void onBackPressed()
    {
        this.finishAffinity();
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
            Location l;
            if(mFrag.mCurrentLocation == null)
            {
                l = new Location("");
                l.setLatitude(0.0d);
                l.setLongitude(0.0d);
            }
            else
                l = mFrag.mCurrentLocation;

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

    /*
    *  Apre il fragment scelto dal burger menu
    *
    * */
    private void selectItemFromDrawer(int position) {
        //switch che in base al pulsante scelto esegue
        switch (position) {
            case 0:   //caso mappa
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, mainMapFragment, "MAPPA")
                        .commit();
                break;
            case 1:  //caso recenti
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, recentSquaresFragment, "RECENTS")
                        .commit();
                break;
            case 2:  //caso profilo
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, profileFragment, "PROFILE")
                        .commit();
                break;
            default:
                break;
        }
        mDrawerList.setItemChecked(position, true);
        setTitle(mNavItems.get(position).getmTitle());
        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawerPane);
    }

    public void getOwnedSquares() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = String.format("http://recapp-insquare.rhcloud.com/squares?byOwner=true&ownerId=%1$s",
                InSquareProfile.getUserId());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        GsonBuilder b = new GsonBuilder();
                        // MessageDeserializer specifica come popolare l'oggetto Message fromJson
                        b.registerTypeAdapter(Square.class, new SquareDeserializer(getResources().getConfiguration().locale));
                        Gson gson = b.create();
                        try {
                            Square[] squares = gson.fromJson(response, Square[].class);
                            InSquareProfile.setOwnedSquaresList(new ArrayList<>(Arrays.asList(squares)));
                            InSquareProfile.save(getApplicationContext());
//                        Log.d(TAG, "onResponse: ho ottenuto OWNED con successo!");
                            Log.d(TAG, "onResponse Owned: " + InSquareProfile.getOwnedSquaresList().toString());
                        } catch (Exception ex) {
                          ex.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "GETOWNEDSQUARES" + error.toString());
            }
        });
        queue.add(stringRequest);
    }

    public void getFavouriteSquares() {
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = String.format("http://recapp-insquare.rhcloud.com/favouritesquares/%1$s",
                InSquareProfile.getUserId());

        Log.d(TAG, "getFavouriteSquares: " + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        GsonBuilder b = new GsonBuilder();
                        // SquareDeserializer specifica come popolare l'oggetto Message fromJson
//                        Log.d(TAG, "onResponse: " + response);
                        b.registerTypeAdapter(Square.class, new SquareDeserializer(getResources().getConfiguration().locale));
                        Gson gson = b.create();
                        try {
                            Square[] squares = gson.fromJson(response, Square[].class);
                            InSquareProfile.setFavouriteSquaresList(new ArrayList<Square>(Arrays.asList(squares)));
//                        userProfile.favouriteSquaresList = new ArrayList<>(Arrays.asList(squares));
                            InSquareProfile.save(getApplicationContext());
                            Log.d(TAG, "onResponse Favourites: " + InSquareProfile.getFavouriteSquaresList().toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "GETFAVOURITESQUARES " + error.toString());
            }
        });
        queue.add(stringRequest);
    }

    public void getRecentSquares() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = String.format("http://recapp-insquare.rhcloud.com/recentSquares/%1$s",
                InSquareProfile.getUserId());

        Log.d(TAG, "getRecentSquares: " + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        GsonBuilder b = new GsonBuilder();
                        // SquareDeserializer specifica come popolare l'oggetto Message fromJson
//                        Log.d(TAG, "onResponse: " + response);
                        b.registerTypeAdapter(Square.class, new SquareDeserializer(getResources().getConfiguration().locale));
                        Gson gson = b.create();
                        try {
                            Square[] squares = gson.fromJson(response, Square[].class);
//                        userProfile.recentSquaresList = new ArrayList<>(Arrays.asList(squares));
                            InSquareProfile.setRecentSquaresList(new ArrayList<Square>(Arrays.asList(squares)));
//                        userProfile.save(getApplicationContext());
                            InSquareProfile.save(getApplicationContext());
                            Log.d(TAG, "onResponse Recents: " + InSquareProfile.getRecentSquaresList().toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "GETRECENTQUARES " + error.toString());
            }
        });
        queue.add(stringRequest);
    }

    public ProfileFragment getProfileFragment() {
        return profileFragment;
    }

    @Override
    // Called when invalidateOptionsMenu() is invoked
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        //menu.findItem(R.id.action_search).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void checkNotifications() {
        //TODO contare i messaggi e non le piazze
        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        int recCount = 0;
        int prefCount = 0;
        TextView recents = (TextView) mDrawerList.getChildAt(1).findViewById(R.id.drawer_counter);
        TextView profile = (TextView) mDrawerList.getChildAt(2).findViewById(R.id.drawer_counter);
        for(String id : sharedPreferences.getAll().keySet()) {
            if(InSquareProfile.isOwned(id)) {
                prefCount += sharedPreferences.getInt(id, 0);
            } else if(InSquareProfile.isFav(id)) {
                prefCount += sharedPreferences.getInt(id, 0);
            }
            if(InSquareProfile.isRecent(id)) {
                recCount += sharedPreferences.getInt(id, 0);
            }
        }
        if(recCount == 0) {
            recents.setVisibility(View.GONE);
        } else {
            recents.setVisibility(View.VISIBLE);
        }
        if(prefCount == 0) {
            profile.setVisibility(View.GONE);
        } else {
            profile.setVisibility(View.VISIBLE);
        }
        recents.setText(String.valueOf(recCount));
        profile.setText(String.valueOf(prefCount));
    }

    @Override
    public void onOwnedChanged() {
        checkNotifications();
    }

    @Override
    public void onFavChanged() {
        checkNotifications();
    }

    @Override
    public void onRecentChanged() {
        checkNotifications();
    }
}
