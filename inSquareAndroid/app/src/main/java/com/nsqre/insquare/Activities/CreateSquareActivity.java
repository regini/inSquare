package com.nsqre.insquare.Activities;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.DialogHandler;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateSquareActivity extends AppCompatActivity
{

    private static final String TAG = "CreateSquareActivity";
    boolean created = false;

    private Bitmap mapScreenshot;

    private Toolbar toolbar;

    private FrameLayout upperSection;
    private ImageView mapImage;
    private ImageView mapOverlay;
    private CircleImageView squarePicture;
    private ImageButton takePictureButton;

    private LinearLayout eventDetailSection;
    private TextInputEditText newSquareName;
    private TextInputEditText newSquareDescription;
    private Button timeButtonPicker;

    private Button facebookConnectButton;
    private EditText facebookUrl;
    private int currentYear;
    private int currentMonth;
    private int currentDay;
    private DatePickerDialog dpd;

    // For Facebook Handling
    private CallbackManager fbCallbackManager;
    private String fbAccessToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutType = 0;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            layoutType = extras.getInt(DialogHandler.SQUARE_TYPE);
            byte[] array = extras.getByteArray(DialogHandler.MAP_SCREENSHOT);
            mapScreenshot = BitmapFactory.decodeByteArray(array, 0, array.length);
            if (array.length > 0)
                Log.d(TAG, "onCreate: just received the screenshot..?");
        }

        switch (layoutType) {
            case 0: // Luogo
                setContentView(R.layout.create_square_place);
                setupToolbar();
                setupUpperSection(layoutType);
                setupMainContent();
                break;
            case 1: // Evento
                setContentView(R.layout.create_square_event);
                setupToolbar();
                setupMainContent();
                setupUpperSection(layoutType);
                setupFacebookLink();
                break;
            case 2: // Att. Commerciale
                break;
        }

    }


    private void setupToolbar()
    {
        toolbar = (Toolbar) findViewById(R.id.create_square_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        Drawable cancelIcon = ContextCompat.getDrawable(this, R.drawable.ic_clear_white_48dp);
        Bitmap bitmap = ((BitmapDrawable)cancelIcon).getBitmap();
        int side = (bitmap.getWidth())*2/3;
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, side, side, false);
        getSupportActionBar().setHomeAsUpIndicator(new BitmapDrawable(getResources(), resized));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.md_purple_800));
        }

        toolbar.setVisibility(View.INVISIBLE);

    }

    private void setupUpperSection(int layoutType)
    {
        mapImage = (ImageView) findViewById(R.id.create_square_map_image);
        mapImage.setImageBitmap(mapScreenshot);

        mapOverlay = (ImageView) findViewById(R.id.create_square_image_overlay);

        squarePicture = (CircleImageView) findViewById(R.id.create_square_circle_image);
        takePictureButton = (ImageButton) findViewById(R.id.create_square_add_picture_button);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO riempire con la possibilita' di fare una foto
                    }
                });

        squarePicture.setVisibility(View.GONE);
        takePictureButton.setVisibility(View.GONE);

        upperSection = (FrameLayout)mapImage.getParent();

        switch (layoutType)
        {
            case 0:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.red_transparent_overlay));
                break;
            case 1:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_transparent_overlay));
                break;
            case 2:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.green_transparent_overlay));
                break;
        }

    }

    private void setupMainContent()
    {

        newSquareName = (TextInputEditText) findViewById(R.id.create_square_name);
        newSquareDescription = (TextInputEditText) findViewById(R.id.create_square_description);
        timeButtonPicker = (Button) findViewById(R.id.create_square_date_button);
        timeButtonPicker.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dpd.show();
                    }
                }
        );
        eventDetailSection = (LinearLayout) newSquareName.getParent().getParent();
        eventDetailSection.setVisibility(View.INVISIBLE);

        final Calendar c = Calendar.getInstance();
        currentYear = c.get(Calendar.YEAR);
        currentMonth = c.get(Calendar.MONTH);
        currentDay = c.get(Calendar.DAY_OF_MONTH);

        Log.d(TAG, "setupMainContent: current day: " + currentDay + " currentMonth: " + currentMonth);

        // Dialog per la selezione dell'orario
        dpd = new DatePickerDialog(CreateSquareActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String text = "";
                        monthOfYear += 1;
                        if(year == currentYear) {
                            text = dayOfMonth + "/" + monthOfYear;
                        }else {
                            text = dayOfMonth + "/" + monthOfYear + "/" + year;
                        }
                        timeButtonPicker.setText(text);
                    }
                }, currentYear, currentMonth, currentDay);

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.created = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus && created) {
            circularReveal(facebookConnectButton).start();
            circularReveal(facebookUrl).start();
            circularReveal(eventDetailSection).start();
            circularReveal(toolbar).start();
            circularReveal(upperSection).start();
            created = false;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Animator circularReveal(View v)
    {
        int cx = v.getMeasuredWidth() / 2;
        int cy = v.getMeasuredHeight() / 2;

        int finalRadius = Math.max(v.getWidth(), v.getHeight()) / 2;

        Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
        v.setVisibility(View.VISIBLE);
        return anim;

    }

    public void setupFacebookLink()
    {
        facebookUrl = (EditText) findViewById(R.id.create_square_facebook_link_field);

        facebookConnectButton = (Button) findViewById(R.id.create_square_facebook_link_button);
        facebookConnectButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO Implementare richiesta al facebook graph
                        if(AccessToken.getCurrentAccessToken() == null)
                        {
                            setupFacebook();
                            LoginManager.getInstance().logInWithReadPermissions(CreateSquareActivity.this,
                                    Arrays.asList("public_profile", "email", "user_friends"));
                        }else
                        {
                            requestFacebookEventDetail();
                        }
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_create_square_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.done_action:
                if(validName())
                {
                    Log.d(TAG, "onOptionsItemSelected: let's go forward!");
                }

                break;
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected: I'm trying to go back!");
                super.onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

        private boolean validName()
        {
            boolean valid = !newSquareName.getText().toString().trim().isEmpty();
            if(!valid) {
                ((TextInputLayout) newSquareName.getParent()).setErrorEnabled(true);
                ((TextInputLayout) newSquareName.getParent()).setError("Il nome non può essere vuoto");
            }
            return valid;
        }

        private void setupFacebook()
        {
            fbCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().registerCallback(fbCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        View main = findViewById(android.R.id.content);
                        Snackbar errorMessage = Snackbar.make(main, "Non posso collegare Facebook senza login!", Snackbar.LENGTH_LONG);
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            requestFacebookUserData();
                        }

                        @Override
                        public void onCancel() {
                            errorMessage.show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            errorMessage.show();
                        }
                    }
            );
        }

        private void requestFacebookUserData()
        {
            Log.d(TAG, "requestFacebookUserData: Sto richiedendo le informazioni da Facebook");
            // Creazione di una nuova richiesta al grafo di Facebook per le informazioni necessarie
            GraphRequest graphRequest = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    Log.d(TAG, "Hello Facebook!\n" + response.toString());

                    try {
                        String nome = object.getString("name");
                        String email = object.getString("email");
                        String gender = object.getString("gender");
                        String id = object.getString("id");

                        fbAccessToken = AccessToken.getCurrentAccessToken().getToken();

                        InSquareProfile.facebookName = nome;
                        InSquareProfile.facebookEmail = email;
                        InSquareProfile.facebookId = id;
                        InSquareProfile.facebookToken = fbAccessToken;
                        InSquareProfile.save(getApplicationContext());

                        final String serviceName = "Facebook";

                        VolleyManager.getInstance().patchLoginToken(
                                serviceName,
                                fbAccessToken,
                                InSquareProfile.getUserId(),
                                nome,
                                email,
                                "",
                                new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        // Vuoto - PATCH Request
                                    }

                                    @Override
                                    public void responsePOST(Object object) {
                                        // Vuoto - PATCH Request
                                    }

                                    @Override
                                    public void responsePATCH(Object object) {
                                        boolean success = (boolean)object;
                                        if (!success) {
                                            Toast.makeText(CreateSquareActivity.this, "Qualcosa non ha funzionato con il token di " + serviceName, Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Vuoto - PATCH Request
                                    }
                                });
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }

                    requestFacebookEventDetail();
                }
            });
            Bundle params = new Bundle();
            params.putString("fields", "id,name,gender,email,picture");
            graphRequest.setParameters(params);
            graphRequest.executeAsync();
        }

        private void requestFacebookEventDetail()
        {
            String text = facebookUrl.getText().toString();
            if(text.isEmpty())
            {
                ((TextInputLayout)facebookUrl.getParent()).setError("Inserisci un link!");
                return;
            }

            String eventId = extractEventId(text);
            Log.d(TAG, "requestFacebookEventDetail: trying to obtain details of " + eventId);
            new GraphRequest(AccessToken.getCurrentAccessToken(),
                    "/" + eventId, null, HttpMethod.GET,
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            Log.d(TAG, "onCompleted: " + response.toString());
                        }
                    }).executeAsync();
        }

        private String extractEventId(String text) {
            String eventId = "";
            String[] tokens = text.split("/");
            for(int i = 0; i < tokens.length - 1; i++)
            {
                if(tokens[i].toLowerCase().equals("events"))
                {
                    eventId = tokens[i+1];
                    break;
                }
            }

            return eventId.split("\\?")[0];
        }
}
