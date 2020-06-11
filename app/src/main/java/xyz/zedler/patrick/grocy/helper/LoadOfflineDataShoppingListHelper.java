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

import android.os.AsyncTask;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;

public class LoadOfflineDataShoppingListHelper extends AsyncTask<Void, Void, String> {
    private AppDatabase appDatabase;
    private AsyncResponse response;
    private ArrayList<ShoppingListItem> shoppingListItems;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;

    public LoadOfflineDataShoppingListHelper(AppDatabase appDatabase, AsyncResponse response) {
        this.appDatabase = appDatabase;
        this.response = response;
    }

    @Override
    protected String doInBackground(Void... voids) {
        shoppingListItems = new ArrayList<>(appDatabase.shoppingListItemDao().getAll());
        shoppingLists = new ArrayList<>(appDatabase.shoppingListDao().getAll());
        productGroups = new ArrayList<>(appDatabase.productGroupDao().getAll());
        quantityUnits = new ArrayList<>(appDatabase.quantityUnitDao().getAll());
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
