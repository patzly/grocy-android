package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return "ProductGroup(" + name + ')';
    }
}
