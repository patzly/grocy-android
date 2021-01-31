package xyz.zedler.patrick.grocy.model;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

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
        if(price != null && !price.isEmpty()) this.price = price;
        if(storeId != null && !storeId.isEmpty()) this.storeId = storeId;
    }

    private BatchPurchaseEntry(Parcel parcel) {
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

    public String getLocationId() {
        return locationId;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public String getPrice() {
        return price;
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
