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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.os.AsyncTask;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

public class PurchaseRepository {

  private final AppDatabase appDatabase;

  public PurchaseRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public void insertPendingProduct(PendingProduct pendingProduct) {
    runInBackground(appDatabase.pendingProductDao().insertRx(pendingProduct));
  }

  public void insertPendingProduct(
      PendingProduct pendingProduct,
      SuccessIdListener onSuccess,
      Runnable onError
  ) {
    appDatabase.pendingProductDao().insertRx(pendingProduct)
        .subscribeOn(Schedulers.io()).subscribe(onSuccess::onSuccess, e -> onError.run());
  }

  public void insertPendingProductBarcode(PendingProductBarcode barcode) {
    runInBackground(appDatabase.pendingProductBarcodeDao().insertRx(barcode));
  }

  private void runInBackground(Single<Long> single) {
    single.subscribeOn(Schedulers.io()).subscribe();
  }

  public interface SuccessIdListener {
    void onSuccess(Long id);
  }

  public interface DataListener {

    void actionFinished(
        ArrayList<Product> products,
        ArrayList<ProductBarcode> barcodes,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<QuantityUnitConversion> quantityUnitConversions,
        ArrayList<Store> stores,
        ArrayList<Location> locations,
        ArrayList<ShoppingListItem> shoppingListItems
    );
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListener listener, boolean loadShoppingListItems) {
    new loadAsyncTask(appDatabase, listener, loadShoppingListItems).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataListener listener;

    private ArrayList<Product> products;
    private ArrayList<ProductBarcode> barcodes;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnitConversion> quantityUnitConversions;
    private ArrayList<Store> stores;
    private ArrayList<Location> locations;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private final boolean loadShoppingListItems;


    loadAsyncTask(AppDatabase appDatabase, DataListener listener, boolean loadShoppingListItems) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.loadShoppingListItems = loadShoppingListItems;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      products = new ArrayList<>(appDatabase.productDao().getAll());
      barcodes = new ArrayList<>(appDatabase.productBarcodeDao().getAll());
      quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
      quantityUnitConversions
          = new ArrayList<>(appDatabase.quantityUnitConversionDao().getAll());
      stores = new ArrayList<>(appDatabase.storeDao().getAll());
      locations = new ArrayList<>(appDatabase.locationDao().getAll());
      if (loadShoppingListItems) {
        shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll());
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(products, barcodes,
            quantityUnits, quantityUnitConversions, stores, locations, shoppingListItems);
      }
    }
  }

  public void updateDatabase(
      ArrayList<Product> products,
      ArrayList<ProductBarcode> barcodes,
      ArrayList<QuantityUnit> quantityUnits,
      ArrayList<QuantityUnitConversion> quantityUnitConversions,
      ArrayList<Store> stores,
      ArrayList<Location> locations,
      ArrayList<ShoppingListItem> shoppingListItems
  ) {
    new updateAsyncTask(
        appDatabase,
        products,
        barcodes,
        quantityUnits,
        quantityUnitConversions,
        stores,
        locations,
        shoppingListItems
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;

    private final ArrayList<Product> products;
    private final ArrayList<ProductBarcode> barcodes;
    private final ArrayList<QuantityUnit> quantityUnits;
    private final ArrayList<QuantityUnitConversion> quantityUnitConversions;
    private final ArrayList<Store> stores;
    private final ArrayList<Location> locations;
    private final ArrayList<ShoppingListItem> shoppingListItems;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<Product> products,
        ArrayList<ProductBarcode> barcodes,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<QuantityUnitConversion> quantityUnitConversions,
        ArrayList<Store> stores,
        ArrayList<Location> locations,
        ArrayList<ShoppingListItem> shoppingListItems
    ) {
      this.appDatabase = appDatabase;
      this.products = products;
      this.barcodes = barcodes;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
      this.stores = stores;
      this.locations = locations;
      this.shoppingListItems = shoppingListItems;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      appDatabase.productBarcodeDao().deleteAll();
      appDatabase.productBarcodeDao().insertAll(barcodes);
      appDatabase.quantityUnitDao().deleteAll();
      appDatabase.quantityUnitDao().insertAll(quantityUnits);
      appDatabase.quantityUnitConversionDao().deleteAll();
      appDatabase.quantityUnitConversionDao().insertAll(quantityUnitConversions);
      appDatabase.storeDao().deleteAll();
      appDatabase.storeDao().insertAll(stores);
      appDatabase.locationDao().deleteAll();
      appDatabase.locationDao().insertAll(locations);
      if (shoppingListItems != null) {
        appDatabase.shoppingListItemDao().deleteAll();
        appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
      }
      return null;
    }
  }
}
