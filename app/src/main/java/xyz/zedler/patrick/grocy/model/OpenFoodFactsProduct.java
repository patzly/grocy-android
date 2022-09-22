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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

public class OpenFoodFactsProduct {

  private JSONObject productJson;

  @SerializedName("_id")
  private String id;

  @SerializedName("product_name")
  private String productName;

  @Nullable
  @SerializedName("nutriments")
  private OpenFoodFactsNutriments nutriments;

  public void setProductJson(JSONObject productJson) {
    this.productJson = productJson;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getLocalizedProductName(Application application) {
    String language = application.getResources().getConfiguration().locale.getLanguage();
    String country = application.getResources().getConfiguration().locale.getCountry();
    String both = language + "_" + country;
    if (productJson == null) return this.productName;
    String name = productJson.optString("product_name_" + both);
    if(name.isEmpty()) {
      name = productJson.optString("product_name_" + language);
    }
    return name.isEmpty() ? this.productName : name;
  }

  public double getEnergy100g() {
    if (nutriments == null) {
      return 0;
    }
    if (nutriments.energyKcal100g != 0) {
      return nutriments.energyKcal100g;
    } else if (nutriments.energyKcalValue != 0) {
      return nutriments.energyKcalValue;
    } else {
      return nutriments.energyKcal;
    }
  }

  @Nullable
  public String getHumanReadableEnergy100g() {
    double energy100g = getEnergy100g();
    if (energy100g == 0 || nutriments == null) {
      return null;
    }
    return energy100g + " " + nutriments.energyKcalUnit;
  }

  @Nullable
  public String getHumanReadableEnergyServing() {
    if (nutriments == null || nutriments.energyKcalServing == 0) {
      return null;
    }
    return nutriments.energyKcalServing + " " + nutriments.energyKcalUnit;
  }

  public static class OpenFoodFactsNutriments {
    @SerializedName("energy-kcal")
    private double energyKcal;

    @SerializedName("energy-kcal_100g")
    private double energyKcal100g;

    @SerializedName("energy-kcal_serving")
    private double energyKcalServing;

    @SerializedName("energy-kcal_unit")
    private String energyKcalUnit;

    @SerializedName("energy-kcal_value")
    private double energyKcalValue;
  }

  @NonNull
  @Override
  public String toString() {
    return "OpenFoodFactsProduct(" + productName + ")";
  }
}
