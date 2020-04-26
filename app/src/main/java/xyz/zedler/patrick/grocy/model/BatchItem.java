package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class BatchItem implements Parcelable {

    @SerializedName("amount")
    double amount;

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

    public BatchItem(
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
        this.bestBeforeDate = bestBeforeDate;
        this.amountOpened = amountOpened;
        this.amountOpenedAggregated = amountOpenedAggregated;
        this.isAggregatedAmount = isAggregatedAmount;
        this.productId = productId;
        this.product = product;
    }

    public BatchItem(Parcel parcel) {
        amount = parcel.readDouble();
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
        dest.writeString(bestBeforeDate);
        dest.writeDouble(amountOpened);
        dest.writeDouble(amountOpenedAggregated);
        dest.writeInt(isAggregatedAmount);
        dest.writeInt(productId);
        dest.writeParcelable(product, 0);
    }

    public static final Creator<BatchItem> CREATOR = new Creator<BatchItem>() {

        @Override
        public BatchItem createFromParcel(Parcel in) {
            return new BatchItem(in);
        }

        @Override
        public BatchItem[] newArray(int size) {
            return new BatchItem[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "BatchItem(" + product + ")";
    }
}
