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
import java.util.HashMap;
import java.util.List;

import xyz.zedler.patrick.grocy.dao.ProductGroupDao;
import xyz.zedler.patrick.grocy.dao.QuantityUnitDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListDao;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

public class AppRepository {
    private AppDatabase appDatabase;
    private ShoppingListDao shoppingListDao;
    private ShoppingListItemDao shoppingListItemDao;
    private ProductGroupDao productGroupDao;
    private QuantityUnitDao quantityUnitDao;

    public AppRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
        this.shoppingListDao = appDatabase.shoppingListDao();
        this.shoppingListItemDao = appDatabase.shoppingListItemDao();
        this.productGroupDao = appDatabase.productGroupDao();
        this.quantityUnitDao = appDatabase.quantityUnitDao();
    }

    // repository.loadFromDatabase(shoppingListData -> groupItems());
    // repository.updateDatabase(shoppingListData, updatedData -> groupItems());

    public interface ShoppingListDataListener {
        void actionFinished(
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<ShoppingList> shoppingLists,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits
        );
    }

    public interface ShoppingListDataUpdatedListener {
        void actionFinished(ArrayList<ShoppingListItem> offlineChangedItems, HashMap<Integer, ShoppingListItem> serverItemsHashMap);
    }

    public interface ShoppingListItemsInsertedListener {
        void actionFinished();
    }

    public void loadFromDatabase(ShoppingListDataListener listener) {
        new loadAsyncTask(appDatabase, listener).execute();
    }

    private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final ShoppingListDataListener listener;

        private ArrayList<ShoppingListItem> shoppingListItems;
        private ArrayList<ShoppingList> shoppingLists;
        private ArrayList<ProductGroup> productGroups;
        private ArrayList<QuantityUnit> quantityUnits;

        loadAsyncTask(AppDatabase appDatabase, ShoppingListDataListener listener) {
            this.appDatabase = appDatabase;
            this.listener = listener;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll()); // TODO: List instead of ArrayList maybe
            shoppingLists = new ArrayList<>(appDatabase.shoppingListDao().getAll());
            productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
            quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(shoppingListItems, shoppingLists, productGroups, quantityUnits);
        }
    }

    public void updateDatabase(
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<Product> products,
            ArrayList<MissingItem> missingItems,
            ShoppingListDataUpdatedListener listener
    ) {
        new updateAsyncTask(appDatabase, shoppingListItems, shoppingLists, productGroups, quantityUnits, products, missingItems, listener).execute();
    }

    private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;
        private final ShoppingListDataUpdatedListener listener;

        private final ArrayList<ShoppingListItem> shoppingListItems;
        private final ArrayList<ShoppingList> shoppingLists;
        private final ArrayList<ProductGroup> productGroups;
        private final ArrayList<QuantityUnit> quantityUnits;
        private final ArrayList<Product> products;
        private final ArrayList<MissingItem> missingItems;
        private final ArrayList<ShoppingListItem> itemsToSync;
        private final HashMap<Integer, ShoppingListItem> serverItemsHashMap;

        updateAsyncTask(
                AppDatabase appDatabase,
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<ShoppingList> shoppingLists,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<Product> products,
                ArrayList<MissingItem> missingItems,
                ShoppingListDataUpdatedListener listener
        ) {
            this.appDatabase = appDatabase;
            this.listener = listener;
            this.shoppingListItems = shoppingListItems;
            this.shoppingLists = shoppingLists;
            this.productGroups = productGroups;
            this.quantityUnits = quantityUnits;
            this.products = products;
            this.missingItems = missingItems;
            this.itemsToSync = new ArrayList<>();
            this.serverItemsHashMap = new HashMap<>();
        }

        @Override
        protected final Void doInBackground(Void... params) {
            // fill shopping list items with product objects
            HashMap<Integer, Product> productHashMap = new HashMap<>();
            for(Product p : this.products) productHashMap.put(p.getId(), p);
            ArrayList<String> missingProductIds = new ArrayList<>();
            for(MissingItem missingItem : this.missingItems) {
                missingProductIds.add(String.valueOf(missingItem.getId()));
            }

            ArrayList<ShoppingListItem> shoppingListItemsWithProduct
                    = new ArrayList<>(this.shoppingListItems);

            for(ShoppingListItem item : shoppingListItemsWithProduct) {
                if(item.getProductId() == null) continue;
                Product product = productHashMap.get(Integer.parseInt(item.getProductId()));
                if(product != null) item.setProduct(product);
                if(missingProductIds.contains(item.getProductId())) item.setIsMissing(true);
            }


            for(ShoppingListItem s : shoppingListItems) serverItemsHashMap.put(s.getId(), s);

            List<ShoppingListItem> offlineItems = appDatabase.shoppingListItemDao().getAll();
            // compare server items with offline items and add modified to separate list
            for(ShoppingListItem offlineItem : offlineItems) {
                ShoppingListItem serverItem = serverItemsHashMap.get(offlineItem.getId());
                if(serverItem != null  // sync only items which are still on server
                        && offlineItem.getDoneSynced() != -1
                        && offlineItem.getDone() != offlineItem.getDoneSynced()
                        && offlineItem.getDone() != serverItem.getDone()
                ) itemsToSync.add(offlineItem);
            }

            appDatabase.shoppingListItemDao().deleteAll();
            appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
            appDatabase.shoppingListDao().deleteAll();
            appDatabase.shoppingListDao().insertAll(shoppingLists);
            appDatabase.productGroupDao().deleteAll();
            appDatabase.productGroupDao().insertAll(productGroups);
            appDatabase.quantityUnitDao().deleteAll();
            appDatabase.quantityUnitDao().insertAll(quantityUnits);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(listener != null) listener.actionFinished(itemsToSync, serverItemsHashMap);
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

    /*public void getAllShoppingListData() {
        new overwriteAsyncTask(shoppingListItemDao).execute();
    }

    private static class getAllDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private final ShoppingListItemDao shoppingListItemDao;
        private ArrayList<ShoppingListItem> shoppingListItems;

        getAsyncTask(ShoppingListItemDao dao) {
            shoppingListItemDao = dao;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            shoppingListItems = shoppingListItemDao.getAll();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }*/

    /*public void insertShoppingListItem(ShoppingListItem shoppingListItem) {
        new overwriteAsyncTask(shoppingListItemDao).execute(shoppingListItem);
    }*/

    /*public void deleteItem(DataItem dataItem) {
        new deleteAsyncTask(mDataDao).execute(dataItem);
    }

    private static class deleteAsyncTask extends AsyncTask<DataItem, Void, Void> {
        private DataDAO mAsyncTaskDao;
        deleteAsyncTask(DataDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final DataItem... params) {
            mAsyncTaskDao.deleteItem(params[0]);
            return null;
        }
    }

    public void deleteItemById(Long idItem) {
        new deleteByIdAsyncTask(mDataDao).execute(idItem);
    }

    private static class deleteByIdAsyncTask extends AsyncTask<Long, Void, Void> {
        private DataDAO mAsyncTaskDao;
        deleteByIdAsyncTask(DataDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.deleteByItemId(params[0]);
            return null;
        }
    }*/
}
