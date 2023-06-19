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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingList;

public class ShoppingListItemEditRepository {

  private final AppDatabase appDatabase;

  public ShoppingListItemEditRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(ShoppingListItemEditData data);
  }

  public static class ShoppingListItemEditData {

    private final List<ShoppingList> shoppingLists;
    private final List<Product> products;
    private final List<ProductBarcode> barcodes;
    private final List<QuantityUnit> quantityUnits;
    private final List<QuantityUnitConversionResolved> quantityUnitConversions;

    public ShoppingListItemEditData(
        List<ShoppingList> shoppingLists,
        List<Product> products,
        List<ProductBarcode> barcodes,
        List<QuantityUnit> quantityUnits,
        List<QuantityUnitConversionResolved> quantityUnitConversions
    ) {
      this.shoppingLists = shoppingLists;
      this.products = products;
      this.barcodes = barcodes;
      this.quantityUnits = quantityUnits;
      this.quantityUnitConversions = quantityUnitConversions;
    }

    public List<ShoppingList> getShoppingLists() {
      return shoppingLists;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<ProductBarcode> getBarcodes() {
      return barcodes;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<QuantityUnitConversionResolved> getQuantityUnitConversions() {
      return quantityUnitConversions;
    }
  }

  public void loadFromDatabase(DataListener onSuccess, Consumer<Throwable> onError) {
    Single
        .zip(
            appDatabase.shoppingListDao().getShoppingLists(),
            appDatabase.productDao().getProducts(),
            appDatabase.productBarcodeDao().getProductBarcodes(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.quantityUnitConversionResolvedDao().getConversionsResolved(),
            ShoppingListItemEditData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
