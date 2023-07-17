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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Store;

public class MasterProductRepository {

  private final AppDatabase appDatabase;

  public MasterProductRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(MasterProductData data);
  }

  public static class MasterProductData {

    private final List<Product> products;
    private final List<ProductGroup> productGroups;
    private final List<ProductBarcode> barcodes;
    private final List<PendingProductBarcode> pendingProductBarcodes;
    private final List<Store> stores;
    private final List<Location> locations;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversion> conversions;
    private final List<QuantityUnitConversionResolved> conversionsResolved;

    public MasterProductData(
        List<Product> products,
        List<ProductGroup> productGroups,
        List<ProductBarcode> barcodes,
        List<PendingProductBarcode> pendingProductBarcodes,
        List<Store> stores,
        List<Location> locations,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversion> conversions,
        List<QuantityUnitConversionResolved> conversionsResolved
    ) {
      this.products = products;
      this.productGroups = productGroups;
      this.barcodes = barcodes;
      this.pendingProductBarcodes = pendingProductBarcodes;
      this.stores = stores;
      this.locations = locations;
      this.quantityUnits = quantityUnits;
      this.conversions = conversions;
      this.conversionsResolved = conversionsResolved;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductGroup> getProductGroups() {
      return productGroups;
    }

    public List<ProductBarcode> getBarcodes() {
      return barcodes;
    }

    public List<PendingProductBarcode> getPendingProductBarcodes() {
      return pendingProductBarcodes;
    }

    public List<Store> getStores() {
      return stores;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversion> getConversions() {
      return conversions;
    }

    public List<QuantityUnitConversionResolved> getConversionsResolved() {
      return conversionsResolved;
    }
  }

  public void loadFromDatabase(DataListener onSuccess, Consumer<Throwable> onError) {
    Single
        .zip(
            appDatabase.productDao().getProducts(),
            appDatabase.productGroupDao().getProductGroups(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.pendingProductBarcodeDao().getProductBarcodes(),
            appDatabase.storeDao().getStores(),
            appDatabase.locationDao().getLocations(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionDao().getConversions(),
            appDatabase.quantityUnitConversionResolvedDao().getConversionsResolved(),
            MasterProductData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
