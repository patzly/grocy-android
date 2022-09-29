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
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingListGrouping;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class ShoppingModeViewModel extends BaseViewModel {

  private static final String TAG = ShoppingModeViewModel.class.getSimpleName();
  private static final int DEFAULT_SHOPPING_LIST_ID = 1;

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Integer> selectedShoppingListIdLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<ShoppingListItem>> filteredShoppingListItemsLive;

  private List<ShoppingListItem> shoppingListItems;
  private List<ShoppingList> shoppingLists;
  private HashMap<Integer, ProductGroup> productGroupHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, ArrayList<QuantityUnitConversion>> unitConversionHashMap;
  private HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private HashMap<Integer, Store> storeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, String> productNamesHashMap;
  private ArrayList<Integer> missingProductIds;

  private ArrayList<ShoppingListItem> itemsToSyncTemp;
  private HashMap<Integer, ShoppingListItem> serverItemHashMapTemp;

  private NetworkQueue currentQueueLoading;
  private final boolean debug;

  public ShoppingModeViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new ShoppingListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    selectedShoppingListIdLive = new MutableLiveData<>(1);
    filteredShoppingListItemsLive = new MutableLiveData<>();

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
      unitConversionHashMap = ArrayUtil.getUnitConversionsHashMap(data.getUnitConversions());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productNamesHashMap = ArrayUtil.getProductNamesHashMap(data.getProducts());
      storeHashMap = ArrayUtil.getStoresHashMap(data.getStores());
      missingProductIds = ArrayUtil.getMissingProductsIds(data.getMissingItems());
      fillShoppingListItemAmountsHashMap();
      updateFilteredShoppingListItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void updateFilteredShoppingListItems() {
    if (this.shoppingListItems == null) {
      return;
    }
    ArrayList<ShoppingListItem> filteredShoppingListItems = new ArrayList<>();
    for (ShoppingListItem item : this.shoppingListItems) {
      if (item.getShoppingListIdInt() != getSelectedShoppingListId()) {
        continue;
      }
      filteredShoppingListItems.add(item);
    }
    filteredShoppingListItemsLive.setValue(filteredShoppingListItems);
    selectedShoppingListIdLive.setValue(selectedShoppingListIdLive.getValue());

    if (filteredShoppingListItems.isEmpty()) {
      infoFullscreenLive.setValue(new InfoFullscreen(InfoFullscreen.INFO_EMPTY_SHOPPING_LIST));
    } else {
      infoFullscreenLive.setValue(null);
    }
  }

  public MutableLiveData<ArrayList<ShoppingListItem>> getFilteredShoppingListItemsLive() {
    return filteredShoppingListItemsLive;
  }

  public MutableLiveData<Integer> getSelectedShoppingListIdLive() {
    return selectedShoppingListIdLive;
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredShoppingListItems();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(
          this::downloadData,
          () -> onDownloadError(null)
      );
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateShoppingListItems(
            dbChangedTime,
            (shoppingListItems, itemsToSync, serverItemsHashMap) -> {
              this.shoppingListItems = shoppingListItems;
              this.itemsToSyncTemp = itemsToSync;
              this.serverItemHashMapTemp = serverItemsHashMap;
            }
        ), dlHelper.updateShoppingLists(
            dbChangedTime, shoppingLists -> this.shoppingLists = shoppingLists
        ), dlHelper.updateProductGroups(
            dbChangedTime,
            productGroups -> productGroupHashMap = ArrayUtil.getProductGroupsHashMap(productGroups)
        ), dlHelper.updateQuantityUnits(
            dbChangedTime,
            quantityUnits -> quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits)
        ), dlHelper.updateQuantityUnitConversions(
            dbChangedTime,
            unitConversions -> unitConversionHashMap = ArrayUtil.getUnitConversionsHashMap(unitConversions)
        ), dlHelper.updateProducts(dbChangedTime, products -> {
          productHashMap = ArrayUtil.getProductsHashMap(products);
          productNamesHashMap = ArrayUtil.getProductNamesHashMap(products);
        }), dlHelper.updateStores(
            dbChangedTime,
            stores -> storeHashMap = ArrayUtil.getStoresHashMap(stores)
        ), dlHelper.updateMissingItems(
            dbChangedTime,
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
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (itemsToSyncTemp == null || itemsToSyncTemp.isEmpty() || serverItemHashMapTemp == null) {
      fillShoppingListItemAmountsHashMap();
      updateFilteredShoppingListItems();
      return;
    }
    DownloadHelper.OnQueueEmptyListener emptyListener = () -> {
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
    DownloadHelper.OnErrorListener errorListener = error -> {
      showMessage(getString(R.string.msg_failed_to_sync));
      downloadData();
    };
    NetworkQueue queue = dlHelper.newQueue(emptyListener, errorListener);
    for (ShoppingListItem itemToSync : itemsToSyncTemp) {
      JSONObject body = new JSONObject();
      try {
        body.put("done", itemToSync.getDoneInt());
      } catch (JSONException e) {
        if (debug) {
          Log.e(TAG, "syncItems: " + e);
        }
      }
      queue.append(dlHelper.editShoppingListItem(itemToSync.getId(), body));
    }
    currentQueueLoading = queue;
    queue.start();
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

  public int getSelectedShoppingListId() {
    if (selectedShoppingListIdLive.getValue() == null) {
      return -1;
    }
    return selectedShoppingListIdLive.getValue();
  }

  public void selectShoppingList(ShoppingList shoppingList) {
    int shoppingListId = shoppingList.getId();
    if (shoppingListId == getSelectedShoppingListId()) {
      return;
    }
    sharedPrefs.edit().putInt(Constants.PREF.SHOPPING_LIST_LAST_ID, shoppingListId).apply();
    selectedShoppingListIdLive.setValue(shoppingListId);
    updateFilteredShoppingListItems();
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
    repository.insertShoppingListItems(
        () -> {
          sharedPrefs.edit()
              .putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null).apply();
          loadFromDatabase(false);
        },
        shoppingListItem
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

  @Nullable
  public List<ShoppingList> getShoppingLists() {
    return shoppingLists;
  }

  private void fillShoppingListItemAmountsHashMap() {
    shoppingListItemAmountsHashMap = new HashMap<>();
    for (ShoppingListItem item : shoppingListItems) {
      Double amount = AmountUtil.getShoppingListItemAmount(
          item, productHashMap, quantityUnitHashMap, unitConversionHashMap
      );
      if (amount != null) {
        shoppingListItemAmountsHashMap.put(item.getId(), amount);
      }
    }
  }

  public HashMap<Integer, Double> getShoppingListItemAmountsHashMap() {
    return shoppingListItemAmountsHashMap;
  }

  public ArrayList<Integer> getMissingProductIds() {
    return missingProductIds;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
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

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public String getGroupingMode() {
    return sharedPrefs.getString(PREF.SHOPPING_LIST_GROUPING_MODE,
        FilterChipLiveDataShoppingListGrouping.GROUPING_PRODUCT_GROUP);
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

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
