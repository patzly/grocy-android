package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class ProductDetails {

    @SerializedName("product")
    Product product;

    @SerializedName("last_purchased")
    String lastPurchased;

    @SerializedName("last_used")
    String lastUsed;

    @SerializedName("stock_amount")
    double stockAmount;

    @SerializedName("stock_amount_opened")
    double stockAmountOpened;

    @SerializedName("stock_amount_aggregated")
    double stockAmountAggregated;

    @SerializedName("stock_amount_opened_aggregated")
    double stockAmountOpenedAggregated;

    @SerializedName("quantity_unit_purchase")
    QuantityUnit quantityUnitPurchase;

    @SerializedName("quantity_unit_stock")
    QuantityUnit quantityUnitStock;

    @SerializedName("last_price")
    String lastPrice;

    @SerializedName("next_best_before_date")
    String nextBestBeforeDate;

    @SerializedName("location")
    Location location;

    @SerializedName("average_shelf_life_days")
    int averageShelfLifeDays;

    @SerializedName("spoil_rate_percent")
    double spoilRatePercent;

    @SerializedName("is_aggregated_amount")
    int isAggregatedAmount;

    public Product getProduct() {
        return product;
    }

    public String getLastPurchased() {
        return lastPurchased;
    }

    public String getLastUsed() {
        return lastUsed;
    }

    public double getStockAmount() {
        return stockAmount;
    }

    public double getStockAmountOpened() {
        return stockAmountOpened;
    }

    public double getStockAmountAggregated() {
        return stockAmountAggregated;
    }

    public double getStockAmountOpenedAggregated() {
        return stockAmountOpenedAggregated;
    }

    public QuantityUnit getQuantityUnitPurchase() {
        return quantityUnitPurchase;
    }

    public QuantityUnit getQuantityUnitStock() {
        return quantityUnitStock;
    }

    public String getLastPrice() {
        return lastPrice;
    }

    public String getNextBestBeforeDate() {
        return nextBestBeforeDate;
    }

    public Location getLocation() {
        return location;
    }

    public int getAverageShelfLifeDays() {
        return averageShelfLifeDays;
    }

    public double getSpoilRatePercent() {
        return spoilRatePercent;
    }

    public int getIsAggregatedAmount() {
        return isAggregatedAmount;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductDetails(" + product + ')';
    }
}
