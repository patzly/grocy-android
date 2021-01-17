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

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;

public class FormDataMasterProduct {

    private final WeakReference<Context> contextWeak;
    private final MutableLiveData<Boolean> displayHelpLive;
    private final MutableLiveData<String> nameLive;
    private final MutableLiveData<Integer> nameErrorLive;
    private final LiveData<Boolean> catOptionalErrorLive;
    private final LiveData<Boolean> catLocationErrorLive;
    private final LiveData<Boolean> catDueDateErrorLive;
    private final LiveData<Boolean> catQuErrorLive;
    private final LiveData<Boolean> catAmountErrorLive;


    private final MutableLiveData<ArrayList<Product>> productsLive;
    private final MutableLiveData<Product> productLive;

    public FormDataMasterProduct(Context contextWeak) {
        this.contextWeak = new WeakReference<>(contextWeak);
        displayHelpLive = new MutableLiveData<>(true);
        productLive = new MutableLiveData<>();
        productsLive = new MutableLiveData<>(new ArrayList<>());
        nameLive = (MutableLiveData<String>) Transformations.map(
                productLive,
                product -> product != null ? product.getName() : null
        );
        nameErrorLive = new MutableLiveData<>();
        catOptionalErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatOptional::isFormInvalid
        );
        catLocationErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatLocation::isFormInvalid
        );
        catDueDateErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatDueDate::isFormInvalid
        );
        catQuErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatAmount::isFormInvalid
        );
        catAmountErrorLive = Transformations.map(
                productLive,
                FormDataMasterProductCatAmount::isFormInvalid
        );
    }

    public MutableLiveData<Boolean> getDisplayHelpLive() {
        return displayHelpLive;
    }

    public void toggleDisplayHelpLive() {
        assert displayHelpLive.getValue() != null;
        displayHelpLive.setValue(!displayHelpLive.getValue());
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

    public LiveData<Boolean> getCatLocationErrorLive() {
        return catLocationErrorLive;
    }

    public LiveData<Boolean> getCatDueDateErrorLive() {
        return catDueDateErrorLive;
    }

    public LiveData<Boolean> getCatQuErrorLive() {
        return catQuErrorLive;
    }

    public LiveData<Boolean> getCatAmountErrorLive() {
        return catAmountErrorLive;
    }

    public MutableLiveData<ArrayList<Product>> getProductsLive() {
        return productsLive;
    }

    public boolean isNameValid() {
        if(nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
            nameErrorLive.setValue(R.string.error_empty);
            return false;
        }
        nameErrorLive.setValue(null);
        return true;
    }

    public boolean isFormValid() {
        boolean valid = isNameValid();
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
        return valid;
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) return product;
        product.setName(nameLive.getValue());
        return product;
    }
}
