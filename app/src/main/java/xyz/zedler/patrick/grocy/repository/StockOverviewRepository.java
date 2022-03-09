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
import android.os.AsyncTask;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;

public class StockOverviewRepository {

  private final AppDatabase appDatabase;

  public StockOverviewRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface StockOverviewDataListener {
    void actionFinished(StockOverviewData data);
  }

  public static class StockOverviewData {

    private final List<QuantityUnit> quantityUnits;
    private final List<ProductGroup> productGroups;
    private final List<StockItem> stockItems;
    private final List<Product> products;
    private final List<ProductBarcode> productBarcodes;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<Location> locations;
    private final List<StockLocation> stockCurrentLocations;

    public StockOverviewData(
        List<QuantityUnit> quantityUnits,
        List<ProductGroup> productGroups,
        List<StockItem> stockItems,
        List<Product> products,
        List<ProductBarcode> productBarcodes,
        List<ShoppingListItem> shoppingListItems,
        List<Location> locations,
        List<StockLocation> stockCurrentLocations
    ) {
      this.quantityUnits = quantityUnits;
      this.productGroups = productGroups;
      this.stockItems = stockItems;
      this.products = products;
      this.productBarcodes = productBarcodes;
      this.shoppingListItems = shoppingListItems;
      this.locations = locations;
      this.stockCurrentLocations = stockCurrentLocations;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<ProductGroup> getProductGroups() {
      return productGroups;
    }

    public List<StockItem> getStockItems() {
      return stockItems;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductBarcode> getProductBarcodes() {
      return productBarcodes;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<StockLocation> getStockCurrentLocations() {
      return stockCurrentLocations;
    }
  }

  public void loadFromDatabase(StockOverviewDataListener listener) {
    Single
        .zip(
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.productGroupDao().getProductGroups(),
            appDatabase.stockItemDao().getStockItems(),
            appDatabase.productDao().getProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.locationDao().getLocations(),
            appDatabase.stockLocationDao().getStockLocations(),
            StockOverviewData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void updateDatabase(
      List<QuantityUnit> quantityUnits,
      List<ProductGroup> productGroups,
      List<StockItem> stockItems,
      List<Product> products,
      List<ProductBarcode> productBarcodes,
      List<ShoppingListItem> shoppingListItems,
      List<Location> locations,
      List<StockLocation> stockCurrentLocations,
      Runnable listener
  ) {
    new updateAsyncTask(
        appDatabase,
        quantityUnits,
        productGroups,
        stockItems,
        products,
        productBarcodes,
        shoppingListItems,
        locations,
        stockCurrentLocations,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final Runnable listener;

    private final List<QuantityUnit> quantityUnits;
    private final List<ProductGroup> productGroups;
    private final List<StockItem> stockItems;
    private final List<Product> products;
    private final List<ProductBarcode> productBarcodes;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<Location> locations;
    private final List<StockLocation> stockCurrentLocations;

    updateAsyncTask(
        AppDatabase appDatabase,
        List<QuantityUnit> quantityUnits,
        List<ProductGroup> productGroups,
        List<StockItem> stockItems,
        List<Product> products,
        List<ProductBarcode> productBarcodes,
        List<ShoppingListItem> shoppingListItems,
        List<Location> locations,
        List<StockLocation> stockCurrentLocations,
        Runnable listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.quantityUnits = quantityUnits;
      this.productGroups = productGroups;
      this.stockItems = stockItems;
      this.products = products;
      this.productBarcodes = productBarcodes;
      this.shoppingListItems = shoppingListItems;
      this.locations = locations;
      this.stockCurrentLocations = stockCurrentLocations;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.quantityUnitDao().deleteAll();
      appDatabase.quantityUnitDao().insertAll(quantityUnits);
      appDatabase.productGroupDao().deleteAll();
      appDatabase.productGroupDao().insertAll(productGroups);
      appDatabase.stockItemDao().deleteAll();
      appDatabase.stockItemDao().insertAll(stockItems);
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      appDatabase.productBarcodeDao().deleteAll();
      appDatabase.productBarcodeDao().insertAll(productBarcodes);
      appDatabase.shoppingListItemDao().deleteAll();
      appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
      appDatabase.locationDao().deleteAll();
      appDatabase.locationDao().insertAll(locations);
      appDatabase.stockLocationDao().deleteAll();
      appDatabase.stockLocationDao().insertAll(stockCurrentLocations);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.run();
      }
    }
  }
}
