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
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.form.FormDataMasterProductCatQuantityUnit;
import xyz.zedler.patrick.grocy.fragment.MasterProductCatQuantityUnitFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.StockLogEntry;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class MasterProductCatQuantityUnitViewModel extends BaseViewModel {

  private static final String TAG = MasterProductCatQuantityUnitViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final MasterProductRepository repository;
  private final FormDataMasterProductCatQuantityUnit formData;
  private final MasterProductCatQuantityUnitFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> isQuantityUnitStockChangeableLive;
  private final MutableLiveData<Boolean> hasProductAlreadyStockTransactionsLive;

  private List<QuantityUnit> quantityUnits;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private List<QuantityUnitConversionResolved> conversionsResolved;

  private Runnable queueEmptyAction;
  private final boolean isActionEdit;

  public MasterProductCatQuantityUnitViewModel(
      @NonNull Application application,
      @NonNull MasterProductCatQuantityUnitFragmentArgs startupArgs
  ) {
    super(application);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    repository = new MasterProductRepository(application);
    formData = new FormDataMasterProductCatQuantityUnit(application, sharedPrefs, getBeginnerModeEnabled());
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);
    isQuantityUnitStockChangeableLive = new MutableLiveData<>(VersionUtil.isGrocyServerMin330(sharedPrefs));
    hasProductAlreadyStockTransactionsLive = new MutableLiveData<>(false);

    infoFullscreenLive = new MutableLiveData<>();
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
      this.quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.conversionsResolved = data.getConversionsResolved();
      formData.setQuantityUnitHashMap(this.quantityUnitHashMap);
      if (downloadAfterLoading) {
        downloadData(false);
      } else {
        formData.fillWithProductIfNecessary(args.getProduct());
        updateHasProductAlreadyStockTransactions();
        removeNotAllowedQuantityUnits();
        if (queueEmptyAction != null) {
          queueEmptyAction.run();
          queueEmptyAction = null;
        }
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) {
            loadFromDatabase(false);
          } else {
            formData.fillWithProductIfNecessary(args.getProduct());
            updateHasProductAlreadyStockTransactions();
            removeNotAllowedQuantityUnits();
            if (queueEmptyAction != null) {
              queueEmptyAction.run();
              queueEmptyAction = null;
            }
          }
        },
        error -> onError(error, TAG),
        forceUpdate,
        false,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class
    );
  }

  private void removeNotAllowedQuantityUnits() {
    List<QuantityUnit> selectableStock = getSelectableQuantityUnits(
        FormDataMasterProductCatQuantityUnit.STOCK, false);
    QuantityUnit currentStock = formData.getQuStockLive().getValue();
    assert isQuantityUnitStockChangeableLive.getValue() != null;
    if (!selectableStock.isEmpty() && currentStock != null
        && !selectableStock.contains(currentStock)
        && isQuantityUnitStockChangeableLive.getValue()) {
      formData.getQuStockLive().setValue(null);
    }
    List<QuantityUnit> selectablePurchase = getSelectableQuantityUnits(
        FormDataMasterProductCatQuantityUnit.PURCHASE, false);
    QuantityUnit currentPurchase = formData.getQuPurchaseLive().getValue();
    if (!selectablePurchase.isEmpty() && currentPurchase != null
        && !selectablePurchase.contains(currentPurchase)) {
      formData.getQuPurchaseLive().setValue(null);
    }
    if (!VersionUtil.isGrocyServerMin400(sharedPrefs)) return;
    List<QuantityUnit> selectableConsume = getSelectableQuantityUnits(
        FormDataMasterProductCatQuantityUnit.CONSUME, false);
    QuantityUnit currentConsume = formData.getQuConsumeLive().getValue();
    if (!selectableConsume.isEmpty() && currentConsume != null
        && !selectableConsume.contains(currentConsume)) {
      formData.getQuConsumeLive().setValue(null);
    }
    List<QuantityUnit> selectablePrice = getSelectableQuantityUnits(
        FormDataMasterProductCatQuantityUnit.PRICE, false);
    QuantityUnit currentPrice = formData.getQuPriceLive().getValue();
    if (!selectablePrice.isEmpty() && currentPrice != null
        && !selectablePrice.contains(currentPrice)) {
      formData.getQuPriceLive().setValue(null);
    }
  }

  public List<QuantityUnit> getSelectableQuantityUnits(String type, boolean showBottomSheet) {
    assert hasProductAlreadyStockTransactionsLive.getValue() != null
        && isQuantityUnitStockChangeableLive.getValue() != null;
    assert args.getProduct() != null;
    List<QuantityUnit> quantityUnitsAllowed;
    boolean displayNewOption;
    if (!isActionEdit) {
      // On product creation, all units are allowed.
      quantityUnitsAllowed = this.quantityUnits;
      displayNewOption = true;
    } else if (VersionUtil.isGrocyServerMin400(sharedPrefs)) {
      // With Grocy server v4.0.0 and higher, only units for
      // which a conversion exist, can be selected.
      // Transitive conversions allowed.
      if (hasProductAlreadyStockTransactionsLive.getValue()
          || !type.equals(FormDataMasterProductCatQuantityUnit.STOCK)) {
        QuantityUnit quStockOld = quantityUnitHashMap.get(args.getProduct().getQuIdStockInt());
        quantityUnitsAllowed = new ArrayList<>();
        ArrayList<Integer> addedQuIds = new ArrayList<>();
        for (QuantityUnitConversion conversion : conversionsResolved) {
          if ((conversion.getProductIdInt() == args.getProduct().getId())
              && quStockOld != null && conversion.getFromQuId() == quStockOld.getId()
              && !addedQuIds.contains(conversion.getToQuId())) {
            QuantityUnit quantityUnit = QuantityUnit
                .getFromId(this.quantityUnits, conversion.getToQuId());
            if (quantityUnit != null) {
              quantityUnitsAllowed.add(quantityUnit);
              addedQuIds.add(quantityUnit.getId());
            }
          }
        }
        displayNewOption = false;
      } else {
        quantityUnitsAllowed = this.quantityUnits;
        displayNewOption = true;
      }
    } else {
      // Old behavior: With Grocy server version until 3.3.2, stock unit can be edited
      // after creation as long as no transactions have been made or purchase unit can always be
      // edited because of available stock to purchase factor.
      // No transitive conversions allowed (conversionsResolved only contain normal conversions
      // with server version < v4).
      if (type.equals(FormDataMasterProductCatQuantityUnit.STOCK)
          && hasProductAlreadyStockTransactionsLive.getValue()) {
        QuantityUnit quStockOld = quantityUnitHashMap.get(args.getProduct().getQuIdStockInt());
        quantityUnitsAllowed = new ArrayList<>();
        ArrayList<Integer> addedQuIds = new ArrayList<>();
        for (QuantityUnitConversion conversion : conversionsResolved) {
          if ((conversion.getProductIdInt() == args.getProduct().getId()
              || !NumUtil.isStringInt(conversion.getProductId()))
              && quStockOld != null && conversion.getFromQuId() == quStockOld.getId()
              && !addedQuIds.contains(conversion.getToQuId())) {
            QuantityUnit quantityUnit = QuantityUnit
                .getFromId(this.quantityUnits, conversion.getToQuId());
            if (quantityUnit != null) {
              quantityUnitsAllowed.add(quantityUnit);
              addedQuIds.add(quantityUnit.getId());
            }
          }
        }
        if (quStockOld != null && !addedQuIds.contains(quStockOld.getId())) {
          quantityUnitsAllowed.add(quStockOld);
        }
        displayNewOption = false;
      } else {
        quantityUnitsAllowed = this.quantityUnits;
        displayNewOption = true;
      }
    }
    if (showBottomSheet) {
      if (type.equals(FormDataMasterProductCatQuantityUnit.STOCK)
          && !isQuantityUnitStockChangeableLive.getValue()) {
        showMessage(getString(R.string.msg_help_qu_stock));
        return quantityUnitsAllowed;
      } else if (quantityUnitsAllowed == null || quantityUnitsAllowed.isEmpty()) {
        showErrorMessage();
        return quantityUnitsAllowed;
      }
    } else {
      return quantityUnitsAllowed;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.QUANTITY_UNITS,
        new ArrayList<>(quantityUnitsAllowed)
    );
    QuantityUnit quantityUnit;
    switch (type) {
      case FormDataMasterProductCatQuantityUnit.STOCK:
        quantityUnit = formData.getQuStockLive().getValue();
        break;
      case FormDataMasterProductCatQuantityUnit.PURCHASE:
        quantityUnit = formData.getQuPurchaseLive().getValue();
        break;
      case FormDataMasterProductCatQuantityUnit.CONSUME:
        quantityUnit = formData.getQuConsumeLive().getValue();
        break;
      default:
        quantityUnit = formData.getQuPriceLive().getValue();
        break;
    }
    int quId = quantityUnit != null ? quantityUnit.getId() : -1;
    bundle.putBoolean(ARGUMENT.DISPLAY_NEW_OPTION, displayNewOption);
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, quId);
    bundle.putString(FormDataMasterProductCatQuantityUnit.QUANTITY_UNIT_TYPE, type);
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
    return quantityUnitsAllowed;
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

  public MutableLiveData<Boolean> getIsQuantityUnitStockChangeableLive() {
    return isQuantityUnitStockChangeableLive;
  }

  private void updateHasProductAlreadyStockTransactions() {
    if (!isActionEdit) {
      hasProductAlreadyStockTransactionsLive.setValue(false);
      isQuantityUnitStockChangeableLive.setValue(true);
      return;
    }
    StockLogEntry.getStockLogEntries(dlHelper, 10, 0, getFilledProduct().getId(),
        entries -> {
      if (VersionUtil.isGrocyServerMin330(sharedPrefs)) {
        hasProductAlreadyStockTransactionsLive.setValue(!entries.isEmpty());
      } else {
        isQuantityUnitStockChangeableLive.setValue(entries.isEmpty());
      }
    }, null).perform(dlHelper.getUuid());
  }

  public MutableLiveData<Boolean> getHasProductAlreadyStockTransactionsLive() {
    return hasProductAlreadyStockTransactionsLive;
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
    private final MasterProductCatQuantityUnitFragmentArgs args;

    public MasterProductCatQuantityUnitViewModelFactory(
        Application application,
        MasterProductCatQuantityUnitFragmentArgs args
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
