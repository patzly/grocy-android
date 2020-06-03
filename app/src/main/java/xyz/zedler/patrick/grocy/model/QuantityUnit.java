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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "quantity_unit_table")
public class QuantityUnit implements Parcelable {

    public QuantityUnit() {}

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

    @ColumnInfo(name = "name_plural")
    @SerializedName("name_plural")
    private String namePlural;

    @ColumnInfo(name = "plural_forms")
    @SerializedName("plural_forms")
    private String pluralForms;

    public QuantityUnit(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        namePlural = parcel.readString();
        pluralForms = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
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

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamePlural() {
        return namePlural == null ? name : namePlural;
    }

    public String getNamePluralCanNull() {
        return namePlural;
    }

    public void setNamePlural(String namePlural) {
        this.namePlural = namePlural;
    }

    public String getPluralForms() {
        return pluralForms;
    }

    public void setPluralForms(String pluralForms) {
        this.pluralForms = pluralForms;
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
