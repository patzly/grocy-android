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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import com.google.gson.annotations.SerializedName;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "stock_item_table")
public class StockLogEntry implements Parcelable {

  @SerializedName("id")
  private int id;

  @SerializedName("product_id")
  private int productId;

  @SerializedName("amount")
  private String amount;

  @SerializedName("best_before_date")
  private String bestBeforeDate;

  @SerializedName("purchased_date")
  private String purchasedDate;

  @SerializedName("used_date")
  private String usedDate;

  @SerializedName("spoiled")
  private String spoiled;

  @SerializedName("stock_id")
  private String stockId;

  @SerializedName("transaction_type")
  private String transactionType;

  @SerializedName("price")
  private String price;

  @SerializedName("undone")
  private String undone;

  @SerializedName("undone_timestamp")
  private String undoneTimestamp;

  @SerializedName("opened_date")
  private String openedDate;

  @SerializedName("location_id")
  private String locationId;

  @SerializedName("recipe_id")
  private String recipeId;

  @SerializedName("correlation_id")
  private String correlationId;

  @SerializedName("transaction_id")
  private String transactionId;

  @SerializedName("stock_row_id")
  private String stockRowId;

  @SerializedName("shopping_location_id")
  private String shoppingLocationId;

  @SerializedName("user_id")
  private String userId;

  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  @SerializedName("note")
  private String note;

  public StockLogEntry() {}

  private StockLogEntry(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    amount = parcel.readString();
    bestBeforeDate = parcel.readString();
    purchasedDate = parcel.readString();
    usedDate = parcel.readString();
    spoiled = parcel.readString();
    stockId = parcel.readString();
    transactionType = parcel.readString();
    price = parcel.readString();
    undone = parcel.readString();
    undoneTimestamp = parcel.readString();
    openedDate = parcel.readString();
    locationId = parcel.readString();
    recipeId = parcel.readString();
    correlationId = parcel.readString();
    transactionId = parcel.readString();
    stockRowId = parcel.readString();
    shoppingLocationId = parcel.readString();
    userId = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
    note = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeString(amount);
    dest.writeString(bestBeforeDate);
    dest.writeString(purchasedDate);
    dest.writeString(usedDate);
    dest.writeString(spoiled);
    dest.writeString(stockId);
    dest.writeString(transactionType);
    dest.writeString(price);
    dest.writeString(undone);
    dest.writeString(undoneTimestamp);
    dest.writeString(openedDate);
    dest.writeString(locationId);
    dest.writeString(recipeId);
    dest.writeString(correlationId);
    dest.writeString(transactionId);
    dest.writeString(stockRowId);
    dest.writeString(shoppingLocationId);
    dest.writeString(userId);
    dest.writeString(rowCreatedTimestamp);
    dest.writeString(note);
  }

  public static final Creator<StockLogEntry> CREATOR = new Creator<StockLogEntry>() {

    @Override
    public StockLogEntry createFromParcel(Parcel in) {
      return new StockLogEntry(in);
    }

    @Override
    public StockLogEntry[] newArray(int size) {
      return new StockLogEntry[size];
    }
  };

  public int getId() {
    return id;
  }

  public int getProductId() {
    return productId;
  }

  public String getAmount() {
    return amount;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public String getPurchasedDate() {
    return purchasedDate;
  }

  public String getUsedDate() {
    return usedDate;
  }

  public String getSpoiled() {
    return spoiled;
  }

  public String getStockId() {
    return stockId;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public String getPrice() {
    return price;
  }

  public String getUndone() {
    return undone;
  }

  public boolean getUndoneBoolean() {
    return NumUtil.isStringInt(undone) && Integer.parseInt(undone) == 1;
  }

  public String getUndoneTimestamp() {
    return undoneTimestamp;
  }

  public String getOpenedDate() {
    return openedDate;
  }

  public String getLocationId() {
    return locationId;
  }

  public String getRecipeId() {
    return recipeId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getStockRowId() {
    return stockRowId;
  }

  public String getShoppingLocationId() {
    return shoppingLocationId;
  }

  public String getUserId() {
    return userId;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public String getNote() {
    return note;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "StockLogEntry(" + productId + ")";
  }
}
