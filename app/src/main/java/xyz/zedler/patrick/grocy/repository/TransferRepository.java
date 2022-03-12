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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;

public class TransferRepository {

  private final AppDatabase appDatabase;

  public TransferRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {

    void actionFinished(TransferData data);
  }

  public static class TransferData {

    private final List<Product> products;
    private final List<ProductBarcode> barcodes;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversion> quantityUnitConversions;
    private final List<Location> locations;

    public TransferData(
        List<Product> products,
        List<ProductBarcode> barcodes,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversion> quantityUnitConversions,
        List<Location> locations
    ) {
      this.products = products;
      this.barcodes = barcodes;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
      this.locations = locations;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductBarcode> getBarcodes() {
      return barcodes;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversion> getQuantityUnitConversions() {
      return quantityUnitConversions;
    }

    public List<Location> getLocations() {
      return locations;
    }
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListener listener) {
    Single
        .zip(
            appDatabase.productDao().getProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionDao().getConversions(),
            appDatabase.locationDao().getLocations(),
            TransferData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }
}
