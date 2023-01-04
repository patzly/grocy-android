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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
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

@Entity(tableName = "product_average_price_table")
public class ProductAveragePrice implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "price")
  @SerializedName("price")
  private String price;

  public ProductAveragePrice() {
  }

  @Ignore
  public ProductAveragePrice(Parcel parcel) {
    productId = parcel.readInt();
    price = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(productId);
    dest.writeString(price);
  }

  public static final Creator<ProductAveragePrice> CREATOR = new Creator<ProductAveragePrice>() {

    @Override
    public ProductAveragePrice createFromParcel(Parcel in) {
      return new ProductAveragePrice(in);
    }

    @Override
    public ProductAveragePrice[] newArray(int size) {
      return new ProductAveragePrice[size];
    }
  };

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
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
    ProductAveragePrice that = (ProductAveragePrice) o;
    return productId == that.productId && Objects.equals(price, that.price);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, price);
  }

  @NonNull
  @Override
  public String toString() {
    return "ProductAveragePrice(" + productId + ')';
  }
}
