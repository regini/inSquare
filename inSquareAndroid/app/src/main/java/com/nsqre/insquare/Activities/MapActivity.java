package com.nsqre.insquare.Activities;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.Fragments.ProfileFragment;
import com.nsqre.insquare.Fragments.RecentSquaresFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.SearchAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.DownloadImageTask;
import com.nsqre.insquare.Utilities.DrawerListAdapter;
import com.nsqre.insquare.Utilities.LocationService;
import com.nsqre.insquare.Utilities.NavItem;
import com.nsqre.insquare.Utilities.PushNotification.MyInstanceIDListenerService;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class  MapActivity extends AppCompatActivity
        implements InSquareProfile.InSquareProfileListener
{
    public static final String TAG_PROFILE_FRAGMENT = "PROFILE";
    private static final String TAG = "MapActivity";
    public static final String TAG_MAP_FRAGMENT = "MAPPA";
    public static final String TAG_RECENT_FRAGMENT = "RECENTS";
    public static final String MAPPA_FRAGMENT_TITLE = "Mappa";
    public static final String RECENTI_FRAGMENT_TITLE = "Recenti";
    public static final String PROFILO_FRAGMENT_TITLE = "Profilo";

    private float startTouchY;

    private String mSquareId;
    private String mSquareName;

    private Tracker mTracker;


    private SearchView searchView;
    private CursorAdapter mSearchAdapter;

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
    private MapFragment mapFragment;
    // ==================
    private ImageView drawerImage;
    private TextView drawerUsername;
    private Menu menu;


    private MapActivity mp;
    private List<Square> searchItems;
    private MatrixCursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // SEARCH
        searchItems = new ArrayList<>();
        /*
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //MapFragment mFrag = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);
            searchSquares(query);
        }
        */

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

        mNavItems.add(new NavItem(MAPPA_FRAGMENT_TITLE, "Dai un'occhiata in giro", R.drawable.google_maps, 0));  // 0 fa scomparire il notification counter
        mNavItems.add(new NavItem(RECENTI_FRAGMENT_TITLE, "Non perderti un messaggio", R.drawable.google_circles_extended, recCount));
        mNavItems.add(new NavItem(PROFILO_FRAGMENT_TITLE, "Gestisci il tuo profilo", R.drawable.account_circle, profCount));

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

        mapFragment = new MapFragment();
        recentSquaresFragment = new RecentSquaresFragment();
        profileFragment = new ProfileFragment();

        if(getIntent().getExtras() != null) {
            if(getIntent().getIntExtra("profile", 0) == 2) {
                selectItemFromDrawer(getIntent().getExtras().getInt("profile"));
            } else if(getIntent().getStringExtra("squareId") != null) {
                selectItemFromDrawer(0);
            }
            getFavouriteSquares();
            getOwnedSquares();
            getRecentSquares();
        }
        else {
            selectItemFromDrawer(0);
        }

        Intent idService = new Intent(this, MyInstanceIDListenerService.class);
        startService(idService);
        Intent locationService = new Intent(this, LocationService.class);
        locationService.putExtra("stopservice", false);
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

    public void setSquareId(String mSquareId) {
        this.mSquareId = mSquareId;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map_actions, menu);

        this.menu = menu;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView search = (SearchView) menu.findItem(R.id.search_squares_action).getActionView();
            search.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

            search.setOnQueryTextListener(new OnQueryTextListener() {

                @Override
                public boolean onQueryTextSubmit(String query) {
                    // loadHistory(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    if(query.length()>=3){
                        loadHistory(query);
                    }
                    return true;
                }
            });
        }


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

    // History
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void loadHistory(String query) {

        searchSquares(query);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            // Cursor
            final String[] columns = new String[] { "_id", "text" };
            Object[] temp = new Object[] { 0, "default" };

            cursor = new MatrixCursor(columns);
            if(searchItems!=null) {
                for (int i = 0; i < searchItems.size(); i++) {
                    temp[0] = i;
                    temp[1] = searchItems.get(i).getName(); //replaced s with i as s not used anywhere.
                    cursor.addRow(temp);

                }
            }

            // SearchView
            SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            final SearchView search = (SearchView) menu.findItem(R.id.search_squares_action).getActionView();

            search.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    Log.d("POSITION CLICK", ""+position);
                    if(position<searchItems.size()) {
                        Square s = searchItems.get(position);
                        if (s != null) {
                            mapFragment.startChatActivity(s);
                            mapFragment.setMapInPosition(s.getLat(), s.getLon());
                        }
                        searchItems = new ArrayList<Square>();
                        cursor = new MatrixCursor(columns);
                    }
                    return true;
                }
            });

            search.setSuggestionsAdapter(new SearchAdapter(this, cursor, searchItems));
        }
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
            /*case R.id.search_squares_action:
                // [START search_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Search")
                        .build());
                // [END search_event]

                Log.d(TAG, "I've just initiated search");
                break;*/
            case R.id.menu_entry_feedback:

                // [START feedback_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Feedback")
                        .build());
                // [END feedback_event]

                final Dialog feedbackDialog = new Dialog(this);
                feedbackDialog.setContentView(R.layout.dialog_feedback);
                feedbackDialog.setTitle("Invia Feedback");
                feedbackDialog.setCancelable(true);
                feedbackDialog.show();

                final EditText feedbackEditText = (EditText) feedbackDialog.findViewById(R.id.dialog_feedbacktext);

                // Parametri:
                final String feedbackParam = feedbackEditText.getText().toString().trim();
                final String activityParam = this.getClass().getSimpleName();
                final String userIdParam = InSquareProfile.getUserId();

                Button confirm = (Button) feedbackDialog.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v) {
                        VolleyManager.getInstance().postFeedback(
                                feedbackParam,
                                userIdParam,
                                activityParam,
                                new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        // Vuoto -- POST Request
                                    }

                                    @Override
                                    public void responsePOST(Object object) {
                                        if(object == null){
                                            Toast.makeText(MapActivity.this, "Non sono riuscito ad inviare il feedback!", Toast.LENGTH_LONG).show();
                                        }else {
                                            Toast.makeText(MapActivity.this, "Feedback inviato con successo!", Toast.LENGTH_SHORT).show();
                                            feedbackDialog.dismiss();
                                        }
                                    }

                                    @Override
                                    public void responsePATCH(Object object) {
                                        // Vuoto -- POST Request
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Vuoto -- POST Request
                                    }
                                }
                        );
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
        int backStackSize = getSupportFragmentManager().getBackStackEntryCount();

        if(backStackSize < 1) {
            return;
        }else if(backStackSize == 1) {
            this.finishAffinity();
        }else {
            String fragmentTag = getSupportFragmentManager().getBackStackEntryAt(backStackSize - 2).getName();
            Log.d(TAG, "onBackPressed: " + backStackSize);
            Log.d(TAG, "onBackPressed: " + fragmentTag);
            getSupportActionBar().setTitle(fragmentTag);
        }

        super.onBackPressed();
    }

    /*
    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "onQueryTextSubmit: Currently looking for: " + query);
        searchLocationName(query.trim());
        searchView.clearFocus();
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

    */

    /*
    *  Apre il fragment scelto dal burger menu
    *
    * */
    private void selectItemFromDrawer(int position) {
        //switch che in base al pulsante scelto esegue
        switch (position) {
            case 0:   //caso mappa
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, mapFragment, TAG_MAP_FRAGMENT)
                        .addToBackStack(MAPPA_FRAGMENT_TITLE)
                        .commit();
                break;
            case 1:  //caso recenti
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, recentSquaresFragment, TAG_RECENT_FRAGMENT)
                        .addToBackStack(RECENTI_FRAGMENT_TITLE)
                        .commit();
                break;
            case 2:  //caso profilo
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_content_layout, profileFragment, TAG_PROFILE_FRAGMENT)
                        .addToBackStack(PROFILO_FRAGMENT_TITLE)
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
        VolleyManager.getInstance().getOwnedSquares("true", InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Log.d(TAG, "responseGET: getOwnedSquares returned NULL!");
                        } else {
                            InSquareProfile.setOwnedSquaresList((ArrayList<Square>) object);
                            Log.d(TAG, "onResponse: ho ottenuto OWNED con successo!");
//                            Log.d(TAG, "onResponse Owned: " + InSquareProfile.getOwnedSquaresList().toString());

                        }
                    }

                    @Override
                    public void responsePOST(Object object) {

                    }

                    @Override
                    public void responsePATCH(Object object) {

                    }

                    @Override
                    public void responseDELETE(Object object) {

                    }
                });
    }

    public void getFavouriteSquares() {
        VolleyManager.getInstance().getFavoriteSquares(InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Log.d(TAG, "responseGET: getFavoriteSquares returned NULL!");
                        } else {
                            InSquareProfile.setFavouriteSquaresList((ArrayList<Square>) object);
                            Log.d(TAG, "onResponse: ho ottenuto FAVS con successo!");
                        }
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Empty - GET Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // Empty - GET Request
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Empty - GET Request
                    }
                });
    }

    public void getRecentSquares() {
        
        VolleyManager.getInstance().getRecentSquares(
                InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Log.d(TAG, "responseGET: getRecentSquares returned NULL!");
                        } else {
                            InSquareProfile.setRecentSquaresList((ArrayList<Square>) object);
                        }
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Empty - GET Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // Empty - GET Request
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Empty - GET Request
                    }
                }
        );
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
        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        TextView recents = (TextView) mDrawerList.getChildAt(1).findViewById(R.id.drawer_counter);
        TextView profile = (TextView) mDrawerList.getChildAt(2).findViewById(R.id.drawer_counter);
        int recCount = 0;
        int profileCount = 0;
        for(String id : sharedPreferences.getAll().keySet()) {
            Log.d(TAG, "checkNotifications: " + id);
            if(InSquareProfile.isOwned(id)) {
                profileCount = profileCount + sharedPreferences.getInt(id, 0);
                Log.d(TAG, "checkNotifications: ");
            } else if(InSquareProfile.isFav(id)) {
                profileCount = profileCount + sharedPreferences.getInt(id, 0);
            }
            if(InSquareProfile.isRecent(id)) {
                recCount = recCount + sharedPreferences.getInt(id, 0);
            }
        }
        if(recCount == 0) {
            recents.setVisibility(View.GONE);
        } else {
            recents.setVisibility(View.VISIBLE);
        }
        if(profileCount == 0) {
            profile.setVisibility(View.GONE);
        } else {
            profile.setVisibility(View.VISIBLE);
        }
        recents.setText(String.valueOf(recCount));
        profile.setText(String.valueOf(profileCount));
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

    private void searchSquares(String query){

        double latitude = mapFragment.getmCurrentLocation().getLatitude();
        double longitude = mapFragment.getmCurrentLocation().getLongitude();
        String userId = InSquareProfile.getUserId();


        VolleyManager.getInstance().searchSquaresByName(query, userId, latitude, longitude,
                new VolleyManager.VolleyResponseListener() {
            @Override
            public void responseGET(Object object) {
                searchItems = (List<Square>) object;
            }

            @Override
            public void responsePOST(Object object) {
                // Lasciare vuoto
            }

            @Override
            public void responsePATCH(Object object) {
                // Lasciare vuoto
            }

            @Override
            public void responseDELETE(Object object) {
                // Lasciare vuoto
            }
        });
    }
}
