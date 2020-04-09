package xyz.zedler.patrick.grocy.model;

import com.google.gson.annotations.SerializedName;

public class ProductGroup {

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("description")
    String description;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
