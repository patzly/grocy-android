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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "volatile_item_table")
public class VolatileItem implements Parcelable {

  public final static int TYPE_DUE = 1;
  public final static int TYPE_OVERDUE = 2;
  public final static int TYPE_EXPIRED = 3;
  // missing items are in a separate table

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private int id;

  @ColumnInfo(name = "product_id")
  private int productId;

  @ColumnInfo(name = "volatile_type")
  private int volatileType;

  // for Room
  public VolatileItem() {
  }

  @Ignore
  public VolatileItem(int productId, int volatileType) {
    this.productId = productId;
    this.volatileType = volatileType;
  }

  private VolatileItem(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    volatileType = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeInt(volatileType);
  }

  public static final Creator<VolatileItem> CREATOR = new Creator<VolatileItem>() {

    @Override
    public VolatileItem createFromParcel(Parcel in) {
      return new VolatileItem(in);
    }

    @Override
    public VolatileItem[] newArray(int size) {
      return new VolatileItem[size];
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

  public int getVolatileType() {
    return volatileType;
  }

  public void setVolatileType(int volatileType) {
    this.volatileType = volatileType;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "VolatileItem(" + id + ", " + volatileType + ')';
  }
}
