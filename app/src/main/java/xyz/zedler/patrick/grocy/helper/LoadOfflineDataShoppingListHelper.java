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

import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

public class LoadOfflineDataShoppingListHelper extends AsyncTask<Void, Void, String> {
    private WeakReference<Activity> weakActivity;
    private AsyncResponse response;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;

    public LoadOfflineDataShoppingListHelper(Activity activity, AsyncResponse response) {
        weakActivity = new WeakReference<>(activity);
        this.response = response;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Activity activity = weakActivity.get();
        AppDatabase database = AppDatabase.getAppDatabase(activity.getApplicationContext());
        shoppingListItems = new ArrayList<>(database.shoppingListItemDao().getAll());
        shoppingLists = new ArrayList<>(database.shoppingListDao().getAll());
        productGroups = new ArrayList<>(database.productGroupDao().getAll());
        quantityUnits = new ArrayList<>(database.quantityUnitDao().getAll());
        return null;
    }

    @Override
    protected void onPostExecute(String arg) {
        response.prepareOfflineData(
                shoppingListItems,
                shoppingLists,
                productGroups,
                quantityUnits
        );
    }

    public interface AsyncResponse {
        void prepareOfflineData(
                ArrayList<ShoppingListItem> shoppingListItems,
                ArrayList<ShoppingList> shoppingLists,
                ArrayList<ProductGroup> productGroups,
                ArrayList<QuantityUnit> quantityUnits
        );
    }
}
