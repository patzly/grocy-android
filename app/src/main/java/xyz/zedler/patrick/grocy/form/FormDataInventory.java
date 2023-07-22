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
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.InventoryFragmentArgs;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.QuantityUnitConversionUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataInventory {

  private final static String TAG = FormDataInventory.class.getSimpleName();

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final String currency;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
  private final LiveData<Boolean> isTareWeightEnabledLive;
  private final MutableLiveData<String> productNameLive;
  private final LiveData<String> productNameInfoStockLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
  private final MutableLiveData<QuantityUnit> quantityUnitStockLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final LiveData<String> quantityUnitNameLive;
  private final MutableLiveData<Boolean> quantityUnitErrorLive;
  private final MutableLiveData<String> amountLive;
  private final MutableLiveData<String> amountErrorLive;
  private final MediatorLiveData<String> amountHelperLive;
  private final LiveData<String> amountHintLive;
  private final MediatorLiveData<String> amountStockLive;
  private final MediatorLiveData<String> transactionAmountHelperLive;
  private final MediatorLiveData<Boolean> productWillBeAddedLive;
  private final MutableLiveData<String> purchasedDateLive;
  private final LiveData<String> purchasedDateTextLive;
  private final LiveData<String> purchasedDateTextHumanLive;
  private final MutableLiveData<String> dueDateLive;
  private final LiveData<String> dueDateTextLive;
  private final LiveData<String> dueDateTextHumanLive;
  private final MutableLiveData<Boolean> dueDateErrorLive;
  private final MutableLiveData<String> priceLive;
  private final MediatorLiveData<String> priceStockLive;
  private final MutableLiveData<String> priceErrorLive;
  private final MediatorLiveData<String> priceHelperLive;
  private final String priceHint;
  private final MutableLiveData<Boolean> showStoreSection;
  private final MutableLiveData<Store> storeLive;
  private final LiveData<String> storeNameLive;
  private final MutableLiveData<Location> locationLive;
  private final LiveData<String> locationNameLive;
  private final MutableLiveData<Integer> printLabelTypeLive;
  private final MutableLiveData<String> noteLive;
  private final PluralUtil pluralUtil;
  private boolean currentProductFlowInterrupted = false;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final int decimalPlacesPriceInput;

  public FormDataInventory(
      Application application,
      SharedPreferences sharedPrefs,
      InventoryFragmentArgs args
  ) {
    DateUtil dateUtil = new DateUtil(application);
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
    displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    ));
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    decimalPlacesPriceInput = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_INPUT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_INPUT
    );
    scannerVisibilityLive = new MutableLiveData<>(false);
    if (getCameraScannerWasVisibleLastTime() && !getExternalScannerEnabled() && !args
        .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    }
    pluralUtil = new PluralUtil(application);
    productsLive = new MutableLiveData<>(new ArrayList<>());
    productDetailsLive = new MutableLiveData<>();
    isTareWeightEnabledLive = Transformations.map(
        productDetailsLive,
        productDetails -> productDetails != null
            && productDetails.getProduct().getEnableTareWeightHandlingBoolean()
    );
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
    quantityUnitsFactorsLive = new MutableLiveData<>();
    quantityUnitStockLive = new MutableLiveData<>();
    quantityUnitsFactorsLive.setValue(null);
    quantityUnitLive = new MutableLiveData<>();
    quantityUnitNameLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null ? quantityUnit.getName() : null
    );
    quantityUnitErrorLive = (MutableLiveData<Boolean>) Transformations.map(
        quantityUnitLive,
        quantityUnit -> !isQuantityUnitValid()
    );
    amountLive = new MutableLiveData<>();
    amountErrorLive = new MutableLiveData<>();
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
    transactionAmountHelperLive = new MediatorLiveData<>();
    transactionAmountHelperLive
        .addSource(amountStockLive,
            i -> transactionAmountHelperLive.setValue(getTransactionAmountHelpText()));
    transactionAmountHelperLive
        .addSource(quantityUnitsFactorsLive,
            i -> transactionAmountHelperLive.setValue(getTransactionAmountHelpText()));
    productWillBeAddedLive = new MediatorLiveData<>();
    productWillBeAddedLive
        .addSource(amountStockLive,
            i -> productWillBeAddedLive.setValue(getProductWillBeAdded()));
    productWillBeAddedLive
        .addSource(quantityUnitsFactorsLive,
            i -> productWillBeAddedLive.setValue(getProductWillBeAdded()));
    productWillBeAddedLive.setValue(false);
    purchasedDateLive = new MutableLiveData<>();
    purchasedDateTextLive = Transformations.map(
        purchasedDateLive,
        date -> {
          if (date == null) {
            return getString(R.string.subtitle_none_selected);
          } else {
            return dateUtil.getLocalizedDate(date, DateUtil.FORMAT_MEDIUM);
          }
        }
    );
    purchasedDateTextHumanLive = Transformations.map(
        purchasedDateLive,
        date -> {
          if (date == null || date.equals(Constants.DATE.NEVER_OVERDUE)) {
            return null;
          }
          return dateUtil.getHumanForDaysFromNow(date);
        }
    );
    purchasedDateLive.setValue(null);
    dueDateLive = new MutableLiveData<>();
    dueDateTextLive = Transformations.map(
        dueDateLive,
        date -> {
          if (date == null) {
            return getString(R.string.subtitle_none_selected);
          } else if (date.equals(Constants.DATE.NEVER_OVERDUE)) {
            return getString(R.string.subtitle_never_overdue);
          } else {
            return dateUtil.getLocalizedDate(date, DateUtil.FORMAT_MEDIUM);
          }
        }
    );
    dueDateTextHumanLive = Transformations.map(
        dueDateLive,
        date -> {
          if (date == null || date.equals(Constants.DATE.NEVER_OVERDUE)) {
            return null;
          }
          return dateUtil.getHumanForDaysFromNow(date);
        }
    );
    dueDateLive.setValue(null);
    dueDateErrorLive = new MutableLiveData<>();
    priceLive = new MutableLiveData<>();
    priceStockLive = new MediatorLiveData<>();
    priceStockLive.addSource(priceLive, i -> priceStockLive.setValue(getPriceStock()));
    priceStockLive.addSource(quantityUnitLive, i -> priceStockLive.setValue(getPriceStock()));
    priceErrorLive = new MutableLiveData<>();
    priceHelperLive = new MediatorLiveData<>();
    priceHelperLive.addSource(priceStockLive, i -> priceHelperLive.setValue(getPriceHelpText()));
    priceHelperLive.addSource(quantityUnitLive, i -> priceHelperLive.setValue(getPriceHelpText()));
    if (currency != null && !currency.isEmpty()) {
      priceHint = application.getString(R.string.property_price_in, currency);
    } else {
      priceHint = getString(R.string.property_price);
    }
    quantityUnitLive.setValue(null);
    showStoreSection = new MutableLiveData<>(true);
    storeLive = new MutableLiveData<>();
    storeNameLive = Transformations.map(
        storeLive,
        store -> store != null ? store.getName() : null
    );
    locationLive = new MutableLiveData<>();
    locationNameLive = Transformations.map(
        locationLive,
        location -> location != null ? location.getName() : null
    );
    printLabelTypeLive = new MutableLiveData<>(0);
    noteLive = new MutableLiveData<>();
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
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
        .putBoolean(PREF.CAMERA_SCANNER_VISIBLE_INVENTORY, isScannerVisible())
        .apply();
  }

  public MutableLiveData<ArrayList<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<ProductDetails> getProductDetailsLive() {
    return productDetailsLive;
  }

  public LiveData<Boolean> getIsTareWeightEnabledLive() {
    return isTareWeightEnabledLive;
  }

  public boolean isTareWeightEnabled() {
    assert isTareWeightEnabledLive.getValue() != null;
    return isTareWeightEnabledLive.getValue();
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

  public MutableLiveData<HashMap<QuantityUnit, Double>> getQuantityUnitsFactorsLive() {
    return quantityUnitsFactorsLive;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitStockLive() {
    return quantityUnitStockLive;
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

  private String getAmountStock() {
    ProductDetails productDetails = productDetailsLive.getValue();
    if (productDetails == null) return null;
    return QuantityUnitConversionUtil.getAmountStock(
        quantityUnitStockLive.getValue(),
        quantityUnitLive.getValue(),
        amountLive.getValue(),
        quantityUnitsFactorsLive.getValue(),
        false,
        maxDecimalPlacesAmount
    );
  }

  private String getAmountHelpText() {
    QuantityUnit stock = quantityUnitStockLive.getValue();
    QuantityUnit current = quantityUnitLive.getValue();
    if (stock == null || current == null || stock.getId() == current.getId()
        || !NumUtil.isStringDouble(amountStockLive.getValue())) {
      return null;
    }
    return application.getString(
        R.string.subtitle_amount_compare,
        amountStockLive.getValue(),
        pluralUtil.getQuantityUnitPlural(stock, NumUtil.toDouble(amountStockLive.getValue()))
    );
  }

  public void moreAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      if (!isTareWeightEnabled() || productDetailsLive.getValue() == null) {
        amountLive.setValue(String.valueOf(1));
      } else {
        amountLive.setValue(NumUtil.trimAmount(productDetailsLive.getValue()
            .getProduct().getTareWeightDouble(), maxDecimalPlacesAmount));
      }
    } else {
      double amountNew = NumUtil.toDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
      double amountCurrent = NumUtil.toDouble(amountLive.getValue());
      Double amountNew = null;
      if (amountCurrent >= 1) {
        amountNew = amountCurrent - 1;
      }
      if (amountNew != null) {
        amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
      }
    }
  }

  private String getTransactionAmountHelpText() {
    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit stockUnit = quantityUnitStockLive.getValue();
    String amountStock = amountStockLive.getValue();
    if (productDetails == null || stockUnit == null || !NumUtil.isStringDouble(amountStock)
        || NumUtil.toDouble(amountStock) == productDetails.getStockAmount()) {
      return null;
    }
    double amountDiff = NumUtil.toDouble(amountStock) - productDetails.getStockAmount();
    if(isTareWeightEnabled()) {
      amountDiff -= productDetails.getProduct().getTareWeightDouble();
    }
    double amountDiffAbs = Math.abs(amountDiff);
    @PluralsRes int msg = amountDiff > 0
        ? R.plurals.msg_inventory_transaction_add
        : R.plurals.msg_inventory_transaction_remove;
    return application.getResources().getQuantityString(
        msg,
        (int) Math.ceil(amountDiffAbs),
        NumUtil.trimAmount(amountDiffAbs, maxDecimalPlacesAmount),
        pluralUtil.getQuantityUnitPlural(stockUnit, amountDiffAbs)
    );
  }

  private boolean getProductWillBeAdded() {
    ProductDetails productDetails = productDetailsLive.getValue();
    String amountStock = amountStockLive.getValue();
    if (productDetails == null || !NumUtil.isStringDouble(amountStock)
        || NumUtil.toDouble(amountStock) == productDetails.getStockAmount()) {
      return false;
    }
    double amountDiff = NumUtil.toDouble(amountStock) - productDetails.getStockAmount();
    if(isTareWeightEnabled()) {
      amountDiff -= productDetails.getProduct().getTareWeightDouble();
    }
    return amountDiff > 0;
  }

  public MediatorLiveData<String> getTransactionAmountHelperLive() {
    return transactionAmountHelperLive;
  }

  public MediatorLiveData<Boolean> getProductWillBeAddedLive() {
    return productWillBeAddedLive;
  }

  public String getTransactionSuccessMsg(double amountDiff) {
    QuantityUnit stockUnit = quantityUnitStockLive.getValue();
    String amountStock = amountStockLive.getValue();
    assert productDetailsLive.getValue() != null && stockUnit != null && amountStock != null;
    double amountStockDouble = NumUtil.toDouble(amountStock);
    if (isTareWeightEnabled()) {
      amountStock = NumUtil.trimAmount(amountStockDouble
          - productDetailsLive.getValue().getProduct().getTareWeightDouble(), maxDecimalPlacesAmount);
    }
    return application.getString(
        R.string.msg_inventoried,
        productDetailsLive.getValue().getProduct().getName(),
        amountStock,
        pluralUtil.getQuantityUnitPlural(stockUnit, amountStockDouble),
        amountDiff >= 0 ? "+" + NumUtil.trimAmount(amountDiff, maxDecimalPlacesAmount) : NumUtil.trimAmount(amountDiff, maxDecimalPlacesAmount)
    );
  }

  public MutableLiveData<String> getPurchasedDateLive() {
    return purchasedDateLive;
  }

  public LiveData<String> getPurchasedDateTextLive() {
    return purchasedDateTextLive;
  }

  public LiveData<String> getPurchasedDateTextHumanLive() {
    return purchasedDateTextHumanLive;
  }

  public MutableLiveData<String> getDueDateLive() {
    return dueDateLive;
  }

  public LiveData<String> getDueDateTextLive() {
    return dueDateTextLive;
  }

  public LiveData<String> getDueDateTextHumanLive() {
    return dueDateTextHumanLive;
  }

  public MutableLiveData<Boolean> getDueDateErrorLive() {
    return dueDateErrorLive;
  }

  public MutableLiveData<String> getPriceLive() {
    return priceLive;
  }

  private String getPriceStock() {
    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit stock = quantityUnitStockLive.getValue();
    QuantityUnit current = quantityUnitLive.getValue();
    HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
    String priceString = priceLive.getValue();

    if (!NumUtil.isStringDouble(priceString)) {
      return null;
    }
    if (stock == null || current == null || productDetails == null || hashMap == null) {
      return null;
    }

    double price = NumUtil.toDouble(priceString);
    Object currentFactor = hashMap.get(current);
    if (currentFactor == null) {
      return null;
    }

    double priceMultiplied;
    if (isTareWeightEnabled() || (double) currentFactor == -1) {
      priceMultiplied = price;
    } else if (current.getId() == productDetails.getProduct()
        .getQuIdPurchaseInt()) {
      priceMultiplied = price / (double) currentFactor;
    } else {
      priceMultiplied = price * (double) currentFactor;
    }
    return NumUtil.trimPrice(priceMultiplied, decimalPlacesPriceInput);
  }

  public MutableLiveData<String> getPriceErrorLive() {
    return priceErrorLive;
  }

  public String getPriceHint() {
    return priceHint;
  }

  public LiveData<String> getPriceHelperLive() {
    return priceHelperLive;
  }

  private String getPriceHelpText() {
    QuantityUnit current = quantityUnitLive.getValue();
    QuantityUnit stock = quantityUnitStockLive.getValue();
    if (current == null || stock == null || current.getId() == stock.getId()) {
      return " ";
    }
    if (priceStockLive.getValue() == null) {
      return " ";
    }
    String priceWithCurrency = priceStockLive.getValue();
    if (currency != null && !currency.isEmpty()) {
      priceWithCurrency += " " + currency;
    }
    return application.getString(
        R.string.subtitle_price_means,
        priceWithCurrency,
        stock.getName()
    );
  }

  public void morePrice() {
    if (priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
      priceLive.setValue(NumUtil.trimPrice(1, decimalPlacesPriceInput));
    } else {
      double priceNew = NumUtil.toDouble(priceLive.getValue()) + 1;
      priceLive.setValue(NumUtil.trimPrice(priceNew, decimalPlacesPriceInput));
    }
  }

  public void lessPrice() {
    if (priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
      return;
    }
    double priceNew = NumUtil.toDouble(priceLive.getValue()) - 1;
    if (priceNew >= 0) {
      priceLive.setValue(NumUtil.trimPrice(priceNew, decimalPlacesPriceInput));
    } else {
      priceLive.setValue(null);
    }
  }

  public MutableLiveData<Boolean> getShowStoreSection() {
    return showStoreSection;
  }

  public MutableLiveData<Store> getStoreLive() {
    return storeLive;
  }

  public LiveData<String> getStoreNameLive() {
    return storeNameLive;
  }

  public MutableLiveData<Location> getLocationLive() {
    return locationLive;
  }

  public LiveData<String> getLocationNameLive() {
    return locationNameLive;
  }

  public MutableLiveData<Integer> getPrintLabelTypeLive() {
    return printLabelTypeLive;
  }

  public void setPrintLabelTypeLive(int type) {
    printLabelTypeLive.setValue(type);
  }

  public MutableLiveData<String> getNoteLive() {
    return noteLive;
  }

  public boolean isCurrentProductFlowNotInterrupted() {
    return !currentProductFlowInterrupted;
  }

  public void setCurrentProductFlowInterrupted(boolean currentProductFlowInterrupted) {
    this.currentProductFlowInterrupted = currentProductFlowInterrupted;
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
      if (productDetailsLive.getValue() != null) {
        clearForm();
        return false;
      }
    }
    if (productDetailsLive.getValue() == null && productNameLive.getValue() == null
        || productDetailsLive.getValue() == null && productNameLive.getValue().isEmpty()) {
      productNameErrorLive.setValue(R.string.error_empty);
      return false;
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

  private boolean isQuantityUnitValid() {
    if (productDetailsLive.getValue() != null && quantityUnitLive.getValue() == null) {
      quantityUnitErrorLive.setValue(true);
      return false;
    }
    quantityUnitErrorLive.setValue(false);
    return true;
  }

  public boolean isAmountValid() {
    ProductDetails productDetails = productDetailsLive.getValue();
    if (productDetails == null) {
      amountErrorLive.setValue(null);
      return true;
    }
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      amountErrorLive.setValue(getString(R.string.error_empty));
      return false;
    }
    if (!NumUtil.isStringNum(amountLive.getValue())) {
      amountErrorLive.setValue(getString(R.string.error_invalid_amount));
      return false;
    }
    if (NumUtil.getDecimalPlacesCount(amountLive.getValue()) > maxDecimalPlacesAmount) {
      amountErrorLive.setValue(application.getResources().getQuantityString(
          R.plurals.error_max_decimal_places, maxDecimalPlacesAmount, maxDecimalPlacesAmount
      ));
      return false;
    }
    if (!isTareWeightEnabled()&& NumUtil.isStringDouble(amountLive.getValue())
        && NumUtil.toDouble(amountLive.getValue()) == productDetails.getStockAmount()) {
      amountErrorLive.setValue(application.getString(R.string.error_amount_equal_stock,
          NumUtil.trimAmount(productDetails.getStockAmount(), maxDecimalPlacesAmount)));
      return false;
    }
    if (isTareWeightEnabled() && NumUtil.isStringDouble(amountLive.getValue())
        && NumUtil.toDouble(amountLive.getValue()) == productDetails.getStockAmount()
        + productDetails.getProduct().getTareWeightDouble()) {
      amountErrorLive.setValue(application.getString(R.string.error_amount_equal_stock,
          NumUtil.trimAmount(productDetails.getStockAmount()
              + productDetails.getProduct().getTareWeightDouble(), maxDecimalPlacesAmount)));
      return false;
    }
    if (!isTareWeightEnabled() && NumUtil.toDouble(amountLive.getValue()) < 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_min, String.valueOf(0)
      ));
      return false;
    } else if (isTareWeightEnabled() && NumUtil.toDouble(amountLive.getValue())
        < productDetails.getProduct().getTareWeightDouble()
    ) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_min,
          NumUtil.trimAmount(productDetails.getProduct().getTareWeightDouble(), maxDecimalPlacesAmount)
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public boolean isDueDateValid() {
    assert productWillBeAddedLive.getValue() != null;
    if (!productWillBeAddedLive.getValue()) {
      dueDateErrorLive.setValue(false);
      return true;
    }
    if (dueDateLive.getValue() == null || dueDateLive.getValue().isEmpty()) {
      dueDateErrorLive.setValue(true);
      return false;
    }
    dueDateErrorLive.setValue(false);
    return true;
  }

  public boolean isPriceValid() {
    assert productWillBeAddedLive.getValue() != null;
    if (!productWillBeAddedLive.getValue()) {
      priceErrorLive.setValue(null);
      return true;
    }
    if (priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
      priceErrorLive.setValue(null);
      return true;
    }
    if (!NumUtil.isStringNum(priceLive.getValue())) {
      priceErrorLive.setValue(getString(R.string.error_invalid_price));
      return false;
    }
    if (NumUtil.getDecimalPlacesCount(priceLive.getValue()) > decimalPlacesPriceInput) {
      priceErrorLive.setValue(application.getResources().getQuantityString(
          R.plurals.error_max_decimal_places, decimalPlacesPriceInput, decimalPlacesPriceInput
      ));
      return false;
    }
    priceErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isProductNameValid();
    valid = isQuantityUnitValid() && valid;
    valid = isAmountValid() && valid;
    if (isFeatureEnabled(PREF.FEATURE_STOCK_BBD_TRACKING)) {
      valid = isDueDateValid() && valid;
    }
    if (isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      valid = isPriceValid() && valid;
    }
    return valid;
  }

  public String getConfirmationText() {
    assert productDetailsLive.getValue() != null && amountStockLive.getValue() != null;
    double amountNew = NumUtil.toDouble(amountStockLive.getValue());
    if (isTareWeightEnabled()) {
      amountNew -= productDetailsLive.getValue().getProduct().getTareWeightDouble();
    }
    QuantityUnit qU = quantityUnitStockLive.getValue();
    ProductDetails details = productDetailsLive.getValue();
    String price = getString(R.string.subtitle_feature_disabled);
    if (isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      price = priceLive.getValue();
      if (NumUtil.isStringDouble(price)) {
        price = NumUtil.trimPrice(NumUtil.toDouble(price), decimalPlacesPriceDisplay);
        if (currency != null && !currency.isEmpty()) {
          price += " " + currency;
        }
      } else {
        price = getString(R.string.subtitle_empty);
      }
    }
    assert qU != null && details != null;
    String store = storeNameLive.getValue();
    if (!isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      store = getString(R.string.subtitle_feature_disabled);
    } else if (store == null) {
      store = getString(R.string.subtitle_none_selected);
    }

    return application.getString(
        R.string.msg_quick_mode_confirm_inventory,
        details.getProduct().getName(),
        NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount),
        pluralUtil.getQuantityUnitPlural(qU, amountNew),
        dueDateTextLive.getValue(),
        price,
        store,
        locationNameLive.getValue()
    );
  }

  public JSONObject getFilledJSONObject() {
    String amount = getAmountStock();
    assert amount != null && productWillBeAddedLive.getValue() != null;
    String price = null;
    String storeId = null;
    if (isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      price = priceStockLive.getValue();
      Store store = storeLive.getValue();
      storeId = store != null ? String.valueOf(store.getId()) : null;
    }
    Location location = locationLive.getValue();
    String purchasedDate = purchasedDateLive.getValue();
    String dueDate = dueDateLive.getValue();
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      dueDate = Constants.DATE.NEVER_OVERDUE;
    }

    JSONObject json = new JSONObject();
    try {
      json.put("new_amount", amount);
      if (productWillBeAddedLive.getValue()) {
        if (NumUtil.isStringDouble(price)) {
          json.put("price", price);
        }
        if (getPurchasedDateEnabled() && purchasedDate != null) {
          json.put("purchased_date", purchasedDate);
        }
        json.put("best_before_date", dueDate);
        if (storeId != null) {
          json.put("shopping_location_id", storeId);
        }
        if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING) && location != null) {
          json.put("location_id", String.valueOf(location.getId()));
        }
        if (isFeatureEnabled(PREF.FEATURE_LABEL_PRINTER)) {
          json.put("stock_label_type", String.valueOf(printLabelTypeLive.getValue()));
        }
        if (noteLive.getValue() != null && !noteLive.getValue().isEmpty()) {
          json.put("note", noteLive.getValue());
        }
      }
    } catch (JSONException e) {
      if (isDebuggingEnabled()) {
        Log.e(TAG, "getFilledJSONObject: " + e);
      }
    }
    return json;
  }

  public ProductBarcode fillProductBarcode() {
    if (!isFormValid()) {
      return null;
    }
    assert productDetailsLive.getValue() != null;
    String barcode = barcodeLive.getValue();
    Product product = productDetailsLive.getValue().getProduct();
    Store store = storeLive.getValue();
    String note = noteLive.getValue();

    ProductBarcode productBarcode = new ProductBarcode();
    productBarcode.setProductIdInt(product.getId());
    productBarcode.setBarcode(barcode);
    productBarcode.setNote(note);
    if (store != null && isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      productBarcode.setStoreId(String.valueOf(store.getId()));
    }
    return productBarcode;
  }

  public ProductBarcode fillProductBarcodeWithoutForm() {
    if (productDetailsLive.getValue() == null) {
      return null;
    }
    String barcode = barcodeLive.getValue();
    Product product = productDetailsLive.getValue().getProduct();

    ProductBarcode productBarcode = new ProductBarcode();
    productBarcode.setProductIdInt(product.getId());
    productBarcode.setBarcode(barcode);
    return productBarcode;
  }

  public void clearForm() {
    currentProductFlowInterrupted = false;
    barcodeLive.setValue(null);
    amountLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitsFactorsLive.setValue(null);
    quantityUnitStockLive.setValue(null);
    productDetailsLive.setValue(null);
    productNameLive.setValue(null);
    purchasedDateLive.setValue(null);
    dueDateLive.setValue(null);
    priceLive.setValue(null);
    storeLive.setValue(null);
    showStoreSection.setValue(true);
    locationLive.setValue(null);
    printLabelTypeLive.setValue(0);
    noteLive.setValue(null);
    new Handler().postDelayed(() -> {
      productNameErrorLive.setValue(null);
      quantityUnitErrorLive.setValue(false);
      amountErrorLive.setValue(null);
      dueDateErrorLive.setValue(false);
    }, 50);
  }

  public boolean getPurchasedDateEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.STOCK.SHOW_PURCHASED_DATE,
        Constants.SETTINGS_DEFAULT.STOCK.SHOW_PURCHASED_DATE
    );
  }

  public boolean getCameraScannerWasVisibleLastTime() {
    return sharedPrefs.getBoolean(
        PREF.CAMERA_SCANNER_VISIBLE_INVENTORY,
        false
    );
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  public boolean showNotesField() {
    return VersionUtil.isGrocyServerMin330(sharedPrefs);
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  private boolean isDebuggingEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
        Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
    );
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
