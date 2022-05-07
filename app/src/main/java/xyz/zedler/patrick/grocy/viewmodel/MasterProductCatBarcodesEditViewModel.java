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
import android.os.Bundle;
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
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatBarcodesEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FormDataMasterProductCatBarcodesEdit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

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
  private final MutableLiveData<Boolean> offlineLive;

  private List<Store> stores;
  private List<ProductBarcode> barcodes;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversion> unitConversions;

  private DownloadHelper.Queue currentQueueLoading;
  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;

  public MasterProductCatBarcodesEditViewModel(
      @NonNull Application application,
      @NonNull MasterProductCatBarcodesEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProductCatBarcodesEdit(application, startupArgs.getProduct());
    args = startupArgs;
    isActionEdit = startupArgs.getProductBarcode() != null;

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
  }

  public FormDataMasterProductCatBarcodesEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.stores = data.getStores();
      this.barcodes = data.getBarcodes();
      this.quantityUnits = data.getQuantityUnits();
      this.unitConversions = data.getConversions();
      fillWithProductBarcodeIfNecessary();
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

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateStores(dbChangedTime, stores -> this.stores = stores),
        dlHelper.updateQuantityUnitConversions(
            dbChangedTime, conversions -> this.unitConversions = conversions
        ), dlHelper.updateProductBarcodes(
            dbChangedTime, barcodes -> this.barcodes = barcodes
        ), dlHelper.updateQuantityUnits(
            dbChangedTime, quantityUnits -> this.quantityUnits = quantityUnits
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

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void saveItem() {
    if (!formData.isFormValid()) {
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
            showErrorMessage(error);
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
            showErrorMessage(error);
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
      return;
    }

    ProductBarcode productBarcode = args.getProductBarcode();
    assert productBarcode != null;

    formData.getBarcodeLive().setValue(productBarcode.getBarcode());

    if (productBarcode.hasAmount() && !productBarcode.hasQuId()) {
      double amount = productBarcode.getAmountDouble();
      formData.getAmountLive().setValue(NumUtil.trim(amount));
    }

    setProductQuantityUnitsAndFactors(args.getProduct());

    if (productBarcode.hasQuId()) {
      QuantityUnit quantityUnit = getQuantityUnit(productBarcode.getQuIdInt());
      if (productBarcode.hasAmount()) {
        double amount = productBarcode.getAmountDouble();
        formData.getAmountLive().setValue(NumUtil.trim(amount));
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
    QuantityUnit stock = getQuantityUnit(product.getQuIdStockInt());
    QuantityUnit purchase = getQuantityUnit(product.getQuIdPurchaseInt());

    if (stock == null || purchase == null) {
      showMessage(getString(R.string.error_loading_qus));
      return;
    }

    formData.setQuantityUnitPurchase(purchase);

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    ArrayList<Integer> quIdsInHashMap = new ArrayList<>();
    unitFactors.put(purchase, (double) -1);
    quIdsInHashMap.add(purchase.getId());
    if (!quIdsInHashMap.contains(stock.getId())) {
      unitFactors.put(stock, product.getQuFactorPurchaseToStockDouble());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (product.getId() != conversion.getProductIdInt()) {
        continue;
      }
      QuantityUnit unit = getQuantityUnit(conversion.getToQuId());
      if (unit == null || quIdsInHashMap.contains(unit.getId())) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
      quIdsInHashMap.add(unit.getId());
    }
    formData.getQuantityUnitsFactorsLive().setValue(unitFactors);
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
        this::showErrorMessage
    );
  }

  private QuantityUnit getQuantityUnit(int id) {
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == id) {
        return quantityUnit;
      }
    }
    return null;
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

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
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
