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
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.api.OpenBeautyFactsApi;
import xyz.zedler.patrick.grocy.api.OpenFoodFactsApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectResponseListener;

public class OpenBeautyFactsProduct {

  private JSONObject productJson;

  @SerializedName("_id")
  private String id;

  @SerializedName("product_name")
  private String productName;

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

  @NonNull
  @Override
  public String toString() {
    return "OpenBeautyFactsProduct(" + productName + ")";
  }

  public static void getOpenBeautyFactsProduct(
      DownloadHelper dlHelper,
      String barcode,
      OnObjectResponseListener<OpenBeautyFactsProduct> successListener,
      OnErrorListener errorListener
  ) {
    dlHelper.get(
        OpenBeautyFactsApi.getProduct(barcode),
        response -> {
          try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject jsonProduct = jsonObject.getJSONObject("product");
            Type type = new TypeToken<OpenBeautyFactsProduct>(){}.getType();
            OpenBeautyFactsProduct product = dlHelper.gson.fromJson(jsonProduct.toString(), type);
            product.setProductJson(jsonProduct);
            successListener.onResponse(product);
            if(dlHelper.debug) Log.i(dlHelper.tag, "getOpenBeautyFactsProduct: " + product);
          } catch (JSONException e) {
            if(dlHelper.debug) Log.e(dlHelper.tag, "getOpenBeautyFactsProduct: " + e);
            errorListener.onError(null);
          }
        },
        error -> {
          if(dlHelper.debug) Log.e(dlHelper.tag, "getOpenBeautyFactsProduct: "
              + "can't get OpenBeautyFacts product");
          errorListener.onError(error);
        },
        OpenFoodFactsApi.getUserAgent(dlHelper.application)
    );
  }
}
