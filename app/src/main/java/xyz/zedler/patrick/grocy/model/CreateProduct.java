package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class CreateProduct implements Parcelable {

    private String productName, barcodes;
    private String defaultStoreId, defaultBestBeforeDays, defaultLocationId;

    public CreateProduct(
            String productName,
            String barcodes,
            String defaultStoreId,
            String defaultBestBeforeDays,
            String defaultLocationId
    ) {
        this.productName = productName;
        this.barcodes = barcodes;
        this.defaultStoreId = defaultStoreId;
        this.defaultBestBeforeDays = defaultBestBeforeDays;
        this.defaultLocationId = defaultLocationId;
    }

    private CreateProduct(Parcel parcel) {
        productName = parcel.readString();
        barcodes = parcel.readString();
        defaultStoreId = parcel.readString();
        defaultBestBeforeDays = parcel.readString();
        defaultLocationId = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(productName);
        dest.writeString(barcodes);
        dest.writeString(defaultStoreId);
        dest.writeString(defaultBestBeforeDays);
        dest.writeString(defaultLocationId);
    }

    public static final Creator<CreateProduct> CREATOR = new Creator<CreateProduct>() {

        @Override
        public CreateProduct createFromParcel(Parcel in) {
            return new CreateProduct(in);
        }

        @Override
        public CreateProduct[] newArray(int size) {
            return new CreateProduct[size];
        }
    };

    public String getProductName() {
        return productName;
    }

    public String getBarcodes() {
        return barcodes;
    }

    public String getDefaultBestBeforeDays() {
        return defaultBestBeforeDays;
    }

    public String getDefaultLocationId() {
        return defaultLocationId;
    }

    public String getDefaultStoreId() {
        return defaultStoreId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "CreateProduct(" + productName + ")";
    }
}
