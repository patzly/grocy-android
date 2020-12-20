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

import java.util.Objects;

@Entity(tableName = "product_table")
public class Product implements Parcelable {

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

    @ColumnInfo(name = "location_id")
    @SerializedName("location_id")
    private String locationId;

    @ColumnInfo(name = "qu_id_purchase")
    @SerializedName("qu_id_purchase")
    private int quIdPurchase; // quantity unit

    @ColumnInfo(name = "qu_id_stock")
    @SerializedName("qu_id_stock")
    private int quIdStock; // quantity unit

    @ColumnInfo(name = "qu_factor_purchase_to_stock")
    @SerializedName("qu_factor_purchase_to_stock")
    private String quFactorPurchaseToStock; // quantity unit

    @ColumnInfo(name = "enable_tare_weight_handling")
    @SerializedName("enable_tare_weight_handling")
    private int enableTareWeightHandling;

    @ColumnInfo(name = "picture_file_name")
    @SerializedName("picture_file_name")
    private String pictureFileName;

    @ColumnInfo(name = "barcode")
    @SerializedName("barcode")
    private String barcode;

    @ColumnInfo(name = "min_stock_amount")
    @SerializedName("min_stock_amount")
    private String minStockAmount;

    @ColumnInfo(name = "default_best_before_days")
    @SerializedName("default_best_before_days")
    private int defaultBestBeforeDays;

    @ColumnInfo(name = "default_best_before_days_after_open")
    @SerializedName("default_best_before_days_after_open")
    private int defaultBestBeforeDaysAfterOpen;

    @ColumnInfo(name = "default_best_before_days_after_freezing")
    @SerializedName("default_best_before_days_after_freezing")
    private int defaultBestBeforeDaysAfterFreezing;

    @ColumnInfo(name = "default_best_before_days_after_thawing")
    @SerializedName("default_best_before_days_after_thawing")
    private int defaultBestBeforeDaysAfterThawing;

    @ColumnInfo(name = "row_created_timestamp")
    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    @ColumnInfo(name = "product_group_id")
    @SerializedName("product_group_id")
    private String productGroupId;

    @ColumnInfo(name = "allow_partial_units_in_stock")
    @SerializedName("allow_partial_units_in_stock")
    private int allowPartialUnitsInStock;

    @ColumnInfo(name = "tare_weight")
    @SerializedName("tare_weight")
    private String tareWeight;

    @ColumnInfo(name = "not_check_stock_fulfillment_for_recipes")
    @SerializedName("not_check_stock_fulfillment_for_recipes")
    private int notCheckStockFulfillmentForRecipes;

    @ColumnInfo(name = "parent_product_id")
    @SerializedName("parent_product_id")
    private String parentProductId; /// STRING: null for empty

    @ColumnInfo(name = "calories")
    @SerializedName("calories")
    private String calories;

    @ColumnInfo(name = "cumulate_min_stock_amount_of_sub_products")
    @SerializedName("cumulate_min_stock_amount_of_sub_products")
    private int cumulateMinStockAmountOfSubProducts;

    @ColumnInfo(name = "shopping_location_id")
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

