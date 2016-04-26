package com.nsqre.insquare.Square;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * SquareDeserializer is the class that Gson uses to deserialize the JSON strings that represent squares
 * @see com.nsqre.insquare.Activities.BottomNavActivity
 * @see MapFragment
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
        final String facebookPageId;
        final String facebookEventId;

        final JsonObject jsonObject = json.getAsJsonObject();
        final JsonObject source = jsonObject.get("_source").getAsJsonObject();

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

        // Tipo:
        // 0 - Luogo
        // 1 - Evento
        // 2 - Attivita Commerciale
        JsonElement typeElement = source.get("type");
        if(typeElement == null)
        {
            type = "0";
        }else
        {
            type = typeElement.getAsString();
        }

        JsonElement facebookIdPageElement = source.get("facebook_id_page");
        if(facebookIdPageElement == null)
        {
            facebookPageId = "";
        }else
        {
            facebookPageId = facebookIdPageElement.getAsString();
            return new FacebookPageSquare(id, name,description, geoloc, ownerid, favouredby, fav, views, state, lmd, type, facebookPageId, this.locale);
        }

        JsonElement facebookIdEventElement = source.get("facebook_id_event");
        if(facebookIdEventElement == null)
        {
            facebookEventId = "";
        }
        else
        {
            facebookEventId = facebookIdEventElement.getAsString();
            return new FacebookEventSquare(id, name,description, geoloc, ownerid, favouredby, fav, views, state, lmd, type, facebookEventId, this.locale);
        }

        return new Square(id, name, description, geoloc, ownerid, favouredby, fav, views, state, lmd, type, this.locale);

    }
}
