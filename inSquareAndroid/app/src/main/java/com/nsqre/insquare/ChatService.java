package com.nsqre.insquare;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;


import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class ChatService extends IntentService {

    private static final String TAG = "ChatService";
    public static final String NOTIFICATION = "notification";
    private Socket mSocket;
    private final int MAX_RETRY = 5;
    private int retryNumber;
    
    public ChatService() {
        super("ChatService");
        retryNumber = 0;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.d(TAG, "onHandleIntent: " + this.getClass().getName());
            String mSquareId = intent.getStringExtra("squareid");
            String mUsername = intent.getStringExtra("username");
            String mUserId = intent.getStringExtra("userid");
            String message = intent.getStringExtra("message");

            JSONObject data = new JSONObject();

            try {
                data.put("room", mSquareId);
                data.put("username", mUsername);
                data.put("userid", mUserId);
                data.put("message", message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessage(data);
        }
    }

    private void sendMessage(final JSONObject data) {
        try {
            String url = "http://recapp-insquare.rhcloud.com/squares";
            mSocket = IO.socket(url);
            mSocket.connect();

            if (!mSocket.connected()) {
                throw new Exception("socket non connesso");
            }
            mSocket.emit("sendMessage", data);
            //TODO aspettare la notifica del server
            //se c'è
            publishResults();
            //se non c'è
            //TODO
        }
        catch (Exception e) {
            Log.d(TAG, "sendMessage: " + e.toString());
            retryNumber++;
            if (retryNumber < MAX_RETRY) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendMessage(data);
                    }
                }, 3000);
            }
            else {
                Toast.makeText(getApplicationContext(), "messaggio non inviato", Toast.LENGTH_SHORT);
            }
        }
    }

    //notifica che l'invio è stato effettuato
    private void publishResults() {
        Intent intent = new Intent(NOTIFICATION);
        sendBroadcast(intent);
    }
}
