package xyz.zedler.patrick.grocy.model;

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
    private int locationId;

    @SerializedName("qu_id_purchase")
    private int quIdPurchase; // quantity unit

    @SerializedName("qu_id_stock")
    private int quIdStock; // quantity unit

    @SerializedName("qu_factor_purchase_to_stock")
    private double quFactorPurchaseToStock; // quantity unit

    @SerializedName("enable_tare_weight_handling")
    private int enableTareWeightHandling;

    @SerializedName("picture_file_name")
    private String pictureFileName;

    @SerializedName("barcode")
    private String barcode;

    @SerializedName("min_stock_amount")
    private double minStockAmount;

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
    private double tareWeight;

    @SerializedName("not_check_stock_fulfillment_for_recipes")
    private int notCheckStockFulfillmentForRecipes;

    @SerializedName("parent_product_id")
    private String parentProductId; /// STRING: null for empty

    @SerializedName("calories")
    private double calories;

    @SerializedName("cumulate_min_stock_amount_of_sub_products")
    private int cumulateMinStockAmountOfSubProducts;

    @SerializedName("shopping_location_id")
    private String storeId;

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
        minStockAmount = parcel.readDouble();
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
        storeId = parcel.readString();
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
        dest.writeDouble(minStockAmount);
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
        dest.writeDouble(calories);
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

    public double getMinStockAmount() {
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

    public double getCalories() {
        return calories;
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
