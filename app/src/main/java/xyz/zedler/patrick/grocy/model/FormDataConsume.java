package xyz.zedler.patrick.grocy.model;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

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
    private final MutableLiveData<Integer> productNameErrorLive;
    private final MutableLiveData<String> barcodeLive;
    private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
    private final LiveData<QuantityUnit> quantityUnitStock;
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
    /*private final MutableLiveData<Recipe> recipeLive;
    private final LiveData<String> recipeNameLive;*/
    private boolean torchOn = false;

    public FormDataConsume(Application application, SharedPreferences sharedPrefs) {
        this.application = application;
        this.sharedPrefs = sharedPrefs;
        displayHelpLive = new MutableLiveData<>(sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
        ));
        scannerVisibilityLive = new MutableLiveData<>(false); // TODO: What on start?
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
        quantityUnitStock = Transformations.map(
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
        amountHelperLive.addSource(amountStockLive, i -> amountHelperLive.setValue(getAmountHelpText()));
        amountHelperLive.addSource(quantityUnitsFactorsLive, i -> amountHelperLive.setValue(getAmountHelpText()));
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

    public LiveData<QuantityUnit> getQuantityUnitStock() {
        return quantityUnitStock;
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
        if(unitsFactors == null || !unitsFactors.containsValue((double) -1)) return null;
        for(Map.Entry<QuantityUnit, Double> entry : unitsFactors.entrySet()) {
            if(entry.getValue() == -1) return entry.getKey();
        } return null;
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
        QuantityUnit stock = quantityUnitStock.getValue();
        QuantityUnit current = quantityUnitLive.getValue();
        if(!isAmountValid() || quantityUnitsFactorsLive.getValue() == null) return null;
        assert amountLive.getValue() != null;

        if(stock != null && current != null && productDetails != null) {
            HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
            double amount = Double.parseDouble(amountLive.getValue());
            Object currentFactor = hashMap.get(current);
            if(currentFactor == null) return null;
            double amountMultiplied;
            if(isTareWeightEnabled() || (double) currentFactor == -1) {
                amountMultiplied = amount;
            } else if(current.getId() == productDetails.getProduct()
                    .getQuIdPurchase()) {
                amountMultiplied = amount * (double) currentFactor;
            } else {
                amountMultiplied = amount / (double) currentFactor;
            }
            return NumUtil.trim(amountMultiplied);
        } else {
            return null;
        }
    }

    private String getAmountHelpText() {
        QuantityUnit stock = quantityUnitStock.getValue();
        QuantityUnit current = quantityUnitLive.getValue();
        if(stock == null || current == null || stock.getId() == current.getId()
                || !NumUtil.isStringDouble(amountStockLive.getValue())) return null;
        return application.getString(
                R.string.subtitle_amount_compare,
                amountStockLive.getValue(),
                Double.parseDouble(amountStockLive.getValue()) == 1
                        ? stock.getName() : stock.getNamePlural()
        );
    }

    public void moreAmount(ImageView view) {
        IconUtil.start(view);
        if(amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
            if(!isTareWeightEnabled() || productDetailsLive.getValue() == null) {
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
        IconUtil.start(view);
        if(amountLive.getValue() != null && !amountLive.getValue().isEmpty()) {
            double amountCurrent = Double.parseDouble(amountLive.getValue());
            Double amountNew = null;
            if(amountCurrent > 1) {
                amountNew = amountCurrent - 1;
            } else if(amountCurrent > 0.1) {
                amountNew = amountCurrent - 0.1;
            } else if(amountCurrent > 0.01) {
                amountNew = amountCurrent - 0.01;
            }
            if(amountNew != null) amountLive.setValue(NumUtil.trim(amountNew));
        }
    }

    public String getTransactionSuccessMsg() {
        double amountAdded = Double.parseDouble(amountStockLive.getValue());
        if(isTareWeightEnabled()) {
            amountAdded -= productDetailsLive.getValue().getStockAmount();
            amountAdded -= productDetailsLive.getValue().getProduct().getTareWeightDouble();
        }
        QuantityUnit stock = quantityUnitStock.getValue();
        return application.getString(
                R.string.msg_purchased,
                NumUtil.trim(amountAdded),
                amountAdded == 1 ? stock.getName() : stock.getNamePlural(),
                productDetailsLive.getValue().getProduct().getName()
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
        if(productNameLive.getValue() != null && productNameLive.getValue().isEmpty()) {
            if(productDetailsLive.getValue() != null) {
                clearForm();
                return false;
            }
        }
        if(productDetailsLive.getValue() == null && productNameLive.getValue() == null
                || productDetailsLive.getValue() == null && productNameLive.getValue().isEmpty()) {
            productNameErrorLive.setValue(R.string.error_empty);
            return false;
        }
        if(productDetailsLive.getValue() == null && !productNameLive.getValue().isEmpty()) {
            productNameErrorLive.setValue(R.string.error_invalid_product);
            return false;
        }
        if(productDetailsLive.getValue() != null && !productNameLive.getValue().isEmpty()
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
        if(productDetailsLive.getValue() != null && quantityUnitLive.getValue() == null) {
            quantityUnitErrorLive.setValue(true);
            return false;
        }
        quantityUnitErrorLive.setValue(false);
        return true;
    }

    public boolean isAmountValid() {
        if(productDetailsLive.getValue() == null) {
            amountErrorLive.setValue(null);
            return true;
        }
        if(amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
            amountErrorLive.setValue(getString(R.string.error_empty));
            return false;
        }
        if(!NumUtil.isStringNum(amountLive.getValue())) {
            amountErrorLive.setValue(getString(R.string.error_invalid_amount));
            return false;
        }
        if(!isTareWeightEnabled() && Double.parseDouble(amountLive.getValue()) <= 0) {
            amountErrorLive.setValue(application.getString(
                    R.string.error_bounds_higher, String.valueOf(0)
            ));
            return false;
        } else if(isTareWeightEnabled() && productDetailsLive.getValue() != null
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

    public boolean isFormValid() {
        boolean valid = isProductNameValid();
        valid = isQuantityUnitValid() && valid;
        valid = isAmountValid() && valid;
        return valid;
    }

    public String getConfirmationText() {
        /*double amountAdded = Double.parseDouble(amountStockLive.getValue());
        if(isTareWeightEnabled()) {
            amountAdded -= productDetailsLive.getValue().getStockAmount();
            amountAdded -= productDetailsLive.getValue().getProduct().getTareWeightDouble();
        }
        QuantityUnit qU = quantityUnitLive.getValue();
        ProductDetails details = productDetailsLive.getValue();
        String price = priceLive.getValue();
        assert qU != null && details != null && price != null;
        price = NumUtil.trimPrice(Double.parseDouble(price));
        if(currency != null && !currency.isEmpty()) {
            price += " " + currency;
        }
        return application.getString(
                R.string.msg_scan_mode_confirm,
                NumUtil.trim(amountAdded),
                amountAdded == 1 ? qU.getName() : qU.getNamePlural(),
                details.getProduct().getName(),
                dueDateTextLive.getValue(),
                price,
                storeNameLive.getValue(),
                locationNameLive.getValue()
        );*/
        return "";
    }

    public JSONObject getFilledJSONObject() {
        String amount = getAmountStock();
        assert amount != null;
        StockLocation location = stockLocationLive.getValue();

        JSONObject json = new JSONObject();
        try {
            json.put("amount", amount);
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING) && location != null) {
                json.put("location_id", String.valueOf(location.getId())); // TODO
            }
        } catch (JSONException e) {
            if(isDebuggingEnabled()) Log.e(TAG, "purchaseProduct: " + e);
        }
        return json;
    }

    public ProductBarcode fillProductBarcode() {
        if(!isFormValid()) return null;
        String barcode = barcodeLive.getValue();
        Product product = productDetailsLive.getValue().getProduct();

        ProductBarcode productBarcode = new ProductBarcode();
        productBarcode.setProductId(product.getId());
        productBarcode.setBarcode(barcode);
        return productBarcode;
    }

    public void clearForm() {
        amountLive.setValue(null);
        quantityUnitLive.setValue(null);
        quantityUnitsFactorsLive.setValue(null);
        productDetailsLive.setValue(null);
        productNameLive.setValue(null);
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
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
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
