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
import com.android.volley.Response;
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
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "chore_entry_table")
public class ChoreEntry implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "chore_id")
  @SerializedName("chore_id")
  private int choreId;

  @ColumnInfo(name = "chore_name")
  @SerializedName("chore_name")
  private String choreName;

  @ColumnInfo(name = "last_tracked_time")
  @SerializedName("last_tracked_time")
  private String lastTrackedTime;

  @ColumnInfo(name = "next_estimated_execution_time")
  @SerializedName("next_estimated_execution_time")
  private String nextEstimatedExecutionTime;

  @ColumnInfo(name = "track_date_only")
  @SerializedName("track_date_only")
  private String trackDateOnly;

  @ColumnInfo(name = "next_execution_assigned_to_user_id")
  @SerializedName("next_execution_assigned_to_user_id")
  private String nextExecutionAssignedToUserId;

  public ChoreEntry() {
  }  // for Room

  @Ignore
  public ChoreEntry(Parcel parcel) {
    id = parcel.readInt();
    choreId = parcel.readInt();
    choreName = parcel.readString();
    lastTrackedTime = parcel.readString();
    nextEstimatedExecutionTime = parcel.readString();
    trackDateOnly = parcel.readString();
    nextExecutionAssignedToUserId = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(choreId);
    dest.writeString(choreName);
    dest.writeString(lastTrackedTime);
    dest.writeString(nextEstimatedExecutionTime);
    dest.writeString(trackDateOnly);
    dest.writeString(nextExecutionAssignedToUserId);
  }

  public static final Creator<ChoreEntry> CREATOR = new Creator<>() {

    @Override
    public ChoreEntry createFromParcel(Parcel in) {
      return new ChoreEntry(in);
    }

    @Override
    public ChoreEntry[] newArray(int size) {
      return new ChoreEntry[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getChoreId() {
    return choreId;
  }

  public void setChoreId(int choreId) {
    this.choreId = choreId;
  }

  public String getChoreName() {
    return choreName;
  }

  public void setChoreName(String choreName) {
    this.choreName = choreName;
  }

  public String getLastTrackedTime() {
    return lastTrackedTime;
  }

  public void setLastTrackedTime(String lastTrackedTime) {
    this.lastTrackedTime = lastTrackedTime;
  }

  public String getNextEstimatedExecutionTime() {
    return nextEstimatedExecutionTime;
  }

  public void setNextEstimatedExecutionTime(String nextEstimatedExecutionTime) {
    this.nextEstimatedExecutionTime = nextEstimatedExecutionTime;
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

  public String getNextExecutionAssignedToUserId() {
    return nextExecutionAssignedToUserId;
  }

  public void setNextExecutionAssignedToUserId(String nextExecutionAssignedToUserId) {
    this.nextExecutionAssignedToUserId = nextExecutionAssignedToUserId;
  }

  public static JSONObject getJsonFromChore(ChoreEntry chore, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      json.put("name", chore.choreName);

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
    ChoreEntry that = (ChoreEntry) o;
    return id == that.id && choreId == that.choreId && Objects
        .equals(choreName, that.choreName) && Objects
        .equals(lastTrackedTime, that.lastTrackedTime) && Objects
        .equals(nextEstimatedExecutionTime, that.nextEstimatedExecutionTime) && Objects
        .equals(trackDateOnly, that.trackDateOnly) && Objects
        .equals(nextExecutionAssignedToUserId, that.nextExecutionAssignedToUserId);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, choreId, choreName, lastTrackedTime, nextEstimatedExecutionTime, trackDateOnly,
            nextExecutionAssignedToUserId);
  }

  @NonNull
  @Override
  public String toString() {
    return "ChoreEntry(" + choreName + ")";
  }

  public static void getChoreEntries(
      DownloadHelper dlHelper,
      OnObjectsResponseListener<ChoreEntry> onResponseListener,
      Response.ErrorListener errorListener
  ) {
    dlHelper.get(
        dlHelper.grocyApi.getChores(),
        response -> {
          Type type = new TypeToken<List<ChoreEntry>>() {
          }.getType();
          ArrayList<ChoreEntry> choreEntries = dlHelper.gson.fromJson(response, type);
          if (dlHelper.debug) {
            Log.i(dlHelper.tag, "getChoreEntries: " + choreEntries);
          }
          onResponseListener.onResponse(choreEntries);
        },
        errorListener::onErrorResponse
    );
  }

  public static QueueItem updateChoreEntries(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<ChoreEntry> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_CHORE_ENTRIES, null
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
              dlHelper.grocyApi.getChores(),
              uuid,
              response -> {
                Type type = new TypeToken<List<ChoreEntry>>() {
                }.getType();
                ArrayList<ChoreEntry> choreEntries = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download ChoreEntries: " + choreEntries);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.choreEntryDao()
                      .deleteChoreEntries().blockingSubscribe();
                  dlHelper.appDatabase.choreEntryDao()
                      .insertChoreEntries(choreEntries).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_CHORE_ENTRIES, dbChangedTime).apply();
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
                        onResponseListener.onResponse(choreEntries);
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
        Log.i(dlHelper.tag, "downloadData: skipped Chores download");
      }
      return null;
    }
  }
}
