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
import android.os.Handler;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class FormDataMasterProduct {
    public final static int CATEGORY_OPTIONAL = 1;

    private final WeakReference<Context> contextWeak;
    private final MutableLiveData<String> nameLive;
    private final MutableLiveData<Integer> nameErrorLive;
    private final LiveData<Boolean> catOptionalErrorLive;



    private final MutableLiveData<ShoppingList> shoppingListLive;
    private final MutableLiveData<ArrayList<Product>> productsLive;
    private final MutableLiveData<Product> productLive;
    private final MutableLiveData<String> barcodeLive;
    private final MutableLiveData<String> amountLive;
    private final MutableLiveData<String> amountErrorLive;
    private final MediatorLiveData<String> amountHelperLive;
    private final LiveData<String> amountHintLive;
    private final MutableLiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive;
    private final LiveData<ArrayList<QuantityUnit>> quantityUnitsLive;
    private final MutableLiveData<QuantityUnit> quantityUnitLive;
    private final LiveData<String> quantityUnitNameLive;
    private final MutableLiveData<String> noteLive;
    private final MutableLiveData<Integer> noteErrorLive;
    private boolean filledWithProduct;

    public FormDataMasterProduct(Context contextWeak) {
        this.contextWeak = new WeakReference<>(contextWeak);
        productLive = new MutableLiveData<>();
        nameLive = (MutableLiveData<String>) Transformations.map(
                productLive,
                product -> product != null ? product.getName() : null
        );
        nameErrorLive = new MutableLiveData<>();
        catOptionalErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatOptional::isFormInvalid
        );

        shoppingListLive = new MutableLiveData<>();
        productsLive = new MutableLiveData<>(new ArrayList<>());


        barcodeLive = new MutableLiveData<>();
        amountLive = new MutableLiveData<>();
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
        amountHintLive = Transformations.map(
                quantityUnitLive,
                quantityUnit -> quantityUnit != null ? this.contextWeak.get().getString(
                        R.string.property_amount_in,
                        quantityUnit.getNamePlural()
                ) : null
        );
        amountHelperLive = new MediatorLiveData<>();
        noteLive = new MutableLiveData<>();
        noteErrorLive = new MutableLiveData<>();
        filledWithProduct = false;
    }

    public MutableLiveData<ShoppingList> getShoppingListLive() {
        return shoppingListLive;
    }

    public MutableLiveData<Product> getProductLive() {
        return productLive;
    }

    public MutableLiveData<String> getNameLive() {
        return nameLive;
    }

    public MutableLiveData<Integer> getNameErrorLive() {
        return nameErrorLive;
    }

    public LiveData<Boolean> getCatOptionalErrorLive() {
        return catOptionalErrorLive;
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
            double amountNew = Double.parseDouble(amountLive.getValue()) - 1;
            if(amountNew >= 1) {
                amountLive.setValue(NumUtil.trim(amountNew));
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

    private QuantityUnit getStockQuantityUnit() {
        HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
        if(hashMap == null || !hashMap.containsValue((double) -1)) return null;
        for(Map.Entry<QuantityUnit, Double> entry : hashMap.entrySet()) {
            if(entry.getValue() == -1) return entry.getKey();
        } return null;
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

    public boolean isFilledWithProduct() {
         return filledWithProduct;
    }

    public void setFilledWithProduct(boolean filled) {
        this.filledWithProduct = filled;
    }

    public boolean isNameValid() {
        if(nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
            nameErrorLive.setValue(R.string.error_empty);
            return false;
        }
        nameErrorLive.setValue(null);
        return true;
    }

    public boolean isCatOptionalValid() {
        return FormDataMasterProductCatOptional.isFormInvalid(productLive.getValue());
    }

    public boolean isFormValid() {
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
        return true;
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) return product;
        product.setName(nameLive.getValue());
        return product;
    }

    public ShoppingListItem fillShoppingListItem(@Nullable ShoppingListItem item) {
        if(!isFormValid()) return null;
        ShoppingList shoppingList = shoppingListLive.getValue();
        Product product = productLive.getValue();
        String amount = amountLive.getValue();
        String note = noteLive.getValue();
        QuantityUnit unit = quantityUnitLive.getValue();
        HashMap<QuantityUnit, Double> unitFactors = quantityUnitsFactorsLive.getValue();
        Double factor = unitFactors != null ? unitFactors.get(unit) : null;
        if(factor != null && factor == -1 && product != null) {
            factor = product.getQuFactorPurchaseToStockDouble();
        }

        assert shoppingList != null && amount != null;
        if(item == null) item = new ShoppingListItem();
        double amountDouble = Double.parseDouble(amount);
        item.setShoppingListId(shoppingList.getId());
        item.setProductId(product != null ? String.valueOf(product.getId()) : null);
        item.setQuId(unit != null ? String.valueOf(unit.getId()) : null);
        item.setAmount(factor != null ? amountDouble / factor : amountDouble);
        item.setNote(note != null ? note.trim() : null);
        return item;
    }

    public ProductBarcode fillProductBarcode(@Nullable ProductBarcode productBarcode) {
        if(!isFormValid()) return null;
        String barcode = barcodeLive.getValue();
        Product product = productLive.getValue();

        if(productBarcode == null) productBarcode = new ProductBarcode();
        if(product == null) return productBarcode;
        productBarcode.setProductId(product.getId());
        productBarcode.setBarcode(barcode);
        return productBarcode;
    }

    public int[] getCategoriesInvalid() {
        return new int[]{};
    }

    public void clearForm() {
        productLive.setValue(null);
        nameLive.setValue(null);
        barcodeLive.setValue(null);
        amountLive.setValue(null);
        quantityUnitsFactorsLive.setValue(null);
        quantityUnitLive.setValue(null);
        noteLive.setValue(null);
        new Handler().postDelayed(() -> {
            nameErrorLive.setValue(null);
            amountErrorLive.setValue(null);
        }, 50);
    }

    private String getString(@StringRes int res) {
        return contextWeak.get().getString(res);
    }
}
