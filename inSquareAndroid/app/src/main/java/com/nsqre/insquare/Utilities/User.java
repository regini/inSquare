package com.nsqre.insquare.Utilities;

import java.io.Serializable;

/**
 * Created by mrsa on 15/01/2016.
 */
public class User implements Serializable {

    private String id;
    private String name;
    private String email;

    public User() {

    }

    public String getId() { return this.id; }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
