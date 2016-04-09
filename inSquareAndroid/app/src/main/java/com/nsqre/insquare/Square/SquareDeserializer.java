package com.nsqre.insquare.Square;

import android.util.Log;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonArray;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * SquareDeserializer is the class that Gson uses to deserialize the JSON strings that represent squares
 * @see com.nsqre.insquare.Activities.BottomNavActivity
 * @see com.nsqre.insquare.Fragments.MapFragment
 */
public class SquareDeserializer implements JsonDeserializer<Square> {

    private static final String TAG = "SquareDeserializer";
    private Locale locale;

    public SquareDeserializer(Locale l)
    {
        this.locale = l;
    }

    /**
     * Manages the particular format of square's JSON representation, so it has sufficient data to instantiate a Square object
     * @param json
     * @param typeOfT
     * @param context
     * @return A Square object based on the data deserialized
     * @throws JsonParseException
     * @see Square
     */
    @Override
    public Square deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        /*
       Input Example:
       ==============
        {
        "_index": "squares",
        "_type": "square",
        "_id": "56fd531ee9ac22b3a1224a12",
        "_score": null,
        "_source": {
            "name": "Circo Massimo",
            "searchName": "Circo Massimo",
            "createdAt": "2016-03-31T16:41:02.168Z",
            "geo_loc": "41.88593924930288,12.484685853123665",
            "messages": [],
            "ownerId": "56c095658b4cc88bba9b32c8",
            "views": 22,
            "favouredBy": 1,
            "userLocated": 0,
            "description": "Circus Maximus, luogo dove gli antichi romani effettuavano corse, combattimenti. Oggi utilizzato per concerti e avvenimenti di qualunque genere",
            "state": "asleep",
            "lastMessageDate": "2016-03-31T16:41:02.168Z",
            "type": 0,
            "expireTime": "9999-01-01T00:00:00.000Z"
        }
       ==============
         */

//        Log.d(TAG, "deserialize: " + json.toString());

        final String id;
        final String name;
        final String geoloc;
        final String ownerid;
        final String views;
        final String favouredby;
        final JsonArray favourers;
        final String userLocated;
        final String description;
        final String state;
        final String lastMessageDate;
        final String type;

        final JsonObject jsonObject = json.getAsJsonObject();
        final JsonObject source = jsonObject.get("_source").getAsJsonObject();

        Log.d(TAG, "deserialize: " + json.toString());

        // ID
        id = jsonObject.get("_id").getAsString();
        // Name
        name = source.get("name").getAsString();

        // Description
        if(source.get("description") != null){
            description = source.get("description").getAsString();
        }
        else {
            description = "";
        }

        // Coordinates
        geoloc = source.get("geo_loc").getAsString();

        // OwnerID
        if(source.get("ownerId") != null){
            ownerid = source.get("ownerId").getAsString();
        }
        else {
            ownerid = "";
        }
        String[] fav = new String[]{};
        if(source.get("favourers") != null) {
            favourers = source.get("favourers").getAsJsonArray();
            fav = new String[favourers.size()];
            for(int i = 0; i<favourers.size(); i++) {
                fav[i] = favourers.get(i).getAsString();
            }
        }

        // View Count
        views = source.get("views").getAsString();

        // Favorite Count
        favouredby = source.get("favouredBy").getAsString();

        // Square State
        state = source.get("state").getAsString();

        // Last Message Date
        String lmd = "";
        try {
            lastMessageDate = source.get("lastMessageDate").getAsString();
            lmd = lastMessageDate;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Tipo
        JsonElement typeElement = source.get("type");
        if(typeElement == null)
        {
            type = "0";
        }else
        {
            type = typeElement.getAsString();
        }

        final Square square = new Square(id, name, description, geoloc, ownerid, favouredby, fav, views, state, lmd, type, this.locale);

        return square;
    }
}
