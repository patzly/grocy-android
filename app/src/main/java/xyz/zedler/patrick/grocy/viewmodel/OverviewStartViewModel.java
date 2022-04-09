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
import androidx.annotation.PluralsRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.repository.OverviewStartRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class OverviewStartViewModel extends BaseViewModel {

  private static final String TAG = OverviewStartViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final OverviewStartRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private final MutableLiveData<List<StockItem>> stockItemsLive;
  private final MutableLiveData<List<ShoppingListItem>> shoppingListItemsLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<List<Task>> tasksLive;
  private final MutableLiveData<List<ChoreEntry>> choreEntriesLive;
  private final MutableLiveData<Integer> itemsDueNextCountLive;
  private final MutableLiveData<Integer> itemsOverdueCountLive;
  private final MutableLiveData<Integer> itemsExpiredCountLive;
  private final MutableLiveData<Integer> itemsMissingCountLive;
  private final MutableLiveData<Integer> itemsMissingShoppingListCountLive;
  private final MutableLiveData<Boolean> storedPurchasesOnDevice;
  private final LiveData<String> stockDescriptionTextLive;
  private final LiveData<String> stockDescriptionDueNextTextLive;
  private final LiveData<String> stockDescriptionOverdueTextLive;
  private final LiveData<String> stockDescriptionExpiredTextLive;
  private final LiveData<String> stockDescriptionMissingTextLive;
  private final LiveData<String> stockDescriptionMissingShoppingListTextLive;
  private final LiveData<String> shoppingListDescriptionTextLive;
  private final MutableLiveData<Integer> choresDueSoonCountLive;
  private final MutableLiveData<Integer> choresOverdueCountLive;
  private final MutableLiveData<Integer> choresDueTodayCountLive;
  private final MutableLiveData<Integer> choresAssignedCountLive;
  private final LiveData<String> choresDescriptionDueSoonTextLive;
  private final LiveData<String> choresDescriptionOverdueTextLive;
  private final LiveData<String> choresDescriptionDueTodayTextLive;
  private final LiveData<String> choresDescriptionAssignedTextLive;
  private final LiveData<String> tasksDescriptionTextLive;
  private final LiveData<String> tasksUserDescriptionTextLive;
  private final LiveData<String> masterDataDescriptionTextLive;
  private final MutableLiveData<Integer> currentUserIdLive;
  private ArrayList<StockItem> stockItemsTemp;
  private ArrayList<StockItem> dueItemsTemp;
  private ArrayList<StockItem> overdueItemsTemp;
  private ArrayList<StockItem> expiredItemsTemp;
  private ArrayList<MissingItem> missingItemsTemp;
  private List<ShoppingList> shoppingLists;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  public OverviewStartViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new OverviewStartRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    stockItemsLive = new MutableLiveData<>();
    itemsDueNextCountLive = new MutableLiveData<>();
    itemsOverdueCountLive = new MutableLiveData<>();
    itemsExpiredCountLive = new MutableLiveData<>();
    itemsMissingCountLive = new MutableLiveData<>();
    itemsMissingShoppingListCountLive = new MutableLiveData<>();
    storedPurchasesOnDevice = new MutableLiveData<>(false);
    shoppingListItemsLive = new MutableLiveData<>();
    productsLive = new MutableLiveData<>();
    choresDueTodayCountLive = new MutableLiveData<>();
    choresDueSoonCountLive = new MutableLiveData<>();
    choresAssignedCountLive = new MutableLiveData<>();
    choresOverdueCountLive = new MutableLiveData<>();
    choreEntriesLive = new MutableLiveData<>();
    tasksLive = new MutableLiveData<>();
    currentUserIdLive = new MutableLiveData<>(sharedPrefs.getInt(PREF.CURRENT_USER_ID, 1));

    stockDescriptionTextLive = Transformations.map(
        stockItemsLive,
        stockItems -> {
          if (stockItems == null) {
            return null;
          }
          int products = stockItems.size();
          double value = 0;
          for (StockItem stockItem : stockItems) {
            if (stockItem.isItemMissing() && !stockItem.isItemMissingAndPartlyInStock()) {
              products--;
              continue;
            }
            value += stockItem.getValueDouble();
          }
          if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            return getResources().getQuantityString(
                R.plurals.description_overview_stock_value,
                products, products,
                NumUtil.trim(value),
                sharedPrefs.getString(Constants.PREF.CURRENCY, "")
            );
          } else {
            return getResources().getQuantityString(
                R.plurals.description_overview_stock,
                products, products
            );
          }
        }
    );
    stockDescriptionDueNextTextLive = Transformations.map(
        itemsDueNextCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          String days = sharedPrefs.getString(
              STOCK.DUE_SOON_DAYS,
              SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
          );
          int daysInt;
          if (NumUtil.isStringInt(days)) {
            daysInt = Integer.parseInt(days);
          } else {
            daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_stock_due_soon,
              count, count, daysInt
          );
        }
    );
    stockDescriptionOverdueTextLive = Transformations.map(
        itemsOverdueCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_stock_overdue,
              count, count
          );
        }
    );
    stockDescriptionExpiredTextLive = Transformations.map(
        itemsExpiredCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_stock_expired,
              count, count
          );
        }
    );
    stockDescriptionMissingTextLive = Transformations.map(
        itemsMissingCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_stock_missing,
              count, count
          );
        }
    );
    stockDescriptionMissingShoppingListTextLive = Transformations.map(
        itemsMissingShoppingListCountLive,
        count -> {
          if (count == null || !isFeatureEnabled(PREF.FEATURE_SHOPPING_LIST)) {
            return null;
          }
          @PluralsRes int string;
          if (shoppingLists == null || shoppingLists.size() > 1) {
            string = R.plurals.description_overview_stock_missing_shopping_list_multi;
          } else {
            string = R.plurals.description_overview_stock_missing_shopping_list_single;
          }
          return getResources().getQuantityString(string, count, count);
        }
    );
    shoppingListDescriptionTextLive = Transformations.map(
        shoppingListItemsLive,
        shoppingListItems -> {
          if (shoppingListItems == null) {
            return null;
          }
          int size = shoppingListItems.size();
          if (shoppingLists == null || shoppingLists.size() > 1) {
            return getResources().getQuantityString(
                R.plurals.description_overview_shopping_list_multi, size, size
            );
          } else {
            return getResources().getQuantityString(
                R.plurals.description_overview_shopping_list, size, size
            );
          }
        }
    );
    choresDescriptionDueSoonTextLive = Transformations.map(
        choresDueSoonCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          String days = sharedPrefs.getString(
              STOCK.DUE_SOON_DAYS,
              SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
          );
          int daysInt;
          if (NumUtil.isStringInt(days)) {
            daysInt = Integer.parseInt(days);
          } else {
            daysInt = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_chores_due_soon,
              count, count, daysInt
          );
        }
    );
    choresDescriptionOverdueTextLive = Transformations.map(
        choresOverdueCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_chores_overdue,
              count, count
          );
        }
    );
    choresDescriptionDueTodayTextLive = Transformations.map(
        choresDueTodayCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_chores_due_today,
              count, count
          );
        }
    );
    choresDescriptionAssignedTextLive = Transformations.map(
        choresAssignedCountLive,
        count -> {
          if (count == null || count == 0) {
            return null;
          }
          return getResources().getQuantityString(
              R.plurals.description_overview_chores_assigned,
              count, count
          );
        }
    );
    tasksDescriptionTextLive = Transformations.map(
        tasksLive,
        tasks -> {
          if (tasks == null) {
            return null;
          }
          int undoneTasksCount = Task.getUndoneTasksCount(tasks);
          return getResources().getQuantityString(
              R.plurals.description_overview_tasks, undoneTasksCount, undoneTasksCount
          );
        }
    );
    tasksUserDescriptionTextLive = Transformations.map(
        tasksLive,
        tasks -> {
          if (tasks == null) return null;
          int currentUserId = currentUserIdLive.getValue() != null ? currentUserIdLive.getValue() : 1;
          int assignedTasksCount = Task
              .getAssignedTasksCount(Task.getUndoneTasksOnly(tasks), currentUserId);
          return getResources().getQuantityString(
              R.plurals.description_overview_tasks_user, assignedTasksCount, assignedTasksCount
          );
        }
    );
    masterDataDescriptionTextLive = Transformations.map(
        productsLive,
        products -> {
          if (products == null) {
            return null;
          }
          int size = products.size();
          return getResources().getQuantityString(
              R.plurals.description_overview_master_data, size, size
          );
        }
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.shoppingLists = data.getShoppingLists();
      this.stockItemsLive.setValue(data.getStockItems());
      this.shoppingListItemsLive.setValue(data.getShoppingListItems());
      this.productsLive.setValue(data.getProducts());
      this.storedPurchasesOnDevice.setValue(data.getStoredPurchases().size() > 0);
      this.choreEntriesLive.setValue(data.getChoreEntries());
      this.tasksLive.setValue(data.getTasks());

      ArrayList<Integer> shoppingListItemsProductIds = new ArrayList<>();
      for (ShoppingListItem item : data.getShoppingListItems()) {
        if (!item.hasProduct()) {
          continue;
        }
        shoppingListItemsProductIds.add(item.getProductIdInt());
      }

      int itemsDueCount = 0;
      int itemsOverdueCount = 0;
      int itemsExpiredCount = 0;
      int itemsMissingCount = 0;
      int missingItemsOnShoppingListCount = 0;
      for (StockItem stockItem : data.getStockItems()) {
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
          if (shoppingListItemsProductIds.contains(stockItem.getProductId())) {
            missingItemsOnShoppingListCount++;
          }
        }
      }
      itemsDueNextCountLive.setValue(itemsDueCount);
      itemsOverdueCountLive.setValue(itemsOverdueCount);
      itemsExpiredCountLive.setValue(itemsExpiredCount);
      itemsMissingCountLive.setValue(itemsMissingCount);
      itemsMissingShoppingListCountLive.setValue(missingItemsOnShoppingListCount);

      int choresDueTodayCount = 0;
      int choresDueSoonCount = 0;
      int choresOverdueCount = 0;
      int choresAssignedCount = 0;
      for (ChoreEntry choreEntry : data.getChoreEntries()) {
        if (NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
            && currentUserIdLive.getValue() != null && currentUserIdLive.getValue()
            == Integer.parseInt(choreEntry.getNextExecutionAssignedToUserId())) {
          choresAssignedCount++;
        }
        if (choreEntry.getNextEstimatedExecutionTime() == null
            || choreEntry.getNextEstimatedExecutionTime().isEmpty()) {
          continue;
        }
        int daysFromNow = DateUtil
            .getDaysFromNowWithTime(choreEntry.getNextEstimatedExecutionTime());
        if (daysFromNow < 0) {
          choresOverdueCount++;
        }
        if (daysFromNow == 0) {
          choresDueTodayCount++;
        }
        if (daysFromNow >= 0 && daysFromNow <= 5) {
          choresDueSoonCount++;
        }
      }
      choresAssignedCountLive.setValue(choresAssignedCount);
      choresOverdueCountLive.setValue(choresOverdueCount);
      choresDueSoonCountLive.setValue(choresDueSoonCount);
      choresDueTodayCountLive.setValue(choresDueTodayCount);

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

    DownloadHelper.OnQueueEmptyListener onQueueEmptyListener = () -> {
      if (stockItemsTemp == null || dueItemsTemp == null || overdueItemsTemp == null
          || expiredItemsTemp == null || missingItemsTemp == null) {
        downloadDataForceUpdate();
        return;
      }

      HashMap<Integer, StockItem> stockItemHashMap = new HashMap<>();
      for (StockItem stockItem : stockItemsTemp) {
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

      ArrayList<Integer> shoppingListItemsProductIds = new ArrayList<>();
      if (shoppingListItemsLive.getValue() != null) {
        for (ShoppingListItem item : shoppingListItemsLive.getValue()) {
          if (!item.hasProduct()) {
            continue;
          }
          shoppingListItemsProductIds.add(item.getProductIdInt());
        }
      }

      DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);

      int missingItemsOnShoppingListCount = 0;

      for (MissingItem missingItem : missingItemsTemp) {

        if (shoppingListItemsProductIds.contains(missingItem.getId())) {
          missingItemsOnShoppingListCount++;
        }
        StockItem missingStockItem = stockItemHashMap.get(missingItem.getId());
        if (missingStockItem != null) {
          missingStockItem.setItemMissing(true);
          missingStockItem.setItemMissingAndPartlyInStock(true);
          continue;
        }
        queue.append(dlHelper.getProductDetails(missingItem.getId(), productDetails -> {
          StockItem stockItem = new StockItem(productDetails);
          stockItem.setItemMissing(true);
          stockItem.setItemMissingAndPartlyInStock(false);
          stockItemsTemp.add(stockItem);
        }));
      }
      itemsMissingShoppingListCountLive.setValue(missingItemsOnShoppingListCount);
      if (queue.getSize() == 0) {
        onQueueEmpty();
        return;
      }
      queue.start();
    };

    DownloadHelper.Queue queue = dlHelper.newQueue(onQueueEmptyListener, this::onDownloadError);
    queue.append(
        dlHelper.updateStockItems(dbChangedTime, stockItems -> stockItemsTemp = stockItems),
        dlHelper.updateShoppingListItems(dbChangedTime, this.shoppingListItemsLive::setValue),
        dlHelper.updateShoppingLists(dbChangedTime, shoppingLists -> {
          this.shoppingLists = shoppingLists;
          this.shoppingListItemsLive.setValue(this.shoppingListItemsLive.getValue());
        }),
        dlHelper.updateProducts(dbChangedTime, this.productsLive::setValue),
        dlHelper.updateVolatile(dbChangedTime, (due, overdue, expired, missing) -> {
          this.dueItemsTemp = due;
          itemsDueNextCountLive.setValue(due.size());
          this.overdueItemsTemp = overdue;
          itemsOverdueCountLive.setValue(overdue.size());
          this.expiredItemsTemp = expired;
          itemsExpiredCountLive.setValue(expired.size());
          this.missingItemsTemp = missing;
          itemsMissingCountLive.setValue(missing.size());
        }),
        dlHelper.updateChoreEntries(dbChangedTime, choreEntries -> {
          this.choreEntriesLive.setValue(choreEntries);
          int choresDueTodayCount = 0;
          int choresDueSoonCount = 0;
          int choresOverdueCount = 0;
          int choresAssignedCount = 0;
          for (ChoreEntry choreEntry : choreEntries) {
            if (NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
                && currentUserIdLive.getValue() != null && currentUserIdLive.getValue()
                == Integer.parseInt(choreEntry.getNextExecutionAssignedToUserId())) {
              choresAssignedCount++;
            }
            if (choreEntry.getNextEstimatedExecutionTime() == null
                || choreEntry.getNextEstimatedExecutionTime().isEmpty()) {
              continue;
            }
            int daysFromNow = DateUtil
                .getDaysFromNowWithTime(choreEntry.getNextEstimatedExecutionTime());
            if (daysFromNow < 0) {
              choresOverdueCount++;
            }
            if (daysFromNow == 0) {
              choresDueTodayCount++;
            }
            if (daysFromNow >= 0 && daysFromNow <= 5) {
              choresDueSoonCount++;
            }
          }
          choresAssignedCountLive.setValue(choresAssignedCount);
          choresOverdueCountLive.setValue(choresOverdueCount);
          choresDueSoonCountLive.setValue(choresDueSoonCount);
          choresDueTodayCountLive.setValue(choresDueTodayCount);
        }),
        dlHelper.updateTasks(dbChangedTime, this.tasksLive::setValue)
    );
    if (sharedPrefs.getInt(PREF.CURRENT_USER_ID, -1) == -1) {
      queue.append(dlHelper.getCurrentUserId(id -> {
        if (id != -1) {
          sharedPrefs.edit().putInt(PREF.CURRENT_USER_ID, id).apply();
          currentUserIdLive.setValue(id);
          tasksLive.setValue(tasksLive.getValue());  // update descriptions above
          if (this.choreEntriesLive.getValue() != null) {
            int choresAssignedCount = 0;
            for (ChoreEntry choreEntry : this.choreEntriesLive.getValue()) {
              if (NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
                  && currentUserIdLive.getValue() != null && currentUserIdLive.getValue()
                  == Integer.parseInt(choreEntry.getNextExecutionAssignedToUserId())) {
                choresAssignedCount++;
              }
            }
            choresAssignedCountLive.setValue(choresAssignedCount);
          }
        }
      }));
    }
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
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_VOLATILE, null);
    editPrefs.putString(PREF.DB_LAST_TIME_TASKS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_CHORE_ENTRIES, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    repository.updateDatabase(stockItemsTemp, () -> {});
    infoFullscreenLive.setValue(null);
    this.stockItemsLive.setValue(stockItemsTemp);
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    String exact = error == null ? null : error.getLocalizedMessage();
    infoFullscreenLive.setValue(
        new InfoFullscreen(InfoFullscreen.ERROR_NETWORK, exact, () -> {
          infoFullscreenLive.setValue(null);
          downloadDataForceUpdate();
        })
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

  public LiveData<String> getStockDescriptionTextLive() {
    return stockDescriptionTextLive;
  }

  public LiveData<String> getStockDescriptionDueNextTextLive() {
    return stockDescriptionDueNextTextLive;
  }

  public LiveData<String> getStockDescriptionOverdueTextLive() {
    return stockDescriptionOverdueTextLive;
  }

  public LiveData<String> getStockDescriptionExpiredTextLive() {
    return stockDescriptionExpiredTextLive;
  }

  public LiveData<String> getStockDescriptionMissingTextLive() {
    return stockDescriptionMissingTextLive;
  }

  public LiveData<String> getStockDescriptionMissingShoppingListTextLive() {
    return stockDescriptionMissingShoppingListTextLive;
  }

  public LiveData<String> getShoppingListDescriptionTextLive() {
    return shoppingListDescriptionTextLive;
  }

  public LiveData<String> getChoresDescriptionAssignedTextLive() {
    return choresDescriptionAssignedTextLive;
  }

  public LiveData<String> getChoresDescriptionDueSoonTextLive() {
    return choresDescriptionDueSoonTextLive;
  }

  public LiveData<String> getChoresDescriptionDueTodayTextLive() {
    return choresDescriptionDueTodayTextLive;
  }

  public LiveData<String> getChoresDescriptionOverdueTextLive() {
    return choresDescriptionOverdueTextLive;
  }

  public LiveData<String> getTasksDescriptionTextLive() {
    return tasksDescriptionTextLive;
  }

  public LiveData<String> getTasksUserDescriptionTextLive() {
    return tasksUserDescriptionTextLive;
  }

  public LiveData<String> getMasterDataDescriptionTextLive() {
    return masterDataDescriptionTextLive;
  }

  public MutableLiveData<Boolean> getStoredPurchasesOnDevice() {
    return storedPurchasesOnDevice;
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

  public boolean getBeginnerModeEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    );
  }

  public boolean getIsDemoInstance() {
    String server = sharedPrefs.getString(PREF.SERVER_URL, null);
    return server != null && server.contains("grocy.info");
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
