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
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;

public class ChooseProductRepository {

  private final AppDatabase appDatabase;

  public ChooseProductRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListenerProducts {

    void actionFinished(ArrayList<Product> products);
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListenerProducts listener) {
    new loadAsyncTask(appDatabase, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataListenerProducts listener;
    private ArrayList<Product> products;

    loadAsyncTask(AppDatabase appDatabase, DataListenerProducts listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      products = new ArrayList<>(appDatabase.productDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(products);
      }
    }
  }

  public void updateDatabase(
      ArrayList<Product> products,
      DataUpdatedListener listener
  ) {
    new updateAsyncTask(appDatabase, products, listener).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final DataUpdatedListener listener;

    private final List<Product> products;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<Product> products,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.products = products;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
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
