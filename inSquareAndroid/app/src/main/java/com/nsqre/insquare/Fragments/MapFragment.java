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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Square.SquareDeserializer;
import com.nsqre.insquare.Square.SquareState;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static MapFragment instance;
    private SupportMapFragment mapFragment;
    public static final int SQUARE_DOWNLOAD_LIMIT = 1000;

    private static final String TAG = "MapFragment";
    private GoogleApiClient mGoogleApiClient;
    public Location mCurrentLocation;
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
    private static String[] PERMISSIONS =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    // Relazione fra Square e Marker sulla mappa
    private HashMap<Marker, Square> squareHashMap;

    private CoordinatorLayout mapCoordinatorLayout;

    private View bottomSheetSeparator;
    private TextView bottomSheetSquareName;
    private ImageButton bottomSheetButton;
    private LinearLayout bottomSheetUpperLinearLayout;
    private LinearLayout bottomSheetLowerLinearLayout;
    private TextView bottomSheetLowerFavs;
    private TextView bottomSheetLowerViews;
    private TextView bottomSheetLowerDescription;
    private View bottomSheetLowerState;


//    private RecyclerView bottomSheetList;
    private TextView bottomSheetSquareActivity;
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
//        rootMainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main_map_full, container, false);

        // Recuperiamo un po' di riferimenti ai layout
        mapCoordinatorLayout = (CoordinatorLayout) v.findViewById(R.id.map_coordinator_layout);

        bottomSheetButton = (ImageButton) v.findViewById(R.id.bottom_sheet_button);
        bottomSheetButton.setVisibility(View.GONE);

        bottomSheetSeparator = v.findViewById(R.id.bottom_sheet_separator);
        bottomSheetSeparator.setVisibility(View.GONE);

        bottomSheetSquareName = (TextView) v.findViewById(R.id.bottom_sheet_square_name);

        bottomSheetSquareActivity = (TextView) v.findViewById(R.id.bottom_sheet_last_activity);
        bottomSheetSquareActivity.setVisibility(View.GONE);

        bottomSheetUpperLinearLayout = (LinearLayout) v.findViewById(R.id.bottom_sheet_upper_ll);

        bottomSheetLowerLinearLayout = (LinearLayout) v.findViewById(R.id.bottom_sheet_lower_ll);
        bottomSheetLowerLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Click listener che intercetta i click sul bottom sliding drawer
                Log.d(TAG, "onClick: Clicky");
            }
        });

        bottomSheetLowerDescription = (TextView) v.findViewById(R.id.bottom_sheet_square_description);
        bottomSheetLowerViews = (TextView) v.findViewById(R.id.bottom_sheet_square_views);
        bottomSheetLowerFavs = (TextView) v.findViewById(R.id.bottom_sheet_square_favourites);
        bottomSheetLowerState = v.findViewById(R.id.bottom_sheet_square_state);
        bottomSheetLowerState.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white));


