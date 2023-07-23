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

package xyz.zedler.patrick.grocy.helper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductAveragePrice;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.ShoppingListItem.ShoppingListItemWithSync;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.CustomByteArrayRequest;
import xyz.zedler.patrick.grocy.web.CustomJsonArrayRequest;
import xyz.zedler.patrick.grocy.web.CustomJsonObjectRequest;
import xyz.zedler.patrick.grocy.web.CustomStringRequest;
import xyz.zedler.patrick.grocy.web.NetworkQueue;
import xyz.zedler.patrick.grocy.web.NetworkQueue.OnQueueEmptyListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class DownloadHelper {

  private static final String TAG = DownloadHelper.class.getSimpleName();

  public final Application application;
  public final GrocyApi grocyApi;
  private final RequestQueue requestQueue;
  public final Gson gson;
  private final String uuidHelper;
  private final OnLoadingListener onLoadingListener;
  private final MutableLiveData<Boolean> offlineLive;
  public final SharedPreferences sharedPrefs;
  public final AppDatabase appDatabase;

  private final ArrayList<NetworkQueue> queueArrayList;
  public final String tag;
  private final String apiKey;
  public final boolean debug;
  private final int timeoutSeconds;
  private int loadingRequests;

  public DownloadHelper(
      Application application,
      String tag,
      OnLoadingListener onLoadingListener,
      MutableLiveData<Boolean> offlineLive
  ) {
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
    gson = new GsonBuilder().registerTypeAdapter(Double.class, new BadDoubleDeserializer())
        .registerTypeAdapter(double.class, new BadDoubleDeserializer()).create();
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application);
    apiKey = sharedPrefs.getString(Constants.PREF.API_KEY, "");
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    this.offlineLive = offlineLive;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(
      Application application,
      String serverUrl,
      String apiKey,
      String tag,
      OnLoadingListener onLoadingListener
  ) {
    this.application = application;
    this.tag = tag;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    gson = new GsonBuilder().registerTypeAdapter(Double.class, new BadDoubleDeserializer()).create();
    appDatabase = AppDatabase.getAppDatabase(application.getApplicationContext());
    RequestQueueSingleton.getInstance(application).newRequestQueue();
    requestQueue = RequestQueueSingleton.getInstance(application).getRequestQueue();
    grocyApi = new GrocyApi(application, serverUrl);
    this.apiKey = apiKey;
    uuidHelper = UUID.randomUUID().toString();
    queueArrayList = new ArrayList<>();
    loadingRequests = 0;
    this.onLoadingListener = onLoadingListener;
    this.offlineLive = null;
    timeoutSeconds = sharedPrefs.getInt(
        Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
        Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
    );
  }

  public DownloadHelper(Activity activity, String tag) {
    this(activity.getApplication(), tag, null, null);
  }

  public DownloadHelper(Context context, String tag) {
    this((Application) context.getApplicationContext(), tag, null, null);
  }

  // cancel all requests
  public void destroy() {
    for (NetworkQueue queue : queueArrayList) {
      queue.reset(true);
    }
    requestQueue.cancelAll(uuidHelper);
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
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomStringRequest request = new CustomStringRequest(
        Request.Method.GET,
        url,
        apiKey,
        sessionKey,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        tag
    );
    requestQueue.add(request);
  }

  // for requests without loading progress (set noLoadingProgress=true) TODO
  public void get(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError,
      boolean noLoadingProgress
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomStringRequest request = new CustomStringRequest(
        Request.Method.GET,
        url,
        apiKey,
        sessionKey,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        tag,
        noLoadingProgress,
        onLoadingListener
    );
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
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomStringRequest request = new CustomStringRequest(
        Request.Method.GET,
        url,
        apiKey,
        sessionKey,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        uuidHelper,
        userAgent
    );
    requestQueue.add(request);
  }

  public void post(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomJsonObjectRequest request = new CustomJsonObjectRequest(
        Request.Method.POST,
        url,
        apiKey,
        sessionKey,
        json,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        uuidHelper
    );
    requestQueue.add(request);
  }

  public void postWithArray(
      String url,
      JSONObject json,
      OnJSONArrayResponseListener onResponse,
      OnErrorListener onError
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomJsonArrayRequest request = new CustomJsonArrayRequest(
        Request.Method.POST,
        url,
        apiKey,
        sessionKey,
        json,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        uuidHelper
    );
    requestQueue.add(request);
  }

  public void post(String url, OnStringResponseListener onResponse, OnErrorListener onError) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomStringRequest request = new CustomStringRequest(
        Request.Method.POST,
        url,
        apiKey,
        sessionKey,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        uuidHelper
    );
    requestQueue.add(request);
  }

  public void put(
      String url,
      JSONObject json,
      OnJSONResponseListener onResponse,
      OnErrorListener onError
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomJsonObjectRequest request = new CustomJsonObjectRequest(
        Request.Method.PUT,
        url,
        apiKey,
        sessionKey,
        json,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        uuidHelper
    );
    requestQueue.add(request);
  }

  public void putFile(
      String url,
      byte[] fileContent,
      Runnable onSuccess,
      OnErrorListener onError
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomByteArrayRequest request = new CustomByteArrayRequest(
        Request.Method.PUT,
        url,
        apiKey,
        sessionKey,
        fileContent,
        onSuccess,
        onError::onError,
        timeoutSeconds,
        uuidHelper
    );
    requestQueue.add(request);
  }

  public void delete(
      String url,
      String tag,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    String sessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
    CustomStringRequest request = new CustomStringRequest(
        Request.Method.DELETE,
        url,
        apiKey,
        sessionKey,
        onResponse::onResponse,
        onError::onError,
        timeoutSeconds,
        tag
    );
    requestQueue.add(request);
  }

  public void delete(
      String url,
      OnStringResponseListener onResponse,
      OnErrorListener onError
  ) {
    delete(url, uuidHelper, onResponse, onError);
  }

  public void getTimeDbChanged(
      OnStringResponseListener onResponseListener,
      OnMultiTypeErrorListener onErrorListener
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
            Log.e(tag, "getTimeDbChanged: " + e);
            onErrorListener.onError(e);
          }
        },
        onErrorListener::onError,
        !sharedPrefs.getBoolean(
            Constants.SETTINGS.NETWORK.LOADING_CIRCLE,
            Constants.SETTINGS_DEFAULT.NETWORK.LOADING_CIRCLE
        )
    );
  }

  public NetworkQueue newQueue(
      OnQueueEmptyListener onQueueEmptyListener,
      OnMultiTypeErrorListener onErrorListener
  ) {
    NetworkQueue queue = new NetworkQueue(
        requestQueue,
        onQueueEmptyListener,
        onErrorListener,
        onLoadingListener
    );
    queueArrayList.add(queue);
    return queue;
  }

  public void updateData(
      OnQueueEmptyListener onFinished,
      OnMultiTypeErrorListener errorListener,
      boolean forceUpdate,
      boolean errorsOnlyWithForceUpdate,
      Class<?>... types
  ) {
    updateData(
        onFinished,
        errorListener,
        null,
        forceUpdate,
        errorsOnlyWithForceUpdate,
        null,
        types
    );
  }

  public void updateData(
      OnQueueEmptyListener onFinished,
      OnMultiTypeErrorListener errorListener,
      @Nullable String dbChangedTime,
      // true if appropriate caching info for respective object lists should be deleted
      // which results in updating even if info is not updated on server
      boolean forceUpdate,
      // true if page can display offline content and errors
      // should not be displayed when user enters page, instead it can display an offline
      // banner (live updated with offlineLive data)
      boolean errorsOnlyWithForceUpdate,
      @Nullable QueueItem extraQueueItem,
      Class<?>... types
  ) {
    if (dbChangedTime == null) {
      getTimeDbChanged(
          time -> updateData(
              onFinished,
              errorListener,
              time,
              forceUpdate,
              errorsOnlyWithForceUpdate,
              extraQueueItem,
              types
          ),
          error -> {
            if (offlineLive != null) offlineLive.setValue(true);
            if (errorsOnlyWithForceUpdate && !forceUpdate) {
              return;
            }
            errorListener.onError(error);
          }
      );
      return;
    }

    NetworkQueue queue = newQueue(updated -> {
      if (offlineLive != null) offlineLive.setValue(false);
      onFinished.onQueueEmpty(updated);
    }, error -> {
      if (offlineLive != null) offlineLive.setValue(true);
      if (errorsOnlyWithForceUpdate && !forceUpdate) {
        return;
      }
      errorListener.onError(error);
    });

    boolean hasUnitConversionType = Arrays.stream(types)
        .anyMatch(type -> type == QuantityUnitConversionResolved.class);
    if (Arrays.stream(types).anyMatch(type -> type == Product.class) || hasUnitConversionType) {
      queue.append(Product.updateProducts(this, dbChangedTime, forceUpdate, products -> {
        if (hasUnitConversionType) {
          queue.appendWhileRunning(QuantityUnitConversionResolved.updateQuantityUnitConversions(
              DownloadHelper.this, dbChangedTime, forceUpdate, products, null
          ));
        }
      }, hasUnitConversionType));
    }
    for (Class<?> type : types) {
      if (type == ProductGroup.class) {
        queue.append(ProductGroup.updateProductGroups(this, dbChangedTime, forceUpdate, null));
      } else if (type == QuantityUnit.class) {
        queue.append(QuantityUnit.updateQuantityUnits(this, dbChangedTime, forceUpdate, null));
      } else if (type == QuantityUnitConversion.class) {
        queue.append(QuantityUnitConversion.updateQuantityUnitConversions(this, dbChangedTime, forceUpdate, null));
      } else if (type == Location.class) {
        queue.append(Location.updateLocations(this, dbChangedTime, forceUpdate, null));
      } else if (type == StockLocation.class) {
        queue.append(StockLocation.updateStockCurrentLocations(this, dbChangedTime, forceUpdate, null));
      } else if (type == ProductLastPurchased.class) {
        queue.append(ProductLastPurchased.updateProductsLastPurchased(this, dbChangedTime, forceUpdate, null, true));
      } else if (type == ProductAveragePrice.class) {
        queue.append(ProductAveragePrice.updateProductsAveragePrice(this, dbChangedTime, forceUpdate, null, true));
      } else if (type == ProductBarcode.class) {
        queue.append(ProductBarcode.updateProductBarcodes(this, dbChangedTime, forceUpdate, null));
      } else if (type == User.class) {
        queue.append(User.updateUsers(this, dbChangedTime, forceUpdate, null));
      } else if (type == StockItem.class) {
        queue.append(StockItem.updateStockItems(this, dbChangedTime, forceUpdate, null));
      } else if (type == StockEntry.class) {
        queue.append(StockEntry.updateStockEntries(this, dbChangedTime, forceUpdate, null));
      } else if (type == VolatileItem.class) {
        queue.append(VolatileItem.updateVolatile(this, dbChangedTime, forceUpdate, null));
      } else if (type == MissingItem.class) {
        queue.append(MissingItem.updateMissingItems(this, dbChangedTime, forceUpdate, null));
      } else if (type == ShoppingListItem.class) {
        queue.append(ShoppingListItem.updateShoppingListItems(this, dbChangedTime, forceUpdate,
            null));
      } else if (type == ShoppingListItemWithSync.class) {
        queue.append(ShoppingListItem.updateShoppingListItemsWithoutNotSyncedItems(this, dbChangedTime, forceUpdate,
            null));
      } else if (type == ShoppingList.class) {
        queue.append(ShoppingList.updateShoppingLists(this, dbChangedTime, forceUpdate, null));
      } else if (type == Store.class) {
        queue.append(Store.updateStores(this, dbChangedTime, forceUpdate, null));
      } else if (type == Task.class) {
        queue.append(Task.updateTasks(this, dbChangedTime, forceUpdate, null));
      } else if (type == TaskCategory.class) {
        queue.append(TaskCategory.updateTaskCategories(this, dbChangedTime, forceUpdate, null));
      } else if (type == Chore.class) {
        queue.append(Chore.updateChores(this, dbChangedTime, forceUpdate, null));
      } else if (type == ChoreEntry.class) {
        queue.append(ChoreEntry.updateChoreEntries(this, dbChangedTime, forceUpdate, null));
      } else if (type == Recipe.class) {
        queue.append(Recipe.updateRecipes(this, dbChangedTime, forceUpdate, null));
      } else if (type == RecipeFulfillment.class) {
        queue.append(RecipeFulfillment.updateRecipeFulfillments(this, dbChangedTime, forceUpdate, null));
      } else if (type == RecipePosition.class) {
        queue.append(RecipePosition.updateRecipePositions(this, dbChangedTime, forceUpdate, null));
      } else if (type == RecipePositionResolved.class) {
        queue.append(RecipePositionResolved.updateRecipePositionsResolved(this, dbChangedTime, forceUpdate, null));
      }
    }

    queue.append(extraQueueItem);
    queue.start();
  }

  public interface OnObjectsResponseListener<T> {

    void onResponse(List<T> objects);
  }

  public interface OnObjectResponseListener<T> {

    void onResponse(T object);
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

  public interface OnErrorListener {

    void onError(VolleyError volleyError);
  }

  public interface OnMultiTypeErrorListener {

    void onError(Object error);
  }

  public interface OnLoadingListener {

    void onLoadingChanged(boolean isLoading);
  }

  public interface OnSettingUploadListener {

    void onFinished(@StringRes int msg);
  }

  public static class BadDoubleDeserializer implements JsonDeserializer<Double> {
    @Override
    public Double deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
      try {
        return NumUtil.toDouble(element.getAsString());
      } catch (NumberFormatException e) {
        throw new JsonParseException(e);
      }
    }
  }
}
