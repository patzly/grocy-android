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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragmentArgs;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataPurchase {

  private final static String TAG = FormDataPurchase.class.getSimpleName();

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final String currency;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Integer> batchModeItemIndexLive;
  private final LiveData<String> batchModeTextLive;
  private final MutableLiveData<ShoppingListItem> shoppingListItemLive;
  private final LiveData<List<PendingProduct>> pendingProductsLive;
  private final LiveData<List<PendingProductBarcode>> pendingProductBarcodesLive;
  private final MutableLiveData<PendingProduct> pendingProductLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
  private final LiveData<Boolean> isTareWeightEnabledLive;
  private final MutableLiveData<String> productNameLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<String> barcodeLive;
  private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
  private final LiveData<QuantityUnit> quantityUnitStockLive;
  private final MutableLiveData<QuantityUnit> quantityUnitLive;
  private final LiveData<String> quantityUnitNameLive;
  private final MutableLiveData<Boolean> quantityUnitErrorLive;
  private final MutableLiveData<String> amountLive;
  private final MutableLiveData<String> amountErrorLive;
  private final MediatorLiveData<String> amountHelperLive;
  private final LiveData<String> amountHintLive;
  private final MediatorLiveData<String> amountStockLive;
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
  private final LiveData<String> unitPriceTextLive;
  private final MutableLiveData<Boolean> isTotalPriceLive;
  private final MutableLiveData<Boolean> showStoreSection;
  private final MutableLiveData<Store> storeLive;
  private final LiveData<String> storeNameLive;
  private final MutableLiveData<Location> locationLive;
  private final LiveData<String> locationNameLive;
  private final PluralUtil pluralUtil;
  private boolean currentProductFlowInterrupted = false;

  public FormDataPurchase(
      Application application,
      SharedPreferences sharedPrefs,
      PurchaseFragmentArgs args
  ) {
    DateUtil dateUtil = new DateUtil(application);
    AppDatabase appDatabase = AppDatabase.getAppDatabase(application);
    pendingProductsLive = appDatabase.pendingProductDao().getAllLive();
    pendingProductBarcodesLive = appDatabase.pendingProductBarcodeDao().getAllLive();
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
    displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    ));
    batchModeItemIndexLive = new MutableLiveData<>(args.getShoppingListItems() != null ? 0 : null);
    batchModeTextLive = Transformations.map(
        batchModeItemIndexLive,
        index -> index != null && args.getShoppingListItems() != null ? application.getString(
            R.string.subtitle_entry_num_of_num,
            index+1, args.getShoppingListItems().length
        ) : null
    );
    shoppingListItemLive = new MutableLiveData<>();
    pendingProductLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);
    if (args.getStartWithScanner() && !getExternalScannerEnabled() && !args
        .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    } else if (getCameraScannerWasVisibleLastTime() && !getExternalScannerEnabled() && !args
        .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    }
    productsLive = new MutableLiveData<>(new ArrayList<>());
    productDetailsLive = new MutableLiveData<>();
    isTareWeightEnabledLive = Transformations.map(
        productDetailsLive,
        productDetails -> productDetails != null
            && productDetails.getProduct().getEnableTareWeightHandlingBoolean()
    );
    productDetailsLive.setValue(null);
    productNameLive = new MutableLiveData<>();
    productNameErrorLive = new MutableLiveData<>();
    barcodeLive = new MutableLiveData<>();
    quantityUnitsFactorsLive = new MutableLiveData<>();
    quantityUnitStockLive = Transformations.map(
        quantityUnitsFactorsLive,
        this::getStockQuantityUnit
    );
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
    isTotalPriceLive = new MutableLiveData<>(false);
    priceStockLive = new MediatorLiveData<>();
    priceStockLive.addSource(amountLive, i -> priceStockLive.setValue(getPriceStock()));
    priceStockLive.addSource(priceLive, i -> priceStockLive.setValue(getPriceStock()));
    priceStockLive.addSource(isTotalPriceLive, i -> priceStockLive.setValue(getPriceStock()));
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
    unitPriceTextLive = Transformations.map(
        quantityUnitLive,
        quantityUnit -> quantityUnit != null
            ? application
            .getString(R.string.title_unit_price_specific, quantityUnit.getName())
            : getString(R.string.title_unit_price)
    );
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
    pluralUtil = new PluralUtil(application);
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public LiveData<List<PendingProduct>> getPendingProductsLive() {
    return pendingProductsLive;
  }

  public LiveData<List<PendingProductBarcode>> getPendingProductBarcodesLive() {
    return pendingProductBarcodesLive;
  }

  public MutableLiveData<Integer> getBatchModeItemIndexLive() {
    return batchModeItemIndexLive;
  }

  public LiveData<String> getBatchModeTextLive() {
    return batchModeTextLive;
  }

  public MutableLiveData<ShoppingListItem> getShoppingListItemLive() {
    return shoppingListItemLive;
  }

  public MutableLiveData<PendingProduct> getPendingProductLive() {
    return pendingProductLive;
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
        .putBoolean(Constants.PREF.CAMERA_SCANNER_VISIBLE_PURCHASE, isScannerVisible())
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

  public MutableLiveData<Integer> getProductNameErrorLive() {
    return productNameErrorLive;
  }

  public MutableLiveData<HashMap<QuantityUnit, Double>> getQuantityUnitsFactorsLive() {
    return quantityUnitsFactorsLive;
  }

  public LiveData<QuantityUnit> getQuantityUnitStockLive() {
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

  private QuantityUnit getStockQuantityUnit(HashMap<QuantityUnit, Double> unitsFactors) {
    if (unitsFactors == null || !unitsFactors.containsValue((double) -1)) {
      return null;
    }
    for (Map.Entry<QuantityUnit, Double> entry : unitsFactors.entrySet()) {
      if (entry.getValue() == -1) {
        return entry.getKey();
      }
    }
    return null;
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
    QuantityUnit stock = quantityUnitStockLive.getValue();
    QuantityUnit current = quantityUnitLive.getValue();
    if (!isAmountValid() || quantityUnitsFactorsLive.getValue() == null) {
      return null;
    }
    assert amountLive.getValue() != null;

    if (stock == null || current == null || productDetails == null) {
      return null;
    }
    HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
    double amount = Double.parseDouble(amountLive.getValue());
    Object currentFactor = hashMap.get(current);
    if (currentFactor == null) {
      return null;
    }
    double amountMultiplied;
    if (isTareWeightEnabled() || (double) currentFactor == -1) {
      amountMultiplied = amount;
    } else if (current.getId() == productDetails.getProduct()
        .getQuIdPurchaseInt()) {
      amountMultiplied = amount * (double) currentFactor;
    } else {
      amountMultiplied = amount / (double) currentFactor;
    }
    return NumUtil.trim(amountMultiplied);
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
        pluralUtil.getQuantityUnitPlural(stock, Double.parseDouble(amountStockLive.getValue()))
    );
  }

  public void moreAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      if (!isTareWeightEnabled() || productDetailsLive.getValue() == null) {
        amountLive.setValue(String.valueOf(1));
      } else {
        amountLive.setValue(NumUtil.trim(productDetailsLive.getValue()
            .getProduct().getTareWeightDouble()
            + productDetailsLive.getValue().getStockAmount() + 1));
      }
    } else {
      double amountNew = Double.parseDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trim(amountNew));
    }
  }

  public void lessAmount(ImageView view) {
    ViewUtil.startIcon(view);
    if (amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
      double amountCurrent = Double.parseDouble(amountLive.getValue());
      Double amountNew = null;
      if (amountCurrent > 1) {
        amountNew = amountCurrent - 1;
      }
      if (amountNew != null) {
        amountLive.setValue(NumUtil.trim(amountNew));
      }
    }
  }

  public String getTransactionSuccessMsg(double amountPurchased) {
    QuantityUnit stock = quantityUnitStockLive.getValue();
    assert stock != null && productDetailsLive.getValue() != null;
    return application.getString(
        R.string.msg_purchased,
        NumUtil.trim(amountPurchased),
        pluralUtil.getQuantityUnitPlural(stock, amountPurchased),
        productDetailsLive.getValue().getProduct().getName()
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
    String amountString = amountLive.getValue();
    String priceString = priceLive.getValue();
    boolean isTotalPrice = isTotalPriceLive.getValue();

    if (!NumUtil.isStringDouble(priceString) || !NumUtil.isStringDouble(amountString)) {
      return null;
    }
    if (stock == null || current == null || productDetails == null || hashMap == null) {
      return null;
    }

    double amount = Double.parseDouble(amountString);
    double price = Double.parseDouble(priceString);
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
    if (isTotalPrice) {
      priceMultiplied /= amount;
    }
    return NumUtil.trimPrice(priceMultiplied);
  }

  public MediatorLiveData<String> getPriceStockLive() {
    return priceStockLive;
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
    if (current == null || stock == null
        || current.getId() == stock.getId() && !isTotalPriceLive.getValue()) {
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
      priceLive.setValue(NumUtil.trimPrice(1));
    } else {
      double priceNew = NumUtil.toDouble(priceLive.getValue()) + 1;
      priceLive.setValue(NumUtil.trimPrice(priceNew));
    }
  }

  public void lessPrice() {
    if (priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
      return;
    }
    double priceNew = NumUtil.toDouble(priceLive.getValue()) - 1;
    if (priceNew >= 0) {
      priceLive.setValue(NumUtil.trimPrice(priceNew));
    } else {
      priceLive.setValue(null);
    }
  }

  public MutableLiveData<Boolean> getIsTotalPriceLive() {
    return isTotalPriceLive;
  }

  public void setIsTotalPriceLive(boolean isTotal) {
    isTotalPriceLive.setValue(isTotal);
  }

  public LiveData<String> getUnitPriceTextLive() {
    return unitPriceTextLive;
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

  public boolean isCurrentProductFlowNotInterrupted() {
    return !currentProductFlowInterrupted;
  }

  public void setCurrentProductFlowInterrupted(boolean currentProductFlowInterrupted) {
    this.currentProductFlowInterrupted = currentProductFlowInterrupted;
  }

  public boolean isProductNameValid() {
    if (productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
      if (productDetailsLive.getValue() != null || pendingProductLive.getValue() != null) {
        clearForm();
        return false;
      }
    }
    if (productDetailsLive.getValue() == null && productNameLive.getValue() == null
        || productDetailsLive.getValue() == null && productNameLive.getValue().isEmpty()) {
      productNameErrorLive.setValue(R.string.error_empty);
      return false;
    }
    if (productDetailsLive.getValue() == null && !productNameLive.getValue().isEmpty()
            && pendingProductLive.getValue() == null) {
      productNameErrorLive.setValue(R.string.error_invalid_product);
      return false;
    }
    if (productDetailsLive.getValue() != null && !productNameLive.getValue().isEmpty()
        && !productDetailsLive.getValue().getProduct().getName()
        .equals(productNameLive.getValue()) || pendingProductLive.getValue() != null
            && !productNameLive.getValue().isEmpty()
            && !pendingProductLive.getValue().getName()
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
    if (productDetailsLive.getValue() == null) {
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
    if (!isTareWeightEnabled() && Double.parseDouble(amountLive.getValue()) <= 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    } else if (isTareWeightEnabled() && productDetailsLive.getValue() != null
        && Double.parseDouble(amountLive.getValue())
        <= productDetailsLive.getValue().getProduct().getTareWeightDouble()
        + productDetailsLive.getValue().getStockAmount()
    ) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher,
          NumUtil.trim(productDetailsLive.getValue().getProduct()
              .getTareWeightDouble() + productDetailsLive.getValue().getStockAmount())
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public boolean isDueDateValid() {
    if (dueDateLive.getValue() == null || dueDateLive.getValue().isEmpty()) {
      dueDateErrorLive.setValue(true);
      return false;
    } else {
      dueDateErrorLive.setValue(false);
      return true;
    }
  }

  public boolean isPriceValid() {
    if (priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
      priceErrorLive.setValue(null);
      return true;
    }
    if (!NumUtil.isStringNum(priceLive.getValue())) {
      priceErrorLive.setValue(getString(R.string.error_invalid_price));
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
    ProductDetails details = productDetailsLive.getValue();
    assert details != null || pendingProductLive.getValue() != null;
    assert NumUtil.isStringDouble(amountStockLive.getValue())
            || NumUtil.isStringDouble(amountLive.getValue());
    assert amountLive.getValue() != null;
    double amountAdded = amountStockLive.getValue() != null
            ? Double.parseDouble(amountStockLive.getValue())
            : Double.parseDouble(amountLive.getValue());
    if (details != null && isTareWeightEnabled()) {
      amountAdded -= details.getStockAmount();
      amountAdded -= details.getProduct().getTareWeightDouble();
    }
    QuantityUnit qU = quantityUnitLive.getValue();
    String price = getString(R.string.subtitle_feature_disabled);
    if (isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      price = priceLive.getValue();
      if (NumUtil.isStringDouble(price)) {
        price = NumUtil.trimPrice(Double.parseDouble(price));
        if (currency != null && !currency.isEmpty()) {
          price += " " + currency;
        }
      } else {
        price = getString(R.string.subtitle_empty);
      }
    }

    String store = storeNameLive.getValue();
    if (!isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      store = getString(R.string.subtitle_feature_disabled);
    } else if (store == null) {
      store = getString(R.string.subtitle_none_selected);
    }

    return application.getString(
        R.string.msg_quick_mode_confirm_purchase,
        NumUtil.trim(amountAdded),
        qU != null ? pluralUtil.getQuantityUnitPlural(qU, amountAdded) : "",
        details != null ? details.getProduct().getName() : pendingProductLive.getValue().getName(),
        dueDateTextLive.getValue(),
        price,
        store,
        locationNameLive.getValue()
    );
  }

  public JSONObject getFilledJSONObject() {
    String amount = getAmountStock();
    assert amount != null;
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
      json.put("amount", amount);
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

    ProductBarcode productBarcode = new ProductBarcode();
    productBarcode.setProductIdInt(product.getId());
    productBarcode.setBarcode(barcode);
    if (store != null && isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      productBarcode.setStoreId(String.valueOf(store.getId()));
    }
    return productBarcode;
  }

  public void clearForm() {
    currentProductFlowInterrupted = false;
    barcodeLive.setValue(null);
    amountLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitsFactorsLive.setValue(null);
    productDetailsLive.setValue(null);
    pendingProductLive.setValue(null);
    productNameLive.setValue(null);
    purchasedDateLive.setValue(null);
    dueDateLive.setValue(null);
    priceLive.setValue(null);
    storeLive.setValue(null);
    showStoreSection.setValue(true);
    locationLive.setValue(null);
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
        Constants.PREF.CAMERA_SCANNER_VISIBLE_PURCHASE,
        false
    );
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
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
