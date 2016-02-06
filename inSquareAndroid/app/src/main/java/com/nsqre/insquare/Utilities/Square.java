package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 6/2/16  */

import java.io.Serializable;

public class Square implements Serializable {

    private String id;
    private String name;
    private double lat, lon;
    private String type;

    public Square(String id, String name, double lat, double lon, String type) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Name: " + name + "; Loc: " + lat + "," + lon;
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
}
