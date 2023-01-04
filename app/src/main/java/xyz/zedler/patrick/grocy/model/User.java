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

@Entity(tableName = "user_table")
public class User  implements Parcelable {
  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "username")
  @SerializedName("username")
  private String userName;

  @ColumnInfo(name = "first_name")
  @SerializedName("first_name")
  private String firstName;

  @ColumnInfo(name = "last_name")
  @SerializedName("last_name")
  private String lastName;

  @ColumnInfo(name = "display_name")
  @SerializedName("display_name")
  private String displayName;

  @ColumnInfo(name = "picture_file_name")
  @SerializedName("picture_file_name")
  private String pictureFileName;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedFilestamp;

  public User() {
  }  // for Room

  @Ignore
  public User(int id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public User(Parcel parcel) {
    id = parcel.readInt();
    userName = parcel.readString();
    firstName = parcel.readString();
    lastName = parcel.readString();
    displayName = parcel.readString();
    pictureFileName = parcel.readString();
    rowCreatedFilestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(userName);
    dest.writeString(firstName);
    dest.writeString(lastName);
    dest.writeString(displayName);
    dest.writeString(pictureFileName);
    dest.writeString(rowCreatedFilestamp);
  }

  public static final Creator<User> CREATOR = new Creator<User>() {

    @Override
    public User createFromParcel(Parcel in) {
      return new User(in);
    }

    @Override
    public User[] newArray(int size) {
      return new User[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPictureFileName() {
    return pictureFileName;
  }

  public void setPictureFileName(String pictureFileName) {
    this.pictureFileName = pictureFileName;
  }

  public String getRowCreatedFilestamp() {
    return rowCreatedFilestamp;
  }

  public void setRowCreatedFilestamp(String rowCreatedFilestamp) {
    this.rowCreatedFilestamp = rowCreatedFilestamp;
  }

  public static User getUserFromId(List<User> users, int id) {
    for (User user : users) {
      if (user.getId() == id) {
        return user;
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
    User user = (User) o;
    return id == user.id && Objects.equals(userName, user.userName) && Objects
        .equals(firstName, user.firstName) && Objects.equals(lastName, user.lastName)
        && Objects.equals(displayName, user.displayName) && Objects
        .equals(pictureFileName, user.pictureFileName) && Objects
        .equals(rowCreatedFilestamp, user.rowCreatedFilestamp);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, userName, firstName, lastName, displayName, pictureFileName, rowCreatedFilestamp);
  }

  @NonNull
  @Override
  public String toString() {
    return "User(" + userName + ')';
  }
}
