package com.nsqre.insquare;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


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
    
    public ChatService() {
        super("ChatService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.d(TAG, "onHandleIntent: " + this.getClass().getName());
            try {
                String url = "http://recapp-insquare.rhcloud.com/squares";
                mSocket = IO.socket(url);
                mSocket.connect();

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

                Log.d(TAG, "profilo " + InSquareProfile.printProfile());
                //This is the callback that socket.io uses to understand that an event has been triggered
                mSocket.emit("sendMessage", data);
                //TODO aspettare la notifica del server
                //se c'è
                publishResults();
                //se non c'è
                //TODO
            }
            catch (Exception e) {
                Log.d(TAG, "ChatService: errore");
            }
        }
    }

    //notifica che l'invio è stato effettuato
    private void publishResults() {
        Intent intent = new Intent(NOTIFICATION);
        sendBroadcast(intent);
    }
}
