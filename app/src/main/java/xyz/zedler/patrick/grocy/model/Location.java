package xyz.zedler.patrick.grocy.model;

import com.google.gson.annotations.SerializedName;

public class Location {

    public Location() {}

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("description")
    String description;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    @SerializedName("is_freezer")
    int isFreezer;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getIsFreezer() {
        return isFreezer;
    }
}
