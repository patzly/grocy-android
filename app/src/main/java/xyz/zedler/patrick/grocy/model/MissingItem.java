package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class MissingItem {

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("amount_missing")
    int amountMissing;

    @SerializedName("is_partly_in_stock")
    int isPartlyInStock;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAmountMissing() {
        return amountMissing;
    }

    public int getIsPartlyInStock() {
        return isPartlyInStock;
    }

    @NonNull
    @Override
    public String toString() {
        return "MissingItem(" + name + ')';
    }
}
