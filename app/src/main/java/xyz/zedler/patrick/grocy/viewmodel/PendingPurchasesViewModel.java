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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductBarcode;
import xyz.zedler.patrick.grocy.model.PendingPurchase;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.repository.PendingPurchasesRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class PendingPurchasesViewModel extends BaseViewModel {

  private static final String TAG = PendingPurchasesViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final PendingPurchasesRepository repository;

  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<List<GroupedListItem>> displayedItemsLive;

  private List<Product> products;
  private final HashMap<String, Product> productHashMap;
  private List<PendingProduct> pendingProducts;
  private final HashMap<String, PendingProduct> pendingProductHashMap;
  private List<PendingProductBarcode> pendingProductBarcodes;
  private List<PendingPurchase> pendingPurchases;
  private final HashMap<Integer, List<PendingPurchase>> pendingPurchasesHashMap;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  public PendingPurchasesViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    displayHelpLive = new MutableLiveData<>(false);
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new PendingPurchasesRepository(application);

    offlineLive = new MutableLiveData<>(false);
    displayedItemsLive = new MutableLiveData<>();

    productHashMap = new HashMap<>();
    pendingProductHashMap = new HashMap<>();
    pendingPurchasesHashMap = new HashMap<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      productHashMap.clear();
      for (Product product : products) {
        productHashMap.put(product.getName().toLowerCase(), product);
      }
      this.pendingProducts = data.getPendingProducts();
      pendingProductHashMap.clear();
      for (PendingProduct pendingProduct : this.pendingProducts) {
        pendingProductHashMap.put(pendingProduct.getName().toLowerCase(), pendingProduct);
      }
      this.pendingProductBarcodes = data.getPendingProductBarcodes();
      this.pendingPurchases = data.getPendingPurchases();
      pendingPurchasesHashMap.clear();
      for (PendingPurchase pendingPurchase : this.pendingPurchases) {
        List<PendingPurchase> tempPurchases
            = pendingPurchasesHashMap.get(pendingPurchase.getPendingProductId());
        if (tempPurchases == null) {
          tempPurchases = new ArrayList<>();
        }
        tempPurchases.add(pendingPurchase);
        pendingPurchasesHashMap.put(pendingPurchase.getPendingProductId(), tempPurchases);
      }
      displayItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(
          this::downloadData,
          () -> onDownloadError(null)
      );
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(dlHelper.updateProducts(dbChangedTime, products -> {
      this.products = products;
      productHashMap.clear();
      for (Product product : products) {
        productHashMap.put(product.getName().toLowerCase(), product);
      }
    }));


    if (queue.isEmpty()) {
      onQueueEmpty();
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    displayItems();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void displayItems() {
    ArrayList<GroupedListItem> items = new ArrayList<>();
    for (PendingProduct pendingProduct : pendingProducts) {
      items.add(pendingProduct);
      List<PendingPurchase> pendingPurchases
          = pendingPurchasesHashMap.get(pendingProduct.getId());
      if (pendingPurchases != null) {
        items.addAll(pendingPurchases);
      }
    }
    displayedItemsLive.setValue(items);
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<List<GroupedListItem>> getDisplayedItemsLive() {
    return displayedItemsLive;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    displayHelpLive.setValue(displayHelpLive.getValue() == null || !displayHelpLive.getValue());
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
