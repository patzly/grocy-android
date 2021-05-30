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
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.ShoppingListHelper;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingle;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class ShoppingListViewModel extends BaseViewModel {

  private static final String TAG = ShoppingListViewModel.class.getSimpleName();
  private static final int DEFAULT_SHOPPING_LIST_ID = 1;

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ShoppingListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Integer> selectedShoppingListIdLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<GroupedListItem>> filteredGroupedListItemsLive;

  private ArrayList<ShoppingListItem> shoppingListItems;
  private ArrayList<ShoppingList> shoppingLists;
  private ArrayList<ProductGroup> productGroups;
  private ArrayList<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private ArrayList<QuantityUnitConversion> unitConversions;
  private HashMap<Integer, ArrayList<QuantityUnitConversion>> unitConversionHashMap;
  private HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private ArrayList<Product> products;
  private HashMap<Integer, Product> productHashMap;
  private ArrayList<MissingItem> missingItems;
  private ArrayList<Integer> missingProductIds;

  private DownloadHelper.Queue currentQueueLoading;
  private String searchInput;
  private final HorizontalFilterBarSingle horizontalFilterBarSingle;
  private int itemsMissingCount;
  private int itemsUndoneCount;
  private final boolean debug;

  public ShoppingListViewModel(@NonNull Application application) {
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
    filteredGroupedListItemsLive = new MutableLiveData<>();

    horizontalFilterBarSingle = new HorizontalFilterBarSingle(
        this::updateFilteredShoppingListItems,
        HorizontalFilterBarSingle.MISSING,
        HorizontalFilterBarSingle.UNDONE
    );
    itemsMissingCount = 0;
    itemsUndoneCount = 0;

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
    repository.loadFromDatabase(
        (shoppingListItems, shoppingLists, productGroups, quantityUnits, unitConversions, products, missingItems) -> {
          this.shoppingListItems = shoppingListItems;
          this.shoppingLists = shoppingLists;
          this.productGroups = productGroups;
          this.quantityUnits = quantityUnits;
          quantityUnitHashMap = new HashMap<>();
          for (QuantityUnit quantityUnit : quantityUnits) {
            quantityUnitHashMap.put(quantityUnit.getId(), quantityUnit);
          }
          this.unitConversions = unitConversions;
          unitConversionHashMap = new HashMap<>();
          for (QuantityUnitConversion unitConversion : unitConversions) {
            ArrayList<QuantityUnitConversion> unitConversionArrayList
                = unitConversionHashMap.get(unitConversion.getProductId());
            if (unitConversionArrayList == null) {
              unitConversionArrayList = new ArrayList<>();
              unitConversionHashMap.put(unitConversion.getProductId(), unitConversionArrayList);
            }
            unitConversionArrayList.add(unitConversion);
          }
          this.missingItems = missingItems;
          missingProductIds = new ArrayList<>();
          for (MissingItem missingItem : missingItems) {
            missingProductIds.add(missingItem.getId());
          }
          this.products = products;
          productHashMap = new HashMap<>();
          for (Product product : products) {
            productHashMap.put(product.getId(), product);
          }
          fillShoppingListItemAmountsHashMap();
          updateFilteredShoppingListItems();
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  public void updateFilteredShoppingListItems() {
    filteredGroupedListItemsLive.setValue(
        ShoppingListHelper.groupItems(
            getApplication(),
            getFilteredShoppingListItems(),
            this.productHashMap,
            getProductNamesHashMap(),
            this.productGroups,
            this.shoppingLists,
            getSelectedShoppingListId(),
            true
        )
    );
    selectedShoppingListIdLive.setValue(selectedShoppingListIdLive.getValue());
  }

  @Nullable
  public ArrayList<ShoppingListItem> getFilteredShoppingListItems() {
    if (this.shoppingListItems == null) {
      return null;
    }

    ArrayList<ShoppingListItem> filteredShoppingListItems = new ArrayList<>();
    itemsMissingCount = 0;
    itemsUndoneCount = 0;

    for (ShoppingListItem item : this.shoppingListItems) {
      if (item.getShoppingListIdInt() != getSelectedShoppingListId()) {
        continue;
      }
      if (item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {
        itemsMissingCount += 1;
      }
      if (item.isUndone()) {
        itemsUndoneCount += 1;
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

      if (horizontalFilterBarSingle.isNoFilterActive()
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.MISSING)
          && item.hasProduct() && missingProductIds.contains(item.getProductIdInt())
          || horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.UNDONE)
          && item.isUndone()
      ) {
        filteredShoppingListItems.add(item);
      }
    }
    return filteredShoppingListItems;
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
  }

  public MutableLiveData<ArrayList<GroupedListItem>> getFilteredGroupedListItemsLive() {
    return filteredGroupedListItemsLive;
  }

  public int getItemsMissingCount() {
    return itemsMissingCount;
  }

  public int getItemsUndoneCount() {
    return itemsUndoneCount;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredShoppingListItems();
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

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateShoppingListItems(
            dbChangedTime, shoppingListItems -> this.shoppingListItems = shoppingListItems
        ), dlHelper.updateShoppingLists(
            dbChangedTime, shoppingLists -> this.shoppingLists = shoppingLists
        ), dlHelper.updateProductGroups(
            dbChangedTime, productGroups -> this.productGroups = productGroups
        ), dlHelper.updateQuantityUnits(
            dbChangedTime, quantityUnits -> {
              this.quantityUnits = quantityUnits;
              quantityUnitHashMap = new HashMap<>();
              for (QuantityUnit quantityUnit : quantityUnits) {
                quantityUnitHashMap.put(quantityUnit.getId(), quantityUnit);
              }
            }
        ), dlHelper.updateQuantityUnitConversions(
            dbChangedTime, unitConversions -> {
              this.unitConversions = unitConversions;
              unitConversionHashMap = new HashMap<>();
              for (QuantityUnitConversion unitConversion : unitConversions) {
                ArrayList<QuantityUnitConversion> unitConversionArrayList
                    = unitConversionHashMap.get(unitConversion.getProductId());
                if (unitConversionArrayList == null) {
                  unitConversionArrayList = new ArrayList<>();
                  unitConversionHashMap.put(unitConversion.getProductId(), unitConversionArrayList);
                }
                unitConversionArrayList.add(unitConversion);
              }
            }
        ), dlHelper.updateProducts(dbChangedTime, products -> {
          this.products = products;
          productHashMap = new HashMap<>();
          for (Product product : products) {
            productHashMap.put(product.getId(), product);
          }
        }),
        dlHelper.updateMissingItems(dbChangedTime, missing -> {
          this.missingItems = missing;
          missingProductIds = new ArrayList<>();
          for (MissingItem missingItem : missingItems) {
            missingProductIds.add(missingItem.getId());
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    repository.updateDatabase(
        this.shoppingListItems,
        this.shoppingLists,
        this.productGroups,
        this.quantityUnits,
        this.unitConversions,
        this.products,
        this.missingItems,
        (itemsToSync, serverItemHashMap) -> {
          if (itemsToSync.isEmpty()) {
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
          DownloadHelper.OnQueueEmptyListener emptyListener = () -> {
            ArrayList<ShoppingListItem> itemsToUpdate = new ArrayList<>();
            for (ShoppingListItem itemToSync : itemsToSync) {
              int itemId = itemToSync.getId();
              ShoppingListItem itemToUpdate = serverItemHashMap.get(itemId);
              if (itemToUpdate == null) {
                continue;
              }
              itemToUpdate.setDone(itemToSync.getDoneInt());
              itemsToUpdate.add(itemToUpdate);
            }
            repository.insertShoppingListItems(
                () -> {
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
          DownloadHelper.Queue queue = dlHelper.newQueue(emptyListener, errorListener);
          for (ShoppingListItem itemToSync : itemsToSync) {
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

    DownloadHelper.Queue queue = dlHelper.newQueue(
        () -> {
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
      queue.append(dlHelper.deleteShoppingListItem(listItem.getId()));
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
    DownloadHelper.Queue queue = dlHelper.newQueue(
        () -> {
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
      queue.append(dlHelper.deleteShoppingListItem(shoppingListItem.getId()));
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

  @Nullable
  public ArrayList<ShoppingList> getShoppingLists() {
    return shoppingLists;
  }

  public ArrayList<Integer> getMissingProductIds() {
    return missingProductIds;
  }

  public HorizontalFilterBarSingle getHorizontalFilterBarSingle() {
    return horizontalFilterBarSingle;
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

  private void fillShoppingListItemAmountsHashMap() {
    shoppingListItemAmountsHashMap = new HashMap<>();
    for (ShoppingListItem item : shoppingListItems) {
      if (!item.hasProduct()) {
        continue;
      }
      Product product = productHashMap.get(item.getProductIdInt());
      ArrayList<QuantityUnitConversion> unitConversions
          = unitConversionHashMap.get(item.getProductIdInt());
      if (product == null) {
        continue;
      }
      if (unitConversions == null) {
        unitConversions = new ArrayList<>();
      }

      QuantityUnit stock = quantityUnitHashMap.get(product.getQuIdStockInt());
      QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());
      if (stock == null || purchase == null) {
        continue;
      }
      HashMap<Integer, Double> unitFactors = new HashMap<>();
      ArrayList<Integer> quIdsInHashMap = new ArrayList<>();
      unitFactors.put(stock.getId(), (double) -1);
      quIdsInHashMap.add(stock.getId());
      if (!quIdsInHashMap.contains(purchase.getId())) {
        unitFactors.put(purchase.getId(), product.getQuFactorPurchaseToStockDouble());
      }
      for (QuantityUnitConversion conversion : unitConversions) {
        QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
        if (unit == null || quIdsInHashMap.contains(unit.getId())) {
          continue;
        }
        unitFactors.put(unit.getId(), conversion.getFactor());
      }
      if (!unitFactors.containsKey(item.getQuIdInt())) {
        continue;
      }
      Double factor = unitFactors.get(item.getQuIdInt());
      assert factor != null;
      if (factor != -1 && item.getQuIdInt() == product.getQuIdPurchaseInt()) {
        shoppingListItemAmountsHashMap.put(item.getId(), item.getAmountDouble() / factor);
      } else if (factor != -1) {
        shoppingListItemAmountsHashMap.put(item.getId(), item.getAmountDouble() * factor);
      }
    }
  }

  public HashMap<Integer, Double> getShoppingListItemAmountsHashMap() {
    return shoppingListItemAmountsHashMap;
  }

  public HashMap<Integer, String> getProductNamesHashMap() {
    if (products == null) {
      return null;
    }
    HashMap<Integer, String> productNamesHashMap = new HashMap<>();
    for (Product product : products) {
      productNamesHashMap.put(product.getId(), product.getName());
    }
    return productNamesHashMap;
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

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
