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
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;

public class MasterObjectListRepository {

  private final AppDatabase appDatabase;
  private final String entity;

  public MasterObjectListRepository(Application application, String entity) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
    this.entity = entity;
  }

  public interface DataListener {

    void actionFinished(ArrayList<Object> objects);
  }

  public interface DataListenerProducts {

    void actionFinished(
        ArrayList<Object> objects,
        ArrayList<ProductGroup> productGroups,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<Location> locations
    );
  }

  public interface DataUpdatedListener {

    void actionFinished();
  }

  public interface ObjectsInsertedListener {

    void actionFinished();
  }

  public void loadFromDatabase(DataListener listener) {
    new loadAsyncTask(appDatabase, entity, listener).execute();
  }

  public void loadFromDatabaseProducts(DataListenerProducts listener) {
    new loadAsyncTask(appDatabase, entity, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final String entity;
    private final DataListener listener;
    private final DataListenerProducts listenerProducts;

    private ArrayList<Object> objects;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Location> locations;

    loadAsyncTask(AppDatabase appDatabase, String entity, DataListener listener) {
      this.appDatabase = appDatabase;
      this.entity = entity;
      this.listener = listener;
      this.listenerProducts = null;
    }

    loadAsyncTask(AppDatabase appDatabase, String entity, DataListenerProducts listener) {
      this.appDatabase = appDatabase;
      this.entity = entity;
      this.listener = null;
      this.listenerProducts = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      switch (entity) {
        case GrocyApi.ENTITY.PRODUCTS:
          objects = new ArrayList<>(appDatabase.productDao().getAll());
          break;
        case GrocyApi.ENTITY.QUANTITY_UNITS:
          objects = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
          break;
        case GrocyApi.ENTITY.LOCATIONS:
          objects = new ArrayList<>(appDatabase.locationDao().getAll());
          break;
        case GrocyApi.ENTITY.PRODUCT_GROUPS:
          objects = new ArrayList<>(appDatabase.productGroupDao().getAll());
          break;
        case ENTITY.TASK_CATEGORIES:
          objects = new ArrayList<>(appDatabase.taskCategoryDao().getAll());
          break;
        default: // STORES
          objects = new ArrayList<>(appDatabase.storeDao().getAll());
      }
      if (listenerProducts != null) {
        productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
        quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
        locations = new ArrayList<>(appDatabase.locationDao().getAll());
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(objects);
      }
      if (listenerProducts != null) {
        listenerProducts.actionFinished(
            objects, productGroups, quantityUnits, locations
        );
      }
    }
  }

  public void updateDatabase(
      ArrayList<Object> objects,
      DataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        entity,
        objects,
        listener
    ).execute();
  }

  public void updateDatabaseProducts(
      ArrayList<Object> objects,
      ArrayList<ProductGroup> productGroups,
      ArrayList<QuantityUnit> quantityUnits,
      ArrayList<Location> locations,
      DataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        entity,
        objects,
        productGroups,
        quantityUnits,
        locations,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final String entity;
    private final DataUpdatedListener listener;

    private final List<Object> objects;
    private final List<ProductGroup> productGroups;
    private final List<QuantityUnit> quantityUnits;
    private final List<Location> locations;

    updateAsyncTask(
        AppDatabase appDatabase,
        String entity,
        ArrayList<Object> objects,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.entity = entity;
      this.listener = listener;
      this.objects = objects;
      this.productGroups = null;
      this.quantityUnits = null;
      this.locations = null;
    }

    updateAsyncTask(
        AppDatabase appDatabase,
        String entity,
        ArrayList<Object> objects,
        ArrayList<ProductGroup> productGroups,
        ArrayList<QuantityUnit> quantityUnits,
        ArrayList<Location> locations,
        DataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.entity = entity;
      this.listener = listener;
      this.objects = objects;
      this.productGroups = productGroups;
      this.quantityUnits = quantityUnits;
      this.locations = locations;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final Void doInBackground(Void... params) {
      switch (entity) {
        case GrocyApi.ENTITY.PRODUCTS:
          appDatabase.productDao().deleteAll();
          appDatabase.productDao().insertAll((List<Product>) (Object) objects);
          appDatabase.quantityUnitDao().deleteAll();
          appDatabase.quantityUnitDao().insertAll(quantityUnits);
          appDatabase.productGroupDao().deleteAll();
          appDatabase.productGroupDao().insertAll(productGroups);
          appDatabase.locationDao().deleteAll();
          appDatabase.locationDao().insertAll(locations);
          break;
        case GrocyApi.ENTITY.QUANTITY_UNITS:
          appDatabase.quantityUnitDao().deleteAll();
          appDatabase.quantityUnitDao().insertAll((List<QuantityUnit>) (Object) objects);
          break;
        case GrocyApi.ENTITY.LOCATIONS:
          appDatabase.locationDao().deleteAll();
          appDatabase.locationDao().insertAll((List<Location>) (Object) objects);
          break;
        case GrocyApi.ENTITY.PRODUCT_GROUPS:
          appDatabase.productGroupDao().deleteAll();
          appDatabase.productGroupDao().insertAll((List<ProductGroup>) (Object) objects);
          break;
        case ENTITY.TASK_CATEGORIES:
          appDatabase.taskCategoryDao().deleteAll();
          appDatabase.taskCategoryDao().insertAll((List<TaskCategory>) (Object) objects);
          break;
        default: // STORES
          appDatabase.storeDao().deleteAll();
          appDatabase.storeDao().insertAll((List<Store>) (Object) objects);
      }
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
