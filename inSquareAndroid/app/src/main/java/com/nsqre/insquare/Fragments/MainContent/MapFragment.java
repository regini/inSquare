package com.nsqre.insquare.Fragments.MainContent;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.arlib.floatingsearchview.util.view.BodyTextView;
import com.arlib.floatingsearchview.util.view.IconImageView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.CreateSquareActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.FacebookEventSquare;
import com.nsqre.insquare.Square.FacebookPageSquare;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.SquareSuggestion;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * The fragment shown as soon as the user logs in the app. It shows the map filled with pins representing Squares active, gives the user the possibility to create his own via long press and start chatting with other people
 */
public class MapFragment extends Fragment
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback,
        InSquareProfile.InSquareProfileListener
{


    private static final int PERMISSION_REQUEST_CODE = 1;
    private List<Square> searchResult;
    public static MapFragment instance;
    private SupportMapFragment mapFragment;

    public static final int REQUEST_SQUARE = 0;
    public static final int RESULT_SQUARE = 1;
    public static final int RESULT_SQUARE_FACEBOOK = 2;
    private static final String TAG = "MapFragment";
    private GoogleApiClient mGoogleApiClient;

    private Location mCurrentLocation;
    private LatLng mLastUpdateLocation; // Da dove ho scaricato i pin l'ultima volta
    private String mLastSelectedSquareId = ""; // L'ultima Square selezionata
    private Square mLastSelectedSquare;
    private static final float PIN_DOWNLOAD_RADIUS = 30.0f;
    private static final float PIN_DOWNLOAD_RADIUS_MAX = 1000.0f;
    private final int[] MAP_TYPES = {GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_NONE};
    private int curMapTypeIndex = 1;
    public GoogleMap mGoogleMap;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute in milliseconds

    // Relazione fra Square e Marker sulla mappa
    private HashMap<Marker, Square> squareHashMap;

    private CoordinatorLayout mapCoordinatorLayout;
    private FloatingSearchView mSearchView;

    private Tracker mTracker;
    // Variabili per l'inizializzazione della Chat
    public static final String SQUARE_TAG = "SQUARE_TAG";

    public MapFragment() {
        Log.d(TAG,"new istance");
    }

    public static MapFragment newInstance() {
        if(instance == null) {
            instance = new MapFragment();
        }
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();

        mLastUpdateLocation = new LatLng(0,0);

        squareHashMap = new HashMap<>();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        searchResult = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main_map_full, container, false);

        // Recuperiamo un po' di riferimenti ai layout
        mapCoordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.map_coordinator_layout);

        mSearchView = (FloatingSearchView) v.findViewById(R.id.main_map_floating_search_view);
        setupSearchView();

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mapFragment = SupportMapFragment.newInstance();
        mapFragment.setRetainInstance(true);
        getChildFragmentManager().beginTransaction().replace(R.id.main_map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);

        if(mGoogleApiClient == null)
        {
            Log.d(TAG, "Google API Client is null");
        }else
        {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
//        Log.d(TAG, "onStart: started");
    }

    @Override
    public void onResume() {
        super.onResume();
        mLastSelectedSquareId = "";
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        InSquareProfile.addListener(this);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mMessageReceiver);
        super.onPause();
