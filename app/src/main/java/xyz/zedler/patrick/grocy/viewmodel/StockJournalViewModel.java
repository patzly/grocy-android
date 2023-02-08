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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockLogEntry;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.repository.StockEntriesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class StockJournalViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final StockEntriesRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<StockLogEntry>> filteredStockLogEntriesLive;

  private List<StockLogEntry> stockLogEntries;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, Location> locationHashMap;
  private HashMap<Integer, User> userHashMap;

  private String searchInput;
  private final boolean debug;

  private int currentPage = 0;
  private boolean isLastPage = false;

  public StockJournalViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new StockEntriesRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredStockLogEntriesLive = new MutableLiveData<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());

      locationHashMap = ArrayUtil.getLocationsHashMap(data.getLocations());
      userHashMap = ArrayUtil.getUsersHashMap(data.getUsers());

      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        NetworkQueue queue = dlHelper.newQueue(this::updateFilteredStockLogEntries, this::onDownloadError);
        queue.append(dlHelper.getStockLogEntries(
            20,
            0,
            entries -> this.stockLogEntries = entries
        ));
        queue.start();
      }
    });
  }

  public void downloadData(boolean skipOfflineCheck) {
    if (!skipOfflineCheck && isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredStockLogEntries();
      return;
    }
    dlHelper.updateData(
        () -> loadFromDatabase(false),
        this::onDownloadError,
        QuantityUnit.class,
        Product.class,
        ProductBarcode.class,
        Location.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_USERS, null);
    editPrefs.apply();
    downloadData(true);
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showNetworkErrorMessage(error);
    if (!isOffline()) setOfflineLive(true);
  }

  public void loadNextPage(DownloadHelper.OnStockLogEntriesResponseListener responseListener) {
    NetworkQueue queue = dlHelper.newQueue(() -> {
      if (isOffline()) setOfflineLive(false);
    }, this::onDownloadError);
    queue.append(dlHelper.getStockLogEntries(
        20,
        currentPage*20,
        responseListener
    ));
    queue.start();
  }

  public void updateFilteredStockLogEntries() {
    if (this.stockLogEntries == null) return;
    ArrayList<StockLogEntry> filteredStockLogEntries = new ArrayList<>(this.stockLogEntries);

    if (filteredStockLogEntries.isEmpty()) {
      InfoFullscreen info;
      if (searchInput != null && !searchInput.isEmpty()) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
      } else {
        info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_STOCK);
      }
      infoFullscreenLive.setValue(info);
    } else {
      infoFullscreenLive.setValue(null);
    }

    filteredStockLogEntriesLive.setValue(filteredStockLogEntries);
  }

  public void undoTransaction(StockLogEntry entry) {
    dlHelper.post(
        grocyApi.undoStockTransaction(entry.getTransactionId()),
        response -> {
          downloadData(false);
          showSnackbar(new SnackbarMessage(
              getString(R.string.msg_undone_transaction),
              Snackbar.LENGTH_SHORT
          ));
          if (debug) {
            Log.i(TAG, "undoTransaction");
          }
        },
        this::showNetworkErrorMessage
    );
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<StockLogEntry>> getFilteredStockLogEntriesLive() {
    return filteredStockLogEntriesLive;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();

    updateFilteredStockLogEntries();
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, Location> getLocationHashMap() {
    return locationHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public HashMap<Integer, User> getUserHashMap() {
    return userHashMap;
  }

  public boolean isLastPage() {
    return isLastPage;
  }

  public void setLastPage(boolean lastPage) {
    isLastPage = lastPage;
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public void setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
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

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public String getCurrency() {
    return sharedPrefs.getString(PREF.CURRENCY, "");
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
