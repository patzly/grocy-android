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
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnIntegerResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;

@Entity(tableName = "user_table")
public class User implements Parcelable {
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

  public static final Creator<User> CREATOR = new Creator<>() {

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

  public static QueueItem updateUsers(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<User> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_USERS, null
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
              dlHelper.grocyApi.getUsers(),
              uuid,
              response -> {
                Type type = new TypeToken<List<User>>() {
                }.getType();
                ArrayList<User> users = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Users: " + users);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.userDao().deleteUsers().blockingSubscribe();
                  dlHelper.appDatabase.userDao().insertUsers(users).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_USERS, dbChangedTime).apply();
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
                        onResponseListener.onResponse(users);
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
        Log.i(dlHelper.tag, "downloadData: skipped Users download");
      }
      return null;
    }
  }

  public static QueueItem getCurrentUserId(
      DownloadHelper dlHelper,
      OnIntegerResponseListener onResponseListener
  ) {
    return new QueueItem() {
      @Override
      public void perform(
          @Nullable OnStringResponseListener responseListener,
          @Nullable OnMultiTypeErrorListener errorListener,
          @Nullable String uuid
      ) {
        dlHelper.get(
            dlHelper.grocyApi.getUser(),
            uuid,
            response -> {
              Type type = new TypeToken<List<User>>() {
              }.getType();
              ArrayList<User> users = dlHelper.gson.fromJson(response, type);
              if (dlHelper.debug) {
                Log.i(dlHelper.tag, "get currentUserId: " + response);
              }
              if (onResponseListener != null) {
                onResponseListener.onResponse(users.size() == 1 ? users.get(0).getId() : -1);
              }
              if (responseListener != null) {
                responseListener.onResponse(response);
              }
            },
            error -> {
              if (errorListener != null) {
                errorListener.onError(error);
              }
            }
        );
      }
    };
  }
}
