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
import com.google.gson.reflect.TypeToken;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.Message.MessageDeserializer;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Square.SquareDeserializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class manages the HTTP requests made with Volley from all the Activities andr Fragments of the application
 */
public class VolleyManager {

    public static final String OK_RESPONSE = "OK!";

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
        String name = emptySpacesForParams(query);

        reqURL += "name=" + name;
        reqURL += "&lat=" + lat;
        reqURL += "&lon=" + lon;
        reqURL += "&userId=" + userId;

        Log.d(TAG, "searchSquaresByName: " + reqURL);

        StringRequest searchSquaresByNameRequest = new StringRequest(Request.Method.GET, reqURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "searchSquaresByName: " + response);
                        List<Square> squares = deserializeSquares(response);
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

    public void getClosestSquares(String distance,
                                  double lat,
                                  double lon,
                                  final VolleyResponseListener listener
    )
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
                        // MapFiller in @MapFragment gestisce la costruzione dell'oggetto
                        listener.responseGET(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            Log.d(TAG, "onErrorResponse: " + error.networkResponse.statusCode);
                            listener.responseGET(null);
                        }
                    }
                }
        );

        requestQueue.add(closeSquareRequest);
    }

    public void getRecentMessages(
            String isRecentRequest,
            String howMany,
            String squareId,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "messages?";
        volleyURL += "recent=" + isRecentRequest;
        volleyURL += "&size=" + howMany;
        volleyURL += "&square=" + squareId;

        Log.d(TAG, "getRecentMessages from : " + volleyURL);

        StringRequest getRecentMessages = new StringRequest(Request.Method.GET, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: got my recent messages!");
                        List<Message> messages = deserializeMessages(response);

                        listener.responseGET(messages);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.networkResponse.toString());
                        listener.responseGET(null);
                    }
                }
        );

        requestQueue.add(getRecentMessages);
    }

    // Gestisce sia POST che DELETE delle Favs
    public void handleFavoriteSquare(
            final int requestType,
            String squareId,
            String userId,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "favouritesquares?";
        volleyURL += "squareId=" + squareId;
        volleyURL += "&userId=" + userId;

        Log.d(TAG, "handleFavoriteSquare " + volleyURL);

        StringRequest favoriteSquare = new StringRequest(requestType, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        switch (requestType)
                        {
                            case Request.Method.POST:
                                listener.responsePOST(OK_RESPONSE);
                                break;
                            case Request.Method.DELETE:
                                listener.responseDELETE(OK_RESPONSE);
                                break;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: " + error.networkResponse.toString());
                        switch (requestType)
                        {
                            case Request.Method.POST:
                                listener.responsePOST(null);
                                break;
                            case Request.Method.DELETE:
                                listener.responseDELETE(null);
                                break;
                        }
                    }
                }
        );

        requestQueue.add(favoriteSquare);
    }

    public void postLoginToken(
            final String service,
            final String accessToken,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "auth/" + service + "/token";

        Log.d(TAG, "postLoginToken: " + volleyURL);

        StringRequest postTokenRequest = new StringRequest(Request.Method.POST, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "ServerResponse " + response);
                        listener.responsePOST(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "ErrorResponse: " + error.toString());
                        listener.responsePOST(null);
                    }
        }) {
            //TOKEN messo nei parametri della query
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("access_token", accessToken);

                return params;
            }
        };
        requestQueue.add(postTokenRequest);
    }

    public void postGCMToken(
            final String token,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "gcmToken";

        StringRequest postGCMTokenRequest = new StringRequest(Request.Method.POST, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response is: " + response);
                        listener.responsePOST(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                        listener.responsePOST(null);
                    }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("gcm", token);
                params.put("userId", InSquareProfile.getUserId());
                return params;
            }
        };

        requestQueue.add(postGCMTokenRequest);
    }


    public void postFeedback(final String feedback,
                             String userId,
                             String fromActivity,
                             final VolleyResponseListener listener)
    {
        String feedbackParam = emptySpacesForParams(feedback);

        String volleyURL = prefixURL + "feedback?";
        volleyURL += "feedback=" + feedbackParam;
        volleyURL += "&username=" + userId;
        volleyURL += "&activity=" + fromActivity;

        Log.d(TAG, "postFeedback - I'm about to POST to: " + volleyURL);

        StringRequest postFeedbackRequest = new StringRequest(Request.Method.POST, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "onResponse: " + response);

                        listener.responsePOST("Grazie per il feedback!");
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d(TAG, "onErrorResponse: errore!");
                        Log.d(TAG, error.toString());
                        listener.responsePOST(null);
                    }
                }
        );

        requestQueue.add(postFeedbackRequest);

    }

    public void postSquare(final String squareName,
                           final String squareDescr,
                           final String latitude,
                           final String longitude,
                           final String ownerId,
                           final VolleyResponseListener listener)
    {
        String name = emptySpacesForParams(squareName);
        String description = emptySpacesForParams(squareDescr);

        String volleyURL = prefixURL + "squares?";
        volleyURL += "name=" + name;
        volleyURL += "&description=" + description;
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
                        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));

                        Square postedSquare = builder.create().fromJson(response, Square.class);
                        listener.responsePOST(postedSquare);
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

    public void getOwnedSquares(String byOwner,
                                String ownerId,
                                final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "squares?";
        volleyURL += "byOwner=" + byOwner;
        volleyURL += "&ownerId=" + ownerId;

        Log.d(TAG, "getOwnedSquares url: " + volleyURL);

        StringRequest getOwnedRequest = new StringRequest(Request.Method.GET, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Log.d(TAG, "getOwnedSquares response: " + response);

                        List<Square> squares = deserializeSquares(response);
//                        Log.d(TAG, "I created: " + squares.toString());

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

    public void getFavoriteSquares(
            final String userId,
            final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "favouritesquares/";
        volleyURL += userId;

        Log.d(TAG, "getFavoriteSquares url: " + volleyURL);

        StringRequest getFavsRequest = new StringRequest(
                Request.Method.GET,
                volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Log.d(TAG, "getFavoriteSquares response: " + response);

                        List<Square> squares = deserializeSquares(response);

//                        for(Square s: squares)
//                            Log.d(TAG, "I obtained: " + s.toString());

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

    public void getRecentSquares(
            String userId,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "recentSquares/";
        volleyURL += userId;

        Log.d(TAG, "getRecentSquares url: " + volleyURL);

        StringRequest getRecentsRequest= new StringRequest(
                Request.Method.GET,
                volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        List<Square> squares = deserializeSquares(response);

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

        requestQueue.add(getRecentsRequest);
    }


    // Deserializza dalla risposta JSON una lista di Squares
    public List<Square> deserializeSquares(String jsonResponse)
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));
        Type listType = new TypeToken<ArrayList<Square>>() {
        }.getType();
        List<Square> squares = builder.create().fromJson(jsonResponse, listType);

        return squares;
    }

    // Deserializza dalla risposta JSON una lista di Messages
    public List<Message> deserializeMessages(String jsonResponse)
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Message.class, new MessageDeserializer(locale));
        Type listType = new TypeToken<ArrayList<Message>>() {
        }.getType();
        List<Message> messages = builder.create().fromJson(jsonResponse, listType);

        Collections.reverse(messages);

        return messages;
    }

    public void patchDescription(
            String name,
            String description,
            final String squareId,
            final String ownerId,
            final VolleyResponseListener listener)
    {
        String volleyURL = prefixURL + "squares?";
        String nameParam = emptySpacesForParams(name);
        String descriptionParam = emptySpacesForParams(description);

        volleyURL += "name=" + nameParam;
        volleyURL += "&description=" + descriptionParam;
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

    public void patchLocation(
            final String latitude,
            final String longitude,
            final String userId,
            final String isUpdateLocation,
            final VolleyResponseListener listener
    )
    {
        String volleyURL = prefixURL + "user";

        StringRequest patchLocationRequest = new StringRequest(Request.Method.PATCH, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response is: " + response);
                        listener.responsePATCH(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                        listener.responsePATCH(null);
                    }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("lat", latitude);
                params.put("lon", longitude);
                params.put("userId", userId);
                params.put("updateLocation",isUpdateLocation);
                return params;
            }
        };

        requestQueue.add(patchLocationRequest);
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

    private String emptySpacesForParams(String urlParameter)
    {
        // Gli url con spazi vuoti causano problemi
        // Negli url gli spazi vuoti compaiono come %20
        String res = urlParameter.replace(" ", "%20");
        return res;
    }
}
