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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.StockLogEntry;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class StockReportSpendingsViewModel extends BaseViewModel {

  private static final String TAG = StockReportSpendingsViewModel.class.getSimpleName();

  public static final int GROUP_BY_PRODUCT = 0;
  public static final int GROUP_BY_PRODUCT_GROUP = 1;
  public static final int GROUP_BY_STORE = 2;

  private final DownloadHelper dlHelper;
  private final AppDatabase appDatabase;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<List<SpendingItem>> spendingItemsLive;
  private final MutableLiveData<String> totalSpendLive;
  private final MutableLiveData<Integer> groupByLive;

  private List<StockLogEntry> purchaseEntries;
  private List<Product> products;
  private List<ProductGroup> productGroups;
  private List<Store> stores;

  public static class SpendingItem {
    public final String name;
    public final double total;

    public SpendingItem(String name, double total) {
      this.name = name;
      this.total = total;
    }
  }

  public StockReportSpendingsViewModel(@NonNull Application application) {
    super(application);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    appDatabase = AppDatabase.getAppDatabase(application);
    infoFullscreenLive = new MutableLiveData<>();
    spendingItemsLive = new MutableLiveData<>();
    totalSpendLive = new MutableLiveData<>();
    groupByLive = new MutableLiveData<>(GROUP_BY_PRODUCT);
  }

  public void loadData(String startDate, String endDate) {
    // Products, product groups and stores are only read from the database below, so make sure
    // they are actually there — otherwise group labels fall back to raw ids.
    dlHelper.updateData(
        updated -> loadFromDatabase(startDate, endDate),
        error -> onError(error, TAG),
        false,
        true,
        Product.class,
        ProductGroup.class,
        Store.class
    );
  }

  private void loadFromDatabase(String startDate, String endDate) {
    Single.zip(
        appDatabase.productDao().getProducts(),
        appDatabase.productGroupDao().getProductGroups(),
        appDatabase.storeDao().getStores(),
        (p, pg, s) -> {
          this.products = p;
          this.productGroups = pg;
          this.stores = s;
          return true;
        }
    )
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .doOnSuccess(ok -> fetchPurchaseEntries(startDate, endDate))
    .doOnError(e -> onError(e, TAG))
    .onErrorComplete()
    .subscribe();
  }

  private void fetchPurchaseEntries(String startDate, String endDate) {
    NetworkQueue queue = dlHelper.newQueue(
        updated -> {
          if (isOffline()) setOfflineLive(false);
          aggregateAndDisplay();
        },
        error -> onError(error, TAG)
    );
    queue.append(StockLogEntry.getPurchaseEntries(
        dlHelper, startDate, endDate,
        items -> this.purchaseEntries = items,
        null
    ));
    queue.start();
  }

  public void refreshWithGroupBy() {
    aggregateAndDisplay();
  }

  private void aggregateAndDisplay() {
    if (purchaseEntries == null) return;
    int groupBy = groupByLive.getValue() != null ? groupByLive.getValue() : GROUP_BY_PRODUCT;

    Map<String, Double> aggregated = new HashMap<>();

    for (StockLogEntry entry : purchaseEntries) {
      String key = resolveGroupKey(entry, groupBy);
      double cost = totalCost(entry);
      aggregated.put(key, aggregated.getOrDefault(key, 0.0) + cost);
    }

    List<SpendingItem> items = new ArrayList<>();
    double total = 0;
    for (Map.Entry<String, Double> e : aggregated.entrySet()) {
      items.add(new SpendingItem(e.getKey(), e.getValue()));
      total += e.getValue();
    }
    Collections.sort(items, (a, b) -> Double.compare(b.total, a.total));

    if (items.isEmpty()) {
      infoFullscreenLive.setValue(new InfoFullscreen(InfoFullscreen.INFO_EMPTY_STOCK));
      spendingItemsLive.setValue(new ArrayList<>());
      totalSpendLive.setValue(formatCurrency(0));
    } else {
      infoFullscreenLive.setValue(null);
      spendingItemsLive.setValue(items);
      totalSpendLive.setValue(formatCurrency(total));
    }
  }

  private double totalCost(StockLogEntry entry) {
    try {
      String priceStr = entry.getPrice();
      if (priceStr == null || priceStr.isEmpty()) return 0;
      double amount = Double.parseDouble(entry.getAmount());
      double price = Double.parseDouble(priceStr);
      return Math.abs(amount) * price;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String resolveGroupKey(StockLogEntry entry, int groupBy) {
    switch (groupBy) {
      case GROUP_BY_PRODUCT: {
        if (products == null) return String.valueOf(entry.getProductId());
        for (Product p : products) {
          if (p.getId() == entry.getProductId()) return p.getName();
        }
        return String.valueOf(entry.getProductId());
      }
      case GROUP_BY_PRODUCT_GROUP: {
        if (products == null || productGroups == null) return getString(R.string.subtitle_unknown);
        String groupId = null;
        for (Product p : products) {
          if (p.getId() == entry.getProductId()) {
            groupId = p.getProductGroupId();
            break;
          }
        }
        if (groupId == null) return getString(R.string.subtitle_none);
        for (ProductGroup g : productGroups) {
          if (String.valueOf(g.getId()).equals(groupId)) return g.getName();
        }
        return getString(R.string.subtitle_unknown);
      }
      case GROUP_BY_STORE: {
        String locationId = entry.getShoppingLocationId();
        if (locationId == null || locationId.isEmpty()) return getString(R.string.subtitle_none);
        if (stores == null) return locationId;
        try {
          int lid = Integer.parseInt(locationId);
          for (Store s : stores) {
            if (s.getId() == lid) return s.getName();
          }
        } catch (NumberFormatException ignored) {}
        return locationId;
      }
      default:
        return String.valueOf(entry.getProductId());
    }
  }

  public String formatCurrency(double amount) {
    String currency = getSharedPrefs().getString(PREF.CURRENCY, "");
    DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.getDefault()));
    return df.format(amount) + (currency.isEmpty() ? "" : " " + currency);
  }

  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public MutableLiveData<List<SpendingItem>> getSpendingItemsLive() {
    return spendingItemsLive;
  }

  public MutableLiveData<String> getTotalSpendLive() {
    return totalSpendLive;
  }

  public MutableLiveData<Integer> getGroupByLive() {
    return groupByLive;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
