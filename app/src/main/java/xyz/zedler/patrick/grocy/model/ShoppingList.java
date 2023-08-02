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
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "shopping_list_table")
public class ShoppingList implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private final int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private final String name;

  @ColumnInfo(name = "notes")
  @SerializedName("description")
  private String notes;

  public ShoppingList(int id, String name, String notes) {
    this.id = id;
    this.name = name;
    this.notes = notes;
  }

  private ShoppingList(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    notes = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(notes);
  }

  public static final Creator<ShoppingList> CREATOR = new Creator<>() {

    @Override
    public ShoppingList createFromParcel(Parcel in) {
      return new ShoppingList(in);
    }

    @Override
    public ShoppingList[] newArray(int size) {
      return new ShoppingList[size];
    }
  };

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public static ShoppingList getFromId(List<ShoppingList> shoppingLists, int id) {
    for (ShoppingList shoppingList : shoppingLists) {
      if (shoppingList.getId() == id) {
        return shoppingList;
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
    return "ShoppingList{id=" + id + ", name='" + name + "', notes='" + notes + "'}";
  }

  public static QueueItem getShoppingLists(
      DownloadHelper dlHelper,
      OnObjectsResponseListener<ShoppingList> onResponseListener,
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
            dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
            uuid,
            response -> {
              Type type = new TypeToken<List<ShoppingList>>() {
              }.getType();
              ArrayList<ShoppingList> shoppingLists = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download ShoppingLists: " + shoppingLists);
              }
              onResponseListener.onResponse(shoppingLists);
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
  public static QueueItem updateShoppingLists(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<ShoppingList> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
              uuid,
              response -> {
                Type type = new TypeToken<List<ShoppingList>>() {
                }.getType();
                ArrayList<ShoppingList> shoppingLists = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download ShoppingLists: " + shoppingLists);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.shoppingListDao()
                      .deleteShoppingLists().blockingSubscribe();
                  dlHelper.appDatabase.shoppingListDao()
                      .insertShoppingLists(shoppingLists).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_SHOPPING_LISTS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(shoppingLists);
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
        Log.i(dlHelper.tag, "downloadData: skipped ShoppingLists download");
      }
      return null;
    }
  }
}
