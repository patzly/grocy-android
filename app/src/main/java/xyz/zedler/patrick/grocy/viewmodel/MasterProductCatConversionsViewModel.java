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
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;

public class MasterProductCatConversionsViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatConversionsViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final EventHandler eventHandler;
  private final MasterProductRepository repository;
  private final MasterProductFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<QuantityUnitConversion>> quantityUnitConversionsLive;

  private List<QuantityUnitConversion> unitConversions;
  private List<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;

  public MasterProductCatConversionsViewModel(
      @NonNull Application application,
      @NonNull MasterProductFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    eventHandler = new EventHandler();
    repository = new MasterProductRepository(application);
    args = startupArgs;

    quantityUnitConversionsLive = new MutableLiveData<>();
    infoFullscreenLive = new MutableLiveData<>();
  }

  public Product getFilledProduct() {
    return args.getProduct();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.quantityUnits = data.getQuantityUnits();
      this.unitConversions = data.getConversions();
      this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(this.quantityUnits);
      quantityUnitConversionsLive.setValue(filterConversions(this.unitConversions));
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData() {
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }

    dlHelper.updateData(
        () -> loadFromDatabase(false),
        error -> onError(error, TAG),
        QuantityUnit.class,
        QuantityUnitConversion.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.apply();
    downloadData();
  }

  private ArrayList<QuantityUnitConversion> filterConversions(List<QuantityUnitConversion> conversions) {
    ArrayList<QuantityUnitConversion> filteredConversions = new ArrayList<>();
    if (args.getProduct() == null) {
      showErrorMessage();
      return filteredConversions;
    }
    int productId = args.getProduct().getId();
    for (QuantityUnitConversion conversion : conversions) {
      if (conversion.getProductIdInt() == productId) {
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
