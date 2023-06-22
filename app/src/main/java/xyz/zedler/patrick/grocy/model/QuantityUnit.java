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
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "quantity_unit_table")
public class QuantityUnit implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "description")
  @SerializedName("description")
  private String description;

  @ColumnInfo(name = "name_plural")
  @SerializedName("name_plural")
  private String namePlural;

  @ColumnInfo(name = "plural_forms")
  @SerializedName("plural_forms")
  private String pluralForms;

  public QuantityUnit() {
  }

  @Ignore
  public QuantityUnit(int id, String name) {
    this.id = id;
    this.name = name;
  }

  @Ignore
  public QuantityUnit(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    namePlural = parcel.readString();
    pluralForms = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(namePlural);
    dest.writeString(pluralForms);
  }

  public static final Creator<QuantityUnit> CREATOR = new Creator<>() {

    @Override
    public QuantityUnit createFromParcel(Parcel in) {
      return new QuantityUnit(in);
    }

    @Override
    public QuantityUnit[] newArray(int size) {
      return new QuantityUnit[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNamePlural() {
    return namePlural == null || namePlural.isEmpty() ? name : namePlural;
  }

  public String getNamePluralCanNull() {
    return namePlural;
  }

  public void setNamePlural(String namePlural) {
    this.namePlural = namePlural;
  }

  public String getPluralForms() {
    return pluralForms;
  }

  public void setPluralForms(String pluralForms) {
    this.pluralForms = pluralForms;
  }

  public static QuantityUnit getFromId(List<QuantityUnit> quantityUnits, int quantityUnitId) {
    if (quantityUnits == null) return null;
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == quantityUnitId) {
        return quantityUnit;
      }
    }
    return null;
  }

  public static ArrayList<QuantityUnit> getQuantityUnitsForRecipePositions(
          List<QuantityUnit> quantityUnits,
          List<RecipePosition> recipePositions
  ) {
    ArrayList<QuantityUnit> result = new ArrayList<>();
    for (RecipePosition recipePosition : recipePositions) {
      QuantityUnit quantityUnit = getFromId(quantityUnits, recipePosition.getQuantityUnitId());
      if (quantityUnit != null) {
        result.add(quantityUnit);
      }
    }
    return result;
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
    QuantityUnit that = (QuantityUnit) o;
    return id == that.id &&
        Objects.equals(name, that.name) &&
        Objects.equals(description, that.description) &&
        Objects.equals(namePlural, that.namePlural) &&
        Objects.equals(pluralForms, that.pluralForms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, namePlural, pluralForms);
  }

  @NonNull
  @Override
  public String toString() {
    return "QuantityUnit(" + name + ')';
  }

  public static QueueItem getQuantityUnits(
      DownloadHelper dlHelper,
      OnObjectsResponseListener<QuantityUnit> onResponseListener,
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
            dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
            uuid,
            response -> {
              Type type = new TypeToken<List<QuantityUnit>>() {
              }.getType();
              ArrayList<QuantityUnit> quantityUnits = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download QuantityUnits: " + quantityUnits);
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

  @SuppressLint("CheckResult")
  public static QueueItem updateQuantityUnits(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<QuantityUnit> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
              uuid,
              response -> {
                Type type = new TypeToken<List<QuantityUnit>>() {
                }.getType();
                ArrayList<QuantityUnit> quantityUnits = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download QuantityUnits: " + quantityUnits);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.quantityUnitDao().deleteQuantityUnits().blockingSubscribe();
                  dlHelper.appDatabase.quantityUnitDao()
                      .insertQuantityUnits(quantityUnits).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(quantityUnits);
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
        Log.i(dlHelper.tag, "downloadData: skipped QuantityUnits download");
      }
      return null;
    }
  }
}
