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
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;

public class OverviewStartRepository {
    private final AppDatabase appDatabase;

    public OverviewStartRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
    }

    public interface DataListener {
        void actionFinished(
                ArrayList<StockItem> stockItems,
                ArrayList<ShoppingListItem> shoppingListItems
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

        loadAsyncTask(AppDatabase appDatabase, DataListener listener) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            stockItems = new ArrayList<>(appDatabase.stockItemDao().getAll());
            shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(stockItems, shoppingListItems);
        }
    }

    public void updateDatabase(
            ArrayList<StockItem> stockItems,
            ArrayList<ShoppingListItem> shoppingListItems,
            DataUpdatedListener listener
    ) {
        new updateAsyncTask(
                appDatabase,
                stockItems,
                shoppingListItems,
                listener
        ).execute();
    }

    private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final DataUpdatedListener listener;

        private final ArrayList<StockItem> stockItems;
        private final ArrayList<ShoppingListItem> shoppingListItems;

        updateAsyncTask(
                AppDatabase appDatabase,
                ArrayList<StockItem> stockItems,
                ArrayList<ShoppingListItem> shoppingListItems,
                DataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.stockItems = stockItems;
            this.shoppingListItems = shoppingListItems;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            appDatabase.stockItemDao().deleteAll();
            appDatabase.stockItemDao().insertAll(stockItems);
            appDatabase.shoppingListItemDao().deleteAll();
            appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished();
        }
    }
}
