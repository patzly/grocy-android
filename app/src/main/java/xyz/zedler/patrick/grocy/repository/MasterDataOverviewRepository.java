package xyz.zedler.patrick.grocy.repository;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.os.AsyncTask;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;

public class MasterDataOverviewRepository {
    private final AppDatabase appDatabase;

    public MasterDataOverviewRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
    }

    public interface DataListener {
        void actionFinished(
                ArrayList<Store> stores,
                ArrayList<Location> locations,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<Product> products
        );
    }

    public interface DataUpdatedListener {
        void actionFinished();
    }

    public interface ShoppingListItemsInsertedListener {
        void actionFinished();
    }

    public void loadFromDatabase(DataListener listener) {
        new loadAsyncTask(appDatabase, listener).execute();
    }

    private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final DataListener listener;

        private ArrayList<Store> stores;
        private ArrayList<Location> locations;
        private ArrayList<ProductGroup> productGroups;
        private ArrayList<QuantityUnit> quantityUnits;
        private ArrayList<Product> products;

        loadAsyncTask(AppDatabase appDatabase, DataListener listener) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            stores = new ArrayList<>(appDatabase.storeDao().getAll()); // TODO: List instead of ArrayList maybe
            locations = new ArrayList<>(appDatabase.locationDao().getAll());
            productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
            quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
            products = new ArrayList<>(appDatabase.productDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(stores, locations, productGroups, quantityUnits, products);
        }
    }

    public void updateDatabase(
            ArrayList<Store> stores,
            ArrayList<Location> locations,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<Product> products,
            DataUpdatedListener listener
    ) {
        new updateAsyncTask(
                appDatabase,
                stores,
                locations,
                productGroups,
                quantityUnits,
                products,
                listener
        ).execute();
    }

    private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final DataUpdatedListener listener;

        private final ArrayList<Store> stores;
        private final ArrayList<Location> locations;
        private final ArrayList<ProductGroup> productGroups;
        private final ArrayList<QuantityUnit> quantityUnits;
        private final ArrayList<Product> products;

        updateAsyncTask(
                AppDatabase appDatabase,
                ArrayList<Store> stores,
                ArrayList<Location> locations,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<Product> products,
                DataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.stores = stores;
            this.locations = locations;
            this.productGroups = productGroups;
            this.quantityUnits = quantityUnits;
            this.products = products;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            appDatabase.storeDao().deleteAll();
            appDatabase.storeDao().insertAll(stores);
            appDatabase.locationDao().deleteAll();
            appDatabase.locationDao().insertAll(locations);
            appDatabase.productGroupDao().deleteAll();
            appDatabase.productGroupDao().insertAll(productGroups);
            appDatabase.quantityUnitDao().deleteAll();
            appDatabase.quantityUnitDao().insertAll(quantityUnits);
            appDatabase.productDao().deleteAll();
            appDatabase.productDao().insertAll(products);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }

    public void insertShoppingListItems(
            ShoppingListItemsInsertedListener listener,
            ShoppingListItem... shoppingListItems
    ) {
        new insertShoppingListItemsAsyncTask(appDatabase, listener).execute(shoppingListItems);
    }

    private static class insertShoppingListItemsAsyncTask extends AsyncTask<ShoppingListItem, Void, Void> {
        private final AppDatabase appDatabase;
        private final ShoppingListItemsInsertedListener listener;

        insertShoppingListItemsAsyncTask(
                AppDatabase appDatabase,
                ShoppingListItemsInsertedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(ShoppingListItem... items) {
            appDatabase.shoppingListItemDao().insertAll(items);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }
}
