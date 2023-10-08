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
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "chore_table")
public class Chore implements Parcelable {

  public final static String PERIOD_TYPE_HOURLY = "hourly";
  public final static String PERIOD_TYPE_WEEKLY = "weekly";
  public final static String PERIOD_TYPE_MANUALLY = "manually";

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

  @ColumnInfo(name = "period_type")
  @SerializedName("period_type")
  private String periodType;

  @ColumnInfo(name = "period_days")
  @SerializedName("period_days")
  private String periodDays;

  @ColumnInfo(name = "period_config")
  @SerializedName("period_config")
  private String periodConfig;

  @ColumnInfo(name = "track_date_only")
  @SerializedName("track_date_only")
  private String trackDateOnly;

  @ColumnInfo(name = "rollover")
  @SerializedName("rollover")
  private String rollover;

  @ColumnInfo(name = "assignment_type")
  @SerializedName("assignment_type")
  private String assignmentType;

  @ColumnInfo(name = "assignment_config")
  @SerializedName("assignment_config")
  private String assignmentConfig;

  @ColumnInfo(name = "next_execution_assigned_to_user_id")
  @SerializedName("next_execution_assigned_to_user_id")
  private String nextExecutionAssignedToUserId;

  @ColumnInfo(name = "consume_product_on_execution")
  @SerializedName("consume_product_on_execution")
  private String consumeProductOnExecution;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private String productId;

  @ColumnInfo(name = "product_amount")
  @SerializedName("product_amount")
  private String productAmount;

  @ColumnInfo(name = "period_interval")
  @SerializedName("period_interval")
  private String periodInterval;

  @ColumnInfo(name = "active")
  @SerializedName("active")
  private String active;

  @ColumnInfo(name = "start_date")
  @SerializedName("start_date")
  private String startDate;

  @ColumnInfo(name = "rescheduled_date")
  @SerializedName("rescheduled_date")
  private String rescheduledDate;

  @ColumnInfo(name = "rescheduled_next_execution_assigned_to_user_id")
  @SerializedName("rescheduled_next_execution_assigned_to_user_id")
  private String rescheduledNextExecutionAssignedToUserId;

  public Chore() {
  }  // for Room

  @Ignore
  public Chore(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    periodType = parcel.readString();
    periodDays = parcel.readString();
    periodConfig = parcel.readString();
    trackDateOnly = parcel.readString();
    rollover = parcel.readString();
    assignmentType = parcel.readString();
    assignmentConfig = parcel.readString();
    nextExecutionAssignedToUserId = parcel.readString();
    consumeProductOnExecution = parcel.readString();
    productId = parcel.readString();
    productAmount = parcel.readString();
    periodInterval = parcel.readString();
    active = parcel.readString();
    startDate = parcel.readString();
    rescheduledDate = parcel.readString();
    rescheduledNextExecutionAssignedToUserId = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(periodType);
    dest.writeString(periodDays);
    dest.writeString(periodConfig);
    dest.writeString(trackDateOnly);
    dest.writeString(rollover);
    dest.writeString(assignmentType);
    dest.writeString(assignmentConfig);
    dest.writeString(nextExecutionAssignedToUserId);
    dest.writeString(consumeProductOnExecution);
    dest.writeString(productId);
    dest.writeString(productAmount);
    dest.writeString(periodInterval);
    dest.writeString(active);
    dest.writeString(startDate);
    dest.writeString(rescheduledDate);
    dest.writeString(rescheduledNextExecutionAssignedToUserId);
  }

