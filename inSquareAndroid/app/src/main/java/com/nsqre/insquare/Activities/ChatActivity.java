package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.gson.GsonBuilder;
import com.nsqre.insquare.ChatService;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.Message.MessageAdapter;
import com.nsqre.insquare.Message.MessageDeserializer;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This activity lets the user chat in a Square, using a socket.io chat
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.ChatMessageClickListener {

    private static final String TAG = "ChatActivity";
    
    private static final long TYPING_TIMER_LENGTH = 600;
    public static final int RECENT_MESSAGES_NUM = 50;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private EditText chatEditText;

    private Socket mSocket;

    /**
     * The InSquareProfile of the current user
     * @see InSquareProfile
     */
    private InSquareProfile mProfile;

    private Menu mMenu;

    private Square mSquare;
    private String mSquareId;
    private String mSquareName;
    private String mUsername;
    private String mUserId;

    private Tracker mTracker;
    private Locale format;

    //TODO dovrebbe cambiare il segnalino di invio messaggio in un segnalino di messaggio inviato
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            //se mettiamo degli extra nell'intent di chat service
            if (bundle != null) {
                //HO INVIATO IL MESSAGGIO
                Toast.makeText(ChatActivity.this, "Inviato dentro if", Toast.LENGTH_SHORT).show();
            }
            //se non mettiamo gli extra
            Toast.makeText(ChatActivity.this, "Inviato", Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Initializes the socket.io components, downloads the messages present in the chat and eventually puts to zero the
     * notification counter for this chat
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        format = getResources().getConfiguration().locale;

        try {

            // Si connette alla socket che e' una sola
            // La Room viene gestita a livello server tramite socket.join(room)
            String url = getString(R.string.squaresUrl);
            Log.d(TAG, "onCreate: " + url);
            mSocket = IO.socket(url);

            messageAdapter = new MessageAdapter(getApplicationContext());
            messageAdapter.setOnClickListener(this);

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

        setContentView(R.layout.activity_chat);

        ImageButton imageButton = (ImageButton) findViewById(R.id.chat_send_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
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

        mSquare = (Square) intent.getSerializableExtra(MapFragment.SQUARE_TAG);
        Log.d("CHAT", mSquare.toString());

        mSquareId = mSquare.getId();
        mSquareName = mSquare.getName();

        // Get Messaggi recenti
        getRecentMessages(RECENT_MESSAGES_NUM);

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        if(sharedPreferences.contains(mSquareId)) {
            sharedPreferences.edit().remove(mSquareId).apply();
            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount",0) - 1).apply();
        }
        sharedPreferences.edit().putString("actualSquare", mSquareId).apply();

    }

    /**
     * Creates a Volley request to download the messages present in a particular square, then it adds the results to the
     * view
     * @param quantity
     */
    private void getRecentMessages(int quantity) {
        // TODO Muovere dentro VolleyManager
        RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);
        final String q = new Integer(quantity).toString();

        String url = String.format("%1$s?recent=%2$s&size=%3$s&square=%4$s",
                getString(R.string.messagesUrl),"true", q, mSquareId);

        Log.d(TAG, "getRecentMessages from: " + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Log.d(TAG + "GETRECENTI", response);
                        GsonBuilder b = new GsonBuilder();
                        // MessageDeserializer specifica come popolare l'oggetto Message fromJson
                        b.registerTypeAdapter(Message.class, new MessageDeserializer(format));
                        Gson gson = b.create();
                        Message[] messages = gson.fromJson(response, Message[].class);
                        Collections.reverse(Arrays.asList(messages));
                        for (Message m : messages) {
                            addMessage(m);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "GETRECENTI Error: " + error.toString());
            }
        });
        queue.add(stringRequest);
    }


    /**
     * Initializes some values and emits an event so the server knows the user is connected to the chat
     */
    @Override
    protected void onStart() {
        super.onStart();


        setTitle(mSquareName);

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

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        registerReceiver(messageReceiver, new IntentFilter(ChatService.NOTIFICATION));
    }

    /**
     * TODO
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("event");
            Log.d("messageReceiver", "Got message: " + message);
            if("deletion".equals(intent.getStringExtra("action"))) {
                if(mSquareId.equals(intent.getStringExtra("squareId"))) {
                    messageAdapter.clear();
                    findViewById(R.id.removed_text).setVisibility(View.VISIBLE);
                    chatEditText.setFocusable(false);
                }
            }
        }
    };

    /**
     * TODO
     */
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        sharedPreferences.edit().remove("actualSquare").apply();

        unregisterReceiver(messageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    /**
     * Adds a message to the view
     * @param m The message you want to add
     */
    private void addMessage(Message m)
    {
        messageAdapter.addItem(m);
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    /**
     * Attempts to send a message throught a socket event, if the message is valid
     */
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

        String message = chatEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            chatEditText.requestFocus();
            Log.d(TAG, "attemptSend: the message you're trying to send is empty");

            return;
        }

        chatEditText.setText("");

        Intent intent = new Intent(this, ChatService.class);
        intent.putExtra("squareid", mSquareId);
        intent.putExtra("username", mUsername);
        intent.putExtra("userid", mUserId);
        intent.putExtra("message", message);
        startService(intent);

        addMessage(new Message(message, mUsername, mUserId, format));  //TODO ora deve esserci un'icona di invio in corso
    }


    /**
     * TODO sistemare
     * Notifies the user if the connection to the socket has failed
     */
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

    /**
     * Receives the event from socket for a new message to display and it displays it
     * @see #addMessage(Message)
     */
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

                    addMessage(new Message(message, username, userId, format));

                }
            });
        }
    };

    /**
     * Receives the event from socket for a new message sent and it displays it
     * @see #addMessage(Message)
     */
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

                    addMessage(new Message(message, username, userid, format));
                }
            });
        }
    };

    /**
     * Receives an event from socket to keep the connection alive(to not let it timeout)
     */
    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    mSocket.emit("pong", data);
                }
            });
        }
    };

    public Socket getmSocket() {
        return mSocket;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_chat_actions, menu);

        mMenu = menu;

