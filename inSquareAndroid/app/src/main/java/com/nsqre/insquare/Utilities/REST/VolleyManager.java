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
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.Message.MessageDeserializer;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Square.SquareDeserializer;
import com.nsqre.insquare.User.InSquareProfile;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class manages the HTTP requests made with Volley from all the Activities and Fragments of the application
 */
public class VolleyManager {

    public static final String OK_RESPONSE = "OK!";

    /**
     * An interface to manage responses from the server
     */
    public interface VolleyResponseListener<E>
    {
        /**
         * Manages the response to the GET request
         * @param object the data on the response of the server
         */
        void responseGET(E object);

        /**
         * Manages the response to the POST request
         * @param object the data on the response of the server
         */
        void responsePOST(E object);

        /**
         * Manages the response to the PATCH request
         * @param object the data on the response of the server
         */
        void responsePATCH(E object);

        /**
         * Manages the response to the DELETE request
         * @param object the data on the response of the server
         */
        void responseDELETE(E object);
    }

    private static final String TAG = "VolleyManager";
    private static VolleyManager instance = null;
    private static Locale locale;
    private Context context;

    private String[] URL_Array;
    private static String baseURL;
    boolean connection;

    public RequestQueue requestQueue;

    private VolleyManager(Context c)
    {
        Log.d(TAG, "VolleyManager: just instantiated the object privately!");
        requestQueue = Volley.newRequestQueue(c.getApplicationContext());
        this.context = c;
    }

