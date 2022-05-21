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

package xyz.zedler.patrick.grocy.helper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.api.OpenBeautyFactsApi;
import xyz.zedler.patrick.grocy.api.OpenFoodFactsApi;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreDetails;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.CustomJsonArrayRequest;
import xyz.zedler.patrick.grocy.web.CustomJsonObjectRequest;
import xyz.zedler.patrick.grocy.web.CustomStringRequest;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class DownloadHelper {

  private final Application application;
  private final GrocyApi grocyApi;
  private final RequestQueue requestQueue;
  private final Gson gson;
  private final String uuidHelper;
  private final OnLoadingListener onLoadingListener;
  private final SharedPreferences sharedPrefs;
  private final DateUtil dateUtil;
  private final AppDatabase appDatabase;

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
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    dateUtil = new DateUtil(application);
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
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
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    gson = new Gson();
    dateUtil = new DateUtil(application);
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
    RequestQueueSingleton.getInstance(application).newRequestQueue();
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

  public DownloadHelper(Context context, String tag) {
    this((Application) context.getApplicationContext(), tag, null);
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

  public QueueItem updateProductGroups(
      String dbChangedTime,
      OnProductGroupsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.productGroupDao().deleteProductGroups(),
                    appDatabase.productGroupDao().insertProductGroups(productGroups)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(productGroups);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
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

  public QueueItem updateQuantityUnits(
      String dbChangedTime,
      OnQuantityUnitsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.quantityUnitDao().deleteQuantityUnits(),
                    appDatabase.quantityUnitDao().insertQuantityUnits(quantityUnits)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(quantityUnits);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped QuantityUnits download");
      }
      return null;
    }
  }

  public QueueItem updateQuantityUnitConversions(
      String dbChangedTime,
      OnQuantityUnitConversionsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.quantityUnitConversionDao().deleteConversions(),
                    appDatabase.quantityUnitConversionDao().insertConversions(conversions)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(conversions);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
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

  public QueueItem updateLocations(
      String dbChangedTime,
      OnLocationsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.locationDao().deleteLocations(),
                    appDatabase.locationDao().insertLocations(locations)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(locations);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Locations download");
      }
      return null;
    }
  }

  public QueueItem updateStockCurrentLocations(
      String dbChangedTime,
      OnStockLocationsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.stockLocationDao().deleteStockLocations(),
                    appDatabase.stockLocationDao().insertStockLocations(locations)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(locations);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockCurrentLocations download");
      }
      return null;
    }
  }

  public QueueItem updateProducts(
      String dbChangedTime,
      OnProductsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.productDao().deleteProducts(),
                    appDatabase.productDao().insertProducts(products)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(products);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Products download");
      }
      return null;
    }
  }

  public QueueItem updateProductsLastPurchased(
      String dbChangedTime,
      OnProductsLastPurchasedResponseListener onResponseListener,
      boolean isOptional
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
                @Nullable OnStringResponseListener responseListener,
                @Nullable OnErrorListener errorListener,
                @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.PRODUCTS_LAST_PURCHASED),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductLastPurchased>>() {
                }.getType();
                ArrayList<ProductLastPurchased> productsLastPurchased = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ProductsLastPurchased: " + productsLastPurchased);
                }
                Single.concat(
                    appDatabase.productLastPurchasedDao().deleteProductsLastPurchased(),
                    appDatabase.productLastPurchasedDao()
                        .insertProductsLastPurchased(productsLastPurchased)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(productsLastPurchased);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (isOptional) {
                  if (responseListener != null) {
                    responseListener.onResponse(null);
                  }
                  return;
                }
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsLastPurchased download");
      }
      return null;
    }
  }

  public QueueItem updateProductsAveragePrice(
      String dbChangedTime,
      OnProductsAveragePriceResponseListener onResponseListener,
      boolean isOptional
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.PRODUCTS_AVERAGE_PRICE),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductAveragePrice>>() {
                }.getType();
                ArrayList<ProductAveragePrice> productsAveragePrice = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ProductsAveragePrice: " + productsAveragePrice);
                }
                Single.concat(
                    appDatabase.productAveragePriceDao().deleteProductsAveragePrice(),
                    appDatabase.productAveragePriceDao()
                        .insertProductsAveragePrice(productsAveragePrice)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(productsAveragePrice);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (isOptional) {
                  if (responseListener != null) {
                    responseListener.onResponse(null);
                  }
                  return;
                }
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ProductsAveragePrice download");
      }
      return null;
    }
  }

  public QueueItem updateProductBarcodes(
      String dbChangedTime,
      OnProductBarcodesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.productBarcodeDao().deleteProductBarcodes(),
                    appDatabase.productBarcodeDao().insertProductBarcodes(barcodes)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(barcodes);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
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

  public QueueItem updateUsers(
      String dbChangedTime,
      OnUsersResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_USERS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getUsers(),
              uuid,
              response -> {
                Type type = new TypeToken<List<User>>() {
                }.getType();
                ArrayList<User> users = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Users: " + users);
                }
                Single.concat(
                    appDatabase.userDao().deleteUsers(),
                    appDatabase.userDao().insertUsers(users)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_USERS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(users);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Users download");
      }
      return null;
    }
  }

  public QueueItem updateStockItems(
      String dbChangedTime,
      OnStockItemsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.stockItemDao().deleteStockItems(),
                    appDatabase.stockItemDao().insertStockItems(stockItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_STOCK_ITEMS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(stockItems);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockItems download");
      }
      return null;
    }
  }

  public QueueItem updateVolatile(
      String dbChangedTime,
      OnVolatileResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                  Log.i(tag, "updateVolatile: success");
                }
                ArrayList<StockItem> dueItems = new ArrayList<>();
                ArrayList<StockItem> overdueItems = new ArrayList<>();
                ArrayList<StockItem> expiredItems = new ArrayList<>();
                ArrayList<MissingItem> missingItems = new ArrayList<>();
                try {
                  Type typeStockItem = new TypeToken<List<StockItem>>() {
                  }.getType();
                  JSONObject jsonObject = new JSONObject(response);
                  // Parse first part of volatile array: expiring products
                  dueItems = gson.fromJson(
                      jsonObject.getJSONArray("due_products").toString(), typeStockItem
                  );
                  // Parse second part of volatile array: overdue products
                  overdueItems = gson.fromJson(
                      jsonObject.getJSONArray("overdue_products").toString(), typeStockItem
                  );
                  // Parse third part of volatile array: expired products
                  expiredItems = gson.fromJson(
                      jsonObject.getJSONArray("expired_products").toString(), typeStockItem
                  );
                  // Parse fourth part of volatile array: missing products
                  missingItems = gson.fromJson(
                      jsonObject.getJSONArray("missing_products").toString(),
                      new TypeToken<List<MissingItem>>() {
                      }.getType()
                  );
                  if (debug) {
                    Log.i(tag, "updateVolatile:\ndue = " + dueItems + "\noverdue: "
                        + overdueItems + "\nexpired: " + expiredItems + "\nmissing: "
                        + missingItems);
                  }
                } catch (JSONException e) {
                  if (debug) {
                    Log.e(tag, "updateVolatile: " + e);
                  }
                }
                ArrayList<VolatileItem> volatileItemsTogether = new ArrayList<>();
                for (StockItem stockItem : dueItems) {
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_DUE)
                  );
                }
                for (StockItem stockItem : overdueItems) {
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_OVERDUE)
                  );
                }
                for (StockItem stockItem : expiredItems) {
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_EXPIRED)
                  );
                }
                ArrayList<StockItem> finalDueItems = dueItems;
                ArrayList<StockItem> finalOverdueItems = overdueItems;
                ArrayList<StockItem> finalExpiredItems = expiredItems;
                ArrayList<MissingItem> finalMissingItems = missingItems;
                Single.concat(
                    appDatabase.volatileItemDao().deleteVolatileItems(),
                    appDatabase.volatileItemDao().insertVolatileItems(volatileItemsTogether),
                    appDatabase.missingItemDao().deleteMissingItems(),
                    appDatabase.missingItemDao().insertMissingItems(missingItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_VOLATILE, dbChangedTime)
                          .putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(finalDueItems, finalOverdueItems,
                            finalExpiredItems, finalMissingItems);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Volatile download");
      }
      return null;
    }
  }

  public void getVolatile(
      Response.Listener<String> responseListener,
      Response.ErrorListener errorListener
  ) {
    get(
        grocyApi.getStockVolatile(),
        responseListener::onResponse,
        errorListener::onErrorResponse
    );
  }

  public QueueItem updateMissingItems(
      String dbChangedTime,
      OnMissingItemsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                ArrayList<MissingItem> finalMissingItems = missingItems;
                Single.concat(
                    appDatabase.missingItemDao().deleteMissingItems(),
                    appDatabase.missingItemDao().insertMissingItems(missingItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(finalMissingItems);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped MissingItems download");
      }
      return null;
    }
  }

  public QueueItem updateStockEntries(
      String dbChangedTime,
      OnStockEntriesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_STOCK_ENTRIES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.STOCK_ENTRIES),
              uuid,
              response -> {
                Type type = new TypeToken<List<StockEntry>>() {
                }.getType();
                ArrayList<StockEntry> stockEntries = gson.fromJson(response, type);
                if (debug) {
                  Log.i(tag, "dowload StockEntries: " + stockEntries);
                }
                Single.concat(
                    appDatabase.stockEntryDao().deleteStockEntries(),
                    appDatabase.stockEntryDao().insertStockEntries(stockEntries)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_STOCK_ENTRIES, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(stockEntries);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped StockEntries download");
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

  public QueueItem updateShoppingListItems(
      String dbChangedTime,
      OnShoppingListItemsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.shoppingListItemDao().deleteShoppingListItems(),
                    appDatabase.shoppingListItemDao().insertShoppingListItems(shoppingListItems)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(shoppingListItems);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingListItems download");
      }
      return null;
    }
  }

  public QueueItem updateShoppingListItems(
      String dbChangedTime,
      OnShoppingListItemsWithSyncResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                ArrayList<ShoppingListItem> itemsToSync = new ArrayList<>();
                HashMap<Integer, ShoppingListItem> serverItemsHashMap = new HashMap<>();
                for (ShoppingListItem s : shoppingListItems) {
                  serverItemsHashMap.put(s.getId(), s);
                }

                appDatabase.shoppingListItemDao().getShoppingListItems()
                    .doOnSuccess(offlineItems -> {
                      // compare server items with offline items and add modified to separate list
                      for (ShoppingListItem offlineItem : offlineItems) {
                        ShoppingListItem serverItem = serverItemsHashMap.get(offlineItem.getId());
                        if (serverItem != null  // sync only items which are still on server
                            && offlineItem.getDoneSynced() != -1
                            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
                            && offlineItem.getDoneInt() != serverItem.getDoneInt()
                            || serverItem != null
                            && serverItem.getDoneSynced() != -1  // server database hasn't changed
                            && offlineItem.getDoneSynced() != -1
                            && offlineItem.getDoneInt() != offlineItem.getDoneSynced()
                        ) {
                          itemsToSync.add(offlineItem);
                        }
                      }
                    })
                    .doFinally(() -> {
                      appDatabase.shoppingListItemDao().deleteAll();
                      appDatabase.shoppingListItemDao().insertAll(shoppingListItems);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(shoppingListItems, itemsToSync,
                            serverItemsHashMap);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
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
              onResponseListener.onResponse(shoppingLists);
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

  public QueueItem updateShoppingLists(
      String dbChangedTime,
      OnShoppingListsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.shoppingListDao().deleteShoppingLists(),
                    appDatabase.shoppingListDao().insertShoppingLists(shoppingLists)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(shoppingLists);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped ShoppingLists download");
      }
      return null;
    }
  }

  public QueueItem updateStores(
      String dbChangedTime,
      OnStoresResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STORES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
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
                Single.concat(
                    appDatabase.storeDao().deleteStores(),
                    appDatabase.storeDao().insertStores(stores)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_STORES, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(stores);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Stores download");
      }
      return null;
    }
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

  public QueueItem updateTasks(
      String dbChangedTime,
      OnTasksResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_TASKS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.TASKS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Task>>() {
                }.getType();
                ArrayList<Task> tasks = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Tasks: " + tasks);
                }
                Single.concat(
                    appDatabase.taskDao().deleteTasks(),
                    appDatabase.taskDao().insertTasks(tasks)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_TASKS, dbChangedTime).apply();
                      if (onResponseListener != null) {{
                        onResponseListener.onResponse(tasks);
                      }}
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Tasks download");
      }
      return null;
    }
  }

  public QueueItem updateTaskCategories(
      String dbChangedTime,
      OnTaskCategoriesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.TASK_CATEGORIES),
              uuid,
              response -> {
                Type type = new TypeToken<List<TaskCategory>>() {
                }.getType();
                ArrayList<TaskCategory> taskCategories = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Task categories: " + taskCategories);
                }
                Single.concat(
                    appDatabase.taskCategoryDao().deleteCategories(),
                    appDatabase.taskCategoryDao().insertCategories(taskCategories)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, dbChangedTime)
                          .apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(taskCategories);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped TaskCategories download");
      }
      return null;
    }
  }

  public QueueItem updateChores(
      String dbChangedTime,
      OnChoresResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_CHORES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getObjects(ENTITY.CHORES),
              uuid,
              response -> {
                Type type = new TypeToken<List<Chore>>() {
                }.getType();
                ArrayList<Chore> chores = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download Chores: " + chores);
                }
                Single.concat(
                    appDatabase.choreDao().deleteChores(),
                    appDatabase.choreDao().insertChores(chores)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_CHORES, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(chores);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Chores download");
      }
      return null;
    }
  }

  public QueueItem updateChoreEntries(
      String dbChangedTime,
      OnChoreEntriesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_CHORE_ENTRIES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnErrorListener errorListener,
            @Nullable String uuid
        ) {
          get(
              grocyApi.getChores(),
              uuid,
              response -> {
                Type type = new TypeToken<List<ChoreEntry>>() {
                }.getType();
                ArrayList<ChoreEntry> choreEntries = new Gson().fromJson(response, type);
                if (debug) {
                  Log.i(tag, "download ChoreEntries: " + choreEntries);
                }
                Single.concat(
                    appDatabase.choreEntryDao().deleteChoreEntries(),
                    appDatabase.choreEntryDao().insertChoreEntries(choreEntries)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_CHORE_ENTRIES, dbChangedTime).apply();
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(choreEntries);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe();
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Chores download");
      }
      return null;
    }
  }

  public QueueItem getChoreDetails(
      int choreId,
      OnChoreDetailsResponseListener onResponseListener,
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
            grocyApi.getChores(choreId),
            uuid,
            response -> {
              Type type = new TypeToken<ChoreDetails>() {
              }.getType();
              ChoreDetails choreDetails = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "download ChoreDetails: " + choreDetails);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(choreDetails);
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

  public QueueItem getChoreDetails(int choreId, OnChoreDetailsResponseListener onResponseListener) {
    return getChoreDetails(choreId, onResponseListener, null);
  }

  public QueueItem updateRecipes(
          String dbChangedTime,
          OnRecipesResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
            PREF.DB_LAST_TIME_RECIPES, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
                @Nullable OnStringResponseListener responseListener,
                @Nullable OnErrorListener errorListener,
                @Nullable String uuid
        ) {
          get(
                  grocyApi.getRecipes(),
                  uuid,
                  response -> {
                    Type type = new TypeToken<List<Recipe>>() {
                    }.getType();
                    ArrayList<Recipe> recipes = new Gson().fromJson(response, type);
                    if (debug) {
                      Log.i(tag, "download Recipes: " + recipes);
                    }
                    Single.concat(
                            appDatabase.recipeDao().deleteRecipes(),
                            appDatabase.recipeDao().insertRecipes(recipes)
                    )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally(() -> {
                              sharedPrefs.edit()
                                      .putString(PREF.DB_LAST_TIME_RECIPES, dbChangedTime).apply();

                              if (onResponseListener != null) {
                                onResponseListener.onResponse(recipes);
                              }
                              if (responseListener != null) {
                                responseListener.onResponse(response);
                              }
                            })
                            .subscribe();
                  },
                  error -> {
                    if (errorListener != null) {
                      errorListener.onError(error);
                    }
                  }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Recipes download");
      }
      return null;
    }
  }

  public QueueItem updateRecipeFulfillments(
          String dbChangedTime,
          OnRecipeFulfillmentsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
            PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
                @Nullable OnStringResponseListener responseListener,
                @Nullable OnErrorListener errorListener,
                @Nullable String uuid
        ) {
          get(
                  grocyApi.getRecipeFulfillments(),
                  uuid,
                  response -> {
                    Type type = new TypeToken<List<RecipeFulfillment>>() {
                    }.getType();
                    ArrayList<RecipeFulfillment> recipeFulfillments = new Gson().fromJson(response, type);
                    if (debug) {
                      Log.i(tag, "download RecipeFulfillments: " + recipeFulfillments);
                    }
                    Single.concat(
                            appDatabase.recipeFulfillmentDao().deleteRecipeFulfillments(),
                            appDatabase.recipeFulfillmentDao().insertRecipeFulfillments(recipeFulfillments)
                    )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally(() -> {
                              sharedPrefs.edit()
                                      .putString(PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, dbChangedTime).apply();

                              if (onResponseListener != null) {
                                onResponseListener.onResponse(recipeFulfillments);
                              }
                              if (responseListener != null) {
                                responseListener.onResponse(response);
                              }
                            })
                            .subscribe();
                  },
                  error -> {
                    if (errorListener != null) {
                      errorListener.onError(error);
                    }
                  }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Recipe fulfillments download");
      }
      return null;
    }
  }

  public QueueItem updateRecipePositions(
          String dbChangedTime,
          OnRecipePositionsResponseListener onResponseListener
  ) {
    String lastTime = sharedPrefs.getString(  // get last offline db-changed-time value
            PREF.DB_LAST_TIME_RECIPE_POSITIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
                @Nullable OnStringResponseListener responseListener,
                @Nullable OnErrorListener errorListener,
                @Nullable String uuid
        ) {
          get(
                  grocyApi.getRecipePositions(),
                  uuid,
                  response -> {
                    Type type = new TypeToken<List<RecipePosition>>() {
                    }.getType();
                    ArrayList<RecipePosition> recipePositions = new Gson().fromJson(response, type);
                    if (debug) {
                      Log.i(tag, "download RecipePositions: " + recipePositions);
                    }
                    Single.concat(
                            appDatabase.recipePositionDao().deleteRecipePositions(),
                            appDatabase.recipePositionDao().insertRecipePositions(recipePositions)
                    )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally(() -> {
                              sharedPrefs.edit()
                                      .putString(PREF.DB_LAST_TIME_RECIPE_POSITIONS, dbChangedTime).apply();

                              if (onResponseListener != null) {
                                onResponseListener.onResponse(recipePositions);
                              }
                              if (responseListener != null) {
                                responseListener.onResponse(response);
                              }
                            })
                            .subscribe();
                  },
                  error -> {
                    if (errorListener != null) {
                      errorListener.onError(error);
                    }
                  }
          );
        }
      };
    } else {
      if (debug) {
        Log.i(tag, "downloadData: skipped Recipe positions download");
      }
      return null;
    }
  }

  public QueueItem getCurrentUserId(OnIntegerResponseListener onResponseListener) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnErrorListener errorListener,
          @Nullable String uuid
      ) {
        get(
            grocyApi.getUser(),
            uuid,
            response -> {
              Type type = new TypeToken<List<User>>() {
              }.getType();
              ArrayList<User> users = new Gson().fromJson(response, type);
              if (debug) {
                Log.i(tag, "get currentUserId: " + response);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(users.size() == 1 ? users.get(0).getId() : -1);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
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

  public void getOpenFoodFactsProductName(
      String barcode,
      OnStringResponseListener successListener,
      OnErrorListener errorListener
  ) {
    get(
        OpenFoodFactsApi.getProduct(barcode),
        response -> {
          String language = application.getResources().getConfiguration().locale.getLanguage();
          String country = application.getResources().getConfiguration().locale.getCountry();
          String both = language + "_" + country;
          if(debug) Log.i(tag, "getOpenFoodFactsProductName: locale = " + both);
          try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject product = jsonObject.getJSONObject("product");
            String name = product.optString("product_name_" + both);
            if(name.isEmpty()) {
              name = product.optString("product_name_" + language);
            }
            if(name.isEmpty()) {
              name = product.optString("product_name");
            }
            successListener.onResponse(name);
            if(debug) Log.i(tag, "getOpenFoodFactsProductName: OpenFoodFacts = " + name);
          } catch (JSONException e) {
            if(debug) Log.e(tag, "getOpenFoodFactsProductName: " + e);
            successListener.onResponse(null);
          }
        },
        error -> {
          if(debug) Log.e(tag, "getOpenFoodFactsProductName: can't get OpenFoodFacts product");
          errorListener.onError(error);
        },
        OpenFoodFactsApi.getUserAgent(application)
    );
  }

  public void getOpenBeautyFactsProductName(
      String barcode,
      OnStringResponseListener successListener,
      OnErrorListener errorListener
  ) {
    get(
        OpenBeautyFactsApi.getProduct(barcode),
        response -> {
          String language = application.getResources().getConfiguration().locale.getLanguage();
          String country = application.getResources().getConfiguration().locale.getCountry();
          String both = language + "_" + country;
          if(debug) Log.i(tag, "getOpenBeautyFactsProductName: locale = " + both);
          try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject product = jsonObject.getJSONObject("product");
            String name = product.optString("product_name_" + both);
            if(name.isEmpty()) {
              name = product.optString("product_name_" + language);
            }
            if(name.isEmpty()) {
              name = product.optString("product_name");
            }
            successListener.onResponse(name);
            if(debug) Log.i(tag, "getOpenBeautyFactsProductName: OpenBeautyFacts = " + name);
          } catch (JSONException e) {
            if(debug) Log.e(tag, "getOpenBeautyFactsProductName: " + e);
            successListener.onResponse(null);
          }
        },
        error -> {
          if(debug) Log.e(tag, "getOpenBeautyFactsProductName: can't get OpenBeautyFacts product");
          errorListener.onError(error);
        },
        OpenBeautyFactsApi.getUserAgent(application)
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

  public void checkHassLongLivedToken(OnStringResponseListener onResponseListener) {
    postHassIngress(
        hassServerUrl + "/api/hassio/ingress/session",
        null,
        response -> {
          try {
            boolean isOk = response.get("result").equals("ok");
            onResponseListener.onResponse(isOk ? (String) response.get("result") : null);
          } catch (JSONException e) {
            Log.e(tag,
                "checkHassLongLivedToken (/api/hassio/ingress/session): JSONException:");
            e.printStackTrace();
            onResponseListener.onResponse(null);
          }
        },
        error -> {
          Log.e(tag, "checkHassLongLivedToken (/api/hassio/ingress/session): error: " + error);
          onResponseListener.onResponse(null);
        }
    );
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

    homeAssistantSessionAuth(sessionKey, onSuccessListener, onErrorListener);
  }

  private void homeAssistantSessionAuth(
      String sessionOld,
      OnStringResponseListener onSuccessListener,
      OnStringResponseListener onErrorListener
  ) {
    String hassUrlExtension;
    JSONObject jsonObject = null;
    if (sessionOld != null) {
      hassUrlExtension = "/api/hassio/ingress/validate_session";
      try {
        jsonObject = new JSONObject();
        jsonObject.put("session", sessionOld);
      } catch (JSONException e) {
        Log.e(tag, "homeAssistantSessionAuth: JSONException1:");
        e.printStackTrace();
      }
    } else {
      hassUrlExtension = "/api/hassio/ingress/session";
    }

    postHassIngress(
        hassServerUrl + hassUrlExtension,
        jsonObject,
        response -> {
          try {
            boolean isOk = response.get("result").equals("ok");
            JSONObject data = isOk && response.has("data") ? response.getJSONObject("data") : null;
            String session = data != null && data.has("session") ? data.getString("session") : null;
            if (session != null) {
              sharedPrefs.edit().putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                  session
              ).putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                  dateUtil.getCurrentDateWithTimeStr()
              ).apply();
              onSuccessListener.onResponse(session);
            } else if (isOk && sessionOld != null) {
              sharedPrefs.edit().putString(
                  Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME,
                  dateUtil.getCurrentDateWithTimeStr()
              ).apply();
              onSuccessListener.onResponse(sessionOld);
            } else {
              Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": bad response: " + response);
              onErrorListener.onResponse(null);
            }
          } catch (JSONException e) {
            Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": JSONException2: ");
            e.printStackTrace();
            onErrorListener.onResponse(null);
          }
        },
        error -> {
          Log.e(tag, "homeAssistantSessionAuth: " + hassUrlExtension + ": error: " + error);
          if (sessionOld != null && error instanceof AuthFailureError) {
            homeAssistantSessionAuth(null, onSuccessListener, onErrorListener);
            return;
          }
          onErrorListener.onResponse(null);
        }
    );
  }

  public class Queue {

    private final ArrayList<BaseQueueItem> queueItems;
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

    public Queue append(BaseQueueItem... queueItems) {
      for (BaseQueueItem queueItem : queueItems) {
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
        BaseQueueItem queueItem = queueItems.remove(0);
        if (queueItem instanceof QueueItem) {
          ((QueueItem) queueItem).perform(response -> {
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
        } else {
          ((QueueItemJson) queueItem).perform(response -> {
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

  public void updateData(OnQueueEmptyListener onFinished,
      OnErrorListener errorListener, Class<?>... types) {
    updateData(onFinished, errorListener, null, types);
  }

  public void updateData(OnQueueEmptyListener onFinished,
      OnErrorListener errorListener, String dbChangedTime, Class<?>... types) {
    if (dbChangedTime == null) {
      getTimeDbChanged(
          time -> updateData(onFinished, errorListener, time, types),
          () -> errorListener.onError(null)
      );
      return;
    }
    DownloadHelper.Queue queue = newQueue(onFinished, errorListener);
    for (Class<?> type : types) {
      if (type == ProductGroup.class) {
        queue.append(updateProductGroups(dbChangedTime, null));
      } else if (type == QuantityUnit.class) {
        queue.append(updateQuantityUnits(dbChangedTime, null));
      } else if (type == QuantityUnitConversion.class) {
        queue.append(updateQuantityUnitConversions(dbChangedTime, null));
      } else if (type == Location.class) {
        queue.append(updateLocations(dbChangedTime, null));
      } else if (type == StockLocation.class) {
        queue.append(updateStockCurrentLocations(dbChangedTime, null));
      } else if (type == Product.class) {
        queue.append(updateProducts(dbChangedTime, null));
      } else if (type == ProductLastPurchased.class) {
        queue.append(updateProductsLastPurchased(dbChangedTime, null, true));
      } else if (type == ProductAveragePrice.class) {
        queue.append(updateProductsAveragePrice(dbChangedTime, null, true));
      } else if (type == ProductBarcode.class) {
        queue.append(updateProductBarcodes(dbChangedTime, null));
      } else if (type == User.class) {
        queue.append(updateUsers(dbChangedTime, null));
      } else if (type == StockItem.class) {
        queue.append(updateStockItems(dbChangedTime, null));
      } else if (type == StockEntry.class) {
        queue.append(updateStockEntries(dbChangedTime, null));
      } else if (type == VolatileItem.class) {
        queue.append(updateVolatile(dbChangedTime, null));
      } else if (type == MissingItem.class) {
        queue.append(updateMissingItems(dbChangedTime, null));
      } else if (type == ShoppingListItem.class) {
        queue.append(updateShoppingListItems(dbChangedTime,
            (OnShoppingListItemsResponseListener) null));
      } else if (type == ShoppingList.class) {
        queue.append(updateShoppingLists(dbChangedTime, null));
      } else if (type == Store.class) {
        queue.append(updateStores(dbChangedTime, null));
      } else if (type == Task.class) {
        queue.append(updateTasks(dbChangedTime, null));
      } else if (type == TaskCategory.class) {
        queue.append(updateTaskCategories(dbChangedTime, null));
      } else if (type == Chore.class) {
        queue.append(updateChores(dbChangedTime, null));
      } else if (type == ChoreEntry.class) {
        queue.append(updateChoreEntries(dbChangedTime, null));
      } else if (type == Recipe.class) {
        queue.append(updateRecipes(dbChangedTime, null));
      } else if (type == RecipeFulfillment.class) {
        queue.append(updateRecipeFulfillments(dbChangedTime, null));
      } else if (type == RecipePosition.class) {
        queue.append(updateRecipePositions(dbChangedTime, null));
      }
    }
    if (queue.isEmpty()) {
      onFinished.execute();
      return;
    }
    queue.start();
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

  public interface OnProductsLastPurchasedResponseListener {

    void onResponse(ArrayList<ProductLastPurchased> productsLastPurchased);
  }

  public interface OnProductsAveragePriceResponseListener {

    void onResponse(ArrayList<ProductAveragePrice> productsAveragePrice);
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

  public interface OnShoppingListItemsWithSyncResponseListener {

    void onResponse(
        ArrayList<ShoppingListItem> shoppingListItems,
        ArrayList<ShoppingListItem> itemsToSync,
        HashMap<Integer, ShoppingListItem> serverItemHashMap
    );
  }

  public interface OnShoppingListsResponseListener {

    void onResponse(ArrayList<ShoppingList> shoppingLists);
  }

  public interface OnStoresResponseListener {

    void onResponse(ArrayList<Store> stores);
  }

  public interface OnTasksResponseListener {

    void onResponse(ArrayList<Task> tasks);
  }

  public interface OnTaskCategoriesResponseListener {

    void onResponse(ArrayList<TaskCategory> taskCategories);
  }

  public interface OnChoresResponseListener {

    void onResponse(ArrayList<Chore> chores);
  }

  public interface OnChoreEntriesResponseListener {

    void onResponse(ArrayList<ChoreEntry> choreEntries);
  }

  public interface OnChoreDetailsResponseListener {

    void onResponse(ChoreDetails choreDetails);
  }

  public interface OnRecipesResponseListener {

    void onResponse(ArrayList<Recipe> recipes);
  }

  public interface OnRecipeFulfillmentsResponseListener {

    void onResponse(ArrayList<RecipeFulfillment> recipeFulfillments);
  }

  public interface OnRecipePositionsResponseListener {

    void onResponse(ArrayList<RecipePosition> recipePositions);
  }

  public interface OnUsersResponseListener {

    void onResponse(ArrayList<User> users);
  }

  public interface OnStringResponseListener {

    void onResponse(String response);
  }

  public interface OnIntegerResponseListener {

    void onResponse(int response);
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