//        Log.d(TAG, "onPause: I've just paused!");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
//        Log.d(TAG, "onStop: I've just stopped!");
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        Log.d(TAG, "onDetach: removing the fragment from the system!");
        InSquareProfile.removeListener(this);
    }

    /**
     * Broadcast receiver that manages in real time the creation, update and deletion of squares on the map
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("event");
            Log.d("receiver", "Got message: " + message);
            if("creation".equals(intent.getStringExtra("action")) &&
                    !intent.getStringExtra("userId").equals(InSquareProfile.getUserId())) {
                downloadAndInsertPins(PIN_DOWNLOAD_RADIUS_MAX, mGoogleMap.getCameraPosition().target);
            } else if("update".equals(intent.getStringExtra("action"))) {
                downloadAndInsertPins(PIN_DOWNLOAD_RADIUS_MAX, mGoogleMap.getCameraPosition().target);
            } else if("deletion".equals(intent.getStringExtra("action")) &&
                    !intent.getStringExtra("userId").equals(InSquareProfile.getUserId())) {
                for(Marker m : squareHashMap.keySet()) {
                    if(squareHashMap.get(m).getId().equals(intent.getStringExtra("squareId"))) {
                        squareHashMap.remove(m);
                        m.remove();
                            Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_delete_success, Snackbar.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        boolean locationEnabled = false;
        if(getContext() != null) {
            int hasFineLocationPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            List<String> permissions = new ArrayList<>();

            if ( ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION)
                    && ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) )
            {

                Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_no_location_permissions, Snackbar.LENGTH_LONG).show();
            }else {
                if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }else if (hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }else
                {
                    locationEnabled = true;
                }

                if (!permissions.isEmpty()) {
                    requestPermissions(permissions.toArray(new String[permissions.size()]), PERMISSION_REQUEST_CODE);
                }
            }
        }

        if(locationEnabled)
        {
            setupLocation();
        }
    }

    /**
     * Sets on the map the marker in the actual position of the user
     */
    private void setupLocation()
    {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mCurrentLocation == null)
        {
            Log.d(TAG, "Nessuna locazione corrente, ora provvedo");
            LocationManager locationManager = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);

            mCurrentLocation = new Location("");
            mCurrentLocation.setLatitude(0.0d);
            mCurrentLocation.setLongitude(0.0d);

            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if(getContext() != null) {
                        mCurrentLocation = location;
                        initCamera();
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            String GPS = LocationManager.GPS_PROVIDER;
            String NETWORK = LocationManager.NETWORK_PROVIDER;

            if(locationManager.isProviderEnabled(GPS))
            {
                locationManager.requestLocationUpdates(GPS,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                Location location = locationManager.getLastKnownLocation(GPS);
                if(location != null)
                {
                    Log.d(TAG, "Locazione da GPS - Lat: ("
                            + location.getLatitude()
                            + "; Lon: "
                            + location.getLongitude() + ")");
                    mCurrentLocation = location;
                }else
                {
                    Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_no_location, Snackbar.LENGTH_LONG).show();
                }

            }else if(locationManager.isProviderEnabled(NETWORK))
            {
                locationManager.requestLocationUpdates(NETWORK,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                Location location = locationManager.getLastKnownLocation(NETWORK);
                if(location != null)
                {
                    Log.d(TAG, "Locazione da NETWORK -  Lat: ("
                            + location.getLatitude()
                            + "; Lon: "
                            + location.getLongitude() + ")");
                    mCurrentLocation = location;
                }else
                {
                    Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_no_location, Snackbar.LENGTH_LONG).show();
                }
            }else
            {
                Snackbar.make(mapCoordinatorLayout,
                        R.string.map_fragment_disabled_location,
                        Snackbar.LENGTH_LONG)
                        .show();
                return;
            }

            // Se ci sono GPS o Network Provider attivati, richiedi la locazione
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0 , 0, locationListener);
        }else {
            initCamera();
        }
    }

    /**
     * When the user gives the location permissions to the app, it calls setupLocation
     * @see #setupLocation()
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean permissionsGranted = false;
        switch (requestCode)
        {
            case PERMISSION_REQUEST_CODE:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permissions", "Permission Granted: " + permissions[i]);
                        permissionsGranted = true;
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.d("Permissions", "Permission Denied: " + permissions[i]);
                    }
                }

                if(permissionsGranted)
                {
                    setupLocation();
                }else {
                    Snackbar.make(mapCoordinatorLayout,
                            R.string.map_fragment_permissions_error, Snackbar.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /**
     * Manages the list of results from the search bar. From the results shown the user can move the map to the location of the square or he can enter directly in the chat
     */
    private void setupSearchView() {
        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {
            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {
                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else if (newQuery.length() > 2) {
                        mSearchView.showProgress();
                        VolleyManager.getInstance()
                                .searchSquares(newQuery, InSquareProfile.getUserId(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        searchResult = (ArrayList<Square>) object;

                                        List<SquareSuggestion> result = new ArrayList<>();

                                        for (Square s : searchResult) {
                                            result.add(new SquareSuggestion(s));
                                        }

                                        mSearchView.swapSuggestions(result);
                                        mSearchView.hideProgress();
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
                }
        });

        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                for (Square s : searchResult) {
                    if (s.getName().equals(searchSuggestion.getBody())) {

                        LatLng coords = new LatLng(s.getLat(), s.getLon());
                        Marker m = createSquarePin(coords, s.getName(), Integer.parseInt(s.getType()));
                        squareHashMap.put(m, s);
                        m.showInfoWindow();

//                        Location squareLocation = new Location("");
//                        squareLocation.setLongitude(s.getLon());
//                        squareLocation.setLatitude(s.getLat());
                        // TODO ANIMATE
                        setMapInPosition(s.getLat(), s.getLon());
//                        moveToPosition(squareLocation);

                        break;
                    }
                }
            }

            @Override
            public void onSearchAction() {
            }
        });

        mSearchView.setOnMenuItemClickListener(
                new FloatingSearchView.OnMenuItemClickListener() {
                    @Override
                    public void onActionMenuItemSelected(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_location_floating_search:
                                moveToPosition(mCurrentLocation);
                                break;
                            default:
                                break;
                        }
                    }
                }
        );

        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(IconImageView leftIcon, final BodyTextView bodyText, final SearchSuggestion item, final int itemPosition) {
                leftIcon.setImageResource(R.drawable.message_processing_black);
                leftIcon.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        for (Square s : searchResult) {
                            if (s.getName().equals(item.getBody())) {
                                LatLng coords = new LatLng(s.getLat(), s.getLon());
                                Marker m = createSquarePin(coords, s.getName(), Integer.parseInt(s.getType()));
                                squareHashMap.put(m, s);
                                mLastSelectedSquare = s;
                                mLastSelectedSquareId = s.getId();
                                onMarkerClick(m);
                                mSearchView.clearSearchFocus();
                                break;
                            }
                        }
                    }
                });
            }
        });

        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {
                mSearchView.clearSearchFocus();
                Log.d(TAG, "onHomeClicked()");
            }
        });

    }

    /**
     * Moves the map to a given location
     * @param toLocation The location in which the map will be centered
     */
    private void moveToPosition(Location toLocation) {
        if(toLocation != null) {
            CameraPosition position = CameraPosition.builder()
                    .target(new LatLng(toLocation.getLatitude(),
                            toLocation.getLongitude()))
                    .zoom(16f)
                    .bearing(0.0f)
                    .tilt(0.0f)
                    .build();
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    }

    /**
     * Initializes the Google Maps map
     */
    private void initCamera() {

        moveToPosition(mCurrentLocation);

        mGoogleMap.setMapType(MAP_TYPES[curMapTypeIndex]);
        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);

        if(getActivity().getIntent() != null) {
            checkActivityIntent(getActivity().getIntent());
        }
    }

    /**
     * TODO documentare
     */
    public void checkActivityIntent(Intent intent) {
        if(intent != null && intent.getStringExtra("squareId") != null) {
            String squareId = intent.getStringExtra("squareId");
            if(InSquareProfile.isFav(squareId)) {
                for(Square s : InSquareProfile.getFavouriteSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        getActivity().setIntent(null);
                        startChatActivity(s);
                        break;
                    }
                }
            } else if(InSquareProfile.isRecent(squareId)) {
                for(Square s : InSquareProfile.getRecentSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        getActivity().setIntent(null);
                        startChatActivity(s);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Triggers when the user scrolls the map and eventually downloads nearby squares
     * @param cameraPosition The new camera position
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        VisibleRegion vr = mGoogleMap.getProjection().getVisibleRegion();

        double distance = getDistance(mLastUpdateLocation, cameraPosition.target);

        double radius = PIN_DOWNLOAD_RADIUS;

        if(cameraPosition.zoom < 8) {
            radius = PIN_DOWNLOAD_RADIUS*((-5f)*cameraPosition.zoom + 40);
        }

        if(distance > radius*0.9f)
        {
            if(cameraPosition.zoom < 8) //Camera molto elevata
            {
                double rad = getDistance(vr.latLngBounds.southwest, vr.latLngBounds.northeast);
                if(rad > PIN_DOWNLOAD_RADIUS_MAX)
                    rad = PIN_DOWNLOAD_RADIUS_MAX;
                downloadAndInsertPins(rad, cameraPosition.target);
            }else{
                // I'm closer
                downloadAndInsertPins(PIN_DOWNLOAD_RADIUS, cameraPosition.target);
            }
        }
    }

    /**
     * Downloads and draws on the map the closest squares, based on the zoom distance and the actual center of the map
     * @see #getClosestSquares(String, double, double)
     * @param distance The zoom distance
     * @param position The actual center of the map
     */
    private void downloadAndInsertPins(double distance, LatLng position)
    {
        String d = distance + "km";

        if(position != null)
        {
            getClosestSquares(d, position.latitude, position.longitude);
        }
        else
        {
            getClosestSquares(d, 0, 0);
            Log.d(TAG, "downloadAndInsertPins: downloading at the center of the world..?");
        }

        mLastUpdateLocation = position;
    }

    /**
     * Sends a request to the server to download the closest squares based on a position and a radius of research
     * @param distance
     * @param lat Latitude of the actual center of the map
     * @param lon Longitude of the actual center of the map
     */
    private void getClosestSquares(String distance, double lat, double lon) {

        VolleyManager.getInstance().getClosestSquares(distance, lat, lon,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Log.d(TAG, "responseGET ClosestSquares: la risposta era null!");
                        } else {
                            String jsonResponse = (String) object;
                            new MapFiller().execute(jsonResponse);
                        }
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto -- GET Request
                    }
                });
    }

    /**
     * Calculates the distance between 2 locations
     * @param southwest location 1
     * @param frnd_latlong location 2
     * @return the distance in meters
     */
    private float getDistance(LatLng southwest, LatLng frnd_latlong){
        Location l1=new Location("Southwest");
        l1.setLatitude(southwest.latitude);
        l1.setLongitude(southwest.longitude);

        Location l2=new Location("Northeast");
        l2.setLatitude(frnd_latlong.latitude);
        l2.setLongitude(frnd_latlong.longitude);

        float distance=l1.distanceTo(l2);

        // Converti da metri in km
        distance=distance/1000.0f;

        return distance;
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        startChatActivity(squareHashMap.get(marker));
    }

    /**
     * Opens the ChatActivity relative to a given Square
     * @param s A square
     * @see ChatActivity
     */
    public void startChatActivity(Square s) {

        // [START PinButton_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("BottomNavActivity")
                .setAction("PinButton")
                .build());
        // [END PinButton_event]

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        SharedPreferences messagePreferences = getActivity().getSharedPreferences(s.getId(), Context.MODE_PRIVATE);
        messagePreferences.edit().clear().apply();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("NOTIFICATION_MAP", Context.MODE_PRIVATE);
        if(sharedPreferences.contains(s.getId())) {
            sharedPreferences.edit().remove(s.getId()).apply();
            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount", 0) - 1).apply();
        }

        intent.putExtra(SQUARE_TAG, s);
        intent.putExtra(BottomNavActivity.INITIALS_TAG, s.getInitials());

        int max = BottomNavActivity.backgroundColors.length;
        int randomBackgroundIndex = (new Random()).nextInt(max);
        intent.putExtra(BottomNavActivity.INITIALS_COLOR_TAG, BottomNavActivity.backgroundColors[randomBackgroundIndex]);

        startActivity(intent);
    }

    /**
     * Creates and adds a marker on the map
     * @param pos location of the marker
     * @param name name to be shown
     * @param type type of the square
     * @return The marker just created
     * @see #correctMapPin(String, int)
     */
    private Marker createSquarePin(LatLng pos, String name, int type) {

        MarkerOptions options;

        if(isAdded()) {
            options = new MarkerOptions().position(pos);
            options.title(name);
            BitmapDescriptor bd;
            switch (type) {
                default:
                case 0:
                    bd = correctMapPin("pin_rosso", 3);
                    break;
                case 1:
                    bd = correctMapPin("pin_viola", 3);
                    break;
                case 2:
                    bd = correctMapPin("pin_verde", 3);
                    break;
            }

            options.icon(bd);
        }else
        {
            options = new MarkerOptions().position(pos);
            options.title("A Square");
            options.icon(BitmapDescriptorFactory.defaultMarker());
        }
        return mGoogleMap.addMarker(options);
    }

    /**
     * Defines the size and color of a pin
     * @param name name of the pin
     * @param scaleSize scale size
     * @return an object that contains the informations elaborated
     */
    private BitmapDescriptor correctMapPin(final String name, final int scaleSize)
    {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(name, "drawable", getContext().getPackageName()));
        Bitmap correctSizeBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/scaleSize, bitmap.getHeight()/scaleSize, false);

        return BitmapDescriptorFactory.fromBitmap(correctSizeBitmap);
    }

    @Override
    public void onMapClick(final LatLng latLng) {
        mLastSelectedSquareId = "";
        Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_long_press_hint, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Manages the kind of request that will be done to the server to create a new Square
     * @param resultCode The code of which kind of square is being created
     * @param data The package of data of the new Square
     */
    public void handleSquareCreation(int resultCode, Intent data)
    {
        String name = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_NAME);
        String description = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_DESCRIPTION);
        String latitude = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_LATITUDE);
        String longitude = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_LONGITUDE);
        String expireTime = data.getStringExtra(CreateSquareActivity.RESULT_EXPIRE_TIME);

        switch (resultCode)
        {
            case RESULT_SQUARE:
                Log.d(TAG, "onActivityResult: trying to create a normal square!");

                VolleyManager.getInstance().postSquare(
                        name,
                        description,
                        latitude,
                        longitude,
                        InSquareProfile.getUserId(),
                        new VolleyManager.VolleyResponseListener() {
                            @Override
                            public void responseGET(Object object) {
                                // POST REQUEST
                            }

                            @Override
                            public void responsePOST(Object object) {
                                if (object == null) {
                                    Log.d(TAG, "responsePOST Square: non sono riuscito a creare la square..!");
                                    Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_create_fail, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Square postedSquare = (Square) object;
                                    InSquareProfile.addFav(postedSquare);
                                    InSquareProfile.addOwned(postedSquare);

                                    LatLng finalPosition = new LatLng(postedSquare.getLat(), postedSquare.getLon());

                                    Marker marker = createSquarePin( finalPosition , postedSquare.getName(), Integer.parseInt(postedSquare.getType()));
                                    squareHashMap.put(marker, postedSquare);
                                    marker.setVisible(true);
                                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(finalPosition), 400, null);
                                    Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_create_success, Snackbar.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void responsePATCH(Object object) {
                                // POST REQUEST
                            }

                            @Override
                            public void responseDELETE(Object object) {
                                // POST REQUEST
                            }
                        }
                );
                break;
            case RESULT_SQUARE_FACEBOOK:
                Log.d(TAG, "onActivityResult: trying to create from facebook!");
                String facebookId = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_FACEBOOK_ID);
                String type = data.getStringExtra(CreateSquareActivity.RESULT_SQUARE_TYPE);

                VolleyManager.getInstance().postFacebookSquare(
                        name,
                        description,
                        latitude,
                        longitude,
                        InSquareProfile.getUserId(),
                        type,
                        facebookId,
                        expireTime,
                        new VolleyManager.VolleyResponseListener() {
                            @Override
                            public void responseGET(Object object) {
                                // POST REQUEST
                            }

                            @Override
                            public void responsePOST(Object object) {
                                if (object == null) {
                                    Log.d(TAG, "responsePOST Square: non sono riuscito a creare la square..!");
                                    Snackbar.make(mapCoordinatorLayout, R.string.map_fragment_create_error, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Square postedSquare = (Square) object;
                                    InSquareProfile.addFav(postedSquare);
                                    InSquareProfile.addOwned(postedSquare);

                                    LatLng finalPosition = new LatLng(postedSquare.getLat(), postedSquare.getLon());

                                    Marker marker = createSquarePin(finalPosition,
                                            postedSquare.getName(), Integer.parseInt(postedSquare.getType()));
                                    squareHashMap.put(marker, postedSquare);
                                    marker.setVisible(true);
                                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(finalPosition), 400, null);
                                    Snackbar.make(mapCoordinatorLayout, "Square creata con successo!", Snackbar.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void responsePATCH(Object object) {
                                // POST REQUEST
                            }

                            @Override
                            public void responseDELETE(Object object) {
                                // POST REQUEST
                            }
                        }
                );
                break;
        }
    }

    /**
     * Listens to a longpress on the map and shows the CreateSquareActivity
     * @param latLng the position of the longpress
     */
    @Override
    public void onMapLongClick(final LatLng latLng) {
        String latitude = String.valueOf(latLng.latitude);
        String longitude = String.valueOf(latLng.longitude);
        Intent intent = new Intent(getContext(), CreateSquareActivity.class);
        Bundle extras = new Bundle();
        extras.putString("latitude", latitude);
        extras.putString("longitude", longitude);
        intent.putExtras(extras);
        getActivity().startActivityForResult(intent, REQUEST_SQUARE);
        /*mGoogleMap.snapshot(
                new GoogleMap.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap bitmap) {

                        (new DialogHandler()).handleCreateOptions(getContext(), TAG, latLng, bitmap);
                    }
                }
        );*/
    }

    /**
     * Triggered when the Google Map is ready. Sets up the pins on the map
     * @see #downloadAndInsertPins(double, LatLng)
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(mapFragment != null)
        {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            if(locationManager != null && googleMap != null)
            {
                mGoogleMap = googleMap;
                if(mGoogleMap != null && squareHashMap.size() > 0)
                {
                    // Riempi la mappa
                    HashMap<String, Square> squarePins = new HashMap<>();
                    for(Square s : squareHashMap.values())
                    {
                        squarePins.put(s.getId(), s);
                    }
                    squareHashMap.clear();
                    mGoogleMap.clear();
                    for(Square closeSquare : squarePins.values())
                    {
                        LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                        Marker m = createSquarePin(coords, closeSquare.getName(), Integer.parseInt(closeSquare.getType()));
                        squareHashMap.put(m, closeSquare);
                    }
                    Log.d(TAG, "onResume: map has been refilled!");
                    // Fine riempimento


                    // Download dei nuovi pin
                    downloadAndInsertPins(PIN_DOWNLOAD_RADIUS_MAX, mGoogleMap.getCameraPosition().target);
                }
            }
        }

        initListeners();
    }

    /**
     * Initializes all the necessary listeners for the MapFragment
     */
    private void initListeners() {
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
        mGoogleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
    }

    /**
     * Shows the info windows for a square when the user taps on its marker
     * @param marker the marker of the square
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        final Square currentSquare = squareHashMap.get(marker);

        marker.showInfoWindow();

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()),
                400, // Tempo di spostamento in ms
                null); // callback
        String text = marker.getTitle();
        if(currentSquare.getId().equals(mLastSelectedSquareId))
        {
            Log.d(TAG, "onMarkerClick: Clicked twice?");
            startChatActivity(currentSquare);
        }else
        {
            mLastSelectedSquare = currentSquare;
            mLastSelectedSquareId = currentSquare.getId();
        }
        
        return true;
    }

    /**
     * unused
     */
    public void favouriteSquare(final int method, final Square square) {
        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        } else {
                            InSquareProfile.addFav(square);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responseDELETE - non sono riuscito ad rimuovere il fav " + square.toString());
                        } else {
                            InSquareProfile.removeFav(square.getId());
                        }
                    }
                });
    }

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged: Owned changed!");
    }

    @Override
    public void onFavChanged() {
        Log.d(TAG, "onFavChanged: Favs changed!");
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged: Recent changed!");
    }

    /**
     * Manages the list of pins to draw on the map
     */
    public class MapFiller extends AsyncTask<String,Void,HashMap<String,Square>> {

        @Override
        protected HashMap<String, Square> doInBackground(String... params) {
            if(!isAdded()) {
                return null;
            }

            // @VolleyManager ha già la deserializzazione integrata
            List<Square> squares = VolleyManager.getInstance().deserializeSquares(String.valueOf(params[0]));

            HashMap<String, Square> squarePins = new HashMap<>();
            for(Square s: squares)
            {
                squarePins.put(s.getId(), s);
            }
            for(Square closeSquare : squarePins.values()) {
                if(squareHashMap.containsValue(closeSquare)) {
                    for(Marker m : squareHashMap.keySet()) {
                        if(closeSquare.equals(squareHashMap.get(m))) {
                            squareHashMap.put(m, closeSquare);
                            break;
                        }
                    }
                }
            }
            return squarePins;
        }
        @Override
        protected void onPostExecute(HashMap<String, Square> squarePins) {
            if(!isAdded()) {
                return;
            }
            for(Square closeSquare: squarePins.values()) {
                if(!squareHashMap.containsValue(closeSquare)) {
                    LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                    Marker m = createSquarePin(coords, closeSquare.getName(), Integer.parseInt(closeSquare.getType()));
                    squareHashMap.put(m, closeSquare);
                }
            }
        }
    }

    public void setMapInPosition(double lat, double lon) {
        Log.d(TAG, "setMapInPosition: lat:" + lat + " lon:" + lon);
        LatLng latlng = new LatLng(lat, lon);
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latlng),
                400, // Tempo di spostamento in ms
                null); // callback
    }


    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    /**
     * Manages the data shown in the info window
     */
    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private static final String TAG = "MarkerInfoWindowAdapter";

        // Componenti della View
        private TextView squareName;
        private TextView squareInitials;
        private TextView squareActivity;
        private LinearLayout facebookLikes;
        private TextView likeNumber;
        private LinearLayout facebookTime;
        private TextView timeText;

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.info_window_layout, null);

            squareName = (TextView) view.findViewById(R.id.info_window_square_name);
            squareInitials = (TextView) view.findViewById(R.id.info_window_square_initials);
            squareActivity = (TextView) view.findViewById(R.id.info_window_square_last_activity);
            facebookLikes = (LinearLayout) view.findViewById(R.id.info_window_facebook_likes);
            facebookTime = (LinearLayout) view.findViewById(R.id.info_window_facebook_time);
            likeNumber = (TextView) view.findViewById(R.id.info_window_like_number);
            timeText = (TextView) view.findViewById(R.id.info_window_time_text);

            Square s = squareHashMap.get(marker);
            setupSquare(s);

            return view;
        }

        private void setupSquare(final Square square)
        {
            squareName.setText(square.getName());
            squareInitials.setText(square.getInitials());
            squareActivity.setText(getContext().getString(R.string.square_last_message_incipit) + square.formatTime());

            if(square.isFacebookPage)
            {
                FacebookPageSquare fbPage = (FacebookPageSquare) square;
                // TODO remove print
                Log.d(TAG, "setupSquare: " + fbPage.toString());
                if(!fbPage.likeCount.isEmpty()) {
                    likeNumber.setText(fbPage.likeCount);
                    facebookLikes.setVisibility(View.VISIBLE);
                }
            }else if(square.isFacebookEvent)
            {
                squareActivity.setVisibility(View.GONE);
                FacebookEventSquare fbEvent = (FacebookEventSquare) square;
                // TODO remove print
                Log.d(TAG, "setupSquare: " + fbEvent.toString());
                if(!fbEvent.time.isEmpty()) {
                    facebookTime.setVisibility(View.VISIBLE);
                    timeText.setText(fbEvent.time);
                }
            }
        }
    }
}
