package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private String id;
    private String text;
    private String from;
    private String createdAt;
    private String squareId;

    private Date messageTimestamp;
    private String name;
    private int messageType;

    public Message () {}


    public Message(int t, String m, String un, String ui)
    {
        this.messageType = t;
        this.text = m;
        Calendar c = new GregorianCalendar(TimeZone.getDefault());
        this.messageTimestamp = c.getTime();
        this.name = un;
        this.from = ui;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public int getMessageType() { return this.messageType; }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSquareId() {
        return squareId;
    }

    public void setSquareId(String squareId) {
        this.squareId = squareId;
    }

    public Date getMessageTimestamp() {
        return messageTimestamp;
    }

    @Override
    public String toString() {
        return this.name + " " + " said: " + this.text + "\nMessage #(" + this.id + ") created: "+ this.createdAt.toString();
    }
}