  public static final Creator<Chore> CREATOR = new Creator<>() {

    @Override
    public Chore createFromParcel(Parcel in) {
      return new Chore(in);
    }

    @Override
    public Chore[] newArray(int size) {
      return new Chore[size];
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

  public String getPeriodType() {
    return periodType;
  }

  public void setPeriodType(String periodType) {
    this.periodType = periodType;
  }

  public String getPeriodDays() {
    return periodDays;
  }

  public void setPeriodDays(String periodDays) {
    this.periodDays = periodDays;
  }

  public String getPeriodConfig() {
    return periodConfig;
  }

  public void setPeriodConfig(String periodConfig) {
    this.periodConfig = periodConfig;
  }

  public String getTrackDateOnly() {
    return trackDateOnly;
  }

  public boolean getTrackDateOnlyBoolean() {
    return NumUtil.isStringInt(trackDateOnly) && Integer.parseInt(trackDateOnly) == 1;
  }

  public void setTrackDateOnly(String trackDateOnly) {
    this.trackDateOnly = trackDateOnly;
  }

  public String getRollover() {
    return rollover;
  }

  public void setRollover(String rollover) {
    this.rollover = rollover;
  }

  public String getAssignmentType() {
    return assignmentType;
  }

  public void setAssignmentType(String assignmentType) {
    this.assignmentType = assignmentType;
  }

  public String getAssignmentConfig() {
    return assignmentConfig;
  }

  public void setAssignmentConfig(String assignmentConfig) {
    this.assignmentConfig = assignmentConfig;
  }

  public String getNextExecutionAssignedToUserId() {
    return nextExecutionAssignedToUserId;
  }

  public void setNextExecutionAssignedToUserId(String nextExecutionAssignedToUserId) {
    this.nextExecutionAssignedToUserId = nextExecutionAssignedToUserId;
  }

  public String getConsumeProductOnExecution() {
    return consumeProductOnExecution;
  }

  public void setConsumeProductOnExecution(String consumeProductOnExecution) {
    this.consumeProductOnExecution = consumeProductOnExecution;
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

  public String getPeriodInterval() {
    return periodInterval;
  }

  public void setPeriodInterval(String periodInterval) {
    this.periodInterval = periodInterval;
  }

  public String getActive() {
    return active;
  }

  public void setActive(String active) {
    this.active = active;
  }

  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getRescheduledDate() {
    return rescheduledDate;
  }

  public void setRescheduledDate(String rescheduledDate) {
    this.rescheduledDate = rescheduledDate;
  }

  public String getRescheduledNextExecutionAssignedToUserId() {
    return rescheduledNextExecutionAssignedToUserId;
  }

  public void setRescheduledNextExecutionAssignedToUserId(
      String rescheduledNextExecutionAssignedToUserId) {
    this.rescheduledNextExecutionAssignedToUserId = rescheduledNextExecutionAssignedToUserId;
  }

  public static Chore getFromId(List<Chore> chores, int choreId) {
    for (Chore chore : chores) {
      if (chore.getId() == choreId) {
        return chore;
      }
    }
    return null;
  }

  public static JSONObject getJsonFromChore(Chore chore, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      json.put("name", chore.name);

    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromChore: " + e);
      }
    }
    return json;
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
    Chore chore = (Chore) o;
    return id == chore.id && Objects.equals(name, chore.name) && Objects
        .equals(description, chore.description) && Objects
        .equals(periodType, chore.periodType) && Objects
        .equals(periodDays, chore.periodDays) && Objects
        .equals(periodConfig, chore.periodConfig) && Objects
        .equals(trackDateOnly, chore.trackDateOnly) && Objects
        .equals(rollover, chore.rollover) && Objects
        .equals(assignmentType, chore.assignmentType) && Objects
        .equals(assignmentConfig, chore.assignmentConfig) && Objects
        .equals(nextExecutionAssignedToUserId, chore.nextExecutionAssignedToUserId)
        && Objects.equals(consumeProductOnExecution, chore.consumeProductOnExecution)
        && Objects.equals(productId, chore.productId) && Objects
        .equals(productAmount, chore.productAmount) && Objects
        .equals(periodInterval, chore.periodInterval) && Objects
        .equals(active, chore.active) && Objects.equals(startDate, chore.startDate)
        && Objects.equals(rescheduledDate, chore.rescheduledDate) && Objects
        .equals(rescheduledNextExecutionAssignedToUserId,
            chore.rescheduledNextExecutionAssignedToUserId);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, name, description, periodType, periodDays, periodConfig, trackDateOnly, rollover,
            assignmentType, assignmentConfig, nextExecutionAssignedToUserId,
            consumeProductOnExecution, productId, productAmount, periodInterval, active, startDate,
            rescheduledDate, rescheduledNextExecutionAssignedToUserId);
  }

  @NonNull
  @Override
  public String toString() {
    return "Chore(" + name + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateChores(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<Chore> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_CHORES, null
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
              dlHelper.grocyApi.getObjects(ENTITY.CHORES),
              uuid,
              response -> {
                Type type = new TypeToken<List<Chore>>() {
                }.getType();
                ArrayList<Chore> chores = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Chores: " + chores);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.choreDao().deleteChores().blockingSubscribe();
                  dlHelper.appDatabase.choreDao().insertChores(chores).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_CHORES, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(chores);
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
        Log.i(dlHelper.tag, "downloadData: skipped Chores download");
      }
      return null;
    }
  }
}
