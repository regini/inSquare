package com.nsqre.insquare.Utilities.REST;/* Created by umbertosonnino on 20/1/16  */

import android.os.AsyncTask;
import android.util.Log;

import com.nsqre.insquare.Utilities.Square;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class DownloadClosestSquares extends AsyncTask<String, Void, HashMap<String, Square>> {

    private static final String TAG = "DownloadClosestSquares";
    private static final String BASE_URL = "http://recapp-insquare.rhcloud.com/squares?";
    private String FINAL_URL;

//    private ArrayList<Square> squareList;
    private HashMap<String, Square> squareMap;
    private String fulltext;
    private String name;
    // TODO save this as a param in the settings
    private String distance;
    private double latitude, longitude;

    /* PARSER THINGS */
    private static final String TAG_ID = "_id";
    private static final String TAG_INDEX = "_index";
    private static final String TAG_TYPE = "_type";
    private static final String TAG_SCORE = "_score";
    private static final String TAG_SOURCE = "_source";
    private static final String TAG_SOURCE_NAME= "name";
    private static final String TAG_SOURCE_GEO_LOC= "geo_loc";
    private static final String TAG_SORT = "sort";

    public DownloadClosestSquares(String fulltext, String name) {
        this.fulltext = fulltext;
        this.name = name;

        this.FINAL_URL = BASE_URL + "fulltext=" + fulltext + "&name=" + name;
    }

    public DownloadClosestSquares(String name) {
        this.name = name;
        this.FINAL_URL = BASE_URL + "name=" + name;
    }

    public DownloadClosestSquares(String distance, double latitude, double longitude) {
        this.distance = distance;
        this.latitude = latitude;
        this.longitude = longitude;
        this.FINAL_URL = BASE_URL + "distance=" + distance + "&lat=" + latitude + "&lon=" + longitude;
    }

    @Override
    protected HashMap<String, Square> doInBackground(String... params)
    {
//        squareList = new ArrayList<>();
        squareMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();

        try
        {
            Log.d(TAG, "Trying to get:" + FINAL_URL);
            java.net.URL url = new URL(FINAL_URL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;

            while((str = in.readLine()) != null)
            {
//                Log.d(TAG, str);
                sb.append(str).append("\n");
            }

//            Log.d(TAG, "Results are IN!\n" + sb.toString());

            JSONArray closeSquares = new JSONArray(sb.toString());
            int i = 0;
            for(i = 0; i < closeSquares.length(); i++) {
                JSONObject cs = closeSquares.getJSONObject(i);

                String cs_index = cs.getString(TAG_INDEX);
                String cs_type = cs.getString(TAG_TYPE);
                String cs_id = cs.getString(TAG_ID);
                String cs_score = cs.getString(TAG_SCORE);

                JSONObject cs_source = cs.getJSONObject(TAG_SOURCE);
                String cs_source_name = cs_source.getString(TAG_SOURCE_NAME);
                String cs_source_loc = cs_source.getString(TAG_SOURCE_GEO_LOC);
                String[] location = cs_source_loc.split(",");
                double lat = Double.parseDouble(location[0]);
                double lon = Double.parseDouble(location[1]);

                // TODO if needed list of messages can be added here
//                String cs_sort          = cs.getString(TAG_SORT);

                if (!cs_id.isEmpty() || !cs_source_name.isEmpty() || !cs_source_loc.isEmpty())
                {
                    Square close_square = new Square(
                            cs_id,
                            cs_source_name,
                            lat,
                            lon,
                            cs_type
                    );

//                    squareList.add(close_square);
                    squareMap.put(cs_source_name, close_square);
                }
            }

            Log.d(TAG, squareMap.size() + " vs " + i);

//            JSONObject json = new JSONObject(sb.toString());
//            JSONArray messages = json.getJSONArray(TAG_ARRAY);
//
//            for(int i = 0; i< messages.length(); i++)
//            {
//                JSONObject jsonObject = messages.getJSONObject(i);
//
//                String id       = jsonObject.getString(TAG_ID);
//                String square   = jsonObject.getString(TAG_SQUARE_ID);
//                String sender   = jsonObject.getString(TAG_SENDER_ID);
//                String date     = jsonObject.getString(TAG_CREATED_AT);
//                String text     = jsonObject.getString(TAG_TEXT);
//
//                if(!square.isEmpty() || !sender.isEmpty() || !text.isEmpty())
//                {
//                    Message message = new Message(
//                            id,
//                            square,
//                            sender,
//                            date,
//                            text
//                    );
//
//                    squareList.add(message);
//                }
//            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return squareMap;
    }

    public class Message
    {
        String id;
        String squareId;
        String senderId;
        String createdAt;
        String text;

        public Message(String id, String sqId, String sendId, String crea, String t)
        {
            this.id = id;
            this.squareId = sqId;
            this.senderId = sendId;
            this.createdAt = crea;
            this.text = t;
        }

        public String getSquareId() {
            return squareId;
        }

        public String getSenderId() {
            return senderId;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getText() {
            return text;
        }

        public String getId() {
            return id;
        }
    }

}