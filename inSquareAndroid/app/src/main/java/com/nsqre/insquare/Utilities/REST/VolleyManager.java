package com.nsqre.insquare.Utilities.REST;/* Created by umbertosonnino on 10/3/16  */

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class VolleyManager {

    public interface VolleyResponseListener
    {
        void getClosestSquares();
    }
    private static final String TAG = "VolleyManager";
    private static VolleyManager instance = null;

    private static final String prefixURL = "http://recapp-insquare.rhcloud.com/";

    public RequestQueue requestQueue;

    private VolleyManager(Context c)
    {
        Log.d(TAG, "VolleyManager: just instantiated the object privately!");
        requestQueue = Volley.newRequestQueue(c.getApplicationContext());
    }

    public static synchronized VolleyManager getInstance(Context c)
    {
        if(instance == null)
        {
            instance = new VolleyManager(c);
        }
        Log.d(TAG, "getInstance: returning VolleyManger");

        return instance;
    }

    public static synchronized VolleyManager getInstance()
    {
        if(instance == null)
        {
            throw new IllegalStateException(VolleyManager.class.getSimpleName() +
                    " is not initialized, call getInstance(context) first");
        }

        return instance;
    }

    public void getClosestSquares(String distance, double lat, double lon, VolleyResponseListener listener)
    {
        String reqURL = prefixURL + "squares?";
        reqURL += "distance=" + distance;
        reqURL += "&lat=" + lat;
        reqURL += "&lon=" + lon;

        Log.d(TAG, "getClosestSquares: " + reqURL);

        StringRequest closeSquareRequest = new StringRequest(Request.Method.GET, reqURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // SquareDeserializer si occupa di costruire l'oggetto Square in maniera appropriata
//                        new MapFragment.MapFiller().execute(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            Log.d(TAG, "onErrorResponse: " + error.networkResponse.statusCode);
                        }
                    }
                }
        );

        requestQueue.add(closeSquareRequest);
    }

}
