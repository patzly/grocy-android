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
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "product_barcode_table")
public class ProductBarcode implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private String productId;

  @ColumnInfo(name = "barcode")
  @SerializedName("barcode")
  private String barcode;

  @ColumnInfo(name = "qu_id")
  @SerializedName("qu_id")
  private String quId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private String amount;

  @ColumnInfo(name = "shopping_location_id")
  @SerializedName("shopping_location_id")
  private String storeId;

  @ColumnInfo(name = "last_price")
  @SerializedName("last_price")
  private String lastPrice;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  public ProductBarcode() {
  }

  @Ignore
  public ProductBarcode(Parcel parcel) {
    id = parcel.readInt();
    productId = parcel.readString();
    barcode = parcel.readString();
    quId = parcel.readString();
    amount = parcel.readString();
    storeId = parcel.readString();
    lastPrice = parcel.readString();
    note = parcel.readString();
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(productId);
    dest.writeString(barcode);
    dest.writeString(quId);
    dest.writeString(amount);
    dest.writeString(storeId);
    dest.writeString(lastPrice);
    dest.writeString(note);
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<ProductBarcode> CREATOR = new Creator<>() {

    @Override
    public ProductBarcode createFromParcel(Parcel in) {
      return new ProductBarcode(in);
    }

    @Override
    public ProductBarcode[] newArray(int size) {
      return new ProductBarcode[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getProductId() {
    return productId;
  }

  public int getProductIdInt() {
    return NumUtil.isStringInt(productId) ? Integer.parseInt(productId) : -1;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public void setProductIdInt(int productId) {
    this.productId = String.valueOf(productId);
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getQuId() {
    return quId;
  }

  public int getQuIdInt() {
    return hasQuId() ? Integer.parseInt(quId) : -1;
  }

  public void setQuId(String quId) {
    this.quId = quId;
  }

  public boolean hasQuId() {
    return NumUtil.isStringInt(quId);
  }

  public String getAmount() {
    return amount;
  }

  public double getAmountDouble() {
    return hasAmount() ? NumUtil.toDouble(amount) : 0;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public boolean hasAmount() {
    return NumUtil.isStringDouble(amount);
  }

  public String getStoreId() {
    return storeId;
  }

  public int getStoreIdInt() {
    return hasStoreId() ? Integer.parseInt(storeId) : -1;
  }

  public void setStoreId(String storeId) {
    this.storeId = storeId;
  }

  public boolean hasStoreId() {
    return NumUtil.isStringInt(storeId);
  }

  public String getLastPrice() {
    return lastPrice;
  }

  public double getLastPriceDouble() {
    return hasLastPrice() ? NumUtil.toDouble(lastPrice) : 0;
  }

  public void setLastPrice(String lastPrice) {
    this.lastPrice = lastPrice;
  }

  public boolean hasLastPrice() {
    return NumUtil.isStringDouble(lastPrice);
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static ProductBarcode getFromBarcode(
      List<ProductBarcode> productBarcodes,
      String barcode
  ) {
    for (ProductBarcode code : productBarcodes) {
      if (code.getBarcode().equals(barcode)) {
        return code;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProductBarcode that = (ProductBarcode) o;
    return id == that.id &&
        Objects.equals(productId, that.productId) &&
        Objects.equals(barcode, that.barcode) &&
        Objects.equals(quId, that.quId) &&
        Objects.equals(amount, that.amount) &&
        Objects.equals(storeId, that.storeId) &&
        Objects.equals(lastPrice, that.lastPrice) &&
        Objects.equals(note, that.note) &&
        Objects.equals(rowCreatedTimestamp, that.rowCreatedTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, barcode, quId, amount, storeId,
        lastPrice, note, rowCreatedTimestamp);
  }

  public static JSONObject getJsonFromProductBarcode(ProductBarcode productBarcode, boolean debug,
      String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object quId = productBarcode.getQuId() != null && !productBarcode.getQuId().isEmpty()
          ? productBarcode.getQuId() : JSONObject.NULL;
      Object note = productBarcode.getNote() != null && !productBarcode.getNote().isEmpty()
          ? productBarcode.getNote() : JSONObject.NULL;
      Object lastPrice = productBarcode.getLastPrice() != null
          && !productBarcode.getLastPrice().isEmpty()
          ? productBarcode.getLastPrice() : JSONObject.NULL;
      Object storeId = productBarcode.getStoreId() != null
          ? productBarcode.getStoreId() : JSONObject.NULL;
      Object amount = productBarcode.getAmount() != null
          && !productBarcode.getAmount().isEmpty()
          ? productBarcode.getAmount() : JSONObject.NULL;
      json.put("product_id", productBarcode.getProductIdInt());
      json.put("barcode", productBarcode.getBarcode());
      json.put("qu_id", quId);
      json.put("amount", amount);
      json.put("shopping_location_id", storeId);
      json.put("last_price", lastPrice);
      json.put("note", note);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromProductBarcode: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromProductBarcode(boolean debug, String TAG) {
    return getJsonFromProductBarcode(this, debug, TAG);
  }

  @NonNull
  @Override
  public String toString() {
    return "ProductBarcode(" + id + ')';
  }

  public static QueueItem updateProductBarcodes(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<ProductBarcode> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
              uuid,
              response -> {
                Type type = new TypeToken<List<ProductBarcode>>() {
                }.getType();
                ArrayList<ProductBarcode> barcodes
                    = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Barcodes: " + barcodes);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.productBarcodeDao()
                      .deleteProductBarcodes().blockingSubscribe();
                  dlHelper.appDatabase.productBarcodeDao()
                      .insertProductBarcodes(barcodes).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, dbChangedTime).apply();
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
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped ProductsBarcodes download");
      }
      return null;
    }
  }

  public static QueueItem addProductBarcode(
      DownloadHelper dlHelper,
      JSONObject jsonObject,
      Runnable onSuccessListener,
      OnErrorListener onErrorListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnMultiTypeErrorListener errorListener,
          @Nullable String uuid
      ) {
        dlHelper.post(
            dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_BARCODES),
            jsonObject,
            response -> {
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "added ProductBarcode");
              }
              if (onSuccessListener != null) {
                onSuccessListener.run();
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

  public QueueItem getSingleFilteredProductBarcode(
      DownloadHelper dlHelper,
      String barcode,
      OnObjectResponseListener<ProductBarcode> onResponseListener,
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
            dlHelper.grocyApi.getObjectsEqualValue(
                GrocyApi.ENTITY.PRODUCT_BARCODES, "barcode", barcode
            ),
            uuid,
            response -> {
              Type type = new TypeToken<List<ProductBarcode>>() {
              }.getType();
              ArrayList<ProductBarcode> barcodes
                  = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download filtered Barcodes: " + barcodes);
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
}
