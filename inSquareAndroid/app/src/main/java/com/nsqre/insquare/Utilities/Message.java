package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private String text;
    private Date createdAt;
    private String name;
    private int messageType;

    public Message () {}


    public Message(int t, String m, String s)
    {
        this.messageType = t;
        this.text =m;

        Calendar c = new GregorianCalendar(TimeZone.getDefault());
        this.createdAt = c.getTime();

        this.name =s;
    }

    public String getText() {
        return text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public int getMessageType() { return this.messageType; }
}
