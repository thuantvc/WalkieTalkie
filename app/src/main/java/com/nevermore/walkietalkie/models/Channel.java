package com.nevermore.walkietalkie.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public abstract class Channel {

    protected String name;
    protected byte id;

    public Channel(byte id, String name) {
        this.id = id;
        this.name = name;
    }

    public JSONObject serialize() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", name);
    }

    public String getName() {
        return name;
    }

    public byte getId() {
        return id;
    }

}
