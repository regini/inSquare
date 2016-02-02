package com.nsqre.insquare.Activities;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.DividerItemDecoration;
import com.nsqre.insquare.Utilities.Message;
import com.nsqre.insquare.Utilities.MessageAdapter;
import com.nsqre.insquare.Utilities.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FabActivity extends AppCompatActivity {

    private static final long TYPING_TIMER_LENGTH = 600;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private EditText chatEditText;
    private EditText usernameEditText;
    private TextInputLayout textInputLayout;

    private Socket mSocket;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;
    private int mNumUsers;

    Dialog mDialog;

    private User currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUser = (User)getIntent().getSerializableExtra("CURRENT_USER");

        try {
            mSocket = IO.socket(getString(R.string.chatUrl));
            messageAdapter = new MessageAdapter(getDataSet());

            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.on("new message", onNewMessage);
            mSocket.on("user joined", onUserJoined);
            mSocket.on("user left", onUserLeft);
            mSocket.on("typing", onTyping);
            mSocket.on("stop typing", onStopTyping);
            mSocket.on("login", onLogin);

            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_fab);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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
        recyclerView.addItemDecoration(new DividerItemDecoration(this, null));

        chatEditText = (EditText) findViewById(R.id.message_text);

        chatEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mUsername == null) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private boolean validName()
    {
        if(usernameEditText.getText().toString().isEmpty())
        {
            textInputLayout.setError("Enter a username");
            usernameEditText.requestFocus();
            return false;
        }else
        {
            textInputLayout.setErrorEnabled(false);
        }

        return true;
    }

    //MODIFICATO invece di chiedere chi sta scrivendo, prende il nome dall'user passato dall'activity di login
    @Override
    protected void onStart() {
        super.onStart();

        mUsername = currentUser.getName();
        mSocket.emit("add user", mUsername);

        //Initialize mDialog
      /*  mDialog = new Dialog(this);
        mDialog.setContentView(R.layout.dialog_username);
        mDialog.setTitle("Enter username");
        mDialog.setCancelable(false);
        mDialog.show();

        usernameEditText = (EditText) mDialog.findViewById(R.id.et_username);
        textInputLayout = (TextInputLayout) mDialog.findViewById(R.id.input_layout_name);
        Button confirm = (Button) mDialog.findViewById(R.id.confirm_button);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validName()) {

                    mDialog.dismiss();
                }
            }
        });*/

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
        mSocket.off("login", onLogin);
    }

    private ArrayList<Message> getDataSet()
    {
        ArrayList<Message> list = new ArrayList<>();

        //TODO retrieve list of old messages stored in shareedprefs

        return list;
    }

    private void addMessage(String username, String message)
    {
        messageAdapter.addItem(new Message(Message.TYPE_MESSAGE, message, username) );
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void addTyping(String username)
    {
        messageAdapter.addItem(
                new Message( Message.TYPE_ACTION, "is typing...", username)
        );
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void removeTyping(String username)
    {

        for(int i = messageAdapter.getItemCount()-1; i >= 0; i--)
        {
            Message message = messageAdapter.getMessage(i);
            if(message.getMessageType() == Message.TYPE_ACTION && message.getSender().equals(username))
            {
                messageAdapter.removeItem(i);
                return;
            }
        }
    }

    private void addLog(String message)
    {
        messageAdapter.addItem( new Message(Message.TYPE_LOG, message, ""));
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void addParticipantsLog(int numUsers)
    {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void attemptSend()
    {
        if(mUsername == null) return;
        if(!mSocket.connected()) return;

        mTyping = false;

        String message = chatEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            chatEditText.requestFocus();
            return;
        }

        chatEditText.setText("");

        addMessage(mUsername, message);

        //This is the callback that socket.io uses to understand that an event has been triggered
        mSocket.emit("new message", message);
    }


    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.error_connect, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username="";
                    String message="";

                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    removeTyping(username);
                    addMessage(username, message);
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_joined, username));
                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_left, username));
                    addParticipantsLog(numUsers);
                    removeTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("stop typing");
        }
    };

    private Emitter.Listener onLogin = new Emitter.Listener()
    {
        @Override
        public void call(Object... args)
        {
            JSONObject data = (JSONObject) args[0];
            try {
                mNumUsers = data.getInt("numUsers");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
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
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.instfeedback:
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
                        RequestQueue queue = Volley.newRequestQueue(FabActivity.this);
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
                                        params.put("username", currentUser.getName());
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
}
