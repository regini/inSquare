package com.nsqre.insquare.Utilities;

/**
 * Created by Regini on 05/03/16.
 */

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.Locale;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = "LocationService";
    private static final int LOCATION_INTERVAL = 20*60*1000;
    public static final long FASTEST_UPDATE_INTERVAL = LOCATION_INTERVAL / 2;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationListener locationListener;

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, locationListener);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult arg0) {

    }

    private class LocationListener implements com.google.android.gms.location.LocationListener {

        public LocationListener() {

        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            sendLocationToServer(location);
        }

    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        boolean stopService = false;
        if (intent != null)
            stopService = intent.getBooleanExtra("stopservice", false);

        Log.d(TAG, "onStartCommand: stop service? " + stopService);

        locationListener = new LocationListener();
        if (stopService)
            stopLocationUpdates();
        else {
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    public void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, locationListener);

        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    private void sendLocationToServer(final Location lastLocation) {
        VolleyManager.getInstance(getApplicationContext(), Locale.getDefault());
        InSquareProfile.getInstance(getApplicationContext());
        final String lat = String.valueOf(lastLocation.getLatitude());
        final String lon = String.valueOf(lastLocation.getLongitude());
        final String isUpdateLocation = String.valueOf(true);
        VolleyManager.getInstance().patchLocation(
                lat,
                lon,
                InSquareProfile.getUserId(),
                isUpdateLocation,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // Vuoto - PATCH Request
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Vuoto - PATCH Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        if(object == null)
                        {
                            Log.d(TAG, "responsePATCH: qualcosa è andato storto durante il PATCH della location");
                        }else 
                        {
                            Log.d(TAG, "responsePATCH: tutto ok!");
                        }
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto - PATCH Request
                    }
                }
        );
    }
}