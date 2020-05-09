package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockLocation implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("location_id")
    private int locationId;

    @SerializedName("location_name")
    private String locationName;

    @SerializedName("location_is_freezer")
    private int isFreezer;

    private StockLocation(Parcel parcel) {
        id = parcel.readInt();
        productId = parcel.readInt();
        locationId = parcel.readInt();
        locationName = parcel.readString();
        isFreezer = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(productId);
        dest.writeInt(locationId);
        dest.writeString(locationName);
        dest.writeInt(isFreezer);
    }

    public static final Creator<StockLocation> CREATOR = new Creator<StockLocation>() {

        @Override
        public StockLocation createFromParcel(Parcel in) {
            return new StockLocation(in);
        }

        @Override
        public StockLocation[] newArray(int size) {
            return new StockLocation[size];
        }
    };

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public int getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public int getIsFreezer() {
        return isFreezer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "StockLocation(" + locationName + ')';
    }
}
