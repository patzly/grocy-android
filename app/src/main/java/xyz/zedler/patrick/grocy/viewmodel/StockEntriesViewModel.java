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
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataLocation;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataProductGroup;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.repository.StockEntriesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class StockEntriesViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final StockEntriesRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<StockEntry>> filteredStockEntriesLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final FilterChipLiveDataLocation filterChipLiveDataLocation;
  private final FilterChipLiveDataStockSort filterChipLiveDataSort;
  private final FilterChipLiveDataStockGrouping filterChipLiveDataGrouping;

  private List<StockEntry> stockEntries;
  private List<Product> products;
  private HashMap<String, ProductBarcode> productBarcodeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, Location> locationHashMap;

  private String searchInput;
  private final boolean debug;

  public StockEntriesViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new StockEntriesRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredStockEntriesLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);

    filterChipLiveDataLocation = new FilterChipLiveDataLocation(
        getApplication(),
        this::updateFilteredStockItems
    );
    filterChipLiveDataSort = new FilterChipLiveDataStockSort(
        getApplication(),
        this::updateFilteredStockItems
    );
    filterChipLiveDataGrouping = new FilterChipLiveDataStockGrouping(
        getApplication(),
        this::updateFilteredStockItems
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.products = data.getProducts();
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productBarcodeHashMap = ArrayUtil.getProductBarcodesHashMap(data.getProductBarcodes());
      this.stockEntries = data.getStockEntries();

      filterChipLiveDataLocation.setLocations(data.getLocations());
      locationHashMap = ArrayUtil.getLocationsHashMap(data.getLocations());

      updateFilteredStockItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData() {
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredStockItems();
      return;
    }
    dlHelper.updateData(
        () -> loadFromDatabase(false),
        this::onDownloadError,
        QuantityUnit.class,
        StockEntry.class,
        Product.class,
        ProductBarcode.class,
        Location.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ENTRIES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.apply();
    downloadData();
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

  public void updateFilteredStockItems() {
    ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();

    ProductBarcode productBarcodeSearch = null;
    if (searchInput != null && !searchInput.isEmpty()) {
      productBarcodeSearch = productBarcodeHashMap.get(searchInput);
    }

    for (StockEntry entry : this.stockEntries) {

      boolean searchContainsItem = true;
      /*if (searchInput != null && !searchInput.isEmpty()) {
        String productName = item.getProduct().getName().toLowerCase();
        searchContainsItem = productName.contains(searchInput);
      }
      if (!searchContainsItem && productBarcodeSearch == null
          || !searchContainsItem && productBarcodeSearch.getProductIdInt() != item.getProductId()) {
        continue;
      }*/

      int locationFilterId = filterChipLiveDataLocation.getSelectedId();
      if (locationFilterId != FilterChipLiveDataLocation.NO_FILTER) {
        /*HashMap<Integer, StockLocation> stockLocationsForProductId
            = stockLocationsHashMap.get(item.getProductId());
        if (stockLocationsForProductId == null
            || !stockLocationsForProductId.containsKey(locationFilterId)
        ) {
          continue;
        }*/
      }

      filteredStockEntries.add(entry);
    }

    if (filteredStockEntries.isEmpty()) {
      InfoFullscreen info;
      if (searchInput != null && !searchInput.isEmpty()) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
      } else if (filterChipLiveDataLocation.getSelectedId()
          != FilterChipLiveDataProductGroup.NO_FILTER
      ) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
      } else {
        info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_STOCK);
      }
      infoFullscreenLive.setValue(info);
    } else {
      infoFullscreenLive.setValue(null);
    }

    filteredStockEntriesLive.setValue(filteredStockEntries);
  }

  public void performAction(String action, StockItem stockItem) {
    switch (action) {
      case Constants.ACTION.CONSUME:
        consumeProduct(stockItem, stockItem.getProduct().getQuickConsumeAmountDouble(), false);
        break;
      case Constants.ACTION.OPEN:
        openProduct(stockItem, stockItem.getProduct().getQuickConsumeAmountDouble());
        break;
      case Constants.ACTION.CONSUME_ALL:
        consumeProduct(
            stockItem,
            stockItem.getProduct().getEnableTareWeightHandlingInt() == 0
                ? stockItem.getAmountDouble()
                : stockItem.getProduct().getTareWeightDouble(),
            false
        );
        break;
      case Constants.ACTION.CONSUME_SPOILED:
        consumeProduct(stockItem, 1, true);
        break;
    }
  }

  private void consumeProduct(StockItem stockItem, double amount, boolean spoiled) {
    JSONObject body = new JSONObject();
    try {
      body.put("amount", amount);
      body.put("allow_subproduct_substitution", true);
      body.put("spoiled", spoiled);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "consumeProduct: " + e);
      }
    }
    dlHelper.postWithArray(
        grocyApi.consumeProduct(stockItem.getProductId()),
        body,
        response -> {
          String transactionId = null;
          double amountConsumed = 0;
          try {
            transactionId = response.getJSONObject(0)
                .getString("transaction_id");
            for (int i = 0; i < response.length(); i++) {
              amountConsumed -= response.getJSONObject(i).getDouble("amount");
            }
          } catch (JSONException e) {
            if (debug) {
              Log.e(TAG, "consumeProduct: " + e);
            }
          }

          String msg = getApplication().getString(
              spoiled ? R.string.msg_consumed_spoiled : R.string.msg_consumed,
              NumUtil.trim(amountConsumed),
              pluralUtil.getQuantityUnitPlural(
                  quantityUnitHashMap,
                  stockItem.getProduct().getQuIdStockInt(),
                  amountConsumed
              ), stockItem.getProduct().getName()
          );
          SnackbarMessage snackbarMsg = new SnackbarMessage(msg, 15);

          // set undo button on snackBar
          if (transactionId != null) {
            String finalTransactionId = transactionId;
            snackbarMsg.setAction(getString(R.string.action_undo), v -> dlHelper.post(
                grocyApi.undoStockTransaction(finalTransactionId),
                response1 -> {
                  downloadData();
                  showSnackbar(new SnackbarMessage(
                      getString(R.string.msg_undone_transaction),
                      Snackbar.LENGTH_SHORT
                  ));
                  if (debug) {
                    Log.i(TAG, "consumeProduct: undone");
                  }
                },
                this::showErrorMessage
            ));
          }
          downloadData();
          showSnackbar(snackbarMsg);
          if (debug) {
            Log.i(
                TAG, "consumeProduct: consumed " + amountConsumed
            );
          }
        },
        error -> {
          showErrorMessage(error);
          if (debug) {
            Log.i(TAG, "consumeProduct: " + error);
          }
        }
    );
  }

  private void openProduct(StockItem stockItem, double amount) {
    JSONObject body = new JSONObject();
    try {
      body.put("amount", amount);
      body.put("allow_subproduct_substitution", true);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "openProduct: " + e);
      }
    }
    dlHelper.postWithArray(
        grocyApi.openProduct(stockItem.getProductId()),
        body,
        response -> {
          String transactionId = null;
          double amountOpened = 0;
          try {
            transactionId = response.getJSONObject(0)
                .getString("transaction_id");
            for (int i = 0; i < response.length(); i++) {
              amountOpened += response.getJSONObject(i).getDouble("amount");
            }
          } catch (JSONException e) {
            if (debug) {
              Log.e(TAG, "openProduct: " + e);
            }
          }

          String msg = getApplication().getString(
              R.string.msg_opened,
              NumUtil.trim(amountOpened),
              pluralUtil.getQuantityUnitPlural(
                  quantityUnitHashMap,
                  stockItem.getProduct().getQuIdStockInt(),
                  amountOpened
              ), stockItem.getProduct().getName()
          );
          SnackbarMessage snackbarMsg = new SnackbarMessage(msg, 15);

          // set undo button on snackBar
          if (transactionId != null) {
            String finalTransactionId = transactionId;
            snackbarMsg.setAction(getString(R.string.action_undo), v -> dlHelper.post(
                grocyApi.undoStockTransaction(finalTransactionId),
                response1 -> {
                  downloadData();
                  showSnackbar(new SnackbarMessage(
                      getString(R.string.msg_undone_transaction),
                      Snackbar.LENGTH_SHORT
                  ));
                  if (debug) {
                    Log.i(TAG, "openProduct: undone");
                  }
                },
                this::showErrorMessage
            ));
          }
          downloadData();
          showSnackbar(snackbarMsg);
          if (debug) {
            Log.i(
                TAG, "openProduct: opened " + amountOpened
            );
          }
        },
        error -> {
          showErrorMessage(error);
          if (debug) {
            Log.i(TAG, "openProduct: " + error);
          }
        }
    );
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<StockEntry>> getFilteredStockEntriesLive() {
    return filteredStockEntriesLive;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();

    updateFilteredStockItems();
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, Location> getLocationHashMap() {
    return locationHashMap;
  }

  public Location getLocationFromId(int id) {
    return locationHashMap.get(id);
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public QuantityUnit getQuantityUnitFromId(int id) {
    return quantityUnitHashMap.get(id);
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataLocation() {
    return () -> filterChipLiveDataLocation;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public String getSortMode() {
    return filterChipLiveDataSort.getSortMode();
  }

  public boolean isSortAscending() {
    return filterChipLiveDataSort.isSortAscending();
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataGrouping() {
    return () -> filterChipLiveDataGrouping;
  }

  public String getGroupingMode() {
    return filterChipLiveDataGrouping.getGroupingMode();
  }

  public MutableLiveData<Boolean> getScannerVisibilityLive() {
    return scannerVisibilityLive;
  }

  public boolean isScannerVisible() {
    assert scannerVisibilityLive.getValue() != null;
    return scannerVisibilityLive.getValue();
  }

  public void toggleScannerVisibility() {
    scannerVisibilityLive.setValue(!isScannerVisible());
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

  public int getDaysExpriringSoon() {
    String days = sharedPrefs.getString(
        STOCK.DUE_SOON_DAYS,
        SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
    );
    return NumUtil.isStringInt(days) ? Integer.parseInt(days) : 5;
  }

  public String getCurrency() {
    return sharedPrefs.getString(
        PREF.CURRENCY,
        ""
    );
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
