package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Message {

    private static final String TAG = "Message";
    private String msg_id;
    private String text;
    private String name;
    private String from;
    private String createdAt;
    private Calendar calendar;

    public Message(String m, String username, String userId, Locale l)
    {
        this.text = m.trim();

        // La data viene formattata con la forma locale
        Calendar c = Calendar.getInstance();
        this.createdAt = getFormatter(l).format(c.getTime());

        this.name = username;
        this.from = userId;
    }

    public Message(String mes_id, String contents, String username, String userId, String date, Locale l)
    {
        this.msg_id = mes_id;
        this.text = contents;
        this.name = username;
        this.from = userId;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", l);
        try {
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

    public SimpleDateFormat getFormatter(Locale loc)
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", loc);
    }

    public Calendar getCalendar() {
        return calendar;
    }

    @Override
    public String toString() {
        return this.name + " " + " said: " + this.text + "\nMessage #(" + this.msg_id + ") created: "+ this.createdAt;
    }
}
