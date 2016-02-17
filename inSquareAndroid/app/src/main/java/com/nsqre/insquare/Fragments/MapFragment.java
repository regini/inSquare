package com.nsqre.insquare.Fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Activities.MapActivity;
import com.nsqre.insquare.Fragments.Helpers.MapWrapperLayout;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.REST.DownloadClosestSquares;
import com.nsqre.insquare.Utilities.Square;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends SupportMapFragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        OnMapReadyCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final int MAX_SQUARENAME_LENGTH = 40;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private MapWrapperLayout mTouchView;
    private View mOriginalContentView;
    private static final String TAG = "MapFragment";
    private GoogleApiClient mGoogleApiClient;
    public Location mCurrentLocation;

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
    private MapActivity rootActivity;

    private boolean waitingDelay;

    private Tracker mTracker;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getActivity().getApplication();
        mTracker = application.getDefaultTracker();

        waitingDelay = false;

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        squareHashMap = new HashMap<Marker, Square>();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        rootActivity = (MapActivity) getActivity();

        getMapAsync(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        if (mGoogleApiClient == null) {
            Log.d(TAG, "Disconnected atm");
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void initListeners() {
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
//        mListener = null;
    }

    @Override
    public void onConnected(Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            handlePermissions();
            return;
        }
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mCurrentLocation == null)
        {
            Log.d(TAG, "The location was null");
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
                if(locationManager != null)
                {
                    Location location = locationManager.getLastKnownLocation(GPS);
                    if(location != null)
                    {
                        Log.d("GPS - " + TAG, "I've been able to get a location! Lat: "
                                + location.getLatitude()
                                + "; Long: "
                                + location.getLongitude() + ";");
                        mCurrentLocation = location;
                    }
                }else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListener);
                    if(locationManager != null)
                    {
                        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if(location != null)
                        {
                            Log.d("NETWORK - " + TAG, "I've been able to get a location! Lat: "
                                    + location.getLatitude()
                                    + "; Long: "
                                    + location.getLongitude() + ";");
                            mCurrentLocation = location;
                        }
                    }
                }else
                {
                    Toast.makeText(getContext(),
                            "There's no way for me to get a location!",
                            Toast.LENGTH_LONG)
                            .show();
                    return;
                }



            }

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0 , 0, locationListener);
        }else {
            initCamera(mCurrentLocation);
        }
    }

    private Address reverseGeocode() throws IOException {
        Geocoder gc = new Geocoder(getContext());
        if(gc.isPresent())
        {
            List<Address> list = gc.getFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 1);

            Address address = list.get(0);
            Log.d(TAG, "reverseGeocode: " + address.toString());

            return address;
        }
        return null;
    }

    private void downloadAndInsertPins(double distance)
    {
            waitingDelay = true;

            String d = distance + "km";
            Log.d(TAG, "downloadAndInsertPins: " + d);

            DownloadClosestSquares dcs;
            if(mCurrentLocation!=null)
            {
                    dcs = new DownloadClosestSquares(d,
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            }
            else
                    dcs = new DownloadClosestSquares(d,0,0);

            try {
                HashMap<String, Square> squarePins = dcs.execute().get();

                for (Square closeSquare : squarePins.values()) {
                    LatLng coords = new LatLng(closeSquare.getLat(), closeSquare.getLon());
                    Marker m = createSquarePin(coords, closeSquare.getName());
                    squareHashMap.put(m, closeSquare);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    waitingDelay = false;
                }
            }, 2000);
    }

    private void handlePermissions() {
        ActivityCompat.requestPermissions(getActivity(),
                PERMISSIONS, REQUEST_FINE_LOCATION);

        ActivityCompat.requestPermissions(getActivity(),
                PERMISSIONS, REQUEST_COARSE_LOCATION);
    }

    private void initCamera(Location mCurrentLocation) {

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            handlePermissions();
            return;
        }
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(mCurrentLocation.getLatitude(),
                        mCurrentLocation.getLongitude()))
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

//        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), null);
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));

        mGoogleMap.setMapType(MAP_TYPES[curMapTypeIndex]);
        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setMyLocationEnabled(true);
