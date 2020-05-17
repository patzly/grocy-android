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

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class PriceHistoryEntry {

    @SerializedName("date")
    private String date;

    @SerializedName("price")
    private String price;

    @SerializedName("shopping_location")
    private Store store;

    public String getDate() {
        return date;
    }

    public double getPrice() {
        if(price == null || price.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(price);
        }
    }

    public Store getStore() {
        return store;
    }

    public String getStoreName() {
        if(store == null) return null;
        return store.getName();
    }

    @NonNull
    @Override
    public String toString() {
        return "PriceHistoryEntry(" + date + ": " + price + ')';
    }
}
