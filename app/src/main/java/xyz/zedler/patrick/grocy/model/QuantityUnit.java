package xyz.zedler.patrick.grocy.model;

import com.google.gson.annotations.SerializedName;

public class QuantityUnit {

    public QuantityUnit() {}

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("description")
    String description;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    @SerializedName("name_plural")
    String namePlural;

    @SerializedName("plural_forms")
    String pluralForms;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getNamePlural() {
        return namePlural;
    }
}
