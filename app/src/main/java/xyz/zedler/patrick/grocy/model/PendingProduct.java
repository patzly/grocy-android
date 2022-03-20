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

@Entity(tableName = "pending_product_table")
public class PendingProduct extends Product {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "name")
    @NonNull
    private String name;

    @ColumnInfo(name = "name_is_from_off")
    private boolean nameIsFromOFF;

    public PendingProduct(@NonNull String name, boolean nameIsFromOFF) {
        this.name = name;
        this.nameIsFromOFF = nameIsFromOFF;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
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
        Integer pendingProductId = PendingProductBarcode.getPendingProductId(barcodes, barcode);
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
            if (product.getName().equals(productName)) return product;
        }
        return null;
    }

    public static PendingProduct getFromId(List<PendingProduct> pendingProducts, int id) {
        if (pendingProducts == null) return null;
        for (PendingProduct product : pendingProducts) {
            if (product.getId() == id) return product;
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return "PendingProduct(" + name + ')';
    }
}
