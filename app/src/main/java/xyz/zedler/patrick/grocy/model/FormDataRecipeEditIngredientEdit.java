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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.RecipeEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.RecipeEditIngredientEditFragmentArgs;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataRecipeEditIngredientEdit {

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
  private final MutableLiveData<String> productNameLive;
  private final LiveData<String> productNameInfoStockLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<Boolean> onlyCheckSingleUnitInStockLive;
  private final MutableLiveData<String> amountLive;
  private final MutableLiveData<Integer> amountErrorLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final MutableLiveData<Boolean> quantityUnitErrorLive;
  private final MutableLiveData<String> variableAmountLive;
  private final MutableLiveData<Boolean> notCheckStockFulfillmentLive;
  private final MutableLiveData<String> ingredientGroupLive;
  private final MutableLiveData<String> noteLive;
  private final MutableLiveData<String> priceFactorLive;
  private final MutableLiveData<Integer> priceFactorErrorLive;
  private final PluralUtil pluralUtil;
  private boolean filledWithRecipePosition;

  public FormDataRecipeEditIngredientEdit(Application application,
                                          SharedPreferences sharedPrefs,
                                          RecipeEditIngredientEditFragmentArgs args,
                                          boolean beginnerMode) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    pluralUtil = new PluralUtil(application);
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    productsLive = new MutableLiveData<>();
    productDetailsLive = new MutableLiveData<>();
    productDetailsLive.setValue(null);
    productNameLive = new MutableLiveData<>();
    productNameInfoStockLive = Transformations.map(
            productDetailsLive,
            productDetails -> {
              String info = AmountUtil.getStockAmountInfo(application, pluralUtil, productDetails);
              return info != null ? application.getString(R.string.property_in_stock, info) : " ";
            }
    );
    productNameErrorLive = new MutableLiveData<>();
    barcodeLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);
    if (args.getStartWithScanner() && !getExternalScannerEnabled() && !args
            .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    } else if (getCameraScannerWasVisibleLastTime() && !getExternalScannerEnabled() && !args
            .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    }

    onlyCheckSingleUnitInStockLive = new MutableLiveData<>();
    amountLive = new MutableLiveData<>();
    amountErrorLive = new MutableLiveData<>();
    quantityUnitLive = new MutableLiveData<>();
    quantityUnitErrorLive = new MutableLiveData<>();
    variableAmountLive = new MutableLiveData<>();
    notCheckStockFulfillmentLive = new MutableLiveData<>();
    ingredientGroupLive = new MutableLiveData<>();
    noteLive = new MutableLiveData<>();
    priceFactorLive = new MutableLiveData<>();
    priceFactorErrorLive = new MutableLiveData<>();

    filledWithRecipePosition = false;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<ArrayList<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<ProductDetails> getProductDetailsLive() {
    return productDetailsLive;
  }

  public MutableLiveData<String> getProductNameLive() {
    return productNameLive;
  }

  public LiveData<String> getProductNameInfoStockLive() {
    return productNameInfoStockLive;
  }

  public MutableLiveData<Integer> getProductNameErrorLive() {
    return productNameErrorLive;
  }

  public MutableLiveData<String> getBarcodeLive() {
    return barcodeLive;
  }

  public MutableLiveData<Boolean> getOnlyCheckSingleUnitInStockLive() {
    return onlyCheckSingleUnitInStockLive;
  }

  public MutableLiveData<String> getAmountLive() {
    return amountLive;
  }

  public MutableLiveData<Integer> getAmountErrorLive() {
    return amountErrorLive;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitLive() {
    return quantityUnitLive;
  }

  public MutableLiveData<Boolean> getQuantityUnitErrorLive() {
    return quantityUnitErrorLive;
  }

  public MutableLiveData<String> getVariableAmountLive() {
    return variableAmountLive;
  }

  public MutableLiveData<Boolean> getNotCheckStockFulfillmentLive() {
    return notCheckStockFulfillmentLive;
  }

  public MutableLiveData<String> getIngredientGroupLive() {
    return ingredientGroupLive;
  }

  public MutableLiveData<String> getNoteLive() {
    return noteLive;
  }

  public MutableLiveData<String> getPriceFactorLive() {
    return priceFactorLive;
  }

  public MutableLiveData<Integer> getPriceFactorErrorLive() {
    return priceFactorErrorLive;
  }

  public boolean isFilledWithRecipePosition() {
    return filledWithRecipePosition;
  }

  public void setFilledWithRecipePosition(boolean filledWithRecipePosition) {
    this.filledWithRecipePosition = filledWithRecipePosition;
  }

  public boolean isFormValid() {
    return isProductNameValid() && isAmountValid();
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
      if (productDetailsLive.getValue() != null) {
        clearForm();
        return false;
      }
    }
    if (productDetailsLive.getValue() == null && !productNameLive.getValue().isEmpty()) {
      productNameErrorLive.setValue(R.string.error_invalid_product);
      return false;
    }
    if (productDetailsLive.getValue() != null && !productNameLive.getValue().isEmpty()
            && !productDetailsLive.getValue().getProduct().getName()
            .equals(productNameLive.getValue())
    ) {
      productNameErrorLive.setValue(R.string.error_invalid_product);
      return false;
    }
    productNameErrorLive.setValue(null);
    return true;
  }

  public boolean isAmountValid() {
    if (amountLive.getValue() != null && amountLive.getValue().isEmpty()) {
      if (amountLive.getValue() != null) {
        clearForm();
        return false;
      }
    }
    if (amountLive.getValue() == null && !amountLive.getValue().isEmpty()) {
      amountErrorLive.setValue(R.string.error_invalid_base_servings);
      return false;
    }

    double baseServings = Double.parseDouble(amountLive.getValue());
    if (baseServings <= 0) {
      amountErrorLive.setValue(R.string.error_invalid_base_servings);
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public void moreAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountLive.setValue(NumUtil.trim(1.0));
    } else {
      double currentValue = Double.parseDouble(amountLive.getValue());
      amountLive.setValue(NumUtil.trim(currentValue + 1));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountLive.setValue(NumUtil.trim(1.0));
    } else {
      double currentValue = Double.parseDouble(amountLive.getValue());

      if (currentValue == 1)
        return;

      amountLive.setValue(NumUtil.trim(currentValue - 1));
    }
  }

  public RecipePosition fillRecipePosition(@Nullable RecipePosition recipePosition) {
    if (!isFormValid()) {
      return null;
    }

    if (recipePosition == null) {
      recipePosition = new RecipePosition();
    }

    // TODO: Add all input fields!!!
    return recipePosition;
  }

  public void clearForm() {
    productDetailsLive.setValue(null);
    productNameLive.setValue(null);
    productNameErrorLive.setValue(null);
    barcodeLive.setValue(null);
    onlyCheckSingleUnitInStockLive.setValue(null);
    amountLive.setValue(null);
    amountErrorLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitErrorLive.setValue(null);
    variableAmountLive.setValue(null);
    notCheckStockFulfillmentLive.setValue(null);
    ingredientGroupLive.setValue(null);
    noteLive.setValue(null);
    priceFactorLive.setValue(null);
    priceFactorErrorLive.setValue(null);
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
    sharedPrefs.edit()
            .putBoolean(Constants.PREF.CAMERA_SCANNER_VISIBLE_RECIPE, isScannerVisible())
            .apply();
  }

  public boolean getCameraScannerWasVisibleLastTime() {
    return sharedPrefs.getBoolean(
            Constants.PREF.CAMERA_SCANNER_VISIBLE_RECIPE,
            false
    );
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
            Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
            Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
