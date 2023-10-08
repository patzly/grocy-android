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
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "recipe_nesting_table")
public class RecipeNesting implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "recipe_id")
  @SerializedName("recipe_id")
  private int recipeId;

  @ColumnInfo(name = "includes_recipe_id")
  @SerializedName("includes_recipe_id")
  private int includesRecipeId;

  @ColumnInfo(name = "servings")
  @SerializedName("servings")
  private double servings;

  public RecipeNesting() {
  }  // for Room

  @Ignore
  public RecipeNesting(Parcel parcel) {
    id = parcel.readInt();
    recipeId = parcel.readInt();
    includesRecipeId = parcel.readInt();
    servings = parcel.readDouble();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(recipeId);
    dest.writeInt(includesRecipeId);
    dest.writeDouble(servings);
  }

  public static final Creator<RecipeNesting> CREATOR = new Creator<>() {

    @Override
    public RecipeNesting createFromParcel(Parcel in) {
      return new RecipeNesting(in);
    }

    @Override
    public RecipeNesting[] newArray(int size) {
      return new RecipeNesting[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getRecipeId() {
    return recipeId;
  }

  public void setRecipeId(int recipeId) {
    this.recipeId = recipeId;
  }

  public int getIncludesRecipeId() {
    return includesRecipeId;
  }

  public void setIncludesRecipeId(int includesRecipeId) {
    this.includesRecipeId = includesRecipeId;
  }

  public double getServings() {
    return servings;
  }

  public void setServings(double servings) {
    this.servings = servings;
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
    RecipeNesting recipe = (RecipeNesting) o;
    return Objects.equals(id, recipe.id) &&
        Objects.equals(recipeId, recipe.recipeId) &&
        Objects.equals(includesRecipeId, recipe.includesRecipeId) &&
        Objects.equals(servings, recipe.servings);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, recipeId, includesRecipeId, servings);
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipeNesting(" + id + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateRecipeNestings(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<RecipeNesting> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_RECIPE_NESTINGS, null
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
              dlHelper.grocyApi.getRecipeNestings(),
              uuid,
              response -> {
                Type type = new TypeToken<List<RecipeNesting>>() {
                }.getType();
                ArrayList<RecipeNesting> recipeNestings = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download RecipeNestings: " + recipeNestings);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.recipeNestingDao()
                      .deleteRecipeNestings().blockingSubscribe();
                  dlHelper.appDatabase.recipeNestingDao()
                      .insertRecipeNestings(recipeNestings).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_RECIPE_NESTINGS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(recipeNestings);
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
        Log.i(dlHelper.tag, "downloadData: skipped RecipeNestings download");
      }
      return null;
    }
  }
}
