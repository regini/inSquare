package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private String content;
    private Date date;
    private String sender;
    private int messageType;

    public Message(int t, String m, String s)
    {
        this.messageType = t;
        this.content=m;

        Calendar c = new GregorianCalendar(TimeZone.getDefault());
        this.date= c.getTime();

        this.sender=s;
    }

    public String getContent() {
        return content;
    }

    public Date getDate() {
        return date;
    }

    public String getSender() {
        return sender;
    }

    public int getMessageType() { return this.messageType; }
}
