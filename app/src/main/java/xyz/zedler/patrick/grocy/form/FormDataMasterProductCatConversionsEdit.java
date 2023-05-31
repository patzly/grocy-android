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

package xyz.zedler.patrick.grocy.form;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class FormDataMasterProductCatConversionsEdit {

  private final Application application;
  private final Product product;
  private final MutableLiveData<String> factorLive;
  private final MutableLiveData<String> factorErrorLive;
  private final MediatorLiveData<String> factorHelperLive;
  private final MutableLiveData<List<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<QuantityUnit> quantityUnitFromLive;
  private final MutableLiveData<Boolean> quantityUnitFromErrorLive;
  private final LiveData<String> quantityUnitFromNameLive;
  private final MutableLiveData<QuantityUnit> quantityUnitToLive;
  private final MutableLiveData<Boolean> quantityUnitToErrorLive;
  private final LiveData<String> quantityUnitToNameLive;
  private final MutableLiveData<List<QuantityUnitConversion>> conversionsLive;
  private final PluralUtil pluralUtil;
  private Integer filledWithConversionId;
  private final int maxDecimalPlacesAmount;

  public FormDataMasterProductCatConversionsEdit(Application application, Product product) {
    this.application = application;
    this.product = product;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    factorLive = new MutableLiveData<>();
    factorErrorLive = new MutableLiveData<>();
    quantityUnitsLive = new MutableLiveData<>();
    quantityUnitFromLive = new MutableLiveData<>();
    quantityUnitFromErrorLive = new MutableLiveData<>(false);
    quantityUnitFromNameLive = Transformations.map(
        quantityUnitFromLive,
        quantityUnit -> quantityUnit != null ? quantityUnit.getName() : null
    );
    quantityUnitToLive = new MutableLiveData<>();
    quantityUnitToErrorLive = new MutableLiveData<>(false);
    quantityUnitToNameLive = Transformations.map(
        quantityUnitToLive,
        quantityUnit -> quantityUnit != null ? quantityUnit.getName() : null
    );
    conversionsLive = new MutableLiveData<>();
    factorHelperLive = new MediatorLiveData<>();
    factorHelperLive
        .addSource(quantityUnitFromLive, i -> factorHelperLive.setValue(getFactorHelpText()));
    factorHelperLive
        .addSource(quantityUnitToLive, i -> factorHelperLive.setValue(getFactorHelpText()));
    factorHelperLive
        .addSource(factorLive, i -> factorHelperLive.setValue(getFactorHelpText()));
    pluralUtil = new PluralUtil(application);
    filledWithConversionId = null;
  }

  public MutableLiveData<String> getFactorLive() {
    return factorLive;
  }

  public MutableLiveData<String> getFactorErrorLive() {
    return factorErrorLive;
  }

  public MutableLiveData<String> getFactorHelperLive() {
    return factorHelperLive;
  }

  public void moreFactor(ImageView view) {
    ViewUtil.startIcon(view);
    if (factorLive.getValue() == null || factorLive.getValue().isEmpty()) {
      factorLive.setValue(String.valueOf(1));
    } else {
      double factorNew = NumUtil.toDouble(factorLive.getValue()) + 1;
      factorLive.setValue(NumUtil.trimAmount(factorNew, maxDecimalPlacesAmount));
    }
  }

  public void lessFactor(ImageView view) {
    ViewUtil.startIcon(view);
    if (factorLive.getValue() != null && !factorLive.getValue().isEmpty()) {
      double factorNew = NumUtil.toDouble(factorLive.getValue()) - 1;
      if (factorNew >= 1) {
        factorLive.setValue(NumUtil.trimAmount(factorNew, maxDecimalPlacesAmount));
      }
    }
  }

  public MutableLiveData<List<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitFromLive() {
    return quantityUnitFromLive;
  }

  public MutableLiveData<Boolean> getQuantityUnitFromErrorLive() {
    return quantityUnitFromErrorLive;
  }

  public LiveData<String> getQuantityUnitFromNameLive() {
    return quantityUnitFromNameLive;
  }

  public MutableLiveData<QuantityUnit> getQuantityUnitToLive() {
    return quantityUnitToLive;
  }

  public MutableLiveData<Boolean> getQuantityUnitToErrorLive() {
    return quantityUnitToErrorLive;
  }

  public LiveData<String> getQuantityUnitToNameLive() {
    return quantityUnitToNameLive;
  }

  public MutableLiveData<List<QuantityUnitConversion>> getConversionsLive() {
    return conversionsLive;
  }

  private String getFactorHelpText() {
    if (quantityUnitFromLive.getValue() == null || quantityUnitToLive.getValue() == null
        || !NumUtil.isStringDouble(factorLive.getValue())) {
      return null;
    }
    return application.getString(
        R.string.subtitle_factor_means,
        application.getString(R.string.subtitle_amount, "1", pluralUtil.getQuantityUnitPlural(quantityUnitFromLive.getValue(), 1)),
        application.getString(R.string.subtitle_amount, NumUtil.trimAmount(NumUtil.toDouble(factorLive.getValue()), maxDecimalPlacesAmount), pluralUtil.getQuantityUnitPlural(quantityUnitToLive.getValue(), NumUtil.toDouble(factorLive.getValue())))
    );
  }

  public boolean isFilledWithConversion() {
    return filledWithConversionId != null;
  }

  public void setFilledWithConversionId(int filled) {
    this.filledWithConversionId = filled;
  }

  public boolean isFactorValid() {
    if (factorLive.getValue() == null || factorLive.getValue().isEmpty()) {
      factorErrorLive.setValue(getString(R.string.error_empty));
      return false;
    }
    if (!NumUtil.isStringDouble(factorLive.getValue())) {
      factorErrorLive.setValue(getString(R.string.error_invalid_factor));
      return false;
    }
    if (NumUtil.toDouble(factorLive.getValue()) <= 0) {
      factorErrorLive.setValue(application.getString(
          R.string.error_bounds_higher, String.valueOf(0)
      ));
      return false;
    }
    factorErrorLive.setValue(null);
    return true;
  }

  public boolean isQuanityUnitValid() {
    quantityUnitFromErrorLive.setValue(quantityUnitFromLive.getValue() == null);
    quantityUnitToErrorLive.setValue(quantityUnitToLive.getValue() == null);
    if (quantityUnitFromLive.getValue() == null || quantityUnitToLive.getValue() == null) {
      return false;
    }
    if (quantityUnitFromLive.getValue().getId() == quantityUnitToLive.getValue().getId()) {
      quantityUnitToErrorLive.setValue(true);
      return false;
    }
    List<QuantityUnitConversion> conversions = conversionsLive.getValue();
    if (conversions != null) {
      QuantityUnitConversion conversion = QuantityUnitConversion.getFromTwoUnits(
          conversions,
          quantityUnitFromLive.getValue().getId(),
          quantityUnitToLive.getValue().getId(),
          product.getId()
      );
      if (conversion == null) {
        conversion = QuantityUnitConversion.getFromTwoUnits(
            conversions,
            quantityUnitToLive.getValue().getId(),
            quantityUnitFromLive.getValue().getId(),
            product.getId()
        );
      }
      if (conversion != null && filledWithConversionId != null
          && conversion.getId() != filledWithConversionId) {
        quantityUnitToErrorLive.setValue(true);
        return false;
      }
      if (conversion != null && filledWithConversionId == null) {
        quantityUnitToErrorLive.setValue(true);
        return false;
      }
    }

    quantityUnitFromErrorLive.setValue(false);
    quantityUnitToErrorLive.setValue(false);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isFactorValid();
    valid = isQuanityUnitValid() && valid;
    return valid;
  }

  public QuantityUnitConversion fillConversion(@Nullable QuantityUnitConversion conversion) {
    if (!isFormValid()) {
      return null;
    }
    if (conversion == null) {
      conversion = new QuantityUnitConversion();
    }
    assert quantityUnitFromLive.getValue() != null;
    assert quantityUnitToLive.getValue() != null;
    assert factorLive.getValue() != null;
    conversion.setProductId(String.valueOf(product.getId()));
    conversion.setFromQuId(quantityUnitFromLive.getValue().getId());
    conversion.setToQuId(quantityUnitToLive.getValue().getId());
    conversion.setFactor(NumUtil.toDouble(factorLive.getValue()));
    return conversion;
  }

  public void clearForm() {
    quantityUnitFromLive.setValue(null);
    quantityUnitFromErrorLive.setValue(false);
    quantityUnitToLive.setValue(null);
    quantityUnitToErrorLive.setValue(false);
    factorLive.setValue(null);
    new Handler().postDelayed(() -> factorErrorLive.setValue(null), 50);
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
