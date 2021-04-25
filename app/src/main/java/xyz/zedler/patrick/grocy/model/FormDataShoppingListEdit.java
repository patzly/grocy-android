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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;

public class FormDataShoppingListEdit {
    private final MutableLiveData<String> nameLive;
    private ArrayList<String> shoppingListNames;
    private final ShoppingList startupShoppingList;
    private final MutableLiveData<Integer> nameErrorLive;

    public FormDataShoppingListEdit(ShoppingList shoppingList) {
        nameLive = new MutableLiveData<>();
        nameErrorLive = new MutableLiveData<>();
        startupShoppingList = shoppingList;

        if(startupShoppingList != null) {
            nameLive.setValue(startupShoppingList.getName());
        }
    }

    public MutableLiveData<String> getNameLive() {
        return nameLive;
    }

    public MutableLiveData<Integer> getNameErrorLive() {
        return nameErrorLive;
    }

    public void setShoppingListNames(ArrayList<String> shoppingListNames) {
        this.shoppingListNames = new ArrayList<>(shoppingListNames);
        if(startupShoppingList != null) {
            this.shoppingListNames.remove(startupShoppingList.getName());
        }
    }

    public boolean isActionEdit() {
        return startupShoppingList != null;
    }

    public boolean isNameValid() {
        if(shoppingListNames == null) return false;
        if(nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
            nameErrorLive.setValue(R.string.error_empty);
            return false;
        }
        if(shoppingListNames.contains(nameLive.getValue())) {
            nameErrorLive.setValue(R.string.error_name_exists);
            return false;
        }
        nameErrorLive.setValue(null);
        return true;
    }

    public boolean isFormValid() {
        return isNameValid();
    }
}
