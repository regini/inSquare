package com.nsqre.insquare;/* Created by umbertosonnino on 5/1/16  */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class InSquareProfile {

    private static final String TAG = "InSquareProfile";

    private static final String USER_ID_KEY = "USER_ID_KEY";
    private static final String USERNAME_KEY = "USERNAME_KEY";
    private static final String EMAIL_KEY = "EMAIL_KEY";
    private static final String PICTURE_URL_KEY = "PICTURE_URL_KEY";

    private static final String FACEBOOK_ID_KEY = "FACEBOOK_ID_KEY";
    private static final String FACEBOOK_TOKEN_KEY = "FACEBOOK_TOKEN_KEY";
    private static final String FACEBOOK_EMAIL_KEY = "FACEBOOK_EMAIL_KEY";
    private static final String FACEBOOK_NAME_KEY = "FACEBOOK_NAME_KEY";

    private static final String TWITTER_ID_KEY = "TWITTER_ID_KEY";
    private static final String TWITTER_TOKEN_KEY = "TWITTER_TOKEN_KEY";
    private static final String TWITTER_NAME_KEY = "TWITTER_NAME_KEY";
    private static final String TWITTER_USERNAME_KEY = "TWITTER_USERNAME_KEY";

    private static final String GOOGLE_ID_KEY = "GOOGLE_ID_KEY";
    private static final String GOOGLE_TOKEN_KEY = "GOOGLE_TOKEN_KEY";
    private static final String GOOGLE_EMAIL_KEY = "GOOGLE_EMAIL_KEY";
    private static final String GOOGLE_NAME_KEY = "GOOGLE_NAME_KEY";

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


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        userId = prefs.getString(USER_ID_KEY, null);
        username = prefs.getString(USERNAME_KEY, null);
        email = prefs.getString(EMAIL_KEY, null);
        pictureUrl = prefs.getString(PICTURE_URL_KEY, null);

        facebookId = prefs.getString(FACEBOOK_ID_KEY, null);
        facebookToken = prefs.getString(FACEBOOK_TOKEN_KEY, null);
        facebookEmail = prefs.getString(FACEBOOK_EMAIL_KEY, null);
        facebookName = prefs.getString(FACEBOOK_NAME_KEY, null);

        twitterId       = prefs.getString(TWITTER_ID_KEY, null);
        twitterToken    = prefs.getString(TWITTER_TOKEN_KEY, null);
        twitterDisplayName = prefs.getString(TWITTER_NAME_KEY, null);
        twitterUsername = prefs.getString(TWITTER_USERNAME_KEY, null);

        googleId    = prefs.getString(GOOGLE_ID_KEY, null);
        googleToken = prefs.getString(GOOGLE_TOKEN_KEY, null);
        googleEmail = prefs.getString(GOOGLE_EMAIL_KEY, null);
        googleName  = prefs.getString(GOOGLE_NAME_KEY, null);

        Log.d(TAG, "==== USER  ====" +
                "\nID: " + userId +
                "\nUsername: " + username +
                "\nEmail: " + email +
                "\nPicture URL: " + pictureUrl +
                "\n==== FACEBOOK  ====" +
                "\nID: " + facebookId +
                "\nEmail: " + facebookEmail +
                "\nName : " + facebookName +
                "\n==== TWITTER ====" +
                "\nID: " + twitterId +
                "\nUsername: " + twitterUsername +
                "\n==== GOOGLE ====" +
                "\nID: " + googleId +
                "\nEmail: " + googleEmail +
                "\nName: " + googleEmail);

        return new InSquareProfile();
    }

    public static String userId;
    public static String username;
    public static String email;
    public static String pictureUrl;

    public static String facebookId;
    public static String facebookToken;
    public static String facebookEmail;
    public static String facebookName;

    public static String twitterId;
    public static String twitterToken;
    public static String twitterDisplayName;
    public static String twitterUsername;

    public static String googleId;
    public static String googleToken;
    public static String googleEmail;
    public static String googleName;

    public void save(Context c)
    {
        // TODO save data to disk
        String NAME = c.getString(R.string.app_name);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();

        editor.putString(USER_ID_KEY, userId);
        editor.putString(USERNAME_KEY, username);
        editor.putString(EMAIL_KEY, email);
        editor.putString(PICTURE_URL_KEY, pictureUrl );

        editor.putString(FACEBOOK_ID_KEY, facebookId);
        editor.putString(FACEBOOK_TOKEN_KEY, facebookToken);
        editor.putString(FACEBOOK_EMAIL_KEY, facebookEmail);
        editor.putString(FACEBOOK_NAME_KEY, facebookName);

        editor.putString(TWITTER_ID_KEY, twitterId);
        editor.putString(TWITTER_TOKEN_KEY, twitterToken);
        editor.putString(TWITTER_NAME_KEY, twitterDisplayName);
        editor.putString(TWITTER_USERNAME_KEY, twitterUsername);

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
        }else if(twitterUsername != null)
        {
            return twitterUsername;
        }

        Log.d(TAG, "getUsername: Current username is empty");

        return null;
    }

    public static String getUserId()
    {
        if(userId != null)
        {
            Log.d(TAG, "getUserId: user id!");
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
        }else if(twitterId != null)
        {
            Log.d(TAG, "getUserId: twitter id!");
            return twitterId;
        }

        Log.d(TAG, "getUserId: Current username is empty");

        return null;
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
                "\n==== TWITTER ====" +
                "\nID: " + twitterId +
                "\nUsername: " + twitterUsername +
                "\n==== GOOGLE ====" +
                "\nID: " + googleId +
                "\nEmail: " + googleEmail +
                "\nName: " + googleEmail;

    }
}