//        FrameLayout bottomSheet = (FrameLayout) bottomSheetButton.getParent().getParent().getParent();
//        BottomSheetBehavior bsb = BottomSheetBehavior.from(bottomSheet);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started");
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
            Toast.makeText(getContext(), "Google API Client is null", Toast.LENGTH_SHORT).show();
        }

        mGoogleApiClient.connect();
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
        Log.d(TAG, "onDetach: removing the fraggment from the system!");
        InSquareProfile.removeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLastSelectedSquareId = "";
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        InSquareProfile.addListener(this);
        if(mGoogleMap != null && squareHashMap.size() > 0)
        {
            mGoogleMap.clear();

            // Riempi la mappa
            HashMap<String, Square> squarePins = new HashMap<>();
            for(Square s : squareHashMap.values())
            {
                squarePins.put(s.getId(), s);
            }
            squareHashMap.clear();
            for(Square closeSquare : squarePins.values())
            {
                LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                Marker m = createSquarePin(coords, closeSquare.getName());
                squareHashMap.put(m, closeSquare);
            }
            Log.d(TAG, "onResume: map has been refilled!");
            // Fine refill

            downloadAndInsertPins(PIN_DOWNLOAD_RADIUS_MAX, mGoogleMap.getCameraPosition().target);
            if(bottomSheetSeparator.getVisibility() == View.VISIBLE) {
                bottomSheetUpperLinearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startChatActivity(mLastSelectedSquare);
                    }
                });
            }
        }
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
            } else if("update".equals(intent.getStringExtra("action")) || "deletion".equals(intent.getStringExtra("action"))) {
                downloadAndInsertPins(PIN_DOWNLOAD_RADIUS_MAX, mGoogleMap.getCameraPosition().target);
            }
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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

            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if(getContext() != null) {
                        mCurrentLocation = location;
                        initCamera(mCurrentLocation);
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
                    Toast.makeText(getContext(), "Non ho modo di prendere la locazione corrente!", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getContext(), "Non ho modo di prendere la locazione corrente!", Toast.LENGTH_LONG).show();
                }
            }else
            {
                Toast.makeText(getContext(),
                        "Geolocalizzazione disattivata..?",
                        Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // Se ci sono GPS o Network Provider attivati, richiedi la locazione
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0 , 0, locationListener);
        }else {
            initCamera(mCurrentLocation);
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
//                    Toast.makeText(getContext(), "Permessi ottenuti!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getContext(),
                            "Senza permessi non posso funzionare!", Toast.LENGTH_SHORT).show();
                }

                return;
        }
    }

    private void initCamera(Location mCurrentLocation) {

        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        mGoogleMap.setMapType(MAP_TYPES[curMapTypeIndex]);
        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setMyLocationEnabled(true);

        if(getActivity().getIntent().getStringExtra("squareId") != null) {
            String squareId = getActivity().getIntent().getStringExtra("squareId");
            if(InSquareProfile.isFav(squareId)) {
                for(Square s : InSquareProfile.getFavouriteSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        startChatActivity(s);
                        break;
                    }
                }
            } else if(InSquareProfile.isRecent(squareId)) {
                for(Square s : InSquareProfile.getRecentSquaresList()) {
                    if(squareId.equals(s.getId())) {
                        startChatActivity(s);
                        break;
                    }
                }
            }
            getActivity().getIntent().getExtras().clear();
        }

        // mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
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
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = "http://recapp-insquare.rhcloud.com/squares?";
        url += "distance=" + distance;
        url += "&lat=" + lat;
        url += "&lon=" + lon;

        Log.d(TAG, "getClosestSquares: " + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        new MapFiller().execute(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "GetClosestSquares " + error.toString());
            }
        });
        queue.add(stringRequest);
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

    private void startChatActivity(Square s) {

        // [START PinButton_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("MapActivity")
                .setAction("PinButton")
                .build());
        // [END PinButton_event]

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        SharedPreferences messagePreferences = getActivity().getSharedPreferences(s.getId(), Context.MODE_PRIVATE);
        messagePreferences.edit().clear().apply();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("NOTIFICATION_MAP", Context.MODE_PRIVATE);
        if(sharedPreferences.contains(s.getId())) {
            sharedPreferences.edit().remove(s.getId()).apply();
            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount",0) - 1).apply();
            MapActivity rootActivity = (MapActivity) getActivity();
            rootActivity.checkNotifications();
        }
        intent.putExtra(SQUARE_TAG, s);
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

    }

    private void createSquarePostRequest(final String squareName,
                                         final String squareDescr,
                                         final String latitude,
                                         final String longitude,
                                         final Marker marker,
                                         final String ownerId) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = "http://recapp-insquare.rhcloud.com/squares";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Create Square response: " + response);
                        GsonBuilder b = new GsonBuilder();
                        // SquareDeserializer specifica come popolare l'oggetto Message fromJson
                        b.registerTypeAdapter(Square.class, new SquareDeserializer(getResources().getConfiguration().locale));
                        Gson gson = b.create();
                        Square s = gson.fromJson(response, Square.class);
                        squareHashMap.put(marker, s);
                        marker.setVisible(true);
                        Snackbar.make(mapCoordinatorLayout, "Square creata!", Snackbar.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("CreateSquare Response", error.toString());
            }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", squareName);
                params.put("description", squareDescr);
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("ownerId",ownerId);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void descriptionDialog(String oldText)
    {
        final Dialog mDialog = new Dialog(getContext());
        mDialog.setContentView(R.layout.dialog_description);
//        mDialog.setTitle("Modifica la descrizione");
        mDialog.setCancelable(true);
        mDialog.show();

        final EditText descriptionEditText = (EditText) mDialog.findViewById(R.id.et_dialog_description);
        if(!oldText.isEmpty())
        {
            ((TextInputLayout) descriptionEditText.getParent()).setHint("Modifica la descrizione");
            descriptionEditText.setText("");
            descriptionEditText.setText(oldText);
        }
        final Button okButton = (Button) mDialog.findViewById(R.id.button_dialog_description);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = descriptionEditText.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(MapFragment.this.getContext(), "Devi inserire una descrizione!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "onClick: stai tentando di inserire la descrizione:\n" + text);
                // TODO VolleyManager request per la PATCH descrizione
                VolleyManager.getInstance().patchDescription(text, mLastSelectedSquareId, new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Lasciare vuoto
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Lasciare vuoto

                    }

                    @Override
                    public void responsePATCH(Object object) {
                        boolean response = (boolean) object;
                        if (response) {
                            // Tutto OK!
                            Log.d(TAG, "responsePATCH: sono riuscito a patchare correttamente!");
                            bottomSheetLowerDescription.setText(text);
                            Snackbar.make(mapCoordinatorLayout, "Descrizione modificata!", Snackbar.LENGTH_SHORT).show();
                            mDialog.dismiss();
                        }
                    }
                });
            }
        });
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
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

        marker.showInfoWindow();

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()),
                400, // Tempo di spostamento in ms
                null); // callback
        final Square currentSquare = squareHashMap.get(marker);
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
        
        Log.d(TAG, currentSquare.getId() + " " + currentSquare.getName());


        // Parte superiore del drawer
        bottomSheetSquareName.setText(text);
        bottomSheetSquareActivity.setVisibility(View.VISIBLE);
        bottomSheetSquareActivity.setText(currentSquare.formatTime());

        // Controllo sulla lista dell'utente
        if(InSquareProfile.isFav(currentSquare.getId()))
            bottomSheetButton.setImageResource(R.drawable.heart_black);
        else
            bottomSheetButton.setImageResource(R.drawable.heart_border_black);

        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int method;

                if(InSquareProfile.isFav(currentSquare.getId()))
                {
                    method = Request.Method.DELETE;
                }else
                {
                    method = Request.Method.POST;
                }

                favouriteSquare(method,currentSquare);
            }
        });
        bottomSheetButton.setVisibility(View.VISIBLE);
        bottomSheetSeparator.setVisibility(View.VISIBLE);

        bottomSheetUpperLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatActivity(currentSquare);
            }
        });
        // ===== Fine Parte Superiore del Drawer

        // Parte Bassa del Drawer
        ((LinearLayout)bottomSheetLowerFavs.getParent()).setVisibility(View.VISIBLE);
        bottomSheetLowerFavs.setText("Seguita da " + currentSquare.getFavouredBy() + " persone");
        ((LinearLayout)bottomSheetLowerViews.getParent()).setVisibility(View.VISIBLE);
        bottomSheetLowerViews.setText("Vista " + currentSquare.getViews() + " volte");
        final String d = currentSquare.getDescription().trim();
        String userid = InSquareProfile.getUserId();
        if( currentSquare.getOwnerId().equals(InSquareProfile.getUserId()) )
        {
            bottomSheetLowerDescription.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Visualizza un dialog per inserire la descrizione
                    descriptionDialog(bottomSheetLowerDescription.getText().toString().trim());
                }
            });
            bottomSheetLowerDescription.setText(d);
            ((LinearLayout)bottomSheetLowerDescription.getParent()).setVisibility(View.VISIBLE);
        }
        else if(d.isEmpty())
        {
            ((LinearLayout)bottomSheetLowerDescription.getParent()).setVisibility(View.GONE);
        }else
        {
            ((LinearLayout)bottomSheetLowerDescription.getParent()).setVisibility(View.VISIBLE);
            bottomSheetLowerDescription.setText(d.trim());
            bottomSheetLowerDescription.setOnClickListener(null);
        }

        SquareState currentState = currentSquare.getSquareState();
        int stateColor;
        switch(currentState)
        {
            default:
            case ASLEEP:
                stateColor = ContextCompat.getColor(getContext(), R.color.state_asleep);
                break;
            case AWOKEN:
                stateColor = ContextCompat.getColor(getContext(), R.color.state_awoken);
                break;
            case CAFFEINATED:
                stateColor = ContextCompat.getColor(getContext(), R.color.state_caffeinated);
                break;
        }
        ((LinearLayout)bottomSheetLowerState.getParent()).setVisibility(View.VISIBLE);
        bottomSheetLowerState.setBackgroundColor(stateColor);
        // ===== Fine Parte Bassa del Drawer

        return true;
    }

    public void favouriteSquare(final int method, final Square square) {
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = "http://recapp-insquare.rhcloud.com/favouritesquares?";
        url += "squareId=" + square.getId();
        url += "&userId=" + InSquareProfile.getUserId();
        Log.d(TAG, "favouriteSquare: " + url);
        StringRequest postRequest = new StringRequest(method, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        switch (method)
                        {
                            case Request.Method.DELETE:
                                bottomSheetButton.setImageResource(R.drawable.heart_border_black);
                                InSquareProfile.removeFav(square.getId());
//                                InSquareProfile.favouriteSquaresList.remove(square);
                                break;
                            case Request.Method.POST:
                                bottomSheetButton.setImageResource(R.drawable.heart_black);
                                InSquareProfile.addFav(square);
//                                InSquareProfile.favouriteSquaresList.add(square);
                                break;
                        }
//                        Log.d(TAG, "FAVOURITE response => " + response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "FAVOURITE error => "+ error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    private void updateBottomSheet(Square s) {
        if(bottomSheetSeparator.getVisibility() == View.VISIBLE) {
            bottomSheetSquareActivity.setText(s.formatTime());
            SquareState currentState = s.getSquareState();
            int stateColor;
            switch(currentState)
            {
                default:
                case ASLEEP:
                    stateColor = ContextCompat.getColor(getContext(), R.color.state_asleep);
                    break;
                case AWOKEN:
                    stateColor = ContextCompat.getColor(getContext(), R.color.state_awoken);
                    break;
                case CAFFEINATED:
                    stateColor = ContextCompat.getColor(getContext(), R.color.state_caffeinated);
                    break;
            }
            bottomSheetLowerState.setBackgroundColor(stateColor);
            bottomSheetLowerFavs.setText("Seguita da " + s.getFavouredBy() + " persone");
            bottomSheetLowerViews.setText("Vista " + s.getViews() + " volte");
        }
    }

    @Override
    public void onOwnedChanged() {
        Log.d(TAG, "onOwnedChanged: Owned changed!");
        if(InSquareProfile.isOwned(mLastSelectedSquareId)) {
            for(Square s : InSquareProfile.getOwnedSquaresList()) {
                if(mLastSelectedSquareId.equals(s.getId())) {
                    updateBottomSheet(s);
                    break;
                }
            }
        }
    }

    @Override
    public void onFavChanged() {
        Log.d(TAG, "onFavChanged: Favs changed!");
        if(InSquareProfile.isFav(mLastSelectedSquareId))
        {
            bottomSheetButton.setImageResource(R.drawable.heart_black);
            for(Square s : InSquareProfile.getFavouriteSquaresList()) {
                if(mLastSelectedSquareId.equals(s.getId())) {
                    updateBottomSheet(s);
                    break;
                }
            }
        }else
        {
            bottomSheetButton.setImageResource(R.drawable.heart_border_black);
        }
    }

    @Override
    public void onRecentChanged() {
        Log.d(TAG, "onRecentChanged: Recent changed!");
        if(InSquareProfile.isRecent(mLastSelectedSquareId)) {
            for(Square s : InSquareProfile.getRecentSquaresList()) {
                if(mLastSelectedSquareId.equals(s.getId())) {
                   updateBottomSheet(s);
                    break;
                }
            }
        }
    }

    public class MapFiller extends AsyncTask<String,Void,HashMap<String,Square>> {

        @Override
        protected HashMap<String, Square> doInBackground(String... params) {
            GsonBuilder b = new GsonBuilder();
            // SquareDeserializer specifica come popolare l'oggetto Message fromJson
            // Log.d(TAG, "Get Closest Squares onResponse: " + response);
            b.registerTypeAdapter(Square.class, new SquareDeserializer(getResources().getConfiguration().locale));
            Gson gson = b.create();
            Square[] squares = gson.fromJson(String.valueOf(params[0]), Square[].class);

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
            for(Square closeSquare: squarePins.values())
            {
                if(!squareHashMap.containsValue(closeSquare)) {
                    LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                    Marker m = createSquarePin(coords, closeSquare.getName());
                    squareHashMap.put(m, closeSquare);
                }
            }
            for(Square s : squareHashMap.values()) {
                if(mLastSelectedSquare != null) {
                    if(s.equals(mLastSelectedSquare)) {
                        updateBottomSheet(s);
                        break;
                    }
                }
            }
        }
    }

}
