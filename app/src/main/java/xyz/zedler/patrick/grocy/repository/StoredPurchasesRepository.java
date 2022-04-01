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
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.StoredPurchase;
import xyz.zedler.patrick.grocy.repository.PurchaseRepository.SuccessIdListener;

public class StoredPurchasesRepository {

  private final AppDatabase appDatabase;

  public StoredPurchasesRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface DataListener {
    void actionFinished(StoredPurchasesData data);
  }

  public static class StoredPurchasesData {

    private final List<Product> products;
    private final List<PendingProduct> pendingProducts;
    private final List<PendingProductBarcode> pendingProductBarcodes;
    private final List<StoredPurchase> pendingPurchases;

    public StoredPurchasesData(
        List<Product> products,
        List<PendingProduct> pendingProducts,
        List<PendingProductBarcode> pendingProductBarcodes,
        List<StoredPurchase> pendingPurchases
    ) {
      this.products = products;
      this.pendingProducts = pendingProducts;
      this.pendingProductBarcodes = pendingProductBarcodes;
      this.pendingPurchases = pendingPurchases;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<PendingProduct> getPendingProducts() {
      return pendingProducts;
    }

    public List<PendingProductBarcode> getPendingProductBarcodes() {
      return pendingProductBarcodes;
    }

    public List<StoredPurchase> getPendingPurchases() {
      return pendingPurchases;
    }
  }

  public void loadFromDatabase(DataListener listener) {
    Single.zip(
        appDatabase.productDao().getProducts(),
        appDatabase.pendingProductDao().getPendingProducts(),
        appDatabase.pendingProductBarcodeDao().getProductBarcodes(),
        appDatabase.storedPurchaseDao().getStoredPurchases(),
        StoredPurchasesData::new
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void insertPendingProduct(
      PendingProduct pendingProduct,
      SuccessIdListener onSuccess,
      Runnable onError
  ) {
    appDatabase.pendingProductDao().insertPendingProduct(pendingProduct)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::onSuccess)
        .doOnError(e -> onError.run())
        .subscribe();
  }
}
