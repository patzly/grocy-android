package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class BatchPurchaseEntry implements Parcelable {

    private String price, locationId, bestBeforeDate, storeId;

    public BatchPurchaseEntry(
            String bestBeforeDate,
            String locationId,
            String price,
            String storeId
    ) {
        this.bestBeforeDate = bestBeforeDate;
        this.locationId = locationId;
        if(price != null && !price.equals("")) this.price = price;
        if(storeId != null && !storeId.equals("")) this.storeId = storeId;
    }

    public BatchPurchaseEntry(Parcel parcel) {
        bestBeforeDate = parcel.readString();
        locationId = parcel.readString();
        price = parcel.readString();
        storeId = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bestBeforeDate);
        dest.writeString(locationId);
        dest.writeString(price);
        dest.writeString(storeId);
    }

    public static final Creator<BatchPurchaseEntry> CREATOR = new Creator<BatchPurchaseEntry>() {

        @Override
        public BatchPurchaseEntry createFromParcel(Parcel in) {
            return new BatchPurchaseEntry(in);
        }

        @Override
        public BatchPurchaseEntry[] newArray(int size) {
            return new BatchPurchaseEntry[size];
        }
    };

    public String getStoreId() {
        return storeId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "BatchPurchaseEntry("
                + "bestBeforeDate: " + bestBeforeDate + ", "
                + "locationId: " + locationId + ", "
                + "price: " + price + ", "
                + "storeId: " + storeId + ")";
    }
}
