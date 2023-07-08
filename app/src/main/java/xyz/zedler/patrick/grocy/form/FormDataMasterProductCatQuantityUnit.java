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

package xyz.zedler.patrick.grocy.form;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class FormDataMasterProductCatQuantityUnit {

  public final static String QUANTITY_UNIT_TYPE = "qu_type";
  public final static String STOCK = "stock";
  public final static String PURCHASE = "purchase";
  public final static String CONSUME = "consume";
  public final static String PRICE = "price";

  private final Application application;
  private final MutableLiveData<Boolean> displayHelpLive;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final MutableLiveData<QuantityUnit> quStockLive;
  private final LiveData<String> quStockNameLive;
  private final LiveData<Boolean> quStockErrorLive;
  private final MutableLiveData<QuantityUnit> quPurchaseLive;
  private final LiveData<String> quPurchaseNameLive;
  private final LiveData<Boolean> quPurchaseErrorLive;
  private final boolean factorPurchaseToStockEnabled;
  private final MutableLiveData<String> factorPurchaseToStockLive;
  private final MutableLiveData<QuantityUnit> quConsumeLive;
  private final LiveData<String> quConsumeNameLive;
  private final LiveData<Boolean> quConsumeErrorLive;
  private final MutableLiveData<QuantityUnit> quPriceLive;
  private final LiveData<String> quPriceNameLive;
  private final LiveData<Boolean> quPriceErrorLive;
  private final int maxDecimalPlacesAmount;
  private final boolean isGrocyVersionMin400;

  private boolean filledWithProduct;

  public FormDataMasterProductCatQuantityUnit(
      Application application,
      SharedPreferences sharedPrefs,
      boolean beginnerMode
  ) {
    this.application = application;
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        SETTINGS.STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    isGrocyVersionMin400 = VersionUtil.isGrocyServerMin400(sharedPrefs);
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    quStockLive = new MutableLiveData<>();
    quStockNameLive = Transformations.map(
        quStockLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quStockErrorLive = Transformations.map(quStockLive, qu -> qu == null);
    quPurchaseLive = new MutableLiveData<>();
    quPurchaseNameLive = Transformations.map(
        quPurchaseLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quPurchaseErrorLive = Transformations.map(quPurchaseLive, qu -> qu == null);
    factorPurchaseToStockEnabled = !VersionUtil.isGrocyServerMin400(sharedPrefs);
    factorPurchaseToStockLive = new MutableLiveData<>();
    quConsumeLive = new MutableLiveData<>();
    quConsumeNameLive = Transformations.map(
        quConsumeLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quConsumeErrorLive = Transformations.map(quConsumeLive, qu -> qu == null);
    quPriceLive = new MutableLiveData<>();
    quPriceNameLive = Transformations.map(
        quPriceLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quPriceErrorLive = Transformations.map(quPriceLive, qu -> qu == null);

    filledWithProduct = false;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public boolean isGrocyVersionMin400() {
    return isGrocyVersionMin400;
  }

  public QuantityUnit getQuantityUnitFromId(int id) {
    return quantityUnitHashMap.get(id);
  }

  public void setQuantityUnitHashMap(
      HashMap<Integer, QuantityUnit> quantityUnitHashMap) {
    this.quantityUnitHashMap = quantityUnitHashMap;
  }

  public MutableLiveData<QuantityUnit> getQuStockLive() {
    return quStockLive;
  }

  public LiveData<String> getQuStockNameLive() {
    return quStockNameLive;
  }

  public LiveData<Boolean> getQuStockErrorLive() {
    return quStockErrorLive;
  }

  public MutableLiveData<QuantityUnit> getQuPurchaseLive() {
    return quPurchaseLive;
  }

  public LiveData<String> getQuPurchaseNameLive() {
    return quPurchaseNameLive;
  }

  public LiveData<Boolean> getQuPurchaseErrorLive() {
    return quPurchaseErrorLive;
  }

  public boolean isFactorPurchaseToStockEnabled() {
    return factorPurchaseToStockEnabled;
  }

  public MutableLiveData<String> getFactorPurchaseToStockLive() {
    return factorPurchaseToStockLive;
  }

  public double getFactorPurchaseToStock() {
    String numberString = factorPurchaseToStockLive.getValue();
    double number = 1;
    if (NumUtil.isStringDouble(numberString)) {
      number = NumUtil.toDouble(numberString);
    }
    return number;
  }

  public void setFactorPurchaseToStock(String number) {
    if (NumUtil.toDouble(number) <= 0) {
      factorPurchaseToStockLive.setValue(String.valueOf(1));
    } else {
      factorPurchaseToStockLive.setValue(number);
    }
  }

  public MutableLiveData<QuantityUnit> getQuConsumeLive() {
    return quConsumeLive;
  }

  public LiveData<String> getQuConsumeNameLive() {
    return quConsumeNameLive;
  }

  public LiveData<Boolean> getQuConsumeErrorLive() {
    return quConsumeErrorLive;
  }

  public MutableLiveData<QuantityUnit> getQuPriceLive() {
    return quPriceLive;
  }

  public LiveData<String> getQuPriceNameLive() {
    return quPriceNameLive;
  }

  public LiveData<Boolean> getQuPriceErrorLive() {
    return quPriceErrorLive;
  }

  public void selectQuantityUnit(QuantityUnit quantityUnit, Bundle argsBundle) {
    if (quantityUnit != null && quantityUnit.getId() == -1) {
      quantityUnit = null;
    }
    String type = argsBundle.getString(QUANTITY_UNIT_TYPE);
    switch (type) {
      case STOCK:
        quStockLive.setValue(quantityUnit);
        if (quPurchaseLive.getValue() == null) {
          quPurchaseLive.setValue(quantityUnit);
        }
        if (quConsumeLive.getValue() == null) {
          quConsumeLive.setValue(quantityUnit);
        }
        if (quPriceLive.getValue() == null) {
          quPriceLive.setValue(quantityUnit);
        }
        break;
      case PURCHASE:
        quPurchaseLive.setValue(quantityUnit);
        break;
      case CONSUME:
        quConsumeLive.setValue(quantityUnit);
        break;
      default:
        quPriceLive.setValue(quantityUnit);
        break;
    }
  }

  public static boolean isFormInvalid(@Nullable Product product, boolean isGrocyVersionMin400) {
    if (product == null) {
      return true;
    }
    boolean valid = product.getQuIdStockInt() != -1 && product.getQuIdPurchaseInt() != -1;
    if (isGrocyVersionMin400) {
      valid = product.getQuIdStockInt() != -1 && product.getQuIdPurchaseInt() != -1 && valid;
    }
    return !valid;
  }

  public Product fillProduct(@NonNull Product product) {
    QuantityUnit quStock = quStockLive.getValue();
    QuantityUnit quPurchase = quPurchaseLive.getValue();
    QuantityUnit quConsume = quConsumeLive.getValue();
    QuantityUnit quPrice = quPriceLive.getValue();
    product.setQuIdStock(quStock != null ? quStock.getId() : -1);
    product.setQuIdPurchase(quPurchase != null ? quPurchase.getId() : -1);
    product.setQuFactorPurchaseToStock(factorPurchaseToStockLive.getValue());
    product.setQuIdConsume(quConsume != null ? quConsume.getId() : -1);
    product.setQuIdPrice(quPrice != null ? quPrice.getId() : -1);
    return product;
  }

  public void fillWithProductIfNecessary(Product product) {
    if (filledWithProduct || product == null) {
      return;
    }

    quStockLive.setValue(getQuantityUnitFromId(product.getQuIdStockInt()));
    quPurchaseLive.setValue(getQuantityUnitFromId(product.getQuIdPurchaseInt()));
    String factor = NumUtil.trimAmount(product.getQuFactorPurchaseToStockDouble(), maxDecimalPlacesAmount);
    factorPurchaseToStockLive.setValue(factor);
    quConsumeLive.setValue(getQuantityUnitFromId(product.getQuIdConsumeInt()));
    quPriceLive.setValue(getQuantityUnitFromId(product.getQuIdPriceInt()));
    filledWithProduct = true;
  }
}
