package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class ProductGroup implements Parcelable {

    @SerializedName("id")
    String id;

    @SerializedName("name")
    String name;

    @SerializedName("description")
    String description;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    public ProductGroup(Parcel parcel) {
        id = parcel.readString();
        name = parcel.readString();
        description = parcel.readString();
        rowCreatedTimestamp = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(rowCreatedTimestamp);
    }

    public static final Creator<ProductGroup> CREATOR = new Creator<ProductGroup>() {

        @Override
        public ProductGroup createFromParcel(Parcel in) {
            return new ProductGroup(in);
        }

        @Override
        public ProductGroup[] newArray(int size) {
            return new ProductGroup[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductGroup(" + name + ')';
    }
}
