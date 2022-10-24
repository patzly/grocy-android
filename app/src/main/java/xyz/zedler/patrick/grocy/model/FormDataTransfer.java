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
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.TransferFragmentArgs;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataTransfer {

  private final static String TAG = FormDataTransfer.class.getSimpleName();

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
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
  private ArrayList<StockLocation> stockLocations;
  private final MutableLiveData<StockLocation> fromLocationLive;
  private final LiveData<String> fromLocationNameLive;
  private final MutableLiveData<Location> toLocationLive;
  private final LiveData<String> toLocationNameLive;
  private final MutableLiveData<Boolean> toLocationErrorLive;
  private final MutableLiveData<Boolean> useSpecificLive;
  private ArrayList<StockEntry> stockEntries;
  private final MutableLiveData<StockEntry> specificStockEntryLive;
  private final PluralUtil pluralUtil;
  private boolean currentProductFlowInterrupted = false;
  private final int maxDecimalPlacesAmount;

  public FormDataTransfer(
      Application application,
      SharedPreferences sharedPrefs,
      TransferFragmentArgs args
  ) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
    displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    ));
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    scannerVisibilityLive = new MutableLiveData<>(false);
    if (getCameraScannerWasVisibleLastTime() && !getExternalScannerEnabled() && !args
        .getCloseWhenFinished()) {
      scannerVisibilityLive.setValue(true);
    }
    productsLive = new MutableLiveData<>(new ArrayList<>());
    productDetailsLive = new MutableLiveData<>();
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
    fromLocationLive = new MutableLiveData<>();
    fromLocationNameLive = Transformations.map(
        fromLocationLive,
        location -> location != null ? location.getLocationName() : null
    );
    toLocationLive = new MutableLiveData<>();
    toLocationNameLive = Transformations.map(
        toLocationLive,
        location -> location != null ? location.getName() : null
    );
    toLocationErrorLive = (MutableLiveData<Boolean>) Transformations.map(
        toLocationLive,
        quantityUnit -> !isToLocationValid()
    );
    useSpecificLive = new MutableLiveData<>(false);
    specificStockEntryLive = new MutableLiveData<>();
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
        .putBoolean(PREF.CAMERA_SCANNER_VISIBLE_TRANSFER, isScannerVisible())
        .apply();
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
    if ((double) currentFactor == -1) {
      amountMultiplied = amount;
    } else if (current.getId() == productDetails.getProduct()
        .getQuIdPurchaseInt()) {
      amountMultiplied = amount * (double) currentFactor;
    } else {
      amountMultiplied = amount / (double) currentFactor;
    }
    return NumUtil.trimAmount(amountMultiplied, maxDecimalPlacesAmount);
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
      if (productDetailsLive.getValue() == null) {
        amountLive.setValue(String.valueOf(1));
      }
    } else {
      double amountNew = Double.parseDouble(amountLive.getValue()) + 1;
      amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
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
        amountLive.setValue(NumUtil.trimAmount(amountNew, maxDecimalPlacesAmount));
      }
    }
  }

  public String getTransactionSuccessMsg(double amountTransferred) {
    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit stock = quantityUnitStockLive.getValue();
    assert productDetails != null && stock != null;
    return application.getString(
        R.string.msg_transferred,
        NumUtil.trimAmount(amountTransferred, maxDecimalPlacesAmount),
        pluralUtil.getQuantityUnitPlural(stock, amountTransferred),
        productDetails.getProduct().getName()
    );
  }

  public MutableLiveData<Boolean> getUseSpecificLive() {
    return useSpecificLive;
  }

  public ArrayList<StockEntry> getStockEntries() {
    return stockEntries;
  }

  public void setStockEntries(ArrayList<StockEntry> stockEntries) {
    this.stockEntries = stockEntries;
  }

  public MutableLiveData<StockEntry> getSpecificStockEntryLive() {
    return specificStockEntryLive;
  }

  public ArrayList<StockLocation> getStockLocations() {
    return stockLocations;
  }

  public void setStockLocations(ArrayList<StockLocation> stockLocations) {
    this.stockLocations = stockLocations;
  }

  public MutableLiveData<StockLocation> getFromLocationLive() {
    return fromLocationLive;
  }

  public LiveData<String> getFromLocationNameLive() {
    return fromLocationNameLive;
  }

  public MutableLiveData<Location> getToLocationLive() {
    return toLocationLive;
  }

  public LiveData<String> getToLocationNameLive() {
    return toLocationNameLive;
  }

  public MutableLiveData<Boolean> getToLocationErrorLive() {
    return toLocationErrorLive;
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
    if (NumUtil.getDecimalPlacesCount(amountLive.getValue()) > maxDecimalPlacesAmount) {
      amountErrorLive.setValue(application.getString(
          R.string.error_max_decimal_places, String.valueOf(maxDecimalPlacesAmount)
      ));
      return false;
    }
    // below
    if (Double.parseDouble(amountLive.getValue()) <= 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    }

    // over
    StockLocation currentLocation = fromLocationLive.getValue();
    if (currentLocation == null) {
      amountErrorLive.setValue(null);
      return true;
    }
    double stockAmount;
    StockEntry specificStockEntry = specificStockEntryLive.getValue();
    if (specificStockEntry == null) {
      stockAmount = currentLocation.getAmountDouble();
    } else {
      stockAmount = specificStockEntry.getAmount();
    }

    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit current = quantityUnitLive.getValue();
    HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
    Double currentFactor = hashMap != null ? hashMap.get(current) : null;
    double maxAmount;
    if (currentFactor == null || currentFactor == -1) {
      maxAmount = stockAmount;
    } else if (current != null && productDetails != null
        && current.getId() == productDetails.getProduct().getQuIdPurchaseInt()) {
      maxAmount = stockAmount / currentFactor;
    } else {
      maxAmount = stockAmount * currentFactor;
    }

    if (Double.parseDouble(amountLive.getValue()) > maxAmount) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_max, NumUtil.trimAmount(maxAmount, maxDecimalPlacesAmount)
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public boolean isToLocationValid() {
    if (toLocationLive.getValue() == null) {
      toLocationErrorLive.setValue(true);
      return false;
    }
    if (toLocationLive.getValue() != null && fromLocationLive.getValue() != null) {
      if (toLocationLive.getValue().getId() == fromLocationLive.getValue().getLocationId()) {
        toLocationErrorLive.setValue(true);
        return false;
      }
    }
    toLocationErrorLive.setValue(false);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isProductNameValid();
    valid = isQuantityUnitValid() && valid;
    valid = isAmountValid() && valid;
    valid = isToLocationValid() && valid;
    return valid;
  }

  public String getConfirmationText() {
    ProductDetails productDetails = productDetailsLive.getValue();
    assert productDetails != null && amountStockLive.getValue() != null;
    double amountRemoved = Double.parseDouble(amountStockLive.getValue());
    QuantityUnit qU = quantityUnitLive.getValue();
    StockLocation fromLocation = fromLocationLive.getValue();
    Location toLocation = toLocationLive.getValue();
    assert qU != null && fromLocation != null && toLocation != null;
    return application.getString(
        R.string.msg_quick_mode_confirm_transfer,
        NumUtil.trimAmount(amountRemoved, maxDecimalPlacesAmount),
        pluralUtil.getQuantityUnitPlural(qU, amountRemoved),
        productDetails.getProduct().getName(),
        fromLocation.getLocationName(),
        toLocation.getName()
    );
  }

  public JSONObject getFilledJSONObject() {
    String amount = getAmountStock();
    assert amount != null;
    StockLocation fromLocation = fromLocationLive.getValue();
    Location toLocation = toLocationLive.getValue();
    StockEntry stockEntry = specificStockEntryLive.getValue();
    assert fromLocation != null && toLocation != null;

    JSONObject json = new JSONObject();
    try {
      json.put("amount", amount);
      json.put("location_id_from", String.valueOf(fromLocation.getLocationId()));
      json.put("location_id_to", String.valueOf(toLocation.getId()));
      if (stockEntry != null) {
        json.put("stock_entry_id", stockEntry.getStockId());
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
    productDetailsLive.setValue(null);
    productNameLive.setValue(null);
    fromLocationLive.setValue(null);
    toLocationLive.setValue(null);
    toLocationErrorLive.setValue(false);
    useSpecificLive.setValue(false);
    specificStockEntryLive.setValue(null);
    new Handler().postDelayed(() -> {
      productNameErrorLive.setValue(null);
      quantityUnitErrorLive.setValue(false);
      amountErrorLive.setValue(null);
    }, 50);
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public boolean getCameraScannerWasVisibleLastTime() {
    return sharedPrefs.getBoolean(
        PREF.CAMERA_SCANNER_VISIBLE_TRANSFER,
        false
    );
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
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
