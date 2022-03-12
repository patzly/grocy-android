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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Product;

public class ChooseProductRepository {

  private final AppDatabase appDatabase;

  public ChooseProductRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(ChooseProductData data);
  }

  public static class ChooseProductData {

    private final List<Product> products;

    public ChooseProductData(List<Product> products) {
      this.products = products;
    }

    public List<Product> getProducts() {
      return products;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    appDatabase.productDao().getProducts()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(products -> listener.actionFinished(new ChooseProductData(products)))
        .subscribe();
  }
}
