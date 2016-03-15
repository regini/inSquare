package com.nsqre.insquare.Message;/* Created by umbertosonnino on 2/1/16  */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Message is the class that represents the concept of message inside the application
 */
public class Message {

    private static final String TAG = "Message";
    /**
     * The id of the message
     */
    private String msg_id;
    /**
     * The text content of the message
     */
    private String text;
    /**
     * The name of the sender
     */
    private String name;
    /**
     * The id of the sender
     */
    private String from;
    private String createdAt;
    /**
     * The date in which the message was sent
     */
    private Calendar calendar;
    /**
     * Wheter the user was in that particular place when he sent the message
     */
    private Boolean userSpot;

    public Message(String m, String username, String userId, Locale l)
    {
        //this.msg_id
        this.text = m.trim();
        this.name = username;
        this.from = userId;
        // La data viene formattata con la forma locale
        this.calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", l);
        this.createdAt = df.format(this.calendar.getTime());
    }

    public Message(String mes_id, String contents, String username, String userId, String date, Boolean userSpot, Locale l)
    {
        this.msg_id = mes_id;
        this.text = contents;
        this.name = username;
        this.from = userId;
        this.userSpot = userSpot;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", l);
        try {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = df.parse(date);
            this.calendar = Calendar.getInstance();
            this.calendar.setTime(d);

            this.createdAt = df.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getText() {
        return text;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return msg_id;
    }

    public String getFrom() {
        return from;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public Boolean getUserSpot() {
        return userSpot;
    }

    @Override
    public String toString() {
        return this.name + " " + " said: " + this.text + "\nMessage #(" + this.msg_id + ") created: "+ this.createdAt;
    }
}
