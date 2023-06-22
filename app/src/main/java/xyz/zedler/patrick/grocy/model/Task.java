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
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "task_table")
public class Task implements Parcelable {

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

  @ColumnInfo(name = "due_date")
  @SerializedName("due_date")
  private String dueDate;

  @ColumnInfo(name = "done")
  @SerializedName("done")
  private Integer done;

  @ColumnInfo(name = "done_timestamp")
  @SerializedName("done_timestamp")
  private String doneTimeStamp;

  @ColumnInfo(name = "category_id")
  @SerializedName("category_id")
  private String categoryId;

  @ColumnInfo(name = "assigned_to_user_id")
  @SerializedName("assigned_to_user_id")
  private String assignedToUserId;

  public Task() {
  }  // for Room

  @Ignore
  public Task(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    dueDate = parcel.readString();
    done = parcel.readInt();
    doneTimeStamp = parcel.readString();
    categoryId = parcel.readString();
    assignedToUserId = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(dueDate);
    dest.writeInt(done);
    dest.writeString(doneTimeStamp);
    dest.writeString(categoryId);
    dest.writeString(assignedToUserId);
  }

  public static final Creator<Task> CREATOR = new Creator<>() {

    @Override
    public Task createFromParcel(Parcel in) {
      return new Task(in);
    }

    @Override
    public Task[] newArray(int size) {
      return new Task[size];
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

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public Integer getDone() {
    return done;
  }

  public boolean isDone() {
    return done == 1;
  }

  public void setDone(Integer done) {
    this.done = done;
  }

  public void setDone(boolean done) {
    this.done = done ? 1 : 0;
  }

  public String getDoneTimeStamp() {
    return doneTimeStamp;
  }

  public void setDoneTimeStamp(String doneTimeStamp) {
    this.doneTimeStamp = doneTimeStamp;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getAssignedToUserId() {
    return assignedToUserId;
  }

  public void setAssignedToUserId(String assignedToUserId) {
    this.assignedToUserId = assignedToUserId;
  }

  public static JSONObject getJsonFromTask(Task task, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object name = task.name;
      Object description = task.description != null ? task.description : "";
      Object dueDate = task.dueDate != null ? task.dueDate : "";
      Object done = task.done != null ? String.valueOf(task.done) : "0";
      Object categoryId = task.categoryId != null ? task.categoryId : "";
      Object assignedToUserId = task.assignedToUserId != null ? task.assignedToUserId : "";

      json.put("name", name);
      json.put("description", description);
      json.put("due_date", dueDate);
      json.put("done", done);
      json.put("category_id", categoryId);
      json.put("assigned_to_user_id", assignedToUserId);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromTask: " + e);
      }
    }
    return json;
  }

  public static Task getTaskFromId(List<Task> tasks, int id) {
    for (Task task : tasks) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  public static ArrayList<Task> getUndoneTasksOnly(List<Task> allTasks) {
    ArrayList<Task> activeTasksOnly = new ArrayList<>();
    for (Task task : allTasks) {
      if (!task.isDone()) {
        activeTasksOnly.add(task);
      }
    }
    return activeTasksOnly;
  }

  public static int getUndoneTasksCount(List<Task> allTasks) {
    int undoneTasks = 0;
    for (Task task : allTasks) {
      if (!task.isDone()) {
        undoneTasks++;
      }
    }
    return undoneTasks;
  }

  public static int getAssignedTasksCount(List<Task> tasks, int userId) {
    int assignedTasks = 0;
    for (Task task : tasks) {
      if (NumUtil.isStringInt(task.getAssignedToUserId())
          && Integer.parseInt(task.getAssignedToUserId()) == userId) {
        assignedTasks++;
      }
    }
    return assignedTasks;
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
    Task task = (Task) o;
    return Objects.equals(id, task.id) &&
        Objects.equals(name, task.name) &&
        Objects.equals(description, task.description) &&
        Objects.equals(dueDate, task.dueDate) &&
        Objects.equals(done, task.done) &&
        Objects.equals(doneTimeStamp, task.doneTimeStamp) &&
        Objects.equals(categoryId, task.categoryId) &&
        Objects.equals(assignedToUserId, task.assignedToUserId);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, name, description, dueDate, done, doneTimeStamp, categoryId, assignedToUserId);
  }

  @NonNull
  @Override
  public String toString() {
    return "Task(" + name + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateTasks(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<Task> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_TASKS, null
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
              dlHelper.grocyApi.getObjects(ENTITY.TASKS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Task>>() {
                }.getType();
                ArrayList<Task> tasks = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Tasks: " + tasks);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.taskDao().deleteTasks().blockingSubscribe();
                  dlHelper.appDatabase.taskDao().insertTasks(tasks).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_TASKS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(tasks);
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
        Log.i(dlHelper.tag, "downloadData: skipped Tasks download");
      }
      return null;
    }
  }
}
