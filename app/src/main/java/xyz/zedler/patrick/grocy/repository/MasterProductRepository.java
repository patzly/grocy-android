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
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;

public class MasterProductRepository {
    private final AppDatabase appDatabase;

    public MasterProductRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
    }

    public interface DataListener {
        void actionFinished(
                ArrayList<Product> products,
                ArrayList<ProductGroup> productGroups
        );
    }

    public interface ProductsListener {
        void actionFinished(ArrayList<Product> products);
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

        private ArrayList<Product> products;
        private ArrayList<ProductGroup> productGroups;

        loadAsyncTask(AppDatabase appDatabase, DataListener listener) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            products = new ArrayList<>(appDatabase.productDao().getAll());
            productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(products, productGroups);
        }
    }

    public void updateDatabase(
            ArrayList<Product> products,
            ArrayList<ProductGroup> productGroups,
            DataUpdatedListener listener
    ) {
        new updateAsyncTask(
                appDatabase,
                products,
                productGroups,
                listener
        ).execute();
    }

    private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final DataUpdatedListener listener;

        private final ArrayList<Product> products;
        private final ArrayList<ProductGroup> productGroups;

        updateAsyncTask(
                AppDatabase appDatabase,
                ArrayList<Product> products,
                ArrayList<ProductGroup> productGroups,
                DataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.products = products;
            this.productGroups = productGroups;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            appDatabase.productDao().deleteAll();
            appDatabase.productDao().insertAll(products);
            appDatabase.productGroupDao().deleteAll();
            appDatabase.productGroupDao().insertAll(productGroups);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }

    public void loadProductsFromDatabase(ProductsListener listener) {
        new loadProductsAsyncTask(appDatabase, listener).execute();
    }

    private static class loadProductsAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final ProductsListener listener;

        private ArrayList<Product> products;

        loadProductsAsyncTask(AppDatabase appDatabase, ProductsListener listener) {
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
            if(listener != null) listener.actionFinished(products);
        }
    }

    public void updateDatabase(
            ArrayList<Product> products,
            DataUpdatedListener listener
    ) {
        new updateProductsAsyncTask(appDatabase, products, listener).execute();
    }

    private static class updateProductsAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final DataUpdatedListener listener;

        private final ArrayList<Product> products;

        updateProductsAsyncTask(
                AppDatabase appDatabase,
                ArrayList<Product> products,
                DataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.products = products;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            appDatabase.productDao().deleteAll();
            appDatabase.productDao().insertAll(products);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }
}
