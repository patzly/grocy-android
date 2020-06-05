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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class MissingItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("amount_missing")
    private String amountMissing;

    @SerializedName("is_partly_in_stock")
    private int isPartlyInStock;

    private MissingItem(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        amountMissing = parcel.readString();
        isPartlyInStock = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(amountMissing);
        dest.writeInt(isPartlyInStock);
    }

    public static final Creator<MissingItem> CREATOR = new Creator<MissingItem>() {

        @Override
        public MissingItem createFromParcel(Parcel in) {
            return new MissingItem(in);
        }

        @Override
        public MissingItem[] newArray(int size) {
            return new MissingItem[size];
        }
    };

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getAmountMissing() {
        if(amountMissing == null || amountMissing.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amountMissing);
        }
    }

    public int getIsPartlyInStock() {
        return isPartlyInStock;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "MissingItem(" + name + ')';
    }
}
