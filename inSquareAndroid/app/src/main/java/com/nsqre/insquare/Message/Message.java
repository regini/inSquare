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
     * Whether the user was in that particular place when he sent the message
     */
    private Boolean userSpot;

    private String urlProvider;
    private String urlTitle;
    private String urlDescription;
    private String urlImage;
    private boolean isLineVisible;

    private Locale locale;

    /**
     * Creates a Message object
     * @param m the text of the message
     * @param username the username of the sender
     * @param userId the id of the sender
     * @param l the locale
     */
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
        isLineVisible = false;
        this.locale = l;
    }

    /**
     * Creates a Message object
     * @param mes_id the id of the message
     * @param contents the text of the message
     * @param username the username of the sender
     * @param userId the id of the sender
     * @param date the day and time in which the message was sent
     * @param userSpot whether the user was in the location of the square that contains the message
     * @param l the locale
     */
    public Message(String mes_id, String contents, String username, String userId, String date, Boolean userSpot, Locale l)
    {
        this.msg_id = mes_id;
        this.text = contents;
        this.name = username;
        this.from = userId;
        this.userSpot = userSpot;
        isLineVisible = false;

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

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return msg_id;
    }

    public void setId(String msg_id) {
        this.msg_id = msg_id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
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

    public String getUrlDescription() {
        return urlDescription;
    }

    public void setUrlDescription(String urlDescription) {
        this.urlDescription = urlDescription;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public boolean isLineVisible() {
        return isLineVisible;
    }

    public void setIsLineVisible(boolean isLineVisible) {
        this.isLineVisible = isLineVisible;
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

        return msg_id.equals(message.msg_id);

    }

    @Override
    public int hashCode() {
        return msg_id.hashCode();
    }
}
