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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.List;

public class FormDataMasterProductCatQuantityUnit {

  public final static String QUANTITY_UNIT_TYPE = "qu_type";
  public final static int STOCK = 0;
  public final static int PURCHASE = 2;

  private final Application application;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<List<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<QuantityUnit> quStockLive;
  private final LiveData<String> quStockNameLive;
  private final LiveData<Boolean> quStockErrorLive;
  private final MutableLiveData<QuantityUnit> quPurchaseLive;
  private final LiveData<String> quPurchaseNameLive;
  private final LiveData<Boolean> quPurchaseErrorLive;

  private boolean filledWithProduct;

  public FormDataMasterProductCatQuantityUnit(Application application, boolean beginnerMode) {
    this.application = application;
    displayHelpLive = new MutableLiveData<>(beginnerMode);
    quantityUnitsLive = new MutableLiveData<>();
    quStockLive = new MutableLiveData<>();
    quStockNameLive = Transformations.map(
        quStockLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quStockErrorLive = Transformations.map(quStockLive, qu -> qu == null);
    quPurchaseLive = new MutableLiveData<>();
    quPurchaseNameLive = Transformations.map(
        quPurchaseLive,
        qu -> qu != null ? qu.getName() : null
    );
    //noinspection Convert2MethodRef
    quPurchaseErrorLive = Transformations.map(quPurchaseLive, qu -> qu == null);

    filledWithProduct = false;
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
  }

  public MutableLiveData<List<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }

  public MutableLiveData<QuantityUnit> getQuStockLive() {
    return quStockLive;
  }

  public LiveData<String> getQuStockNameLive() {
    return quStockNameLive;
  }

  public LiveData<Boolean> getQuStockErrorLive() {
    return quStockErrorLive;
  }

  public MutableLiveData<QuantityUnit> getQuPurchaseLive() {
    return quPurchaseLive;
  }

  public LiveData<String> getQuPurchaseNameLive() {
    return quPurchaseNameLive;
  }

  public LiveData<Boolean> getQuPurchaseErrorLive() {
    return quPurchaseErrorLive;
  }

  public void selectQuantityUnit(QuantityUnit quantityUnit, Bundle argsBundle) {
    if (quantityUnit != null && quantityUnit.getId() == -1) {
      quantityUnit = null;
    }
    int type = argsBundle.getInt(QUANTITY_UNIT_TYPE);
    if (type == STOCK) {
      quStockLive.setValue(quantityUnit);
      if (quPurchaseLive.getValue() == null) {
        quPurchaseLive.setValue(quantityUnit);
      }
    } else {
      quPurchaseLive.setValue(quantityUnit);
    }
  }

  public static boolean isFormInvalid(@Nullable Product product) {
    if (product == null) {
      return true;
    }
    boolean valid = product.getQuIdStockInt() != -1 && product.getQuIdPurchaseInt() != -1;
    return !valid;
  }

  public Product fillProduct(@NonNull Product product) {
    QuantityUnit quStock = quStockLive.getValue();
    QuantityUnit quPurchase = quPurchaseLive.getValue();
    product.setQuIdStock(quStock != null ? quStock.getId() : -1);
    product.setQuIdPurchase(quPurchase != null ? quPurchase.getId() : -1);
    return product;
  }

  public void fillWithProductIfNecessary(Product product) {
    if (filledWithProduct || product == null) {
      return;
    }

    quStockLive.setValue(QuantityUnit.getFromId(quantityUnitsLive.getValue(), product.getQuIdStockInt()));
    quPurchaseLive.setValue(QuantityUnit.getFromId(quantityUnitsLive.getValue(), product.getQuIdPurchaseInt()));
    filledWithProduct = true;
  }
}
