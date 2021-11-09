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
import android.util.Log;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;

public class OverviewStartRepository {

  private final static String TAG = OverviewStartRepository.class.getSimpleName();

  private final AppDatabase appDatabase;

  public OverviewStartRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {

    void actionFinished(
        ArrayList<StockItem> stockItems,
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingList> shoppingLists,
        ArrayList<Product> products
    );
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListener listener) {
    new loadAsyncTask(appDatabase, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataListener listener;

    private ArrayList<StockItem> stockItems;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<Product> products;

    loadAsyncTask(AppDatabase appDatabase, DataListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      try {
        products = new ArrayList<>(appDatabase.productDao().getAll());
        stockItems = new ArrayList<>(appDatabase.stockItemDao().getAll());
        shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll());
        shoppingLists = new ArrayList<>(appDatabase.shoppingListDao().getAll());
      } catch (Exception e) {
        Log.e(TAG, "doInBackground: " + e);
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(stockItems, shoppingListItems, shoppingLists, products);
      }
    }
  }

  public void updateDatabase(
      ArrayList<StockItem> stockItems,
      ArrayList<ShoppingListItem> shoppingListItems,
      ArrayList<ShoppingList> shoppingLists,
      ArrayList<Product> products,
      DataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        stockItems,
        shoppingListItems,
        shoppingLists,
        products,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final ArrayList<StockItem> stockItems;
    private final ArrayList<ShoppingListItem> shoppingListItems;
    private final ArrayList<ShoppingList> shoppingLists;
    private final ArrayList<Product> products;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<StockItem> stockItems,
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingList> shoppingLists,
        ArrayList<Product> products,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.stockItems = stockItems;
      this.shoppingListItems = shoppingListItems;
      this.shoppingLists = shoppingLists;
      this.products = products;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.stockItemDao().deleteAll();
      appDatabase.stockItemDao().insertAll(stockItems);
      appDatabase.shoppingListItemDao().deleteAll();
      appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
      appDatabase.shoppingListDao().deleteAll();
      appDatabase.shoppingListDao().insertAll(shoppingLists);
      appDatabase.productDao().deleteAll();
      appDatabase.productDao().insertAll(products);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }
}
