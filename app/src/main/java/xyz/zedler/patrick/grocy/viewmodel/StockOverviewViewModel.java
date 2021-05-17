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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
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
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingle;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.repository.StockOverviewRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class StockOverviewViewModel extends BaseViewModel {

  private static final String TAG = ShoppingListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final StockOverviewRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<StockItem>> filteredStockItemsLive;
  private final MutableLiveData<ArrayList<ProductGroup>> productGroupsLive;
  private final MutableLiveData<ArrayList<Location>> locationsLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;

  private ArrayList<StockItem> stockItems;
  private ArrayList<Product> products;
  private ArrayList<ProductBarcode> productBarcodesTemp;
  private HashMap<String, ProductBarcode> productBarcodeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private ArrayList<ShoppingListItem> shoppingListItems;
  private ArrayList<String> shoppingListItemsProductIds;
  private ArrayList<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private ArrayList<StockItem> dueItemsTemp;
  private ArrayList<StockItem> overdueItemsTemp;
  private ArrayList<StockItem> expiredItemsTemp;
  private ArrayList<MissingItem> missingItemsTemp;
  private HashMap<Integer, StockItem> productIdsMissingStockItems;
  private HashMap<Integer, Location> locationHashMap;
  private ArrayList<StockLocation> stockCurrentLocationsTemp;
  private HashMap<Integer, HashMap<Integer, StockLocation>> stockLocationsHashMap;

  private DownloadHelper.Queue currentQueueLoading;
  private String searchInput;
  private String sortMode;
  private final HorizontalFilterBarSingle horizontalFilterBarSingle;
  private final HorizontalFilterBarMulti horizontalFilterBarMulti;
  private int itemsDueCount;
  private int itemsOverdueCount;
  private int itemsExpiredCount;
  private int itemsMissingCount;
  private int itemsInStockCount;
  private boolean sortAscending;
  private boolean torchOn = false;
  private final boolean debug;

  public StockOverviewViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new StockOverviewRepository(application);
    pluralUtil = new PluralUtil(application.getResources().getConfiguration().locale);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredStockItemsLive = new MutableLiveData<>();
    productGroupsLive = new MutableLiveData<>();
    locationsLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);

    horizontalFilterBarSingle = new HorizontalFilterBarSingle(
        this::updateFilteredStockItems,
        HorizontalFilterBarSingle.DUE_NEXT,
        HorizontalFilterBarSingle.OVERDUE,
        HorizontalFilterBarSingle.EXPIRED,
        HorizontalFilterBarSingle.MISSING,
        HorizontalFilterBarSingle.IN_STOCK
    );
    itemsDueCount = 0;
    itemsOverdueCount = 0;
    itemsExpiredCount = 0;
    itemsMissingCount = 0;
    itemsInStockCount = 0;
    horizontalFilterBarMulti = new HorizontalFilterBarMulti(
        this::updateFilteredStockItems
    );
    sortMode = sharedPrefs.getString(Constants.PREF.STOCK_SORT_MODE, Constants.STOCK.SORT.NAME);
    sortAscending = sharedPrefs.getBoolean(Constants.PREF.STOCK_SORT_ASCENDING, true);
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(
        (quantityUnits, productGroups, stockItems, products, barcodes, shoppingListItems, locations, stockLocations) -> {
          this.quantityUnits = quantityUnits;
          quantityUnitHashMap = new HashMap<>();
          for (QuantityUnit quantityUnit : quantityUnits) {
            quantityUnitHashMap.put(quantityUnit.getId(), quantityUnit);
          }
          this.productGroupsLive.setValue(productGroups);
          this.products = products;
          productHashMap = new HashMap<>();
          for (Product product : products) {
            productHashMap.put(product.getId(), product);
          }
          this.productBarcodesTemp = barcodes;
          productBarcodeHashMap = new HashMap<>();
          for (ProductBarcode barcode : barcodes) {
            productBarcodeHashMap.put(barcode.getBarcode(), barcode);
          }
          itemsDueCount = 0;
          itemsOverdueCount = 0;
          itemsExpiredCount = 0;
          itemsMissingCount = 0;
          itemsInStockCount = 0;
          productIdsMissingStockItems = new HashMap<>();
          this.stockItems = stockItems;
          for (StockItem stockItem : stockItems) {
            stockItem.setProduct(productHashMap.get(stockItem.getProductId()));
            if (stockItem.isItemDue()) {
              itemsDueCount++;
            }
            if (stockItem.isItemOverdue()) {
              itemsOverdueCount++;
            }
            if (stockItem.isItemExpired()) {
              itemsExpiredCount++;
            }
            if (stockItem.isItemMissing()) {
              itemsMissingCount++;
              productIdsMissingStockItems.put(stockItem.getProductId(), stockItem);
            }
            if (!stockItem.isItemMissing() || stockItem.isItemMissingAndPartlyInStock()) {
              itemsInStockCount++;
            }
          }

          this.shoppingListItems = shoppingListItems;
          shoppingListItemsProductIds = new ArrayList<>();
          for (ShoppingListItem item : shoppingListItems) {
            if (item.getProductId() != null && !item.getProductId().isEmpty()) {
              shoppingListItemsProductIds.add(item.getProductId());
            }
          }
          this.locationsLive.setValue(locations);
          locationHashMap = new HashMap<>();
          for (Location location : locations) {
            locationHashMap.put(location.getId(), location);
          }

          this.stockCurrentLocationsTemp = stockLocations;
          stockLocationsHashMap = new HashMap<>();
          for (StockLocation stockLocation : stockLocations) {
            HashMap<Integer, StockLocation> locationsForProductId = stockLocationsHashMap
                .get(stockLocation.getProductId());
            if (locationsForProductId == null) {
              locationsForProductId = new HashMap<>();
              stockLocationsHashMap.put(stockLocation.getProductId(), locationsForProductId);
            }
            locationsForProductId.put(stockLocation.getLocationId(), stockLocation);
          }

          updateFilteredStockItems();
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredStockItems();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.OnQueueEmptyListener onQueueEmptyListener = () -> {
      if (dueItemsTemp == null || overdueItemsTemp == null
          || expiredItemsTemp == null || missingItemsTemp == null) {
        downloadDataForceUpdate();
        return;
      }

      HashMap<Integer, StockItem> stockItemHashMap = new HashMap<>();
      for (StockItem stockItem : stockItems) {
        stockItemHashMap.put(stockItem.getProductId(), stockItem);
      }

      for (StockItem stockItemDue : dueItemsTemp) {
        StockItem stockItem = stockItemHashMap.get(stockItemDue.getProductId());
        if (stockItem == null) {
          continue;
        }
        stockItem.setItemDue(true);
      }
      for (StockItem stockItemOverdue : overdueItemsTemp) {
        StockItem stockItem = stockItemHashMap.get(stockItemOverdue.getProductId());
        if (stockItem == null) {
          continue;
        }
        stockItem.setItemOverdue(true);
      }
      for (StockItem stockItemExpired : expiredItemsTemp) {
        StockItem stockItem = stockItemHashMap.get(stockItemExpired.getProductId());
        if (stockItem == null) {
          continue;
        }
        stockItem.setItemExpired(true);
      }

      DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);

      productIdsMissingStockItems = new HashMap<>();
      for (MissingItem missingItem : missingItemsTemp) {

        StockItem missingStockItem = stockItemHashMap.get(missingItem.getId());
        if (missingStockItem != null) {
          productIdsMissingStockItems.put(missingItem.getId(), missingStockItem);
        }
        if (missingStockItem != null && !missingStockItem.isItemMissing()) {
          missingStockItem.setItemMissing(true);
          missingStockItem.setItemMissingAndPartlyInStock(true);
          continue;
        } else if (missingStockItem != null) {
          continue;
        }
        queue.append(dlHelper.getProductDetails(missingItem.getId(), productDetails -> {
          StockItem stockItem = new StockItem(productDetails);
          stockItem.setItemMissing(true);
          stockItem.setItemMissingAndPartlyInStock(false);
          productIdsMissingStockItems.put(missingItem.getId(), stockItem);
          stockItems.add(stockItem);
        }));
      }
      if (queue.getSize() == 0) {
        onQueueEmpty();
        return;
      }
      queue.start();
    };

    sharedPrefs.edit().putString(Constants.PREF.DB_LAST_TIME_VOLATILE, null).apply();

    DownloadHelper.Queue queue = dlHelper.newQueue(onQueueEmptyListener, this::onDownloadError);
    queue.append(
        dlHelper.updateQuantityUnits(dbChangedTime, quantityUnits -> {
          this.quantityUnits = quantityUnits;
          quantityUnitHashMap = new HashMap<>();
          for (QuantityUnit quantityUnit : quantityUnits) {
            quantityUnitHashMap.put(quantityUnit.getId(), quantityUnit);
          }
        }), dlHelper.updateProductGroups(dbChangedTime, this.productGroupsLive::setValue),
        dlHelper.updateStockItems(dbChangedTime, stockItems -> {
          this.stockItems = stockItems;
          itemsInStockCount = stockItems.size();
        }), dlHelper.updateProducts(dbChangedTime, products -> {
          this.products = products;
          productHashMap = new HashMap<>();
          for (Product product : products) {
            productHashMap.put(product.getId(), product);
          }
        }), dlHelper.updateProductBarcodes(dbChangedTime, productBarcodes -> {
          this.productBarcodesTemp = productBarcodes;
          productBarcodeHashMap = new HashMap<>();
          for (ProductBarcode barcode : productBarcodes) {
            productBarcodeHashMap.put(barcode.getBarcode(), barcode);
          }
        }), dlHelper.updateVolatile(dbChangedTime, (due, overdue, expired, missing) -> {
          this.dueItemsTemp = due;
          itemsDueCount = due.size();
          this.overdueItemsTemp = overdue;
          itemsOverdueCount = overdue.size();
          this.expiredItemsTemp = expired;
          itemsExpiredCount = expired.size();
          this.missingItemsTemp = missing;
          itemsMissingCount = missing.size();
        }), dlHelper.updateShoppingListItems(dbChangedTime, shoppingListItems -> {
          this.shoppingListItems = shoppingListItems;
          shoppingListItemsProductIds = new ArrayList<>();
          for (ShoppingListItem item : shoppingListItems) {
            if (item.getProductId() != null && !item.getProductId().isEmpty()) {
              shoppingListItemsProductIds.add(item.getProductId());
            }
          }
        }), dlHelper.updateLocations(dbChangedTime, locations -> {
          this.locationsLive.setValue(locations);
          locationHashMap = new HashMap<>();
          for (Location location : locations) {
            locationHashMap.put(location.getId(), location);
          }
        }),
        dlHelper.updateStockCurrentLocations(dbChangedTime, stockLocations -> {
          this.stockCurrentLocationsTemp = stockLocations;

          stockLocationsHashMap = new HashMap<>();
          for (StockLocation stockLocation : stockLocations) {
            HashMap<Integer, StockLocation> locationsForProductId = stockLocationsHashMap
                .get(stockLocation.getProductId());
            if (locationsForProductId == null) {
              locationsForProductId = new HashMap<>();
              stockLocationsHashMap.put(stockLocation.getProductId(), locationsForProductId);
            }
            locationsForProductId.put(stockLocation.getLocationId(), stockLocation);
          }
        })
    );

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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    repository.updateDatabase(
        this.quantityUnits,
        this.productGroupsLive.getValue(),
        this.stockItems,
        this.products,
        this.productBarcodesTemp,
        this.shoppingListItems,
        this.locationsLive.getValue(),
        this.stockCurrentLocationsTemp,
        this::updateFilteredStockItems
    );
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

    ProductBarcode productBarcodeSearch = null;
    if (searchInput != null && !searchInput.isEmpty()) {
      productBarcodeSearch = productBarcodeHashMap.get(searchInput);
    }

    for (StockItem item : this.stockItems) {
      if (item.getProduct().getHideOnStockOverviewInt() == 1) {
        continue;
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = item.getProduct().getName().toLowerCase().contains(searchInput);
      }
      if (!searchContainsItem && productBarcodeSearch == null
          || !searchContainsItem && productBarcodeSearch.getProductId() != item.getProductId()) {
        continue;
      }

      if (horizontalFilterBarMulti.areFiltersActive()) {
        HorizontalFilterBarMulti.Filter productGroup = horizontalFilterBarMulti
            .getFilter(HorizontalFilterBarMulti.PRODUCT_GROUP);
        HorizontalFilterBarMulti.Filter location = horizontalFilterBarMulti
            .getFilter(HorizontalFilterBarMulti.LOCATION);
        if (productGroup != null && NumUtil.isStringInt(item.getProduct().getProductGroupId())
            && productGroup.getObjectId() != Integer
            .parseInt(item.getProduct().getProductGroupId())) {
          continue;
        }
        if (location != null) {
          HashMap<Integer, StockLocation> stockLocationsForProductId
              = stockLocationsHashMap.get(item.getProductId());
          if (stockLocationsForProductId == null
              || !stockLocationsForProductId.containsKey(location.getObjectId())
          ) {
            continue;
          }
        }
      }

      if (horizontalFilterBarSingle.isNoFilterActive()
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.DUE_NEXT)
          && item.isItemDue()
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.OVERDUE)
          && item.isItemOverdue()
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.EXPIRED)
          && item.isItemExpired()
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.MISSING)
          && productIdsMissingStockItems.containsKey(item.getProductId())
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.IN_STOCK)
          && (!productIdsMissingStockItems.containsKey(item.getProductId())
          || productIdsMissingStockItems.get(item.getProductId()).isItemMissingAndPartlyInStock())
      ) {
        filteredStockItems.add(item);
      }
    }

    switch (sortMode) {
      case Constants.STOCK.SORT.NAME:
        SortUtil.sortStockItemsByName(getApplication(), filteredStockItems, sortAscending);
        break;
      case Constants.STOCK.SORT.BBD:
        SortUtil.sortStockItemsByBBD(filteredStockItems, sortAscending);
        break;
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
                error -> showErrorMessage()
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
          showErrorMessage();
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
                error -> showErrorMessage()
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
          showErrorMessage();
          if (debug) {
            Log.i(TAG, "openProduct: " + error);
          }
        }
    );
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
  }

  public MutableLiveData<ArrayList<StockItem>> getFilteredStockItemsLive() {
    return filteredStockItemsLive;
  }

  public int getItemsDueCount() {
    return itemsDueCount;
  }

  public int getItemsOverdueCount() {
    return itemsOverdueCount;
  }

  public int getItemsExpiredCount() {
    return itemsExpiredCount;
  }

  public int getItemsMissingCount() {
    return itemsMissingCount;
  }

  public int getItemsInStockCount() {
    return itemsInStockCount;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredStockItems();
  }

  public void toggleDoneStatus(ShoppingListItem listItem) {
    if (listItem == null) {
      showErrorMessage();
      return;
    }
    ShoppingListItem shoppingListItem = listItem.getClone();

    if (shoppingListItem.getDoneSynced() == -1) {
      shoppingListItem.setDoneSynced(shoppingListItem.getDoneInt());
    }

    shoppingListItem.setDone(shoppingListItem.getDoneInt() == 0 ? 1 : 0);  // toggle state

    if (isOffline()) {
      updateDoneStatus(shoppingListItem);
      return;
    }

    JSONObject body = new JSONObject();
    try {
      body.put("done", shoppingListItem.getDoneInt());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "toggleDoneStatus: " + e);
      }
    }
    dlHelper.editShoppingListItem(
        shoppingListItem.getId(),
        body,
        response -> updateDoneStatus(shoppingListItem),
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(TAG, "toggleDoneStatus: " + error);
          }
        }
    ).perform(dlHelper.getUuid());
  }

  private void updateDoneStatus(ShoppingListItem shoppingListItem) {

  }

  public void addMissingItems() {
        /*ShoppingList shoppingList = getSelectedShoppingList();
        if(shoppingList == null) {
            showMessage(getString(R.string.error_undefined));
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("list_id", getSelectedShoppingListId());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "setUpBottomMenu: add missing: " + e);
        }
        dlHelper.post(
                grocyApi.addMissingProducts(),
                jsonObject,
                response -> {
                    showMessage(getApplication().getString(
                            R.string.msg_added_missing_products,
                            shoppingList.getName()
                    ));
                    downloadData();
                },
                error -> {
                    showMessage(getString(R.string.error_undefined));
                    if(debug) Log.e(
                            TAG, "setUpBottomMenu: add missing "
                                    + shoppingList.getName()
                                    + ": " + error
                    );
                }
        );*/
  }

  public ArrayList<Integer> getProductIdsMissingStockItems() {
    return new ArrayList<>(productIdsMissingStockItems.keySet());
  }

  public ArrayList<String> getShoppingListItemsProductIds() {
    return shoppingListItemsProductIds;
  }

  public HorizontalFilterBarSingle getHorizontalFilterBarSingle() {
    return horizontalFilterBarSingle;
  }

  public HorizontalFilterBarMulti getHorizontalFilterBarMulti() {
    return horizontalFilterBarMulti;
  }

  public String getSortMode() {
    return sortMode;
  }

  public void setSortMode(String sortMode) {
    this.sortMode = sortMode;
    sharedPrefs.edit().putString(Constants.PREF.STOCK_SORT_MODE, sortMode).apply();
    updateFilteredStockItems();
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  public void setSortAscending(boolean sortAscending) {
    this.sortAscending = sortAscending;
    sharedPrefs.edit().putBoolean(Constants.PREF.STOCK_SORT_ASCENDING, sortAscending).apply();
    updateFilteredStockItems();
  }

  public MutableLiveData<ArrayList<ProductGroup>> getProductGroupsLive() {
    return productGroupsLive;
  }

  public MutableLiveData<ArrayList<Location>> getLocationsLive() {
    return locationsLive;
  }

  public Location getLocationFromId(int id) {
    return locationHashMap.get(id);
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public QuantityUnit getQuantityUnitFromId(int id) {
    return quantityUnitHashMap.get(id);
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

  public boolean isTorchOn() {
    return torchOn;
  }

  public void setTorchOn(boolean torchOn) {
    this.torchOn = torchOn;
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

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public boolean getUseFrontCam() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.FRONT_CAM,
        Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
    );
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
