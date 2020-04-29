package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class BatchItem implements Parcelable {

    private String productName, bestBeforeDate, barcodes;
    private double amount;

    public BatchItem(
            String productName,
            String bestBeforeDate,
            String barcodes,
            double amount
    ) {
        this.productName = productName;
        this.bestBeforeDate = bestBeforeDate;
        this.barcodes = barcodes;
        this.amount = amount;
    }

    public BatchItem(Parcel parcel) {
        productName = parcel.readString();
        bestBeforeDate = parcel.readString();
        barcodes = parcel.readString();
        amount = parcel.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productName);
        dest.writeString(bestBeforeDate);
        dest.writeString(barcodes);
        dest.writeDouble(amount);
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

    public String getProductName() {
        return productName;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public String getBarcodes() {
        return barcodes;
    }

    public void setBarcodes(String barcodes) {
        this.barcodes = barcodes;
    }

    public double getAmount() {
        return amount;
    }

    public void amountOneUp() {
        amount++;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "BatchItem(" + productName + ")";
    }
}
