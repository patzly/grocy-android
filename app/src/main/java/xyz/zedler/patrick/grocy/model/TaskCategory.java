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
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "task_category_table")
public class TaskCategory extends GroupedListItem implements Parcelable {
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

  @Ignore
  @SerializedName("display_divider")
  private int displayDivider = 1;

  /**
   * First element in bottomSheet selection: NONE (id = null)
   */
  @Ignore
  public TaskCategory(int id, String name) {
    this.id = id;
    this.name = name;
  }

  @Ignore
  public TaskCategory(int id, String name, String description, int displayDivider) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.displayDivider = displayDivider;
  }

  public TaskCategory() {
  }  // for Room

  public TaskCategory(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    displayDivider = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeInt(displayDivider);
  }

  public static final Creator<TaskCategory> CREATOR = new Creator<>() {

    @Override
    public TaskCategory createFromParcel(Parcel in) {
      return new TaskCategory(in);
    }

    @Override
    public TaskCategory[] newArray(int size) {
      return new TaskCategory[size];
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

  public int getDisplayDivider() {
    return displayDivider;
  }

  public void setDisplayDivider(int display) {
    displayDivider = display;
  }

  public void setDisplayDivider(boolean display) {
    displayDivider = display ? 1 : 0;
  }

  public static TaskCategory getTaskCategoryFromId(List<TaskCategory> taskCategories, int id) {
    for (TaskCategory taskCategory : taskCategories) {
      if (taskCategory.getId() == id) {
        return taskCategory;
      }
    }
    return null;
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
    TaskCategory that = (TaskCategory) o;
    return id == that.id &&
        Objects.equals(name, that.name) &&
        Objects.equals(description, that.description) &&
        displayDivider == that.displayDivider;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, displayDivider);
  }

  @NonNull
  @Override
  public String toString() {
    return "TaskCategory(" + name + ')';
  }

  @NonNull
  public TaskCategory getClone() {
    return new TaskCategory(this.id, this.name, this.description, this.displayDivider);
  }
}