//        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // LatLng | Name |
    private Marker createSquarePin(LatLng pos, String name) {

        MarkerOptions options = new MarkerOptions().position(pos);
        options.title(name);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.nsqre_map_pin));
        Marker marker = mGoogleMap.addMarker(options);

        return marker;
    }
    @Override
    public void onMapClick(final LatLng latLng) {

        final String lat = Double.toString(latLng.latitude);
        final String lon = Double.toString(latLng.longitude);

        final Dialog mDialog = new Dialog(getContext());
        mDialog.setContentView(R.layout.dialog_crea_square);
        mDialog.setTitle("Crea una Square");
        mDialog.setCancelable(true);
        mDialog.show();

        final EditText usernameEditText = (EditText) mDialog.findViewById(R.id.et_square);
        TextInputLayout textInputLayout = (TextInputLayout) mDialog.findViewById(R.id.input_layout_crea_square);
        Button crea = (Button) mDialog.findViewById(R.id.button_crea);
        crea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String squareName = usernameEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(squareName)) {
                    Marker m = createSquarePin(latLng, squareName);
                    // Richiesta Volley POST per la creazione di piazze
                    // Si occupa anche di creare e aggiungere la nuova Square al HashMap
                    createSquarePostRequest(squareName, lat, lon, m);
                    mDialog.dismiss();
                }
            }
        });

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
    }

    public String getAddressFromLatLng(LatLng latLng)
    {
        Geocoder geocoder = new Geocoder( getActivity() );

        String address = "";
        try {
            address = geocoder
                    .getFromLocation( latLng.latitude, latLng.longitude, 1 )
                    .get( 0 ).getAddressLine( 0 );
        } catch (IOException e ) {
        }

        return address;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        // TODO on secondo click start chat
        marker.showInfoWindow();

        TextView tv = (TextView)getActivity().findViewById(R.id.square_textview);
        String text = marker.getTitle();
        if(text.length() > MAX_SQUARENAME_LENGTH)
        {
            text = text.substring(0, MAX_SQUARENAME_LENGTH-3) + "...";
        }
        tv.setText("#" + text);

        // Animazione per mostrare il riquadro in fondo allo schermo insieme al tasto per accedere alle piazze
        LinearLayout ll = ((LinearLayout) getActivity().findViewById(R.id.slider_ll));
        if(ll.getVisibility() == View.GONE) {
            ll.setVisibility(View.VISIBLE);
            FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.map_fab);
            fab.setVisibility(View.VISIBLE);
        }

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()),
                400, // Tempo di spostamento in ms
                null); // callback
        Square currentSquare = squareHashMap.get(marker);

        rootActivity.setSquareId(currentSquare.getId());
        rootActivity.setSquareName(marker.getTitle());
        Log.d(TAG, currentSquare.getId() + " " + currentSquare.getName());

        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        startChatActivity(marker);
    }

    private void startChatActivity(Marker marker) {

        // [START PinButton_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("MapActivity")
                .setAction("PinButton")
                .build());
        // [END PinButton_event]

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        Square s = squareHashMap.get(marker);
        intent.putExtra(MapActivity.SQUARE_ID_TAG, s.getId());
        intent.putExtra(MapActivity.SQUARE_NAME_TAG, s.getName());
        startActivity(intent);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;

        initListeners();
    }

    private void createSquarePostRequest(final String squareName,
                                         final String latitude,
                                         final String longitude,
                                         final Marker marker) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = "http://recapp-insquare.rhcloud.com/squares";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Create Square response: " + response);
                        try {
                            /*
                                Devo recuperare ID della Square dal responso del server
                                per poter mantenere coerenti le strutture dati della mappa
                            */

                            JSONObject o = new JSONObject(response);
                            String squareId = o.getString("_id");
                            double lat = Double.parseDouble(latitude);
                            double lon = Double.parseDouble(longitude);
                            Square s = new Square(squareId, squareName, lat, lon, "geo_point");
                            squareHashMap.put(marker, s);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Toast.makeText(getContext(), "Square creata con successo!", Toast.LENGTH_SHORT).show();
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
                params.put("lat", latitude);
                params.put("lon", longitude);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        VisibleRegion vr = mGoogleMap.getProjection().getVisibleRegion();
        
        Log.d(TAG, "onCameraChange distance is:" + getDistance(vr.latLngBounds.southwest, vr.latLngBounds.northeast));

        double distance = getDistance(vr.latLngBounds.southwest, vr.latLngBounds.northeast);
        if (!waitingDelay)
            downloadAndInsertPins(distance);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
