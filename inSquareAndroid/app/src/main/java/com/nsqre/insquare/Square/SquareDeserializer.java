package com.nsqre.insquare.Square;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

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

        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("_id").getAsString();
        final JsonObject source = jsonObject.get("_source").getAsJsonObject();
        final String name = source.get("name").getAsString();
        final String description;
        if(source.get("description") != null){
            description = source.get("description").getAsString();
        }
        else {
            description = "";
        }
        final String geoloc = source.get("geo_loc").getAsString();
        final String ownerid;
        if(source.get("ownerId") != null){
            ownerid = source.get("ownerId").getAsString();
        }
        else {
            ownerid = "";
        }
        final String favouredby = source.get("favouredBy").getAsString();
        String[] fav = new String[]{};
        if(source.get("favourers") != null) {
            final JsonArray favourers = source.get("favourers").getAsJsonArray();
            fav = new String[favourers.size()];
            for(int i = 0; i<favourers.size(); i++) {
                fav[i] = favourers.get(i).getAsString();
            }
        }
        final String views = source.get("views").getAsString();
        final String state = source.get("state").getAsString();
        String lmd = "";
        try {
            final String lastMessageDate = source.get("lastMessageDate").getAsString();
            lmd = lastMessageDate;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        final Square square = new Square(id, name, description, geoloc, ownerid, favouredby, fav, views, state, lmd, this.locale);
        return square;
    }
}
