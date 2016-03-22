package com.nsqre.insquare.Utilities;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by mrsa on 21/03/2016.
 */
public class MessageDispatcherService extends Service {
    private static final String TAG = "MessageDispService";

    private List<Message> messages;
    private Socket mSocket;
    private Locale format;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        messages = new ArrayList<>();
        format = getResources().getConfiguration().locale;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if(intent.getBooleanExtra("stopservice", false)) {
            try {
                // Si connette alla socket che e' una sola
                // La Room viene gestita a livello server tramite socket.join(room)
                String url = getString(R.string.squaresUrl);
                Log.d(TAG, "onCreate: " + url);
                mSocket = IO.socket(url);

                mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

                mSocket.on("sendMessage", onSendMessage);
                mSocket.on("newMessage", onNewMessage);
                mSocket.on("ping", onPing);

            /*
            mSocket.on("typing", onTyping);
            mSocket.on("stop typing", onStopTyping);
            mSocket.on("login", onLogin);
            */

                mSocket.connect();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            mSocket.disconnect();
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.off("sendMessage", onSendMessage);
            mSocket.off("newMessage", onNewMessage);
            stopSelf();
        }
        return START_STICKY;
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, getString(R.string.error_connect));
                    try {
                        String url = getString(R.string.squaresUrl);
                        mSocket = IO.socket(url);

                        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

                        mSocket.on("sendMessage", onSendMessage);
                        mSocket.on("newMessage", onNewMessage);
                        mSocket.on("ping", onPing);

                        mSocket.connect();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            };
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener()
    {

        @Override
        public void call(final Object... args) {
            new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: First arg: " + args[0]);
                    JSONObject data = (JSONObject) args[0];
                    String username = "";
                    String message = "";
                    String userId = "";

                    try {
                        username = data.getString("username");
                        message = data.getString("contents");
                        userId = data.getString("userid");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    addMessage(new Message(message, username, userId, format));

                }
            };
        }
    };

    /**
     * Receives the event from socket for a new message sent and it displays it
     * @see #addMessage(Message)
     */
    private Emitter.Listener onSendMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    String room = "";
                    String userid = "";
                    String username = "";
                    String message = "";

                    try {
                        room = data.getString("room");
                        userid = data.getString("userid");
                        username = data.getString("username");
                        message = data.getString("contents");

                        Log.d(TAG, userid + " - " + username + " IN: " + room + " saying: " + message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    addMessage(new Message(message, username, userid, format));
                }
            };
        }
    };

    /**
     * Receives an event from socket to keep the connection alive(to not let it timeout)
     */
    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    mSocket.emit("pong", data);
                }
            };
        }
    };

    public void addMessage(Message m) {
        messages.add(m);
    }

    public void removeMessage() {
        messages.remove(0);
    }

}
