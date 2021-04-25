/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ProductDetails implements Parcelable {

    @SerializedName("product")
    private final Product product;

    @SerializedName("product_barcodes")
    private final ArrayList<ProductBarcode> productBarcodes;

    @SerializedName("last_purchased")
    private final String lastPurchased;

    @SerializedName("last_used")
    private final String lastUsed;

    @SerializedName("stock_amount")
    private final String stockAmount;

    @SerializedName("stock_value")
    private final String stockValue;

    @SerializedName("stock_amount_opened")
    private final String stockAmountOpened;

    @SerializedName("stock_amount_aggregated")
    private final String stockAmountAggregated;

    @SerializedName("stock_amount_opened_aggregated")
    private final String stockAmountOpenedAggregated;

    @SerializedName("quantity_unit_purchase")
    private final QuantityUnit quantityUnitPurchase;

    @SerializedName("quantity_unit_stock")
    private final QuantityUnit quantityUnitStock;

    @SerializedName("last_price")
    private final String lastPrice;

    @SerializedName("avg_price")
    private final String avgPrice;

    @SerializedName("oldest_price")
    private final String oldestPrice;

    @SerializedName("last_shopping_location_id")
    private final String lastShoppingLocationId;

    @SerializedName("default_shopping_location_id")
    private final String defaultShoppingLocationId;

    @SerializedName("next_due_date")
    private final String nextDueDate;

    @SerializedName("location")
    private final Location location;

    @SerializedName("average_shelf_life_days")
    private final int averageShelfLifeDays;

    @SerializedName("spoil_rate_percent")
    private final String spoilRatePercent;

    @SerializedName("is_aggregated_amount")
    private final int isAggregatedAmount;

    public ProductDetails(Parcel parcel) {
        product = parcel.readParcelable(Product.class.getClassLoader());
        // productBarcodes = parcel.readParcelableList(); // TODO ?
        productBarcodes = null;
        lastPurchased = parcel.readString();
        lastUsed = parcel.readString();
        stockAmount = parcel.readString();
        stockValue = parcel.readString();
        stockAmountOpened = parcel.readString();
        stockAmountAggregated = parcel.readString();
        stockAmountOpenedAggregated = parcel.readString();
        quantityUnitPurchase = parcel.readParcelable(QuantityUnit.class.getClassLoader());
        quantityUnitStock = parcel.readParcelable(QuantityUnit.class.getClassLoader());
        lastPrice = parcel.readString();
        avgPrice = parcel.readString();
        oldestPrice = parcel.readString();
        lastShoppingLocationId = parcel.readString();
        defaultShoppingLocationId = parcel.readString();
        nextDueDate = parcel.readString();
        location = parcel.readParcelable(Location.class.getClassLoader());
        averageShelfLifeDays = parcel.readInt();
        spoilRatePercent = parcel.readString();
        isAggregatedAmount = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(product, 0);
        // dest.writeParcelableList(productBarcodes); // TODO ?
        dest.writeString(lastPurchased);
        dest.writeString(lastUsed);
        dest.writeString(stockAmount);
        dest.writeString(stockValue);
        dest.writeString(stockAmountOpened);
        dest.writeString(stockAmountAggregated);
        dest.writeString(stockAmountOpenedAggregated);
        dest.writeParcelable(quantityUnitPurchase, 0);
        dest.writeParcelable(quantityUnitStock, 0);
        dest.writeString(lastPrice);
        dest.writeString(avgPrice);
        dest.writeString(oldestPrice);
        dest.writeString(lastShoppingLocationId);
        dest.writeString(defaultShoppingLocationId);
        dest.writeString(nextDueDate);
        dest.writeParcelable(location, 0);
        dest.writeInt(averageShelfLifeDays);
        dest.writeString(spoilRatePercent);
        dest.writeInt(isAggregatedAmount);
    }

    public static final Creator<ProductDetails> CREATOR = new Creator<ProductDetails>() {

        @Override
        public ProductDetails createFromParcel(Parcel in) {
            return new ProductDetails(in);
        }

        @Override
        public ProductDetails[] newArray(int size) {
            return new ProductDetails[size];
        }
    };

    public Product getProduct() {
        return product;
    }

    public String getLastPurchased() {
        return lastPurchased;
    }

    public String getLastUsed() {
        return lastUsed;
    }

    public double getStockAmount() {
        if(stockAmount == null || stockAmount.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(stockAmount);
        }
    }

    public double getStockAmountOpened() {
        if(stockAmountOpened == null || stockAmountOpened.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(stockAmountOpened);
        }
    }

    public double getStockAmountAggregated() {
        if(stockAmountAggregated == null || stockAmountAggregated.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(stockAmountAggregated);
        }
    }

    public double getStockAmountOpenedAggregated() {
        if(stockAmountOpenedAggregated == null || stockAmountOpenedAggregated.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(stockAmountOpenedAggregated);
        }
    }

    public QuantityUnit getQuantityUnitPurchase() {
        return quantityUnitPurchase;
    }

    public QuantityUnit getQuantityUnitStock() {
        return quantityUnitStock;
    }

    public String getLastPrice() {
        return lastPrice;
    }

    public String getNextDueDate() {
        return nextDueDate;
    }

    public Location getLocation() {
        return location;
    }

    public int getAverageShelfLifeDays() {
        return averageShelfLifeDays;
    }

    public double getSpoilRatePercent() {
        if(spoilRatePercent == null || spoilRatePercent.isEmpty()) {
            return 0;
        } else {
            return Double.parseDouble(spoilRatePercent);
        }
    }

    public int getIsAggregatedAmount() {
        return isAggregatedAmount;
    }

    public String getLastShoppingLocationId() {
        return lastShoppingLocationId;
    }

    public String getDefaultShoppingLocationId() {
        return defaultShoppingLocationId;
    }

    public ArrayList<ProductBarcode> getProductBarcodes() {
        return productBarcodes;
    }

    public String getStockValue() {
        return stockValue;
    }

    public String getAvgPrice() {
        return avgPrice;
    }

    public String getOldestPrice() {
        return oldestPrice;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductDetails(" + product + ')';
    }
}
