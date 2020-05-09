package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class QuantityUnit implements Parcelable {

    public QuantityUnit() {}

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    @SerializedName("name_plural")
    private String namePlural;

    @SerializedName("plural_forms")
    private String pluralForms;

    public QuantityUnit(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        rowCreatedTimestamp = parcel.readString();
        namePlural = parcel.readString();
        pluralForms = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(rowCreatedTimestamp);
        dest.writeString(namePlural);
        dest.writeString(pluralForms);
    }

    public static final Creator<QuantityUnit> CREATOR = new Creator<QuantityUnit>() {

        @Override
        public QuantityUnit createFromParcel(Parcel in) {
            return new QuantityUnit(in);
        }

        @Override
        public QuantityUnit[] newArray(int size) {
            return new QuantityUnit[size];
        }
    };

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
        return namePlural == null ? name : namePlural;
    }

    public String getNamePluralCanNull() {
        return namePlural;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "QuantityUnit(" + name + ')';
    }
}
