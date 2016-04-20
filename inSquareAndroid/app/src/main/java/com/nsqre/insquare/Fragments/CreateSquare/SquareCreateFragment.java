package com.nsqre.insquare.Fragments.CreateSquare;


import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.nsqre.insquare.Activities.CreateIntroActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class SquareCreateFragment extends Fragment {

    private static final String TAG = "CreateSquareActivity";
    private static SquareCreateFragment instance;

    boolean created = false;

    private Bitmap mapScreenshot;

    private Toolbar toolbar;

    private FrameLayout sectionPhoto;

    private LinearLayout sectionUserData;
    private ImageView mapImage;
    private ImageView mapOverlay;
    private CircleImageView squarePicture;
    private ImageButton takePictureButton;

    private TextInputEditText newSquareName;
    private TextInputEditText newSquareDescription;

    // Sezione Data e Ora
    private LinearLayout sectionDateTime;
    private Button dateButtonPicker;
    private Button timeButtonPicker;

    // Sezione Facebook
    private LinearLayout sectionFacebook;
    private Button facebookConnectButton;
    private EditText facebookUrl;

    private int currentYear, currentMonth, currentDay;
    private int currentHour, currentMinute;

    private String expireString;

    private DatePickerDialog dpd;
    private TimePickerDialog tpd;

    // For Facebook Handling
    private CallbackManager fbCallbackManager;
    private String fbAccessToken;
    private CreateIntroActivity father;
    private TextView warningText;

    public SquareCreateFragment() {
        // Required empty public constructor
    }

    public static SquareCreateFragment newInstance() {

        if(instance == null)
        {
            instance = new SquareCreateFragment();
        }

        return instance;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.father = (CreateIntroActivity) getActivity();
        this.expireString = "";

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_square_create, container, false);

        setupMainContent(v);
        setupFacebookLink(v);

        return v;
    }

    public void setLayoutType()
    {

        Log.d(TAG, "setLayoutType: " + father.squareType.toString());

        switch (father.squareType) {
            default:
            case TYPE_PLACE:
                sectionDateTime.setVisibility(View.GONE);
                sectionFacebook.setVisibility(View.GONE);
                warningText.setVisibility(View.GONE);

                changeStatusBarColor(R.color.colorAccent);
                break;
            case TYPE_EVENT:
                sectionDateTime.setVisibility(View.VISIBLE);
                sectionFacebook.setVisibility(View.VISIBLE);
                warningText.setVisibility(View.GONE);
                ((TextInputLayout)facebookUrl.getParent()).setHint("URL Facebook Event");
                changeStatusBarColor(R.color.md_deep_purple_600);
                break;
            case TYPE_SHOP:
                sectionFacebook.setVisibility(View.VISIBLE);
                sectionDateTime.setVisibility(View.GONE);
                warningText.setVisibility(View.GONE);
                changeStatusBarColor(R.color.md_green_600);
                ((TextInputLayout)facebookUrl.getParent()).setHint("URL Facebook Page");
                break;
        }

    }

    private void changeStatusBarColor(int color)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.setStatusBarColor(ContextCompat.getColor(getContext(), color));
        }
    }

    private void setupUpperSection(View v,int layoutType)
    {
        mapImage = (ImageView) v.findViewById(R.id.create_square_map_image);
        mapImage.setImageBitmap(mapScreenshot);

        mapOverlay = (ImageView) v.findViewById(R.id.create_square_image_overlay);

        squarePicture = (CircleImageView) v.findViewById(R.id.create_square_circle_image);
        takePictureButton = (ImageButton) v.findViewById(R.id.create_square_add_picture_button);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO riempire con la possibilita' di fare una foto
            }
        });

        squarePicture.setVisibility(View.GONE);
        takePictureButton.setVisibility(View.GONE);

