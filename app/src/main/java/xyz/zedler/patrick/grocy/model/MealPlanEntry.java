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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
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
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;

@Entity(tableName = "meal_plan_entry_table")
public class MealPlanEntry implements Parcelable {

  public final static String TYPE_RECIPE = "recipe";
  public final static String TYPE_PRODUCT = "product";
  public final static String TYPE_NOTE = "note";

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "day")
  @SerializedName("day")
  private String day;

  @ColumnInfo(name = "type")
  @SerializedName("type")
  private String type;

  @ColumnInfo(name = "recipe_id")
  @SerializedName("recipe_id")
  private String recipeId;

  @ColumnInfo(name = "recipe_servings")
  @SerializedName("recipe_servings")
  private String recipeServings;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private String productId;

  @ColumnInfo(name = "product_amount")
  @SerializedName("product_amount")
  private String productAmount;

  @ColumnInfo(name = "product_qu_id")
  @SerializedName("product_qu_id")
  private String productQuId;

  @ColumnInfo(name = "done")
  @SerializedName("done")
  private String done;

  @ColumnInfo(name = "section_id")
  @SerializedName("section_id")
  private String sectionId;

  public MealPlanEntry() {
  }

  public MealPlanEntry(Parcel parcel) {
    id = parcel.readInt();
    day = parcel.readString();
    type = parcel.readString();
    recipeId = parcel.readString();
    recipeServings = parcel.readString();
    note = parcel.readString();
    productId = parcel.readString();
    productAmount = parcel.readString();
    productQuId = parcel.readString();
    done = parcel.readString();
    sectionId = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(day);
    dest.writeString(type);
    dest.writeString(recipeId);
    dest.writeString(recipeServings);
    dest.writeString(note);
    dest.writeString(productId);
    dest.writeString(productAmount);
    dest.writeString(productQuId);
    dest.writeString(done);
    dest.writeString(sectionId);
  }

  public static final Creator<MealPlanEntry> CREATOR = new Creator<>() {

    @Override
    public MealPlanEntry createFromParcel(Parcel in) {
      return new MealPlanEntry(in);
    }

    @Override
    public MealPlanEntry[] newArray(int size) {
      return new MealPlanEntry[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getDay() {
    return day;
  }

  public void setDay(String day) {
    this.day = day;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRecipeId() {
    return recipeId;
  }

  public void setRecipeId(String recipeId) {
    this.recipeId = recipeId;
  }

  public String getRecipeServings() {
    return recipeServings;
  }

  public void setRecipeServings(String recipeServings) {
    this.recipeServings = recipeServings;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getProductAmount() {
    return productAmount;
  }

  public void setProductAmount(String productAmount) {
    this.productAmount = productAmount;
  }

  public String getProductQuId() {
    return productQuId;
  }

  public void setProductQuId(String productQuId) {
    this.productQuId = productQuId;
  }

  public String getDone() {
    return done;
  }

  public void setDone(String done) {
    this.done = done;
  }

  public String getSectionId() {
    return sectionId;
  }

  public void setSectionId(String sectionId) {
    this.sectionId = sectionId;
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
    MealPlanEntry that = (MealPlanEntry) o;
    return id == that.id && Objects.equals(day, that.day) && Objects.equals(type,
        that.type) && Objects.equals(recipeId, that.recipeId) && Objects.equals(
        recipeServings, that.recipeServings) && Objects.equals(note, that.note)
        && Objects.equals(productId, that.productId) && Objects.equals(
        productAmount, that.productAmount) && Objects.equals(productQuId, that.productQuId)
        && Objects.equals(done, that.done) && Objects.equals(sectionId,
        that.sectionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, day, type, recipeId, recipeServings, note, productId, productAmount,
        productQuId, done, sectionId);
  }

  public static MealPlanEntry getFromId(List<MealPlanEntry> entries, int id) {
    for (MealPlanEntry entry : entries) {
      if (entry.getId() == id) {
        return entry;
      }
    }
    return null;
  }

  @NonNull
  @Override
  public String toString() {
    return "MealPlanEntry(" + id + ')';
  }

  public static QueueItem updateMealPlanEntries(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<MealPlanEntry> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_MEAL_PLAN_ENTRIES, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.MEAL_PLAN),
              uuid,
              response -> {
                Type type = new TypeToken<List<MealPlanEntry>>() {
                }.getType();
                ArrayList<MealPlanEntry> mealPlanEntries = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download MealPlanEntries: " + mealPlanEntries);
                }
                Single.fromCallable(() -> {
                      dlHelper.appDatabase.mealPlanEntryDao()
                          .deleteMealPlanEntries().blockingSubscribe();
                      dlHelper.appDatabase.mealPlanEntryDao()
                          .insertMealPlanEntries(mealPlanEntries).blockingSubscribe();
                      dlHelper.sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_MEAL_PLAN_ENTRIES, dbChangedTime).apply();
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
                        onResponseListener.onResponse(mealPlanEntries);
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
        Log.i(dlHelper.tag, "downloadData: skipped MealPlanEntries download");
      }
      return null;
    }
  }
}
