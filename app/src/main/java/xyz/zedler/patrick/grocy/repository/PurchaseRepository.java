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

  public interface DataListener {
    void actionFinished(PurchaseData data);
  }

  public static class PurchaseData {

    private final List<Product> products;
    private final List<ProductBarcode> barcodes;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversion> quantityUnitConversions;
    private final List<Store> stores;
    private final List<Location> locations;
    private final List<ShoppingListItem> shoppingListItems;

    public PurchaseData(
        List<Product> products,
        List<ProductBarcode> barcodes,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversion> quantityUnitConversions,
        List<Store> stores,
        List<Location> locations,
        List<ShoppingListItem> shoppingListItems
    ) {
      this.products = products;
      this.barcodes = barcodes;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
      this.stores = stores;
      this.locations = locations;
      this.shoppingListItems = shoppingListItems;
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

    public List<Store> getStores() {
      return stores;
    }

    public List<Location> getLocations() {
      return locations;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    Single
        .zip(
            appDatabase.productDao().getProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionDao().getConversions(),
            appDatabase.storeDao().getStores(),
            appDatabase.locationDao().getLocations(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            PurchaseData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void insertPendingProduct(PendingProduct pendingProduct) {
    appDatabase.pendingProductDao().insertPendingProduct(pendingProduct)
            .subscribeOn(Schedulers.io()).subscribe();
  }

  public void insertPendingProduct(
          PendingProduct pendingProduct,
          SuccessIdListener onSuccess,
          Runnable onError
  ) {
    appDatabase.pendingProductDao().insertPendingProduct(pendingProduct)
            .subscribeOn(Schedulers.io()).subscribe(onSuccess::onSuccess, e -> onError.run());
  }

  public void insertPendingProductBarcode(PendingProductBarcode barcode) {
    appDatabase.pendingProductBarcodeDao().insertProductBarcode(barcode)
            .subscribeOn(Schedulers.io()).subscribe();
  }

  public interface SuccessIdListener {
    void onSuccess(Long id);
  }
}
