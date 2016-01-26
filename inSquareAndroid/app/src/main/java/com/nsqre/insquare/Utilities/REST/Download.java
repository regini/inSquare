package com.nsqre.insquare.Utilities.REST;/* Created by umbertosonnino on 20/1/16  */

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class Download extends AsyncTask<String, Void, ArrayList> {

    private static final String TAG = "Download";
    private static final String URL = "http://insquare-bettorius.rhcloud.com/get_all_messages";

    private ArrayList<Message> messageList;

    /* PARSER THINGS */
    private static final String TAG_ID = "_id";
    private static final String TAG_ARRAY = "messages";
    private static final String TAG_SQUARE_ID = "squareId";
    private static final String TAG_SENDER_ID = "senderId";
    private static final String TAG_CREATED_AT = "createdAt";
    private static final String TAG_TEXT = "text";

    @Override
    protected ArrayList doInBackground(String... params) {

        messageList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        try
        {
            java.net.URL url = new URL(URL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;

            while((str = in.readLine()) != null)
            {
                Log.d(TAG, str);
                sb.append(str).append("\n");
            }

            Log.d(TAG, "Results are IN!\n" + sb.toString());

            JSONObject json = new JSONObject(sb.toString());
            JSONArray messages = json.getJSONArray(TAG_ARRAY);

            for(int i = 0; i< messages.length(); i++)
            {
                JSONObject jsonObject = messages.getJSONObject(i);

                String id       = jsonObject.getString(TAG_ID);
                String square   = jsonObject.getString(TAG_SQUARE_ID);
                String sender   = jsonObject.getString(TAG_SENDER_ID);
                String date     = jsonObject.getString(TAG_CREATED_AT);
                String text     = jsonObject.getString(TAG_TEXT);

                if(!square.isEmpty() || !sender.isEmpty() || !text.isEmpty())
                {
                    Message message = new Message(
                            id,
                            square,
                            sender,
                            date,
                            text
                    );

                    messageList.add(message);
                }


            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return messageList;
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
