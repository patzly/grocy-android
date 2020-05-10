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

public class ProductGroup extends GroupedListItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    /**
     * First element in bottomSheet selection: NONE (id = null)
     */
    public ProductGroup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public ProductGroup(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        rowCreatedTimestamp = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
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

    public int getId() {
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

    @Override
    public int getType() {
        return TYPE_HEADER;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductGroup(" + name + ')';
    }
}
