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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

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

  @Ignore
  @SerializedName("category")
  private TaskCategory category;

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
    category = parcel.readParcelable(TaskCategory.class.getClassLoader());
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
    dest.writeParcelable(category, 0);
  }

  public static final Creator<Task> CREATOR = new Creator<Task>() {

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

//  public boolean isActive() {
//    return NumUtil.isStringInt(active) && Integer.parseInt(active) == 1;
//  }

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

  public TaskCategory getCategory() {
    return category;
  }

  public void setCategory(TaskCategory category) {
    this.category = category;
  }

  public static JSONObject getJsonFromTask(Task task, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object name = task.name;
      Object description = task.description != null ? task.description : JSONObject.NULL;
      Object dueDate = task.dueDate;
      Object done = task.done;
      Object doneTimeStamp = task.doneTimeStamp;
      Object categoryId = task.categoryId;

      json.put("name", name);
      json.put("description", description);
      json.put("due_date", dueDate);
      json.put("done", done);
      json.put("done_time_stamp", doneTimeStamp);
      json.put("category_id", categoryId);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromTask: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromTask(boolean debug, String TAG) {
    return getJsonFromTask(this, debug, TAG);
  }

  public static Task getTaskFromId(ArrayList<Task> tasks, int id) {
    for (Task task : tasks) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  public static Task getTaskFromName(ArrayList<Task> tasks, String name) {
    if (name == null || name.isEmpty()) return null;
    for (Task task : tasks) {
      if (task.getName() != null && task.getName().equals(name)) {
        return task;
      }
    }
    return null;
  }

  public static ArrayList<Task> getDoneTasksOnly(ArrayList<Task> allTasks) {
    ArrayList<Task> activeTasksOnly = new ArrayList<>();
    for (Task task : allTasks) {
      if (task.isDone()) {
        activeTasksOnly.add(task);
      }
    }
    return activeTasksOnly;
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
        Objects.equals(category, task.category);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, name, description, dueDate, done, doneTimeStamp, categoryId, category);
  }

  @NonNull
  @Override
  public String toString() {
    return "Task(" + name + ")";
  }
}
