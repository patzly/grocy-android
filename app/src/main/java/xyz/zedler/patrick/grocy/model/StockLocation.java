package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockLocation {

    @SerializedName("id")
    int id;

    @SerializedName("product_id")
    int productId;

    @SerializedName("location_id")
    int locationId;

    @SerializedName("name")
    String name;

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public int getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return "StockLocation(" + name + ')';
    }
}
