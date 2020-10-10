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
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.CustomJsonObjectRequest;
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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

public class DownloadHelper {
    private GrocyApi grocyApi;
    private RequestQueue requestQueue;
    private Gson gson;
    private SimpleDateFormat dateTimeFormat;
    private String uuidHelper;
    private OnLoadingListener onLoadingListener;

    private ArrayList<Queue> queueArrayList;
    private String tag;
    private boolean debug;
    private int loadingRequests;

    public DownloadHelper(
            Application application,
            String tag,
            OnLoadingListener onLoadingListener
    ) {
        this.tag = tag;
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);
        gson = new Gson();
        requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
        grocyApi = new GrocyApi(application);
        uuidHelper = UUID.randomUUID().toString();
        dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        queueArrayList = new ArrayList<>();
        loadingRequests = 0;
        this.onLoadingListener = onLoadingListener;
    }

    public DownloadHelper(Activity activity, String tag) {
        this(activity.getApplication(), tag, null);
    }

    // cancel all requests
    public void destroy() {
        for(Queue queue : queueArrayList) {
            queue.reset();
        }
        requestQueue.cancelAll(uuidHelper);
        resetLoadingRequests();
    }

    private void resetLoadingRequests() {
        this.loadingRequests = 0;
        if(onLoadingListener != null) onLoadingListener.onLoadingChanged(false);
    }

    private void onRequestLoading() {
        loadingRequests += 1;
        if(onLoadingListener != null) onLoadingListener.onLoadingChanged(true);
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
            OnResponseListener onResponse,
            OnErrorListener onError
    ) {
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        );
        if(tag != null) request.setTag(tag);
        onRequestLoading();
        requestQueue.add(request);
    }

    // for single requests without a queue
    public void get(
            String url,
            OnResponseListener onResponse,
            OnErrorListener onError
    ) {
        get(url, uuidHelper, onResponse, onError);
    }

    // GET requests with modified user-agent
    public void get(
            String url,
            OnResponseListener onResponse,
            OnErrorListener onError,
            String userAgent
    ) {
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("User-Agent", userAgent);
                return params;
            }
        };
        request.setTag(uuidHelper);
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
                json,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        );
        request.setTag(uuidHelper);
        onRequestLoading();
        requestQueue.add(request);
    }

    public void post(String url, OnResponseListener onResponse, OnErrorListener onError) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        );
        request.setTag(uuidHelper);
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
                json,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        );
        request.setTag(uuidHelper);
        onRequestLoading();
        requestQueue.add(request);
    }

    public void delete(
            String url,
            String tag,
            OnResponseListener onResponse,
            OnErrorListener onError
    ) {
        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    onRequestFinished();
                    onResponse.onResponse(response);
                },
                error -> {
                    onRequestFinished();
                    onError.onError(error);
                }
        );
        request.setTag(tag);
        onRequestLoading();
        requestQueue.add(request);
    }

    public void delete(
            String url,
            OnResponseListener onResponse,
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
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getQuantityUnits(
            OnQuantityUnitsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getLocations(
            OnLocationsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getProducts(
            OnProductsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getStockItems(
            OnStockItemsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getVolatile(
            OnVolatileResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
                    @Nullable OnErrorListener errorListener,
                    @Nullable String uuid
            ) {
                get(
                        grocyApi.getStockVolatile(),
                        uuid,
                        response -> {
                            if(debug) Log.i(tag, "downloadVolatile: success");
                            ArrayList<StockItem> expiringItems = new ArrayList<>();
                            ArrayList<StockItem> expiredItems = new ArrayList<>();
                            ArrayList<MissingItem> missingItems = new ArrayList<>();
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                // Parse first part of volatile array: expiring products
                                expiringItems = gson.fromJson(
                                        jsonObject.getJSONArray("expiring_products").toString(),
                                        new TypeToken<List<StockItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "downloadVolatile: expiring = " + expiringItems);
                                // Parse second part of volatile array: expired products
                                expiredItems = gson.fromJson(
                                        jsonObject.getJSONArray("expired_products").toString(),
                                        new TypeToken<List<StockItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "downloadVolatile: expired = " + expiredItems);
                                // Parse third part of volatile array: missing products
                                missingItems = gson.fromJson(
                                        jsonObject.getJSONArray("missing_products").toString(),
                                        new TypeToken<List<MissingItem>>(){}.getType()
                                );
                                if(debug) Log.i(tag, "downloadVolatile: missing = " + missingItems);
                            } catch (JSONException e) {
                                if(debug) Log.e(tag, "downloadVolatile: " + e);
                            }
                            if(onResponseListener != null) {
                                onResponseListener.onResponse(expiringItems, expiredItems, missingItems);
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
                    @Nullable OnResponseListener responseListener,
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
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getShoppingLists(
            OnShoppingListsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getStores(
            OnStoresResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public void deleteProduct(
            int productId,
            OnResponseListener onResponseListener,
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
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem editShoppingListItem(
            int itemId,
            JSONObject body,
            OnJSONResponseListener onResponseListener
    ) {
        return editShoppingListItem(itemId, body, onResponseListener, null);
    }

    public QueueItem deleteShoppingListItem(
            int itemId,
            OnResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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
            OnDateResponseListener onResponseListener,
            OnSimpleErrorListener onErrorListener
    ) {
        get(
                grocyApi.getDbChangedTime(),
                uuidHelper,
                response -> {
                    try {
                        JSONObject body = new JSONObject(response);
                        String dateStr = body.getString("changed_time");
                        Date date = dateTimeFormat.parse(dateStr);
                        onResponseListener.onResponse(date);
                    } catch (JSONException | ParseException e) {
                        if(debug) Log.e(tag, "getTimeDbChanged: " + e);
                        onErrorListener.onError();
                    }
                },
                error -> onErrorListener.onError()
        );
    }

    public QueueItem getStringData(
            String url,
            OnResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        return new QueueItem() {
            @Override
            public void perform(
                    @Nullable OnResponseListener responseListener,
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

    public QueueItem getStringData(String url, OnResponseListener onResponseListener) {
        return getStringData(url, onResponseListener, null);
    }

    public class Queue {
        private ArrayList<QueueItem> queueItems;
        private OnQueueEmptyListener onQueueEmptyListener;
        private OnErrorListener onErrorListener;
        private String uuidQueue;
        private int queueSize;

        public Queue(OnQueueEmptyListener onQueueEmptyListener, OnErrorListener onErrorListener) {
            this.onQueueEmptyListener = onQueueEmptyListener;
            this.onErrorListener = onErrorListener;
            queueItems = new ArrayList<>();
            uuidQueue = UUID.randomUUID().toString();
            queueSize = 0;
        }

        public void append(ArrayList<QueueItem> queueItems) {
            this.queueItems.addAll(queueItems);
            queueSize += queueItems.size();
        }

        public Queue append(QueueItem... queueItems) {
            this.queueItems.addAll(Arrays.asList(queueItems));
            queueSize += queueItems.length;
            return this;
        }

        public void start() {
            while(!queueItems.isEmpty()) {
                QueueItem queueItem = queueItems.remove(0);
                queueItem.perform(response -> {
                    queueSize--;
                    if(queueSize > 0) return;
                    if(onQueueEmptyListener != null) onQueueEmptyListener.execute();
                    reset();
                }, error -> {
                    if(onErrorListener != null) onErrorListener.onError(error);
                    reset();
                }, uuidQueue);
            }
        }

        private void reset() {
            requestQueue.cancelAll(uuidQueue);
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

    public abstract static class QueueItem {
        public abstract void perform(
                OnResponseListener responseListener,
                OnErrorListener errorListener,
                String uuid
        );
        public void perform(String uuid) {
            // UUID is for cancelling the requests; should be uuidHelper from above
            perform(null, null, uuid);
        }
    }

    public interface OnProductGroupsResponseListener {
        void onResponse(ArrayList<ProductGroup> arrayList);
    }

    public interface OnQuantityUnitsResponseListener {
        void onResponse(ArrayList<QuantityUnit> arrayList);
    }

    public interface OnLocationsResponseListener {
        void onResponse(ArrayList<Location> arrayList);
    }

    public interface OnProductsResponseListener {
        void onResponse(ArrayList<Product> arrayList);
    }

    public interface OnStockItemsResponseListener {
        void onResponse(ArrayList<StockItem> arrayList);
    }

    public interface OnVolatileResponseListener {
        void onResponse(
                ArrayList<StockItem> expiring,
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

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnJSONResponseListener {
        void onResponse(JSONObject response);
    }

    public interface OnDateResponseListener {
        void onResponse(Date date);
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
