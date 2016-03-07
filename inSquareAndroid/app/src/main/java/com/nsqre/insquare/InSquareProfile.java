package com.nsqre.insquare;/* Created by umbertosonnino on 5/1/16  */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nsqre.insquare.Utilities.Square;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class InSquareProfile {

    private static final String TAG = "InSquareProfile";

    private static final String USER_ID_KEY = "USER_ID_KEY";
    private static final String USERNAME_KEY = "USERNAME_KEY";
    private static final String EMAIL_KEY = "EMAIL_KEY";
    private static final String PICTURE_URL_KEY = "PICTURE_URL_KEY";
    private static final String OWNED_SQUARES_KEY = "OWNED_SQUARES_KEY";
    private static final String FAVOURITE_SQUARES_KEY = "FAVOURITE_SQUARES_KEY";
    private static final String RECENT_SQUARES_KEY = "RECENT_SQUARES_KEY";

    private static final String FACEBOOK_ID_KEY = "FACEBOOK_ID_KEY";
    private static final String FACEBOOK_TOKEN_KEY = "FACEBOOK_TOKEN_KEY";
    private static final String FACEBOOK_EMAIL_KEY = "FACEBOOK_EMAIL_KEY";
    private static final String FACEBOOK_NAME_KEY = "FACEBOOK_NAME_KEY";

    private static final String GOOGLE_ID_KEY = "GOOGLE_ID_KEY";
    private static final String GOOGLE_TOKEN_KEY = "GOOGLE_TOKEN_KEY";
    private static final String GOOGLE_EMAIL_KEY = "GOOGLE_EMAIL_KEY";
    private static final String GOOGLE_NAME_KEY = "GOOGLE_NAME_KEY";


    public static ArrayList<Square> ownedSquaresList;
    public static ArrayList<Square> favouriteSquaresList;
    public static ArrayList<Square> recentSquaresList;

    public static String userId;
    public static String username;
    public static String email;
    public static String pictureUrl;

    public static String facebookId;
    public static String facebookToken;
    public static String facebookEmail;
    public static String facebookName;

    public static String googleId;
    public static String googleToken;
    public static String googleEmail;
    public static String googleName;

    private static InSquareProfile profile;

    private InSquareProfile()
    {
        // TODO: Check for data in SharedPrefs upon instantiation

    }

    public static InSquareProfile getInstance(Context c)
    {
        if(profile != null)
        {
            return profile;
        }

        Gson gs = new Gson();
        Type type = new TypeToken<ArrayList<Square>>(){}.getType();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        userId = prefs.getString(USER_ID_KEY, null);
        username = prefs.getString(USERNAME_KEY, null);
        email = prefs.getString(EMAIL_KEY, null);
        pictureUrl = prefs.getString(PICTURE_URL_KEY, null);
        ownedSquaresList = gs.fromJson(prefs.getString(OWNED_SQUARES_KEY, null), type);
        favouriteSquaresList = gs.fromJson(prefs.getString(FAVOURITE_SQUARES_KEY, null), type);
        recentSquaresList = gs.fromJson(prefs.getString(RECENT_SQUARES_KEY, null), type);

        facebookId = prefs.getString(FACEBOOK_ID_KEY, null);
        facebookToken = prefs.getString(FACEBOOK_TOKEN_KEY, null);
        facebookEmail = prefs.getString(FACEBOOK_EMAIL_KEY, null);
        facebookName = prefs.getString(FACEBOOK_NAME_KEY, null);

        googleId    = prefs.getString(GOOGLE_ID_KEY, null);
        googleToken = prefs.getString(GOOGLE_TOKEN_KEY, null);
        googleEmail = prefs.getString(GOOGLE_EMAIL_KEY, null);
        googleName  = prefs.getString(GOOGLE_NAME_KEY, null);

        return new InSquareProfile();
    }

    public void save(Context c)
    {
        String NAME = c.getString(R.string.app_name);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
        Gson gs = new Gson();

        editor.putString(USER_ID_KEY, userId);
        editor.putString(USERNAME_KEY, username);
        editor.putString(EMAIL_KEY, email);
        editor.putString(PICTURE_URL_KEY, pictureUrl );
        editor.putString(OWNED_SQUARES_KEY, gs.toJson(ownedSquaresList));
        editor.putString(FAVOURITE_SQUARES_KEY, gs.toJson(favouriteSquaresList));
        editor.putString(RECENT_SQUARES_KEY, gs.toJson(recentSquaresList));

        editor.putString(FACEBOOK_ID_KEY, facebookId);
        editor.putString(FACEBOOK_TOKEN_KEY, facebookToken);
        editor.putString(FACEBOOK_EMAIL_KEY, facebookEmail);
        editor.putString(FACEBOOK_NAME_KEY, facebookName);

        editor.putString(GOOGLE_ID_KEY, googleId);
        editor.putString(GOOGLE_TOKEN_KEY, googleToken);
        editor.putString(GOOGLE_EMAIL_KEY, googleEmail);
        editor.putString(GOOGLE_NAME_KEY, googleName);

        editor.commit();

        Log.d(TAG, this.toString());
    }

    public static String getUsername()
    {
        // TODO dovrebbe return un HashMap
        if(username != null)
        {
            return username;
        }else if(googleName != null)
        {
            return googleName;
        }
        else if(facebookName != null)
        {
            return facebookName;
        }

        Log.d(TAG, "getUsername: Current username is empty");

        return null;
    }

    public static String getUserId()
    {
        if(userId != null)
        {
            return userId;
        }else if(googleId != null)
        {
            Log.d(TAG, "getUserId: google id!");
            return googleId;
        }
        else if(facebookId != null)
        {
            Log.d(TAG, "getUserId: facebook id!");
            return facebookId;
        }

        Log.d(TAG, "getUserId: Current username is empty");

        return null;
    }

    public static String getPictureUrl() {
        return pictureUrl;
    }

    public static boolean hasLoginData() {
        return userId!= null && username!= null && email != null;
    }

    @Override
    public String toString() {
        return "==== USER  ====" +
                "\nID: " + userId +
                "\nUsername: " + username +
                "\nEmail: " + email +
                "\nPicture URL: " + pictureUrl +
                "\n==== FACEBOOK  ====" +
                "\nID: " + facebookId +
                "\nEmail: " + facebookEmail +
                "\nName : " + facebookName +
                "\n==== GOOGLE ====" +
                "\nID: " + googleId +
                "\nEmail: " + googleEmail +
                "\nName: " + googleEmail;

    }

    public static boolean isFavourite(String mSquareId) {
        for(Square s: favouriteSquaresList)
        {
            if(s.getId().equals(mSquareId))
                return true;
        }
        return false;
    }
}
