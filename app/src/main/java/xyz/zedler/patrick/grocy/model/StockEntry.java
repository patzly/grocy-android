package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockEntry implements Parcelable {

    @SerializedName("id")
    int id;

    @SerializedName("product_id")
    int productId;

    @SerializedName("amount")
    double amount;

    @SerializedName("best_before_date")
    String bestBeforeDate;

    @SerializedName("purchased_date")
    String purchasedDate;

    @SerializedName("stock_id")
    String stockId;

    @SerializedName("price")
    String price;

    @SerializedName("open")
    int open;

    @SerializedName("opened_date")
    String openedDate;

    @SerializedName("row_created_timestamp")
    String rowCreatedTimestamp;

    @SerializedName("location_id")
    int locationId;

    public StockEntry() {
        stockId = null;
    }

    public StockEntry(Parcel parcel) {
        id = parcel.readInt();
        productId = parcel.readInt();
        amount = parcel.readDouble();
        bestBeforeDate = parcel.readString();
        purchasedDate = parcel.readString();
        stockId = parcel.readString();
        price = parcel.readString();
        open = parcel.readInt();
        openedDate = parcel.readString();
        rowCreatedTimestamp = parcel.readString();
        locationId = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(productId);
        dest.writeDouble(amount);
        dest.writeString(bestBeforeDate);
        dest.writeString(purchasedDate);
        dest.writeString(stockId);
        dest.writeString(price);
        dest.writeInt(open);
        dest.writeString(openedDate);
        dest.writeString(rowCreatedTimestamp);
        dest.writeInt(locationId);
    }

    public static final Creator<StockEntry> CREATOR = new Creator<StockEntry>() {

        @Override
        public StockEntry createFromParcel(Parcel in) {
            return new StockEntry(in);
        }

        @Override
        public StockEntry[] newArray(int size) {
            return new StockEntry[size];
        }
    };

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public double getAmount() {
        return amount;
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public String getPurchasedDate() {
        return purchasedDate;
    }

    public String getStockId() {
        return stockId;
    }

    public String getPrice() {
        return price;
    }

    public int getOpen() {
        return open;
    }

    public String getOpenedDate() {
        return openedDate;
    }

    public String getRowCreatedTimestamp() {
        return rowCreatedTimestamp;
    }

    public int getLocationId() {
        return locationId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "StockEntry(" + productId + ")";
    }
}
