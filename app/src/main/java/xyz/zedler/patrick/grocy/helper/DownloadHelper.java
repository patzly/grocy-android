package xyz.zedler.patrick.grocy.helper;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.CustomJsonObjectRequest;
import xyz.zedler.patrick.grocy.web.CustomStringRequest;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

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

public class DownloadHelper {
    private final GrocyApi grocyApi;
    private RequestQueue requestQueue;
    private final Gson gson;
    private final String uuidHelper;
    private final OnLoadingListener onLoadingListener;
    private final SharedPreferences sharedPrefs;

    private final ArrayList<Queue> queueArrayList;
    private final String tag;
    private final String apiKey;
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
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);
        gson = new Gson();
        requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
        grocyApi = new GrocyApi(application);
        apiKey = sharedPrefs.getString(Constants.PREF.API_KEY, "");
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
        for(Queue queue : queueArrayList) {
            queue.reset(true);
        }
        requestQueue.cancelAll(uuidHelper);
    }

    private void onRequestLoading() {
        loadingRequests += 1;
        if(onLoadingListener != null && loadingRequests == 1) {
            onLoadingListener.onLoadingChanged(true);
        }
    }

    private void onRequestFinished() {
        loadingRequests -= 1;
        if(onLoadingListener != null && loadingRequests == 0) {
            onLoadingListener.onLoadingChanged(false);
        }
    }

    public String getUuid() {
        return uuidHelper;
    }

    public void reloadRequestQueue(Activity activity) {
        requestQueue = RequestQueueSingleton
                .getInstance(activity.getApplicationContext()).getRequestQueue();
    }

    public void get(
            String url,
            String tag,
            OnStringResponseListener onResponse,
            OnErrorListener onError
    ) {
        CustomStringRequest request = new CustomStringRequest(
                Request.Method.GET,
                url,
                apiKey,
                onResponse::onResponse,
                onError::onError,
                this::onRequestFinished,
                timeoutSeconds,
                tag
        );
        onRequestLoading();
        requestQueue.add(request);
    }

    // for requests without loading progress (set noLoadingProgress=true)
    public void get(
            String url,
            String tag,
            OnStringResponseListener onResponse,
            OnErrorListener onError,
            boolean noLoadingProgress
    ) {
        CustomStringRequest request = new CustomStringRequest(
                Request.Method.GET,
                url,
                apiKey,
                onResponse::onResponse,
                onError::onError,
                this::onRequestFinished,
                timeoutSeconds,
                tag,
                noLoadingProgress,
                onLoadingListener
        );
        if(!noLoadingProgress) onRequestLoading();
        requestQueue.add(request);
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
        CustomStringRequest request = new CustomStringRequest(
                Request.Method.GET,
                url,
                apiKey,
                onResponse::onResponse,
                onError::onError,
                this::onRequestFinished,
                timeoutSeconds,
                uuidHelper,
                userAgent
        );
        onRequestLoading();
        requestQueue.add(request);
    }

    public void post(
            String url,
            JSONObject json,
            OnJSONResponseListener onResponse,
            OnErrorListener onError
    ) {
        CustomJsonObjectRequest request = new CustomJsonObjectRequest(
                Request.Method.POST,
                url,
                apiKey,
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

    public void post(String url, OnStringResponseListener onResponse, OnErrorListener onError) {
        CustomStringRequest request = new CustomStringRequest(
                Request.Method.POST,
                url,
                apiKey,
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
        CustomJsonObjectRequest request = new CustomJsonObjectRequest(
                Request.Method.PUT,
                url,
                apiKey,
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

    public void delete(
            String url,
            String tag,
            OnStringResponseListener onResponse,
            OnErrorListener onError
    ) {
        CustomStringRequest request = new CustomStringRequest(
                Request.Method.DELETE,
                url,
                apiKey,
                onResponse::onResponse,
                onError::onError,
                this::onRequestFinished,
                timeoutSeconds,
                tag
        );
        onRequestLoading();
        requestQueue.add(request);
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
                            if(grocyEntity.equals(GrocyApi.ENTITY.QUANTITY_UNITS)) { // Don't change to switch-case!
                                type = new TypeToken<List<QuantityUnit>>(){}.getType();
                            } else if(grocyEntity.equals(GrocyApi.ENTITY.LOCATIONS)) {
                                type = new TypeToken<List<Location>>(){}.getType();
                            } else if(grocyEntity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)) {
                                type = new TypeToken<List<ProductGroup>>(){}.getType();
                            } else if(grocyEntity.equals(GrocyApi.ENTITY.STORES)) {
                                type = new TypeToken<List<Store>>(){}.getType();
                            } else {
                                type = new TypeToken<List<Product>>(){}.getType();
                            }
                            ArrayList<Object> objects = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download Objects: " + objects);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(objects);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
                            Type type = new TypeToken<List<ProductGroup>>(){}.getType();
                            ArrayList<ProductGroup> productGroups = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download ProductGroups: " + productGroups);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(productGroups);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getProductGroups(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped ProductGroups download");
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
                            Type type = new TypeToken<List<QuantityUnit>>(){}.getType();
                            ArrayList<QuantityUnit> quantityUnits = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download QuantityUnits: " + quantityUnits);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(quantityUnits);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getQuantityUnits(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped QuantityUnits download");
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
                            Type type = new TypeToken<List<QuantityUnitConversion>>(){}.getType();
                            ArrayList<QuantityUnitConversion> conversions
                                    = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download QuantityUnitConversions: "
                                    + conversions);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(conversions);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getQuantityUnitConversions(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped QuantityUnitConversions download");
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
                            Type type = new TypeToken<List<Location>>(){}.getType();
                            ArrayList<Location> locations = gson.fromJson(response, type);
                            if(debug) Log.i(tag, "download Locations: " + locations);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(locations);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getLocations(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped Locations download");
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
                            Type type = new TypeToken<List<Product>>(){}.getType();
                            ArrayList<Product> products = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download Products: " + products);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(products);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getProducts(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped Products download");
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
                            Type type = new TypeToken<List<ProductBarcode>>(){}.getType();
                            ArrayList<ProductBarcode> barcodes
                                    = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download Barcodes: " + barcodes);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(barcodes);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getProductBarcodes(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped ProductsBarcodes download");
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
                            if(debug) Log.i(tag, "added ProductBarcode");
                            if(onResponseListener != null) {
                                onResponseListener.onResponse();
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
                            Type type = new TypeToken<List<ProductBarcode>>(){}.getType();
                            ArrayList<ProductBarcode> barcodes
                                    = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download filtered Barcodes: " + barcodes);
                            if(onResponseListener != null) {
                                ProductBarcode barcode = !barcodes.isEmpty()
                                        ? barcodes.get(0) : null; // take first object
                                onResponseListener.onResponse(barcode);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
                            Type type = new TypeToken<List<StockItem>>(){}.getType();
                            ArrayList<StockItem> stockItems = gson.fromJson(response, type);
                            if(debug) Log.i(tag, "download StockItems: " + stockItems);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(stockItems);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getStockItems(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped StockItems download");
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
                            if(debug) Log.i(tag, "download Volatile: success");
                            ArrayList<StockItem> dueItems = new ArrayList<>();
                            ArrayList<StockItem> overdueItems = new ArrayList<>();
                            ArrayList<StockItem> expiredItems = new ArrayList<>();
                            ArrayList<MissingItem> missingItems = new ArrayList<>();
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                // Parse first part of volatile array: expiring products
                                dueItems = gson.fromJson(
                                        jsonObject.getJSONArray("due_products").toString(),
                                        new TypeToken<List<StockItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "download Volatile: due = " + dueItems);
                                // Parse second part of volatile array: overdue products
                                overdueItems = gson.fromJson(
                                        jsonObject.getJSONArray("overdue_products").toString(),
                                        new TypeToken<List<StockItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "download Volatile: overdue = " + overdueItems);
                                // Parse third part of volatile array: expired products
                                expiredItems = gson.fromJson(
                                        jsonObject.getJSONArray("expired_products").toString(),
                                        new TypeToken<List<StockItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "download Volatile: expired = " + overdueItems);
                                // Parse fourth part of volatile array: missing products
                                missingItems = gson.fromJson(
                                        jsonObject.getJSONArray("missing_products").toString(),
                                        new TypeToken<List<MissingItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "download Volatile: missing = " + missingItems);
                            } catch (JSONException e) {
                                if(debug) Log.e(tag, "download Volatile: " + e);
                            }
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(dueItems, overdueItems,
                                        expiredItems, missingItems);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
                        }
                );
            }
        };
    }

    public QueueItem getVolatile(OnVolatileResponseListener onResponseListener) {
        return getVolatile(onResponseListener, null);
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
                            Type type = new TypeToken<ProductDetails>(){}.getType();
                            ProductDetails productDetails = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download ProductDetails: " + productDetails);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(productDetails);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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

    public QueueItem getShoppingListItems(
            OnShoppingListResponseListener onResponseListener,
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
                            Type type = new TypeToken<List<ShoppingListItem>>(){}.getType();
                            ArrayList<ShoppingListItem> shoppingListItems = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download ShoppingListItems: " + shoppingListItems);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(shoppingListItems);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
                        }
                );
            }
        };
    }

    public QueueItem getShoppingListItems(
            OnShoppingListResponseListener onResponseListener
    ) {
        return getShoppingListItems(onResponseListener, null);
    }

    public QueueItem updateShoppingListItems(
            String dbChangedTime,
            OnShoppingListResponseListener onResponseListener
    ) {
        OnShoppingListResponseListener newOnResponseListener = shoppingListItems -> {
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getShoppingListItems(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped ShoppingListItems download");
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
                            Type type = new TypeToken<List<ShoppingList>>(){}.getType();
                            ArrayList<ShoppingList> shoppingLists = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download ShoppingLists: " + shoppingLists);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(shoppingLists);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getShoppingLists(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped ShoppingLists download");
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
                            Type type = new TypeToken<List<Store>>(){}.getType();
                            ArrayList<Store> stores = new Gson().fromJson(response, type);
                            if(debug) Log.i(tag, "download Stores: " + stores);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(stores);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
        if(lastTime == null || !lastTime.equals(dbChangedTime)) {
            return getStores(newOnResponseListener, null);
        } else {
            if(debug) Log.i(tag, "downloadData: skipped Stores download");
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
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(response);
                            }
                            if(responseListener != null) responseListener.onResponse(null);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
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
                            if(debug) Log.i(tag, "delete ShoppingListItem: " + itemId);
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(response);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
                        }
                );
            }
        };
    }

    public QueueItem deleteShoppingListItem(int itemId) {
        return deleteShoppingListItem(itemId, null, null);
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
                        if(debug) Log.e(tag, "getTimeDbChanged: " + e);
                        onErrorListener.onError();
                    }
                },
                error -> onErrorListener.onError(),
                true
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
                            if(debug) Log.i(
                                    tag,
                                    "download StringData from " + url + " : " + response
                            );
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(response);
                            }
                            if(responseListener != null) responseListener.onResponse(response);
                        },
                        error -> {
                            if(debug) Log.e(tag, "download StringData: " + error);
                            if(onErrorListener != null) onErrorListener.onError(error);
                            if(errorListener != null) errorListener.onError(error);
                        }
                );
            }
        };
    }

    public QueueItem getStringData(String url, OnStringResponseListener onResponseListener) {
        return getStringData(url, onResponseListener, null);
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
            for(QueueItem queueItem : queueItems) {
                if(queueItem == null) continue;
                this.queueItems.add(queueItem);
                queueSize++;
            }
            return this;
        }

        public void start() {
            if(isRunning) {
                return;
            } else {
                isRunning = true;
            }
            if(queueItems.isEmpty()) {
                if(onQueueEmptyListener != null) onQueueEmptyListener.execute();
                return;
            }
            while(!queueItems.isEmpty()) {
                QueueItem queueItem = queueItems.remove(0);
                queueItem.perform(response -> {
                    queueSize--;
                    if(queueSize > 0) return;
                    isRunning = false;
                    if(onQueueEmptyListener != null) onQueueEmptyListener.execute();
                    reset(false);
                }, error -> {
                    isRunning = false;
                    if(onErrorListener != null) onErrorListener.onError(error);
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
            if(cancelAll) requestQueue.cancelAll(uuidQueue);
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

    public abstract static class BaseQueueItem { }

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
            perform((OnJSONResponseListener) null, null, uuid);
        }
    }

    public interface OnObjectsResponseListener {
        void onResponse(ArrayList<Object> arrayList);
    }

    public interface OnProductGroupsResponseListener {
        void onResponse(ArrayList<ProductGroup> arrayList);
    }

    public interface OnQuantityUnitsResponseListener {
        void onResponse(ArrayList<QuantityUnit> arrayList);
    }

    public interface OnQuantityUnitConversionsResponseListener {
        void onResponse(ArrayList<QuantityUnitConversion> arrayList);
    }

    public interface OnLocationsResponseListener {
        void onResponse(ArrayList<Location> arrayList);
    }

    public interface OnProductsResponseListener {
        void onResponse(ArrayList<Product> arrayList);
    }

    public interface OnProductBarcodesResponseListener {
        void onResponse(ArrayList<ProductBarcode> arrayList);
    }

    public interface OnProductBarcodeResponseListener {
        void onResponse(ProductBarcode productBarcode);
    }

    public interface OnStockItemsResponseListener {
        void onResponse(ArrayList<StockItem> arrayList);
    }

    public interface OnVolatileResponseListener {
        void onResponse(
                ArrayList<StockItem> due,
                ArrayList<StockItem> overdue,
                ArrayList<StockItem> expired,
                ArrayList<MissingItem> missing
        );
    }

    public interface OnProductDetailsResponseListener {
        void onResponse(ProductDetails productDetails);
    }

    public interface OnShoppingListResponseListener {
        void onResponse(ArrayList<ShoppingListItem> arrayList);
    }

    public interface OnShoppingListsResponseListener {
        void onResponse(ArrayList<ShoppingList> arrayList);
    }

    public interface OnStoresResponseListener {
        void onResponse(ArrayList<Store> arrayList);
    }

    public interface OnStringResponseListener {
        void onResponse(String response);
    }

    public interface OnJSONResponseListener {
        void onResponse(JSONObject response);
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
}
