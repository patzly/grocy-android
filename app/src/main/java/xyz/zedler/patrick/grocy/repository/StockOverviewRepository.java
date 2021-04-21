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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.os.AsyncTask;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;

public class StockOverviewRepository {
    private final AppDatabase appDatabase;

    public StockOverviewRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
    }

    public interface StockOverviewDataListener {
        void actionFinished(
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<ProductGroup> productGroups,
                ArrayList<StockItem> stockItems,
                ArrayList<Product> products,
                ArrayList<MissingItem> missingItems,
                ArrayList<StockItem> missingStockItems,
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<Location> locations
        );
    }

    public interface StockOverviewDataUpdatedListener {
        void actionFinished();
    }

    public void loadFromDatabase(StockOverviewDataListener listener) {
        new loadAsyncTask(appDatabase, listener).execute();
    }

    private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final StockOverviewDataListener listener;

        private ArrayList<QuantityUnit> quantityUnits;
        private ArrayList<ProductGroup> productGroups;
        private ArrayList<StockItem> stockItems;
        private ArrayList<Product> products;
        private ArrayList<MissingItem> missingItems;
        private ArrayList<StockItem> missingStockItems;
        private ArrayList<ShoppingListItem> shoppingListItems;
        private ArrayList<Location> locations;

        loadAsyncTask(AppDatabase appDatabase, StockOverviewDataListener listener) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
            productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
            stockItems = new ArrayList<>(appDatabase.stockItemDao().getAll());
            products = new ArrayList<>(appDatabase.productDao().getAll());

            missingStockItems = new ArrayList<>();

            missingItems = new ArrayList<>(appDatabase.missingItemDao().getAll());

            shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll());
            locations = new ArrayList<>(appDatabase.locationDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(quantityUnits, productGroups, stockItems, products, missingItems, missingStockItems, shoppingListItems, locations);
        }
    }

    public void updateDatabase(
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<ProductGroup> productGroups,
            ArrayList<StockItem> stockItems,
            ArrayList<Product> products,
            ArrayList<MissingItem> missingItems,
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<Location> locations,
            StockOverviewDataUpdatedListener listener
    ) {
        new updateAsyncTask(
                appDatabase,
                quantityUnits,
                productGroups,
                stockItems,
                products,
                missingItems,
                shoppingListItems,
                locations,
                listener
        ).execute();
    }

    private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final StockOverviewDataUpdatedListener listener;

        private final ArrayList<QuantityUnit> quantityUnits;
        private final ArrayList<ProductGroup> productGroups;
        private final ArrayList<StockItem> stockItems;
        private final ArrayList<Product> products;
        private final ArrayList<MissingItem> missingItems;
        private final ArrayList<ShoppingListItem> shoppingListItems;
        private final ArrayList<Location> locations;

        updateAsyncTask(
                AppDatabase appDatabase,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<ProductGroup> productGroups,
                ArrayList<StockItem> stockItems,
                ArrayList<Product> products,
                ArrayList<MissingItem> missingItems,
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<Location> locations,
                StockOverviewDataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.quantityUnits = quantityUnits;
            this.productGroups = productGroups;
            this.stockItems = stockItems;
            this.products = products;
            this.missingItems = missingItems;
            this.shoppingListItems = shoppingListItems;
            this.locations = locations;
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
            appDatabase.missingItemDao().deleteAll();
            appDatabase.missingItemDao().insertAll(missingItems);
            appDatabase.shoppingListItemDao().deleteAll();
            appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
            appDatabase.locationDao().deleteAll();
            appDatabase.locationDao().insertAll(locations);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }
}
