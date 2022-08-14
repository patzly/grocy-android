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
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.User;

public class StockEntriesRepository {

  private final AppDatabase appDatabase;

  public StockEntriesRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface StockOverviewDataListener {
    void actionFinished(StockOverviewData data);
  }

  public static class StockOverviewData {

    private final List<QuantityUnit> quantityUnits;
    private final List<StockEntry> stockEntries;
    private final List<Product> products;
    private final List<ProductBarcode> productBarcodes;
    private final List<Location> locations;
    private final List<Store> stores;
    private final List<User> users;

    public StockOverviewData(
        List<QuantityUnit> quantityUnits,
        List<StockEntry> stockEntries,
        List<Product> products,
        List<ProductBarcode> productBarcodes,
        List<Location> locations,
        List<Store> stores,
        List<User> users
    ) {
      this.quantityUnits = quantityUnits;
      this.stockEntries = stockEntries;
      this.products = products;
      this.productBarcodes = productBarcodes;
      this.locations = locations;
      this.stores = stores;
      this.users = users;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<StockEntry> getStockEntries() {
      return stockEntries;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductBarcode> getProductBarcodes() {
      return productBarcodes;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<Store> getStores() {
      return stores;
    }

    public List<User> getUsers() {
      return users;
    }
  }

  public void loadFromDatabase(StockOverviewDataListener listener) {
    Single
        .zip(
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.stockEntryDao().getStockEntries(),
            appDatabase.productDao().getProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.locationDao().getLocations(),
            appDatabase.storeDao().getStores(),
            appDatabase.userDao().getUsers(),
            StockOverviewData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }
}
