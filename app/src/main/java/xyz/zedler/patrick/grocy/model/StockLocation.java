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

import com.google.gson.annotations.SerializedName;

import xyz.zedler.patrick.grocy.util.NumUtil;

public class StockLocation implements Parcelable {

    @SerializedName("id")
    private final int id;

    @SerializedName("product_id")
    private final int productId;

    @SerializedName("amount")
    private final String amount;

    @SerializedName("location_id")
    private final int locationId;

    @SerializedName("location_name")
    private final String locationName;

    @SerializedName("location_is_freezer")
    private final String isFreezer;

    private StockLocation(Parcel parcel) {
        id = parcel.readInt();
        productId = parcel.readInt();
        amount = parcel.readString();
        locationId = parcel.readInt();
        locationName = parcel.readString();
        isFreezer = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(productId);
        dest.writeString(amount);
        dest.writeInt(locationId);
        dest.writeString(locationName);
        dest.writeString(isFreezer);
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

    public String getAmount() {
        return amount;
    }

    public double getAmountDouble() {
        return NumUtil.isStringDouble(amount) ? Double.parseDouble(amount) : 0;
    }

    public int getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getIsFreezer() {
        return isFreezer;
    }

    public boolean getIsFreezerBoolean() {
        return NumUtil.isStringInt(isFreezer) && Integer.parseInt(isFreezer) == 1;
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
