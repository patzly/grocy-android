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
import android.text.Spanned;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.RecipeEditFragmentArgs;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataRecipeEdit {

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<String> nameLive;
  private final MutableLiveData<Integer> nameErrorLive;
  private final MutableLiveData<String> baseServingsLive;
  private final MutableLiveData<Integer> baseServingsErrorLive;
  private final MutableLiveData<Boolean> notCheckShoppingListLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
  private final MutableLiveData<String> productNameLive;
  private final LiveData<String> productNameInfoStockLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<String> preparationLive;
  private final MutableLiveData<Spanned> preparationSpannedLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final PluralUtil pluralUtil;
  private boolean filledWithRecipe;
  private final int maxDecimalPlacesAmount;

  public FormDataRecipeEdit(Application application,
                            SharedPreferences sharedPrefs,
                            RecipeEditFragmentArgs args) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    pluralUtil = new PluralUtil(application);
    displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    ));
    nameLive = new MutableLiveData<>();
    nameErrorLive = new MutableLiveData<>();
    baseServingsLive = new MutableLiveData<>("1");
    baseServingsErrorLive = new MutableLiveData<>();
    notCheckShoppingListLive = new MutableLiveData<>();
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
    preparationLive = new MutableLiveData<>();
    preparationSpannedLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);
    if (args.getStartWithScanner() && !getExternalScannerEnabled() && !args
            .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    } else if (getCameraScannerWasVisibleLastTime() && !getExternalScannerEnabled() && !args
            .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    }

    filledWithRecipe = false;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<String> getNameLive() {
    return nameLive;
  }

  public MutableLiveData<Integer> getNameErrorLive() {
    return nameErrorLive;
  }

  public MutableLiveData<String> getBaseServingsLive() {
    return baseServingsLive;
  }

  public MutableLiveData<Integer> getBaseServingsErrorLive() {
    return baseServingsErrorLive;
  }

  public MutableLiveData<Boolean> getNotCheckShoppingListLive() {
    return notCheckShoppingListLive;
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

  public MutableLiveData<String> getPreparationLive() {
    return preparationLive;
  }

  public MutableLiveData<Spanned> getPreparationSpannedLive() {
    return preparationSpannedLive;
  }

  public boolean isFilledWithRecipe() {
    return filledWithRecipe;
  }

  public void setFilledWithRecipe(boolean filled) {
    this.filledWithRecipe = filled;
  }

  public boolean isFormValid() {
    boolean valid = isNameValid();
    valid = isBaseServingsValid() && valid;
    valid = isProductNameValid() && valid;
    return valid;
  }

  public boolean isNameValid() {
    if (nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
      nameErrorLive.setValue(R.string.error_empty);
      return false;
    }
    nameErrorLive.setValue(null);
    return true;
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() == null || productNameLive.getValue().isEmpty()) {
      productDetailsLive.setValue(null);
      productNameErrorLive.setValue(null);
      return true;
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

  public boolean isBaseServingsValid() {
    if (baseServingsLive.getValue() == null || baseServingsLive.getValue().isEmpty()) {
      baseServingsErrorLive.setValue(R.string.error_invalid_base_servings);
      return false;
    }

    double baseServings = NumUtil.toDouble(baseServingsLive.getValue());
    if (baseServings <= 0) {
      baseServingsErrorLive.setValue(R.string.error_invalid_base_servings);
      return false;
    }
    baseServingsErrorLive.setValue(null);
    return true;
  }

  public void moreBaseServings(ImageView view) {
    ViewUtil.startIcon(view);
    if (baseServingsLive.getValue() == null || baseServingsLive.getValue().isEmpty()) {
      baseServingsLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(baseServingsLive.getValue());
      baseServingsLive.setValue(NumUtil.trimAmount(currentValue + 1, maxDecimalPlacesAmount));
    }
  }

  public void lessBaseServings(ImageView view) {
    ViewUtil.startIcon(view);
    if (baseServingsLive.getValue() == null || baseServingsLive.getValue().isEmpty()) {
      baseServingsLive.setValue(NumUtil.trimAmount(1.0, maxDecimalPlacesAmount));
    } else {
      double currentValue = NumUtil.toDouble(baseServingsLive.getValue());

      if (currentValue == 1)
        return;

      baseServingsLive.setValue(NumUtil.trimAmount(currentValue - 1, maxDecimalPlacesAmount));
    }
  }

  public Recipe fillRecipe(@Nullable Recipe recipe) {
    if (!isFormValid()) {
      return null;
    }

    if (recipe == null) {
      recipe = new Recipe();
    }
    recipe.setName(nameLive.getValue());

    if (baseServingsLive.getValue() == null || baseServingsLive.getValue().isEmpty()) {
      recipe.setBaseServings(1.0);
    } else {
      recipe.setBaseServings(NumUtil.toDouble(baseServingsLive.getValue()));
    }

    recipe.setNotCheckShoppingList(notCheckShoppingListLive.getValue() != null ? notCheckShoppingListLive.getValue() : false);
    ProductDetails productDetails = productDetailsLive.getValue();
    recipe.setProductId(productDetails == null ? null : productDetails.getProduct().getId());
    recipe.setDescription(preparationLive.getValue());
    return recipe;
  }

  public void clearForm() {
    nameLive.setValue(null);
    baseServingsLive.setValue(null);
    notCheckShoppingListLive.setValue(false);
    productDetailsLive.setValue(null);
    productNameLive.setValue(null);
    productNameErrorLive.setValue(null);
    barcodeLive.setValue(null);
    preparationLive.setValue(null);
    preparationSpannedLive.setValue(null);
    new Handler().postDelayed(() -> nameErrorLive.setValue(null), 50);
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
