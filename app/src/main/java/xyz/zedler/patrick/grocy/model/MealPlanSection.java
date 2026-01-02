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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
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
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "meal_plan_section_table")
public class MealPlanSection extends GroupedListItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "sort_number")
  @SerializedName("sort_number")
  private int sortNumber;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  @ColumnInfo(name = "time_info")
  @SerializedName("time_info")
  private String timeInfo;

  @Ignore
  @SerializedName("is_top_item")
  private boolean isTopItem;

  @Ignore
  @SerializedName("is_info_item")
  private boolean isInfoItem;

  public MealPlanSection() {
  }

  public MealPlanSection(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    sortNumber = parcel.readInt();
    rowCreatedTimestamp = parcel.readString();
    timeInfo = parcel.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeInt(sortNumber);
    dest.writeString(rowCreatedTimestamp);
    dest.writeString(timeInfo);
  }

  public static final Creator<MealPlanSection> CREATOR = new Creator<>() {

    @Override
    public MealPlanSection createFromParcel(Parcel in) {
      return new MealPlanSection(in);
    }

    @Override
    public MealPlanSection[] newArray(int size) {
      return new MealPlanSection[size];
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

  public int getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(int sortNumber) {
    this.sortNumber = sortNumber;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
  }

  public String getTimeInfo() {
    return timeInfo;
  }

  public void setTimeInfo(String timeInfo) {
    this.timeInfo = timeInfo;
  }

  public boolean isTopItem() {
    return isTopItem;
  }

  public void setTopItem(boolean topItem) {
    isTopItem = topItem;
  }

  public boolean isInfoItem() {
    return isInfoItem;
  }

  public void setInfoItem(boolean infoItem) {
    isInfoItem = infoItem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MealPlanSection section = (MealPlanSection) o;
    return id == section.id && sortNumber == section.sortNumber && isTopItem == section.isTopItem
        && Objects.equals(name, section.name) && Objects.equals(
        rowCreatedTimestamp, section.rowCreatedTimestamp) && Objects.equals(timeInfo,
        section.timeInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, sortNumber, rowCreatedTimestamp, timeInfo, isTopItem);
  }

  public static MealPlanSection getFromId(List<MealPlanSection> sections, int id) {
    for (MealPlanSection section : sections) {
      if (section.getId() == id) {
        return section;
      }
    }
    return null;
  }

  @NonNull
  @Override
  public String toString() {
    return "MealPlanSection(" + id + ", " + name + ')';
  }

  public static QueueItem updateMealPlanSections(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<MealPlanSection> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_MEAL_PLAN_SECTIONS, null
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
              dlHelper.grocyApi.getObjects(ENTITY.MEAL_PLAN_SECTIONS),
              uuid,
              response -> {
                Type type = new TypeToken<List<MealPlanSection>>() {
                }.getType();
                ArrayList<MealPlanSection> mealPlanSections = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download MealPlanSections: " + mealPlanSections);
                }
                Single.fromCallable(() -> {
                      dlHelper.appDatabase.mealPlanSectionDao()
                          .deleteMealPlanSections().blockingSubscribe();
                      dlHelper.appDatabase.mealPlanSectionDao()
                          .insertMealPlanSections(mealPlanSections).blockingSubscribe();
                      dlHelper.sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_MEAL_PLAN_SECTIONS, dbChangedTime).apply();
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
                        onResponseListener.onResponse(mealPlanSections);
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
        Log.i(dlHelper.tag, "downloadData: skipped MealPlanSections download");
      }
      return null;
    }
  }
}
