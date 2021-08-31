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

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "pending_product_table")
public class PendingProduct {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private int id;

  @ColumnInfo(name = "product_name")
  @NonNull
  private String productName;

  @ColumnInfo(name = "amount")
  private double amount;

  @ColumnInfo(name = "name_is_from_off")
  private boolean nameIsFromOFF;

  public PendingProduct(@NonNull String productName, double amount, boolean nameIsFromOFF) {
    this.productName = productName;
    this.amount = amount;
    this.nameIsFromOFF = nameIsFromOFF;
  }

  @Ignore
  public PendingProduct(@NonNull String productName, boolean nameIsFromOFF) {
    this.productName = productName;
    this.amount = 1;
    this.nameIsFromOFF = nameIsFromOFF;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @NonNull
  public String getProductName() {
    return productName;
  }

  public void setProductName(@NonNull String productName) {
    this.productName = productName;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public void addAmount(double amount) {
    this.amount += amount;
  }

  public boolean isNameIsFromOFF() {
    return nameIsFromOFF;
  }

  public void setNameIsFromOFF(boolean nameIsFromOFF) {
    this.nameIsFromOFF = nameIsFromOFF;
  }

  public static PendingProduct getFromBarcode(
      LiveData<List<PendingProduct>> pendingProducts,
      LiveData<List<PendingProductBarcode>> barcodes,
      String barcode
  ) {
    if (pendingProducts.getValue() == null || barcodes.getValue() == null) return null;
    if (barcode == null) return null;
    Long pendingProductId = PendingProductBarcode.getPendingProductId(barcodes, barcode);
    if (pendingProductId == null) return null;
    for (PendingProduct product : pendingProducts.getValue()) {
      if (product.getId() == pendingProductId) return product;
    }
    return null;
  }

  public static PendingProduct getFromName(
      LiveData<List<PendingProduct>> pendingProducts,
      String productName
  ) {
    if (pendingProducts.getValue() == null || productName == null) return null;
    for (PendingProduct product : pendingProducts.getValue()) {
      if (product.getProductName().equals(productName)) return product;
    }
    return null;
  }

  @NonNull
  @Override
  public String toString() {
    return "PendingProduct(" + productName + ": " + amount + ')';
  }
}
