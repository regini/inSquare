package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.AnalyticsApplication;
import com.nsqre.insquare.Utilities.Message;
import com.nsqre.insquare.Utilities.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.ChatMessageClickListener{

    private static final String TAG = "ChatActivity";
    
    private static final long TYPING_TIMER_LENGTH = 600;
    public static final int RECENT_MESSAGES_NUM = 50;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private EditText chatEditText;
    private EditText usernameEditText;
    private TextInputLayout textInputLayout;
    private Toolbar toolbar;

    private Socket mSocket;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private int mNumUsers;

    Dialog mDialog;

    private InSquareProfile mProfile;

    private String mSquareId;
    private String mSquareName;
    private String mUsername;
    private String mUserId;

    private Tracker mTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());


        try {

            // Si connette alla socket che e' una sola
            // La Room viene gestita a livello server tramite socket.join(room)
            String url = getString(R.string.chatUrl);
            Log.d(TAG, "onCreate: " + url);
            mSocket = IO.socket(url);

            messageAdapter = new MessageAdapter(getApplicationContext());
            messageAdapter.setOnClickListener(this);

            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

            mSocket.on("sendMessage", onSendMessage);
            mSocket.on("newMessage", onNewMessage);

            /*
            mSocket.on("typing", onTyping);
            mSocket.on("stop typing", onStopTyping);
            mSocket.on("login", onLogin);
            */

            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_chat);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.chat_send_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.message_list);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, null));

        chatEditText = (EditText) findViewById(R.id.message_text);

        // Recuperiamo i dati passati dalla MapActivity
        Intent intent = getIntent();

        mSquareId = intent.getStringExtra(MapActivity.SQUARE_ID_TAG);
        mSquareName = intent.getStringExtra(MapActivity.SQUARE_NAME_TAG);

        // Get Messaggi recenti
        getRecentMessages(RECENT_MESSAGES_NUM);
    }

    private void getRecentMessages(int quantity) {
        RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);
        final String q = new Integer(quantity).toString();

        String url = String.format("http://recapp-insquare.rhcloud.com/messages?recent=%1$s&size=%2$s&square=%3$s",
                "true", q, mSquareId);

        Log.d(TAG, "getRecentMessages from: " + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("GETRECENTI", response);
                        Gson gson = new Gson();
                        Message[] messages = gson.fromJson(response, Message[].class);
                        Collections.reverse(Arrays.asList(messages));
                        for (Message m : messages) {
                            addMessage(m.getText(), m.getName(), m.getFrom());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("GETRECENTI", error.toString());
            }
        });
        queue.add(stringRequest);
    }


    //MODIFICATO invece di chiedere chi sta scrivendo, prende il nome dall'user passato dall'activity di login
    @Override
    protected void onStart() {
        super.onStart();

        setTitle("#" + mSquareName);

        Log.d(TAG, "onCreate: " + mSquareId);
        Log.d(TAG, "onCreate: " + mSquareName);

        mUsername = InSquareProfile.getUsername();
        mUserId = InSquareProfile.getUserId();

        JSONObject data = new JSONObject();

        try{
            data.put("room", mSquareId);
            data.put("username", mUsername);
            data.put("userid", mUserId);
            data.put("message", mUsername + " joined");
        } catch(JSONException e)
        {
            e.printStackTrace();
        }

        mSocket.emit("addUser", data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("sendMessage", onSendMessage);
        mSocket.off("newMessage", onNewMessage);
        
        /*
            mSocket.off("typing", onTyping);
            mSocket.off("stop typing", onStopTyping);
            mSocket.off("login", onLogin);
        */
    }

    private void addMessage(String message, String username, String userId)
    {
        messageAdapter.addItem(new Message(Message.TYPE_MESSAGE, message, username, userId));
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void attemptSend()
    {
        // [START message_event]
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Send Message")
                .build());
        // [END message_event]

        if(mUsername == null) 
        {
            Log.d(TAG, "attemptSend: there's no Username specified");
            return;
        }
        if(!mSocket.connected()) 
        {
            Log.d(TAG, "attemptSend: Socket is not connected");
            return;
        }

//        mTyping = false;

        String message = chatEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            chatEditText.requestFocus();
            Log.d(TAG, "attemptSend: the message you're trying to send is empty");

            return;
        }

        chatEditText.setText("");

        // TODO aggiungere una coda di messaggi da mandare quando non ci sta una buona connessione
        addMessage(message, mUsername, mUserId);

        // Il server riceve un oggetto in JSON che deve essere processato
        JSONObject data = new JSONObject();

        try{
            data.put("room", mSquareId);
            data.put("username", mUsername);
            data.put("userid", mUserId);
            data.put("message", message);
        }catch(JSONException e)
        {
            e.printStackTrace();
        }

        Log.d("profilo", InSquareProfile.getInstance(getApplicationContext()).toString());
        //This is the callback that socket.io uses to understand that an event has been triggered
        mSocket.emit("sendMessage", data);
    }


    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.error_connect, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, getString(R.string.error_connect));
                }
            });
        }
    };


    // TODO SOLLEVA ERRORE  java.lang.ClassCastException: java.lang.String cannot be cast to org.json.JSONObject ANCHE SE NESSUNO LO CHIAMA (?)
    private Emitter.Listener onNewMessage = new Emitter.Listener()
    {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
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

                    addMessage(message, username, userId);

                }
            });
        }
    };
    private Emitter.Listener onSendMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
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

                    addMessage(message, username, userid);
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.instfeedback:
                // [START feedback_event]
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Feedback")
                        .build());
                // [END feedback_event]
                final Dialog d = new Dialog(this);
                d.setContentView(R.layout.dialog_feedback);
                d.setTitle("Feedback");
                d.setCancelable(true);
                d.show();

                final EditText feedbackText = (EditText) d.findViewById(R.id.dialog_feedbacktext);
                Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String feedback = feedbackText.getText().toString();
                        final String activity = this.getClass().getSimpleName();
                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);
                        String url = "http://recapp-insquare.rhcloud.com/feedback";

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("VOLLEY","ServerResponse: "+response);
                                        CharSequence text = getString(R.string.thanks_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("VOLLEY", error.toString());
                                        CharSequence text = getString(R.string.error_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }) {
                                    @Override
                                    protected Map<String, String> getParams() {
                                        Map<String, String> params = new HashMap<String, String>();
                                        params.put("feedback", feedback);
                                        params.put("username", mUsername);
                                        params.put("activity", activity);
                                        return params;
                                    }

                        };
                        queue.add(stringRequest);
                        d.dismiss();
                    }
                });
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(int position, View v) {
        // TODO implementare onclick behavior per i messaggi nella chat
        Log.d(TAG, "onItemClick: I've just clicked item " + position);
    }
}
