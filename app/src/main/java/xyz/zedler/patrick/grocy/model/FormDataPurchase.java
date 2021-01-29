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

import android.content.Context;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class FormDataPurchase {
    private final static String TAG = FormDataPurchase.class.getSimpleName();

    private final WeakReference<Context> contextWeak;
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
    private final MutableLiveData<QuantityUnit> quantityUnitLive;
    private final LiveData<String> quantityUnitNameLive;
    private final MutableLiveData<Boolean> quantityUnitErrorLive;
    private final MutableLiveData<String> amountLive;
    private final MutableLiveData<String> amountErrorLive;
    private final MediatorLiveData<String> amountHelperLive;
    private final LiveData<String> amountHintLive;
    private final MediatorLiveData<String> amountStockLive;
    private final MutableLiveData<String> dueDateLive;
    private final LiveData<String> dueDateTextLive;
    private final MutableLiveData<Boolean> dueDateErrorLive;
    private final MutableLiveData<String> priceLive;
    private final MutableLiveData<String> priceErrorLive;
    private final MediatorLiveData<String> priceHelperLive;
    private final String priceHint;
    private final LiveData<String> unitPriceTextLive;
    private final MutableLiveData<Boolean> isTotalPriceLive;
    private final MutableLiveData<Store> storeLive;
    private final LiveData<String> storeNameLive;
    private final MutableLiveData<Location> locationLive;
    private final LiveData<String> locationNameLive;
    private boolean filledWithProduct;

    public FormDataPurchase(Context contextWeak, SharedPreferences sharedPrefs) {
        DateUtil dateUtil = new DateUtil(contextWeak);
        this.contextWeak = new WeakReference<>(contextWeak);
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
                quantityUnit -> quantityUnit != null ? this.contextWeak.get().getString(
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
        dueDateLive = new MutableLiveData<>();
        dueDateTextLive = Transformations.map(
                dueDateLive,
                date -> {
                    if(date == null) {
                        return getString(R.string.subtitle_none_selected);
                    } else if(date.equals(Constants.DATE.NEVER_OVERDUE)) {
                        return getString(R.string.subtitle_never_overdue);
                    } else {
                        return dateUtil.getLocalizedDate(date, DateUtil.FORMAT_MEDIUM);
                    }
                }
        );
        dueDateLive.setValue(null);
        dueDateErrorLive = new MutableLiveData<>();
        priceLive = new MutableLiveData<>();
        priceErrorLive = new MutableLiveData<>();
        isTotalPriceLive = new MutableLiveData<>(false);
        priceHelperLive = new MediatorLiveData<>();
        priceHelperLive.addSource(isTotalPriceLive, i -> priceHelperLive.setValue(getPriceHelpText()));
        priceHelperLive.addSource(quantityUnitLive, i -> priceHelperLive.setValue(getPriceHelpText()));
        String currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        if(currency != null && !currency.isEmpty()) {
            priceHint = contextWeak.getString(R.string.property_price_in, currency);
        } else {
            priceHint = getString(R.string.property_price);
        }
        unitPriceTextLive = Transformations.map(
                quantityUnitLive,
                quantityUnit -> quantityUnit != null
                        ? this.contextWeak.get()
                        .getString(R.string.title_unit_price_specific, quantityUnit.getName())
                        : getString(R.string.title_unit_price)
        );
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

        filledWithProduct = false;
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

    public MutableLiveData<String> getProductNameLive() {
        return productNameLive;
    }

    public MutableLiveData<Integer> getProductNameErrorLive() {
        return productNameErrorLive;
    }

    public MutableLiveData<HashMap<QuantityUnit, Double>> getQuantityUnitsFactorsLive() {
        return quantityUnitsFactorsLive;
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
        if(hashMap == null || !hashMap.containsValue((double) -1)) return null;
        for(Map.Entry<QuantityUnit, Double> entry : hashMap.entrySet()) {
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
        QuantityUnit stock = getStockQuantityUnit();
        QuantityUnit current = quantityUnitLive.getValue();
        if(!isAmountValid() || quantityUnitsFactorsLive.getValue() == null) return null;
        assert amountLive.getValue() != null;

        if(stock != null && current != null) {
            HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
            double amount = Double.parseDouble(amountLive.getValue());
            Object currentFactor = hashMap.get(current);
            if(currentFactor == null) {
                amountHelperLive.setValue(null);
                return null;
            }
            double amountMultiplied;
            if((double) currentFactor == -1) {
                amountMultiplied = amount;
            } else if(productDetailsLive.getValue() != null
                    && current.getId() == productDetailsLive.getValue().getProduct()
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
        QuantityUnit stock = getStockQuantityUnit();
        QuantityUnit current = quantityUnitLive.getValue();
        if(stock == null || current == null || stock.getId() == current.getId()
                || !NumUtil.isStringDouble(amountStockLive.getValue())) return null;
        return contextWeak.get().getString(
                R.string.subtitle_amount_compare,
                amountStockLive.getValue(),
                Double.parseDouble(amountStockLive.getValue()) == 1
                        ? stock.getName() : stock.getNamePlural()
        );
    }

    public void moreAmount(ImageView view) {
        IconUtil.start(view);
        if(amountLive.getValue() == null || amountLive.getValue().isEmpty()) {
            amountLive.setValue(String.valueOf(1));
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
        QuantityUnit stock = getStockQuantityUnit();
        return contextWeak.get().getString(
                R.string.msg_purchased,
                NumUtil.trim(Double.parseDouble(amountStockLive.getValue())),
                Double.parseDouble(amountStockLive.getValue()) == 1
                        ? stock.getName() : stock.getNamePlural(),
                productDetailsLive.getValue().getProduct().getName()
        );
    }

    public MutableLiveData<String> getDueDateLive() {
        return dueDateLive;
    }

    public LiveData<String> getDueDateTextLive() {
        return dueDateTextLive;
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

    public String getPriceHint() {
        return priceHint;
    }

    public LiveData<String> getPriceHelperLive() {
        return priceHelperLive;
    }

    private String getPriceHelpText() {
        if(true) return " "; // TODO
        boolean isTotalPrice = isTotalPriceLive.getValue();
        QuantityUnit unit = quantityUnitLive.getValue();
        if(isTotalPrice) {
            return getString(R.string.subtitle_price_help_total);
        } else if(unit == null) {
            return getString(R.string.subtitle_price_help_unit);
        } else {
            String name = unit.getName();
            return contextWeak.get().getString(R.string.subtitle_price_help_unit_in, name);
        }
    }

    public void morePrice() {
        if(priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
            priceLive.setValue(NumUtil.trimPrice(1));
        } else {
            double priceNew = NumUtil.toDouble(priceLive.getValue()) + 1;
            priceLive.setValue(NumUtil.trimPrice(priceNew));
        }
    }

    public void lessPrice() {
        if(priceLive.getValue() == null || priceLive.getValue().isEmpty()) return;
        double priceNew = NumUtil.toDouble(priceLive.getValue()) - 1;
        if(priceNew >= 0) {
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

    private Product getProductFromName(String name) {
        if(productsLive.getValue() == null) return null;
        for(Product product : productsLive.getValue()) {
            if(product.getName().equals(name)) return product;
        } return null;
    }

    private Product getProductFromId(String id) {
        if(productsLive.getValue() == null || id == null) return null;
        int idInt = Integer.parseInt(id);
        for(Product product : productsLive.getValue()) {
            if(product.getId() == idInt) return product;
        } return null;
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
        if(Double.parseDouble(amountLive.getValue()) <= 0) {
            amountErrorLive.setValue(contextWeak.get().getString(
                    R.string.error_bounds_higher, String.valueOf(0)
            )); // TODO
            return false;
        }
        amountErrorLive.setValue(null);
        return true;
    }

    public boolean isDueDateValid() {
        if(dueDateLive.getValue() == null || dueDateLive.getValue().isEmpty()) {
            dueDateErrorLive.setValue(true);
            return false;
        } else {
            dueDateErrorLive.setValue(false);
            return true;
        }
    }

    public boolean isPriceValid() {
        if(priceLive.getValue() == null || priceLive.getValue().isEmpty()) {
            priceErrorLive.setValue(null);
            return true;
        }
        if(!NumUtil.isStringNum(priceLive.getValue())) {
            priceErrorLive.setValue(getString(R.string.error_invalid_price));
            return false;
        }
        amountErrorLive.setValue(null);
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
        String amount = amountLive.getValue();
        QuantityUnit qU = quantityUnitLive.getValue();
        ProductDetails details = productDetailsLive.getValue();
        String price = priceLive.getValue();
        assert amount != null && qU != null && details != null && price != null;
        price = NumUtil.trimPrice(Double.parseDouble(price));
        price += " " + sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        return contextWeak.get().getString(
                R.string.msg_scan_mode_confirm,
                NumUtil.trim(Double.parseDouble(amount)),
                Double.parseDouble(amount) == 1 ? qU.getName() : qU.getNamePlural(),
                details.getProduct().getName(),
                dueDateTextLive.getValue(),
                price,
                storeNameLive.getValue(),
                locationNameLive.getValue()
        );
    }

    public JSONObject getFilledJSONObject() {
        String amount = getAmountStock();
        String price = priceLive.getValue();
        Store store = storeLive.getValue();
        String storeId = store != null ? String.valueOf(store.getId()) : null;
        Location location = locationLive.getValue();
        String dueDate = dueDateLive.getValue();
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            dueDate = Constants.DATE.NEVER_OVERDUE;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("amount", amount);
            if(NumUtil.isStringDouble(price)) {
                double priceDouble = NumUtil.toDouble(price);
                if(isTotalPriceLive.getValue()) {
                    priceDouble = priceDouble / Double.parseDouble(amount);
                }
                json.put("price", String.valueOf(priceDouble));
            }
            json.put("best_before_date", dueDate);
            if(storeId != null) json.put("shopping_location_id", storeId);
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING) && location != null) {
                json.put("location_id", String.valueOf(location.getId()));
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
        Store store = storeLive.getValue();

        ProductBarcode productBarcode = new ProductBarcode();
        productBarcode.setProductId(product.getId());
        productBarcode.setBarcode(barcode);
        if(store != null) productBarcode.setStoreId(String.valueOf(store.getId()));
        return productBarcode;
    }

    public void clearForm() {
        amountLive.setValue(null);
        quantityUnitLive.setValue(null);
        quantityUnitsFactorsLive.setValue(null);
        productDetailsLive.setValue(null);
        productNameLive.setValue(null);
        dueDateLive.setValue(null);
        priceLive.setValue(null);
        storeLive.setValue(null);
        locationLive.setValue(null);
        new Handler().postDelayed(() -> {
            productNameErrorLive.setValue(null);
            quantityUnitErrorLive.setValue(false);
            amountErrorLive.setValue(null);
            dueDateErrorLive.setValue(false);
        }, 50);
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    private boolean isDebuggingEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
                Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
        );
    }

    private String getString(@StringRes int res) {
        return contextWeak.get().getString(res);
    }
}
