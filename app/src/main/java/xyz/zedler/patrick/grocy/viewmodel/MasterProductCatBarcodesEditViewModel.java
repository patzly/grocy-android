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
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.form.FormDataMasterProductCatBarcodesEdit;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatBarcodesEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterProductCatBarcodesEditViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatBarcodesEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterProductRepository repository;
  private final FormDataMasterProductCatBarcodesEdit formData;
  private final MasterProductCatBarcodesEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<Store> stores;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private List<QuantityUnitConversion> unitConversions;

  private NetworkQueue currentQueueLoading;
  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;
  private final int maxDecimalPlacesAmount;

  public MasterProductCatBarcodesEditViewModel(
      @NonNull Application application,
      @NonNull MasterProductCatBarcodesEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProductCatBarcodesEdit(application, startupArgs.getProduct());
    args = startupArgs;
    isActionEdit = startupArgs.getProductBarcode() != null;
    infoFullscreenLive = new MutableLiveData<>();
  }

  public FormDataMasterProductCatBarcodesEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.stores = data.getStores();
      formData.getBarcodesLive().setValue(getBarcodes(data.getBarcodes()));
      this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.unitConversions = data.getConversions();
      fillWithProductBarcodeIfNecessary();
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
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
      dlHelper.getTimeDbChanged(this::downloadData, error -> onError(error, TAG));
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(this::onQueueEmpty, error -> onError(error, TAG));
    queue.append(
        Store.updateStores(dlHelper, dbChangedTime, stores -> this.stores = stores),
        QuantityUnitConversion.updateQuantityUnitConversions(
            dlHelper, dbChangedTime, conversions -> this.unitConversions = conversions
        ), ProductBarcode.updateProductBarcodes(
            dlHelper,
            dbChangedTime,
            barcodes -> formData.getBarcodesLive().setValue(getBarcodes(barcodes))
        ), QuantityUnit.updateQuantityUnits(
            dlHelper,
            dbChangedTime,
            quantityUnits -> this.quantityUnitHashMap = ArrayUtil
                .getQuantityUnitsHashMap(quantityUnits)
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
      return;
    }
    fillWithProductBarcodeIfNecessary();
  }

  public void saveItem() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    ProductBarcode productBarcode = formData.fillProductBarcode(args.getProductBarcode());
    JSONObject jsonObject = ProductBarcode.getJsonFromProductBarcode(productBarcode, debug, TAG);

    if (isActionEdit) {
      dlHelper.put(
          grocyApi.getObject(ENTITY.PRODUCT_BARCODES, productBarcode.getId()),
          jsonObject,
          response -> navigateUp(),
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(ENTITY.PRODUCT_BARCODES),
          jsonObject,
          response -> navigateUp(),
          error -> {
            showNetworkErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveItem: " + error);
            }
          }
      );
    }
  }

  private void fillWithProductBarcodeIfNecessary() {
    if (formData.isFilledWithProductBarcode()) {
      return;
    } else if(!isActionEdit) {
      setProductQuantityUnitsAndFactors(args.getProduct());
      sendEvent(Event.FOCUS_INVALID_VIEWS);
      return;
    }

    ProductBarcode productBarcode = args.getProductBarcode();
    assert productBarcode != null;

    formData.getBarcodeLive().setValue(productBarcode.getBarcode());

    if (productBarcode.hasAmount() && !productBarcode.hasQuId()) {
      double amount = productBarcode.getAmountDouble();
      formData.getAmountLive().setValue(NumUtil.trimAmount(amount, maxDecimalPlacesAmount));
    }

    setProductQuantityUnitsAndFactors(args.getProduct());

    if (productBarcode.hasQuId()) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(productBarcode.getQuIdInt());
      if (productBarcode.hasAmount()) {
        double amount = productBarcode.getAmountDouble();
        formData.getAmountLive().setValue(NumUtil.trimAmount(amount, maxDecimalPlacesAmount));
      }
      formData.getQuantityUnitLive().setValue(quantityUnit);
    }

    if (productBarcode.hasStoreId()) {
      formData.getStoreLive().setValue(getStore(productBarcode.getStoreIdInt()));
    }
    formData.getNoteLive().setValue(productBarcode.getNote());
    formData.setFilledWithProductBarcode(true);
  }

  private void setProductQuantityUnitsAndFactors(Product product) {
    try {
      HashMap<QuantityUnit, Double> unitFactors = QuantityUnitConversionUtil.getUnitFactors(
          getApplication(),
          quantityUnitHashMap,
          unitConversions,
          product,
          false
      );
      formData.getQuantityUnitsFactorsLive().setValue(unitFactors);
      QuantityUnit purchase = quantityUnitHashMap.get(product.getQuIdPurchaseInt());
      formData.setQuantityUnitPurchase(purchase);
    } catch (IllegalArgumentException e) {
      showMessage(e.getMessage());
    }
  }

  public void onBarcodeRecognized(String barcode) {
    formData.getBarcodeLive().setValue(barcode);
  }

  public void showQuantityUnitsBottomSheet() {
    ArrayList<QuantityUnit> quantityUnits = formData.getQuantityUnitsLive().getValue();
    if (quantityUnits == null) return;
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, quantityUnits);
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    QuantityUnit quantityUnit = formData.getQuantityUnitLive().getValue();
    bundle.putInt(ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public void showStoresBottomSheet() {
    if (stores == null || stores.isEmpty()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, new ArrayList<>(stores));
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        formData.getStoreLive().getValue() != null
            ? formData.getStoreLive().getValue().getId()
            : -1
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new StoresBottomSheet(), bundle);
  }

  public void deleteItem() {
    if (!isActionEdit()) {
      return;
    }
    ProductBarcode productBarcode = args.getProductBarcode();
    assert productBarcode != null;
    dlHelper.delete(
        grocyApi.getObject(
            ENTITY.PRODUCT_BARCODES,
            productBarcode.getId()
        ),
        response -> navigateUp(),
        this::showNetworkErrorMessage
    );
  }

  private List<String> getBarcodes(List<ProductBarcode> barcodes) {
    ArrayList<String> barcodeStrings = new ArrayList<>();
    for (ProductBarcode barcode : barcodes) {
      barcodeStrings.add(barcode.getBarcode());
    }
    if (isActionEdit() && (args.getProductBarcode() != null)) {
      barcodeStrings.remove(args.getProductBarcode().getBarcode());
    }
    return barcodeStrings;
  }

  private Store getStore(int id) {
    for (Store store : stores) {
      if (store.getId() == id) {
        return store;
      }
    }
    return null;
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
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

  public static class MasterProductCatBarcodesEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductCatBarcodesEditFragmentArgs args;

    public MasterProductCatBarcodesEditViewModelFactory(
        Application application,
        MasterProductCatBarcodesEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductCatBarcodesEditViewModel(application, args);
    }
  }
}
