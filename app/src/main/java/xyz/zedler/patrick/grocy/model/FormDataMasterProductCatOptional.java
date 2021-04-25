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
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;

public class FormDataMasterProductCatOptional {
    private final Application application;
    private final MutableLiveData<Boolean> displayHelpLive;
    private final MutableLiveData<Boolean> isActiveLive;
    private final MutableLiveData<ArrayList<Product>> productsLive;
    private final MutableLiveData<Product> parentProductLive;
    private final MutableLiveData<String> parentProductNameLive;
    private final MutableLiveData<Integer> parentProductNameErrorLive;
    private final MutableLiveData<Spanned> descriptionLive;
    private final MutableLiveData<ArrayList<ProductGroup>> productGroupsLive;
    private final MutableLiveData<ProductGroup> productGroupLive;
    private final LiveData<String> productGroupNameLive;
    private final MutableLiveData<String> energyLive;
    private final MutableLiveData<Boolean> neverShowOnStockLive;


    private final MutableLiveData<Product> productLive;
    private boolean filledWithProduct;

    public FormDataMasterProductCatOptional(Application application, boolean beginnerMode) {
        this.application = application;
        displayHelpLive = new MutableLiveData<>(beginnerMode);
        isActiveLive = new MutableLiveData<>();
        productsLive = new MutableLiveData<>(new ArrayList<>());
        parentProductLive = new MutableLiveData<>();
        parentProductNameLive = (MutableLiveData<String>) Transformations.map(
                parentProductLive,
                product -> product != null ? product.getName() : null
        );
        parentProductNameErrorLive = new MutableLiveData<>();
        descriptionLive = new MutableLiveData<>();
        productGroupsLive = new MutableLiveData<>();
        productGroupLive = new MutableLiveData<>();
        productGroupNameLive = Transformations.map(
                productGroupLive,
                productGroup -> productGroup != null ? productGroup.getName() : null
        );
        energyLive = new MutableLiveData<>();
        neverShowOnStockLive = new MutableLiveData<>();

        productLive = new MutableLiveData<>();
        filledWithProduct = false;
    }

    public MutableLiveData<Boolean> getDisplayHelpLive() {
        return displayHelpLive;
    }

    public void toggleDisplayHelpLive() {
        assert displayHelpLive.getValue() != null;
        displayHelpLive.setValue(!displayHelpLive.getValue());
    }

    public MutableLiveData<Boolean> getIsActiveLive() {
        return isActiveLive;
    }

    public void toggleActiveLive() {
        isActiveLive.setValue(isActiveLive.getValue() == null || !isActiveLive.getValue());
    }

    public MutableLiveData<ArrayList<Product>> getProductsLive() {
        return productsLive;
    }

    public MutableLiveData<Product> getParentProductLive() {
        return parentProductLive;
    }

    public MutableLiveData<String> getParentProductNameLive() {
        return parentProductNameLive;
    }

    public MutableLiveData<Integer> getParentProductNameErrorLive() {
        return parentProductNameErrorLive;
    }

    public MutableLiveData<Spanned> getDescriptionLive() {
        return descriptionLive;
    }

    public MutableLiveData<ArrayList<ProductGroup>> getProductGroupsLive() {
        return productGroupsLive;
    }

    public MutableLiveData<ProductGroup> getProductGroupLive() {
        return productGroupLive;
    }

    public LiveData<String> getProductGroupNameLive() {
        return productGroupNameLive;
    }

    public MutableLiveData<String> getEnergyLive() {
        return energyLive;
    }

    public MutableLiveData<Boolean> getNeverShowOnStockLive() {
        return neverShowOnStockLive;
    }

    public void toggleNeverShowOnStockLive() {
        neverShowOnStockLive.setValue(
                neverShowOnStockLive.getValue() == null || !neverShowOnStockLive.getValue()
        );
    }

    public MutableLiveData<Product> getProductLive() {
        return productLive;
    }

    public boolean isParentProductValid() {
        if(parentProductNameLive.getValue() == null || parentProductNameLive.getValue().isEmpty()) {
            if(parentProductLive.getValue() != null) parentProductLive.setValue(null);
            parentProductNameErrorLive.setValue(null);
            return true;
        }
        Product product = getProductFromName(parentProductNameLive.getValue());
        if(product != null) {
            parentProductLive.setValue(product);
            parentProductNameErrorLive.setValue(null);
            return true;
        } else {
            parentProductNameErrorLive.setValue(R.string.error_invalid_product);
            return false;
        }
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

    private ProductGroup getProductGroupFromId(String id) {
        if(productGroupsLive.getValue() == null || id == null || id.isEmpty()) return null;
        int idInt = Integer.parseInt(id);
        for(ProductGroup productGroup : productGroupsLive.getValue()) {
            if(productGroup.getId() == idInt) return productGroup;
        } return null;
    }

    public boolean isFormValid() {
        boolean valid = isParentProductValid();
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
        return valid;
    }

    public static boolean isFormInvalid(@Nullable Product product) {
        if(product == null) return true;
        boolean valid = true;
        return !valid;
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) return product;
        assert isActiveLive.getValue() != null;
        assert neverShowOnStockLive.getValue() != null;
        ProductGroup pGroup = productGroupLive.getValue();
        product.setActive(isActiveLive.getValue() ? 1 : 0);
        product.setParentProductId(parentProductLive.getValue() != null
                ? String.valueOf(parentProductLive.getValue().getId()) : null);
        product.setDescription(descriptionLive.getValue() != null
                ? Html.toHtml(descriptionLive.getValue()) : null);
        product.setProductGroupId(pGroup != null ? String.valueOf(pGroup.getId()) : null);
        product.setCalories(energyLive.getValue());
        product.setHideOnStockOverview(neverShowOnStockLive.getValue() ? 1 : 0);
        return product;
    }

    public void fillWithProductIfNecessary(Product product) {
        if(filledWithProduct || product == null) return;

        isActiveLive.setValue(product.isActive());
        parentProductLive.setValue(getProductFromId(product.getParentProductId()));
        descriptionLive.setValue(product.getDescription() != null
                ? Html.fromHtml(product.getDescription()) : null);
        productGroupLive.setValue(getProductGroupFromId(product.getProductGroupId()));
        energyLive.setValue(product.getCalories());
        neverShowOnStockLive.setValue(product.getHideOnStockOverview() == 1);
        filledWithProduct = true;
    }

    private String getString(@StringRes int res) {
        return application.getString(res);
    }
}
