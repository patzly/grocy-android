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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "stock_item_table")
public class StockItem implements Parcelable {

  public static int DUE_TYPE_BEST_BEFORE = 1;
  public static int DUE_TYPE_EXPIRATION = 2;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "amount_aggregated")
  @SerializedName("amount_aggregated")
  private String amountAggregated;

  @ColumnInfo(name = "value")
  @SerializedName("value")
  private String value;

  @ColumnInfo(name = "best_before_date")
  @SerializedName("best_before_date")
  private String bestBeforeDate;

  @ColumnInfo(name = "amount_opened")
  @SerializedName("amount_opened")
  private String amountOpened;

  @ColumnInfo(name = "amount_opened_aggregated")
  @SerializedName("amount_opened_aggregated")
  private String amountOpenedAggregated;

  @ColumnInfo(name = "is_aggregated_amount")
  @SerializedName("is_aggregated_amount")
  private int isAggregatedAmount;

  @ColumnInfo(name = "due_type")
  @SerializedName("due_type")
  private int dueType;

  @PrimaryKey
  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @Ignore
  @SerializedName("product")
  private Product product;

  @ColumnInfo(name = "item_due")
  private boolean itemDue = false;

  @ColumnInfo(name = "item_overdue")
  private boolean itemOverdue = false;

  @ColumnInfo(name = "item_expired")
  private boolean itemExpired = false;

  @ColumnInfo(name = "item_missing")
  private boolean itemMissing = false;

  @ColumnInfo(name = "item_missing_partly_in_stock")
  private boolean itemMissingAndPartlyInStock = false;

  public StockItem() {
  }

  @Ignore
  public StockItem(ProductDetails productDetails) {
    this.amount = String.valueOf(productDetails.getStockAmount());
    this.amountAggregated = String.valueOf(productDetails.getStockAmountAggregated());
    this.value = productDetails.getStockValue();
    this.bestBeforeDate = productDetails.getNextDueDate();
    this.amountOpened = String.valueOf(productDetails.getStockAmountOpened());
    this.amountOpenedAggregated = String.valueOf(productDetails.getStockAmountOpenedAggregated());
    this.isAggregatedAmount = productDetails.getIsAggregatedAmount();
    this.dueType = productDetails.getProduct().getDueDateType();
    this.productId = productDetails.getProduct().getId();
    this.product = productDetails.getProduct();
  }

  @Ignore
  private StockItem(Parcel parcel) {
    amount = parcel.readString();
    amountAggregated = parcel.readString();
    value = parcel.readString();
    bestBeforeDate = parcel.readString();
    amountOpened = parcel.readString();
    amountOpenedAggregated = parcel.readString();
    isAggregatedAmount = parcel.readInt();
    dueType = parcel.readInt();
    productId = parcel.readInt();
    product = parcel.readParcelable(Product.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(amount);
    dest.writeString(amountAggregated);
    dest.writeString(value);
    dest.writeString(bestBeforeDate);
    dest.writeString(amountOpened);
    dest.writeString(amountOpenedAggregated);
    dest.writeInt(isAggregatedAmount);
    dest.writeInt(dueType);
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

  public double getAmountAggregatedDouble() {
    if (amountAggregated == null || amountAggregated.isEmpty()) {
      return 0;
    } else {
      return Double.parseDouble(amountAggregated);
    }
  }

  public String getValue() {
    return value;
  }

  public double getValueDouble() {
    return NumUtil.isStringDouble(value) ? Double.parseDouble(value) : 0;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public double getAmountOpenedAggregatedDouble() {
    if (amountOpenedAggregated == null || amountOpenedAggregated.isEmpty()) {
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

  public double getAmountDouble() {
    if (amount == null || amount.isEmpty()) {
      return 0;
    } else {
      return Double.parseDouble(amount);
    }
  }

  public double getAmountOpenedDouble() {
    if (amountOpened == null || amountOpened.isEmpty()) {
      return 0;
    } else {
      return Double.parseDouble(amountOpened);
    }
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public void setAmountAggregated(String amountAggregated) {
    this.amountAggregated = amountAggregated;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setBestBeforeDate(String bestBeforeDate) {
    this.bestBeforeDate = bestBeforeDate;
  }

  public void setAmountOpened(String amountOpened) {
    this.amountOpened = amountOpened;
  }

  public void setAmountOpenedAggregated(String amountOpenedAggregated) {
    this.amountOpenedAggregated = amountOpenedAggregated;
  }

  public void setIsAggregatedAmount(int isAggregatedAmount) {
    this.isAggregatedAmount = isAggregatedAmount;
  }

  public String getAmount() {
    return amount;
  }

  public String getAmountAggregated() {
    return amountAggregated;
  }

  public String getAmountOpened() {
    return amountOpened;
  }

  public String getAmountOpenedAggregated() {
    return amountOpenedAggregated;
  }

  public int getDueType() {
    return dueType;
  }

  public void setDueType(int dueType) {
    this.dueType = dueType;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public boolean isItemDue() {
    return itemDue;
  }

  public void setItemDue(boolean itemDue) {
    this.itemDue = itemDue;
  }

  public boolean isItemOverdue() {
    return itemOverdue;
  }

  public void setItemOverdue(boolean itemOverdue) {
    this.itemOverdue = itemOverdue;
  }

  public boolean isItemExpired() {
    return itemExpired;
  }

  public void setItemExpired(boolean itemExpired) {
    this.itemExpired = itemExpired;
  }

  public boolean isItemMissing() {
    return itemMissing;
  }

  public void setItemMissing(boolean itemMissing) {
    this.itemMissing = itemMissing;
  }

  public boolean isItemMissingAndPartlyInStock() {
    return itemMissingAndPartlyInStock;
  }

  public void setItemMissingAndPartlyInStock(boolean itemMissingAndPartlyInStock) {
    this.itemMissingAndPartlyInStock = itemMissingAndPartlyInStock;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StockItem stockItem = (StockItem) o;
    return isAggregatedAmount == stockItem.isAggregatedAmount &&
        productId == stockItem.productId &&
        dueType == stockItem.dueType &&
        Objects.equals(amount, stockItem.amount) &&
        Objects.equals(amountAggregated, stockItem.amountAggregated) &&
        Objects.equals(value, stockItem.value) &&
        Objects.equals(bestBeforeDate, stockItem.bestBeforeDate) &&
        Objects.equals(amountOpened, stockItem.amountOpened) &&
        Objects.equals(amountOpenedAggregated, stockItem.amountOpenedAggregated) &&
        Objects.equals(product, stockItem.product);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(amount, amountAggregated, value, bestBeforeDate, amountOpened, amountOpenedAggregated,
            isAggregatedAmount, productId, product);
  }

  @NonNull
  @Override
  public String toString() {
    return "StockItem(" + product + ")";
  }
}
