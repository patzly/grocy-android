package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MissingBatchItem implements Parcelable {

    private String productName, barcodes, defaultBestBeforeDate;
    private int amount;

    public MissingBatchItem(
            String productName,
            String barcodes,
            String defaultBestBeforeDate,
            int amount
    ) {
        this.productName = productName;
        this.barcodes = barcodes;
        this.defaultBestBeforeDate = defaultBestBeforeDate;
        this.amount = amount;
    }

    public MissingBatchItem(Parcel parcel) {
        productName = parcel.readString();
        barcodes = parcel.readString();
        defaultBestBeforeDate = parcel.readString();
        amount = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productName);
        dest.writeString(barcodes);
        dest.writeString(defaultBestBeforeDate);
        dest.writeInt(amount);
    }

    public static final Creator<MissingBatchItem> CREATOR = new Creator<MissingBatchItem>() {

        @Override
        public MissingBatchItem createFromParcel(Parcel in) {
            return new MissingBatchItem(in);
        }

        @Override
        public MissingBatchItem[] newArray(int size) {
            return new MissingBatchItem[size];
        }
    };

    public String getProductName() {
        return productName;
    }

    public String getBarcodes() {
        return barcodes;
    }

    public String getDefaultBestBeforeDate() {
        return defaultBestBeforeDate;
    }

    public void setBarcodes(String barcodes) {
        this.barcodes = barcodes;
    }

    public int getAmount() {
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
        return "MissingBatchItem(" + productName + ")";
    }
}
