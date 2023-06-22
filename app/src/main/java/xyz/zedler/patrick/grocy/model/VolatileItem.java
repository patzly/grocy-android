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

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.android.volley.Response;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "volatile_item_table")
public class VolatileItem implements Parcelable {

  public final static int TYPE_DUE = 1;
  public final static int TYPE_OVERDUE = 2;
  public final static int TYPE_EXPIRED = 3;
  // missing items are in a separate table

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private int id;

  @ColumnInfo(name = "product_id")
  private int productId;

  @ColumnInfo(name = "volatile_type")
  private int volatileType;

  // for Room
  public VolatileItem() {
  }

  @Ignore
  public VolatileItem(int productId, int volatileType) {
    this.productId = productId;
    this.volatileType = volatileType;
  }

  private VolatileItem(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    volatileType = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeInt(volatileType);
  }

  public static final Creator<VolatileItem> CREATOR = new Creator<>() {

    @Override
    public VolatileItem createFromParcel(Parcel in) {
      return new VolatileItem(in);
    }

    @Override
    public VolatileItem[] newArray(int size) {
      return new VolatileItem[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public int getVolatileType() {
    return volatileType;
  }

  public void setVolatileType(int volatileType) {
    this.volatileType = volatileType;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "VolatileItem(" + id + ", " + volatileType + ')';
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateVolatile(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnVolatileResponseListener onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE, null
    ) : null;
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          dlHelper.get(
              dlHelper.grocyApi.getStockVolatile(),
              uuid,
              response -> {
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "updateVolatile: success");
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
                  dueItems = dlHelper.gson.fromJson(
                      jsonObject.getJSONArray("due_products").toString(), typeStockItem
                  );
                  // Parse second part of volatile array: overdue products
                  overdueItems = dlHelper.gson.fromJson(
                      jsonObject.getJSONArray("overdue_products").toString(), typeStockItem
                  );
                  // Parse third part of volatile array: expired products
                  expiredItems = dlHelper.gson.fromJson(
                      jsonObject.getJSONArray("expired_products").toString(), typeStockItem
                  );
                  // Parse fourth part of volatile array: missing products
                  missingItems = dlHelper.gson.fromJson(
                      jsonObject.getJSONArray("missing_products").toString(),
                      new TypeToken<List<MissingItem>>() {
                      }.getType()
                  );
                  if (dlHelper.debug) {
                    Log.i(dlHelper.tag, "updateVolatile:\ndue = " + dueItems + "\noverdue: "
                        + overdueItems + "\nexpired: " + expiredItems + "\nmissing: "
                        + missingItems);
                  }
                } catch (JSONException e) {
                  if (dlHelper.debug) {
                    Log.e(dlHelper.tag, "updateVolatile: " + e);
                  }
                }
                ArrayList<VolatileItem> volatileItemsTogether = new ArrayList<>();
                for (StockItem stockItem : dueItems) {
                  if (stockItem.getDueTypeInt() != StockItem.DUE_TYPE_BEST_BEFORE) continue;
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_DUE)
                  );
                }
                for (StockItem stockItem : overdueItems) {
                  if (stockItem.getDueTypeInt() != StockItem.DUE_TYPE_BEST_BEFORE) continue;
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_OVERDUE)
                  );
                }
                for (StockItem stockItem : expiredItems) {
                  if (stockItem.getDueTypeInt() != StockItem.DUE_TYPE_EXPIRATION) continue;
                  volatileItemsTogether.add(
                      new VolatileItem(stockItem.getProductId(), VolatileItem.TYPE_EXPIRED)
                  );
                }
                ArrayList<StockItem> finalDueItems = dueItems;
                ArrayList<StockItem> finalOverdueItems = overdueItems;
                ArrayList<StockItem> finalExpiredItems = expiredItems;
                ArrayList<MissingItem> finalMissingItems = missingItems;
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.volatileItemDao().deleteVolatileItems().blockingSubscribe();
                  dlHelper.appDatabase.volatileItemDao()
                      .insertVolatileItems(volatileItemsTogether).blockingSubscribe();
                  dlHelper.appDatabase.missingItemDao().deleteMissingItems().blockingSubscribe();
                  dlHelper.appDatabase.missingItemDao()
                      .insertMissingItems(finalMissingItems).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_VOLATILE, dbChangedTime)
                      .putString(PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(finalDueItems, finalOverdueItems,
                            finalExpiredItems, finalMissingItems);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe(ignored -> {}, throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    });
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
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped Volatile download");
      }
      return null;
    }
  }

  public static void getVolatile(
      DownloadHelper dlHelper,
      Response.Listener<String> responseListener,
      Response.ErrorListener errorListener
  ) {
    dlHelper.get(
        dlHelper.grocyApi.getStockVolatile(),
        responseListener::onResponse,
        errorListener::onErrorResponse
    );
  }

  public interface OnVolatileResponseListener {

    void onResponse(
        ArrayList<StockItem> due,
        ArrayList<StockItem> overdue,
        ArrayList<StockItem> expired,
        ArrayList<MissingItem> missing
    );
  }
}
