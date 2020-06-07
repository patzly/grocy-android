package xyz.zedler.patrick.grocy.helper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

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

    private final static boolean DEBUG = true;

    private Activity activity;
    private String tag;
    private RequestQueue requestQueue;
    private GrocyApi grocyApi;
    private WebRequest request;
    private Gson gson = new Gson();
    private OnErrorListener onErrorListener;
    private OnQueueEmptyListener onQueueEmptyListener;
    private int queueSize;

    public DownloadHelper(
            Activity activity,
            String tag,
            OnErrorListener onErrorListener,
            OnQueueEmptyListener onQueueEmptyListener
    ) {
        Context context = activity.getApplicationContext();
        this.activity = activity;
        this.tag = tag;
        this.onErrorListener = onErrorListener;
        this.onQueueEmptyListener = onQueueEmptyListener;
        requestQueue = RequestQueueSingleton.getInstance(context).getRequestQueue();
        request = new WebRequest(requestQueue);
        grocyApi = new GrocyApi(context);
    }

    public DownloadHelper(
            Activity activity,
            String tag,
            OnQueueEmptyListener onQueueEmptyListener
    ) {
        Context context = activity.getApplicationContext();
        this.activity = activity;
        this.tag = tag;
        this.onErrorListener = null;
        this.onQueueEmptyListener = onQueueEmptyListener;
        requestQueue = RequestQueueSingleton.getInstance(context).getRequestQueue();
        request = new WebRequest(requestQueue);
        grocyApi = new GrocyApi(context);
    }

    public DownloadHelper(
            Activity activity,
            String tag,
            OnErrorListener onErrorListener
    ) {
        Context context = activity.getApplicationContext();
        this.activity = activity;
        this.tag = tag;
        this.onErrorListener = onErrorListener;
        this.onQueueEmptyListener = null;
        requestQueue = RequestQueueSingleton.getInstance(context).getRequestQueue();
        request = new WebRequest(requestQueue);
        grocyApi = new GrocyApi(context);
    }

    public DownloadHelper(
            Activity activity,
            String tag
    ) {
        Context context = activity.getApplicationContext();
        this.activity = activity;
        this.tag = tag;
        this.onErrorListener = null;
        this.onQueueEmptyListener = null;
        requestQueue = RequestQueueSingleton.getInstance(context).getRequestQueue();
        request = new WebRequest(requestQueue);
        grocyApi = new GrocyApi(context);
    }

    public void downloadProductGroups(
            OnProductGroupsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                tag,
                response -> {
                    Type type = new TypeToken<List<ProductGroup>>(){}.getType();
                    ArrayList<ProductGroup> productGroups = gson.fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadProductGroups: " + productGroups);
                    onResponseListener.onResponse(productGroups);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadProductGroups(OnProductGroupsResponseListener onResponseListener) {
        downloadProductGroups(onResponseListener, null);
    }

    public void downloadQuantityUnits(
            OnQuantityUnitsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                tag,
                response -> {
                    Type type = new TypeToken<List<QuantityUnit>>(){}.getType();
                    ArrayList<QuantityUnit> quantityUnits = gson.fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadQuantityUnits: " + quantityUnits);
                    onResponseListener.onResponse(quantityUnits);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadQuantityUnits(OnQuantityUnitsResponseListener onResponseListener) {
        downloadQuantityUnits(onResponseListener, null);
    }

    public void downloadLocations(
            OnLocationsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                tag,
                response -> {
                    Type type = new TypeToken<List<Location>>(){}.getType();
                    ArrayList<Location> locations = gson.fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadLocations: " + locations);
                    onResponseListener.onResponse(locations);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadLocations(OnLocationsResponseListener onResponseListener) {
        downloadLocations(onResponseListener, null);
    }

    public void downloadProducts(
            OnProductsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                tag,
                response -> {
                    Type type = new TypeToken<List<Product>>(){}.getType();
                    ArrayList<Product> products = gson.fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadProducts: " + products);
                    onResponseListener.onResponse(products);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadProducts(OnProductsResponseListener onResponseListener) {
        downloadProducts(onResponseListener, null);
    }

    public void downloadStockItems(
            OnStockItemsResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getStock(),
                tag,
                response -> {
                    Type type = new TypeToken<List<StockItem>>(){}.getType();
                    ArrayList<StockItem> stockItems = gson.fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadStockItems: " + stockItems);
                    onResponseListener.onResponse(stockItems);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadStockItems(OnStockItemsResponseListener onResponseListener) {
        downloadStockItems(onResponseListener, null);
    }

    public void downloadVolatile(
            OnVolatileResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        queueSize++;
        request.get(
                grocyApi.getStockVolatile(),
                tag,
                response -> {
                    if(DEBUG) Log.i(tag, "downloadVolatile: success");
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
                        if(DEBUG) Log.i(tag, "downloadVolatile: expiring = " + expiringItems);
                        // Parse second part of volatile array: expired products
                        expiredItems = gson.fromJson(
                                jsonObject.getJSONArray("expired_products").toString(),
                                new TypeToken<List<StockItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(tag, "downloadVolatile: expired = " + expiredItems);
                        // Parse third part of volatile array: missing products
                        missingItems = gson.fromJson(
                                jsonObject.getJSONArray("missing_products").toString(),
                                new TypeToken<List<MissingItem>>(){}.getType()
                        );
                        if(DEBUG) Log.i(tag, "downloadVolatile: missing = " + missingItems);
                    } catch (JSONException e) {
                        Log.e(tag, "downloadVolatile: " + e);
                    }
                    onResponseListener.onResponse(expiringItems, expiredItems, missingItems);
                    checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadVolatile(OnVolatileResponseListener onResponseListener) {
        downloadVolatile(onResponseListener, null);
    }

    public void downloadProductDetails(
            int productId,
            OnProductDetailsResponseListener onResponseListener,
            OnErrorListener onErrorListener,
            boolean enableQueue
    ) {
        if(enableQueue) queueSize++;
        request.get(
                grocyApi.getStockProductDetails(productId),
                response -> {
                    Type type = new TypeToken<ProductDetails>(){}.getType();
                    ProductDetails productDetails = new Gson().fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadProductDetails: " + productDetails);
                    onResponseListener.onResponse(productDetails);
                    if(enableQueue) checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadProductDetails(
            int productId,
            OnProductDetailsResponseListener onResponseListener
    ) {
        downloadProductDetails(productId, onResponseListener, null, true);
    }

    public void downloadShoppingList(
            OnShoppingListResponseListener onResponseListener,
            OnErrorListener onErrorListener,
            boolean enableQueue
    ) {
        if(enableQueue) queueSize++;
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                response -> {
                    Type type = new TypeToken<List<ShoppingListItem>>(){}.getType();
                    ArrayList<ShoppingListItem> listItems = new Gson().fromJson(response, type);
                    if(DEBUG) Log.i(tag, "downloadShoppingList: " + listItems);
                    onResponseListener.onResponse(listItems);
                    if(enableQueue) checkQueueSize();
                },
                error -> onError(error, onErrorListener)
        );
    }

    public void downloadShoppingList(
            OnShoppingListResponseListener onResponseListener
    ) {
        downloadShoppingList(onResponseListener, null, true);
    }

    public void deleteProduct(
            int productId,
            OnResponseListener onResponseListener,
            OnErrorListener onErrorListener
    ) {
        request.delete(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, productId),
                onResponseListener::onResponse,
                onErrorListener::onError
        );
    }

    private void checkQueueSize() {
        queueSize--;
        if(onQueueEmptyListener != null && queueSize == 0) {
            onQueueEmptyListener.execute();
        }
    }

    private void onError(VolleyError error, OnErrorListener onErrorListener) {
        if(onErrorListener != null) onErrorListener.onError(error);
        else if(this.onErrorListener != null) this.onErrorListener.onError(error);
        if(onQueueEmptyListener != null) {
            request.cancelAll(tag);
            queueSize = 0;
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

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnErrorListener {
        void onError(VolleyError volleyError);
    }

    public interface OnQueueEmptyListener {
        void execute();
    }
}
