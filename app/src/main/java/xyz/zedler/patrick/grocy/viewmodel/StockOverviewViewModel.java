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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataLocation;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataProductGroup;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockExtraField;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockStatus;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.repository.StockOverviewRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class StockOverviewViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final StockOverviewRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<StockItem>> filteredStockItemsLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final FilterChipLiveDataStockStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataProductGroup filterChipLiveDataProductGroup;
  private final FilterChipLiveDataLocation filterChipLiveDataLocation;
  private final FilterChipLiveDataStockSort filterChipLiveDataSort;
  private final FilterChipLiveDataStockGrouping filterChipLiveDataGrouping;
  private final FilterChipLiveDataStockExtraField filterChipLiveDataExtraField;

  private List<StockItem> stockItems;
  private List<Product> products;
  private HashMap<Integer, ProductGroup> productGroupHashMap;
  private HashMap<String, ProductBarcode> productBarcodeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, String> productAveragePriceHashMap;
  private HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private List<ShoppingListItem> shoppingListItems;
  private ArrayList<String> shoppingListItemsProductIds;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, MissingItem> productIdsMissingItems;
  private HashMap<Integer, Location> locationHashMap;
  private HashMap<Integer, HashMap<Integer, StockLocation>> stockLocationsHashMap;

  private String searchInput;
  private ArrayList<String> searchResultsFuzzy;
  private final boolean debug;

  public StockOverviewViewModel(@NonNull Application application, StockOverviewFragmentArgs args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new StockOverviewRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredStockItemsLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);

    filterChipLiveDataStatus = new FilterChipLiveDataStockStatus(
        getApplication(),
        this::updateFilteredStockItems
    );
    if (NumUtil.isStringInt(args.getStatusFilterId())) {
      if (Integer.parseInt(args.getStatusFilterId())
          == FilterChipLiveDataStockStatus.STATUS_DUE_SOON) {
        filterChipLiveDataStatus.setStatusDueSoon();
      }
    }
    filterChipLiveDataProductGroup = new FilterChipLiveDataProductGroup(
        getApplication(),
        this::updateFilteredStockItems
    );
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
    filterChipLiveDataExtraField = new FilterChipLiveDataStockExtraField(
        getApplication(),
        this::updateFilteredStockItems
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      productGroupHashMap = ArrayUtil.getProductGroupsHashMap(data.getProductGroups());
      filterChipLiveDataProductGroup.setProductGroups(data.getProductGroups());
      this.products = data.getProducts();
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productAveragePriceHashMap = ArrayUtil
          .getProductAveragePriceHashMap(data.getProductsAveragePrice());
      productLastPurchasedHashMap = ArrayUtil
          .getProductLastPurchasedHashMap(data.getProductsLastPurchased());
      productBarcodeHashMap = ArrayUtil.getProductBarcodesHashMap(data.getProductBarcodes());
      this.stockItems = data.getStockItems();

      int itemsDueCount = 0;
      int itemsOverdueCount = 0;
      int itemsExpiredCount = 0;
      HashMap<Integer, StockItem> stockItemHashMap = ArrayUtil.getStockItemHashMap(stockItems);
      for (VolatileItem volatileItem : data.getVolatileItems()) {
        StockItem stockItem = stockItemHashMap.get(volatileItem.getProductId());
        if (stockItem == null) continue;
        if (volatileItem.getVolatileType() == VolatileItem.TYPE_DUE) {
          stockItem.setItemDue(true);
          itemsDueCount++;
        } else if (volatileItem.getVolatileType() == VolatileItem.TYPE_OVERDUE) {
          stockItem.setItemOverdue(true);
          itemsOverdueCount++;
        } else if (volatileItem.getVolatileType() == VolatileItem.TYPE_EXPIRED) {
          stockItem.setItemExpired(true);
          itemsExpiredCount++;
        }
      }
      int itemsMissingCount = 0;
      productIdsMissingItems = new HashMap<>();
      for (MissingItem missingItem : data.getMissingItems()) {
        itemsMissingCount++;
        productIdsMissingItems.put(missingItem.getId(), missingItem);
        StockItem stockItem = stockItemHashMap.get(missingItem.getId());
        if (stockItem == null && !missingItem.getIsPartlyInStockBoolean()) {
          StockItem stockItemMissing = new StockItem(missingItem);
          stockItems.add(stockItemMissing);
        } else if (stockItem != null) {
          stockItem.setItemMissing(true);
          stockItem.setItemMissingAndPartlyInStock(missingItem.getIsPartlyInStockBoolean());
        }
      }
      int itemsInStockCount = 0;
      int itemsOpenedCount = 0;
      for (StockItem stockItem : stockItems) {
        stockItem.setProduct(productHashMap.get(stockItem.getProductId()));
        if (!stockItem.isItemMissing() || stockItem.isItemMissingAndPartlyInStock()) {
          itemsInStockCount++;
        }
        if (stockItem.getAmountOpenedDouble() > 0) {
          itemsOpenedCount++;
        }
      }

      this.shoppingListItems = data.getShoppingListItems();
      shoppingListItemsProductIds = new ArrayList<>();
      for (ShoppingListItem item : shoppingListItems) {
        if (item.getProductId() != null && !item.getProductId().isEmpty()) {
          shoppingListItemsProductIds.add(item.getProductId());
        }
      }
      filterChipLiveDataLocation.setLocations(data.getLocations());
      locationHashMap = ArrayUtil.getLocationsHashMap(data.getLocations());

      stockLocationsHashMap = new HashMap<>();
      for (StockLocation stockLocation : data.getStockCurrentLocations()) {
        HashMap<Integer, StockLocation> locationsForProductId = stockLocationsHashMap
            .get(stockLocation.getProductId());
        if (locationsForProductId == null) {
          locationsForProductId = new HashMap<>();
          stockLocationsHashMap.put(stockLocation.getProductId(), locationsForProductId);
        }
        locationsForProductId.put(stockLocation.getLocationId(), stockLocation);
      }

      filterChipLiveDataStatus
          .setDueSoonCount(itemsDueCount)
          .setOverdueCount(itemsOverdueCount)
          .setExpiredCount(itemsExpiredCount)
          .setBelowStockCount(itemsMissingCount)
          .setInStockCount(itemsInStockCount)
          .setOpenedCount(itemsOpenedCount)
          .emitCounts();
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
        ProductGroup.class,
        StockItem.class,
        Product.class,
        ProductBarcode.class,
        VolatileItem.class,
        ShoppingListItem.class,
        Location.class,
        ProductAveragePrice.class,
        ProductLastPurchased.class,
        StockLocation.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null);
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
    ArrayList<StockItem> filteredStockItems = new ArrayList<>();

    Product productSearch = null;
    ProductBarcode productBarcodeSearch = null;
    if (searchInput != null && !searchInput.isEmpty()) {
      Grocycode grocycode = GrocycodeUtil.getGrocycode(searchInput);
      if (grocycode != null && grocycode.isProduct()) {
        productSearch = productHashMap.get(grocycode.getObjectId());
      }
      if (productSearch == null) {
        productBarcodeSearch = productBarcodeHashMap.get(searchInput);
      }
    }

    for (StockItem item : this.stockItems) {
      if (item.getProduct() == null) {
        // invalidate products and stock items offline cache because products may have changed
        SharedPreferences.Editor editPrefs = sharedPrefs.edit();
        editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
        editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ITEMS, null);
        editPrefs.apply();
        continue;
      }

      if (item.getProduct().getHideOnStockOverviewBoolean()) {
        continue;
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        String productName = item.getProduct().getName().toLowerCase();
        searchContainsItem = productName.contains(searchInput);
        if (!searchContainsItem) {
          searchContainsItem = searchResultsFuzzy.contains(productName);
        }
      }
      if (!searchContainsItem && productSearch == null && productBarcodeSearch == null) {
        continue;
      }
      if (!searchContainsItem && productSearch == null
          && productBarcodeSearch.getProductIdInt() != item.getProductId()) {
        continue;
      }
      if (productSearch != null && productSearch.getId() != item.getProductId()) {
        continue;
      }

      int productGroupFilterId = filterChipLiveDataProductGroup.getSelectedId();
      if (productGroupFilterId != FilterChipLiveDataProductGroup.NO_FILTER
          && NumUtil.isStringInt(item.getProduct().getProductGroupId())
          && productGroupFilterId != Integer.parseInt(item.getProduct().getProductGroupId())
          || productGroupFilterId != FilterChipLiveDataProductGroup.NO_FILTER
          && !NumUtil.isStringInt(item.getProduct().getProductGroupId())
      ) {
        continue;
      }
      int locationFilterId = filterChipLiveDataLocation.getSelectedId();
      if (locationFilterId != FilterChipLiveDataLocation.NO_FILTER) {
        HashMap<Integer, StockLocation> stockLocationsForProductId
            = stockLocationsHashMap.get(item.getProductId());
        if (stockLocationsForProductId == null
            || !stockLocationsForProductId.containsKey(locationFilterId)
        ) {
          continue;
        }
      }

      MissingItem missingItem = productIdsMissingItems.get(item.getProductId());
      if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_ALL
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_DUE_SOON
          && item.isItemDue()
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_OVERDUE
          && item.isItemOverdue()
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_EXPIRED
          && item.isItemExpired()
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_BELOW_MIN
          && missingItem != null
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_IN_STOCK
          && (missingItem == null || missingItem.getIsPartlyInStockBoolean())
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataStockStatus.STATUS_OPENED
          && item.getAmountOpenedDouble() > 0
      ) {
        filteredStockItems.add(item);
      }
    }

    if (filteredStockItems.isEmpty()) {
      InfoFullscreen info;
      if (searchInput != null && !searchInput.isEmpty()) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
      } else if (filterChipLiveDataStatus.getStatus()
          != FilterChipLiveDataStockStatus.STATUS_ALL
          || filterChipLiveDataProductGroup.getSelectedId()
          != FilterChipLiveDataProductGroup.NO_FILTER
          || filterChipLiveDataLocation.getSelectedId()
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

    filteredStockItemsLive.setValue(filteredStockItems);
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

  public MutableLiveData<ArrayList<StockItem>> getFilteredStockItemsLive() {
    return filteredStockItemsLive;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();

    // Initialize suggestion list with max. capacity; growing is expensive.
    searchResultsFuzzy = new ArrayList<>(products.size());
    List<BoundExtractedResult<Product>> results = FuzzySearch.extractSorted(
        this.searchInput,
        products,
        item -> item.getName().toLowerCase(),
        70
    );
    for (BoundExtractedResult<Product> result : results) {
      searchResultsFuzzy.add(result.getString());
    }

    updateFilteredStockItems();
  }

  public ArrayList<Integer> getProductIdsMissingItems() {
    return new ArrayList<>(productIdsMissingItems.keySet());
  }

  public HashMap<Integer, ProductGroup> getProductGroupHashMap() {
    return productGroupHashMap;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, String> getProductAveragePriceHashMap() {
    return productAveragePriceHashMap;
  }

  public HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap() {
    return productLastPurchasedHashMap;
  }

  public ArrayList<String> getShoppingListItemsProductIds() {
    return shoppingListItemsProductIds;
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

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataProductGroup() {
    return () -> filterChipLiveDataProductGroup;
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

  public FilterChipLiveData.Listener getFilterChipLiveDataExtraField() {
    return () -> filterChipLiveDataExtraField;
  }

  public String getExtraField() {
    return filterChipLiveDataExtraField.getExtraField();
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

  public static class StockOverviewViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final StockOverviewFragmentArgs args;

    public StockOverviewViewModelFactory(
        Application application,
        StockOverviewFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new StockOverviewViewModel(application, args);
    }
  }
}
