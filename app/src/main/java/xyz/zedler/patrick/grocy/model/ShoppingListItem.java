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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "shopping_list_item_table")
public class ShoppingListItem extends GroupedListItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "shopping_list_id")
  @SerializedName("shopping_list_id")
  private String shoppingListId;

  @ColumnInfo(name = "qu_id")
  @SerializedName("qu_id")
  private String quId;

  @ColumnInfo(name = "done")
  @SerializedName("done")
  private String done;

  @ColumnInfo(name = "done_synced")
  private int doneSynced = -1;  // state of param "done" on server during time of last sync
  // -1 means that the "done" was not edited since sync

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private String productId;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  public ShoppingListItem() {  // for Room
  }

  private ShoppingListItem( // for clone
      int id,
      String productId,
      String note,
      String amount,
      String shoppingListId,
      String quId,
      String done,
      int doneSynced
  ) {
    this.id = id;
    this.productId = productId;
    this.note = note;
    this.amount = amount;
    this.shoppingListId = shoppingListId;
    this.quId = quId;
    this.done = done;
    this.doneSynced = doneSynced;
  }

  private ShoppingListItem(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readString();
    note = parcel.readString();
    amount = parcel.readString();
    shoppingListId = parcel.readString();
    quId = parcel.readString();
    done = parcel.readString();
    doneSynced = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(productId);
    dest.writeString(note);
    dest.writeString(amount);
    dest.writeString(shoppingListId);
    dest.writeString(quId);
    dest.writeString(done);
    dest.writeInt(doneSynced);
  }

  public static final Creator<ShoppingListItem> CREATOR = new Creator<ShoppingListItem>() {

    @Override
    public ShoppingListItem createFromParcel(Parcel in) {
      return new ShoppingListItem(in);
    }

    @Override
    public ShoppingListItem[] newArray(int size) {
      return new ShoppingListItem[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getProductId() {
    if (productId != null && productId.isEmpty()) {
      return null;
    }
    return productId;
  }

  public int getProductIdInt() {
    if (!hasProduct()) {
      return -1;
    }
    return Integer.parseInt(productId);
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {  // getter & setter seem useless,
    this.note = note;               // but are required by Room
  }

  public String getAmount() {
    return amount;
  }

  public double getAmountDouble() {
    return NumUtil.isStringDouble(amount) ? Double.parseDouble(amount) : 0;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public void setAmountDouble(double amount) {
    this.amount = NumUtil.trim(amount);
  }

  public String getShoppingListId() {
    return shoppingListId;
  }

  public int getShoppingListIdInt() {
    return NumUtil.isStringInt(shoppingListId) ? Integer.parseInt(shoppingListId) : 1;
  }

  public void setShoppingListId(String shoppingListId) {
    this.shoppingListId = shoppingListId;
  }

  public void setShoppingListId(int shoppingListId) {
    this.shoppingListId = String.valueOf(shoppingListId);
  }

  public String getDone() {
    return done;
  }

  public int getDoneInt() {
    return NumUtil.isStringInt(done) ? Integer.parseInt(done) : 0;
  }

  public boolean isUndone() {
    return getDoneInt() != 1;
  }

  public void setDone(String done) {
    this.done = done;
  }

  public void setDone(int done) {
    this.done = String.valueOf(done);
  }

  public int getDoneSynced() {
    return doneSynced;
  }

  public void setDoneSynced(int doneSynced) {
    this.doneSynced = doneSynced;
  }

  public boolean hasProduct() {
    return NumUtil.isStringInt(productId);
  }

  public boolean hasQuId() {
    return NumUtil.isStringInt(quId);
  }

  public String getQuId() {
    return quId;
  }

  public int getQuIdInt() {
    return NumUtil.isStringInt(quId) ? Integer.parseInt(quId) : -1;
  }

  public void setQuId(String quId) {
    this.quId = quId;
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
  public int getType() {
    return TYPE_ENTRY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShoppingListItem that = (ShoppingListItem) o;
    return id == that.id &&
        doneSynced == that.doneSynced &&
        Objects.equals(note, that.note) &&
        Objects.equals(amount, that.amount) &&
        Objects.equals(shoppingListId, that.shoppingListId) &&
        Objects.equals(quId, that.quId) &&
        Objects.equals(done, that.done) &&
        Objects.equals(productId, that.productId) &&
        Objects.equals(rowCreatedTimestamp, that.rowCreatedTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, note, amount, shoppingListId, quId, done, doneSynced, productId,
        rowCreatedTimestamp);
  }

  @NonNull
  @Override
  public String toString() {
    return "ShoppingListItem(" + id + ")";
  }

  @NonNull
  public ShoppingListItem getClone() {
    return new ShoppingListItem(
        this.id,
        this.productId,
        this.note,
        this.amount,
        this.shoppingListId,
        this.quId,
        this.done,
        this.doneSynced
    );
  }

  public static JSONObject getJsonFromShoppingListItem(ShoppingListItem item, boolean addId,
      boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      if (addId) {
        json.put("id", item.getId());
        json.put("done", item.getDoneInt());
      }
      Object productId = item.getProductId() != null ? item.getProductId() : JSONObject.NULL;
      Object quId = item.getQuId() != null ? item.getQuId() : JSONObject.NULL;
      Object note = item.getNote() == null || item.getNote().isEmpty()
          ? JSONObject.NULL : item.getNote();
      json.put("shopping_list_id", item.getShoppingListIdInt());
      json.put("amount", item.getAmountDouble());
      json.put("qu_id", quId);
      json.put("product_id", productId);
      json.put("note", note);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromShoppingListItem: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromShoppingListItem(boolean addId, boolean debug, String TAG) {
    return getJsonFromShoppingListItem(this, addId, debug, TAG);
  }
}
