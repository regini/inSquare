package com.nsqre.insquare.Activities;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.nsqre.insquare.Services.ChatService;
import com.nsqre.insquare.Fragments.MapFragment;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Message.Message;
import com.nsqre.insquare.Message.MessageAdapter;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.Utilities.Analytics.AnalyticsApplication;
import com.nsqre.insquare.Utilities.Photo.helpers.DocumentHelper;
import com.nsqre.insquare.Utilities.Photo.helpers.IntentHelper;
import com.nsqre.insquare.Utilities.Photo.imgurmodel.ImageResponse;
import com.nsqre.insquare.Utilities.Photo.imgurmodel.Upload;
import com.nsqre.insquare.Utilities.Photo.services.UploadService;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * This activity lets the user chat in a Square, using a socket.io chat
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.ChatMessageClickListener,
        GoogleApiClient.OnConnectionFailedListener {

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

    private boolean isScrolled;

    private Tracker mTracker;
    private Locale format;



    private Upload upload; // Upload object containging image and meta data
    private File chosenFile; //chosen file from intent


    //SHARE
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_INVITE = 0;

    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;

    //TODO dovrebbe cambiare il segnalino di invio messaggio in un segnalino di messaggio inviato
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            //se mettiamo degli extra nell'intent di chat service
            if (bundle != null) {
                //HO INVIATO IL MESSAGGIO
                Log.d(TAG, "onReceive: messaggio inviato con chatservice");
            }
            //se non mettiamo gli extra
        }
    };

    //TODO aggiungere slider "nuovi messaggi" se sto guardando messaggi vecchi, risolvere problema download messaggi
    /**
     * Initializes the socket.io components, downloads the messages present in the chat and eventually puts to zero the
     * notification counter for this chat
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FOTO
        //ButterKnife.bind(this);

        //ANALYTICS
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        format = getResources().getConfiguration().locale;

        isScrolled = false;

        messageAdapter = new MessageAdapter(getApplicationContext());
        messageAdapter.setOnClickListener(this);

        setContentView(R.layout.activity_chat);

        ImageButton sendButton = (ImageButton) findViewById(R.id.chat_send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend();
            }
        });

        //Foto
        ImageButton uploadImage = (ImageButton) findViewById(R.id.chat_foto_button);
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertPhotoWrapper();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, this)
                .build();

        // Check for App Invite invitations and launch deep-link activity if possible.
        // Requires that an Activity is registered in AndroidManifest.xml to handle
        // deep-link URLs.
        boolean autoLaunchDeepLink = true;
        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this, autoLaunchDeepLink)
                .setResultCallback(
                        new ResultCallback<AppInviteInvitationResult>() {
                            @Override
                            public void onResult(AppInviteInvitationResult result) {
                                Log.d(TAG, "getInvitation:onResult:" + result.getStatus());
                                // Because autoLaunchDeepLink = true we don't have to do anything
                                // here, but we could set that to false and manually choose
                                // an Activity to launch to handle the deep link here.
                            }
                        });


        //FINE SHARE

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

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        if(sharedPreferences.contains(mSquareId)) {
            sharedPreferences.edit().remove(mSquareId).apply();
            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount",0) - 1).apply();
        }
    }

    private void insertPhotoWrapper() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("WRITE Storage");
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionsNeeded.add("READ Storage");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }

        onChooseImage();
    }

    public void onChooseImage() {
        IntentHelper.chooseFileIntent(this);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_INVITE:
                if (resultCode == RESULT_OK) {
                    // Check how many invitations were sent and log a message
                    // The ids array contains the unique invitation ids for each invitation sent
                    // (one for each contact select by the user). You can use these for analytics
                    // as the ID will be consistent on the sending and receiving devices.
                    String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                    Log.d(TAG, getString(R.string.sent_invitations_fmt, ids.length));
                } else {
                    // Sending failed or it was canceled, show failure message to the user
                    //showMessage(getString(R.string.send_failed));
                }
                break;
            case IntentHelper.FILE_PICK:
                Uri returnUri;

                if (resultCode != RESULT_OK) {
                    return;
                }

                returnUri = data.getData();
                String filePath = DocumentHelper.getPath(this, returnUri);
                //Safety check to prevent null pointer exception
                if (filePath == null || filePath.isEmpty()) return;
                chosenFile = new File(filePath);
                uploadImage();
                break;
        }
    }
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    onChooseImage();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Creates a Volley request to download the messages present in a particular square, then it adds the results to the
     * view
     * @param quantity
     */
    private void getRecentMessages(int quantity) {

        final String q = new Integer(quantity).toString();

        VolleyManager.getInstance().getRecentMessages("true", q, mSquareId,
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        if (object == null) {
                            Toast.makeText(ChatActivity.this, "Non sono riuscito ad ottenere i messaggi recenti!", Toast.LENGTH_SHORT).show();
                        } else {

                            ArrayList<Message> messages = (ArrayList<Message>) object;
                            for (Message m : messages) {
                                addMessage(m);
                            }
                        }
                    }

                    @Override
                    public void responsePOST(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // Vuoto -- GET Request
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        // Vuoto -- GET Request
                    }
                });
    }


    /**
     * Initializes some values and emits an event so the server knows the user is connected to the chat
     */
    @Override
    protected void onStart() {
        super.onStart();

        setTitle(mSquareName);
        ColorDrawable toolbarColor = new ColorDrawable(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        getSupportActionBar().setBackgroundDrawable(toolbarColor);

        Log.d(TAG, "onCreate: " + mSquareId);
        Log.d(TAG, "onCreate: " + mSquareName);

    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("update_squares"));
        mTracker.setScreenName(this.getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        SharedPreferences sharedPreferences = getSharedPreferences("NOTIFICATION_MAP", MODE_PRIVATE);
        sharedPreferences.edit().putString("actualSquare", mSquareId).apply();

        try {
            String url = getString(R.string.squaresUrl);
            Log.d(TAG, "onCreate: " + url);
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

        // Get Messaggi recenti
        getRecentMessages(RECENT_MESSAGES_NUM);

        mUsername = InSquareProfile.getUsername();
        mUserId = InSquareProfile.getUserId();

        JSONObject data = new JSONObject();

        try {
            data.put("room", mSquareId);
            data.put("username", mUsername);
            data.put("userid", mUserId);
            data.put("message", mUsername + " joined");
        } catch(JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("addUser", data);

        final View rootView = this.getWindow().getDecorView(); // this = activity
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(isScrolled) {
                    isScrolled = false;
                    return;
                }
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getHeight();
                int heightDifference = screenHeight - (r.bottom - r.top);

                if (heightDifference > screenHeight / 3) {
                    recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    isScrolled = true;
                }
            }
        });
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
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("sendMessage", onSendMessage);
        mSocket.off("newMessage", onNewMessage);
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
    private void addMessage(Message m) {
        if(m.getId() != null && messageAdapter.contains(m)) {
            return;
        } else {
            messageAdapter.addItem(m);
        }
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        isScrolled = true;
    }

    /**
     * Attempts to send a message throught a socket event, if the message is valid
     */
    private void attemptSend() {
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
        /*
        if(!mSocket.connected())
        {
            Log.d(TAG, "attemptSend: Socket is not connected");
            return;
        }
        */
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
     * Attempts to send a foto throught a socket event, if the message is valid
     */
    private void attemptSendFoto(String fotoURL) {
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
        /*
        if(!mSocket.connected())
        {
            Log.d(TAG, "attemptSend: Socket is not connected");
            return;
        }
        */
        String message = fotoURL;
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
     * TODO TENERE ANCORA SOLO PER TEST
     * Notifies the user if the connection to the socket has failed
     */
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, getString(R.string.error_connect));
                    Toast.makeText(getApplicationContext(), getString(R.string.error_connect), Toast.LENGTH_SHORT).show();
                    /*
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
                    */
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_chat_actions, menu);

        mMenu = menu;

        //SHARE

       //menu.findItem(R.id.share_action).setIcon(R.drawable.ic_share_white_48dp);

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
            case R.id.menu_entry_feedback:
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

                final EditText feedbackEditText = (EditText) d.findViewById(R.id.dialog_feedbacktext);

                final String feedback = feedbackEditText.getText().toString().trim();
                final String activity = this.getClass().getSimpleName();

                Button confirm = (Button) d.findViewById(R.id.dialog_feedback_confirm_button);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        VolleyManager.getInstance().postFeedback(
                                feedback,
                                InSquareProfile.getUserId(),
                                activity,
                                new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        // Vuoto - POST Request
                                    }

                                    @Override
                                    public void responsePOST(Object object) {
                                        if (object == null) {
                                            Toast.makeText(ChatActivity.this, "Non sono riuscito ad inviare il feedback", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ChatActivity.this, "Feedback inviato con successo!", Toast.LENGTH_SHORT).show();
                                            d.dismiss();
                                        }
                                    }

                                    @Override
                                    public void responsePATCH(Object object) {
                                        // Vuoto - POST Request
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Vuoto - POST Request
                                    }
                                }
                        );
                    }
                });
                break;
            case R.id.favourite_square_action:
                if(InSquareProfile.isFav(mSquare.getId()))
                {
                    favouriteSquare(Request.Method.DELETE, mSquare);
                } else {
                    favouriteSquare(Request.Method.POST, mSquare);
                }
                break;
            case R.id.share_action:
                Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                        .setMessage(getString(R.string.invitation_message))
                        .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                        .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                        .setCallToActionText(getString(R.string.invitation_cta))
                        .build();
                startActivityForResult(intent, REQUEST_INVITE);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Creates a Volley request to put/remove a square in/from the favourite squares list, then calls updateList
     * @param method The volley method you want to use(POST to add, DELETE to remove
     * @param square The square you want to add/remove
     */
    public void favouriteSquare(final int method, final Square square) {

        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if (object == null) {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        } else {
                            InSquareProfile.addFav(square);
                            mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_white);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        InSquareProfile.removeFav(square.getId());
                        mMenu.findItem(R.id.favourite_square_action).setIcon(R.drawable.heart_border_white);
                    }
                });
    }

    @Override
    public void onItemClick(int position, View v) {
        // TODO implementare onclick behavior per i messaggi nella chat
        Log.d(TAG, "onItemClick: I've just clicked item " + position);
    }


    public void uploadImage() {
    /*
      Create the @Upload object
     */
        if (chosenFile == null) return;
        createUpload(chosenFile);

    /*
      Start upload
     */
        new UploadService(this).Execute(upload, new UiCallback());

    }

    private void createUpload(File image) {
        Log.d("createUpload", "createUpload");
        upload = new Upload();

        upload.image = image;
    }

    private void clearInput() {
        Log.d("clearInput", "clearInput");
        //uploadImage.setImageResource(R.drawable.ic_add_a_photo_black_48dp);
    }

    private class UiCallback implements Callback<ImageResponse> {

        @Override
        public void success(ImageResponse imageResponse, Response response) {
            attemptSendFoto(imageResponse.data.link);
            Log.d("success", imageResponse.data.link);
            clearInput();
        }

        @Override
        public void failure(RetrofitError error) {
            Log.d("failure", "failure");
            //Assume we have no connection, since error is null
            if (error == null) {
                Log.d("ERROR UiCallback", "ERROR UiCallback");
            }
        }
    }

    //SHARE
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        //showMessage(getString(R.string.google_play_services_error));
    }

    /*
    private void showMessage(String msg) {
        ViewGroup container = (ViewGroup) findViewById(R.id.snackbar_layout);
        Snackbar.make(container, msg, Snackbar.LENGTH_SHORT).show();
    }

    */
}
