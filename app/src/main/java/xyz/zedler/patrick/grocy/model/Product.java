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

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import xyz.zedler.patrick.grocy.util.Constants;

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

    @ColumnInfo(name = "product_group_id")
    @SerializedName("product_group_id")
    private String productGroupId;

    @ColumnInfo(name = "active")
    @SerializedName("active")
    private int active;

    @ColumnInfo(name = "location_id")
    @SerializedName("location_id")
    private String locationId;

    @ColumnInfo(name = "shopping_location_id")
    @SerializedName("shopping_location_id")
    private String storeId;

    @ColumnInfo(name = "qu_id_purchase")
    @SerializedName("qu_id_purchase")
    private int quIdPurchase; // quantity unit

    @ColumnInfo(name = "qu_id_stock")
    @SerializedName("qu_id_stock")
    private int quIdStock; // quantity unit

    @ColumnInfo(name = "qu_factor_purchase_to_stock")
    @SerializedName("qu_factor_purchase_to_stock")
    private String quFactorPurchaseToStock; // quantity unit

    @ColumnInfo(name = "min_stock_amount")
    @SerializedName("min_stock_amount")
    private String minStockAmount;

    @ColumnInfo(name = "default_best_before_days")
    @SerializedName("default_best_before_days")
    private int defaultDueDays;

    @ColumnInfo(name = "default_best_before_days_after_open")
    @SerializedName("default_best_before_days_after_open")
    private int defaultDueDaysAfterOpen;

    @ColumnInfo(name = "default_best_before_days_after_freezing")
    @SerializedName("default_best_before_days_after_freezing")
    private int defaultDueDaysAfterFreezing;

    @ColumnInfo(name = "default_best_before_days_after_thawing")
    @SerializedName("default_best_before_days_after_thawing")
    private int defaultDueDaysAfterThawing;

    @ColumnInfo(name = "picture_file_name")
    @SerializedName("picture_file_name")
    private String pictureFileName;

    @ColumnInfo(name = "enable_tare_weight_handling")
    @SerializedName("enable_tare_weight_handling")
    private int enableTareWeightHandling;

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
    private int accumulateSubProductsMinStockAmount;

    @ColumnInfo(name = "due_type")
    @SerializedName("due_type")
    private int dueDateType;

    @ColumnInfo(name = "quick_consume_amount")
    @SerializedName("quick_consume_amount")
    private String quickConsumeAmount;

    @ColumnInfo(name = "hide_on_stock_overview")
    @SerializedName("hide_on_stock_overview")
    private int hideOnStockOverview;

    @ColumnInfo(name = "row_created_timestamp")
    @SerializedName("row_created_timestamp")
    private String rowCreatedTimestamp;

    public Product() {}  // for Room

    @Ignore
    public Product(SharedPreferences sharedPrefs) {
        int presetLocationId = sharedPrefs.getInt(
                Constants.SETTINGS.PRESETS.LOCATION,
                Constants.SETTINGS_DEFAULT.PRESETS.LOCATION
        );
        int presetProductGroupId = sharedPrefs.getInt(
                Constants.SETTINGS.PRESETS.PRODUCT_GROUP,
                Constants.SETTINGS_DEFAULT.PRESETS.PRODUCT_GROUP
        );
        int presetQuId = sharedPrefs.getInt(
                Constants.SETTINGS.PRESETS.QUANTITY_UNIT,
                Constants.SETTINGS_DEFAULT.PRESETS.QUANTITY_UNIT
        );
        name = null;  // initialize default values (used in masterProductFragment)
        active = 1;
        parentProductId = null;
        description = null;
        locationId = presetLocationId == -1 ? null : String.valueOf(presetLocationId);
        storeId = null;
        minStockAmount = String.valueOf(0);
        accumulateSubProductsMinStockAmount = 0;
        dueDateType = 1;
        defaultDueDays = 0;
        defaultDueDaysAfterOpen = 0;
        productGroupId = presetProductGroupId == -1 ? null : String.valueOf(presetProductGroupId);
        quIdStock = presetQuId;
        quIdPurchase = presetQuId;
        quFactorPurchaseToStock = String.valueOf(1);
        enableTareWeightHandling = 0;
        tareWeight = String.valueOf(0);
        notCheckStockFulfillmentForRecipes = 0;
        calories = String.valueOf(0);
        defaultDueDaysAfterFreezing = 0;
        defaultDueDaysAfterThawing = 0;
        quickConsumeAmount = String.valueOf(1);
        hideOnStockOverview = 0;
        // TODO: Use sharedPrefs for values which can be configured in settings (like default location etc...)
    }

    @Ignore
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

    @Ignore
    public Product(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        productGroupId = parcel.readString();
        active = parcel.readInt();
        locationId = parcel.readString();
        storeId = parcel.readString();
        quIdPurchase = parcel.readInt();
        quIdStock = parcel.readInt();
        quFactorPurchaseToStock = parcel.readString();
        minStockAmount = parcel.readString();
        defaultDueDays = parcel.readInt();
        defaultDueDaysAfterOpen = parcel.readInt();
        defaultDueDaysAfterFreezing = parcel.readInt();
        defaultDueDaysAfterThawing = parcel.readInt();
        pictureFileName = parcel.readString();
        enableTareWeightHandling = parcel.readInt();
        tareWeight = parcel.readString();
        notCheckStockFulfillmentForRecipes = parcel.readInt();
        parentProductId = parcel.readString();
        calories = parcel.readString();
        accumulateSubProductsMinStockAmount = parcel.readInt();
        dueDateType = parcel.readInt();
        quickConsumeAmount = parcel.readString();
        hideOnStockOverview = parcel.readInt();
        rowCreatedTimestamp = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(productGroupId);
        dest.writeInt(active);
        dest.writeString(locationId);
        dest.writeString(storeId);
        dest.writeInt(quIdPurchase);
        dest.writeInt(quIdStock);
        dest.writeString(quFactorPurchaseToStock);
        dest.writeString(minStockAmount);
        dest.writeInt(defaultDueDays);
        dest.writeInt(defaultDueDaysAfterOpen);
        dest.writeInt(defaultDueDaysAfterFreezing);
        dest.writeInt(defaultDueDaysAfterThawing);
        dest.writeString(pictureFileName);
        dest.writeInt(enableTareWeightHandling);
        dest.writeString(tareWeight);
        dest.writeInt(notCheckStockFulfillmentForRecipes);
        dest.writeString(parentProductId);
        dest.writeString(calories);
        dest.writeInt(accumulateSubProductsMinStockAmount);
        dest.writeInt(dueDateType);
        dest.writeString(quickConsumeAmount);
        dest.writeInt(hideOnStockOverview);
        dest.writeString(rowCreatedTimestamp);
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

    @Deprecated
    public String getBarcode() {
        return null;
    }

    public double getMinStockAmountDouble() {
        if(minStockAmount == null || minStockAmount.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(minStockAmount);
        }
    }

    public int getDefaultDueDays() {
        return defaultDueDays;
    }

    public int getDefaultDueDaysAfterOpen() {
        return defaultDueDaysAfterOpen;
    }

    public int getDefaultDueDaysAfterFreezing() {
        return defaultDueDaysAfterFreezing;
    }

    public int getDefaultDueDaysAfterThawing() {
        return defaultDueDaysAfterThawing;
    }

    public String getRowCreatedTimestamp() {
        return rowCreatedTimestamp;
    }

    public String getProductGroupId() {
        return productGroupId;
    }

    @Deprecated
    public int getAllowPartialUnitsInStock() {
        return 0;
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

    public int getAccumulateSubProductsMinStockAmount() {
        return accumulateSubProductsMinStockAmount;
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

    @Deprecated
    public void setBarcode(String barcode) { }

    public void setMinStockAmount(String minStockAmount) {
        this.minStockAmount = minStockAmount;
    }

    public void setDefaultDueDays(int defaultDueDays) {
        this.defaultDueDays = defaultDueDays;
    }

    public void setDefaultDueDaysAfterOpen(int defaultDueDaysAfterOpen) {
        this.defaultDueDaysAfterOpen = defaultDueDaysAfterOpen;
    }

    public void setDefaultDueDaysAfterFreezing(int defaultDueDaysAfterFreezing) {
        this.defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing;
    }

    public void setDefaultDueDaysAfterThawing(int defaultDueDaysAfterThawing) {
        this.defaultDueDaysAfterThawing = defaultDueDaysAfterThawing;
    }

    public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
        this.rowCreatedTimestamp = rowCreatedTimestamp;
    }

    public void setProductGroupId(String productGroupId) {
        this.productGroupId = productGroupId;
    }

    @Deprecated
    public void setAllowPartialUnitsInStock(int allowPartialUnitsInStock) {}

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

    public void setAccumulateSubProductsMinStockAmount(int accumulateSubProductsMinStockAmount) {
        this.accumulateSubProductsMinStockAmount = accumulateSubProductsMinStockAmount;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public int getActive() {
        return active;
    }

    public boolean isActive() {
        return active == 1;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public int getDueDateType() {
        return dueDateType;
    }

    public void setDueDateType(int dueDateType) {
        this.dueDateType = dueDateType;
    }

    public String getQuickConsumeAmount() {
        return quickConsumeAmount;
    }

    public void setQuickConsumeAmount(String quickConsumeAmount) {
        this.quickConsumeAmount = quickConsumeAmount;
    }

    public int getHideOnStockOverview() {
        return hideOnStockOverview;
    }

    public void setHideOnStockOverview(int hideOnStockOverview) {
        this.hideOnStockOverview = hideOnStockOverview;
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
                active == product.active &&
                quIdPurchase == product.quIdPurchase &&
                quIdStock == product.quIdStock &&
                defaultDueDays == product.defaultDueDays &&
                defaultDueDaysAfterOpen == product.defaultDueDaysAfterOpen &&
                defaultDueDaysAfterFreezing == product.defaultDueDaysAfterFreezing &&
                defaultDueDaysAfterThawing == product.defaultDueDaysAfterThawing &&
                enableTareWeightHandling == product.enableTareWeightHandling &&
                notCheckStockFulfillmentForRecipes == product.notCheckStockFulfillmentForRecipes &&
                accumulateSubProductsMinStockAmount == product.accumulateSubProductsMinStockAmount &&
                dueDateType == product.dueDateType &&
                hideOnStockOverview == product.hideOnStockOverview &&
                Objects.equals(name, product.name) &&
                Objects.equals(description, product.description) &&
                Objects.equals(productGroupId, product.productGroupId) &&
                Objects.equals(locationId, product.locationId) &&
                Objects.equals(storeId, product.storeId) &&
                Objects.equals(quFactorPurchaseToStock, product.quFactorPurchaseToStock) &&
                Objects.equals(minStockAmount, product.minStockAmount) &&
                Objects.equals(pictureFileName, product.pictureFileName) &&
                Objects.equals(tareWeight, product.tareWeight) &&
                Objects.equals(parentProductId, product.parentProductId) &&
                Objects.equals(calories, product.calories) &&
                Objects.equals(quickConsumeAmount, product.quickConsumeAmount) &&
                Objects.equals(rowCreatedTimestamp, product.rowCreatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, productGroupId, active, locationId, storeId,
                quIdPurchase, quIdStock, quFactorPurchaseToStock, minStockAmount,
                defaultDueDays, defaultDueDaysAfterOpen,
                defaultDueDaysAfterFreezing, defaultDueDaysAfterThawing,
                pictureFileName, enableTareWeightHandling, tareWeight,
                notCheckStockFulfillmentForRecipes, parentProductId, calories,
                accumulateSubProductsMinStockAmount, dueDateType, quickConsumeAmount,
                hideOnStockOverview, rowCreatedTimestamp);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
