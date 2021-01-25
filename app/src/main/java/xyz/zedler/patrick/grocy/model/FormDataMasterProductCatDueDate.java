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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;

import java.lang.ref.WeakReference;

import xyz.zedler.patrick.grocy.util.NumUtil;

public class FormDataMasterProductCatDueDate {

    public static final String DUE_DAYS_ARG = "due_days_arg";
    public static final int DUE_DAYS = 0;
    public static final int DUE_DAYS_OPENED = 2;
    public static final int DUE_DAYS_FREEZING = 4;
    public static final int DUE_DAYS_THAWING = 8;

    private final WeakReference<Context> contextWeak;
    private final MutableLiveData<Boolean> displayHelpLive;
    private final MutableLiveData<Integer> dueDateTypeLive;
    private final MutableLiveData<Boolean> dueDateTypeErrorLive;
    private final MutableLiveData<String> dueDaysLive;
    private final MutableLiveData<String> dueDaysOpenedLive;
    private final MutableLiveData<String> dueDaysFreezingLive;
    private final MutableLiveData<String> dueDaysThawingLive;

    private boolean filledWithProduct;

    public FormDataMasterProductCatDueDate(Context contextWeak, boolean beginnerMode) {
        this.contextWeak = new WeakReference<>(contextWeak);
        displayHelpLive = new MutableLiveData<>(beginnerMode);
        dueDateTypeLive = new MutableLiveData<>(0);
        dueDateTypeErrorLive = new MutableLiveData<>(false);
        dueDaysLive = new MutableLiveData<>();
        dueDaysOpenedLive = new MutableLiveData<>();
        dueDaysFreezingLive = new MutableLiveData<>();
        dueDaysThawingLive = new MutableLiveData<>();
        filledWithProduct = false;
    }

    public MutableLiveData<Boolean> getDisplayHelpLive() {
        return displayHelpLive;
    }

    public void toggleDisplayHelpLive() {
        assert displayHelpLive.getValue() != null;
        displayHelpLive.setValue(!displayHelpLive.getValue());
    }

    public MutableLiveData<Integer> getDueDateTypeLive() {
        return dueDateTypeLive;
    }

    public void setDueDateTypeLive(int type) {
        dueDateTypeLive.setValue(type);
    }

    public MutableLiveData<Boolean> getDueDateTypeErrorLive() {
        return dueDateTypeErrorLive;
    }

    public MutableLiveData<String> getDueDaysLive() {
        return dueDaysLive;
    }

    public MutableLiveData<String> getDueDaysOpenedLive() {
        return dueDaysOpenedLive;
    }

    public MutableLiveData<String> getDueDaysFreezingLive() {
        return dueDaysFreezingLive;
    }

    public MutableLiveData<String> getDueDaysThawingLive() {
        return dueDaysThawingLive;
    }

    public void setDaysNumber(String input, Bundle argsBundle) {
        String number = NumUtil.isStringInt(input) ? input : String.valueOf(0);
        int type = argsBundle.getInt(DUE_DAYS_ARG);
        if(type == DUE_DAYS) {
            if(Integer.parseInt(number) < -1) dueDaysLive.setValue(String.valueOf(-1));
            else dueDaysLive.setValue(number);
        } else if(type == DUE_DAYS_OPENED) {
            if(Integer.parseInt(number) < 0) dueDaysOpenedLive.setValue(String.valueOf(0));
            else dueDaysOpenedLive.setValue(number);
        } else if(type == DUE_DAYS_FREEZING) {
            if(Integer.parseInt(number) < -1) dueDaysFreezingLive.setValue(String.valueOf(-1));
            else dueDaysFreezingLive.setValue(number);
        } else {
            if(Integer.parseInt(number) < 0) dueDaysThawingLive.setValue(String.valueOf(0));
            else dueDaysThawingLive.setValue(number);
        }
    }

    public int getDaysNumber(int type) {
        String numberString;
        if(type == DUE_DAYS) {
            numberString = dueDaysLive.getValue();
        } else if(type == DUE_DAYS_OPENED) {
            numberString = dueDaysOpenedLive.getValue();
        } else if(type == DUE_DAYS_FREEZING) {
            numberString = dueDaysFreezingLive.getValue();
        } else {
            numberString = dueDaysThawingLive.getValue();
        }
        int number = 0;
        if(NumUtil.isStringInt(numberString)) number = Integer.parseInt(numberString);
        return number;
    }

    private boolean isDueDateTypeValid() {
        Integer type = dueDateTypeLive.getValue();
        boolean valid = type != null && (type == 1 || type == 2);
        dueDateTypeErrorLive.setValue(!valid);
        return valid;
    }

    public boolean isFormValid() {
        boolean valid = isDueDateTypeValid();
        /*boolean valid = isProductNameValid();
        valid = isAmountValid() && valid;
        valid = isQuantityUnitValid() && valid;*/
        return valid;
    }

    public static boolean isFormInvalid(@Nullable Product product) {
        if(product == null) return true;
        int dueDateType = product.getDueDateType();
        boolean valid = dueDateType == 1 || dueDateType == 2;
        return !valid;
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) return product;
        assert dueDateTypeLive.getValue() != null;
        product.setDueDateType(dueDateTypeLive.getValue());
        product.setDefaultDueDays(Integer.parseInt(dueDaysLive.getValue()));
        product.setDefaultDueDaysAfterOpen(Integer.parseInt(dueDaysOpenedLive.getValue()));
        product.setDefaultDueDaysAfterFreezing(Integer.parseInt(dueDaysFreezingLive.getValue()));
        product.setDefaultDueDaysAfterThawing(Integer.parseInt(dueDaysThawingLive.getValue()));
        return product;
    }

    public void fillWithProductIfNecessary(Product product) {
        if(filledWithProduct || product == null) return;

        dueDateTypeLive.setValue(product.getDueDateType());
        dueDaysLive.setValue(String.valueOf(product.getDefaultDueDays()));
        dueDaysOpenedLive.setValue(String.valueOf(product.getDefaultDueDaysAfterOpen()));
        dueDaysFreezingLive.setValue(String.valueOf(product.getDefaultDueDaysAfterFreezing()));
        dueDaysThawingLive.setValue(String.valueOf(product.getDefaultDueDaysAfterThawing()));
        filledWithProduct = true;
    }

    private String getString(@StringRes int res) {
        return contextWeak.get().getString(res);
    }
}
