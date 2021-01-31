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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.util.NumUtil;

public class FormDataMasterProductCatLocation {
    private final Application application;
    private final MutableLiveData<ArrayList<Location>> locationsLive;
    private final MutableLiveData<Location> locationLive;
    private final LiveData<String> locationNameLive;
    private final LiveData<Boolean> locationErrorLive;
    private final MutableLiveData<ArrayList<Store>> storesLive;
    private final MutableLiveData<Store> storeLive;
    private final LiveData<String> storeNameLive;

    private final MutableLiveData<Product> productLive;
    private boolean filledWithProduct;

    public FormDataMasterProductCatLocation(Application application) {
        this.application = application;
        locationsLive = new MutableLiveData<>();
        locationLive = new MutableLiveData<>();
        locationNameLive = Transformations.map(
                locationLive,
                location -> location != null ? location.getName() : null
        );
        locationErrorLive = Transformations.map(
                locationLive,
                location -> location == null
        );
        storesLive = new MutableLiveData<>();
        storeLive = new MutableLiveData<>();
        storeNameLive = Transformations.map(
                storeLive,
                store -> store != null ? store.getName() : null
        );

        productLive = new MutableLiveData<>();
        filledWithProduct = false;
    }

    public MutableLiveData<ArrayList<Location>> getLocationsLive() {
        return locationsLive;
    }

    public MutableLiveData<Location> getLocationLive() {
        return locationLive;
    }

    public LiveData<String> getLocationNameLive() {
        return locationNameLive;
    }

    public LiveData<Boolean> getLocationErrorLive() {
        return locationErrorLive;
    }

    public MutableLiveData<ArrayList<Store>> getStoresLive() {
        return storesLive;
    }

    public MutableLiveData<Store> getStoreLive() {
        return storeLive;
    }

    public LiveData<String> getStoreNameLive() {
        return storeNameLive;
    }

    private Location getLocationFromId(String id) {
        if(locationsLive.getValue() == null || !NumUtil.isStringInt(id)) return null;
        int idInt = Integer.parseInt(id);
        for(Location location : locationsLive.getValue()) {
            if(location.getId() == idInt) return location;
        } return null;
    }

    private Store getStoreFromId(String id) {
        if(storesLive.getValue() == null || !NumUtil.isStringInt(id)) return null;
        int idInt = Integer.parseInt(id);
        for(Store store : storesLive.getValue()) {
            if(store.getId() == idInt) return store;
        } return null;
    }

    public boolean isFormValid() {
        return locationLive.getValue() != null;
    }

    public static boolean isFormInvalid(@Nullable Product product) {
        if(product == null) return true;
        boolean valid = product.getLocationId() != null && !product.getLocationId().isEmpty();
        return !valid;
    }

    public Product fillProduct(@NonNull Product product) {
        if(!isFormValid()) {
            if(locationLive.getValue() == null && NumUtil.isStringInt(product.getLocationId())) {
                product.setLocationId(null);
            }
            if(storeLive.getValue() == null && NumUtil.isStringInt(product.getStoreId())) {
                product.setStoreId(null);
            }
            return product;
        }
        Location location = locationLive.getValue();
        Store store = storeLive.getValue();
        product.setLocationId(location != null ? String.valueOf(location.getId()) : null);
        product.setStoreId(store != null ? String.valueOf(store.getId()) : null);
        return product;
    }

    public void fillWithProductIfNecessary(Product product) {
        if(filledWithProduct || product == null) return;

        locationLive.setValue(getLocationFromId(product.getLocationId()));
        storeLive.setValue(getStoreFromId(product.getStoreId()));
        filledWithProduct = true;
    }
}
