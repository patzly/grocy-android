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
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.RecipeEditIngredientEditFragmentArgs;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
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
  private final MutableLiveData<String> amountErrorLive;
  private final MediatorLiveData<String> amountHelperLive;
  private final LiveData<String> amountHintLive;
  private final MediatorLiveData<String> amountStockLive;
  private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final MutableLiveData<String> quantityUnitLabelLive;
  private final MutableLiveData<QuantityUnit> quantityUnitStockLive;
  private final MutableLiveData<String> variableAmountLive;
  private final MutableLiveData<Boolean> notCheckStockFulfillmentLive;
  private final MutableLiveData<String> ingredientGroupLive;
  private final MutableLiveData<String> noteLive;
  private final MutableLiveData<String> priceFactorLive;
  private final MutableLiveData<String> priceFactorErrorLive;
  private final PluralUtil pluralUtil;
  private boolean filledWithRecipePosition;
  private final int maxDecimalPlacesAmount;

  public FormDataRecipeEditIngredientEdit(Application application,
                                          SharedPreferences sharedPrefs,
                                          RecipeEditIngredientEditFragmentArgs args) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    pluralUtil = new PluralUtil(application);
    displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    ));
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    productsLive = new MutableLiveData<>();
    productDetailsLive = new MutableLiveData<>();
    productDetailsLive.setValue(null);
    productNameLive = new MutableLiveData<>();
    productNameInfoStockLive = Transformations.map(
            productDetailsLive,
            productDetails -> {
              String info = AmountUtil.getStockAmountInfo(application, pluralUtil, productDetails, maxDecimalPlacesAmount);
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

    onlyCheckSingleUnitInStockLive = new MutableLiveData<>(false);
    amountLive = new MutableLiveData<>();
    amountErrorLive = new MutableLiveData<>();
    quantityUnitsFactorsLive = new MutableLiveData<>();
    quantityUnitLive = new MutableLiveData<>();
    quantityUnitLabelLive = new MutableLiveData<>(getString(R.string.subtitle_none_selected));
    quantityUnitStockLive = new MutableLiveData<>();
    amountHintLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null ? application.getString(
            R.string.property_amount_in,
            quantityUnit.getNamePlural()
        ) : null
    );
    amountStockLive = new MediatorLiveData<>();
    amountStockLive.addSource(amountLive, i -> amountStockLive.setValue(getAmountStock()));
    amountStockLive.addSource(quantityUnitLive, i -> amountStockLive.setValue(getAmountStock()));
    amountStockLive.addSource(onlyCheckSingleUnitInStockLive, i -> amountStockLive.setValue(getAmountStock()));
    amountHelperLive = new MediatorLiveData<>();
    amountHelperLive
        .addSource(amountStockLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    amountHelperLive
        .addSource(quantityUnitsFactorsLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    amountHelperLive
        .addSource(quantityUnitStockLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    variableAmountLive = new MutableLiveData<>();
    notCheckStockFulfillmentLive = new MutableLiveData<>(false);
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

  public MutableLiveData<String> getAmountErrorLive() {
    return amountErrorLive;
  }

  public MediatorLiveData<String> getAmountHelperLive() {
    return amountHelperLive;
  }

  public LiveData<String> getAmountHintLive() {
    return amountHintLive;
  }

  private String getAmountStock() {
    if (productDetailsLive.getValue() == null) return null;
    return QuantityUnitConversionUtil.getAmountStock(
        quantityUnitStockLive.getValue(),
        quantityUnitLive.getValue(),
        amountLive.getValue(),
        quantityUnitsFactorsLive.getValue(),
        Boolean.TRUE.equals(onlyCheckSingleUnitInStockLive.getValue()),
        maxDecimalPlacesAmount
    );
  }

  private String getAmountHelpText() {
    QuantityUnit stock = quantityUnitStockLive.getValue();
    if (stock == null || !NumUtil.isStringDouble(amountStockLive.getValue())) {
      return null;
    }
    return application.getString(
        R.string.subtitle_amount_compare,
        amountStockLive.getValue(),
        pluralUtil.getQuantityUnitPlural(stock, NumUtil.toDouble(amountStockLive.getValue()))
    );
  }

  public MutableLiveData<HashMap<QuantityUnit, Double>> getQuantityUnitsFactorsLive() {
    return quantityUnitsFactorsLive;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitLive() {
    return quantityUnitLive;
  }

  public MutableLiveData<String> getQuantityUnitLabelLive() {
    return quantityUnitLabelLive;
  }

  public void setQuantityUnit(QuantityUnit quantityUnit) {
    quantityUnitLive.setValue(quantityUnit);
    quantityUnitLabelLive.setValue(
            quantityUnit == null ? getString(R.string.subtitle_none_selected) : quantityUnit.getName()
    );
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitStockLive() {
    return quantityUnitStockLive;
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

  public MutableLiveData<String> getPriceFactorErrorLive() {
    return priceFactorErrorLive;
  }

  public boolean isFilledWithRecipePosition() {
    return filledWithRecipePosition;
  }

  public void setFilledWithRecipePosition(boolean filledWithRecipePosition) {
    this.filledWithRecipePosition = filledWithRecipePosition;
  }

  public boolean isFormValid() {
    boolean valid = isProductNameValid();
    valid = isAmountValid() && valid;
    valid = isPriceFactorValid() && valid;
    return valid;
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
      if (productDetailsLive.getValue() != null) {
        clearForm();
        return false;
      }
    }
    if (productDetailsLive.getValue() == null || productNameLive.getValue().isEmpty()) {
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
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountErrorLive.setValue(application.getString(R.string.error_invalid_amount));
      return false;
    }

    if (NumUtil.getDecimalPlacesCount(amountLive.getValue()) > maxDecimalPlacesAmount) {
      amountErrorLive.setValue(application.getResources().getQuantityString(
          R.plurals.error_max_decimal_places, maxDecimalPlacesAmount, maxDecimalPlacesAmount
      ));
      return false;
    }

    double amount = NumUtil.toDouble(amountLive.getValue());
    if (amount <= 0) {
      amountErrorLive.setValue(application.getString(R.string.error_invalid_amount));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public void moreAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(amountLive.getValue());
      amountLive.setValue(NumUtil.trimAmount(currentValue + 1, maxDecimalPlacesAmount));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(amountLive.getValue());
      if (currentValue == 1) return;
      amountLive.setValue(NumUtil.trimAmount(currentValue - 1, maxDecimalPlacesAmount));
    }
  }

  public boolean isPriceFactorValid() {
    if (priceFactorLive.getValue() == null || priceFactorLive.getValue().isEmpty()) {
      priceFactorErrorLive.setValue(application.getString(R.string.error_invalid_factor));
      return false;
    }

    if (NumUtil.getDecimalPlacesCount(priceFactorLive.getValue()) > maxDecimalPlacesAmount) {
      priceFactorErrorLive.setValue(application.getResources().getQuantityString(
          R.plurals.error_max_decimal_places, maxDecimalPlacesAmount, maxDecimalPlacesAmount
      ));
      return false;
    }

    double factor = NumUtil.toDouble(priceFactorLive.getValue());
    if (factor <= 0) {
      priceFactorErrorLive.setValue(application.getString(R.string.error_invalid_factor));
      return false;
    }
    priceFactorErrorLive.setValue(null);
    return true;
  }

  public void morePriceFactor() {
    if (priceFactorLive.getValue() == null || priceFactorLive.getValue().isEmpty()) {
      amountLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(priceFactorLive.getValue());
      priceFactorLive.setValue(NumUtil.trimAmount(currentValue + 1, maxDecimalPlacesAmount));
    }
  }

  public void lessPriceFactor() {
    if (priceFactorLive.getValue() == null || priceFactorLive.getValue().isEmpty()) {
      priceFactorLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(priceFactorLive.getValue());
      if (currentValue == 1) return;
      priceFactorLive.setValue(NumUtil.trimAmount(currentValue - 1, maxDecimalPlacesAmount));
    }
  }

  public RecipePosition fillRecipePosition(@Nullable RecipePosition recipePosition) {
    if (!isFormValid()) {
      return null;
    }

    if (recipePosition == null) {
      recipePosition = new RecipePosition();
    }

    recipePosition.setProductId(productDetailsLive.getValue().getProduct().getId());
    recipePosition.setOnlyCheckSingleUnitInStock(onlyCheckSingleUnitInStockLive.getValue());
    recipePosition.setAmount(NumUtil.isStringDouble(amountStockLive.getValue()) && !onlyCheckSingleUnitInStockLive.getValue()
        ? NumUtil.toDouble(amountStockLive.getValue())
        : NumUtil.toDouble(amountLive.getValue()));
    recipePosition.setQuantityUnitId(quantityUnitLive.getValue().getId());
    recipePosition.setVariableAmount(variableAmountLive.getValue());
    recipePosition.setNotCheckStockFulfillment(notCheckStockFulfillmentLive.getValue());
    recipePosition.setIngredientGroup(ingredientGroupLive.getValue());
    recipePosition.setNote(noteLive.getValue());
    recipePosition.setPriceFactor(NumUtil.isStringDouble(priceFactorLive.getValue())
        ? NumUtil.toDouble(priceFactorLive.getValue()) : 1);

    return recipePosition;
  }

  public void clearForm() {
    productDetailsLive.setValue(null);
    productNameLive.setValue(null);
    productNameErrorLive.setValue(null);
    barcodeLive.setValue(null);
    onlyCheckSingleUnitInStockLive.setValue(false);
    amountLive.setValue(null);
    amountErrorLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitLabelLive.setValue(getString(R.string.subtitle_none_selected));
    quantityUnitStockLive.setValue(null);
    variableAmountLive.setValue(null);
    notCheckStockFulfillmentLive.setValue(false);
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
