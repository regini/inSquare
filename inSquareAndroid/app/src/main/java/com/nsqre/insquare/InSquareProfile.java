package com.nsqre.insquare;/* Created by umbertosonnino on 5/1/16  */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nsqre.insquare.Square.Square;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * InSquareProfile is the class that handles most of the user data.
 * It's a singleton class which stores user's data locally, so the app can react faster to inputs
 * and has less data to request from the backend
 */
public class InSquareProfile {

    public interface InSquareProfileListener {
        void onOwnedChanged();
        void onFavChanged();
        void onRecentChanged();
    }

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

    // Lista di ascoltatori che vengono notificati quando si modifica qualcosa nella lista
    private static ArrayList<InSquareProfileListener> listeners;

    /**
     * The list of the squares owned by the user
     */
    private static ArrayList<Square> ownedSquaresList;
    /**
     * The list of the squares favoured by the user
     */
    private static ArrayList<Square> favouriteSquaresList;
    /**
     * The list of the squares recently used by the user
     */
    private static ArrayList<Square> recentSquaresList;

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
    }

    /**
     * Loads the data from the disk and creates user's InSquareProfile
     * @param c The context of the application
     * @return Returns the instance of the user's InSquareProfile
     */
    public static InSquareProfile getInstance(Context c)
    {
        if(profile != null)
        {
            Log.d(TAG, "getInstance: already instantiated!");
            return profile;
        }

        Log.d(TAG, "getInstance: Instantiating profile!");
        profile = new InSquareProfile();

        Gson gs = new Gson();
        Type type = new TypeToken<ArrayList<Square>>(){}.getType();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        profile.userId = prefs.getString(USER_ID_KEY, null);
        profile.username = prefs.getString(USERNAME_KEY, null);
        profile.email = prefs.getString(EMAIL_KEY, null);
        profile.pictureUrl = prefs.getString(PICTURE_URL_KEY, null);

        profile.listeners = new ArrayList<>();
        profile.ownedSquaresList = gs.fromJson(prefs.getString(OWNED_SQUARES_KEY, null), type);
        if(profile.ownedSquaresList == null)
        {
            profile.ownedSquaresList = new ArrayList<>();
            Log.d(TAG, "ownedSquaresList was null");
        }
        profile.favouriteSquaresList = gs.fromJson(prefs.getString(FAVOURITE_SQUARES_KEY, null), type);
        if(profile.favouriteSquaresList == null)
        {
            profile.favouriteSquaresList = new ArrayList<>();
            Log.d(TAG, "favourtieSquaresList was null");
        }
        profile.recentSquaresList = gs.fromJson(prefs.getString(RECENT_SQUARES_KEY, null), type);
        if(profile.recentSquaresList == null)
        {
            profile.recentSquaresList = new ArrayList<>();
            Log.d(TAG, "recentSquaresList was null");
        }

        profile.facebookId = prefs.getString(FACEBOOK_ID_KEY, null);
        profile.facebookToken = prefs.getString(FACEBOOK_TOKEN_KEY, null);
        profile.facebookEmail = prefs.getString(FACEBOOK_EMAIL_KEY, null);
        profile.facebookName = prefs.getString(FACEBOOK_NAME_KEY, null);

        profile.googleId    = prefs.getString(GOOGLE_ID_KEY, null);
        profile.googleToken = prefs.getString(GOOGLE_TOKEN_KEY, null);
        profile.googleEmail = prefs.getString(GOOGLE_EMAIL_KEY, null);
        profile.googleName  = prefs.getString(GOOGLE_NAME_KEY, null);

        return profile;
    }

    /**
     * Saves locally user's data
     * @param c The context of the application
     */
    public static void save(Context c)
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
        /*
        Log.d(TAG, "Save: " + "==== USER  ====" +
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
                "\nName: " + googleEmail);
         */
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

    public static String printProfile()
    {
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

    public static void addListener(InSquareProfileListener listener)
    {
        boolean exists = false;
        for(InSquareProfileListener ispl : listeners)
        {
            if(ispl.getClass().toString().equals(listener.getClass().toString()))
                exists = true;
        }
        if(!exists){
            listeners.add(listener);
            Log.d(TAG, "addListener: " + listener.getClass().toString());
        }else
            Log.d(TAG, "addListener: already there!");
    }

    public static void removeListener(InSquareProfileListener listener) {
        for(InSquareProfileListener ispl : listeners)
        {
            if(ispl.getClass().toString().equals(listener.getClass().toString()))
            {
                listeners.remove(ispl);
                Log.d(TAG, "removeListener: " + listener.getClass().toString());
                return;
            }
        }
    }

    /**
     * Adds the square passed as parameter to the list of the owned squares
     * @param square The square you want to add
     */
    public static void addOwned(Square square)
    {
        // Aggiungi in coda
        ownedSquaresList.add(square);

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl: listeners)
        {
            ispl.onOwnedChanged();
            Log.d(TAG, "addOwned: notifying listeners!");
        }
    }

    /**
     * Removes the square passed as parameter from the list of the owned squares
     * @param square The square you want to remove
     */
    public static void removeOwned(String square)
    {
        // trova la square da rimuovere
        for(Square s: ownedSquaresList)
        {
            if(s.getId().equals(square))
            {
                ownedSquaresList.remove(s);
                break;
            }
        }

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onOwnedChanged();
            Log.d(TAG, "removeOwned: notifying listeners!");
        }
    }

    /**
     * Checks if there is a square with the id equals to the one passed as parameter inside the owned squares list
     * @param squareId The id of the square
     * @return True if the square is present
     */
    public static boolean isOwned(String squareId)
    {
        for(Square s: ownedSquaresList)
        {
            if(s.getId().equals(squareId))
                return true;
        }
        return false;
    }

    /**
     * Adds the square passed as parameter to the list of the favourite squares
     * @param square The square you want to add
     */
    public static void addFav(Square square)
    {
        // Aggiungi in coda
        favouriteSquaresList.add(square);

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl: listeners)
        {
            ispl.onFavChanged();
            Log.d(TAG, "addFav: notifying listeners!");
        }
    }

    /**
     * Removes the square passed as parameter from the list of the favourite squares
     * @param square The square you want to remove
     */
    public static void removeFav(String square)
    {
        // trova la square da rimuovere
        for(Square s: favouriteSquaresList)
        {
            if(s.getId().equals(square))
            {
                favouriteSquaresList.remove(s);
                break;
            }
        }

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onFavChanged();
            Log.d(TAG, "removeFav: notifying listeners!");
        }
    }

    /**
     * Checks if there is a square with the id equals to the one passed as parameter inside the favourite squares list
     * @param squareId The id of the square
     * @return True if the square is present
     */
    public static boolean isFav(String squareId)
    {
        for(Square s: favouriteSquaresList)
        {
            if(s.getId().equals(squareId))
                return true;
        }
        return false;
    }

    /**
     * Adds the square passed as parameter to the list of the recent squares
     * @param square The square you want to add
     */
    public static void addRecent(Square square)
    {
        // Aggiungi in coda
        recentSquaresList.add(square);

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl: listeners)
        {
            ispl.onRecentChanged();
            Log.d(TAG, "addRecent: notifying listeners!");
        }
    }

    /**
     * Removes the square passed as parameter from the list of the recent squares
     * @param square The square you want to remove
     */
    public static void removeRecent(String square)
    {
        // trova la square da rimuovere
        for(Square s: recentSquaresList)
        {
            if(s.getId().equals(square))
            {
                recentSquaresList.remove(s);
                break;
            }
        }

        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onRecentChanged();
            Log.d(TAG, "addRecent: notifying listeners!");
        }
    }

    /**
     * Checks if there is a square with the id equals to the one passed as parameter inside the recent squares list
     * @param squareId The id of the square
     * @return True if the square is present
     */
    public static boolean isRecent(String squareId)
    {
        for(Square s: recentSquaresList)
        {
            if(s.getId().equals(squareId))
                return true;
        }
        return false;
    }

    public static ArrayList<Square> getOwnedSquaresList() {
        return ownedSquaresList;
    }

    public static ArrayList<Square> getFavouriteSquaresList() {
        return favouriteSquaresList;
    }

    public static ArrayList<Square> getRecentSquaresList() {
        return recentSquaresList;
    }

    public static void setOwnedSquaresList(ArrayList<Square> ownedSquaresList) {
        InSquareProfile.ownedSquaresList = ownedSquaresList;
        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onOwnedChanged();
            Log.d(TAG, "setOwnedSquares: notifying listeners!");
        }
    }

    public static void setFavouriteSquaresList(ArrayList<Square> favouriteSquaresList) {
        InSquareProfile.favouriteSquaresList = favouriteSquaresList;
        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onFavChanged();
            Log.d(TAG, "setFavSquares: notifying listeners!");
        }
    }

    public static void setRecentSquaresList(ArrayList<Square> recentSquaresList) {
        InSquareProfile.recentSquaresList = recentSquaresList;
        // Notifica gli ascoltatori
        for(InSquareProfileListener ispl : listeners)
        {
            ispl.onRecentChanged();
            Log.d(TAG, "setRecentSquares: notifying listeners!");
        }
    }
}
