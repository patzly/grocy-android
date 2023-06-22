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
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "missing_item_table")
public class MissingItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "amount_missing")
  @SerializedName("amount_missing")
  private String amountMissing;

  @ColumnInfo(name = "is_partly_in_stock")
  @SerializedName("is_partly_in_stock")
  private String isPartlyInStock;

  // for Room
  public MissingItem() {
  }

  private MissingItem(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    amountMissing = parcel.readString();
    isPartlyInStock = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(amountMissing);
    dest.writeString(isPartlyInStock);
  }

  public static final Creator<MissingItem> CREATOR = new Creator<>() {

    @Override
    public MissingItem createFromParcel(Parcel in) {
      return new MissingItem(in);
    }

    @Override
    public MissingItem[] newArray(int size) {
      return new MissingItem[size];
    }
  };

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getAmountMissingDouble() {
    if (amountMissing == null || amountMissing.isEmpty()) {
      return 0;
    } else {
      return NumUtil.toDouble(amountMissing);
    }
  }

  public String getAmountMissing() {
    return amountMissing;
  }

  public String getIsPartlyInStock() {
    return isPartlyInStock;
  }

  public boolean getIsPartlyInStockBoolean() {
    return NumUtil.isStringInt(isPartlyInStock) && Integer.parseInt(isPartlyInStock) == 1;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setAmountMissing(String amountMissing) {
    this.amountMissing = amountMissing;
  }

  public void setIsPartlyInStock(String isPartlyInStock) {
    this.isPartlyInStock = isPartlyInStock;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @NonNull
  @Override
  public String toString() {
    return "MissingItem(" + name + ')';
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateMissingItems(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<MissingItem> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_VOLATILE_MISSING, null
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
                  Log.i(dlHelper.tag, "download Volatile (only missing): success");
                }
                ArrayList<MissingItem> missingItems = new ArrayList<>();
                try {
                  JSONObject jsonObject = new JSONObject(response);
                  // Parse fourth part of volatile array: missing products
                  missingItems = dlHelper.gson.fromJson(
                      jsonObject.getJSONArray("missing_products").toString(),
                      new TypeToken<List<MissingItem>>() {
                      }.getType()
                  );
                  if (dlHelper.debug) {
                    Log.i(dlHelper.tag, "download Volatile (only missing): missing = " + missingItems);
                  }

                } catch (JSONException e) {
                  if (dlHelper.debug) {
                    Log.e(dlHelper.tag, "download Volatile (only missing): " + e);
                  }
                }
                ArrayList<MissingItem> finalMissingItems = missingItems;
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.missingItemDao().deleteMissingItems().blockingSubscribe();
                  dlHelper.appDatabase.missingItemDao()
                      .insertMissingItems(finalMissingItems).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_VOLATILE_MISSING, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(finalMissingItems);
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
        Log.i(dlHelper.tag, "downloadData: skipped MissingItems download");
      }
      return null;
    }
  }
}
