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
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.StockEntriesFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockEntryBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataLocation;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataProductGroup;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockEntriesGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockEntriesSort;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.StockEntriesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
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
  private final FilterChipLiveDataStockEntriesSort filterChipLiveDataSort;
  private final FilterChipLiveDataStockEntriesGrouping filterChipLiveDataGrouping;

  private List<StockEntry> stockEntries;
  private HashMap<String, ProductBarcode> productBarcodeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, Location> locationHashMap;
  private HashMap<Integer, Store> storeHashMap;

  private String searchInput;
  @Nullable private final Integer productId;
  private final boolean debug;
  private final int maxDecimalPlacesAmount;

  public StockEntriesViewModel(@NonNull Application application, StockEntriesFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    productId = NumUtil.isStringInt(args.getProductId())
        ? Integer.parseInt(args.getProductId()) : null;

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
        this::updateFilteredStockEntries
    );
    filterChipLiveDataSort = new FilterChipLiveDataStockEntriesSort(
        getApplication(),
        this::updateFilteredStockEntries
    );
    filterChipLiveDataGrouping = new FilterChipLiveDataStockEntriesGrouping(
        getApplication(),
        this::updateFilteredStockEntries
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productBarcodeHashMap = ArrayUtil.getProductBarcodesHashMap(data.getProductBarcodes());
      this.stockEntries = data.getStockEntries();

      filterChipLiveDataLocation.setLocations(data.getLocations());
      locationHashMap = ArrayUtil.getLocationsHashMap(data.getLocations());
      storeHashMap = ArrayUtil.getStoresHashMap(data.getStores());

      updateFilteredStockEntries();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(boolean skipOfflineCheck) {
    if (!skipOfflineCheck && isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredStockEntries();
      return;
    }
    dlHelper.updateData(
        () -> {
          if (isOffline()) setOfflineLive(false);
          loadFromDatabase(false);
        },
        this::onDownloadError,
        QuantityUnit.class,
        StockEntry.class,
        Product.class,
        ProductBarcode.class,
        Location.class,
        Store.class
    );
  }

  public void downloadData() {
    downloadData(false);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ENTRIES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STORES, null);
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

  public void updateFilteredStockEntries() {
    ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();

    ProductBarcode productBarcodeSearch = null;
    if (searchInput != null && !searchInput.isEmpty()) {
      productBarcodeSearch = productBarcodeHashMap.get(searchInput);
    }

    for (StockEntry entry : this.stockEntries) {

      if (productId != null && entry.getProductId() != productId) {
        continue;
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        Product product = productHashMap.get(entry.getProductId());
        String productName = product != null ? product.getName().toLowerCase() : null;
        searchContainsItem = productName != null && productName.contains(searchInput);
        if (!searchContainsItem) {
          searchContainsItem = entry.getNote() != null && !entry.getNote().isEmpty()
              && entry.getNote().toLowerCase().contains(searchInput);
        }
      }
      if (!searchContainsItem && productBarcodeSearch == null
          || !searchContainsItem && productBarcodeSearch.getProductIdInt() != entry.getProductId()) {
        continue;
      }

      int locationFilterId = filterChipLiveDataLocation.getSelectedId();
      if (locationFilterId != FilterChipLiveDataLocation.NO_FILTER
          && entry.getLocationIdInt() != locationFilterId) {
        continue;
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

  public void showStockEntryBottomSheet(StockEntry stockEntry) {
    Product product = productHashMap.get(stockEntry.getProductId());
    QuantityUnit quantityUnitStock = product != null
        ? quantityUnitHashMap.get(product.getQuIdStockInt()) : null;
    QuantityUnit quantityUnitPurchase = product != null
        ? quantityUnitHashMap.get(product.getQuIdPurchaseInt()) : null;
    Location location = locationHashMap.get(stockEntry.getLocationIdInt());
    Store store = storeHashMap.get(stockEntry.getShoppingLocationIdInt());
    Bundle bundle = new Bundle();
    bundle.putParcelable(ARGUMENT.PRODUCT, product);
    bundle.putParcelable(ARGUMENT.QUANTITY_UNIT_PURCHASE, quantityUnitPurchase);
    bundle.putParcelable(ARGUMENT.QUANTITY_UNIT_STOCK, quantityUnitStock);
    bundle.putParcelable(ARGUMENT.LOCATION, location);
    bundle.putParcelable(ARGUMENT.STORE, store);
    bundle.putParcelable(ARGUMENT.STOCK_ENTRY, stockEntry);
    showBottomSheet(new StockEntryBottomSheet(), bundle);
  }

  public void performAction(String action, StockEntry stockEntry) {
    Product product = productHashMap.get(stockEntry.getProductId());
    if (product == null) {
      showErrorMessage();
      return;
    }
    switch (action) {
      case Constants.ACTION.CONSUME:
        consumeEntry(stockEntry, product, false);
        break;
      case Constants.ACTION.OPEN:
        openEntry(stockEntry, product);
        break;
      case Constants.ACTION.CONSUME_SPOILED:
        consumeEntry(stockEntry, product, true);
        break;
    }
  }

  private void consumeEntry(StockEntry stockEntry, Product product, boolean spoiled) {
    JSONObject body = new JSONObject();
    try {
      body.put("amount", stockEntry.getAmount());
      body.put("exact_amount", true);
      if (NumUtil.isStringInt(stockEntry.getLocationId())) {
        body.put("location_id", stockEntry.getLocationId());
      }
      body.put("spoiled", spoiled);
      body.put("stock_entry_id", stockEntry.getStockId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "consumeEntry: " + e);
      }
    }
    dlHelper.postWithArray(
        grocyApi.consumeProduct(stockEntry.getProductId()),
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
              Log.e(TAG, "consumeEntry: " + e);
            }
          }

          String msg = getApplication().getString(
              spoiled ? R.string.msg_consumed_spoiled : R.string.msg_consumed,
              NumUtil.trimAmount(amountConsumed, maxDecimalPlacesAmount),
              pluralUtil.getQuantityUnitPlural(
                  quantityUnitHashMap,
                  product.getQuIdStockInt(),
                  amountConsumed
              ), product.getName()
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
                    Log.i(TAG, "consumeEntry: undone");
                  }
                },
                this::showNetworkErrorMessage
            ));
          }
          downloadData();
          showSnackbar(snackbarMsg);
          if (debug) {
            Log.i(
                TAG, "consumeEntry: consumed " + amountConsumed
            );
          }
        },
        error -> {
          showNetworkErrorMessage(error);
          if (debug) {
            Log.i(TAG, "consumeEntry: " + error);
          }
        }
    );
  }

  private void openEntry(StockEntry stockEntry, Product product) {
    JSONObject body = new JSONObject();
    try {
      body.put("amount", stockEntry.getAmount());
      body.put("stock_entry_id", stockEntry.getStockId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "openEntry: " + e);
      }
    }
    dlHelper.postWithArray(
        grocyApi.openProduct(stockEntry.getProductId()),
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
              Log.e(TAG, "openEntry: " + e);
            }
          }

          String msg = getApplication().getString(
              R.string.msg_opened,
              NumUtil.trimAmount(amountOpened, maxDecimalPlacesAmount),
              pluralUtil.getQuantityUnitPlural(
                  quantityUnitHashMap,
                  product.getQuIdStockInt(),
                  amountOpened
              ), product.getName()
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
                    Log.i(TAG, "openEntry: undone");
                  }
                },
                this::showNetworkErrorMessage
            ));
          }
          downloadData();
          showSnackbar(snackbarMsg);
          if (debug) {
            Log.i(
                TAG, "openEntry: opened " + amountOpened
            );
          }
        },
        error -> {
          showNetworkErrorMessage(error);
          if (debug) {
            Log.i(TAG, "openEntry: " + error);
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

    updateFilteredStockEntries();
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

  public HashMap<Integer, Store> getStoreHashMap() {
    return storeHashMap;
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

  public boolean hasProductFilter() {
    return productId != null;
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

  public String getCurrency() {
    return sharedPrefs.getString(PREF.CURRENCY, "");
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class StockEntriesViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final StockEntriesFragmentArgs args;

    public StockEntriesViewModelFactory(Application application, StockEntriesFragmentArgs args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new StockEntriesViewModel(application, args);
    }
  }
}