    public int getLocationIdInt() {
        if(locationId == null || locationId.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(locationId);
    }

    public String getLocationId() {
        return locationId;
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

    public double getQuFactorPurchaseToStockDouble() {
        if(quFactorPurchaseToStock == null || quFactorPurchaseToStock.isEmpty()) {
            return 1;
        } else {
            return Double.parseDouble(quFactorPurchaseToStock);
        }
    }

    public String getQuFactorPurchaseToStock() {
        return quFactorPurchaseToStock;
    }

    public int getEnableTareWeightHandling() {
        return enableTareWeightHandling;
    }

    public String getBarcode() {
        return barcode;
    }

    public double getMinStockAmountDouble() {
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

    public double getTareWeightDouble() {
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

    public double getCaloriesDouble() {
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

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setQuIdPurchase(int quIdPurchase) {
        this.quIdPurchase = quIdPurchase;
    }

    public void setQuIdStock(int quIdStock) {
        this.quIdStock = quIdStock;
    }

    public void setQuFactorPurchaseToStock(String quFactorPurchaseToStock) {
        this.quFactorPurchaseToStock = quFactorPurchaseToStock;
    }

    public void setEnableTareWeightHandling(int enableTareWeightHandling) {
        this.enableTareWeightHandling = enableTareWeightHandling;
    }

    public void setPictureFileName(String pictureFileName) {
        this.pictureFileName = pictureFileName;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setMinStockAmount(String minStockAmount) {
        this.minStockAmount = minStockAmount;
    }

    public void setDefaultBestBeforeDays(int defaultBestBeforeDays) {
        this.defaultBestBeforeDays = defaultBestBeforeDays;
    }

    public void setDefaultBestBeforeDaysAfterOpen(int defaultBestBeforeDaysAfterOpen) {
        this.defaultBestBeforeDaysAfterOpen = defaultBestBeforeDaysAfterOpen;
    }

    public void setDefaultBestBeforeDaysAfterFreezing(int defaultBestBeforeDaysAfterFreezing) {
        this.defaultBestBeforeDaysAfterFreezing = defaultBestBeforeDaysAfterFreezing;
    }

    public void setDefaultBestBeforeDaysAfterThawing(int defaultBestBeforeDaysAfterThawing) {
        this.defaultBestBeforeDaysAfterThawing = defaultBestBeforeDaysAfterThawing;
    }

    public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
        this.rowCreatedTimestamp = rowCreatedTimestamp;
    }

    public void setProductGroupId(String productGroupId) {
        this.productGroupId = productGroupId;
    }

    public void setAllowPartialUnitsInStock(int allowPartialUnitsInStock) {
        this.allowPartialUnitsInStock = allowPartialUnitsInStock;
    }

    public void setTareWeight(String tareWeight) {
        this.tareWeight = tareWeight;
    }

    public void setNotCheckStockFulfillmentForRecipes(int notCheckStockFulfillmentForRecipes) {
        this.notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes;
    }

    public void setParentProductId(String parentProductId) {
        this.parentProductId = parentProductId;
    }

    public String getMinStockAmount() {
        return minStockAmount;
    }

    public String getTareWeight() {
        return tareWeight;
    }

    public String getCalories() {
        return calories;
    }

    public void setCalories(String calories) {
        this.calories = calories;
    }

    public void setCumulateMinStockAmountOfSubProducts(int cumulateMinStockAmountOfSubProducts) {
        this.cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id == product.id &&
                locationId == product.locationId &&
                quIdPurchase == product.quIdPurchase &&
                quIdStock == product.quIdStock &&
                enableTareWeightHandling == product.enableTareWeightHandling &&
                defaultBestBeforeDays == product.defaultBestBeforeDays &&
                defaultBestBeforeDaysAfterOpen == product.defaultBestBeforeDaysAfterOpen &&
                defaultBestBeforeDaysAfterFreezing == product.defaultBestBeforeDaysAfterFreezing &&
                defaultBestBeforeDaysAfterThawing == product.defaultBestBeforeDaysAfterThawing &&
                allowPartialUnitsInStock == product.allowPartialUnitsInStock &&
                notCheckStockFulfillmentForRecipes == product.notCheckStockFulfillmentForRecipes &&
                cumulateMinStockAmountOfSubProducts == product.cumulateMinStockAmountOfSubProducts &&
                Objects.equals(name, product.name) &&
                Objects.equals(description, product.description) &&
                Objects.equals(quFactorPurchaseToStock, product.quFactorPurchaseToStock) &&
                Objects.equals(pictureFileName, product.pictureFileName) &&
                Objects.equals(barcode, product.barcode) &&
                Objects.equals(minStockAmount, product.minStockAmount) &&
                Objects.equals(rowCreatedTimestamp, product.rowCreatedTimestamp) &&
                Objects.equals(productGroupId, product.productGroupId) &&
                Objects.equals(tareWeight, product.tareWeight) &&
                Objects.equals(parentProductId, product.parentProductId) &&
                Objects.equals(calories, product.calories) &&
                Objects.equals(storeId, product.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                name,
                description,
                locationId,
                quIdPurchase,
                quIdStock,
                quFactorPurchaseToStock,
                enableTareWeightHandling,
                pictureFileName,
                barcode,
                minStockAmount,
                defaultBestBeforeDays,
                defaultBestBeforeDaysAfterOpen,
                defaultBestBeforeDaysAfterFreezing,
                defaultBestBeforeDaysAfterThawing,
                rowCreatedTimestamp,
                productGroupId,
                allowPartialUnitsInStock,
                tareWeight,
                notCheckStockFulfillmentForRecipes,
                parentProductId,
                calories,
                cumulateMinStockAmountOfSubProducts,
                storeId
        );
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
