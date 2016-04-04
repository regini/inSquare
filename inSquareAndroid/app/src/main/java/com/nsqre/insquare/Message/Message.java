package com.nsqre.insquare.Message;/* Created by umbertosonnino on 2/1/16  */

import java.io.Serializable;
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
public class Message implements Serializable {

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

    private String urlProvider;
    private String urlTitle;
    private String urlDesription;

    private Locale locale;

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

        this.locale = l;
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

        this.locale = l;
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

    public String getUrlProvider() {
        return urlProvider;
    }

    public void setUrlProvider(String urlProvider) {
        this.urlProvider = urlProvider;
    }

    public String getUrlTitle() {
        return urlTitle;
    }

    public void setUrlTitle(String urlTitle) {
        this.urlTitle = urlTitle;
    }

    public String getUrlDesription() {
        return urlDesription;
    }

    public void setUrlDesription(String urlDesription) {
        this.urlDesription = urlDesription;
    }

    public void setTime() {
        this.calendar = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", this.locale);
        this.createdAt = df.format(this.calendar.getTime());
    }

    @Override
    public String toString() {
        return this.name + " " + " said: " + this.text + "\nMessage #(" + this.msg_id + ") created: "+ this.createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return createdAt.equals(message.getCreatedAt()) && from.equals(message.getFrom());

    }

    @Override
    public int hashCode() {
        return msg_id.hashCode();
    }
}
