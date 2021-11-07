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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.repository.ChooseProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class ChooseProductViewModel extends BaseViewModel {

  private static final String TAG = ChooseProductViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final ChooseProductRepository repository;

  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<Product>> displayedItemsLive;
  private final MutableLiveData<String> productNameLive;
  private final MutableLiveData<String> offHelpText;
  private final MutableLiveData<String> createProductTextLive;
  private final MutableLiveData<Boolean> productNameAlreadyExists;

  private final String barcode;
  private ArrayList<Product> products;
  private final HashMap<String, Product> productHashMap;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  public ChooseProductViewModel(@NonNull Application application, String barcode) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    displayHelpLive = new MutableLiveData<>(false);
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new ChooseProductRepository(application);

    offlineLive = new MutableLiveData<>(false);
    displayedItemsLive = new MutableLiveData<>();
    productNameLive = new MutableLiveData<>();
    offHelpText = new MutableLiveData<>();
    createProductTextLive = new MutableLiveData<>(getString(R.string.msg_create_new_product));
    productNameAlreadyExists = new MutableLiveData<>(false);

    this.barcode = barcode;
    products = new ArrayList<>();
    productHashMap = new HashMap<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase((products) -> {
      this.products = products;
      productHashMap.clear();
      for (Product product : products) {
        productHashMap.put(product.getName().toLowerCase(), product);
      }
      displayItems();
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
      dlHelper.getTimeDbChanged(
          this::downloadData,
          () -> onDownloadError(null)
      );
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(dlHelper.updateProducts(dbChangedTime, products -> {
      this.products = products;
      productHashMap.clear();
      for (Product product : products) {
        productHashMap.put(product.getName().toLowerCase(), product);
      }
    }));


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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    repository.updateDatabase(
        products, this::displayItems
    );
    boolean productNameFilled = productNameLive.getValue() != null
        && !productNameLive.getValue().isEmpty();
    if(isOpenFoodFactsEnabled() && !productNameFilled) {
      dlHelper.getOpenFoodFactsProductName(
          barcode,
          productName -> {
            if (productName != null && !productName.isEmpty()) {
              productNameLive.setValue(productName);
              offHelpText.setValue(getString(R.string.msg_product_name_off));
            } else {
              offHelpText.setValue(getString(R.string.msg_product_name_off_empty));
              sendEvent(Event.FOCUS_INVALID_VIEWS);
            }
          },
          error-> {
            offHelpText.setValue(getString(R.string.msg_product_name_off_error));
            sendEvent(Event.FOCUS_INVALID_VIEWS);
          }
      );
    } else if (!productNameFilled) {
      sendEvent(Event.FOCUS_INVALID_VIEWS);
    }
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

  public void displayItems() {
    String productName = productNameLive.getValue();

    if (productName == null || productName.isEmpty()) {
      SortUtil.sortProductsByName(products, true);
      displayedItemsLive.setValue(products);
      createProductTextLive.setValue(getString(R.string.msg_create_new_product));
      productNameAlreadyExists.setValue(false);
      return;
    }

    ArrayList<Product> suggestions = new ArrayList<>(productHashMap.keySet().size());
    List<ExtractedResult> results = FuzzySearch.extractSorted(
        productName.toLowerCase(),
        productHashMap.keySet(),
        20
    );
    for (ExtractedResult result : results) {
      suggestions.add(productHashMap.get(result.getString()));
    }

    displayedItemsLive.setValue(suggestions);
    createProductTextLive.setValue(
        getApplication().getString(R.string.msg_create_new_product_filled, productName)
    );
    productNameAlreadyExists.setValue(productHashMap.containsKey(productName.toLowerCase()));
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
  public MutableLiveData<ArrayList<Product>> getDisplayedItemsLive() {
    return displayedItemsLive;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    displayHelpLive.setValue(displayHelpLive.getValue() == null || !displayHelpLive.getValue());
  }

  public MutableLiveData<String> getProductNameLive() {
    return productNameLive;
  }

  public MutableLiveData<String> getOffHelpText() {
    return offHelpText;
  }

  public MutableLiveData<String> getCreateProductTextLive() {
    return createProductTextLive;
  }

  public MutableLiveData<Boolean> getProductNameAlreadyExists() {
    return productNameAlreadyExists;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
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

  public static class ChooseProductViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final String barcode;

    public ChooseProductViewModelFactory(Application application, String barcode) {
      this.application = application;
      this.barcode = barcode;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new ChooseProductViewModel(application, barcode);
    }
  }
}
