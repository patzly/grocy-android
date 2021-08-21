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
import xyz.zedler.patrick.grocy.fragment.ConsumeFragmentArgs;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class FormDataConsume {

  private final static String TAG = FormDataConsume.class.getSimpleName();

  private final Application application;
  private final SharedPreferences sharedPrefs;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;
  private final MutableLiveData<ArrayList<Product>> productsLive;
  private final MutableLiveData<ProductDetails> productDetailsLive;
  private final LiveData<Boolean> isTareWeightEnabledLive;
  private final MutableLiveData<String> productNameLive;
  private final LiveData<String> productNameInfoStockLive;
  private final MutableLiveData<Integer> productNameErrorLive;
  private final MutableLiveData<Boolean> consumeExactAmountLive;
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
  private final MutableLiveData<StockLocation> stockLocationLive;
  private final LiveData<String> stockLocationNameLive;
  private final MutableLiveData<Boolean> spoiledLive;
  private final MutableLiveData<Boolean> useSpecificLive;
  private ArrayList<StockEntry> stockEntries;
  private final MutableLiveData<StockEntry> specificStockEntryLive;
  private final PluralUtil pluralUtil;
  private boolean torchOn = false;

  public FormDataConsume(
      Application application,
      SharedPreferences sharedPrefs,
      ConsumeFragmentArgs args
  ) {
    this.application = application;
    this.sharedPrefs = sharedPrefs;
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
          String info = AmountUtil.getStockAmountInfo(application, pluralUtil, productDetails);
          return info != null ? application.getString(R.string.property_in_stock, info) : " ";
        }
    );
    productNameErrorLive = new MutableLiveData<>();
    consumeExactAmountLive = new MutableLiveData<>(false);
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
    amountStockLive
        .addSource(consumeExactAmountLive, i -> amountStockLive.setValue(getAmountStock()));
    amountHelperLive = new MediatorLiveData<>();
    amountHelperLive
        .addSource(amountStockLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    amountHelperLive
        .addSource(quantityUnitsFactorsLive, i -> amountHelperLive.setValue(getAmountHelpText()));
    stockLocationLive = new MutableLiveData<>();
    stockLocationNameLive = Transformations.map(
        stockLocationLive,
        location -> location != null ? location.getLocationName() : null
    );
    spoiledLive = new MutableLiveData<>(false);
    useSpecificLive = new MutableLiveData<>(false);
    specificStockEntryLive = new MutableLiveData<>();
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
        .putBoolean(Constants.PREF.CAMERA_SCANNER_VISIBLE_CONSUME, isScannerVisible())
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
    assert isTareWeightEnabledLive.getValue() != null && consumeExactAmountLive.getValue() != null;
    return isTareWeightEnabledLive.getValue() && !consumeExactAmountLive.getValue();
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

  public MutableLiveData<Boolean> getConsumeExactAmountLive() {
    return consumeExactAmountLive;
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
            .getProduct().getTareWeightDouble() + 1));
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
      if (amountCurrent > 1) {
        amountNew = amountCurrent - 1;
      }
      if (amountNew != null) {
        amountLive.setValue(NumUtil.trim(amountNew));
      }
    }
  }

  public String getTransactionSuccessMsg(boolean isActionOpen, double amountConsumed) {
    ProductDetails productDetails = productDetailsLive.getValue();
    QuantityUnit stock = quantityUnitStockLive.getValue();
    assert productDetails != null && stock != null;
    return application.getString(
        isActionOpen ? R.string.msg_opened : R.string.msg_consumed,
        NumUtil.trim(amountConsumed),
        pluralUtil.getQuantityUnitPlural(stock, amountConsumed),
        productDetails.getProduct().getName()
    );
  }

  public MutableLiveData<Boolean> getSpoiledLive() {
    return spoiledLive;
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

  public MutableLiveData<StockLocation> getStockLocationLive() {
    return stockLocationLive;
  }

  public LiveData<String> getStockLocationNameLive() {
    return stockLocationNameLive;
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
    // below
    if (!isTareWeightEnabled() && Double.parseDouble(amountLive.getValue()) <= 0) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    } else if (isTareWeightEnabled() && productDetailsLive.getValue() != null
        && Double.parseDouble(amountLive.getValue())
        < productDetailsLive.getValue().getProduct().getTareWeightDouble()
    ) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_min,
          NumUtil.trim(productDetailsLive.getValue().getProduct()
              .getTareWeightDouble())
      ));
      return false;
    }

    // over
    StockLocation currentLocation = stockLocationLive.getValue();
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
    if (isTareWeightEnabled() && productDetails != null) {
      maxAmount = stockAmount + productDetails.getProduct().getTareWeightDouble();
    } else if (currentFactor == null || currentFactor == -1) {
      maxAmount = stockAmount;
    } else if (current != null && productDetails != null
        && current.getId() == productDetails.getProduct().getQuIdPurchaseInt()) {
      maxAmount = stockAmount / currentFactor;
    } else {
      maxAmount = stockAmount * currentFactor;
    }

    if (!isTareWeightEnabled() && Double.parseDouble(amountLive.getValue()) > maxAmount) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_max, NumUtil.trim(maxAmount)
      ));
      return false;
    } else if (isTareWeightEnabled() && productDetailsLive.getValue() != null
        && Double.parseDouble(amountLive.getValue())
        > productDetailsLive.getValue().getProduct().getTareWeightDouble() + stockAmount
    ) {
      amountErrorLive.setValue(application.getString(
          R.string.error_bounds_max,
          NumUtil.trim(productDetailsLive.getValue().getProduct()
              .getTareWeightDouble() + stockAmount)
      ));
      return false;
    }
    amountErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isProductNameValid();
    valid = isQuantityUnitValid() && valid;
    valid = isAmountValid() && valid;
    return valid;
  }

  public String getConfirmationText() {
    ProductDetails productDetails = productDetailsLive.getValue();
    assert productDetails != null && amountStockLive.getValue() != null;
    double amountRemoved = Double.parseDouble(amountStockLive.getValue());
    if (isTareWeightEnabled()) {
      amountRemoved = productDetails.getStockAmount();
      amountRemoved -= Double.parseDouble(amountStockLive.getValue());
      amountRemoved += productDetails.getProduct().getTareWeightDouble();
    }
    QuantityUnit qU = quantityUnitLive.getValue();
    StockLocation stockLocation = stockLocationLive.getValue();
    assert qU != null && stockLocation != null;
    return application.getString(
        R.string.msg_quick_mode_confirm_consume,
        NumUtil.trim(amountRemoved),
        pluralUtil.getQuantityUnitPlural(qU, amountRemoved),
        productDetails.getProduct().getName(),
        stockLocation.getLocationName()
    );
  }

  public JSONObject getFilledJSONObject(boolean isActionOpen) {
    String amount = getAmountStock();
    assert amount != null && spoiledLive.getValue() != null;
    assert isTareWeightEnabledLive.getValue() != null;
    StockLocation location = stockLocationLive.getValue();
    StockEntry stockEntry = specificStockEntryLive.getValue();
    boolean spoiled = !isActionOpen && spoiledLive.getValue();
    boolean tareWeightEnabled = isTareWeightEnabledLive.getValue();

    JSONObject json = new JSONObject();
    try {
      json.put("amount", amount);
      if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING) && location != null) {
        json.put("location_id", String.valueOf(location.getLocationId()));
      }
      json.put("allow_subproduct_substitution", true);
      if (tareWeightEnabled) {
        json.put("exact_amount", consumeExactAmountLive.getValue());
      }
      if (stockEntry != null) {
        json.put("stock_entry_id", stockEntry.getStockId());
      }
      if (spoiled) {
        json.put("spoiled", true);
      }
      if (isRecipesFeatureEnabled()) {
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
    consumeExactAmountLive.setValue(false);
    stockLocationLive.setValue(null);
    spoiledLive.setValue(false);
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
        Constants.PREF.CAMERA_SCANNER_VISIBLE_CONSUME,
        false
    );
  }

  public boolean getExternalScannerEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
        Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
    );
  }

  public boolean isRecipesFeatureEnabled() {
    return isFeatureEnabled(Constants.PREF.FEATURE_RECIPES);
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
