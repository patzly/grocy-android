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
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "stock_entry_table")
public class StockEntry extends GroupedListItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private double amount;

  @ColumnInfo(name = "best_before_date")
  @SerializedName("best_before_date")
  private String bestBeforeDate;

  @ColumnInfo(name = "purchased_date")
  @SerializedName("purchased_date")
  private String purchasedDate;

  @ColumnInfo(name = "stock_id")
  @SerializedName("stock_id")
  private String stockId;

  @ColumnInfo(name = "price")
  @SerializedName("price")
  private String price;

  @ColumnInfo(name = "open")
  @SerializedName("open")
  private int open;

  @ColumnInfo(name = "opened_date")
  @SerializedName("opened_date")
  private String openedDate;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  @ColumnInfo(name = "location_id")
  @SerializedName("location_id")
  private String locationId;

  @ColumnInfo(name = "shopping_location_id")
  @SerializedName("shopping_location_id")
  private String shoppingLocationId;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  public StockEntry() {
  }

  @Ignore
  public StockEntry(int id, String stockId) {
    this.id = id;
    this.stockId = stockId;
  }

  private StockEntry(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readInt();
    amount = parcel.readDouble();
    bestBeforeDate = parcel.readString();
    purchasedDate = parcel.readString();
    stockId = parcel.readString();
    price = parcel.readString();
    open = parcel.readInt();
    openedDate = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
    locationId = parcel.readString();
    shoppingLocationId = parcel.readString();
    note = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(productId);
    dest.writeDouble(amount);
    dest.writeString(bestBeforeDate);
    dest.writeString(purchasedDate);
    dest.writeString(stockId);
    dest.writeString(price);
    dest.writeInt(open);
    dest.writeString(openedDate);
    dest.writeString(rowCreatedTimestamp);
    dest.writeString(locationId);
    dest.writeString(shoppingLocationId);
    dest.writeString(note);
  }

  public static final Creator<StockEntry> CREATOR = new Creator<>() {

    @Override
    public StockEntry createFromParcel(Parcel in) {
      return new StockEntry(in);
    }

    @Override
    public StockEntry[] newArray(int size) {
      return new StockEntry[size];
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

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getBestBeforeDate() {
    return bestBeforeDate;
  }

  public void setBestBeforeDate(String bestBeforeDate) {
    this.bestBeforeDate = bestBeforeDate;
  }

  public String getPurchasedDate() {
    return purchasedDate;
  }

  public void setPurchasedDate(String purchasedDate) {
    this.purchasedDate = purchasedDate;
  }

  public String getStockId() {
    return stockId;
  }

  public void setStockId(String stockId) {
    this.stockId = stockId;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public int getOpen() {
    return open;
  }

  public void setOpen(int open) {
    this.open = open;
  }

  public String getOpenedDate() {
    return openedDate;
  }

  public void setOpenedDate(String openedDate) {
    this.openedDate = openedDate;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
  }

  public String getLocationId() {
    return locationId;
  }

  public int getLocationIdInt() {
    return NumUtil.isStringInt(locationId) ? Integer.parseInt(locationId) : -1;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getShoppingLocationId() {
    return shoppingLocationId;
  }

  public int getShoppingLocationIdInt() {
    return NumUtil.isStringInt(shoppingLocationId) ? Integer.parseInt(shoppingLocationId) : -1;
  }

  public void setShoppingLocationId(String shoppingLocationId) {
    this.shoppingLocationId = shoppingLocationId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public static StockEntry getStockEntryFromId(List<StockEntry> stockEntries, String id) {
    for (StockEntry stockEntry : stockEntries) {
      if (stockEntry.getStockId().equals(id)) {
        return stockEntry;
      }
    }
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StockEntry that = (StockEntry) o;
    return id == that.id && productId == that.productId
        && Double.compare(that.amount, amount) == 0 && open == that.open
        && Objects.equals(bestBeforeDate, that.bestBeforeDate) && Objects
        .equals(purchasedDate, that.purchasedDate) && Objects.equals(stockId, that.stockId)
        && Objects.equals(price, that.price) && Objects
        .equals(openedDate, that.openedDate) && Objects
        .equals(rowCreatedTimestamp, that.rowCreatedTimestamp) && Objects
        .equals(locationId, that.locationId) && Objects
        .equals(shoppingLocationId, that.shoppingLocationId) && Objects
        .equals(note, that.note);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, productId, amount, bestBeforeDate, purchasedDate, stockId, price, open,
            openedDate,
            rowCreatedTimestamp, locationId, shoppingLocationId, note);
  }

  @NonNull
  @Override
  public String toString() {
    return "StockEntry(" + productId + ")";
  }

  public static QueueItem updateStockEntries(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<StockEntry> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_STOCK_ENTRIES, null
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
              dlHelper.grocyApi.getObjects(ENTITY.STOCK_ENTRIES),
              uuid,
              response -> {
                Type type = new TypeToken<List<StockEntry>>() {
                }.getType();
                ArrayList<StockEntry> stockEntries = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "dowload StockEntries: " + stockEntries);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.stockEntryDao().deleteStockEntries().blockingSubscribe();
                  dlHelper.appDatabase.stockEntryDao()
                      .insertStockEntries(stockEntries).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_STOCK_ENTRIES, dbChangedTime).apply();
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
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped StockEntries download");
      }
      return null;
    }
  }

  public static QueueItem getStockEntries(
      DownloadHelper dlHelper,
      int productId,
      OnObjectsResponseListener<StockEntry> onResponseListener,
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
            dlHelper.grocyApi.getStockEntriesFromProduct(productId),
            uuid,
            response -> {
              Type type = new TypeToken<ArrayList<StockEntry>>() {
              }.getType();
              ArrayList<StockEntry> stockEntries = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download StockEntries: " + stockEntries);
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

  public static QueueItem getStockEntries(
      DownloadHelper dlHelper,
      int productId,
      OnObjectsResponseListener<StockEntry> onResponseListener
  ) {
    return getStockEntries(dlHelper, productId, onResponseListener, null);
  }
}
