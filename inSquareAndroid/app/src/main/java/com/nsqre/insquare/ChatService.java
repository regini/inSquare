package com.nsqre.insquare;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class ChatService extends Service {

    private static final String TAG = "ChatService";

    public static final String NOTIFICATION = "notification";
    private Socket mSocket;
    private final int MAX_RETRY = 10;
    private int retryNumber;
    
    public ChatService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Log.d(TAG, "Service onStartCommand: intent is null? " + (intent==null));
        //Log.d(TAG, "onStartCommand: questo è il service " + this.toString());
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
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage(final JSONObject data) {
        try {
            String url = "http://recapp-insquare.rhcloud.com/squares";
            mSocket = IO.socket(url);
            mSocket.connect();

            if (!mSocket.connected()) {
                throw new Exception("socket non connesso");
            }
            mSocket.emit("sendMessage", data, new Ack() {
                @Override
                //TODO gestire la notifica del server
                public void call(Object... args) {
                    if (args.length > 0) {
                        //args[0] è un JSONObject ed ha gli stessi dati inviati
                        Log.d(TAG, "call: ho avuto acknowledgement per: " + args[0].toString());
                        publishResults();
                    }
                }
            });
        }
        catch (Exception e) {
            Log.d(TAG, "sendMessage: " + e.toString());
            retryNumber++;
            if (retryNumber < MAX_RETRY) {
                new Timer().schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                Log.d(TAG, "run: Eseguo sendmessage tentativo num: " + retryNumber);
                                sendMessage(data);
                            }
                        },
                        5000);
            }
            else {
                //TODO NON c'è notifica dal server
                Log.d(TAG, "sendMessage: messaggio non inviato per num tentativi troppo alto");
            }
        }
    }

    //notifica che l'invio è stato effettuato
    private void publishResults() {
        Intent intent = new Intent(NOTIFICATION);
        sendBroadcast(intent);
    }
}
