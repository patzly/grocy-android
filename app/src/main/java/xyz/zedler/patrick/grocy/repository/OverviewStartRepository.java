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
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StoredPurchase;
import xyz.zedler.patrick.grocy.model.Task;

public class OverviewStartRepository {

  private final AppDatabase appDatabase;

  public OverviewStartRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(OverviewStartData data);
  }

  public static class OverviewStartData {

    private final List<StockItem> stockItems;
    private final List<ShoppingListItem> shoppingListItems;
    private final List<ShoppingList> shoppingLists;
    private final List<Product> products;
    private final List<StoredPurchase> storedPurchases;
    private final List<ChoreEntry> choreEntries;
    private final List<Task> tasks;

    public OverviewStartData(
        List<StockItem> stockItems,
        List<ShoppingListItem> shoppingListItems,
        List<ShoppingList> shoppingLists,
        List<Product> products,
        List<StoredPurchase> storedPurchases,
        List<ChoreEntry> choreEntries,
        List<Task> tasks
    ) {
      this.stockItems = stockItems;
      this.shoppingListItems = shoppingListItems;
      this.shoppingLists = shoppingLists;
      this.products = products;
      this.storedPurchases = storedPurchases;
      this.choreEntries = choreEntries;
      this.tasks = tasks;
    }

    public List<StockItem> getStockItems() {
      return stockItems;
    }

    public List<ShoppingListItem> getShoppingListItems() {
      return shoppingListItems;
    }

    public List<ShoppingList> getShoppingLists() {
      return shoppingLists;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<StoredPurchase> getStoredPurchases() {
      return storedPurchases;
    }

    public List<ChoreEntry> getChoreEntries() {
      return choreEntries;
    }

    public List<Task> getTasks() {
      return tasks;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    Single
        .zip(
            appDatabase.stockItemDao().getStockItems(),
            appDatabase.shoppingListItemDao().getShoppingListItems(),
            appDatabase.shoppingListDao().getShoppingLists(),
            appDatabase.productDao().getProducts(),
            appDatabase.storedPurchaseDao().getStoredPurchases(),
            appDatabase.choreEntryDao().getChoreEntries(),
            appDatabase.taskDao().getTasks(),
            OverviewStartData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void updateDatabase(
      List<StockItem> stockItems,
      Runnable listener
  ) {
    Single.concat(
        appDatabase.stockItemDao().deleteStockItems(),
        appDatabase.stockItemDao().insertStockItems(stockItems)
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally(listener::run)
        .subscribe();
  }
}
