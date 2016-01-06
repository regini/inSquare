package com.nsqre.insquare.Activities;

import android.app.Dialog;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Utilities.DividerItemDecoration;
import com.nsqre.insquare.Utilities.Message;
import com.nsqre.insquare.Utilities.MessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mSocket = IO.socket("http://chat.socket.io");
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

    @Override
    protected void onStart() {
        super.onStart();

        //Initialize mDialog
        mDialog = new Dialog(this);
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
                    mUsername = usernameEditText.getText().toString().trim();
                    mSocket.emit("add user", mUsername);
                    mDialog.dismiss();
                }
            }
        });

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
                new Message( Message.TYPE_ACTION, "", username)
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


}
