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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

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
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.InventoryFragmentArgs;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

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
  private final MutableLiveData<String> priceErrorLive;
  private final MediatorLiveData<String> priceHintLive;
  private final MutableLiveData<Boolean> showStoreSection;
  private final MutableLiveData<Store> storeLive;
  private final LiveData<String> storeNameLive;
  private final MutableLiveData<Location> locationLive;
  private final LiveData<String> locationNameLive;
  private final PluralUtil pluralUtil;
  private boolean torchOn = false;

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
    priceErrorLive = new MutableLiveData<>();
    priceHintLive = new MediatorLiveData<>();
    priceHintLive.addSource(quantityUnitStockLive, i -> {
      if (currency != null && !currency.isEmpty() && i != null) {
        priceHintLive.setValue(
            application.getString(R.string.property_price_unit_in, i.getName(), currency));
      } else if (currency != null && !currency.isEmpty()) {
        priceHintLive.setValue(application.getString(R.string.property_price_in, currency));
      } else if (i != null) {
        priceHintLive.setValue(application.getString(R.string.property_price_unit, i.getName()));
      } else {
        priceHintLive.setValue(getString(R.string.property_price));
      }
    });
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
    pluralUtil = new PluralUtil(application);
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
    IconUtil.start(view);
    if (amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
      if (!isTareWeightEnabled() || productDetailsLive.getValue() == null) {
        amountLive.setValue(String.valueOf(1));
      } else {
        amountLive.setValue(NumUtil.trim(productDetailsLive.getValue()
            .getProduct().getTareWeightDouble()));
      }
    } else {
      double amountNew = Double.parseDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trim(amountNew));
    }
  }

  public void lessAmount(ImageView view) {
    IconUtil.start(view);
    if (amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
      double amountCurrent = Double.parseDouble(amountLive.getValue());
      Double amountNew = null;
      if (amountCurrent >= 1) {
        amountNew = amountCurrent - 1;
      }
      if (amountNew != null) {
        amountLive.setValue(NumUtil.trim(amountNew));
      }
    }
  }

  private String getTransactionAmountHelpText() {
    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit stockUnit = quantityUnitStockLive.getValue();
    String amountStock = amountStockLive.getValue();
    if (productDetails == null || stockUnit == null || !NumUtil.isStringDouble(amountStock)
        || Double.parseDouble(amountStock) == productDetails.getStockAmount()) {
      return null;
    }
    double amountDiff = Double.parseDouble(amountStock) - productDetails.getStockAmount();
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
        NumUtil.trim(amountDiffAbs),
        pluralUtil.getQuantityUnitPlural(stockUnit, amountDiffAbs)
    );
  }

  private boolean getProductWillBeAdded() {
    ProductDetails productDetails = productDetailsLive.getValue();
    String amountStock = amountStockLive.getValue();
    if (productDetails == null || !NumUtil.isStringDouble(amountStock)
        || Double.parseDouble(amountStock) == productDetails.getStockAmount()) {
      return false;
    }
    double amountDiff = Double.parseDouble(amountStock) - productDetails.getStockAmount();
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
    if (isTareWeightEnabled()) {
      amountStock = NumUtil.trim(Double.parseDouble(amountStock)
          - productDetailsLive.getValue().getProduct().getTareWeightDouble());
    }
    return application.getString(
        R.string.msg_inventoried,
        productDetailsLive.getValue().getProduct().getName(),
        amountStock,
        pluralUtil.getQuantityUnitPlural(stockUnit, amountDiff),
        amountDiff >= 0 ? "+" + NumUtil.trim(amountDiff) : NumUtil.trim(amountDiff)
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

  public MutableLiveData<String> getPriceErrorLive() {
    return priceErrorLive;
  }

  public MediatorLiveData<String> getPriceHintLive() {
    return priceHintLive;
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

  public boolean isTorchOn() {
    return torchOn;
  }

  public void setTorchOn(boolean torchOn) {
    this.torchOn = torchOn;
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
    if (!isTareWeightEnabled()&& NumUtil.isStringDouble(amountLive.getValue())
        && Double.parseDouble(amountLive.getValue()) == productDetails.getStockAmount()) {
      amountErrorLive.setValue(application.getString(R.string.error_amount_equal_stock,
          NumUtil.trim(productDetails.getStockAmount())));
      return false;
    }
    if (isTareWeightEnabled() && NumUtil.isStringDouble(amountLive.getValue())
        && Double.parseDouble(amountLive.getValue()) == productDetails.getStockAmount()
        + productDetails.getProduct().getTareWeightDouble()) {
      amountErrorLive.setValue(application.getString(R.string.error_amount_equal_stock,
          NumUtil.trim(productDetails.getStockAmount()
              + productDetails.getProduct().getTareWeightDouble())));
      return false;
    }
    if (!isTareWeightEnabled() && Double.parseDouble(amountLive.getValue()) < 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_min, String.valueOf(0)
      ));
      return false;
    } else if (isTareWeightEnabled() && Double.parseDouble(amountLive.getValue())
        < productDetails.getProduct().getTareWeightDouble()
    ) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_min,
          NumUtil.trim(productDetails.getProduct().getTareWeightDouble())
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
    priceErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isProductNameValid();
    valid = isQuantityUnitValid() && valid;
    valid = isAmountValid() && valid;
    valid = isDueDateValid() && valid;
    valid = isPriceValid() && valid;
    return valid;
  }

  public String getConfirmationText() {
    assert productDetailsLive.getValue() != null && amountStockLive.getValue() != null;
    double amountNew = Double.parseDouble(amountStockLive.getValue());
    if (isTareWeightEnabled()) {
      amountNew -= productDetailsLive.getValue().getProduct().getTareWeightDouble();
    }
    QuantityUnit qU = quantityUnitLive.getValue();
    ProductDetails details = productDetailsLive.getValue();
    String price = priceLive.getValue();
    assert qU != null && details != null;
    if (NumUtil.isStringDouble(price)) {
      price = NumUtil.trimPrice(Double.parseDouble(price));
      if (currency != null && !currency.isEmpty()) {
        price += " " + currency;
      }
    } else {
      price = getString(R.string.subtitle_empty);
    }
    String store = storeNameLive.getValue();
    if (store == null) {
      store = getString(R.string.subtitle_none_selected);
    }

    return application.getString(
        R.string.msg_quick_mode_confirm_inventory,
        details.getProduct().getName(),
        NumUtil.trim(amountNew),
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
    String price = priceLive.getValue();
    Store store = storeLive.getValue();
    String storeId = store != null ? String.valueOf(store.getId()) : null;
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

    ProductBarcode productBarcode = new ProductBarcode();
    productBarcode.setProductId(product.getId());
    productBarcode.setBarcode(barcode);
    return productBarcode;
  }

  public void clearForm() {
    barcodeLive.setValue(null);
    amountLive.setValue(null);
    quantityUnitLive.setValue(null);
    quantityUnitsFactorsLive.setValue(null);
    productDetailsLive.setValue(null);
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
