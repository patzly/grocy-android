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
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;

public class FormDataMasterProduct {
    private final MutableLiveData<Boolean> displayHelpLive;
    private final MutableLiveData<String> nameLive;
    private final MediatorLiveData<Integer> nameErrorLive;
    private final LiveData<Boolean> catOptionalErrorLive;
    private final LiveData<Boolean> catLocationErrorLive;
    private final LiveData<Boolean> catDueDateErrorLive;
    private final LiveData<Boolean> catQuErrorLive;
    private final LiveData<Boolean> catAmountErrorLive;
    private final MutableLiveData<ArrayList<String>> productNamesLive;
    private final MutableLiveData<Product> productLive;

    public FormDataMasterProduct(Context contextWeak) {
        displayHelpLive = new MutableLiveData<>(true);
        productLive = new MutableLiveData<>();
        productNamesLive = new MutableLiveData<>();
        nameLive = (MutableLiveData<String>) Transformations.map(
                productLive,
                product -> product != null ? product.getName() : null
        );
        nameErrorLive = new MediatorLiveData<>();
        nameErrorLive.addSource(nameLive, i -> isNameValid());
        nameErrorLive.addSource(productNamesLive, i -> isNameValid());
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
                FormDataMasterProductCatQuantityUnit::isFormInvalid
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

    public MutableLiveData<ArrayList<String>> getProductNamesLive() {
        return productNamesLive;
    }

    public boolean isNameValid() {
        if(nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
            nameErrorLive.setValue(R.string.error_empty);
            return false;
        }
        if(nameLive.getValue() != null && !nameLive.getValue().isEmpty()
                && productNamesLive.getValue() != null
                && productNamesLive.getValue().contains(nameLive.getValue())
        ) {
            nameErrorLive.setValue(R.string.error_name_exists);
            return false;
        }
        nameErrorLive.setValue(null);
        return true;
    }

    public boolean isFormValid() {
        return isNameValid();
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) return product;
        product.setName(nameLive.getValue());
        return product;
    }

    public boolean isWholeFormValid() {
        boolean valid = isFormValid();
        valid = !catOptionalErrorLive.getValue() && valid;
        valid = !catLocationErrorLive.getValue() && valid;
        valid = !catDueDateErrorLive.getValue() && valid;
        valid = !catQuErrorLive.getValue() && valid;
        valid = !catAmountErrorLive.getValue() && valid;
        return valid;
    }
}
