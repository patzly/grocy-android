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

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "pending_product_barcode_table")
public class PendingProductBarcode {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "pending_product_id")
    private long pendingProductId;

    @ColumnInfo(name = "barcode")
    private String barcode;

    @ColumnInfo(name = "qu_id")
    private String quId;

    @ColumnInfo(name = "amount")
    private String amount;

    @ColumnInfo(name = "shopping_location_id")
    private String storeId;

    @ColumnInfo(name = "last_price")
    private String lastPrice;

    public PendingProductBarcode() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPendingProductId() {
        return pendingProductId;
    }

    public void setPendingProductId(long pendingProductId) {
        this.pendingProductId = pendingProductId;
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

    public void setQuId(String quId) {
        this.quId = quId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(String lastPrice) {
        this.lastPrice = lastPrice;
    }

    public static Long getPendingProductId(
            LiveData<List<PendingProductBarcode>> barcodes,
            String barcode
    ) {
        List<PendingProductBarcode> barcodesList = barcodes.getValue();
        if (barcodesList == null || barcodesList.isEmpty() || barcode == null) return null;
        for (PendingProductBarcode barcodeTemp : barcodesList) {
            if (barcodeTemp.getBarcode().equals(barcode))
                return barcodeTemp.getPendingProductId();
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return "PendingProductBarcode(" + id + ", " + pendingProductId + ": " + barcode + ')';
    }
}
