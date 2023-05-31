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
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataShoppingListItemEdit {

  private final Application application;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<ShoppingList> shoppingListLive;
  private final LiveData<String> shoppingListNameLive;
  private final MutableLiveData<Integer> shoppingListIdLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<Product> productLive;
  private final MutableLiveData<String> productNameLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<String> amountLive;
  private final MutableLiveData<String> amountErrorLive;
  private final MediatorLiveData<String> amountHelperLive;
  private final LiveData<String> amountHintLive;
  private final MediatorLiveData<String> amountStockLive;
  private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
  private final LiveData<ArrayList<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final LiveData<String> quantityUnitNameLive;
  private final MutableLiveData<Boolean> quantityUnitErrorLive;
  private final MutableLiveData<Boolean> useMultilineNoteLive;
  private final MutableLiveData<String> noteLive;
  private final MutableLiveData<Integer> noteErrorLive;
  private final PluralUtil pluralUtil;
  private boolean filledWithShoppingListItem;
  private final int maxDecimalPlacesAmount;

  public FormDataShoppingListItemEdit(Application application) {
    this.application = application;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    scannerVisibilityLive = new MutableLiveData<>(false);
    shoppingListLive = new MutableLiveData<>();
    shoppingListNameLive = Transformations.map(
        shoppingListLive,
        shoppingList -> shoppingList != null ? shoppingList.getName() : null
    );
    shoppingListIdLive = (MutableLiveData<Integer>) Transformations.map(
        shoppingListLive,
        shoppingList -> shoppingList != null ? shoppingList.getId() : -1
    );
    productsLive = new MutableLiveData<>(new ArrayList<>());
    productLive = new MutableLiveData<>();
    productNameLive = new MutableLiveData<>();
    productNameErrorLive = new MutableLiveData<>();
    barcodeLive = new MutableLiveData<>();
    amountLive = new MutableLiveData<>(String.valueOf(1));
    amountErrorLive = new MutableLiveData<>();
    quantityUnitsFactorsLive = new MutableLiveData<>();
    quantityUnitsLive = Transformations.map(
        quantityUnitsFactorsLive,
        quantityUnitsFactors -> quantityUnitsFactors != null
            ? new ArrayList<>(quantityUnitsFactors.keySet()) : null
    );
    quantityUnitLive = new MutableLiveData<>();
    quantityUnitNameLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null ? quantityUnit.getName() : null
    );
    quantityUnitErrorLive = (MutableLiveData<Boolean>) Transformations.map(
        quantityUnitLive,
        quantityUnit -> !isQuantityUnitValid()
    );
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
    amountHelperLive = new MediatorLiveData<>();
    amountHelperLive
        .addSource(amountStockLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    amountHelperLive
        .addSource(quantityUnitsFactorsLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    useMultilineNoteLive = new MutableLiveData<>(false);
    noteLive = new MutableLiveData<>();
    noteErrorLive = new MutableLiveData<>();
    pluralUtil = new PluralUtil(application);
    filledWithShoppingListItem = false;
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

  public MutableLiveData<ShoppingList> getShoppingListLive() {
    return shoppingListLive;
  }

  public LiveData<String> getShoppingListNameLive() {
    return shoppingListNameLive;
  }

  public MutableLiveData<Integer> getShoppingListIdLive() {
    return shoppingListIdLive;
  }

  public MutableLiveData<Product> getProductLive() {
    return productLive;
  }

  public MutableLiveData<String> getProductNameLive() {
    return productNameLive;
  }

  public MutableLiveData<Integer> getProductNameErrorLive() {
    return productNameErrorLive;
  }

  public MutableLiveData<String> getBarcodeLive() {
    return barcodeLive;
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
      double amountNew = NumUtil.toDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
      double amountNew = NumUtil.toDouble(amountLive.getValue()) - 1;
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

  public MutableLiveData<QuantityUnit> getQuantityUnitLive() {
    return quantityUnitLive;
  }

  public LiveData<String> getQuantityUnitNameLive() {
    return quantityUnitNameLive;
  }

  public MutableLiveData<Boolean> getQuantityUnitErrorLive() {
    return quantityUnitErrorLive;
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

  private String getAmountStock() {
    QuantityUnit stock = getStockQuantityUnit();
    QuantityUnit current = quantityUnitLive.getValue();
    if (!NumUtil.isStringDouble(amountLive.getValue())
        || quantityUnitsFactorsLive.getValue() == null
    ) {
      return null;
    }
    assert amountLive.getValue() != null;

    if (stock != null && current != null && stock.getId() != current.getId()) {
      HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
      double amount = NumUtil.toDouble(amountLive.getValue());
      Object currentFactor = hashMap.get(current);
      if (currentFactor == null) {
        amountHelperLive.setValue(null);
        return null;
      }
      double amountMultiplied;
      if (productLive.getValue() != null
          && current.getId() == productLive.getValue().getQuIdPurchaseInt()) {
        amountMultiplied = amount * (double) currentFactor;
      } else {
        amountMultiplied = amount / (double) currentFactor;
      }
      return NumUtil.trimAmount(amountMultiplied, maxDecimalPlacesAmount);
    } else {
      return null;
    }
  }

  private String getAmountHelpText() {
    QuantityUnit stock = getStockQuantityUnit();
    if (stock == null || !NumUtil.isStringDouble(amountStockLive.getValue())) {
      return null;
    }
    return application.getString(
        R.string.subtitle_amount_compare,
        amountStockLive.getValue(),
        pluralUtil.getQuantityUnitPlural(stock, NumUtil.toDouble(amountStockLive.getValue()))
    );
  }

  public MutableLiveData<Boolean> getUseMultilineNoteLive() {
    return useMultilineNoteLive;
  }

  public void setUseMultilineNoteLive(boolean useMultiline) {
    useMultilineNoteLive.setValue(useMultiline);
  }

  public MutableLiveData<String> getNoteLive() {
    return noteLive;
  }

  public MutableLiveData<Integer> getNoteErrorLive() {
    return noteErrorLive;
  }

  public MutableLiveData<ArrayList<Product>> getProductsLive() {
    return productsLive;
  }

  public boolean isFilledWithShoppingListItem() {
    return filledWithShoppingListItem;
  }

  public void setFilledWithShoppingListItem(boolean filled) {
    this.filledWithShoppingListItem = filled;
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
      if (productLive.getValue() != null) {
        productLive.setValue(null);
      }
      if (quantityUnitLive.getValue() != null) {
        quantityUnitLive.setValue(null);
      }
    }
    if (barcodeLive.getValue() != null && productLive.getValue() == null) {
      productNameErrorLive.setValue(R.string.error_empty);
      return false;
    }
    if ((noteLive.getValue() == null || noteLive.getValue().isEmpty())
        && productLive.getValue() == null) {
      productNameErrorLive.setValue(R.string.error_empty);
      return false;
    }
    productNameErrorLive.setValue(null);
    return true;
  }

  public boolean isAmountValid() {
    if (productLive.getValue() == null
        && (noteLive.getValue() == null || noteLive.getValue().isEmpty())
    ) {
      amountErrorLive.setValue(null);
      return true;
    }
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountErrorLive.setValue(getString(R.string.error_empty));
      return false;
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
    if (NumUtil.toDouble(amountLive.getValue()) <= 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  private boolean isQuantityUnitValid() {
    if (productLive.getValue() != null && quantityUnitLive.getValue() == null) {
      quantityUnitErrorLive.setValue(true);
      return false;
    }
    quantityUnitErrorLive.setValue(false);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = shoppingListLive.getValue() != null;
    valid = isProductNameValid() && valid;
    valid = isAmountValid() && valid;
    valid = isQuantityUnitValid() && valid;
    return valid;
  }

  public ShoppingListItem fillShoppingListItem(@Nullable ShoppingListItem item) {
    if (!isFormValid()) {
      return null;
    }
    ShoppingList shoppingList = shoppingListLive.getValue();
    Product product = productLive.getValue();
    String amountStock = amountStockLive.getValue();
    String amount = amountLive.getValue();
    String note = noteLive.getValue();
    QuantityUnit unit = quantityUnitLive.getValue();

    assert shoppingList != null;
    if (item == null) {
      item = new ShoppingListItem();
    }
    item.setShoppingListId(shoppingList.getId());
    item.setProductId(product != null ? String.valueOf(product.getId()) : null);
    item.setQuId(unit != null ? String.valueOf(unit.getId()) : null);
    item.setAmountDouble(amountStock != null
        ? NumUtil.toDouble(amountStock) : NumUtil.toDouble(amount), maxDecimalPlacesAmount);
    item.setNote(note != null ? note.trim() : null);
    return item;
  }

  public ProductBarcode fillProductBarcode(@Nullable ProductBarcode productBarcode) {
    if (!isFormValid()) {
      return null;
    }
    String barcode = barcodeLive.getValue();
    Product product = productLive.getValue();

    if (productBarcode == null) {
      productBarcode = new ProductBarcode();
    }
    if (product == null) {
      return productBarcode;
    }
    productBarcode.setProductIdInt(product.getId());
    productBarcode.setBarcode(barcode);
    return productBarcode;
  }

  public void clearForm() {
    barcodeLive.setValue(null);
    amountLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitsFactorsLive.setValue(null);
    productLive.setValue(null);
    productNameLive.setValue(null);
    barcodeLive.setValue(null);
    noteLive.setValue(null);
    new Handler().postDelayed(() -> {
      productNameErrorLive.setValue(null);
      quantityUnitErrorLive.setValue(false);
      amountErrorLive.setValue(null);
    }, 50);
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
