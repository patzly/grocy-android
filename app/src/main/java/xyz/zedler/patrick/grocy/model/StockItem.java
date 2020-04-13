package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockItem implements Parcelable {

    @SerializedName("amount")
    double amount;

    @SerializedName("amount_aggregated")
    double amountAggregated;

    @SerializedName("best_before_date")
    String bestBeforeDate;

    @SerializedName("amount_opened")
    double amountOpened;

    @SerializedName("amount_opened_aggregated")
    double amountOpenedAggregated;

    @SerializedName("is_aggregated_amount")
    int isAggregatedAmount;

    @SerializedName("product_id")
    int productId;

    @SerializedName("product")
    Product product;

    public StockItem(
            double amount,
            double amountAggregated,
            String bestBeforeDate,
            double amountOpened,
            double amountOpenedAggregated,
            int isAggregatedAmount,
            int productId,
            Product product
    ) {
        this.amount = amount;
        this.amountAggregated = amountAggregated;
        this.bestBeforeDate = bestBeforeDate;
        this.amountOpened = amountOpened;
        this.amountOpenedAggregated = amountOpenedAggregated;
        this.isAggregatedAmount = isAggregatedAmount;
        this.productId = productId;
        this.product = product;
    }

    public StockItem(Parcel parcel) {
        amount = parcel.readDouble();
        amountAggregated = parcel.readDouble();
        bestBeforeDate = parcel.readString();
        amountOpened = parcel.readDouble();
        amountOpenedAggregated = parcel.readDouble();
        isAggregatedAmount = parcel.readInt();
        productId = parcel.readInt();
        product = parcel.readParcelable(Product.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(amount);
        dest.writeDouble(amountAggregated);
        dest.writeString(bestBeforeDate);
        dest.writeDouble(amountOpened);
        dest.writeDouble(amountOpenedAggregated);
        dest.writeInt(isAggregatedAmount);
        dest.writeInt(productId);
        dest.writeParcelable(product, 0);
    }

    public static final Creator<StockItem> CREATOR = new Creator<StockItem>() {

        @Override
        public StockItem createFromParcel(Parcel in) {
            return new StockItem(in);
        }

        @Override
        public StockItem[] newArray(int size) {
            return new StockItem[size];
        }
    };

    public double getAmountAggregated() {
        return amountAggregated;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public double getAmountOpenedAggregated() {
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

    public double getAmount() {
        return amount;
    }

    public double getAmountOpened() {
        return amountOpened;
    }

    public void changeAmount(double difference) {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "StockItem(" + product + ")";
    }
}
