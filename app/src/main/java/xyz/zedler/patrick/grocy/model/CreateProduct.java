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

public class CreateProduct implements Parcelable {

    private String productName, barcodes;
    private String defaultStoreId, defaultBestBeforeDays, defaultLocationId;

    public CreateProduct(
            String productName,
            String barcodes,
            String defaultStoreId,
            String defaultBestBeforeDays,
            String defaultLocationId
    ) {
        this.productName = productName;
        this.barcodes = barcodes;
        this.defaultStoreId = defaultStoreId;
        this.defaultBestBeforeDays = defaultBestBeforeDays;
        this.defaultLocationId = defaultLocationId;
    }

    private CreateProduct(Parcel parcel) {
        productName = parcel.readString();
        barcodes = parcel.readString();
        defaultStoreId = parcel.readString();
        defaultBestBeforeDays = parcel.readString();
        defaultLocationId = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productName);
        dest.writeString(barcodes);
        dest.writeString(defaultStoreId);
        dest.writeString(defaultBestBeforeDays);
        dest.writeString(defaultLocationId);
    }

    public static final Creator<CreateProduct> CREATOR = new Creator<CreateProduct>() {

        @Override
        public CreateProduct createFromParcel(Parcel in) {
            return new CreateProduct(in);
        }

        @Override
        public CreateProduct[] newArray(int size) {
            return new CreateProduct[size];
        }
    };

    public String getProductName() {
        return productName;
    }

    public String getBarcodes() {
        return barcodes;
    }

    public String getDefaultBestBeforeDays() {
        return defaultBestBeforeDays;
    }

    public String getDefaultLocationId() {
        return defaultLocationId;
    }

    public String getDefaultStoreId() {
        return defaultStoreId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "CreateProduct(" + productName + ")";
    }
}
