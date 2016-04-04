package com.nsqre.insquare.Square;/* Created by umbertosonnino on 6/2/16  */

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Square is the class that represents the concept of Square inside the application
 */
public class Square implements Serializable {

    private static final String TAG = "Square";
    /**
     * The square id
     */
    private String id;
    /**
     * The square name
     */
    private String name;
    /**
     * A user written description for the square
     */
    private String description;
    /**
     * The latitude value of the position of the square
     */
    private double lat;
    /**
     * The longitude value of the position of the square
     */
    private double lon;
    private String type;
    /**
     * The id of the user that has created the square
     */
    private String ownerId;
    /**
     * The number of how many people are following the square
     */
    private long favouredBy;
    /**
     * The number of how many people have seen this square
     */
    private long views;
    /**
     * The state of the square
     * @see SquareState
     */
    private SquareState squareState;
    /**
     * The date of the last message sent to this square
     */
    private Calendar lastMessageDate;
    private String lastMessageDateString;
    private Locale myLocale;

    public Square(String id, String name, double lat, double lon, String type, String ownerId) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.ownerId = ownerId;
    }

    public Square(String id, String name, String description, String geoloc, String ownerId, String favouredBy, String views, String state, String lastMessageDate, Locale l) {
        this.id = id;
        this.name = name;
        this.description = description;

        String[] parts = geoloc.split(",", 2);
        lat = Double.parseDouble(parts[0]);
        lon = Double.parseDouble(parts[1]);

        this.ownerId = ownerId;
        this.favouredBy = Long.parseLong(favouredBy);
        this.views = Long.parseLong(views);

        switch (state.toLowerCase()) {
            case "asleep":
                this.squareState = SquareState.ASLEEP;
                break;
            case "awoken":
                this.squareState = SquareState.AWOKEN;
                break;
            case "caffeinated":
                this.squareState = SquareState.CAFFEINATED;
                break;
            default:
                break;
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", l);
        try {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = df.parse(lastMessageDate);
            this.lastMessageDate = Calendar.getInstance();
            this.lastMessageDate.setTime(d);
            this.lastMessageDateString = df.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.myLocale = l;
//        Log.d(TAG, "NEWSQUARE:\n" + this.toString());
    }

    @Override
    public String toString() {
        return "Square{" +
                "id='" + id + '\'' +
                "\nname='" + name + '\'' +
                "\nlat=" + lat +
                "\nlon=" + lon +
                "\ntype='" + type + '\'' +
                "\nownerId='" + ownerId + '\'' +
                "\nfavouredBy=" + favouredBy +
                "\nviews=" + views +
                "\nsquareState=" + squareState +
                "\nlastMessageDate=" + lastMessageDate +
                "\nlastMessageDateString='" + lastMessageDateString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return this.id.equals(((Square)o).getId());
//        return this.id.equals(((Square)o).getId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public long getFavouredBy() {
        return favouredBy;
    }

    public long getViews() {
        return views;
    }

    public SquareState getSquareState() {
        return squareState;
    }

    public String getLastMessageDateString() {
        return lastMessageDateString;
    }

    public Calendar getLastMessageDate() {
        return lastMessageDate;
    }

    /**
     * Creates a string out of the lastMessageDate Calendar object
     * @return A string representing the date of the last message sent to the square
     */
    public String formatTime()
    {
        String timetoShow = "";
        Calendar c = Calendar.getInstance();
        int tYear = c.get(Calendar.YEAR);
        int tDay = c.get(Calendar.DAY_OF_MONTH);

        Calendar msgCal = this.getLastMessageDate();
        int mYear = msgCal.get(Calendar.YEAR);
        int mDay = msgCal.get(Calendar.DAY_OF_MONTH);

        DateFormat df;
        if(mYear != tYear)
        {
            df = new SimpleDateFormat("MMM d, ''yy, HH:mm", this.myLocale);
        }else if(mDay != tDay)
        {
            df = new SimpleDateFormat("MMM d, HH:mm", this.myLocale);
        }else
        {
            df = new SimpleDateFormat("HH:mm", this.myLocale);
            timetoShow += "Oggi, ";
        }

        timetoShow += df.format(msgCal.getTime());

        return "Ultimo messaggio: " + timetoShow;
    }


    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
