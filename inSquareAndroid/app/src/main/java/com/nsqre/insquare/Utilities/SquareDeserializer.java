package com.nsqre.insquare.Utilities;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Created by emanu on 28/02/2016.
 */
public class SquareDeserializer implements JsonDeserializer<Square> {

    private Locale locale;

    public SquareDeserializer(Locale l)
    {
        this.locale = l;
    }

    @Override
    public Square deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        /*
       Input Example:
       ==============
       {
        "_index": "squares",
        "_type": "square",
        "_id": "56cc47e2ee9a60fe92ca47a2",
        "_score": null,
        "_source": {
          "name": "Prova",
          "searchName": "Prova",
          "geo_loc": "41.566872185995614,12.440877668559551",
          "messages": [
            "56cc48d5ee9a60fe92ca47a4",
            "56d0651fda98e2a49986c2a5"...
          ],
          "ownerId": "56bf62fb01469357eaabb167",
          "views": 50,
          "favouredBy": 0,
          "state": "awoken",
          "lastMessageDate": "2016-02-28T17:57:44.357Z"
        }
       ==============
         */

        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("_id").getAsString();
        final JsonObject source = jsonObject.get("_source").getAsJsonObject();
        final String name = source.get("name").getAsString();
        final String geoloc = source.get("geo_loc").getAsString();
        final String ownerid = source.get("ownerId").getAsString();
        final String favouredby = source.get("favouredBy").getAsString();
        final String views = source.get("views").getAsString();
        final String state = source.get("state").getAsString();
        String lmd = "";
        try {
            final String lastMessageDate = source.get("lastMessageDate").getAsString();
            lmd = lastMessageDate;
        }
        catch (Exception e) {
        }
        final Square square = new Square(id, name, geoloc, ownerid, favouredby, views, state, lmd, this.locale);
        return square;
    }
}
