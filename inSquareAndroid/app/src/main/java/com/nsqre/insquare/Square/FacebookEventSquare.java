package com.nsqre.insquare.Square;/* Created by umbertosonnino on 26/4/16  */

import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FacebookEventSquare extends Square {

    private static final String TAG = "FacebookEventSquare";
    private String eventId;
    public String street;
    public String website;
    public String time;

    public FacebookEventSquare(String id, String name, String description, String geoloc, String ownerId, String favouredBy,
                               String[] favourers, String views, String state, String lastMessageDate, String type, String eventId, Locale l)
    {
        super(id, name, description, geoloc, ownerId, favouredBy, favourers, views, state, lastMessageDate, type, l);
        this.eventId = eventId;
        this.isFacebookEvent = true;

        website = "www.facebook.com/events/" + eventId;

        downloadAndFillEventDetails();
    }

    private void downloadAndFillEventDetails() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + this.eventId,
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
//                        Log.d(TAG, "onCompleted: event details ====\n" + response.toString());

                        JSONObject object = response.getJSONObject();

                        if(object == null)
                        {
                            Log.d(TAG, "onCompleted: Facebook ha fallito il download dei dati di " + FacebookEventSquare.this.name);
                            return;
                        }

                        try {

                            String startTime, endTime, finalTime;
                            // TODO String picture;
                            JSONObject location;

                            Date date;
                            SimpleDateFormat incomingFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                            SimpleDateFormat outgoingStartFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");
                            SimpleDateFormat outgoingEndFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");

                            Date endDate;
                            SimpleDateFormat serverEndFormat = new SimpleDateFormat("yyyy-MM-dd");

                            {
                                String jsonTime= object.getString("start_time");
                                // Il parsing mi restituisce un oggetto Data
                                date = incomingFormat.parse(jsonTime);
                                // Il format ritorna una stringa con il formato specificato nel costruttore
                                startTime = outgoingStartFormat.format(date);
                                Log.d(TAG, "onCompleted: " + startTime);
                            }

                            if(object.has("end_time"))
                            {
                                String jsonTime = object.getString("end_time");
                                date = incomingFormat.parse(jsonTime);
                                endTime = outgoingEndFormat.format(date);

                                String startIncipit = startTime.substring(0, startTime.indexOf(" at "));
                                String endIncipit = endTime.substring(0,endTime.indexOf(" at "));

                                if(startIncipit.equals(endIncipit))
                                {
                                    int stringLength = endTime.length();
                                    endTime = endTime.substring(stringLength-4, stringLength);
                                }

                                finalTime = startTime + " to " + endTime;
                            }else
                            {
                                endTime = "";
                                finalTime = startTime;
                            }

                            if(object.has("place"))
                            {
                                JSONObject place = object.getJSONObject("place");
                                if(place.has("location")){
                                    location = place.getJSONObject("location");
                                    street = location.getString("street").trim();
                                }
                            }

                            FacebookEventSquare.this.time = finalTime;

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }).executeAsync();
    }
}
