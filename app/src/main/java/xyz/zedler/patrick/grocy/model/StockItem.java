package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockItem {

    public StockItem(
            int amount,
            int amount_aggregated,
            String best_before_date,
            int amount_opened,
            int amount_opened_aggregated,
            int is_aggregated_amount,
            int product_id,
            Product product
    ) {
        this.amount = amount;
        this.amountAggregated = amount_aggregated;
        this.bestBeforeDate = best_before_date;
        this.amountOpened = amount_opened;
        this.amountOpenedAggregated = amount_opened_aggregated;
        this.isAggregatedAmount = is_aggregated_amount;
        this.productId = product_id;
        this.product = product;
    }

    @SerializedName("amount")
    int amount;

    @SerializedName("amount_aggregated")
    int amountAggregated;

    @SerializedName("best_before_date")
    String bestBeforeDate;

    @SerializedName("amount_opened")
    int amountOpened;

    @SerializedName("amount_opened_aggregated")
    int amountOpenedAggregated;

    @SerializedName("is_aggregated_amount")
    int isAggregatedAmount;

    @SerializedName("product_id")
    int productId;

    @SerializedName("product")
    Product product;

    public int getAmountAggregated() {
        return amountAggregated;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public int getAmountOpenedAggregated() {
        return amountOpenedAggregated;
    }

    public int getIsAggregatedAmount() {
        return isAggregatedAmount;
    }

    public int getProductId() {
        return productId;
    }

    public Product getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }

    public int getAmountOpened() {
        return amountOpened;
    }

    public void changeAmount(int difference) {
        if(difference > 0 || amount > 0) {
            amount = amount + difference;
            if(difference == -1 && amountOpened > 0) {
                amountOpened--;
            }
        }
    }

    public void changeAmountOpened(int difference) {
        if(amount > 0 && amountOpened + difference <= amount) {
            amountOpened = amountOpened + difference;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "StockItem(" + product + ")";
    }
}
