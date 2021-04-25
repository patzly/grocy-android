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

import java.util.Objects;

@Entity(tableName = "location_table")
public class Location implements Parcelable {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    private int id;

    @ColumnInfo(name = "name")
    @SerializedName("name")
    private String name;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "row_created_timestamp")
    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    @ColumnInfo(name = "is_freezer")
    @SerializedName("is_freezer")
    private String isFreezer;

    public Location(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Location(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        rowCreatedTimestamp = parcel.readString();
        isFreezer = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(rowCreatedTimestamp);
        dest.writeString(isFreezer);
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {

        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
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

    public String getIsFreezer() {
        return isFreezer;
    }

    public int getIsFreezerInt() {
        return isFreezer == null || isFreezer.isEmpty() ? 0 : Integer.parseInt(isFreezer);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRowCreatedTimestamp() {
        return rowCreatedTimestamp;
    }

    public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
        this.rowCreatedTimestamp = rowCreatedTimestamp;
    }

    public void setIsFreezer(String isFreezer) {
        this.isFreezer = isFreezer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return id == location.id &&
                isFreezer == location.isFreezer &&
                Objects.equals(name, location.name) &&
                Objects.equals(description, location.description) &&
                Objects.equals(rowCreatedTimestamp, location.rowCreatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, rowCreatedTimestamp, isFreezer);
    }

    @NonNull
    @Override
    public String toString() {
        return "Location(" + name + ')';
    }
}