//        if (mProfile.favouriteSquaresList.contains(mSquare))
        if(InSquareProfile.isFav(mSquare.getId()))
            menu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_white);
        else menu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_border_white);

        return true;
    }

    /**
     * Manages the option menu items
     * @param item The item selected
     * TODO @see la richiesta a feedback
     * @see #favouriteSquare(int, Square)
     */
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
                        // TODO Muovere dentro VolleyManager
                        final String feedback = feedbackText.getText().toString();
                        final String activity = this.getClass().getSimpleName();
                        // Instantiate the RequestQueue.
                        RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);
                        String url = getString(R.string.feedbackUrl);

                        // Request a string response from the provided URL.
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d(TAG, "VOLLEY ServerResponse: "+response);
                                        CharSequence text = getString(R.string.thanks_feedback);
                                        int duration = Toast.LENGTH_SHORT;
                                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                                        toast.show();
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d(TAG, "VOLLEY " + error.toString());
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
            case R.id.favourite_square_action:
//                if (mProfile.favouriteSquaresList.contains(mSquare)) {
                if(InSquareProfile.isFav(mSquare.getId()))
                {
                    favouriteSquare(Request.Method.DELETE, mSquare);
                } else {
                    favouriteSquare(Request.Method.POST, mSquare);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Creates a Volley request to put/remove a square in/from the favourite squares list, then calls updateList
     * @param method The volley method you want to use(POST to add, DELETE to remove
     * @param square The square you want to add/remove
     * @see #updateList(int, Square)
     */
    public void favouriteSquare(final int method, final Square square) {
        // TODO Muovere dentro VolleyManager
        RequestQueue queue = Volley.newRequestQueue(this);
        final String squareId = square.getId();
        final String userId = InSquareProfile.getUserId();

        String url = String.format("%1$s?squareId=%2$s&userId=%3$s",
                getString(R.string.favouritesquaresUrl), squareId, userId);;

        Log.d(TAG, "favouriteSquare: " + url);
        StringRequest postRequest = new StringRequest(method, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        updateList(method, square);
                        Log.d(TAG, "FAVOURITE response => " + response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "FAVOURITE error => "+error.toString());
                    }
                }
        );
        queue.add(postRequest);
    }

    /**
     * Updates the list of favourite squares
     * @param method The volley method used
     * @param square The square to add/remove
     */
    public void updateList (int method, Square square) {
        // Checking the house is not empty!
        if (method == Request.Method.DELETE) {
            InSquareProfile.removeFav(square.getId());
            mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_border_white);

        } else {
            InSquareProfile.addFav(square);
            mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_white);
        }
    }

    @Override
    public void onItemClick(int position, View v) {
        // TODO implementare onclick behavior per i messaggi nella chat
        Log.d(TAG, "onItemClick: I've just clicked item " + position);
    }
}
