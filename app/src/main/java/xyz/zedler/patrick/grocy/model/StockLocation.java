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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "stock_current_location_table")
public class StockLocation implements Parcelable {

  @PrimaryKey(autoGenerate = true)
  private int autoId;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "location_id")
  @SerializedName("location_id")
  private int locationId;

  @ColumnInfo(name = "location_name")
  @SerializedName("location_name")
  private String locationName;

  @ColumnInfo(name = "location_is_freezer")
  @SerializedName("location_is_freezer")
  private String isFreezer;

  public StockLocation() {
  }  // for room

  private StockLocation(Parcel parcel) {
    autoId = parcel.readInt();
    productId = parcel.readInt();
    amount = parcel.readString();
    locationId = parcel.readInt();
    locationName = parcel.readString();
    isFreezer = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(autoId);
    dest.writeInt(productId);
    dest.writeString(amount);
    dest.writeInt(locationId);
    dest.writeString(locationName);
    dest.writeString(isFreezer);
  }

  public static final Creator<StockLocation> CREATOR = new Creator<>() {

    @Override
    public StockLocation createFromParcel(Parcel in) {
      return new StockLocation(in);
    }

    @Override
    public StockLocation[] newArray(int size) {
      return new StockLocation[size];
    }
  };

  public int getAutoId() {
    return autoId;
  }

  public void setAutoId(int id) {
    this.autoId = id;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getAmount() {
    return amount;
  }

  public double getAmountDouble() {
    return NumUtil.isStringDouble(amount) ? NumUtil.toDouble(amount) : 0;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public int getLocationId() {
    return locationId;
  }

  public void setLocationId(int locationId) {
    this.locationId = locationId;
  }

  public String getLocationName() {
    return locationName;
  }

  public void setLocationName(String locationName) {
    this.locationName = locationName;
  }

  public String getIsFreezer() {
    return isFreezer;
  }

  public boolean getIsFreezerBoolean() {
    return NumUtil.isStringInt(isFreezer) && Integer.parseInt(isFreezer) == 1;
  }

  public void setIsFreezer(String isFreezer) {
    this.isFreezer = isFreezer;
  }

  public static StockLocation getFromId(List<StockLocation> locations, int locationId) {
    for (StockLocation stockLocation : locations) {
      if (stockLocation.getLocationId() == locationId) {
        return stockLocation;
      }
    }
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "StockLocation(" + locationName + ')';
  }

  public static QueueItem getStockLocations(
      DownloadHelper dlHelper,
      int productId,
      OnObjectsResponseListener<StockLocation> onResponseListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnMultiTypeErrorListener errorListener,
          @Nullable String uuid
      ) {
        dlHelper.get(
            dlHelper.grocyApi.getStockLocationsFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockLocation>>() {
              }.getType();
              ArrayList<StockLocation> stockLocations = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download StockLocations: " + stockLocations);
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

  public static QueueItem getStockLocations(
      DownloadHelper dlHelper,
      int productId,
      OnObjectsResponseListener<StockLocation> onResponseListener
  ) {
    return getStockLocations(dlHelper, productId, onResponseListener, null);
  }

  public static QueueItem updateStockCurrentLocations(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<StockLocation> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_STOCK_LOCATIONS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          dlHelper.get(
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.STOCK_CURRENT_LOCATIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<StockLocation>>() {
                }.getType();
                ArrayList<StockLocation> locations = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download StockCurrentLocations: " + locations);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.stockLocationDao()
                      .deleteStockLocations().blockingSubscribe();
                  dlHelper.appDatabase.stockLocationDao()
                      .insertStockLocations(locations).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_STOCK_LOCATIONS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    })
                    .doFinally(() -> {
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
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped StockCurrentLocations download");
      }
      return null;
    }
  }
}
