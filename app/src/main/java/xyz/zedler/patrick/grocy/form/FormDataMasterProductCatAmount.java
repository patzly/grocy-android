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
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class FormDataMasterProductCatAmount {

  public static final String AMOUNT_ARG = "amount_arg";
  public static final int MIN_AMOUNT = 0;
  public static final int QUICK_CONSUME_AMOUNT = 2;
  public static final int FACTOR_AMOUNT = 4;
  public static final int TARE_WEIGHT = 6;

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<String> minAmountLive;
  private final MutableLiveData<Boolean> accumulateMinAmount;
  private final MutableLiveData<Boolean> treatOpenedAsOutOfStock;
  private final MutableLiveData<String> quickConsumeAmountLive;
  private final LiveData<String> quickConsumeAmountTitleLive;
  private final MutableLiveData<String> factorPurchaseToStockLive;
  private final MutableLiveData<Boolean> enableTareWeightHandlingLive;
  private final MutableLiveData<String> tareWeightLive;
  private final LiveData<String> tareWeightTitleLive;
  private final LiveData<Boolean> tareWeightErrorLive;
  private final MutableLiveData<Boolean> disableStockCheckLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final int maxDecimalPlacesAmount;

  private boolean filledWithProduct;

  public FormDataMasterProductCatAmount(
      Application application,
      SharedPreferences sharedPrefs,
      boolean beginnerMode
  ) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    quantityUnitLive = new MutableLiveData<>();
    minAmountLive = new MutableLiveData<>();
    accumulateMinAmount = new MutableLiveData<>(false);
    treatOpenedAsOutOfStock = new MutableLiveData<>(false);
    quickConsumeAmountLive = new MutableLiveData<>();
    quickConsumeAmountTitleLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit == null ?
            getString(R.string.property_amount_quick_consume) :
            this.application.getString(
                R.string.property_amount_quick_consume_in,
                quantityUnit.getNamePlural()
            )
    );
    factorPurchaseToStockLive = new MutableLiveData<>();
    enableTareWeightHandlingLive = new MutableLiveData<>(false);
    tareWeightLive = new MutableLiveData<>();
    tareWeightTitleLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit == null ?
            getString(R.string.property_tare_weight) :
            this.application.getString(
                R.string.property_tare_weight_in,
                quantityUnit.getNamePlural()
            )
    );
    //noinspection Convert2MethodRef
    tareWeightErrorLive = Transformations.map(
        tareWeightLive,
        weight -> weight == null
    );
    disableStockCheckLive = new MutableLiveData<>(false);
    filledWithProduct = false;
    quantityUnitLive.setValue(null);
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<String> getMinAmountLive() {
    return minAmountLive;
  }

  public MutableLiveData<Boolean> getAccumulateMinAmount() {
    return accumulateMinAmount;
  }

  public boolean getTreatOpenedAsOutOfStockVisible() {
    return VersionUtil.isGrocyServerMin320(sharedPrefs);
  }

  public MutableLiveData<Boolean> getTreatOpenedAsOutOfStock() {
    return treatOpenedAsOutOfStock;
  }

  public MutableLiveData<String> getQuickConsumeAmountLive() {
    return quickConsumeAmountLive;
  }

  public LiveData<String> getQuickConsumeAmountTitleLive() {
    return quickConsumeAmountTitleLive;
  }

  public MutableLiveData<String> getFactorPurchaseToStockLive() {
    return factorPurchaseToStockLive;
  }

  public MutableLiveData<Boolean> getEnableTareWeightHandlingLive() {
    return enableTareWeightHandlingLive;
  }

  public MutableLiveData<String> getTareWeightLive() {
    return tareWeightLive;
  }

  public LiveData<String> getTareWeightTitleLive() {
    return tareWeightTitleLive;
  }

  public LiveData<Boolean> getTareWeightErrorLive() {
    return tareWeightErrorLive;
  }

  public MutableLiveData<Boolean> getDisableStockCheckLive() {
    return disableStockCheckLive;
  }

  public void setAmount(String input, Bundle argsBundle) {
    String number = NumUtil.isStringDouble(input) ? input : String.valueOf(0);
    int type = argsBundle.getInt(AMOUNT_ARG);
    if (type == MIN_AMOUNT) {
      if (NumUtil.toDouble(number) < 0) {
        minAmountLive.setValue(String.valueOf(0));
      } else {
        minAmountLive.setValue(number);
      }
    } else if (type == QUICK_CONSUME_AMOUNT) {
      if (NumUtil.toDouble(number) <= 0) {
        quickConsumeAmountLive.setValue(String.valueOf(1));
      } else {
        quickConsumeAmountLive.setValue(number);
      }
    } else if (type == FACTOR_AMOUNT) {
      if (NumUtil.toDouble(number) <= 0) {
        factorPurchaseToStockLive.setValue(String.valueOf(1));
      } else {
        factorPurchaseToStockLive.setValue(number);
      }
    } else { // TARE_WEIGHT
      if (NumUtil.toDouble(number) < 0) {
        tareWeightLive.setValue(String.valueOf(0));
      } else {
        tareWeightLive.setValue(number);
      }
    }
  }

  public double getAmount(int type) {
    String numberString;
    if (type == MIN_AMOUNT) {
      numberString = minAmountLive.getValue();
    } else if (type == QUICK_CONSUME_AMOUNT) {
      numberString = quickConsumeAmountLive.getValue();
    } else if (type == FACTOR_AMOUNT) {
      numberString = factorPurchaseToStockLive.getValue();
    } else { // TARE_WEIGHT
      numberString = tareWeightLive.getValue();
    }
    double number = 0;
    if (NumUtil.isStringDouble(numberString)) {
      number = NumUtil.toDouble(numberString);
    }
    return number;
  }

  public boolean isFormValid() {
    boolean valid = true;
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
    return valid;
  }

  public static boolean isFormInvalid(@Nullable Product product) {
    if (product == null) {
      return true;
    }
    String tareWeight = product.getTareWeight();
    boolean valid = !product.getEnableTareWeightHandlingBoolean()
        || product.getEnableTareWeightHandlingBoolean()
        && tareWeight != null && !tareWeight.isEmpty()
        && NumUtil.isStringDouble(tareWeight) && NumUtil.toDouble(tareWeight) >= 0;
    return !valid;
  }

  public Product fillProduct(@NonNull Product product) {
    if (!isFormValid()) {
      return product;
    }
    product.setMinStockAmount(minAmountLive.getValue());
    product.setAccumulateSubProductsMinStockAmount(accumulateMinAmount.getValue());
    if (VersionUtil.isGrocyServerMin320(sharedPrefs)) {
      boolean treatOpened = treatOpenedAsOutOfStock.getValue() != null
          ? treatOpenedAsOutOfStock.getValue() : true;
      product.setTreatOpenedAsOutOfStock(treatOpened ? "1" : "0");
    }
    product.setQuickConsumeAmount(quickConsumeAmountLive.getValue());
    product.setQuFactorPurchaseToStock(factorPurchaseToStockLive.getValue());
    product.setEnableTareWeightHandling(enableTareWeightHandlingLive.getValue());
    product.setTareWeight(tareWeightLive.getValue());
    product.setNotCheckStockFulfillmentForRecipes(disableStockCheckLive.getValue());
    return product;
  }

  public void fillWithProductIfNecessary(Product product, List<QuantityUnit> quantityUnits) {
    if (filledWithProduct || product == null) {
      return;
    }

    String minAmount = NumUtil.trimAmount(product.getMinStockAmountDouble(), maxDecimalPlacesAmount);
    String quickAmount = NumUtil.trimAmount(product.getQuickConsumeAmountDouble(), maxDecimalPlacesAmount);
    String factor = NumUtil.trimAmount(product.getQuFactorPurchaseToStockDouble(), maxDecimalPlacesAmount);
    String tareWeight = NumUtil.trimAmount(product.getTareWeightDouble(), maxDecimalPlacesAmount);
    minAmountLive.setValue(minAmount);
    accumulateMinAmount.setValue(product.getAccumulateSubProductsMinStockAmountBoolean());
    treatOpenedAsOutOfStock.setValue(product.getTreatOpenedAsOutOfStockBoolean());
    quickConsumeAmountLive.setValue(quickAmount);
    factorPurchaseToStockLive.setValue(factor);
    enableTareWeightHandlingLive.setValue(product.getEnableTareWeightHandlingBoolean());
    tareWeightLive.setValue(tareWeight);
    disableStockCheckLive.setValue(product.getNotCheckStockFulfillmentForRecipesBoolean());
    quantityUnitLive.setValue(QuantityUnit.getFromId(quantityUnits, product.getQuIdStockInt()));
    filledWithProduct = true;
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
