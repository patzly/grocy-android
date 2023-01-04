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
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FormDataMasterProductCatQuantityUnit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterProductCatQuantityUnitViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatQuantityUnitViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final MasterProductRepository repository;
  private final FormDataMasterProductCatQuantityUnit formData;
  private final MasterProductFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversion> conversions;

  private NetworkQueue currentQueueLoading;
  private final boolean debug;
  private final boolean isActionEdit;

  public MasterProductCatQuantityUnitViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProductCatQuantityUnit(application, getBeginnerModeEnabled());
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
  }

  public FormDataMasterProductCatQuantityUnit getFormData() {
    return formData;
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  public Product getFilledProduct() {
    return formData.fillProduct(args.getProduct());
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.quantityUnits = data.getQuantityUnits();
      this.conversions = data.getConversions();
      formData.getQuantityUnitsLive().setValue(this.quantityUnits);
      formData.fillWithProductIfNecessary(args.getProduct());
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

    NetworkQueue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateQuantityUnits(dbChangedTime, quantityUnits -> {
          this.quantityUnits = quantityUnits;
          formData.getQuantityUnitsLive().setValue(quantityUnits);
        }),
        dlHelper.updateQuantityUnitConversions(
            dbChangedTime, conversions -> this.conversions = conversions
        )
    );
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
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    formData.fillWithProductIfNecessary(args.getProduct());
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

  public void showQuBottomSheet(int type) {
    if (type == FormDataMasterProductCatQuantityUnit.STOCK && !isQuantityUnitStockChangeable()) {
      showMessage(getString(R.string.msg_help_qu_stock));
      return;
    }
    List<QuantityUnit> quantityUnitsAllowed;
    if (type == FormDataMasterProductCatQuantityUnit.STOCK && isActionEdit
        && isQuantityUnitStockChangeable()) {
      QuantityUnit quStockOld = QuantityUnit
          .getFromId(this.quantityUnits, getFilledProduct().getQuIdStockInt());
      quantityUnitsAllowed = new ArrayList<>();
      ArrayList<Integer> addedQuIds = new ArrayList<>();
      for (QuantityUnitConversion conversion : conversions) {
        if (conversion.getProductIdInt() == getFilledProduct().getId()
            && conversion.getFromQuId() == quStockOld.getId()
            && !addedQuIds.contains(conversion.getToQuId())) {
          QuantityUnit quantityUnit = QuantityUnit
              .getFromId(this.quantityUnits, conversion.getToQuId());
          if (quantityUnit != null) {
            quantityUnitsAllowed.add(quantityUnit);
            addedQuIds.add(quantityUnit.getId());
          }
        }
      }
      if (!addedQuIds.contains(quStockOld.getId())) {
        quantityUnitsAllowed.add(quStockOld);
      }
    } else {
      quantityUnitsAllowed = this.quantityUnits;
    }
    if (quantityUnitsAllowed == null || quantityUnitsAllowed.isEmpty()) {
      showErrorMessage();
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.QUANTITY_UNITS,
        new ArrayList<>(quantityUnitsAllowed)
    );
    QuantityUnit quantityUnit;
    if (type == FormDataMasterProductCatQuantityUnit.STOCK) {
      quantityUnit = formData.getQuStockLive().getValue();
    } else {
      quantityUnit = formData.getQuPurchaseLive().getValue();
    }
    int quId = quantityUnit != null ? quantityUnit.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, quId);
    bundle.putInt(FormDataMasterProductCatQuantityUnit.QUANTITY_UNIT_TYPE, type);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
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

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean isQuantityUnitStockChangeable() {
    return VersionUtil.isGrocyServerMin330(sharedPrefs);
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

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MasterProductCatQuantityUnitViewModelFactory implements
      ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductFragmentArgs args;

    public MasterProductCatQuantityUnitViewModelFactory(
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
      return (T) new MasterProductCatQuantityUnitViewModel(application, args);
    }
  }
}
