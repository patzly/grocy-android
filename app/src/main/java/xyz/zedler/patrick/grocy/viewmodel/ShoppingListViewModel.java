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
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingListGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingListStatus;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class ShoppingListViewModel extends BaseViewModel {

  private static final String TAG = ShoppingListViewModel.class.getSimpleName();
  private static final int DEFAULT_SHOPPING_LIST_ID = 1;

  public final static String FIELD_AMOUNT = "field_amount";
  public final static String FIELD_PRICE_LAST_UNIT = "field_price_last_unit";
  public final static String FIELD_PRICE_LAST_TOTAL = "field_price_last_total";
  public final static String FIELD_NOTES = "field_notes";
  public final static String FIELD_PICTURE = "field_picture";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Integer> selectedShoppingListIdLive;
  private final MutableLiveData<ArrayList<ShoppingListItem>> filteredShoppingListItemsLive;
  private final FilterChipLiveDataShoppingListStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataShoppingListGrouping filterChipLiveDataGrouping;
  private final FilterChipLiveDataFields filterChipLiveDataFields;

  private List<ShoppingListItem> shoppingListItems;
  private List<ShoppingList> shoppingLists;
  private HashMap<Integer, ProductGroup> productGroupHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private List<QuantityUnitConversionResolved> unitConversions;
  private HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, String> productNamesHashMap;
  private HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private HashMap<Integer, Store> storeHashMap;
  private ArrayList<Integer> missingProductIds;

  private ArrayList<ShoppingListItem> itemsToSyncTemp;
  private HashMap<Integer, ShoppingListItem> serverItemHashMapTemp;

  private NetworkQueue currentQueueLoading;
  private String searchInput;
  private final boolean debug;
  private final int maxDecimalPlacesAmount;


  public ShoppingListViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, null);
    grocyApi = new GrocyApi(getApplication());
    repository = new ShoppingListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    selectedShoppingListIdLive = new MutableLiveData<>(1);
    filteredShoppingListItemsLive = new MutableLiveData<>();
    filterChipLiveDataStatus = new FilterChipLiveDataShoppingListStatus(
        getApplication(),
        this::updateFilteredShoppingListItems
    );
    filterChipLiveDataGrouping = new FilterChipLiveDataShoppingListGrouping(
        getApplication(),
        this::updateFilteredShoppingListItems
    );
    filterChipLiveDataFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.SHOPPING_MODE_FIELDS,
        this::updateFilteredShoppingListItems,
        new Field(FIELD_AMOUNT, R.string.property_amount, true),
        new Field(FIELD_PRICE_LAST_TOTAL, R.string.property_last_price_total, false),
        new Field(FIELD_PRICE_LAST_UNIT, R.string.property_last_price_unit, false),
        new Field(FIELD_NOTES, R.string.property_notes, true),
        new Field(FIELD_PICTURE, R.string.property_picture, false)
    );

    int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
    if (lastId != DEFAULT_SHOPPING_LIST_ID
        && !isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
      sharedPrefs.edit()
          .putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, DEFAULT_SHOPPING_LIST_ID)
          .apply();
      lastId = DEFAULT_SHOPPING_LIST_ID;
    }
    selectedShoppingListIdLive.setValue(lastId);
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.shoppingListItems = data.getShoppingListItems();
      this.shoppingLists = data.getShoppingLists();
      productGroupHashMap = ArrayUtil.getProductGroupsHashMap(data.getProductGroups());
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      unitConversions = data.getUnitConversionsResolved();
      storeHashMap = ArrayUtil.getStoresHashMap(data.getStores());
      missingProductIds = ArrayUtil.getMissingProductsIds(data.getMissingItems());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productNamesHashMap = ArrayUtil.getProductNamesHashMap(data.getProducts());
      productLastPurchasedHashMap = ArrayUtil
          .getProductLastPurchasedHashMap(data.getProductsLastPurchased());
      fillShoppingListItemAmountsHashMap();
      updateFilteredShoppingListItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void updateFilteredShoppingListItems() {
    if (this.shoppingListItems == null) {
      return;
    }

    ArrayList<ShoppingListItem> filteredShoppingListItems = new ArrayList<>();
    int itemsMissingCount = 0;
    int itemsUndoneCount = 0;
    int itemsDoneCount = 0;

    for (ShoppingListItem item : this.shoppingListItems) {
      if (item.getShoppingListIdInt() != getSelectedShoppingListId()) {
        continue;
      }
      if (item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {
        itemsMissingCount++;
      }
      if (item.isUndone()) {
        itemsUndoneCount++;
      } else {
        itemsDoneCount++;
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        String name;
        if (item.hasProduct()) {
          Product product = productHashMap.get(item.getProductIdInt());
          name = product != null ? product.getName() : null;
        } else {
          name = item.getNote();
        }
        name = name != null ? name.toLowerCase() : "";
        searchContainsItem = name.contains(searchInput);
      }
      if (!searchContainsItem) {
        continue;
      }

      if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataShoppingListStatus.STATUS_ALL
          || filterChipLiveDataStatus.getStatus()
          == FilterChipLiveDataShoppingListStatus.STATUS_BELOW_MIN
          && item.hasProduct() && missingProductIds.contains(item.getProductIdInt())
          || filterChipLiveDataStatus.getStatus()
          == FilterChipLiveDataShoppingListStatus.STATUS_UNDONE && item.isUndone()
          || filterChipLiveDataStatus.getStatus()
          == FilterChipLiveDataShoppingListStatus.STATUS_DONE && !item.isUndone()
      ) {
        filteredShoppingListItems.add(item);
      }
    }
    filterChipLiveDataStatus
        .setBelowStockCount(itemsMissingCount)
        .setUndoneCount(itemsUndoneCount)
        .setDoneCount(itemsDoneCount)
        .emitCounts();

    filteredShoppingListItemsLive.setValue(filteredShoppingListItems);
    selectedShoppingListIdLive.setValue(selectedShoppingListIdLive.getValue());

    if (filteredShoppingListItems.isEmpty()) {
      InfoFullscreen info;
      if (searchInput != null && !searchInput.isEmpty()) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
      } else if (filterChipLiveDataStatus.getStatus()
          != FilterChipLiveDataShoppingListStatus.STATUS_ALL) {
        info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
      } else {
        info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_SHOPPING_LIST);
      }
      infoFullscreenLive.setValue(info);
    } else {
      infoFullscreenLive.setValue(null);
    }
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<ShoppingListItem>> getFilteredShoppingListItemsLive() {
    return filteredShoppingListItemsLive;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredShoppingListItems();
  }

  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return selectedShoppingListIdLive;
  }

  public void downloadData(@Nullable String dbChangedTime, boolean skipOfflineCheck) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (!skipOfflineCheck && isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredShoppingListItems();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(
          time -> downloadData(time, skipOfflineCheck),
          error -> onError(error, TAG)
      );
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(updated -> onQueueEmpty(), error -> onError(error, TAG));
    queue.append(
        ShoppingListItem.updateShoppingListItems(
            dlHelper,
            dbChangedTime,
            (shoppingListItems, itemsToSync, serverItemsHashMap) -> {
              this.shoppingListItems = shoppingListItems;
              this.itemsToSyncTemp = itemsToSync;
              this.serverItemHashMapTemp = serverItemsHashMap;
            }
        ), ShoppingList.updateShoppingLists(
            dlHelper, dbChangedTime, false, shoppingLists -> this.shoppingLists = shoppingLists
        ), ProductGroup.updateProductGroups(
            dlHelper,
            dbChangedTime, false,
            productGroups -> productGroupHashMap = ArrayUtil.getProductGroupsHashMap(productGroups)
        ), QuantityUnit.updateQuantityUnits(
            dlHelper,
            dbChangedTime, false,
            quantityUnits -> quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits)
        ), Product.updateProducts(dlHelper, dbChangedTime, products -> {
          productHashMap = ArrayUtil.getProductsHashMap(products);
          productNamesHashMap = ArrayUtil.getProductNamesHashMap(products);
          queue.append(QuantityUnitConversionResolved.updateQuantityUnitConversions(
              dlHelper,
              dbChangedTime, false,
              products,
              unitConversions -> this.unitConversions = unitConversions
          ));
        }), ProductLastPurchased.updateProductsLastPurchased(
            dlHelper,
            dbChangedTime, false,
            productsLastPurchased -> productLastPurchasedHashMap = ArrayUtil
            .getProductLastPurchasedHashMap(productsLastPurchased),
            true
        ), Store.updateStores(
            dlHelper,
            dbChangedTime, false,
            stores -> storeHashMap = ArrayUtil.getStoresHashMap(stores)
        ), MissingItem.updateMissingItems(
            dlHelper,
            dbChangedTime, false,
            missing -> missingProductIds = ArrayUtil.getMissingProductsIds(missing)
        )
    );

    if (queue.isEmpty()) {
      onQueueEmpty();
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null, false);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS_RESOLVED, null);
    editPrefs.putString(PREF.DB_LAST_TIME_VOLATILE_MISSING, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STORES, null);
    editPrefs.apply();
    downloadData(null, true);
  }

  private void onQueueEmpty() {
    if (isOffline()) setOfflineLive(false);

    if (itemsToSyncTemp == null || itemsToSyncTemp.isEmpty() || serverItemHashMapTemp == null) {
      tidyUpItems(itemsChanged -> {
        if (itemsChanged) {
          downloadData();
        } else {
          fillShoppingListItemAmountsHashMap();
          updateFilteredShoppingListItems();
        }
      });
      return;
    }
    Runnable emptyListener = () -> {
      ArrayList<ShoppingListItem> itemsToUpdate = new ArrayList<>();
      for (ShoppingListItem itemToSync : itemsToSyncTemp) {
        int itemId = itemToSync.getId();
        ShoppingListItem itemToUpdate = serverItemHashMapTemp.get(itemId);
        if (itemToUpdate == null) {
          continue;
        }
        itemToUpdate.setDone(itemToSync.getDoneInt());
        itemsToUpdate.add(itemToUpdate);
      }
      repository.insertShoppingListItems(
          () -> {
            itemsToSyncTemp = null;
            serverItemHashMapTemp = null;
            showMessage(getString(R.string.msg_synced));
            loadFromDatabase(true);
          },
          itemsToUpdate.toArray(new ShoppingListItem[0])
      );
    };
    DownloadHelper.OnMultiTypeErrorListener errorListener = error -> {
      showMessage(getString(R.string.msg_failed_to_sync));
      downloadData();
    };
    NetworkQueue queue = dlHelper.newQueue(updated -> emptyListener.run(), errorListener);
    for (ShoppingListItem itemToSync : itemsToSyncTemp) {
      JSONObject body = new JSONObject();
      try {
        body.put("done", itemToSync.getDoneInt());
      } catch (JSONException e) {
        if (debug) {
          Log.e(TAG, "syncItems: " + e);
        }
      }
      queue.append(ShoppingListItem.editShoppingListItem(dlHelper, itemToSync.getId(), body));
    }
    currentQueueLoading = queue;
    queue.start();
  }

  private void tidyUpItems(OnTidyUpFinishedListener onFinished) {
    // Tidy up lost shopping list items, which have deleted shopping lists
    // as an id – else they will never show up on any shopping list
    ArrayList<Integer> listIds = new ArrayList<>();
    if (isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
      for (ShoppingList shoppingList : shoppingLists) {
        listIds.add(shoppingList.getId());
      }
      if (listIds.isEmpty()) {
        if (onFinished != null) {
          onFinished.run(false);
        }
        return;  // possible if download error happened
      }
    } else {
      listIds.add(1);  // id of first and single shopping list
    }

    NetworkQueue queue = dlHelper.newQueue(
        updated -> {
          if (onFinished != null) {
            onFinished.run(true);
          }
        },
        error -> {
          if (onFinished != null) {
            onFinished.run(true);
          }
        }
    );
    for (ShoppingListItem listItem : shoppingListItems) {
      if (listIds.contains(listItem.getShoppingListIdInt())) {
        continue;
      }
      if (debug) {
        Log.i(TAG, "tidyUpItems: " + listItem);
      }
      queue.append(ShoppingListItem.deleteShoppingListItem(dlHelper, listItem.getId()));
    }
    if (queue.getSize() == 0) {
      onFinished.run(false);
      return;
    }
    currentQueueLoading = queue;
    queue.start();
  }

  private interface OnTidyUpFinishedListener {

    void run(boolean itemsChanged);
  }

  public int getSelectedShoppingListId() {
    if (selectedShoppingListIdLive.getValue() == null) {
      return -1;
    }
    return selectedShoppingListIdLive.getValue();
  }

  public void selectShoppingList(int shoppingListId) {
    if (shoppingListId == getSelectedShoppingListId()) {
      return;
    }
    sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, shoppingListId).apply();
    selectedShoppingListIdLive.setValue(shoppingListId);
    updateFilteredShoppingListItems();
  }

  public void selectShoppingList(ShoppingList shoppingList) {
    selectShoppingList(shoppingList.getId());
  }

  public void toggleDoneStatus(ShoppingListItem listItem) {
    if (listItem == null) {
      showNetworkErrorMessage(null);
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
    ShoppingListItem.editShoppingListItem(
        dlHelper,
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
    repository.insertShoppingListItems(
        () -> loadFromDatabase(false),
        shoppingListItem
    );
  }

  public void addMissingItems() {
    ShoppingList shoppingList = getSelectedShoppingList();
    if (shoppingList == null) {
      showMessage(getString(R.string.error_undefined));
      return;
    }
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("list_id", getSelectedShoppingListId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "setUpBottomMenu: add missing: " + e);
      }
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
          if (debug) {
            Log.e(
                TAG, "setUpBottomMenu: add missing "
                    + shoppingList.getName()
                    + ": " + error
            );
          }
        }
    );
  }

  public void saveNotes(Spanned notes) {
    JSONObject body = new JSONObject();

    String notesHtml = notes != null ? Html.toHtml(notes) : "";
    try {
      body.put("description", notesHtml);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveNotes: " + e);
      }
    }
    dlHelper.put(
        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, getSelectedShoppingListId()),
        body,
        response -> {
          ShoppingList shoppingList = getSelectedShoppingList();
          if (shoppingList == null) {
            return;
          }
          shoppingList.setNotes(notesHtml);
          downloadData();
        },
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(TAG, "saveNotes: " + error);
          }
          downloadData();
        }
    );
  }

  public void deleteItem(@NonNull ShoppingListItem shoppingListItem) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
        response -> loadFromDatabase(true),
        error -> {
          showMessage(getString(R.string.error_undefined));
          loadFromDatabase(true);
          if (debug) {
            Log.e(TAG, "deleteItem: " + error);
          }
        }
    );
  }

  public void safeDeleteShoppingList(ShoppingList shoppingList) {
    if (shoppingList == null) {
      showMessage(getString(R.string.error_undefined));
      return;
    }
    clearAllItems(
        shoppingList,
        () -> deleteShoppingList(shoppingList)
    );
  }

  public void deleteShoppingList(ShoppingList shoppingList) {
    int selectedShoppingListId = getSelectedShoppingListId();

    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("list_id", getSelectedShoppingListId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "deleteShoppingList: delete list: " + e);
      }
    }
    dlHelper.delete(
        grocyApi.getObject(
            GrocyApi.ENTITY.SHOPPING_LISTS,
            shoppingList.getId()
        ),
        response -> {
          showMessage(
              getApplication().getString(
                  R.string.msg_shopping_list_deleted,
                  shoppingList.getName()
              )
          );
          shoppingLists.remove(shoppingList);
          if (selectedShoppingListId == shoppingList.getId()) {
            selectShoppingList(1);
          }
          tidyUpItems(itemsChanged -> downloadData());
        },
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(
                TAG, "deleteShoppingList: delete "
                    + shoppingList.getName() + ": " + error
            );
          }
          downloadData();
        }
    );
  }

  public void clearAllItems(ShoppingList shoppingList, Runnable onSuccess) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("list_id", shoppingList.getId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "clearShoppingList: " + e);
      }
    }
    dlHelper.post(
        grocyApi.clearShoppingList(),
        jsonObject,
        response -> {
          if (onSuccess != null) {
            onSuccess.run();
            return;
          }
          showMessage(getApplication().getString(
              R.string.msg_shopping_list_cleared,
              shoppingList.getName()
          ));
          downloadData();
        },
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(
                TAG, "clearShoppingList: "
                    + shoppingList.getName()
                    + ": " + error
            );
          }
          downloadData();
        }
    );
  }

  public void clearDoneItems(ShoppingList shoppingList) {
    NetworkQueue queue = dlHelper.newQueue(
        updated -> {
          showMessage(getApplication().getString(
              R.string.msg_shopping_list_cleared,
              shoppingList.getName()
          ));
          downloadData();
        }, volleyError -> {
          showMessage(getString(R.string.error_undefined));
          downloadData();
        }
    );
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      if (shoppingListItem.getShoppingListIdInt() != shoppingList.getId()) {
        continue;
      }
      if (shoppingListItem.getDoneInt() == 0) {
        continue;
      }
      queue.append(ShoppingListItem.deleteShoppingListItem(dlHelper, shoppingListItem.getId()));
    }
    queue.start();
  }

  @Nullable
  public ShoppingList getShoppingListFromId(int id) {
    if (shoppingLists == null) {
      return null;
    }
    for (ShoppingList temp : shoppingLists) {
      if (temp.getId() == id) {
        return temp;
      }
    }
    return null;
  }

  @Nullable
  public ShoppingList getSelectedShoppingList() {
    return getShoppingListFromId(getSelectedShoppingListId());
  }

  public String getShoppingListNotes() {
    for (ShoppingList s : shoppingLists) {
      if (s.getId() == getSelectedShoppingListId()) {
        return s.getNotes();
      }
    }
    return null;
  }

  public HashMap<Integer, String> getProductNamesHashMap() {
    return productNamesHashMap;
  }

  public HashMap<Integer, ProductGroup> getProductGroupHashMap() {
    return productGroupHashMap;
  }

  public HashMap<Integer, Store> getStoreHashMap() {
    return storeHashMap;
  }

  @Nullable
  public List<ShoppingList> getShoppingLists() {
    return shoppingLists;
  }

  public ArrayList<Integer> getMissingProductIds() {
    return missingProductIds;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap() {
    return productLastPurchasedHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataGrouping() {
    return () -> filterChipLiveDataGrouping;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataFields() {
    return () -> filterChipLiveDataFields;
  }

  public String getGroupingMode() {
    return filterChipLiveDataGrouping.getGroupingMode();
  }

  public List<String> getActiveFields() {
    return filterChipLiveDataFields.getActiveFields();
  }

  private void fillShoppingListItemAmountsHashMap() {
    shoppingListItemAmountsHashMap = new HashMap<>();
    for (ShoppingListItem item : shoppingListItems) {
      Double amount = AmountUtil.getShoppingListItemAmount(
          item, productHashMap, quantityUnitHashMap, unitConversions
      );
      if (amount != null) {
        shoppingListItemAmountsHashMap.put(item.getId(), amount);
      }
    }
  }

  public HashMap<Integer, Double> getShoppingListItemAmountsHashMap() {
    return shoppingListItemAmountsHashMap;
  }

  public int getMaxDecimalPlacesAmount() {
    return maxDecimalPlacesAmount;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