//        sectionPhoto = (FrameLayout)mapImage.getParent();

        switch (layoutType)
        {
            case 0:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.red_transparent_overlay));
                break;
            case 1:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.purple_transparent_overlay));
                break;
            case 2:
                mapOverlay.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.green_transparent_overlay));
                break;
        }

    }

    private void setupMainContent(View v)
    {
        sectionUserData = (LinearLayout) v.findViewById(R.id.create_square_userdata_section);
        newSquareName = (TextInputEditText) v.findViewById(R.id.create_square_name);
        newSquareDescription = (TextInputEditText) v.findViewById(R.id.create_square_description);

        newSquareName.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count)
                    {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                        if(s.length() > 0){
                            father.circularReveal(father.nextButton);
                        }else
                        {
                            father.nextButton.setVisibility(View.GONE);
                        }
                    }
                }
        );

        sectionDateTime = (LinearLayout) v.findViewById(R.id.create_square_date_time_section);
        dateButtonPicker = (Button) v.findViewById(R.id.create_square_date_button);
        timeButtonPicker = (Button) v.findViewById(R.id.create_square_time_button);

        warningText = (TextView) v.findViewById(R.id.create_square_warning);
        warningText.setVisibility(View.GONE);

        final Calendar c = Calendar.getInstance();
        currentYear = c.get(Calendar.YEAR);
        currentMonth = c.get(Calendar.MONTH);
        currentDay = c.get(Calendar.DAY_OF_MONTH);
        currentHour = c.get(Calendar.HOUR_OF_DAY);
        currentMinute = c.get(Calendar.MINUTE);

        Log.d(TAG, "setupMainContent: current day: " + currentDay + " currentMonth: " + currentMonth);

        // Dialog per la selezione dell'orario
        dpd = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String text = "";
                        monthOfYear += 1;

                        text = dayOfMonth + "/" + monthOfYear + "/" + year;

                        dateButtonPicker.setText(text);
                        warningText.setVisibility(View.VISIBLE);

                        expireString = year + "-" + monthOfYear + "-" + dayOfMonth;
                        Log.d(TAG, "onDateSet: expireString " + expireString);
                    }
                }, currentYear, currentMonth, currentDay);

        dpd.getDatePicker().setMinDate(System.currentTimeMillis());


        dateButtonPicker.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dpd.show();
                    }
                }
        );

        tpd = new TimePickerDialog(getContext(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String text = "";
                        text = hourOfDay + ":" + minute;

                        timeButtonPicker.setText(text);
                        warningText.setVisibility(View.VISIBLE);

                        String resData = getContext().getString(R.string.create_square_button_date);

                        String data = dateButtonPicker.getText().equals(resData) ? ""
                                : dateButtonPicker.getText().toString().trim();
                        if(data.isEmpty())
                        {
                            expireString = currentYear + "-" + (currentMonth+1) + "-" + currentDay;
                            data = (currentDay+1) + "/" + (currentMonth+1) + "/" + currentYear;
                        }
                        dateButtonPicker.setText(data);
                    }
                }, currentHour, currentMinute,
                android.text.format.DateFormat.is24HourFormat(getContext())
        );

        timeButtonPicker.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tpd.show();
                    }
                }
        );

    }


    public void setupFacebookLink(View v)
    {
        sectionFacebook = (LinearLayout) v.findViewById(R.id.create_square_facebook_section);
        facebookUrl = (EditText) v.findViewById(R.id.create_square_facebook_link_field);

        facebookConnectButton = (Button) v.findViewById(R.id.create_square_facebook_link_button);
        facebookConnectButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ChooseCreateFragment.SQUARE_TYPE requestType = father.squareType;

                        String url = getAndCheckURL();
                        if (url == null) return;

                        if(AccessToken.getCurrentAccessToken() == null)
                        {
                            setupFacebook(v);
                            LoginManager.getInstance().logInWithReadPermissions(getActivity(),
                                    Arrays.asList("public_profile", "email", "user_friends"));
                        }else if(requestType == ChooseCreateFragment.SQUARE_TYPE.TYPE_EVENT)
                        {
                            requestFacebookEventDetail(url);
                        }else if (requestType == ChooseCreateFragment.SQUARE_TYPE.TYPE_SHOP)
                        {
                            requestFacebookPageDetail(url);
                        }
                    }
                }
        );
    }

    public boolean validLink()
    {
        boolean validLink = !facebookUrl.getText().toString().trim().isEmpty();

        if(!validLink)
        {
            ((TextInputLayout) facebookUrl.getParent()).setErrorEnabled(true);
            ((TextInputLayout) facebookUrl.getParent()).setError("Il nome non può essere vuoto");
        }

        return validLink;
    }

    public boolean validName()
    {
        boolean validName = !newSquareName.getText().toString().trim().isEmpty();
        if(validName) {
            ((TextInputLayout) newSquareName.getParent()).setErrorEnabled(true);
            ((TextInputLayout) newSquareName.getParent()).setError("Il nome non può essere vuoto");
        }
        return validName;
    }

    private void setupFacebook(final View v)
    {
        fbCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(fbCallbackManager,
                new FacebookCallback<LoginResult>() {
                    View main = v.findViewById(android.R.id.content);
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
        final ChooseCreateFragment.SQUARE_TYPE requestType = father.squareType;
        Log.d(TAG, "requestFacebookUserData: Sto richiedendo le informazioni da Facebook");
        // Creazione di una nuova richiesta al grafo di Facebook per le informazioni necessarie
        GraphRequest graphRequest = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response)
            {
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
                    InSquareProfile.save(getContext());

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
                                        Toast.makeText(getContext(), "Qualcosa non ha funzionato con il token di " + serviceName, Toast.LENGTH_SHORT).show();
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

                String url = getAndCheckURL();
                if(url == null) return;

                switch (requestType) {
                    case TYPE_EVENT:
                        requestFacebookEventDetail(url);
                        break;
                    case TYPE_SHOP:
                        requestFacebookPageDetail(url);
                        break;
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "id,name,gender,email,picture");
        graphRequest.setParameters(params);
        graphRequest.executeAsync();
    }

    private void requestFacebookPageDetail(String url)
    {
        String pageId = extractPageName(url);
        if(pageId.isEmpty())
        {
            Toast.makeText(getContext(), "Il link inserito non e' valido", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "requestFacebookPageDetail: trying to get the page details of " + pageId);

        Bundle requestParams = new Bundle();
        requestParams.putString("fields", "name,fan_count,description,price_range,hours,phone,location,website");

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/v2.6/" + pageId, requestParams, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject object = response.getJSONObject();
                        Log.d(TAG, "onCompleted: page details ====\n" + response.toString());

                        if(father.pagerAdapter.getCount() > 1 && object.isNull("error") && object != null)
                        {
                            ReviewCreateFragment reviewFragment = (ReviewCreateFragment) father.pagerAdapter.getItem(2);

                            try {

                                String id, name, description, likes,price, phone, website;
                                String street, latitude, longitude;
                                id = name = description = likes = price = phone = website = street = latitude = longitude = "";
                                JSONObject location, hours;

                                id = object.getString("id").trim();

                                // Basic Info
                                name = object.getString("name").trim();
                                if(object.has("description")) {
                                    description = object.getString("description").trim();
                                }

                                if(object.has("fan_count")) {
                                    likes = object.getString("fan_count").trim();
                                }
                                if(object.has("price_range")) {
                                    price = object.getString("price_range").trim();
                                }
                                if(object.has("phone")) {
                                    phone = object.getString("phone").trim();
                                }
                                if(object.has("website")) {
                                    website = object.getString("website").trim();
                                }

                                // Location
                                if(object.has("location")) {
                                    location = object.getJSONObject("location");
                                    street = location.getString("street").trim();
                                    latitude = location.getString("latitude").trim();
                                    longitude = location.getString("longitude").trim();
                                }

                                List<String> hoursList = new ArrayList<>();

                                // Hours
                                if(object.has("hours")) {
                                    hours = object.getJSONObject("hours");
                                    Iterator<String> keys = hours.keys();

                                    while (keys.hasNext()) {
                                        String listValue = "";

                                        String keyDay = keys.next();
                                        String valueOpen = hours.getString(keyDay);

                                        String keyDayOpen = keyDay.split("_")[0];
                                        String capitalized = keyDayOpen.substring(0, 1).toUpperCase() + keyDayOpen.substring(1);

                                        listValue += capitalized + ": " + valueOpen;

                                        if (keys.hasNext()) {
                                            // Ha anche una controparte per la chiusura
                                            String valueClose = hours.getString(keys.next());
                                            listValue += (" - " + valueClose);
                                        }
                                        hoursList.add(listValue.trim());
                                    }
                                }

                                reviewFragment.setupShopInfo(
                                        name, price,
                                        description, likes, website, phone, street, hoursList,
                                        id, latitude, longitude
                                );

                                displayNextButton();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        else
                        {
                            Log.d(TAG, "onCompleted: Qualcosa non era in grado di funzionare..!");
                        }
                    }
                }

        ).executeAsync();
    }

    public void displayNextButton()
    {
        father.circularReveal(father.nextButton);
        father.vpager.setCurrentItem(2, true);
    }

    private void requestFacebookEventDetail(String url)
    {
        String eventId = extractEventId(url);
        if(eventId.isEmpty())
        {
            Toast.makeText(getContext(), "L'Id non e' valido", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "requestFacebookEventDetail: trying to obtain details of " + eventId);
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/" + eventId,
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        Log.d(TAG, "onCompleted: event details ====\n" + response.toString());

                        JSONObject object = response.getJSONObject();

                        if(object == null)
                        {
                            Toast.makeText(getContext(), "Facebook ha fallito...riprova?", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(father.pagerAdapter.getCount() > 1 && object.isNull("error") && object != null) {
                            ReviewCreateFragment reviewFragment = (ReviewCreateFragment) father.pagerAdapter.getItem(2);

                            try {
                                String id;
                                String name, description;
                                name = description = "";
                                String street, latitude, longitude;
                                street = latitude = longitude = "";
                                String startTime, endTime, finalTime;
                                // TODO String picture;
                                JSONObject location;

                                id = object.getString("id").trim();
                                name = object.getString("name").trim();
                                if(object.has("description"))
                                {
                                    description = object.getString("description").trim();
                                }

                                Date date;
                                SimpleDateFormat incomingFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                                SimpleDateFormat outgoingStartFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");
                                SimpleDateFormat outgoingEndFormat = new SimpleDateFormat("EEE, d MMM 'at' HH:mm");

                                Date endDate;
                                SimpleDateFormat serverEndFormat = new SimpleDateFormat("yyyy-MM-dd");

                                {
                                    String jsonTime= object.getString("start_time");
                                    // Il parsing mi restituisce un oggetto Data
                                    date = incomingFormat.parse(jsonTime);
                                    // Il format ritorna una stringa con il formato specificato nel costruttore
                                    startTime = outgoingStartFormat.format(date);
                                    Log.d(TAG, "onCompleted: " + startTime);
                                }

                                if(object.has("end_time"))
                                {
                                    String jsonTime = object.getString("end_time");
                                    date = incomingFormat.parse(jsonTime);
                                    endTime = outgoingEndFormat.format(date);

                                    endDate = serverEndFormat.parse(jsonTime);
                                    expireString = serverEndFormat.format(endDate);

                                    Log.d(TAG, "Parsed end_time for expireString: " + expireString);

                                    String startIncipit = startTime.substring(0, startTime.indexOf(" at "));
                                    String endIncipit = endTime.substring(0,endTime.indexOf(" at "));

                                    if(startIncipit.equals(endIncipit))
                                    {
                                        int stringLength = endTime.length();
                                        endTime = endTime.substring(stringLength-4, stringLength);
                                    }

                                    finalTime = startTime + " to " + endTime;
                                }else
                                {
                                    endTime = "";
                                    finalTime = startTime;
                                }

                                if(object.has("place"))
                                {
                                    JSONObject place = object.getJSONObject("place");
                                    if(place.has("location")){
                                        location = place.getJSONObject("location");
                                        street = location.getString("street").trim();
                                        latitude = location.getString("latitude").trim();
                                        longitude = location.getString("longitude").trim();
                                    }
                                }

                                String website = "www.facebook.com/events/" + id;
                                reviewFragment.setupEventInfo(name, description, finalTime, street, website, id, latitude, longitude, expireString);

                                displayNextButton();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                });
        request.executeAsync();
    }

    @Nullable
    private String getAndCheckURL() {
        String url = facebookUrl.getText().toString();
        if(url.isEmpty() || !url.toLowerCase().contains("facebook"))
        {
            ((TextInputLayout)facebookUrl.getParent()).setErrorEnabled(true);
            ((TextInputLayout)facebookUrl.getParent()).setError("Inserisci un link valido!");
            return null;
        }
        return url;
    }

    private String extractEventId(String eventUrl) {
        String eventId = "";
        String[] tokens = eventUrl.split("/");
        for(int i = 0; i < tokens.length - 1; i++)
        {
            if(tokens[i].toLowerCase().equals("events"))
            {
                eventId = tokens[i+1];
                break;
            }
        }
        eventId = eventId.split("\\?")[0];
        Log.d(TAG, "extractPageName: " + StringUtil.isNumeric(eventId));

        return StringUtil.isNumeric(eventId) ? eventId : "";
    }


    private String extractPageName(String pageUrl)
    {
        String pageId = "";
        String[] tokens = pageUrl.split("/");

        for (int i = 0; i < tokens.length - 1; i++)
        {
            if(tokens[i].toLowerCase().contains("facebook"))
            {
                pageId = tokens[i+1];
                Log.d(TAG, "extractPageName: I've just found the pageName as " + pageId);
                break;
            }
        }

        tokens = pageId.split("-");
        pageId = tokens[tokens.length-1];

        Log.d(TAG, "extractPageName: " + StringUtil.isNumeric(pageId));

        return StringUtil.isNumeric(pageId) ? pageId : "";
    }

    public String getInsertedName()
    {
        return this.newSquareName.getText().toString();
    }

    public String getInsertedDescription()
    {
        return this.newSquareDescription.getText().toString();
    }

    public String getExpireTime()
    {
        return this.expireString;
    }

    public String getEventTime() {
        String eventTime = "";

        String resOra = getContext().getString(R.string.create_square_button_time);
        String resData = getContext().getString(R.string.create_square_button_date);

        String data = this.dateButtonPicker.getText().equals(resData) ? ""
                : this.dateButtonPicker.getText().toString().trim();
        String ora = this.timeButtonPicker.getText().equals(resOra) ? ""
                : this.timeButtonPicker.getText().toString().trim();

        if(data.isEmpty())
        {
            return "";
        }else
        {
            try {
                SimpleDateFormat niceFormatting = new SimpleDateFormat("EEE, d MMM");
                SimpleDateFormat eventDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                // eventDateFormat estrapola le informazioni dalla data
                Date date = eventDateFormat.parse(data);
                // niceFormatting crea una visualizzazione piu' adatta per le informazioni
                eventTime += niceFormatting.format(date);
                if(!ora.isEmpty())
                {

                    SimpleDateFormat eventTimeFormat = new SimpleDateFormat("HH:mm");
                    date = eventTimeFormat.parse(ora);
                    eventTime += " at " + eventTimeFormat.format(date);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


        return eventTime;
    }
}
