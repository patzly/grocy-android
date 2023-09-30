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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingModeGrouping;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.ShoppingListItem.ShoppingListItemWithSync;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class ShoppingModeViewModel extends BaseViewModel {

  private static final String TAG = ShoppingModeViewModel.class.getSimpleName();
  private static final int DEFAULT_SHOPPING_LIST_ID = 1;

  public final static String FIELD_AMOUNT = "field_amount";
  public final static String FIELD_PRICE_LAST_UNIT = "field_price_last_unit";
  public final static String FIELD_PRICE_LAST_TOTAL = "field_price_last_total";
  public final static String FIELD_NOTES = "field_notes";
  public final static String FIELD_PRODUCT_DESCRIPTION = "field_product_description";
  public final static String FIELD_PICTURE = "field_picture";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Integer> selectedShoppingListIdLive;
  private final MutableLiveData<ArrayList<ShoppingListItem>> filteredShoppingListItemsLive;
  private final FilterChipLiveDataShoppingModeGrouping filterChipLiveDataGrouping;
  private final FilterChipLiveDataFields filterChipLiveDataFields;

  private List<ShoppingListItem> shoppingListItems;
  private List<ShoppingList> shoppingLists;
  private HashMap<Integer, ProductGroup> productGroupHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private List<QuantityUnitConversionResolved> unitConversions;
  private HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private HashMap<Integer, Store> storeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, String> productNamesHashMap;
  private HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private ArrayList<Integer> missingProductIds;
  private final boolean debug;

  public ShoppingModeViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new ShoppingListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    selectedShoppingListIdLive = new MutableLiveData<>(1);
    filteredShoppingListItemsLive = new MutableLiveData<>();
    filterChipLiveDataGrouping = new FilterChipLiveDataShoppingModeGrouping(
        getApplication(),
        this::updateFilteredShoppingListItems
    );
    boolean featurePriceEnabled = isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING);
    filterChipLiveDataFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.SHOPPING_MODE_FIELDS,
        this::updateFilteredShoppingListItems,
        new Field(FIELD_AMOUNT, getString(R.string.property_amount), true),
        featurePriceEnabled
            ? new Field(FIELD_PRICE_LAST_TOTAL, getString(R.string.property_last_price_total), false) : null,
        featurePriceEnabled
            ? new Field(FIELD_PRICE_LAST_UNIT, getString(R.string.property_last_price_unit), false) : null,
        new Field(FIELD_NOTES, getString(R.string.property_notes), true),
        new Field(FIELD_PRODUCT_DESCRIPTION, getString(R.string.property_product_description), false),
        new Field(FIELD_PICTURE, getString(R.string.property_picture), false)
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
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productNamesHashMap = ArrayUtil.getProductNamesHashMap(data.getProducts());
      productLastPurchasedHashMap = ArrayUtil
          .getProductLastPurchasedHashMap(data.getProductsLastPurchased());
      storeHashMap = ArrayUtil.getStoresHashMap(data.getStores());
      missingProductIds = ArrayUtil.getMissingProductsIds(data.getMissingItems());
      fillShoppingListItemAmountsHashMap();
      updateFilteredShoppingListItems();
      if (downloadAfterLoading) {
        downloadData(false, false);
      } else {
        syncShoppingListItems();
      }
    }, error -> onError(error, TAG));
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

  public void downloadData(boolean forceUpdate, boolean noSync) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            syncShoppingListItems();
          }
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        noSync ? ShoppingListItem.class : ShoppingListItemWithSync.class,
        ShoppingList.class,
        ProductGroup.class,
        Product.class,
        Store.class,
        MissingItem.class,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class
    );
  }

  private void syncShoppingListItems() {
    if (isOffline()) return;
    ArrayList<ShoppingListItem> itemsToSync = new ArrayList<>();
    for (ShoppingListItem item : shoppingListItems) {
      if (item.getDoneSynced() != -1) {
        itemsToSync.add(item);
      }
    }
    if (itemsToSync.isEmpty()) return;
    Runnable emptyListener = () -> {
      ArrayList<ShoppingListItem> itemsToUpdate = new ArrayList<>();
      for (ShoppingListItem itemToSync : itemsToSync) {
        itemToSync.setDoneSynced(-1);
        itemsToUpdate.add(itemToSync);
      }
      repository.insertShoppingListItems(
          () -> {
            showMessage(getString(R.string.msg_synced));
            loadFromDatabase(false);
          },
          itemsToUpdate.toArray(new ShoppingListItem[0])
      );
    };
    DownloadHelper.OnMultiTypeErrorListener errorListener = error -> {
      SnackbarMessage snackbarMessage = new SnackbarMessage(getString(R.string.msg_failed_to_sync));
      snackbarMessage.setAction(
          getString(R.string.action_details),
          v -> showSyncErrorDetailsAlertDialog()
      );
      snackbarMessage.setDurationSecs(5);
      showSnackbar(snackbarMessage);
      showMessage(getString(R.string.msg_failed_to_sync));
    };
    NetworkQueue queue = dlHelper.newQueue(updated -> emptyListener.run(), errorListener);
    for (ShoppingListItem itemToSync : itemsToSync) {
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
    queue.start();
  }

  private void showSyncErrorDetailsAlertDialog() {
    new MaterialAlertDialogBuilder(
        getApplication(), R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.msg_failed_to_sync)
        .setPositiveButton(R.string.action_try_again, (dialog, which) -> syncShoppingListItems())
        .setNegativeButton(
            R.string.action_reload,
            (dialog, which) -> downloadData(true, true)
        ).create().show();
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

    if (isOffline()) {
      if (shoppingListItem.getDoneSynced() == -1) {
        shoppingListItem.setDoneSynced(shoppingListItem.getDoneInt());
      }
      shoppingListItem.setDone(shoppingListItem.getDoneInt() == 0 ? 1 : 0);  // toggle state
      updateDoneStatus(shoppingListItem);
      return;
    }

    shoppingListItem.setDone(shoppingListItem.getDoneInt() == 0 ? 1 : 0);  // toggle state

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
          downloadData(false, false);
        },
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(TAG, "saveNotes: " + error);
          }
          downloadData(false, false);
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
    boolean isGrocyServerMin400 = VersionUtil.isGrocyServerMin400(sharedPrefs);
    for (ShoppingListItem item : shoppingListItems) {
      Double amount = AmountUtil.getShoppingListItemAmount(
          item, productHashMap, quantityUnitHashMap, unitConversions, isGrocyServerMin400
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

  public HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap() {
    return productLastPurchasedHashMap;
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

  public List<QuantityUnitConversionResolved> getUnitConversions() {
    return unitConversions;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public FilterChipLiveData getFilterChipLiveDataGrouping() {
    return filterChipLiveDataGrouping;
  }

  public String getGroupingMode() {
    return filterChipLiveDataGrouping.getGroupingMode();
  }

  public FilterChipLiveDataFields getFilterChipLiveDataFields() {
    return filterChipLiveDataFields;
  }

  public List<String> getActiveFields() {
    return filterChipLiveDataFields.getActiveFields();
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
