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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;

public class MasterProductCatBarcodesViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatBarcodesViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final EventHandler eventHandler;
  private final MasterProductRepository repository;
  private final MasterProductFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<ProductBarcode>> productBarcodesLive;

  private List<ProductBarcode> productBarcodes;
  private List<QuantityUnit> quantityUnits;
  private List<Store> stores;

  public MasterProductCatBarcodesViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    eventHandler = new EventHandler();
    repository = new MasterProductRepository(application);
    args = startupArgs;

    productBarcodesLive = new MutableLiveData<>();
    infoFullscreenLive = new MutableLiveData<>();
  }

  public Product getFilledProduct() {
    return args.getProduct();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.productBarcodes = data.getBarcodes();
      this.quantityUnits = data.getQuantityUnits();
      this.stores = data.getStores();
      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        productBarcodesLive.setValue(filterBarcodes(productBarcodes));
      }
    }, error -> onError(error, TAG));
  }


  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            productBarcodesLive.setValue(filterBarcodes(productBarcodes));
          }
        }, error -> onError(error, null),
        forceUpdate,
        false,
        QuantityUnit.class,
        ProductBarcode.class,
        Store.class
    );
  }

  private ArrayList<ProductBarcode> filterBarcodes(List<ProductBarcode> barcodes) {
    ArrayList<ProductBarcode> filteredBarcodes = new ArrayList<>();
    assert args.getProduct() != null;
    int productId = args.getProduct().getId();
    for (ProductBarcode barcode : barcodes) {
      if (barcode.getProductIdInt() == productId) {
        filteredBarcodes.add(barcode);
      }
    }
    return filteredBarcodes;
  }

  public MutableLiveData<ArrayList<ProductBarcode>> getProductBarcodesLive() {
    return productBarcodesLive;
  }

  public List<QuantityUnit> getQuantityUnits() {
    return quantityUnits;
  }

  public List<Store> getStores() {
    return stores;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void showErrorMessage() {
    showMessage(getString(R.string.error_undefined));
  }

  @NonNull
  public EventHandler getEventHandler() {
    return eventHandler;
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

  public static class MasterProductCatBarcodesViewModelFactory implements
      ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductFragmentArgs args;

    public MasterProductCatBarcodesViewModelFactory(
        Application application,
        MasterProductFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductCatBarcodesViewModel(application, args);
    }
  }
}
