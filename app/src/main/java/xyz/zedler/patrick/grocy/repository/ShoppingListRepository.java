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
import androidx.lifecycle.LiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

public class ShoppingListRepository {

  private final AppDatabase appDatabase;

  public ShoppingListRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {

    void actionFinished(ShoppingListData data);
  }

  public static class ShoppingListData {

    private final List<ShoppingListItem> shoppingListItems;
    private final List<ShoppingList> shoppingLists;
    private final List<ProductGroup> productGroups;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversion> unitConversions;
    private final List<Product> products;
    private final List<ProductLastPurchased> productsLastPurchased;
    private final List<Store> stores;
    private final List<MissingItem> missingItems;

    public ShoppingListData(
        List<ShoppingListItem> shoppingListItems,
        List<ShoppingList> shoppingLists,
        List<ProductGroup> productGroups,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversion> unitConversions,
        List<Product> products,
        List<ProductLastPurchased> productsLastPurchased,
        List<Store> stores,
        List<MissingItem> missingItems
    ) {
      this.shoppingListItems = shoppingListItems;
      this.shoppingLists = shoppingLists;
      this.productGroups = productGroups;
      this.quantityUnits = quantityUnits;
      this.unitConversions = unitConversions;
      this.products = products;
      this.productsLastPurchased = productsLastPurchased;
      this.stores = stores;
      this.missingItems = missingItems;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<ShoppingList> getShoppingLists() {
      return shoppingLists;
    }

    public List<ProductGroup> getProductGroups() {
      return productGroups;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversion> getUnitConversions() {
      return unitConversions;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductLastPurchased> getProductsLastPurchased() {
      return productsLastPurchased;
    }

    public List<Store> getStores() {
      return stores;
    }

    public List<MissingItem> getMissingItems() {
      return missingItems;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    Single
        .zip(
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.shoppingListDao().getShoppingLists(),
            appDatabase.productGroupDao().getProductGroups(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionDao().getConversions(),
            appDatabase.productDao().getProducts(),
            appDatabase.productLastPurchasedDao().getProductsLastPurchased(),
            appDatabase.storeDao().getStores(),
            appDatabase.missingItemDao().getMissingItems(),
            ShoppingListData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void insertShoppingListItems(
      Runnable onFinished,
      ShoppingListItem... shoppingListItems
  ) {
    appDatabase.shoppingListItemDao().insertShoppingListItems(shoppingListItems)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(onFinished::run)
        .subscribe();
  }

  public interface ShoppingListsListener {
    void actionFinished(List<ShoppingList> shoppingLists);
  }

  public void loadShoppingListsFromDatabase(ShoppingListsListener listener) {
    appDatabase.shoppingListDao().getShoppingLists()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public LiveData<List<ShoppingList>> getShoppingListsLive() {
    return appDatabase.shoppingListDao().getAllLive();
  }

  public void updateShoppingLists(List<ShoppingList> shoppingLists) {
    Single.concat(
        appDatabase.shoppingListDao().deleteShoppingLists(),
        appDatabase.shoppingListDao().insertShoppingLists(shoppingLists)
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }
}
