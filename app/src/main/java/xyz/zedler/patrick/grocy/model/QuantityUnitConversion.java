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
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

@Entity(tableName = "quantity_unit_conversion_table")
public class QuantityUnitConversion implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "from_qu_id")
  @SerializedName("from_qu_id")
  private int fromQuId;

  @ColumnInfo(name = "to_qu_id")
  @SerializedName("to_qu_id")
  private int toQuId;

  @ColumnInfo(name = "factor")
  @SerializedName("factor")
  private double factor;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  public QuantityUnitConversion() {
  }

  @Ignore
  public QuantityUnitConversion(Parcel parcel) {
    id = parcel.readInt();
    fromQuId = parcel.readInt();
    toQuId = parcel.readInt();
    factor = parcel.readDouble();
    productId = parcel.readInt();
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(fromQuId);
    dest.writeInt(toQuId);
    dest.writeDouble(factor);
    dest.writeInt(productId);
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<QuantityUnitConversion> CREATOR = new Creator<QuantityUnitConversion>() {

    @Override
    public QuantityUnitConversion createFromParcel(Parcel in) {
      return new QuantityUnitConversion(in);
    }

    @Override
    public QuantityUnitConversion[] newArray(int size) {
      return new QuantityUnitConversion[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getFromQuId() {
    return fromQuId;
  }

  public void setFromQuId(int fromQuId) {
    this.fromQuId = fromQuId;
  }

  public int getToQuId() {
    return toQuId;
  }

  public void setToQuId(int toQuId) {
    this.toQuId = toQuId;
  }

  public double getFactor() {
    return factor;
  }

  public void setFactor(double factor) {
    this.factor = factor;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
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
    QuantityUnitConversion that = (QuantityUnitConversion) o;
    return id == that.id &&
        fromQuId == that.fromQuId &&
        toQuId == that.toQuId &&
        Double.compare(that.factor, factor) == 0 &&
        productId == that.productId &&
        Objects.equals(rowCreatedTimestamp, that.rowCreatedTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, fromQuId, toQuId, factor, productId, rowCreatedTimestamp);
  }

  @NonNull
  @Override
  public String toString() {
    return "QuantityUnitConversion(" + id + ')';
  }
}
