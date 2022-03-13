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
import java.util.List;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.repository.MasterDataOverviewRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class MasterDataOverviewViewModel extends BaseViewModel {

  private static final String TAG = MasterDataOverviewViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final MasterDataOverviewRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private final MutableLiveData<List<Store>> storesLive;
  private final MutableLiveData<List<Location>> locationsLive;
  private final MutableLiveData<List<ProductGroup>> productGroupsLive;
  private final MutableLiveData<List<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<List<TaskCategory>> taskCategoriesLive;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  public MasterDataOverviewViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new MasterDataOverviewRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    storesLive = new MutableLiveData<>();
    locationsLive = new MutableLiveData<>();
    productGroupsLive = new MutableLiveData<>();
    quantityUnitsLive = new MutableLiveData<>();
    productsLive = new MutableLiveData<>();
    taskCategoriesLive = new MutableLiveData<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.storesLive.setValue(data.getStores());
      this.locationsLive.setValue(data.getLocations());
      this.productGroupsLive.setValue(data.getProductGroups());
      this.quantityUnitsLive.setValue(data.getQuantityUnits());
      this.productsLive.setValue(data.getProducts());
      this.taskCategoriesLive.setValue(data.getTaskCategories());
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
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(() -> {}, this::onDownloadError);
    queue.append(
        dlHelper.updateStores(dbChangedTime, this.storesLive::setValue),
        dlHelper.updateLocations(dbChangedTime, this.locationsLive::setValue),
        dlHelper.updateProductGroups(dbChangedTime, this.productGroupsLive::setValue),
        dlHelper.updateQuantityUnits(dbChangedTime, this.quantityUnitsLive::setValue),
        dlHelper.updateProducts(dbChangedTime, this.productsLive::setValue),
        dlHelper.updateTaskCategories(dbChangedTime, this.taskCategoriesLive::setValue)
    );

    if (queue.isEmpty()) {
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null);
    editPrefs.apply();
    downloadData();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    String exact = error == null ? null : error.getLocalizedMessage();
    infoFullscreenLive.setValue(
        new InfoFullscreen(InfoFullscreen.ERROR_NETWORK, exact, this::downloadData)
    );
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
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  @NonNull
  public MutableLiveData<List<Store>> getStoresLive() {
    return storesLive;
  }

  public MutableLiveData<List<Location>> getLocationsLive() {
    return locationsLive;
  }

  public MutableLiveData<List<ProductGroup>> getProductGroupsLive() {
    return productGroupsLive;
  }

  public MutableLiveData<List<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }

  public MutableLiveData<List<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<List<TaskCategory>> getTaskCategoriesLive() {
    return taskCategoriesLive;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
