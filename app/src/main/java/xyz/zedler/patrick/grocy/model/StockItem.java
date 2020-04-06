package xyz.zedler.patrick.grocy.model;

import com.google.gson.annotations.SerializedName;

public class StockItem {

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

    // ONLY VOLATILE

    @SerializedName("id")
    int id;

    @SerializedName("name")
    String name;

    @SerializedName("amount_missing")
    int amountMissing;

    @SerializedName("is_partly_in_stock")
    int isPartlyInStock;

    // GETTERS

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

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAmountMissing() {
        return amountMissing;
    }

    public int getIsPartlyInStock() {
        return isPartlyInStock;
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

    public int getPropertyCount() {
        // amount + product
        return 1 + getProduct().getDisplayedPropertiesCount();
    }

    public void removeConsumed() {
        if(amount > 0) amount--;
    }

    public void addOpened() {
        if(amount > amountOpened) amountOpened++;
    }

    public void removeOpened() {
        if(amountOpened > 0) amountOpened--;
    }
}
