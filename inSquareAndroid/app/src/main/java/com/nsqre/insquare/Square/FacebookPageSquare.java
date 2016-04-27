package com.nsqre.insquare.Square;/* Created by umbertosonnino on 26/4/16  */

import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class FacebookPageSquare extends Square {

    private static final String TAG = "FacebookPageSquare";
    private String pageId;
    public String likeCount;
    public String priceRange;
    public String phoneNumber;
    public String website;
    public String street;
    public List<String> hoursList;

    public FacebookPageSquare(String id, String name, String description, String geoloc, String ownerId, String favouredBy,
                              String[] favourers, String views, String state, String lastMessageDate, String type,
                              String pageId, Locale l) {

        super(id, name, description, geoloc, ownerId, favouredBy, favourers, views, state, lastMessageDate, type, l);
        this.pageId = pageId;
        this.isFacebookPage = true;

        if(FacebookSdk.isInitialized()) {
            downloadAndFillPageDetails();
        }
    }

    private void downloadAndFillPageDetails() {
        Bundle requestParams = new Bundle();
        requestParams.putString("fields", "fan_count,price_range,hours,phone,location,website");

        Log.d(TAG, "downloadAndFillePageDetails: " + this.pageId);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/v2.6/" + this.pageId,
                requestParams,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject object = response.getJSONObject();
//                        Log.d(TAG, "onCompleted: page details ====\n" + response.toString());

                        if(object == null)
                        {
                            Log.d(TAG, "onCompleted: fallito il download dei dati da Facebook!");
                            return;
                        }

                        try {

                            JSONObject location, hours;

                            if(object.has("fan_count")) {
                                likeCount = object.getString("fan_count").trim();
                            }
                            if(object.has("price_range")) {
                                priceRange = object.getString("price_range").trim();
                            }
                            if(object.has("phone")) {
                                phoneNumber = object.getString("phone").trim();
                            }
                            if(object.has("website")) {
                                website = object.getString("website").trim();
                            }

                            // Location
                            if(object.has("location")) {
                                location = object.getJSONObject("location");
                                street = location.getString("street").trim();
                            }

                            hoursList = new ArrayList<>();

                            // Hours
                            if(object.has("hours")) {
                                hours = object.getJSONObject("hours");
                                Iterator<String> keys = hours.keys();

                                while (keys.hasNext()) {
                                    String listValue = "";

                                    String keyDay = keys.next();
                                    String valueOpen = hours.getString(keyDay);

                                    String keyDayOpen = keyDay.split("_")[0];
                                    String capitalized = keyDayOpen.substring(0, 1).toUpperCase() + keyDayOpen.substring(1);

                                    listValue += capitalized + ": " + valueOpen;

                                    if (keys.hasNext()) {
                                        // Ha anche una controparte per la chiusura
                                        String valueClose = hours.getString(keys.next());
                                        listValue += (" - " + valueClose);
                                    }
                                    hoursList.add(listValue.trim());
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }
}

