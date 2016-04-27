package com.nsqre.insquare.Message;/* Created by umbertosonnino on 25/2/16  */

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * MessageDeserializer is the class that Gson uses to deserialize the JSON strings that represent messages
 * @see com.nsqre.insquare.Activities.ChatActivity
 */
public class MessageDeserializer implements JsonDeserializer<Message> {

    private Locale locale;

    public MessageDeserializer(Locale l)
    {
        this.locale = l;
    }

    /**
     * Manages the particular format of messages's JSON representation, so it has sufficient data to instantiate a Message object
     * @return A Square object based on the data deserialized
     * @throws JsonParseException
     * @see Message
     */
    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        /*
       Input Example:
       ==============
       {
        "name": "Umberto Sonnino",
        "text": "One more time!",
        "createdAt": "2016-02-25T14:16:37.757Z",
        "msg_id": "56cf0cc54b055cfe68327c5c",
        "from": "56c094748b4cc88bba9b32c7"
        "userSpot": "true"
       }
       ==============
         */

        final JsonObject jsonObject = json.getAsJsonObject();
        final String sender = jsonObject.get("name").getAsString();
        final String text = jsonObject.get("text").getAsString();
        final String createdAt = jsonObject.get("createdAt").getAsString();
        final String msg_id = jsonObject.get("msg_id").getAsString();
        final String senderId = jsonObject.get("from").getAsString();
        final Boolean userSpot = jsonObject.get("userSpot").getAsBoolean();

        final Message msg = new Message(msg_id, text, sender, senderId, createdAt, userSpot, this.locale);
        return msg;
    }
}
