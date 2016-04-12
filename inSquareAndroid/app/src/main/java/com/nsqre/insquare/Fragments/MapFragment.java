package com.nsqre.insquare.Fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.SquareSuggestion;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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


    private List<Square> searchResult;
    public static MapFragment instance;
    private SupportMapFragment mapFragment;

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

    private static final int REQUEST_FINE_LOCATION = 0;
    private static final int REQUEST_COARSE_LOCATION = 1;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute in milliseconds
    private static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

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
            Snackbar.make(mapCoordinatorLayout, "Google API Client is null", Snackbar.LENGTH_SHORT).show();
        }

        mGoogleApiClient.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started");
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
        Log.d(TAG, "onPause: I've just paused!");
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG, "onStop: I've just stopped!");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach: removing the fragment from the system!");
        InSquareProfile.removeListener(this);
    }

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
                            Snackbar.make(mapCoordinatorLayout, "La square è stata eliminata", Snackbar.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            this.requestPermissions(PERMISSIONS,
                    REQUEST_COARSE_LOCATION);
            this.requestPermissions(PERMISSIONS,
                    REQUEST_FINE_LOCATION);
            return;
        }
        setupLocation();
    }

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
                    Snackbar.make(mapCoordinatorLayout, "Non ho modo di prendere la locazione corrente!", Snackbar.LENGTH_LONG).show();
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
                    Snackbar.make(mapCoordinatorLayout, "Non ho modo di prendere la locazione corrente!", Snackbar.LENGTH_LONG).show();
                }
            }else
            {
                Snackbar.make(mapCoordinatorLayout,
                        "Geolocalizzazione disattivata..?",
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

    // Permessi di locazioni richiesti
    // Gestione di ritorno dalla richiesta
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_COARSE_LOCATION:
            case REQUEST_FINE_LOCATION:
                if(grantResults.length>0)
                {
                    setupLocation();
                }
                else{
                    Snackbar.make(mapCoordinatorLayout,
                            "Senza permessi non posso funzionare!", Snackbar.LENGTH_SHORT).show();
                }

                return;
        }
    }


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
                        Marker m = createSquarePin(coords, s.getName());
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
                                Marker m = createSquarePin(coords, s.getName());
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

    private void moveToPosition(Location toLocation) {
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(toLocation.getLatitude(),
                        toLocation.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
    }

    private void initCamera() {

        moveToPosition(mCurrentLocation);

        mGoogleMap.setMapType(MAP_TYPES[curMapTypeIndex]);
        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);

        checkActivityIntent(getActivity().getIntent());
    }

    public void checkActivityIntent(Intent intent) {
        if(intent.getStringExtra("squareId") != null) {
            String squareId = intent.getStringExtra("squareId");
            if(InSquareProfile.isFav(squareId)) {
                for(Square s : InSquareProfile.getFavouriteSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        intent.getExtras().clear();
                        startChatActivity(s);
                        break;
                    }
                }
            } else if(InSquareProfile.isRecent(squareId)) {
                for(Square s : InSquareProfile.getRecentSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        intent.getExtras().clear();
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
            Log.d(TAG, "Downloading From: " + cameraPosition.target.toString());
        }
    }

    private void downloadAndInsertPins(double distance, LatLng position)
    {
        String d = distance + "km";

        //TODO check sul centro del mondo
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

    // Con due locazioni restituisce il valore in km di distanza
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
        intent.putExtra(BottomNavActivity.INITIALS_TAG, BottomNavActivity.setupInitials(s.getName()));

        int max = BottomNavActivity.backgroundColors.length;
        int randomBackgroundIndex = (new Random()).nextInt(max);
        intent.putExtra(BottomNavActivity.INITIALS_COLOR_TAG, BottomNavActivity.backgroundColors[randomBackgroundIndex]);

        startActivity(intent);
    }

    // LatLng | Name |
    private Marker createSquarePin(LatLng pos, String name) {

        MarkerOptions options = new MarkerOptions().position(pos);
        options.title(name);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.nsqre_map_pin));
        return mGoogleMap.addMarker(options);
    }

    @Override
    public void onMapClick(final LatLng latLng) {
        hideBottomSheet();
    }

    private void hideBottomSheet() {
        mLastSelectedSquareId = "";
    }

    private void createSquarePostRequest(final String squareName,
                                         final String squareDescr,
                                         final String latitude,
                                         final String longitude,
                                         final Marker marker,
                                         final String ownerId) {

        VolleyManager.getInstance().postSquare(
                squareName,
                squareDescr,
                latitude,
                longitude,
                ownerId,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto -- POST Request
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            Log.d(TAG, "responsePOST Square: non sono riuscito a creare la square..!");
                        } else {
                            Square postedSquare = (Square) object;
                            InSquareProfile.addFav(postedSquare);
                            InSquareProfile.addOwned(postedSquare);
                            squareHashMap.put(marker, postedSquare);
                            marker.setVisible(true);
                            Snackbar.make(mapCoordinatorLayout, "Square creata con successo!", Snackbar.LENGTH_SHORT).show();
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

    @Override
    public void onMapLongClick(final LatLng latLng) {
        final String lat = Double.toString(latLng.latitude);
        final String lon = Double.toString(latLng.longitude);

        final Dialog mDialog = new Dialog(getContext());
        mDialog.setContentView(R.layout.dialog_crea_square);
        mDialog.setTitle("Crea una Square");
        mDialog.setCancelable(true);
        mDialog.show();

        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        final EditText usernameEditText = (EditText) mDialog.findViewById(R.id.et_square);
        final EditText descriptionEditText = (EditText) mDialog.findViewById((R.id.descr_square));
        TextInputLayout textInputLayout = (TextInputLayout) mDialog.findViewById(R.id.input_layout_crea_square);
        Button crea = (Button) mDialog.findViewById(R.id.button_crea);
        crea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String squareName = usernameEditText.getText().toString().trim();
                String squareDescr = descriptionEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(squareName)) {
                    Marker m = createSquarePin(latLng, squareName);
                    m.setVisible(false);
                    // Richiesta Volley POST per la creazione di piazze
                    // Si occupa anche di creare e aggiungere la nuova Square al HashMap
                    String ownerId = InSquareProfile.getUserId();
                    Snackbar.make(mapCoordinatorLayout, "Stiamo creando la square", Snackbar.LENGTH_SHORT).show();
                    createSquarePostRequest(squareName, squareDescr, lat, lon, m, ownerId);
                    mDialog.dismiss();
                }
            }
        });
    }

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
                        Marker m = createSquarePin(coords, closeSquare.getName());
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

    private void initListeners() {
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
        mGoogleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
    }

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
        if(InSquareProfile.isFav(mLastSelectedSquareId)) {
            for(Square s : InSquareProfile.getFavouriteSquaresList()) {
                if(mLastSelectedSquareId.equals(s.getId())) {
                    break;
                }
            }
        }
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged: Recent changed!");
    }

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
                    Marker m = createSquarePin(coords, closeSquare.getName());
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

    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private static final String TAG = "MarkerInfoWindowAdapter";

        // Componenti della View
        private ImageView heartButton;
        private ImageView squareIcon;
        private TextView squareName;
        private TextView squareActivity;

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.info_window_layout, null);

            squareName = (TextView) view.findViewById(R.id.info_window_square_name);
            squareActivity = (TextView) view.findViewById(R.id.info_window_square_last_activity);
            heartButton = (ImageView) view.findViewById(R.id.info_window_heart_button);

            Square s = squareHashMap.get(marker);
            setupSquare(s);

            return view;
        }

        private void setupSquare(final Square square)
        {
            squareName.setText(square.getName());
            squareActivity.setText(square.formatTime());

            if(InSquareProfile.isFav(square.getId()))
            {
                heartButton.setImageResource(R.drawable.like_filled_96);
            }
        }
    }
}
