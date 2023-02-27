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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataMasterProductCatBarcodesEdit {

  private final Application application;
  private final Product product;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<Integer> barcodeErrorLive;
  private final MutableLiveData<String> amountLive;
  private final MutableLiveData<String> amountErrorLive;
  private final MediatorLiveData<String> amountHelperLive;
  private final LiveData<String> amountHintLive;
  private final MediatorLiveData<String> amountPurchaseLive;
  private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
  private final LiveData<ArrayList<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final LiveData<String> quantityUnitNameLive;
  private final MutableLiveData<Store> storeLive;
  private final LiveData<String> storeNameLive;
  private final MutableLiveData<String> noteLive;
  private final PluralUtil pluralUtil;
  private boolean filledWithProductBarcode;
  private QuantityUnit quantityUnitPurchase;
  private final int maxDecimalPlacesAmount;

  public FormDataMasterProductCatBarcodesEdit(Application application, Product product) {
    this.application = application;
    this.product = product;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    scannerVisibilityLive = new MutableLiveData<>(false);
    barcodeLive = new MutableLiveData<>();
    barcodeErrorLive = new MutableLiveData<>();
    amountLive = new MutableLiveData<>();
    amountErrorLive = new MutableLiveData<>();
    quantityUnitsFactorsLive = new MutableLiveData<>();
    quantityUnitsLive = Transformations.map(
        quantityUnitsFactorsLive,
        quantityUnitsFactors -> quantityUnitsFactors != null
            ? new ArrayList<>(quantityUnitsFactors.keySet()) : null
    );
    quantityUnitPurchase = null;
    quantityUnitLive = new MutableLiveData<>();
    quantityUnitNameLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null ? quantityUnit.getName() : null
    );
    amountHintLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null ? application.getString(
            R.string.property_amount_in,
            quantityUnit.getNamePlural()
        ) : null
    );
    amountPurchaseLive = new MediatorLiveData<>();
    amountPurchaseLive.addSource(amountLive, i -> amountPurchaseLive.setValue(getAmountPurchase()));
    amountPurchaseLive.addSource(quantityUnitLive, i -> amountPurchaseLive.setValue(
        getAmountPurchase()));
    amountHelperLive = new MediatorLiveData<>();
    amountHelperLive
        .addSource(amountPurchaseLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    amountHelperLive
        .addSource(quantityUnitsFactorsLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    storeLive = new MutableLiveData<>();
    storeNameLive = Transformations.map(
        storeLive,
        store -> store != null ? store.getName() : null
    );
    noteLive = new MutableLiveData<>();
    pluralUtil = new PluralUtil(application);
    filledWithProductBarcode = false;
  }

  public MutableLiveData<Boolean> getScannerVisibilityLive() {
    return scannerVisibilityLive;
  }

  public boolean isScannerVisible() {
    assert scannerVisibilityLive.getValue() != null;
    return scannerVisibilityLive.getValue();
  }

  public void toggleScannerVisibility() {
    scannerVisibilityLive.setValue(!isScannerVisible());
  }

  public MutableLiveData<String> getBarcodeLive() {
    return barcodeLive;
  }

  public MutableLiveData<Integer> getBarcodeErrorLive() {
    return barcodeErrorLive;
  }

  public MutableLiveData<String> getAmountLive() {
    return amountLive;
  }

  public MutableLiveData<String> getAmountErrorLive() {
    return amountErrorLive;
  }

  public MutableLiveData<String> getAmountHelperLive() {
    return amountHelperLive;
  }

  public LiveData<String> getAmountHintLive() {
    return amountHintLive;
  }

  public void moreAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountLive.setValue(String.valueOf(1));
    } else {
      double amountNew = Double.parseDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
      double amountNew = Double.parseDouble(amountLive.getValue()) - 1;
      if (amountNew >= 1) {
        amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
      }
    }
  }

  public MutableLiveData<HashMap<QuantityUnit, Double>> getQuantityUnitsFactorsLive() {
    return quantityUnitsFactorsLive;
  }

  public LiveData<ArrayList<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }

  public void setQuantityUnitPurchase(QuantityUnit quantityUnitPurchase) {
    this.quantityUnitPurchase = quantityUnitPurchase;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitLive() {
    return quantityUnitLive;
  }

  public LiveData<String> getQuantityUnitNameLive() {
    return quantityUnitNameLive;
  }

  private QuantityUnit getStockQuantityUnit() {
    HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
    if (hashMap == null || !hashMap.containsValue((double) -1)) {
      return null;
    }
    for (Map.Entry<QuantityUnit, Double> entry : hashMap.entrySet()) {
      if (entry.getValue() == -1) {
        return entry.getKey();
      }
    }
    return null;
  }

  private String getAmountPurchase() {
    QuantityUnit purchase = quantityUnitPurchase;
    QuantityUnit current = quantityUnitLive.getValue();
    if (!NumUtil.isStringDouble(amountLive.getValue())
        || quantityUnitsFactorsLive.getValue() == null
    ) {
      return null;
    }
    assert amountLive.getValue() != null;

    if (purchase != null && current != null && purchase.getId() != current.getId()) {
      HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
      double amount = Double.parseDouble(amountLive.getValue());
      Object currentFactor = hashMap.get(current);
      if (currentFactor == null) {
        amountHelperLive.setValue(null);
        return null;
      }
      double amountMultiplied = amount;
      if (product != null) {
        amountMultiplied = amount / (double) currentFactor;
      }
      return NumUtil.trimAmount(amountMultiplied, maxDecimalPlacesAmount);
    } else {
      return null;
    }
  }

  private String getAmountHelpText() {
    QuantityUnit purchase = quantityUnitPurchase;
    if (purchase == null || !NumUtil.isStringDouble(amountPurchaseLive.getValue())) {
      return null;
    }
    return application.getString(
        R.string.subtitle_amount_compare,
        amountPurchaseLive.getValue(),
        pluralUtil.getQuantityUnitPlural(purchase, Double.parseDouble(amountPurchaseLive.getValue()))
    );
  }

  public MutableLiveData<Store> getStoreLive() {
    return storeLive;
  }

  public LiveData<String> getStoreNameLive() {
    return storeNameLive;
  }

  public MutableLiveData<String> getNoteLive() {
    return noteLive;
  }

  public boolean isFilledWithProductBarcode() {
    return filledWithProductBarcode;
  }

  public void setFilledWithProductBarcode(boolean filled) {
    this.filledWithProductBarcode = filled;
  }

  public boolean isBarcodeValid() {
    if (barcodeLive.getValue() == null || barcodeLive.getValue().isEmpty()) {
      barcodeErrorLive.setValue(R.string.error_empty);
      return false;
    }
    barcodeErrorLive.setValue(null);
    return true;
  }

  public boolean isAmountValid() {
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountErrorLive.setValue(null);
      return true;
    }
    if (!NumUtil.isStringDouble(amountLive.getValue())) {
      amountErrorLive.setValue(getString(R.string.error_invalid_amount));
      return false;
    }
    if (NumUtil.getDecimalPlacesCount(amountLive.getValue()) > maxDecimalPlacesAmount) {
      amountErrorLive.setValue(application.getResources().getQuantityString(
          R.plurals.error_max_decimal_places, maxDecimalPlacesAmount, maxDecimalPlacesAmount
      ));
      return false;
    }
    if (Double.parseDouble(amountLive.getValue()) <= 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isBarcodeValid();
    valid = isAmountValid() && valid;
    return valid;
  }

  public ProductBarcode fillProductBarcode(@Nullable ProductBarcode productBarcode) {
    if (!isFormValid()) {
      return null;
    }
    if (productBarcode == null) {
      productBarcode = new ProductBarcode();
    }
    productBarcode.setProductIdInt(product.getId());

    if (barcodeLive.getValue() != null && !barcodeLive.getValue().trim().isEmpty()) {
      productBarcode.setBarcode(barcodeLive.getValue().trim());
    } else {
      productBarcode.setBarcode(null);
    }
    if (NumUtil.isStringDouble(amountLive.getValue())) {
      productBarcode.setAmount(amountLive.getValue().trim());
    } else {
      productBarcode.setAmount(null);
    }
    if (quantityUnitLive.getValue() != null) {
      productBarcode.setQuId(String.valueOf(quantityUnitLive.getValue().getId()));
    } else {
      productBarcode.setQuId(null);
    }
    if (storeLive.getValue() != null) {
      productBarcode.setStoreId(String.valueOf(storeLive.getValue().getId()));
    } else {
      productBarcode.setStoreId(null);
    }
    if (noteLive.getValue() != null && !noteLive.getValue().trim().isEmpty()) {
      productBarcode.setNote(noteLive.getValue());
    } else {
      productBarcode.setNote(null);
    }
    return productBarcode;
  }

  public void clearForm() {
    barcodeLive.setValue(null);
    barcodeErrorLive.setValue(null);
    amountLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitsFactorsLive.setValue(null);
    barcodeLive.setValue(null);
    noteLive.setValue(null);
    new Handler().postDelayed(() -> {
      barcodeErrorLive.setValue(null);
      amountErrorLive.setValue(null);
    }, 50);
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
