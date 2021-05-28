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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.helper;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.CustomJsonArrayRequest;
import xyz.zedler.patrick.grocy.web.CustomJsonObjectRequest;
import xyz.zedler.patrick.grocy.web.CustomStringRequest;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class DownloadHelper {

  private final GrocyApi grocyApi;
  private final RequestQueue requestQueue;
  private final Gson gson;
  private final String uuidHelper;
  private final OnLoadingListener onLoadingListener;
  private final SharedPreferences sharedPrefs;
  private final DateUtil dateUtil;

  private final ArrayList<Queue> queueArrayList;
  private final String tag;
  private final String apiKey;
  private final String hassServerUrl;
  private final String hassLongLivedAccessToken;
  private final boolean debug;
  private final int timeoutSeconds;
  private int loadingRequests;

  public DownloadHelper(
      Application application,
      String tag,
      OnLoadingListener onLoadingListener
  ) {
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    dateUtil = new DateUtil(application);
    gson = new Gson();
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application);
    apiKey = sharedPrefs.getString(Constants.PREF.API_KEY, "");
    hassServerUrl = sharedPrefs.getString(
        Constants.PREF.HOME_ASSISTANT_SERVER_URL,
        null
    );
    hassLongLivedAccessToken = sharedPrefs.getString(
        Constants.PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN,
        null
    );
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(
      Application application,
      String serverUrl,
      String apiKey,
      String hassServerUrl,
      String hassLongLivedAccessToken,
      String tag,
      OnLoadingListener onLoadingListener
  ) {
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    gson = new Gson();
    dateUtil = new DateUtil(application);
    RequestQueueSingleton.getInstance(application).newRequestQueue(serverUrl);
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application, serverUrl);
    this.apiKey = apiKey;
    this.hassServerUrl = hassServerUrl;
    this.hassLongLivedAccessToken = hassLongLivedAccessToken;
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(Activity activity, String tag) {
    this(activity.getApplication(), tag, null);
  }

  // cancel all requests
  public void destroy() {
    for (Queue queue : queueArrayList) {
      queue.reset(true);
    }
    requestQueue.cancelAll(uuidHelper);
  }

  private void onRequestLoading() {
    loadingRequests += 1;
    if (onLoadingListener != null && loadingRequests == 1) {
      onLoadingListener.onLoadingChanged(true);
    }
  }

  private void onRequestFinished() {
    loadingRequests -= 1;
    if (onLoadingListener != null && loadingRequests == 0) {
      onLoadingListener.onLoadingChanged(false);
    }
  }

  public String getUuid() {
    return uuidHelper;
  }

  public void get(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  // for requests without loading progress (set noLoadingProgress=true)
  public void get(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError,
      boolean noLoadingProgress
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag,
          noLoadingProgress,
          onLoadingListener
      );
      if (!noLoadingProgress) {
        onRequestLoading();
      }
      requestQueue.add(request);
    });
  }

  // for single requests without a queue
  public void get(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    get(url, uuidHelper, onResponse, onError);
  }

  // GET requests with modified user-agent
  public void get(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError,
      String userAgent
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.GET,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper,
          userAgent
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void post(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonObjectRequest request = new CustomJsonObjectRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void postWithArray(
      String url,
      JSONObject json,
      OnJSONArrayResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonArrayRequest request = new CustomJsonArrayRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void post(String url, OnStringResponseListener onResponse, OnErrorListener onError) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.POST,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void postHassIngress(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    CustomJsonObjectRequest request = new CustomJsonObjectRequest(
        url,
        hassLongLivedAccessToken,
        json,
        onResponse::onResponse,
        onError::onError,
        this::onRequestFinished,
        timeoutSeconds,
        uuidHelper
    );
    onRequestLoading();
    requestQueue.add(request);
  }

  public void put(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomJsonObjectRequest request = new CustomJsonObjectRequest(
          Request.Method.PUT,
          url,
          apiKey,
          sessionKey,
          json,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          uuidHelper
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void delete(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    validateHassIngressSessionIfNecessary(sessionKey -> {
      CustomStringRequest request = new CustomStringRequest(
          Request.Method.DELETE,
          url,
          apiKey,
          sessionKey,
          onResponse::onResponse,
          onError::onError,
          this::onRequestFinished,
          timeoutSeconds,
          tag
      );
      onRequestLoading();
      requestQueue.add(request);
    });
  }

  public void delete(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    delete(url, uuidHelper, onResponse, onError);
  }

  public QueueItem getObjects(
      OnObjectsResponseListener onResponseListener,
      OnErrorListener onErrorListener,
      String grocyEntity
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(grocyEntity),
            uuid,
            response -> {
              Type type;
              //noinspection IfCanBeSwitch
              if (grocyEntity
                  .equals(GrocyApi.ENTITY.QUANTITY_UNITS)) { // Don't change to switch-case!
                type = new TypeToken<List<QuantityUnit>>() {
                }.getType();
              } else if (grocyEntity.equals(GrocyApi.ENTITY.LOCATIONS)) {
                type = new TypeToken<List<Location>>() {
                }.getType();
              } else if (grocyEntity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)) {
                type = new TypeToken<List<ProductGroup>>() {
                }.getType();
              } else if (grocyEntity.equals(GrocyApi.ENTITY.STORES)) {
                type = new TypeToken<List<Store>>() {
                }.getType();
              } else {
                type = new TypeToken<List<Product>>() {
                }.getType();
              }
              ArrayList<Object> objects = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Objects: " + objects);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(objects);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getObjects(OnObjectsResponseListener onResponseListener, String grocyEntity) {
    return getObjects(onResponseListener, null, grocyEntity);
  }

  public QueueItem getProductGroups(
      OnProductGroupsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductGroup>>() {
              }.getType();
              ArrayList<ProductGroup> productGroups = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ProductGroups: " + productGroups);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(productGroups);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getProductGroups(OnProductGroupsResponseListener onResponseListener) {
    return getProductGroups(onResponseListener, null);
  }

  public QueueItem updateProductGroups(
      String dbChangedTime,
      OnProductGroupsResponseListener onResponseListener
  ) {
    OnProductGroupsResponseListener newOnResponseListener = productGroups -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(productGroups);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getProductGroups(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductGroups download");
      }
      return null;
    }
  }

  public QueueItem getQuantityUnits(
      OnQuantityUnitsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
            uuid,
            response -> {
              Type type = new TypeToken<List<QuantityUnit>>() {
              }.getType();
              ArrayList<QuantityUnit> quantityUnits = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download QuantityUnits: " + quantityUnits);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(quantityUnits);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getQuantityUnits(OnQuantityUnitsResponseListener onResponseListener) {
    return getQuantityUnits(onResponseListener, null);
  }

  public QueueItem updateQuantityUnits(
      String dbChangedTime,
      OnQuantityUnitsResponseListener onResponseListener
  ) {
    OnQuantityUnitsResponseListener newOnResponseListener = conversions -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(conversions);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getQuantityUnits(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped QuantityUnits download");
      }
      return null;
    }
  }

  public QueueItem getQuantityUnitConversions(
      OnQuantityUnitConversionsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNIT_CONVERSIONS),
            uuid,
            response -> {
              Type type = new TypeToken<List<QuantityUnitConversion>>() {
              }.getType();
              ArrayList<QuantityUnitConversion> conversions
                  = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download QuantityUnitConversions: "
                    + conversions);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(conversions);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getQuantityUnitConversions(
      OnQuantityUnitConversionsResponseListener onResponseListener
  ) {
    return getQuantityUnitConversions(onResponseListener, null);
  }

  public QueueItem updateQuantityUnitConversions(
      String dbChangedTime,
      OnQuantityUnitConversionsResponseListener onResponseListener
  ) {
    OnQuantityUnitConversionsResponseListener newOnResponseListener = conversions -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(conversions);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getQuantityUnitConversions(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped QuantityUnitConversions download");
      }
      return null;
    }
  }

  public QueueItem getLocations(
      OnLocationsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
            uuid,
            response -> {
              Type type = new TypeToken<List<Location>>() {
              }.getType();
              ArrayList<Location> locations = gson.fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Locations: " + locations);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(locations);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getLocations(OnLocationsResponseListener onResponseListener) {
    return getLocations(onResponseListener, null);
  }

  public QueueItem updateLocations(
      String dbChangedTime,
      OnLocationsResponseListener onResponseListener
  ) {
    OnLocationsResponseListener newOnResponseListener = products -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(products);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getLocations(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Locations download");
      }
      return null;
    }
  }

  public QueueItem getStockCurrentLocations(
      OnStockLocationsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.STOCK_CURRENT_LOCATIONS),
            uuid,
            response -> {
              Type type = new TypeToken<List<StockLocation>>() {
              }.getType();
              ArrayList<StockLocation> locations = gson.fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockCurrentLocations: " + locations);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(locations);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockCurrentLocations(OnStockLocationsResponseListener onResponseListener) {
    return getStockCurrentLocations(onResponseListener, null);
  }

  public QueueItem updateStockCurrentLocations(
      String dbChangedTime,
      OnStockLocationsResponseListener onResponseListener
  ) {
    OnStockLocationsResponseListener newOnResponseListener = products -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(products);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getStockCurrentLocations(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockCurrentLocations download");
      }
      return null;
    }
  }

  public QueueItem getProducts(
      OnProductsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
            uuid,
            response -> {
              Type type = new TypeToken<List<Product>>() {
              }.getType();
              ArrayList<Product> products = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Products: " + products);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(products);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getProducts(OnProductsResponseListener onResponseListener) {
    return getProducts(onResponseListener, null);
  }

  public QueueItem updateProducts(
      String dbChangedTime,
      OnProductsResponseListener onResponseListener
  ) {
    OnProductsResponseListener newOnResponseListener = products -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(products);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getProducts(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Products download");
      }
      return null;
    }
  }

  public QueueItem getProductBarcodes(
      OnProductBarcodesResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductBarcode>>() {
              }.getType();
              ArrayList<ProductBarcode> barcodes
                  = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Barcodes: " + barcodes);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(barcodes);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getProductBarcodes(OnProductBarcodesResponseListener onResponseListener) {
    return getProductBarcodes(onResponseListener, null);
  }

  public QueueItem updateProductBarcodes(
      String dbChangedTime,
      OnProductBarcodesResponseListener onResponseListener
  ) {
    OnProductBarcodesResponseListener newOnResponseListener = barcodes -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(barcodes);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getProductBarcodes(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsBarcodes download");
      }
      return null;
    }
  }

  public QueueItemJson addProductBarcode(
      JSONObject jsonObject,
      OnResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItemJson() {
      @Override
      public void perform(
          @Nullable OnJSONResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        post(
            grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
            jsonObject,
            response -> {
              if (debug) {
                Log.i(tag, "added ProductBarcode");
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse();
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getSingleFilteredProductBarcode(
      String barcode,
      OnProductBarcodeResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjectsEqualValue(
                GrocyApi.ENTITY.PRODUCT_BARCODES, "barcode", barcode
            ),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductBarcode>>() {
              }.getType();
              ArrayList<ProductBarcode> barcodes
                  = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download filtered Barcodes: " + barcodes);
              }
              if (onResponseListener != null) {
                ProductBarcode barcode = !barcodes.isEmpty()
                    ? barcodes.get(0) : null; // take first object
                onResponseListener.onResponse(barcode);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockItems(
      OnStockItemsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStock(),
            uuid,
            response -> {
              Type type = new TypeToken<List<StockItem>>() {
              }.getType();
              ArrayList<StockItem> stockItems = gson.fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockItems: " + stockItems);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockItems(OnStockItemsResponseListener onResponseListener) {
    return getStockItems(onResponseListener, null);
  }

  public QueueItem updateStockItems(
      String dbChangedTime,
      OnStockItemsResponseListener onResponseListener
  ) {
    OnStockItemsResponseListener newOnResponseListener = stockItems -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(stockItems);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getStockItems(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockItems download");
      }
      return null;
    }
  }

  public QueueItem getVolatile(
      OnVolatileResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockVolatile(),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "download Volatile: success");
              }
              ArrayList<StockItem> dueItems = new ArrayList<>();
              ArrayList<StockItem> overdueItems = new ArrayList<>();
              ArrayList<StockItem> expiredItems = new ArrayList<>();
              ArrayList<MissingItem> missingItems = new ArrayList<>();
              try {
                JSONObject jsonObject = new JSONObject(response);
                // Parse first part of volatile array: expiring products
                dueItems = gson.fromJson(
                    jsonObject.getJSONArray("due_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: due = " + dueItems);
                }
                // Parse second part of volatile array: overdue products
                overdueItems = gson.fromJson(
                    jsonObject.getJSONArray("overdue_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: overdue = " + overdueItems);
                }
                // Parse third part of volatile array: expired products
                expiredItems = gson.fromJson(
                    jsonObject.getJSONArray("expired_products").toString(),
                    new TypeToken<List<StockItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: expired = " + overdueItems);
                }
                // Parse fourth part of volatile array: missing products
                missingItems = gson.fromJson(
                    jsonObject.getJSONArray("missing_products").toString(),
                    new TypeToken<List<MissingItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile: missing = " + missingItems);
                }
              } catch (JSONException e) {
                if (debug) {
                  Log.e(tag, "download Volatile: " + e);
                }
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(dueItems, overdueItems,
                    expiredItems, missingItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getVolatile(OnVolatileResponseListener onResponseListener) {
    return getVolatile(onResponseListener, null);
  }

  public QueueItem updateVolatile(
      String dbChangedTime,
      OnVolatileResponseListener onResponseListener
  ) {
    OnVolatileResponseListener newOnResponseListener = (due, overdue, expired, missing) -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_VOLATILE, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(due, overdue, expired, missing);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getVolatile(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Volatile download");
      }
      return null;
    }
  }

  public QueueItem getMissingItems(
      OnMissingItemsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockVolatile(),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "download Volatile (only missing): success");
              }
              ArrayList<MissingItem> missingItems = new ArrayList<>();
              try {
                JSONObject jsonObject = new JSONObject(response);
                // Parse fourth part of volatile array: missing products
                missingItems = gson.fromJson(
                    jsonObject.getJSONArray("missing_products").toString(),
                    new TypeToken<List<MissingItem>>() {
                    }.getType()
                );
                if (debug) {
                  Log.i(tag, "download Volatile (only missing): missing = " + missingItems);
                }
              } catch (JSONException e) {
                if (debug) {
                  Log.e(tag, "download Volatile (only missing): " + e);
                }
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(missingItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getMissingItems(OnMissingItemsResponseListener onResponseListener) {
    return getMissingItems(onResponseListener, null);
  }

  public QueueItem updateMissingItems(
      String dbChangedTime,
      OnMissingItemsResponseListener onResponseListener
  ) {
    OnMissingItemsResponseListener newOnResponseListener = shoppingListItems -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(shoppingListItems);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getMissingItems(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped MissingItems download");
      }
      return null;
    }
  }

  public QueueItem getProductDetails(
      int productId,
      OnProductDetailsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockProductDetails(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ProductDetails>() {
              }.getType();
              ProductDetails productDetails = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ProductDetails: " + productDetails);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(productDetails);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getProductDetails(
      int productId,
      OnProductDetailsResponseListener onResponseListener
  ) {
    return getProductDetails(productId, onResponseListener, null);
  }

  public QueueItem getStockLocations(
      int productId,
      OnStockLocationsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockLocationsFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockLocation>>() {
              }.getType();
              ArrayList<StockLocation> stockLocations = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockLocations: " + stockLocations);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockLocations);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockLocations(
      int productId,
      OnStockLocationsResponseListener onResponseListener
  ) {
    return getStockLocations(productId, onResponseListener, null);
  }

  public QueueItem getStockEntries(
      int productId,
      OnStockEntriesResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getStockEntriesFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockEntry>>() {
              }.getType();
              ArrayList<StockEntry> stockEntries = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download StockEntries: " + stockEntries);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stockEntries);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStockEntries(
      int productId,
      OnStockEntriesResponseListener onResponseListener
  ) {
    return getStockEntries(productId, onResponseListener, null);
  }

  public QueueItem getShoppingListItems(
      OnShoppingListItemsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
            uuid,
            response -> {
              Type type = new TypeToken<List<ShoppingListItem>>() {
              }.getType();
              ArrayList<ShoppingListItem> shoppingListItems = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ShoppingListItems: " + shoppingListItems);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(shoppingListItems);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getShoppingListItems(
      OnShoppingListItemsResponseListener onResponseListener
  ) {
    return getShoppingListItems(onResponseListener, null);
  }

  public QueueItem updateShoppingListItems(
      String dbChangedTime,
      OnShoppingListItemsResponseListener onResponseListener
  ) {
    OnShoppingListItemsResponseListener newOnResponseListener = shoppingListItems -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(
          Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime
      );
      editPrefs.apply();
      onResponseListener.onResponse(shoppingListItems);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getShoppingListItems(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingListItems download");
      }
      return null;
    }
  }

  public QueueItem getShoppingLists(
      OnShoppingListsResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
            uuid,
            response -> {
              Type type = new TypeToken<List<ShoppingList>>() {
              }.getType();
              ArrayList<ShoppingList> shoppingLists = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ShoppingLists: " + shoppingLists);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(shoppingLists);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getShoppingLists(OnShoppingListsResponseListener onResponseListener) {
    return getShoppingLists(onResponseListener, null);
  }

  public QueueItem updateShoppingLists(
      String dbChangedTime,
      OnShoppingListsResponseListener onResponseListener
  ) {
    OnShoppingListsResponseListener newOnResponseListener = shoppingListItems -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(shoppingListItems);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getShoppingLists(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingLists download");
      }
      return null;
    }
  }

  public QueueItem getStores(
      OnStoresResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getObjects(GrocyApi.ENTITY.STORES),
            uuid,
            response -> {
              Type type = new TypeToken<List<Store>>() {
              }.getType();
              ArrayList<Store> stores = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download Stores: " + stores);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(stores);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStores(OnStoresResponseListener onResponseListener) {
    return getStores(onResponseListener, null);
  }

  public QueueItem updateStores(
      String dbChangedTime,
      OnStoresResponseListener onResponseListener
  ) {
    OnStoresResponseListener newOnResponseListener = products -> {
      SharedPreferences.Editor editPrefs = sharedPrefs.edit();
      editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, dbChangedTime);
      editPrefs.apply();
      onResponseListener.onResponse(products);
    };
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STORES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return getStores(newOnResponseListener, null);
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Stores download");
      }
      return null;
    }
  }

  public void deleteProduct(
      int productId,
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    delete(
        grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, productId),
        onResponseListener,
        onErrorListener
    );
  }

  public QueueItem editShoppingListItem(
      int itemId,
      JSONObject body,
      OnJSONResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        put(
            grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId),
            body,
            response -> {
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(null);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem editShoppingListItem(int itemId, JSONObject body) {
    return editShoppingListItem(itemId, body, null, null);
  }

  public QueueItem editShoppingListItem(
      int itemId,
      JSONObject body,
      OnJSONResponseListener onResponseListener
  ) {
    return editShoppingListItem(itemId, body, onResponseListener, null);
  }

  public QueueItem deleteShoppingListItem(
      int itemId,
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        delete(
            grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, itemId),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "delete ShoppingListItem: " + itemId);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem deleteShoppingListItem(int itemId) {
    return deleteShoppingListItem(itemId, null, null);
  }

  public QueueItem getSystemInfo(
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getSystemInfo(),
            uuid,
            response -> {
              if (debug) {
                Log.i(tag, "get systemInfo: " + response);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public void getTimeDbChanged(
      OnStringResponseListener onResponseListener,
      OnSimpleErrorListener onErrorListener
  ) {
    get(
        grocyApi.getDbChangedTime(),
        uuidHelper,
        response -> {
          try {
            JSONObject body = new JSONObject(response);
            String dateStr = body.getString("changed_time");
            onResponseListener.onResponse(dateStr);
          } catch (JSONException e) {
            if (debug) {
              Log.e(tag, "getTimeDbChanged: " + e);
            }
            onErrorListener.onError();
          }
        },
        error -> onErrorListener.onError(),
        !sharedPrefs.getBoolean(
            Constants.SETTINGS.NETWORK.LOADING_CIRCLE,
            Constants.SETTINGS_DEFAULT.NETWORK.LOADING_CIRCLE
        )
    );
  }

  public void uploadSetting(
      String settingKey,
      Object settingValue,
      OnSettingUploadListener listener
  ) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("value", settingValue);
    } catch (JSONException e) {
      e.printStackTrace();
      listener.onFinished(R.string.option_synced_error);
      return;
    }
    put(
        grocyApi.getUserSetting(settingKey),
        jsonObject,
        response -> listener.onFinished(R.string.option_synced_success),
        volleyError -> listener.onFinished(R.string.option_synced_error)
    );
  }

  public QueueItem getStringData(
      String url,
      OnStringResponseListener onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            url,
            uuid,
            response -> {
              if (debug) {
                Log.i(
                    tag,
                    "download StringData from " + url + " : " + response
                );
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(response);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (debug) {
                Log.e(tag, "download StringData: " + error);
              }
              if (onErrorListener != null) {
                onErrorListener.onError(error);
              }
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }

  public QueueItem getStringData(String url, OnStringResponseListener onResponseListener) {
    return getStringData(url, onResponseListener, null);
  }

  public void validateHassIngressSessionIfNecessary(OnStringResponseListener onFinishedListener) {
    validateHassIngressSessionIfNecessary(onFinishedListener, onFinishedListener);
  }

  public void validateHassIngressSessionIfNecessary(
      OnStringResponseListener onSuccessListener,
      OnStringResponseListener onErrorListener
  ) {
    if (hassServerUrl == null || hassServerUrl.isEmpty()) {
      onSuccessListener.onResponse(null);
      return;
    }

    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    String sessionKeyTimeStr = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME, null);

    if (sessionKey != null && dateUtil.isTimeLessThanOneMinuteAway(sessionKeyTimeStr)) {
      onSuccessListener.onResponse(sessionKey);
      return;
    }

    if (sessionKey == null) {
      postHassIngress(
          hassServerUrl + "/api/hassio/ingress/session",
          null,
          response -> {
            try {
              boolean isOk = response.get("result").equals("ok");
              if (isOk && response.has("data")) {
                JSONObject data = response.getJSONObject("data");
                if (!data.has("session")) {
                  Log.e(tag,
                      "validateHassIngressSession (/api/hassio/ingress/session): data doesn't contain session");
                  onErrorListener.onResponse(null);
                  return;
                }
                sharedPrefs.edit().putString(
                    Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                    data.getString("session")
                ).putString(
                    Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                    dateUtil.getCurrentDateWithTimeStr()
                ).apply();
                onSuccessListener.onResponse(data.getString("session"));
              } else if (!isOk) {
                Log.e(tag,
                    "validateHassIngressSession (/api/hassio/ingress/session): isOk is false");
                onErrorListener.onResponse(null);
              } else if (!response.has("data")) {
                Log.e(tag,
                    "validateHassIngressSession (/api/hassio/ingress/session): response doesn't contain data");
                onErrorListener.onResponse(null);
              }
            } catch (JSONException e) {
              Log.e(tag,
                  "validateHassIngressSession (/api/hassio/ingress/session): JSONException:");
              e.printStackTrace();
              onErrorListener.onResponse(null);
            }
          },
          error -> {
            Log.e(tag, "validateHassIngressSession (/api/hassio/ingress/session): error: " + error);
            onErrorListener.onResponse(null);
          }
      );
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("session", sessionKey);
    } catch (JSONException e) {
      Log.e(tag,
          "validateHassIngressSession (/api/hassio/ingress/validate_session): JSONException1:");
      e.printStackTrace();
    }
    postHassIngress(
        hassServerUrl + "/api/hassio/ingress/validate_session",
        jsonObject,
        response -> {
          try {
            boolean isOk = response.get("result").equals("ok");
            if (!isOk && response.has("data")) {
              JSONObject data = response.getJSONObject("data");
              if (!data.has("session")) {
                Log.e(tag,
                    "validateHassIngressSession (/api/hassio/ingress/validate_session): data doesn't contain session");
                onErrorListener.onResponse(null);
                return;
              }
              sharedPrefs.edit().putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                  data.getString("session")
              ).putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                  dateUtil.getCurrentDateWithTimeStr()
              ).apply();
            }
            onSuccessListener.onResponse(sessionKey);
          } catch (JSONException e) {
            Log.e(tag,
                "validateHassIngressSession (/api/hassio/ingress/validate_session): JSONException2:");
            e.printStackTrace();
            onErrorListener.onResponse(null);
          }
        },
        error -> {
          Log.e(tag, "validateHassIngressSession: error: " + error);
          if (error instanceof AuthFailureError) {
            postHassIngress(
                hassServerUrl + "/api/hassio/ingress/session",
                null,
                response2 -> {
                  try {
                    boolean isOk = response2.get("result").equals("ok");
                    if (isOk && response2.has("data")) {
                      JSONObject data = response2.getJSONObject("data");
                      if (!data.has("session")) {
                        onErrorListener.onResponse(null);
                        return;
                      }
                      sharedPrefs.edit().putString(
                          Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                          data.getString("session")
                      ).putString(
                          Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                          dateUtil.getCurrentDateWithTimeStr()
                      ).apply();
                      onSuccessListener.onResponse(data.getString("session"));
                    } else if (!isOk) {
                      Log.e(tag, "validateHassIngressSession: isOk is false");
                      onErrorListener.onResponse(null);
                    }
                  } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(tag, "validateHassIngressSession: JSONException");
                    onErrorListener.onResponse(null);
                  }
                },
                error2 -> {
                  Log.e(tag, "validateHassIngressSession: error2: " + error2);
                  onErrorListener.onResponse(null);
                }
            );
          } else {
            onErrorListener.onResponse(null);
          }
        }
    );
  }

  public class Queue {

    private final ArrayList<QueueItem> queueItems;
    private final OnQueueEmptyListener onQueueEmptyListener;
    private final OnErrorListener onErrorListener;
    private final String uuidQueue;
    private int queueSize;
    private boolean isRunning;

    public Queue(OnQueueEmptyListener onQueueEmptyListener, OnErrorListener onErrorListener) {
      this.onQueueEmptyListener = onQueueEmptyListener;
      this.onErrorListener = onErrorListener;
      queueItems = new ArrayList<>();
      uuidQueue = UUID.randomUUID().toString();
      queueSize = 0;
      isRunning = false;
    }

    public Queue append(QueueItem... queueItems) {
      for (QueueItem queueItem : queueItems) {
        if (queueItem == null) {
          continue;
        }
        this.queueItems.add(queueItem);
        queueSize++;
      }
      return this;
    }

    public void start() {
      if (isRunning) {
        return;
      } else {
        isRunning = true;
      }
      if (queueItems.isEmpty()) {
        if (onQueueEmptyListener != null) {
          onQueueEmptyListener.execute();
        }
        return;
      }
      while (!queueItems.isEmpty()) {
        QueueItem queueItem = queueItems.remove(0);
        queueItem.perform(response -> {
          queueSize--;
          if (queueSize > 0) {
            return;
          }
          isRunning = false;
          if (onQueueEmptyListener != null) {
            onQueueEmptyListener.execute();
          }
          reset(false);
        }, error -> {
          isRunning = false;
          if (onErrorListener != null) {
            onErrorListener.onError(error);
          }
          reset(true);
        }, uuidQueue);
      }
    }

    public int getSize() {
      return queueSize;
    }

    public boolean isEmpty() {
      return queueSize == 0;
    }

    public void reset(boolean cancelAll) {
      if (cancelAll) {
        requestQueue.cancelAll(uuidQueue);
      }
      queueItems.clear();
      queueSize = 0;
    }
  }

  public Queue newQueue(
      OnQueueEmptyListener onQueueEmptyListener,
      OnErrorListener onErrorListener
  ) {
    Queue queue = new Queue(onQueueEmptyListener, onErrorListener);
    queueArrayList.add(queue);
    return queue;
  }

  public abstract static class BaseQueueItem {

  }

  public abstract static class QueueItem extends BaseQueueItem {

    public abstract void perform(
        OnStringResponseListener responseListener,
        OnErrorListener errorListener,
        String uuid
    );

    public void perform(String uuid) {
      // UUID is for cancelling the requests; should be uuidHelper from above
      perform(null, null, uuid);
    }
  }

  public abstract static class QueueItemJson extends BaseQueueItem {

    public abstract void perform(
        OnJSONResponseListener responseListener,
        OnErrorListener errorListener,
        String uuid
    );

    public void perform(String uuid) {
      // UUID is for cancelling the requests; should be uuidHelper from above
      perform(null, null, uuid);
    }
  }

  public interface OnObjectsResponseListener {

    void onResponse(ArrayList<Object> objects);
  }

  public interface OnProductGroupsResponseListener {

    void onResponse(ArrayList<ProductGroup> productGroups);
  }

  public interface OnQuantityUnitsResponseListener {

    void onResponse(ArrayList<QuantityUnit> quantityUnits);
  }

  public interface OnQuantityUnitConversionsResponseListener {

    void onResponse(ArrayList<QuantityUnitConversion> quantityUnitConversions);
  }

  public interface OnLocationsResponseListener {

    void onResponse(ArrayList<Location> locations);
  }

  public interface OnProductsResponseListener {

    void onResponse(ArrayList<Product> products);
  }

  public interface OnProductBarcodesResponseListener {

    void onResponse(ArrayList<ProductBarcode> productBarcodes);
  }

  public interface OnProductBarcodeResponseListener {

    void onResponse(ProductBarcode productBarcode);
  }

  public interface OnStockItemsResponseListener {

    void onResponse(ArrayList<StockItem> stockItems);
  }

  public interface OnVolatileResponseListener {

    void onResponse(
        ArrayList<StockItem> due,
        ArrayList<StockItem> overdue,
        ArrayList<StockItem> expired,
        ArrayList<MissingItem> missing
    );
  }

  public interface OnMissingItemsResponseListener {

    void onResponse(ArrayList<MissingItem> missingItems);
  }

  public interface OnProductDetailsResponseListener {

    void onResponse(ProductDetails productDetails);
  }

  public interface OnStockLocationsResponseListener {

    void onResponse(ArrayList<StockLocation> stockLocations);
  }

  public interface OnStockEntriesResponseListener {

    void onResponse(ArrayList<StockEntry> stockEntries);
  }

  public interface OnShoppingListItemsResponseListener {

    void onResponse(ArrayList<ShoppingListItem> shoppingListItems);
  }

  public interface OnShoppingListsResponseListener {

    void onResponse(ArrayList<ShoppingList> shoppingLists);
  }

  public interface OnStoresResponseListener {

    void onResponse(ArrayList<Store> stores);
  }

  public interface OnStringResponseListener {

    void onResponse(String response);
  }

  public interface OnJSONResponseListener {

    void onResponse(JSONObject response);
  }

  public interface OnJSONArrayResponseListener {

    void onResponse(JSONArray response);
  }

  public interface OnResponseListener {

    void onResponse();
  }

  public interface OnErrorListener {

    void onError(VolleyError volleyError);
  }

  public interface OnSimpleErrorListener {

    void onError();
  }

  public interface OnQueueEmptyListener {

    void execute();
  }

  public interface OnLoadingListener {

    void onLoadingChanged(boolean isLoading);
  }

  public interface OnSettingUploadListener {

    void onFinished(@StringRes int msg);
  }
}
