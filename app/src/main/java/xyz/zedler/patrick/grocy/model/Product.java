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

public class Product implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("location_id")
    private String locationId;

    @SerializedName("qu_id_purchase")
    private int quIdPurchase; // quantity unit

    @SerializedName("qu_id_stock")
    private int quIdStock; // quantity unit

    @SerializedName("qu_factor_purchase_to_stock")
    private String quFactorPurchaseToStock; // quantity unit

    @SerializedName("enable_tare_weight_handling")
    private int enableTareWeightHandling;

    @SerializedName("picture_file_name")
    private String pictureFileName;

    @SerializedName("barcode")
    private String barcode;

    @SerializedName("min_stock_amount")
    private String minStockAmount;

    @SerializedName("default_best_before_days")
    private int defaultBestBeforeDays;

    @SerializedName("default_best_before_days_after_open")
    private int defaultBestBeforeDaysAfterOpen;

    @SerializedName("default_best_before_days_after_freezing")
    private int defaultBestBeforeDaysAfterFreezing;

    @SerializedName("default_best_before_days_after_thawing")
    private int defaultBestBeforeDaysAfterThawing;

    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    @SerializedName("product_group_id")
    private String productGroupId;

    @SerializedName("allow_partial_units_in_stock")
    private int allowPartialUnitsInStock;

    @SerializedName("tare_weight")
    private String tareWeight;

    @SerializedName("not_check_stock_fulfillment_for_recipes")
    private int notCheckStockFulfillmentForRecipes;

    @SerializedName("parent_product_id")
    private String parentProductId; /// STRING: null for empty

    @SerializedName("calories")
    private String calories;

    @SerializedName("cumulate_min_stock_amount_of_sub_products")
    private int cumulateMinStockAmountOfSubProducts;

    @SerializedName("shopping_location_id")
    private String storeId;

    public Product(
            int id,
            String name,
            String description,
            int quIdPurchase,
            String productGroupId
    ) {  // for shopping list
        this.id = id;
        this.name = name;
        this.description = description;
        this.quIdPurchase = quIdPurchase;
        this.productGroupId = productGroupId;
    }

    public Product(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        locationId = parcel.readString();
        quIdPurchase = parcel.readInt();
        quIdStock = parcel.readInt();
        quFactorPurchaseToStock = parcel.readString();
        enableTareWeightHandling = parcel.readInt();
        pictureFileName = parcel.readString();
        barcode = parcel.readString();
        minStockAmount = parcel.readString();
        defaultBestBeforeDays = parcel.readInt();
        defaultBestBeforeDaysAfterOpen = parcel.readInt();
        defaultBestBeforeDaysAfterFreezing = parcel.readInt();
        defaultBestBeforeDaysAfterThawing = parcel.readInt();
        rowCreatedTimestamp = parcel.readString();
        productGroupId = parcel.readString();
        allowPartialUnitsInStock = parcel.readInt();
        tareWeight = parcel.readString();
        notCheckStockFulfillmentForRecipes = parcel.readInt();
        parentProductId = parcel.readString();
        calories = parcel.readString();
        cumulateMinStockAmountOfSubProducts = parcel.readInt();
        storeId = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(locationId);
        dest.writeInt(quIdPurchase);
        dest.writeInt(quIdStock);
        dest.writeString(quFactorPurchaseToStock);
        dest.writeInt(enableTareWeightHandling);
        dest.writeString(pictureFileName);
        dest.writeString(barcode);
        dest.writeString(minStockAmount);
        dest.writeInt(defaultBestBeforeDays);
        dest.writeInt(defaultBestBeforeDaysAfterOpen);
        dest.writeInt(defaultBestBeforeDaysAfterFreezing);
        dest.writeInt(defaultBestBeforeDaysAfterThawing);
        dest.writeString(rowCreatedTimestamp);
        dest.writeString(productGroupId);
        dest.writeInt(allowPartialUnitsInStock);
        dest.writeString(tareWeight);
        dest.writeInt(notCheckStockFulfillmentForRecipes);
        dest.writeString(parentProductId);
        dest.writeString(calories);
        dest.writeInt(cumulateMinStockAmountOfSubProducts);
        dest.writeString(storeId);
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {

        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
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

    public int getLocationId() {
        if(locationId == null || locationId.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(locationId);
    }

    public int getQuIdStock() {
        return quIdStock;
    }

    public String getPictureFileName() {
        return pictureFileName;
    }

    public int getQuIdPurchase() {
        return quIdPurchase;
    }

    public double getQuFactorPurchaseToStock() {
        if(quFactorPurchaseToStock == null || quFactorPurchaseToStock.isEmpty()) {
            return 1;
        } else {
            return Double.parseDouble(quFactorPurchaseToStock);
        }
    }

    public int getEnableTareWeightHandling() {
        return enableTareWeightHandling;
    }

    public String getBarcode() {
        return barcode;
    }

    public double getMinStockAmount() {
        if(minStockAmount == null || minStockAmount.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(minStockAmount);
        }
    }

    public int getDefaultBestBeforeDays() {
        return defaultBestBeforeDays;
    }

    public int getDefaultBestBeforeDaysAfterOpen() {
        return defaultBestBeforeDaysAfterOpen;
    }

    public int getDefaultBestBeforeDaysAfterFreezing() {
        return defaultBestBeforeDaysAfterFreezing;
    }

    public int getDefaultBestBeforeDaysAfterThawing() {
        return defaultBestBeforeDaysAfterThawing;
    }

    public String getRowCreatedTimestamp() {
        return rowCreatedTimestamp;
    }

    public String getProductGroupId() {
        return productGroupId;
    }

    public int getAllowPartialUnitsInStock() {
        return allowPartialUnitsInStock;
    }

    public double getTareWeight() {
        if(tareWeight == null || tareWeight.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(tareWeight);
        }
    }

    public int getNotCheckStockFulfillmentForRecipes() {
        return notCheckStockFulfillmentForRecipes;
    }

    public String getParentProductId() {
        return parentProductId;
    }

    public double getCalories() {
        if(calories == null || calories.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(calories);
        }
    }

    public int getCumulateMinStockAmountOfSubProducts() {
        return cumulateMinStockAmountOfSubProducts;
    }

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
        return name;
    }
}
