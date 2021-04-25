/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "missing_item_table")
public class MissingItem implements Parcelable {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    private int id;

    @ColumnInfo(name = "name")
    @SerializedName("name")
    private String name;

    @ColumnInfo(name = "amount_missing")
    @SerializedName("amount_missing")
    private String amountMissing;

    @ColumnInfo(name = "is_partly_in_stock")
    @SerializedName("is_partly_in_stock")
    private int isPartlyInStock;

    // for Room
    public MissingItem() {}

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

    public double getAmountMissingDouble() {
        if(amountMissing == null || amountMissing.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amountMissing);
        }
    }

    public String getAmountMissing() {
        return amountMissing;
    }

    public int getIsPartlyInStock() {
        return isPartlyInStock;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAmountMissing(String amountMissing) {
        this.amountMissing = amountMissing;
    }

    public void setIsPartlyInStock(int isPartlyInStock) {
        this.isPartlyInStock = isPartlyInStock;
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
