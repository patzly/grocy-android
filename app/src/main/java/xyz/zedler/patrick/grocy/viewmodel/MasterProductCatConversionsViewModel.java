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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.repository.MasterProductCatConversionsEditRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class MasterProductCatConversionsViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatConversionsViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final EventHandler eventHandler;
  private final MasterProductCatConversionsEditRepository repository;
  private final MasterProductFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<QuantityUnitConversion>> quantityUnitConversionsLive;

  private ArrayList<QuantityUnitConversion> unitConversions;
  private ArrayList<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;

  private DownloadHelper.Queue currentQueueLoading;
  private final boolean debug;

  public MasterProductCatConversionsViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    eventHandler = new EventHandler();
    repository = new MasterProductCatConversionsEditRepository(application);
    args = startupArgs;

    quantityUnitConversionsLive = new MutableLiveData<>();
    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
  }

  public Product getFilledProduct() {
    return args.getProduct();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase((qUs, conversions) -> {
          this.quantityUnits = qUs;
          this.unitConversions = conversions;
          this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);
          quantityUnitConversionsLive.setValue(filterConversions(conversions));
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
        dlHelper.updateQuantityUnits(dbChangedTime, qUs -> {
          this.quantityUnits = qUs;
          this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);
        }), dlHelper.updateQuantityUnitConversions(
            dbChangedTime,
            conversions -> this.unitConversions = conversions
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    repository.updateDatabase(quantityUnits, unitConversions, () -> {});
    quantityUnitConversionsLive.setValue(filterConversions(unitConversions));
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

  private ArrayList<QuantityUnitConversion> filterConversions(ArrayList<QuantityUnitConversion> conversions) {
    ArrayList<QuantityUnitConversion> filteredConversions = new ArrayList<>();
    assert args.getProduct() != null;
    int productId = args.getProduct().getId();
    for (QuantityUnitConversion conversion : conversions) {
      if (conversion.getProductId() == productId) {
        filteredConversions.add(conversion);
      }
    }
    return filteredConversions;
  }

  public MutableLiveData<ArrayList<QuantityUnitConversion>> getQuantityUnitConversionsLive() {
    return quantityUnitConversionsLive;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
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

  public static class MasterProductCatConversionsViewModelFactory implements
      ViewModelProvider.Factory {

    private final Application application;
    private final MasterProductFragmentArgs args;

    public MasterProductCatConversionsViewModelFactory(
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
      return (T) new MasterProductCatConversionsViewModel(application, args);
    }
  }
}
