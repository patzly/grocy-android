package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Product implements Parcelable {

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("description")
    String description;

    @SerializedName("location_id")
    int locationId;

    @SerializedName("qu_id_purchase")
    int quIdPurchase; // quantity unit

    @SerializedName("qu_id_stock")
    int quIdStock; // quantity unit

    @SerializedName("qu_factor_purchase_to_stock")
    double quFactorPurchaseToStock; // quantity unit

    @SerializedName("enable_tare_weight_handling")
    int enableTareWeightHandling;

    @SerializedName("picture_file_name")
    String pictureFileName;

    @SerializedName("barcode")
    String barcode;

    @SerializedName("min_stock_amount")
    int minStockAmount;

    @SerializedName("default_best_before_days")
    int defaultBestBeforeDays;

    @SerializedName("default_best_before_days_after_open")
    int defaultBestBeforeDaysAfterOpen;

    @SerializedName("default_best_before_days_after_freezing")
    int defaultBestBeforeDaysAfterFreezing;

    @SerializedName("default_best_before_days_after_thawing")
    int defaultBestBeforeDaysAfterThawing;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    @SerializedName("product_group_id")
    String productGroupId;

    @SerializedName("allow_partial_units_in_stock")
    int allowPartialUnitsInStock;

    @SerializedName("tare_weight")
    double tareWeight;

    @SerializedName("not_check_stock_fulfillment_for_recipes")
    int notCheckStockFulfillmentForRecipes;

    @SerializedName("parent_product_id")
    String parentProductId; /// STRING: null for empty

    @SerializedName("calories")
    int calories;

    @SerializedName("cumulate_min_stock_amount_of_sub_products")
    int cumulateMinStockAmountOfSubProducts;

    public Product(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        description = parcel.readString();
        locationId = parcel.readInt();
        quIdPurchase = parcel.readInt();
        quIdStock = parcel.readInt();
        quFactorPurchaseToStock = parcel.readDouble();
        enableTareWeightHandling = parcel.readInt();
        pictureFileName = parcel.readString();
        barcode = parcel.readString();
        minStockAmount = parcel.readInt();
        defaultBestBeforeDays = parcel.readInt();
        defaultBestBeforeDaysAfterOpen = parcel.readInt();
        defaultBestBeforeDaysAfterFreezing = parcel.readInt();
        defaultBestBeforeDaysAfterThawing = parcel.readInt();
        rowCreatedTimestamp = parcel.readString();
        productGroupId = parcel.readString();
        allowPartialUnitsInStock = parcel.readInt();
        tareWeight = parcel.readDouble();
        notCheckStockFulfillmentForRecipes = parcel.readInt();
        parentProductId = parcel.readString();
        calories = parcel.readInt();
        cumulateMinStockAmountOfSubProducts = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeInt(locationId);
        dest.writeInt(quIdPurchase);
        dest.writeInt(quIdStock);
        dest.writeDouble(quFactorPurchaseToStock);
        dest.writeInt(enableTareWeightHandling);
        dest.writeString(pictureFileName);
        dest.writeString(barcode);
        dest.writeInt(minStockAmount);
        dest.writeInt(defaultBestBeforeDays);
        dest.writeInt(defaultBestBeforeDaysAfterOpen);
        dest.writeInt(defaultBestBeforeDaysAfterFreezing);
        dest.writeInt(defaultBestBeforeDaysAfterThawing);
        dest.writeString(rowCreatedTimestamp);
        dest.writeString(productGroupId);
        dest.writeInt(allowPartialUnitsInStock);
        dest.writeDouble(tareWeight);
        dest.writeInt(notCheckStockFulfillmentForRecipes);
        dest.writeString(parentProductId);
        dest.writeInt(calories);
        dest.writeInt(cumulateMinStockAmountOfSubProducts);
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

    public double getQuFactorPurchaseToStock() {
        return quFactorPurchaseToStock;
    }

    public int getEnableTareWeightHandling() {
        return enableTareWeightHandling;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getMinStockAmount() {
        return minStockAmount;
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
        return tareWeight;
    }

    public int getNotCheckStockFulfillmentForRecipes() {
        return notCheckStockFulfillmentForRecipes;
    }

    public String getParentProductId() {
        return parentProductId;
    }

    public int getCalories() {
        return calories;
    }

    public int getCumulateMinStockAmountOfSubProducts() {
        return cumulateMinStockAmountOfSubProducts;
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
