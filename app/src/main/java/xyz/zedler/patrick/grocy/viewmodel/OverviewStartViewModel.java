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
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.repository.OverviewStartRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class OverviewStartViewModel extends BaseViewModel {

  private static final String TAG = OverviewStartViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final OverviewStartRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<List<StockItem>> stockItemsLive;
  private final MutableLiveData<List<ShoppingListItem>> shoppingListItemsLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<List<Recipe>> recipesLive;
  private final MutableLiveData<List<Task>> tasksLive;
  private final MutableLiveData<List<ChoreEntry>> choreEntriesLive;
  private final MutableLiveData<Integer> itemsDueNextCountLive;
  private final MutableLiveData<Integer> itemsOverdueCountLive;
  private final MutableLiveData<Integer> itemsExpiredCountLive;
  private final MutableLiveData<Integer> itemsMissingCountLive;
  private final MutableLiveData<Integer> itemsMissingShoppingListCountLive;
  private final MutableLiveData<Integer> itemsInStockCountLive;
  private final MutableLiveData<Double> stockValueLive;
  private final MutableLiveData<Boolean> storedPurchasesOnDevice;
  private final MediatorLiveData<String> stockDescriptionTextLive;
  private final LiveData<String> stockDescriptionDueNextTextLive;
  private final LiveData<String> stockDescriptionOverdueTextLive;
  private final LiveData<String> stockDescriptionExpiredTextLive;
  private final LiveData<String> stockDescriptionMissingTextLive;
  private final LiveData<String> stockDescriptionMissingShoppingListTextLive;
  private final LiveData<String> shoppingListDescriptionTextLive;
  private final LiveData<String> recipesDescriptionTextLive;
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
  private List<ShoppingList> shoppingLists;
  private boolean alreadyLoadedFromDatabase;

  public OverviewStartViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    int decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new OverviewStartRepository(application);

    stockItemsLive = new MutableLiveData<>();
    itemsDueNextCountLive = new MutableLiveData<>();
    itemsOverdueCountLive = new MutableLiveData<>();
    itemsExpiredCountLive = new MutableLiveData<>();
    itemsMissingCountLive = new MutableLiveData<>();
    itemsMissingShoppingListCountLive = new MutableLiveData<>();
    itemsInStockCountLive = new MutableLiveData<>();
    stockValueLive = new MutableLiveData<>();
    storedPurchasesOnDevice = new MutableLiveData<>(false);
    shoppingListItemsLive = new MutableLiveData<>();
    productsLive = new MutableLiveData<>();
    recipesLive = new MutableLiveData<>();
    choresDueTodayCountLive = new MutableLiveData<>();
    choresDueSoonCountLive = new MutableLiveData<>();
    choresAssignedCountLive = new MutableLiveData<>();
    choresOverdueCountLive = new MutableLiveData<>();
    choreEntriesLive = new MutableLiveData<>();
    tasksLive = new MutableLiveData<>();
    currentUserIdLive = new MutableLiveData<>(sharedPrefs.getInt(PREF.CURRENT_USER_ID, 1));
    alreadyLoadedFromDatabase = false;

    stockDescriptionTextLive = new MediatorLiveData<>();
    stockDescriptionTextLive.addSource(
        itemsInStockCountLive,
        count -> {
          if (count == null) return;
          double value = stockValueLive.getValue() != null ? stockValueLive.getValue() : 0;
          if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            stockDescriptionTextLive.setValue(getResources().getQuantityString(
                R.plurals.description_overview_stock_value,
                count, count,
                NumUtil.trimPrice(value, decimalPlacesPriceDisplay),
                sharedPrefs.getString(Constants.PREF.CURRENCY, "")
            ));
          } else {
            stockDescriptionTextLive.setValue(getResources().getQuantityString(
                R.plurals.description_overview_stock,
                count, count
            ));
          }
        }
    );
    stockDescriptionTextLive.addSource(
        stockValueLive,
        value -> {
          if (itemsInStockCountLive.getValue() == null) return;
          int count = itemsInStockCountLive.getValue();
          if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            stockDescriptionTextLive.setValue(getResources().getQuantityString(
                R.plurals.description_overview_stock_value,
                count, count,
                NumUtil.trimPrice(value, decimalPlacesPriceDisplay),
                sharedPrefs.getString(Constants.PREF.CURRENCY, "")
            ));
          } else {
            stockDescriptionTextLive.setValue(getResources().getQuantityString(
                R.plurals.description_overview_stock,
                count, count
            ));
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
    recipesDescriptionTextLive = Transformations.map(
        recipesLive,
        recipes -> {
          if (recipes == null) {
            return null;
          }
          int size = recipes.size();
          return getResources().getQuantityString(
              R.plurals.description_overview_recipes, size, size
          );
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
      this.recipesLive.setValue(data.getRecipes());
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
      HashMap<Integer, StockItem> stockItemHashMap = ArrayUtil
          .getStockItemHashMap(data.getStockItems());
      for (VolatileItem volatileItem : data.getVolatileItems()) {
        StockItem stockItem = stockItemHashMap.get(volatileItem.getProductId());
        if (stockItem == null) {
          continue;
        }
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
      int missingItemsOnShoppingListCount = 0;
      for (MissingItem missingItem : data.getMissingItems()) {
        itemsMissingCount++;
        StockItem stockItem = stockItemHashMap.get(missingItem.getId());
        if (stockItem == null && !missingItem.getIsPartlyInStockBoolean()) {
          StockItem stockItemMissing = new StockItem(missingItem);
          if (stockItemsLive.getValue() != null) {
            stockItemsLive.getValue().add(stockItemMissing);
          }
        } else if (stockItem != null) {
          stockItem.setItemMissing(true);
          stockItem.setItemMissingAndPartlyInStock(missingItem.getIsPartlyInStockBoolean());
        }
        if (shoppingListItemsProductIds.contains(missingItem.getId())) {
          missingItemsOnShoppingListCount++;
        }
      }
      int itemsInStockCount = 0;
      double stockValue = 0;
      for (StockItem stockItem : data.getStockItems()) {
        if (!stockItem.isItemMissing() || stockItem.isItemMissingAndPartlyInStock()) {
          itemsInStockCount++;
          stockValue += stockItem.getValueDouble();
        }
      }

      itemsDueNextCountLive.setValue(itemsDueCount);
      itemsOverdueCountLive.setValue(itemsOverdueCount);
      itemsExpiredCountLive.setValue(itemsExpiredCount);
      itemsMissingCountLive.setValue(itemsMissingCount);
      itemsInStockCountLive.setValue(itemsInStockCount);
      itemsMissingShoppingListCountLive.setValue(missingItemsOnShoppingListCount);
      stockValueLive.setValue(stockValue);

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
            .getDaysFromNow(choreEntry.getNextEstimatedExecutionTime());
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

      alreadyLoadedFromDatabase = true;
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, this::showThrowableErrorMessage);
  }

  public void downloadData(boolean skipOfflineCheck) {
    if (!skipOfflineCheck && isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    dlHelper.updateData(
        this::onQueueEmpty,
        error -> onError(error, TAG),
        StockItem.class,
        ShoppingListItem.class,
        ShoppingList.class,
        Product.class,
        VolatileItem.class,
        Recipe.class,
        ChoreEntry.class,
        Task.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_VOLATILE, null);
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_TASKS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_CHORE_ENTRIES, null);
    editPrefs.apply();
    downloadData(true);
  }

  private void onQueueEmpty() {
    if (isOffline()) setOfflineLive(false);

    if (sharedPrefs.getInt(PREF.CURRENT_USER_ID, -1) == -1) {
      User.getCurrentUserId(dlHelper, id -> {
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
      }).perform(
          i -> loadFromDatabase(false),
          error -> loadFromDatabase(false),
          dlHelper.getUuid()
      );
    } else {
      loadFromDatabase(false);
    }
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
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

  public LiveData<String> getRecipesDescriptionTextLive() {
    return recipesDescriptionTextLive;
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

  public boolean isAlreadyLoadedFromDatabase() {
    return alreadyLoadedFromDatabase;
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

  public boolean getOverviewFabInfoShown() {
    return sharedPrefs.getBoolean(PREF.OVERVIEW_FAB_INFO_SHOWN, false);
  }

  public void setOverviewFabInfoShown() {
    sharedPrefs.edit().putBoolean(PREF.OVERVIEW_FAB_INFO_SHOWN, true).apply();
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
