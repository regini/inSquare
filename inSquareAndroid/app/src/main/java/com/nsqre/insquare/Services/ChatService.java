package com.nsqre.insquare.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;

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
    private String mSquareId;
    
    public ChatService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Log.d(TAG, "Service onStartCommand: intent is null? " + (intent==null));
        //Log.d(TAG, "onStartCommand: questo è il service " + this.toString());
        if (intent != null) {
            Log.d(TAG, "onHandleIntent: " + this.getClass().getName());
            mSquareId = intent.getStringExtra("squareid");
            Message message = (Message) intent.getSerializableExtra("message");
            String mUsername = message.getName();
            String mUserId = message.getFrom();
            String messageText = message.getText();

            JSONObject data = new JSONObject();

            try {
                data.put("room", mSquareId);
                data.put("username", mUsername);
                data.put("userid", mUserId);
                data.put("message", messageText);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMessage(data, message);
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage(final JSONObject data, final Message m) {
        try {
            String url = getString(R.string.socket);
            mSocket = IO.socket(url);
            mSocket.connect();

            if (!mSocket.connected()) {
                throw new Exception("socket non connesso");
            }
            mSocket.emit("sendMessage", data, new Ack() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        Log.d(TAG, "call: ho avuto acknowledgement per: " + args[0].toString());
                        JSONObject data = (JSONObject) args[0];
                        String messageId = "";
                        try {
                            messageId = data.getString("msg_id");

                            if(!"".equals(messageId)) {
                                m.setId(messageId);
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                        publishResults(m);
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
                                sendMessage(data, m);
                            }
                        },
                        5000);
            }
            else {
                Log.d(TAG, "sendMessage: messaggio non inviato per num tentativi troppo alto");
            }
        }
    }

    //notifica che l'invio è stato effettuato
    private void publishResults(Message m) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("messageSent", m);
        InSquareProfile profile = InSquareProfile.getInstance(getApplicationContext());
        Log.d(TAG, "publishResults: " + profile.getOutgoingMessages());
        Log.d(TAG, "publishResults: " + mSquareId);
        profile.removeOutgoing(mSquareId, getApplicationContext());
        sendBroadcast(intent);
        stopSelf();
    }
}
