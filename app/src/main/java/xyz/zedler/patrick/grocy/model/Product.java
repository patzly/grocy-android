package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Product {

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

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
