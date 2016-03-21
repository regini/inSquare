package com.nsqre.insquare.Utilities.REST;/* Created by umbertosonnino on 10/3/16  */

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.GsonBuilder;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Square.SquareDeserializer;

import java.util.Locale;

/**
 * This class manages the HTTP requests made with Volley from all the Activities andr Fragments of the application
 */
public class VolleyManager {

    public interface VolleyResponseListener<E>
    {
        // Handler per le risposte
        // =======================
        // Risposta ad una GET
        void responseGET(E object);
        // Risposta ad una POST
        void responsePOST(E object);
        // Risposta per la PATCH
        void responsePATCH(E object);
        // Risposta per la DELETE
        void responseDELETE(E object);
    }
    private static final String TAG = "VolleyManager";
    private static VolleyManager instance = null;
    private static Locale locale;

    private static final String prefixURL = "http://recapp-insquare.rhcloud.com/";

    public RequestQueue requestQueue;

    private VolleyManager(Context c)
    {
        Log.d(TAG, "VolleyManager: just instantiated the object privately!");
        requestQueue = Volley.newRequestQueue(c.getApplicationContext());
    }

    public static synchronized VolleyManager getInstance(Context c, Locale l)
    {
        if(instance == null)
        {
            locale = l;
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

    public void searchSquaresByName(String query, String userId, double lat, double lon, final VolleyResponseListener listener)
    {
        String reqURL = prefixURL + "squares?";
        String name = query.replace(" ", "%20");
        reqURL += "name=" + name;

        Log.d(TAG, "searchSquaresByName: " + reqURL);

        StringRequest searchSquaresByNameRequest = new StringRequest(Request.Method.GET, reqURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "searchSquaresByName: " + response);
                        GsonBuilder builder = new GsonBuilder();
                        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));

                        Square[] squares = builder.create().fromJson(response, Square[].class);
                        Log.d(TAG, "I found: " + squares.toString());

                        listener.responseGET(squares);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            Log.d(TAG, "onErrorResponse: " + error.toString());
                            listener.responseGET(null);
                        }
                    }
                }
        );

        requestQueue.add(searchSquaresByNameRequest);
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

    public void postSquare(final String squareName,
                           final String squareDescr,
                           final String latitude,
                           final String longitude,
                           final String ownerId,
                           final Locale loc,
                           final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "squares?";
        volleyURL += "name=" + squareName;
        volleyURL += "&description=" + squareDescr;
        volleyURL += "&lat=" + latitude;
        volleyURL += "&lon=" + longitude;
        volleyURL += "&ownerId=" + ownerId;

        Log.d(TAG, "postSquare url: " + volleyURL);

        StringRequest postSquareRequest = new StringRequest(Request.Method.POST, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "postSquare response: " + response);
                        GsonBuilder builder = new GsonBuilder();
                        builder.registerTypeAdapter(Square.class, new SquareDeserializer(loc));

                        Square s = builder.create().fromJson(response, Square.class);
                        listener.responsePOST(s);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        // Risposta in caso di errore e' null
                        listener.responsePOST(null);
                    }
                }
        );

        requestQueue.add(postSquareRequest);

    }

    public void getOwnedSquares(boolean byOwner,
                                String ownerId,
                                final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "squares?";
        volleyURL += "byOwner=" + byOwner;
        volleyURL += "&ownerId=" + ownerId;

        Log.d(TAG, "getOwnedSquares url: " + volleyURL );

        StringRequest getOwnedRequest = new StringRequest(Request.Method.GET, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "getOwnedSquares response: " + response);
                        GsonBuilder builder = new GsonBuilder();
                        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));

                        Square[] squares = builder.create().fromJson(response, Square[].class);
                        Log.d(TAG, "I created: " + squares.toString());

                        listener.responseGET(squares);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        listener.responseGET(null);
                    }
                }
        );

        requestQueue.add(getOwnedRequest);
    }

    public void getFavs(
            final String userId,
            final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "favouritesquares/";
        volleyURL += userId;

        Log.d(TAG, "getFavs url: " + volleyURL);

        StringRequest getFavsRequest = new StringRequest(
                Request.Method.GET,
                volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "getFavs response: " + response);
                        GsonBuilder builder = new GsonBuilder();
                        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));
                        Square[] squares = builder.create().fromJson(response, Square[].class);

                        for(Square s: squares)
                            Log.d(TAG, "I obtained: " + s.toString());
                        listener.responseGET(squares);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        listener.responseGET(null);
                    }
                }
        );

        requestQueue.add(getFavsRequest);
    }

    public void patchDescription(
            String name,
            String description,
            final String squareId,
            final String ownerId,
            final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "squares?";
        description = description.replace(" ", "%20");
        name = name.replace(" ", "%20");
        volleyURL += "name=" + name;
        volleyURL += "&description=" + description;
        volleyURL += "&squareId=" + squareId;
        volleyURL += "&ownerId=" + ownerId;

        Log.d(TAG, "patchDescr url: " + volleyURL);

        StringRequest patchDescriptionRequest = new StringRequest(
                Request.Method.PATCH,
                volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "patchDescription response: " + response);
                        if(response.toLowerCase().contains("non"))
                        {
                            listener.responsePATCH(false);
                        }else
                        {
                            // Tutto e' andato bene
                            listener.responsePATCH(true);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        listener.responsePATCH(false);
                    }
        }
        );

        requestQueue.add(patchDescriptionRequest);
    }

    public void deleteSquare(
            final String squareId,
            final String ownerId,
            final VolleyResponseListener listener) {
        String volleyURL = prefixURL + "squares?";
        volleyURL += "&squareId=" + squareId;
        volleyURL += "&ownerId=" + ownerId;
        Log.d(TAG, "deleteSquare url: " + volleyURL);
        StringRequest deleteSquareRequest = new StringRequest(
                Request.Method.DELETE,
                volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "deleteSquare response is " + response);
                        if(response.toLowerCase().contains("error")) {
                            listener.responseDELETE(false);
                        } else {
                            listener.responseDELETE(true);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: " + error.toString());
                listener.responseDELETE(false);
            }
        });
        requestQueue.add(deleteSquareRequest);
    }

}
