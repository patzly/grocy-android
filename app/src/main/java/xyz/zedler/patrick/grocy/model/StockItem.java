package xyz.zedler.patrick.grocy.model;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class StockItem implements Parcelable {

    @SerializedName("amount")
    private double amount;

    @SerializedName("amount_aggregated")
    private double amountAggregated;

    @SerializedName("best_before_date")
    private String bestBeforeDate;

    @SerializedName("amount_opened")
    private double amountOpened;

    @SerializedName("amount_opened_aggregated")
    private double amountOpenedAggregated;

    @SerializedName("is_aggregated_amount")
    private int isAggregatedAmount;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("product")
    private Product product;

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

    private StockItem(Parcel parcel) {
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
