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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "product_barcode_table")
public class ProductBarcode implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "barcode")
  @SerializedName("barcode")
  private String barcode;

  @ColumnInfo(name = "qu_id")
  @SerializedName("qu_id")
  private String quId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "shopping_location_id")
  @SerializedName("shopping_location_id")
  private String storeId;

  @ColumnInfo(name = "last_price")
  @SerializedName("last_price")
  private String lastPrice;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  public ProductBarcode() {
  }

  @Ignore
  public ProductBarcode(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    barcode = parcel.readString();
    quId = parcel.readString();
    amount = parcel.readString();
    storeId = parcel.readString();
    lastPrice = parcel.readString();
    note = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeString(barcode);
    dest.writeString(quId);
    dest.writeString(amount);
    dest.writeString(storeId);
    dest.writeString(lastPrice);
    dest.writeString(note);
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<ProductBarcode> CREATOR = new Creator<ProductBarcode>() {

    @Override
    public ProductBarcode createFromParcel(Parcel in) {
      return new ProductBarcode(in);
    }

    @Override
    public ProductBarcode[] newArray(int size) {
      return new ProductBarcode[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getQuId() {
    return quId;
  }

  public int getQuIdInt() {
    return hasQuId() ? Integer.parseInt(quId) : -1;
  }

  public void setQuId(String quId) {
    this.quId = quId;
  }

  public boolean hasQuId() {
    return NumUtil.isStringInt(quId);
  }

  public String getAmount() {
    return amount;
  }

  public double getAmountDouble() {
    return hasAmount() ? Double.parseDouble(amount) : 0;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public boolean hasAmount() {
    return NumUtil.isStringDouble(amount);
  }

  public String getStoreId() {
    return storeId;
  }

  public int getStoreIdInt() {
    return hasStoreId() ? Integer.parseInt(storeId) : -1;
  }

  public void setStoreId(String storeId) {
    this.storeId = storeId;
  }

  public boolean hasStoreId() {
    return NumUtil.isStringInt(storeId);
  }

  public String getLastPrice() {
    return lastPrice;
  }

  public double getLastPriceDouble() {
    return hasLastPrice() ? Double.parseDouble(lastPrice) : 0;
  }

  public void setLastPrice(String lastPrice) {
    this.lastPrice = lastPrice;
  }

  public boolean hasLastPrice() {
    return NumUtil.isStringDouble(lastPrice);
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
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
    ProductBarcode that = (ProductBarcode) o;
    return id == that.id &&
        productId == that.productId &&
        Objects.equals(barcode, that.barcode) &&
        Objects.equals(quId, that.quId) &&
        Objects.equals(amount, that.amount) &&
        Objects.equals(storeId, that.storeId) &&
        Objects.equals(lastPrice, that.lastPrice) &&
        Objects.equals(note, that.note) &&
        Objects.equals(rowCreatedTimestamp, that.rowCreatedTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, barcode, quId, amount, storeId,
        lastPrice, note, rowCreatedTimestamp);
  }

  public static JSONObject getJsonFromProductBarcode(ProductBarcode productBarcode, boolean debug,
      String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object quId = productBarcode.getQuId() != null && !productBarcode.getQuId().isEmpty()
          ? productBarcode.getQuId() : JSONObject.NULL;
      Object note = productBarcode.getNote() != null && !productBarcode.getNote().isEmpty()
          ? productBarcode.getNote() : JSONObject.NULL;
      Object lastPrice = productBarcode.getLastPrice() != null
          && !productBarcode.getLastPrice().isEmpty()
          ? productBarcode.getLastPrice() : JSONObject.NULL;
      Object storeId = productBarcode.getStoreId() != null
          ? productBarcode.getStoreId() : JSONObject.NULL;
      Object amount = productBarcode.getAmount() != null
          && !productBarcode.getAmount().isEmpty()
          ? productBarcode.getAmount() : JSONObject.NULL;
      json.put("product_id", productBarcode.getProductId());
      json.put("barcode", productBarcode.getBarcode());
      json.put("qu_id", quId);
      json.put("amount", amount);
      json.put("shopping_location_id", storeId);
      json.put("last_price", lastPrice);
      json.put("note", note);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromProductBarcode: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromProductBarcode(boolean debug, String TAG) {
    return getJsonFromProductBarcode(this, debug, TAG);
  }

  @NonNull
  @Override
  public String toString() {
    return "ProductBarcode(" + id + ')';
  }
}