    public static synchronized VolleyManager getInstance(Context c)
    {
        if(instance == null)
        {
            locale = c.getResources().getConfiguration().locale;
            instance = new VolleyManager(c);
        }
//        Log.d(TAG, "getInstance: returning VolleyManger");
        baseURL = c.getString(R.string.baseUrl);
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

    /**
     * Searches for squares in the server's database
     * @param query the query of the user
     * @param userId the id of the user
     * @param lat the latitude of the position of the user
     * @param lon the longitude of the position of the user
     * @param listener the listener to handle the results
     */
    public void searchSquares(String query, String userId, double lat, double lon, final VolleyResponseListener listener)
    {
         
        String reqURL = baseURL + "squares?";
        String name = emptySpacesForParams(query);

        reqURL += "name=" + name;
        reqURL += "&lat=" + lat;
        reqURL += "&lon=" + lon;
        reqURL += "&userId=" + userId;

        // Log.d(TAG, "searchSquares: " + reqURL);

        StringRequest searchSquaresRequest = new StringRequest(Request.Method.GET, reqURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "searchSquares: " + response);
                        List<Square> squares = deserializeSquares(response);
                        Log.d(TAG, "I found: " + squares.toString());

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

        requestQueue.add(searchSquaresRequest);
    }

    /**
     * Searchs on the server's database for squares around a certain position and a certain distance
     * @param distance the radius of research
     * @param lat the latitude of the position of the user
     * @param lon the longitude of the position of the user
     * @param listener the listener to handle the results
     */
    public void getClosestSquares(String distance,
                                  double lat,
                                  double lon,
                                  final VolleyResponseListener listener
    )
    {
         
        String reqURL = baseURL + "squares?";
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
                            Log.d(TAG, "onErrorResponse: " + error.toString());
                            listener.responseGET(null);
                    }
                }
        );

        requestQueue.add(closeSquareRequest);
    }

    /**
     * Gets the most recent messages from a square
     * @param isRecentRequest if true gets the user the most recent messages
     * @param howMany the number of messages to download
     * @param squareId the square id from which to download
     * @param listener the listener to handle the results
     */
    public void getRecentMessages(
            String isRecentRequest,
            String howMany,
            String squareId,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "messages?";
        volleyURL += "recent=" + isRecentRequest;
        volleyURL += "&size=" + howMany;
        volleyURL += "&square=" + squareId;

        // Log.d(TAG, "getRecentMessages from : " + volleyURL);

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
                        Log.d(TAG, "onErrorResponse: " + error.toString());
                        listener.responseGET(null);
                    }
                }
        );

        requestQueue.add(getRecentMessages);
    }

    /**
     * Adds or removes a square from the user's favourite
     * @param requestType POST for adding, DELETE for removing
     * @param squareId the square to add/remove
     * @param userId the user's id
     * @param listener the listener to handle the results
     */
    public void handleFavoriteSquare(
            final int requestType,
            String squareId,
            String userId,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "favouritesquares?";
        volleyURL += "squareId=" + squareId;
        volleyURL += "&userId=" + userId;

        // Log.d(TAG, "handleFavoriteSquare " + volleyURL);

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
                        Log.d(TAG, "onErrorResponse: " + error.toString());
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

    /**
     * Manages the POST request to login via Google/Facebook
     * @param service the service you want to use
     * @param accessToken the access token
     * @param listener the listener to handle the results
     */
    public void postLoginToken(
            final String service,
            final String accessToken,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "auth/" + service + "/token";

        // Log.d(TAG, "postLoginToken: " + volleyURL);

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

    /**
     * Updates the access token for the login
     * @param service the service you want to use
     * @param accessToken the access token
     * @param listener the listener to handle the results
     */
    public void patchLoginToken (
            final String service,
            final String accessToken,
            final VolleyResponseListener listener
    ) {
        String volleyURL = baseURL + "user/" + service;
        // Log.d(TAG, "patchLoginToken: " + volleyURL);

        StringRequest patchTokenRequest = new StringRequest(Request.Method.PATCH, volleyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "ServerResponse " + response);
                        listener.responsePATCH(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "ErrorResponse: " + error.toString());
                        listener.responsePATCH(null);
                    }
        }) {
            @Override
            protected Map<String, String> getParams() {
                InSquareProfile.getInstance(context);
                Map<String,String> params = new HashMap<>();
                params.put("userId", InSquareProfile.getUserId());
                params.put("access_token", accessToken);
                return params;
            }
        };
        requestQueue.add(patchTokenRequest);
    }

    /**
     * Updates the GCM Token, if expired
     * @param token the new token
     * @param listener the listener to handle the results
     */
    public void patchGCMToken(
            final String token,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "gcmToken";

        StringRequest patchGCMTokenRequest = new StringRequest(Request.Method.PATCH, volleyURL,
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
                params.put("gcm", token);
                params.put("userId", InSquareProfile.getUserId());
                return params;
            }
        };

        requestQueue.add(patchGCMTokenRequest);
    }

    /**
     * Sends a feedback that will be stored on the backend database
     * @param feedback the feedback
     * @param userId the user that submitted it
     * @param fromActivity from which activity it was submitted
     * @param listener the listener to handle the results
     */
    public void postFeedback(final String feedback,
                             String userId,
                             String fromActivity,
                             final VolleyResponseListener listener)
    {
         
        String feedbackParam = emptySpacesForParams(feedback);

        String volleyURL = baseURL + "feedback?";
        volleyURL += "feedback=" + feedbackParam;
        volleyURL += "&username=" + userId;
        volleyURL += "&activity=" + fromActivity;

        // Log.d(TAG, "postFeedback - I'm about to POST to: " + volleyURL);

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

    /**
     * POST request to create a new square
     * @param squareName name of the square
     * @param squareDescr description of the square
     * @param latitude latitude of the position of the square
     * @param longitude longitude of the position of the square
     * @param ownerId the id of the owner
     * @param listener the listener to handle the results
     */
    public void postSquare(final String squareName,
                           final String squareDescr,
                           final String latitude,
                           final String longitude,
                           final String ownerId,
                           final VolleyResponseListener listener)
    {
         
        String name = emptySpacesForParams(squareName);
        String description = emptySpacesForParams(squareDescr);

        String volleyURL = baseURL + "squares?";
        volleyURL += "name=" + name;
        volleyURL += "&description=" + description;
        volleyURL += "&lat=" + latitude;
        volleyURL += "&lon=" + longitude;
        volleyURL += "&ownerId=" + ownerId;
        volleyURL += "&type=0";

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

    /**
     * POST request to create a Facebook square (Page or Event)
     * @param squareName name of the square
     * @param squareDescr description of the square
     * @param latitude latitude of the position of the square
     * @param longitude longitude of the position of the square
     * @param ownerId the id of the owner
     * @param squareType type of the square
     * @param facebookId Facebook's id of the page/event
     * @param expireTime expiration time of the square
     * @param listener the listener to handle the results
     */
    public void postFacebookSquare(
           final String squareName,
           final String squareDescr,
           final String latitude,
           final String longitude,
           final String ownerId,
           final String squareType,
           final String facebookId,
           final String expireTime,
           final VolleyResponseListener listener
    )
    {

        String name = emptySpacesForParams(squareName);
        String description = emptySpacesForParams(squareDescr);

        String volleyURL = baseURL + "squares?";
        volleyURL += "name=" + name;
        volleyURL += "&description=" + description;
        volleyURL += "&lat=" + latitude;
        volleyURL += "&lon=" + longitude;
        volleyURL += "&ownerId=" + ownerId;
        volleyURL += "&type=" + squareType;
        if(expireTime != null) {
            volleyURL += "&expireTime=" + expireTime;
        }
        if(squareType.equals("1"))
        {
            volleyURL += "&facebookIdEvent=" + facebookId;
        }else
        {
            volleyURL += "&facebookIdPage=" + facebookId;
        }

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

    /**
     * Downloads user's owned squares
     * @param byOwner true if the research is for the owner
     * @param ownerId the id of the owner
     * @param listener the listener to handle the results
     */
    public void getOwnedSquares(String byOwner,
                                String ownerId,
                                final VolleyResponseListener listener)
    {
         
        String volleyURL = baseURL + "squares?";
        volleyURL += "byOwner=" + byOwner;
        volleyURL += "&ownerId=" + ownerId;

         Log.d(TAG, "getOwnedSquares url: " + volleyURL);

        StringRequest getOwnedRequest = new StringRequest(Request.Method.GET, volleyURL,
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

        requestQueue.add(getOwnedRequest);
    }

    /**
     * Downloads user's favourite squares
     * @param userId the id of the user
     * @param listener the listener to handle the results
     */
    public void getFavoriteSquares(
            final String userId,
            final VolleyResponseListener listener)
    {
         
        String volleyURL = baseURL + "favouritesquares/";
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

    /**
     * Downloads user's recent squares
     * @param userId the id of the user
     * @param listener the listener to handle the results
     */
    public void getRecentSquares(
            String userId,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "recentSquares/";
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


    /**
     * Deserializes a list of squares received from the server
     * @param jsonResponse the data received from the server
     * @return a list of Square objects
     */
    public List<Square> deserializeSquares(String jsonResponse)
    {

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Square.class, new SquareDeserializer(locale));
        Type listType = new TypeToken<ArrayList<Square>>() {
        }.getType();
        List<Square> squares = builder.create().fromJson(jsonResponse, listType);

        return squares;
    }

    /**
     * Deserializes a list of messages received from the server
     * @param jsonResponse the data received from the server
     * @return a list of Message objects
     */
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

    /**
     * Updates name and description of the square
     * @param name the new name
     * @param description the new description
     * @param squareId the id of the square to update
     * @param ownerId the id of the owner of the square
     * @param listener the listener to handle the results
     */
    public void patchDescription(
            String name,
            String description,
            final String squareId,
            final String ownerId,
            final VolleyResponseListener listener)
    {
         
        String volleyURL = baseURL + "squares?";
        String nameParam = emptySpacesForParams(name);
        String descriptionParam = emptySpacesForParams(description);

        volleyURL += "name=" + nameParam;
        volleyURL += "&description=" + descriptionParam;
        volleyURL += "&squareId=" + squareId;
        volleyURL += "&ownerId=" + ownerId;

        // Log.d(TAG, "patchDescr url: " + volleyURL);

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

    /**
     * Updates the location of a square
     * @param latitude the new latitude of the square
     * @param longitude the new longitude of the square
     * @param userId the id of the user
     * @param isUpdateLocation true if the location has to be updated
     * @param listener the listener to handle the results
     */
    public void patchLocation(
            final String latitude,
            final String longitude,
            final String userId,
            final String isUpdateLocation,
            final VolleyResponseListener listener
    )
    {
         
        String volleyURL = baseURL + "user";

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

    /**
     * Deletes a square and all the messages contained in it from the database
     * @param squareId the id of the square
     * @param ownerId the id of the owner
     * @param listener the listener to handle the results
     */
    public void deleteSquare(
            final String squareId,
            final String ownerId,
            final VolleyResponseListener listener) {
         
        String volleyURL = baseURL + "squares?";
        volleyURL += "&squareId=" + squareId;
        volleyURL += "&ownerId=" + ownerId;
        // Log.d(TAG, "deleteSquare url: " + volleyURL);
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
                Log.d(TAG, "onErrorDeleteSquare Response: " + error.toString());
                listener.responseDELETE(false);
            }
        });
        requestQueue.add(deleteSquareRequest);
    }

    /**
     * Removes empty spaces from strings
     * @param urlParameter the url
     * @return the initial string with spaces replaced by '%20'
     */
    private String emptySpacesForParams(String urlParameter)
    {
        // Gli url con spazi vuoti causano problemi
        // Negli url gli spazi vuoti compaiono come %20
        String res = urlParameter.replace(" ", "%20");
        res = res.replace("\n", "%0A");
        return res;
    }
}
