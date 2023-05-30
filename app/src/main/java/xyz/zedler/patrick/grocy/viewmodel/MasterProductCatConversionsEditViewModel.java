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
import java.util.List;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.form.FormDataMasterProductCatConversionsEdit;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatConversionsEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterProductCatConversionsEditViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatConversionsEditViewModel.class.getSimpleName();
  public static final String QUANTITY_UNIT_IS_FROM = "qu_from";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterProductRepository repository;
  private final FormDataMasterProductCatConversionsEdit formData;
  private final MasterProductCatConversionsEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversion> unitConversions;

  private NetworkQueue currentQueueLoading;
  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;

  public MasterProductCatConversionsEditViewModel(
      @NonNull Application application,
      @NonNull MasterProductCatConversionsEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProductCatConversionsEdit(application, startupArgs.getProduct());
    args = startupArgs;
    isActionEdit = startupArgs.getConversion() != null;
    infoFullscreenLive = new MutableLiveData<>();
  }

  public FormDataMasterProductCatConversionsEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.quantityUnits = data.getQuantityUnits();
      this.unitConversions = data.getConversions();
      fillWithConversionIfNecessary();
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
        QuantityUnitConversion.updateQuantityUnitConversions(
            dlHelper, dbChangedTime, conversions -> this.unitConversions = conversions
        ), QuantityUnit.updateQuantityUnits(
            dlHelper, dbChangedTime, quantityUnits -> this.quantityUnits = quantityUnits
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
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
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
    fillWithConversionIfNecessary();
  }

  public void saveItem() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    QuantityUnitConversion conversion = formData.fillConversion(args.getConversion());
    JSONObject jsonObject = QuantityUnitConversion.getJsonFromConversion(conversion, debug, TAG);

    if (isActionEdit) {
      dlHelper.put(
          grocyApi.getObject(ENTITY.QUANTITY_UNIT_CONVERSIONS, conversion.getId()),
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
          grocyApi.getObjects(ENTITY.QUANTITY_UNIT_CONVERSIONS),
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

  private void fillWithConversionIfNecessary() {
    if (formData.isFilledWithConversion()) {
      return;
    } else if(!isActionEdit) {
      formData.getQuantityUnitsLive().setValue(quantityUnits);
      QuantityUnit quStock = QuantityUnit
          .getFromId(quantityUnits, args.getProduct().getQuIdStockInt());
      formData.getQuantityUnitFromLive().setValue(quStock);
      formData.getConversionsLive().setValue(unitConversions);
      return;
    }

    QuantityUnitConversion conversion = args.getConversion();
    assert conversion != null;

    formData.getQuantityUnitsLive().setValue(quantityUnits);
    formData.getQuantityUnitFromLive().setValue(
        QuantityUnit.getFromId(quantityUnits, conversion.getFromQuId())
    );
    formData.getQuantityUnitToLive().setValue(
        QuantityUnit.getFromId(quantityUnits, conversion.getToQuId())
    );
    formData.getConversionsLive().setValue(unitConversions);
    formData.getFactorLive().setValue(NumUtil.trimAmount(
        conversion.getFactor(),
        sharedPrefs.getInt(STOCK.DECIMAL_PLACES_AMOUNT, SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT)
    ));
    formData.setFilledWithConversionId(conversion.getId());
  }

  public void showQuantityUnitsBottomSheet(boolean from) {
    List<QuantityUnit> quantityUnits = formData.getQuantityUnitsLive().getValue();
    if (quantityUnits == null) return;
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.QUANTITY_UNITS, new ArrayList<>(quantityUnits));
    bundle.putBoolean(QUANTITY_UNIT_IS_FROM, from);
    QuantityUnit quantityUnit;
    if (from) {
      quantityUnit = formData.getQuantityUnitFromLive().getValue();
    } else {
      quantityUnit = formData.getQuantityUnitToLive().getValue();
    }
    bundle.putInt(ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public void deleteItem() {
    if (!isActionEdit()) {
      return;
    }
    QuantityUnitConversion conversion = args.getConversion();
    assert conversion != null;
    dlHelper.delete(
        grocyApi.getObject(
            ENTITY.QUANTITY_UNIT_CONVERSIONS,
            conversion.getId()
        ),
        response -> navigateUp(),
        this::showNetworkErrorMessage
    );
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

  public static class MasterProductCatConversionsEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductCatConversionsEditFragmentArgs args;

    public MasterProductCatConversionsEditViewModelFactory(
        Application application,
        MasterProductCatConversionsEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterProductCatConversionsEditViewModel(application, args);
    }
  }
}
