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
    private String amount;

    @SerializedName("amount_aggregated")
    private String amountAggregated;

    @SerializedName("best_before_date")
    private String bestBeforeDate;

    @SerializedName("amount_opened")
    private String amountOpened;

    @SerializedName("amount_opened_aggregated")
    private String amountOpenedAggregated;

    @SerializedName("is_aggregated_amount")
    private int isAggregatedAmount;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("product")
    private Product product;

    public StockItem(ProductDetails productDetails) {
        this.amount = String.valueOf(productDetails.getStockAmount());
        this.amountAggregated = String.valueOf(productDetails.getStockAmountAggregated());
        this.bestBeforeDate = productDetails.getNextBestBeforeDate();
        this.amountOpened = String.valueOf(productDetails.getStockAmountOpened());
        this.amountOpenedAggregated = String.valueOf(productDetails.getStockAmountOpenedAggregated());
        this.isAggregatedAmount = productDetails.getIsAggregatedAmount();
        this.productId = productDetails.getProduct().getId();
        this.product = productDetails.getProduct();
    }

    private StockItem(Parcel parcel) {
        amount = parcel.readString();
        amountAggregated = parcel.readString();
        bestBeforeDate = parcel.readString();
        amountOpened = parcel.readString();
        amountOpenedAggregated = parcel.readString();
        isAggregatedAmount = parcel.readInt();
        productId = parcel.readInt();
        product = parcel.readParcelable(Product.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(amount);
        dest.writeString(amountAggregated);
        dest.writeString(bestBeforeDate);
        dest.writeString(amountOpened);
        dest.writeString(amountOpenedAggregated);
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
        if(amountAggregated == null || amountAggregated.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amountAggregated);
        }
    }

    public String getBestBeforeDate() {
        return bestBeforeDate;
    }

    public double getAmountOpenedAggregated() {
        if(amountOpenedAggregated == null || amountOpenedAggregated.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amountOpenedAggregated);
        }
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
        if(amount == null || amount.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amount);
        }
    }

    public double getAmountOpened() {
        if(amountOpened == null || amountOpened.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(amountOpened);
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
