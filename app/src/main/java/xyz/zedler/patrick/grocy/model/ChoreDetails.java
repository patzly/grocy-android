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
import androidx.room.Ignore;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Objects;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

public class ChoreDetails implements Parcelable {

  @SerializedName("chore")
  private Chore chore;

  @SerializedName("last_tracked")
  private String lastTracked;

  @SerializedName("tracked_count")
  private int trackedCount;

  @SerializedName("last_done_by")
  private User lastDoneBy;

  @SerializedName("next_estimated_execution_time")
  private String nextEstimatedExecutionTime;

  @SerializedName("next_estimated_assigned_user")
  private User nextEstimatedAssignedUser;

  @SerializedName("average_execution_frequency_hours")
  private String averageExecutionFrequencyHours;

  @Ignore
  public ChoreDetails(Parcel parcel) {
    chore = parcel.readParcelable(Chore.class.getClassLoader());
    lastTracked = parcel.readString();
    trackedCount = parcel.readInt();
    lastDoneBy = parcel.readParcelable(User.class.getClassLoader());
    nextEstimatedExecutionTime = parcel.readString();
    averageExecutionFrequencyHours = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(chore, 0);
    dest.writeString(lastTracked);
    dest.writeInt(trackedCount);
    dest.writeParcelable(lastDoneBy, 0);
    dest.writeString(nextEstimatedExecutionTime);
    dest.writeString(averageExecutionFrequencyHours);
  }

  public static final Creator<ChoreDetails> CREATOR = new Creator<>() {

    @Override
    public ChoreDetails createFromParcel(Parcel in) {
      return new ChoreDetails(in);
    }

    @Override
    public ChoreDetails[] newArray(int size) {
      return new ChoreDetails[size];
    }
  };

  public Chore getChore() {
    return chore;
  }

  public void setChore(Chore chore) {
    this.chore = chore;
  }

  public String getLastTracked() {
    return lastTracked;
  }

  public void setLastTracked(String lastTracked) {
    this.lastTracked = lastTracked;
  }

  public int getTrackedCount() {
    return trackedCount;
  }

  public void setTrackedCount(int trackedCount) {
    this.trackedCount = trackedCount;
  }

  public User getLastDoneBy() {
    return lastDoneBy;
  }

  public void setLastDoneBy(User lastDoneBy) {
    this.lastDoneBy = lastDoneBy;
  }

  public String getNextEstimatedExecutionTime() {
    return nextEstimatedExecutionTime;
  }

  public void setNextEstimatedExecutionTime(String nextEstimatedExecutionTime) {
    this.nextEstimatedExecutionTime = nextEstimatedExecutionTime;
  }

  public User getNextEstimatedAssignedUser() {
    return nextEstimatedAssignedUser;
  }

  public void setNextEstimatedAssignedUser(User nextEstimatedAssignedUser) {
    this.nextEstimatedAssignedUser = nextEstimatedAssignedUser;
  }

  public String getAverageExecutionFrequencyHours() {
    return averageExecutionFrequencyHours;
  }

  public void setAverageExecutionFrequencyHours(String averageExecutionFrequencyHours) {
    this.averageExecutionFrequencyHours = averageExecutionFrequencyHours;
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
    ChoreDetails that = (ChoreDetails) o;
    return trackedCount == that.trackedCount && Objects.equals(chore, that.chore)
        && Objects.equals(lastTracked, that.lastTracked) && Objects
        .equals(lastDoneBy, that.lastDoneBy) && Objects
        .equals(nextEstimatedExecutionTime, that.nextEstimatedExecutionTime) && Objects
        .equals(nextEstimatedAssignedUser, that.nextEstimatedAssignedUser) && Objects
        .equals(averageExecutionFrequencyHours, that.averageExecutionFrequencyHours);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chore, lastTracked, trackedCount, lastDoneBy, nextEstimatedExecutionTime,
        nextEstimatedAssignedUser, averageExecutionFrequencyHours);
  }

  @NonNull
  @Override
  public String toString() {
    return "ChoreDetails(" + chore.getName() + ")";
  }

  public static QueueItem getChoreDetails(
      DownloadHelper dlHelper,
      int choreId,
      OnObjectResponseListener<ChoreDetails> onResponseListener,
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
            dlHelper.grocyApi.getChores(choreId),
            uuid,
            response -> {
              Type type = new TypeToken<ChoreDetails>() {
              }.getType();
              ChoreDetails choreDetails = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "download ChoreDetails: " + choreDetails);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(choreDetails);
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

  public static QueueItem getChoreDetails(
      DownloadHelper dlHelper,
      int choreId,
      OnObjectResponseListener<ChoreDetails> onResponseListener
  ) {
    return getChoreDetails(dlHelper, choreId, onResponseListener, null);
  }
}
