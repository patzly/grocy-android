package xyz.zedler.patrick.grocy.helper;

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

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.dao.ShoppingListItemDao;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

public class StoreOfflineDataShoppingListHelper extends AsyncTask<Void, Void, String> {
    private WeakReference<Activity> weakActivity;
    private AsyncResponse response;
    private boolean syncIfNecessary;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Product> products;
    private ArrayList<Integer> usedProductIds;
    private ArrayList<ShoppingListItem> itemsToSync = new ArrayList<>();
    private HashMap<Integer, ShoppingListItem> serverItemHashMap = new HashMap<>();

    public StoreOfflineDataShoppingListHelper(
            Activity activity,
            AsyncResponse response,
            boolean syncIfNecessary,
            ArrayList<ShoppingList> shoppingLists,
            ArrayList<ShoppingListItem> shoppingListItems,
            ArrayList<ProductGroup> productGroups,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<Product> products,
            ArrayList<Integer> usedProductIds
    ) {
        weakActivity = new WeakReference<>(activity);
        this.response = response;
        this.syncIfNecessary = syncIfNecessary;
        this.shoppingLists = shoppingLists;
        this.shoppingListItems = new ArrayList<>(shoppingListItems);
        this.productGroups = productGroups;
        this.quantityUnits = quantityUnits;
        this.products = products;
        this.usedProductIds = usedProductIds;
    }

    @Override
    protected String doInBackground(Void... voids) {
        MainActivity activity = (MainActivity) weakActivity.get();
        AppDatabase database = activity.getDatabase();

        if(syncIfNecessary) {
            serverItemHashMap = new HashMap<>();
            for(ShoppingListItem s : shoppingListItems) serverItemHashMap.put(s.getId(), s);

            ShoppingListItemDao itemDao = database.shoppingListItemDao();
            ArrayList<ShoppingListItem> offlineItems = new ArrayList<>(itemDao.getAll());

            // compare server items with offline items and add modified to separate list
            for(ShoppingListItem offlineItem : offlineItems) {
                ShoppingListItem serverItem = serverItemHashMap.get(offlineItem.getId());
                if(serverItem != null  // sync only items which are still on server
                        && offlineItem.getDoneSynced() != -1
                        && offlineItem.getDone() != offlineItem.getDoneSynced()
                        && offlineItem.getDone() != serverItem.getDone()
                ) {
                    itemsToSync.add(offlineItem);
                }
            }
            if(!itemsToSync.isEmpty()) return null; // don't overwrite offline items yet
        }

        // fill shopping list items with product objects
        HashMap<Integer, Product> productHashMap = new HashMap<>();
        for(Product p : products) productHashMap.put(p.getId(), p);

        for(ShoppingListItem serverItem : shoppingListItems) {
            if(serverItem.getProductId() == null) continue;
            Product product = productHashMap.get(Integer.parseInt(serverItem.getProductId()));
            if(product != null) serverItem.setProduct(product);
        }
        database.shoppingListItemDao().deleteAll();
        database.shoppingListItemDao().insertAll(shoppingListItems);

        // shopping lists
        database.shoppingListDao().deleteAll();
        database.shoppingListDao().insertAll(shoppingLists);

        // product groups
        database.productGroupDao().deleteAll();
        database.productGroupDao().insertAll(productGroups);

        // quantity units
        database.quantityUnitDao().deleteAll();
        database.quantityUnitDao().insertAll(quantityUnits);

        return null;
    }

    @Override
    protected void onPostExecute(String arg) {
        if(itemsToSync.isEmpty()) {
            response.storedDataSuccessfully(shoppingListItems);
        } else {
            response.syncItems(
                    itemsToSync,
                    shoppingLists,
                    shoppingListItems,
                    productGroups,
                    quantityUnits,
                    products,
                    usedProductIds,
                    serverItemHashMap
            );
        }
    }

    public interface AsyncResponse {
        void syncItems(
                ArrayList<ShoppingListItem> itemsToSync,
                ArrayList<ShoppingList> shoppingLists,
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits,
                ArrayList<Product> products,
                ArrayList<Integer> usedProductIds,
                HashMap<Integer, ShoppingListItem> serverItemHashMap
        );

        void storedDataSuccessfully(ArrayList<ShoppingListItem> shoppingListItems);
    }
}
