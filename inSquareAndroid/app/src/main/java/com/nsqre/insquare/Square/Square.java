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

    protected static final String TAG = "Square";
    /**
     * The square id
     */
    protected String id;
    /**
     * The square name
     */
    protected String name;
    /**
     * A user written description for the square
     */
    protected String description;
    /**
     * The latitude value of the position of the square
     */
    protected double lat;
    /**
     * The longitude value of the position of the square
     */
    protected double lon;
    protected String type;
    /**
     * The id of the user that has created the square
     */
    protected String ownerId;
    /**
     * The number of how many people are following the square
     */
    protected long favouredBy;
    protected String[] favourers;
    /**
     * The number of how many people have seen this square
     */
    protected long views;
    /**
     * The state of the square
     * @see SquareState
     */
    protected SquareState squareState;
    /**
     * The date of the last message sent to this square
     */
    protected Calendar lastMessageDate;
    protected String lastMessageDateString;

    protected Locale myLocale;

    public boolean isFacebookEvent;
    public boolean isFacebookPage;

    public Square(String id, String name, double lat, double lon, String type, String ownerId) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
        this.ownerId = ownerId;
    }

    public Square(String id,
                  String name,
                  String description,
                  String geoloc,
                  String ownerId,
                  String favouredBy,
                  String[] favourers,
                  String views,
                  String state,
                  String lastMessageDate,
                  String type,
                  Locale l) {
        this.id = id;
        this.name = name;
        this.description = description;

        String[] parts = geoloc.split(",", 2);
        lat = Double.parseDouble(parts[0]);
        lon = Double.parseDouble(parts[1]);

        this.ownerId = ownerId;
        this.favourers = favourers;
        this.favouredBy = favourers.length;
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

        this.type = type;

        this.myLocale = l;
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

    public String getInitials()
    {
        String[] division = this.name.split("\\s+");

        if(division.length <= 1)
        {
            return this.name.substring(0,1).toUpperCase();
        }
        else if(division.length == 2)
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase();
        }
        else
        {
            return division[0].substring(0,1).toUpperCase() + division[1].substring(0,1).toUpperCase() + division[2].substring(0, 1).toUpperCase();
        }
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
