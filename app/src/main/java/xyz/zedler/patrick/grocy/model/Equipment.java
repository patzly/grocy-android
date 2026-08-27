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
import java.util.Map;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.database.Converters;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "equipment_table")
public class Equipment extends GroupedListItem implements Parcelable {

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

  @ColumnInfo(name = "instruction_manual_file_name")
  @SerializedName("instruction_manual_file_name")
  private String instructionManualFileName;

  @ColumnInfo(name = "userfields")
  @SerializedName("userfields")
  private Map<String, String> userfields;

  @ColumnInfo(name = "row_created_timestamp")
  @SerializedName("row_created_timestamp")
  private String rowCreatedTimestamp;

  @Ignore
  @SerializedName("display_divider")
  private int displayDivider = 1;

  public Equipment() {
  }  // for Room

  public Equipment(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    instructionManualFileName = parcel.readString();
    displayDivider = parcel.readInt();
    userfields = Converters.stringToMap(parcel.readString());
    rowCreatedTimestamp = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(instructionManualFileName);
    dest.writeInt(displayDivider);
    dest.writeString(Converters.mapToString(userfields));
    dest.writeString(rowCreatedTimestamp);
  }

  public static final Creator<Equipment> CREATOR = new Creator<>() {
    @Override
    public Equipment createFromParcel(Parcel in) {
      return new Equipment(in);
    }

    @Override
    public Equipment[] newArray(int size) {
      return new Equipment[size];
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

  public String getInstructionManualFileName() {
    return instructionManualFileName;
  }

  public void setInstructionManualFileName(String instructionManualFileName) {
    this.instructionManualFileName = instructionManualFileName;
  }

  public Map<String, String> getUserfields() {
    return userfields;
  }

  public void setUserfields(Map<String, String> userfields) {
    this.userfields = userfields;
  }

  public String getRowCreatedTimestamp() {
    return rowCreatedTimestamp;
  }

  public void setRowCreatedTimestamp(String rowCreatedTimestamp) {
    this.rowCreatedTimestamp = rowCreatedTimestamp;
  }

  public int getDisplayDivider() {
    return displayDivider;
  }

  public void setDisplayDivider(int displayDivider) {
    this.displayDivider = displayDivider;
  }

  public void setDisplayDivider(boolean display) {
    displayDivider = display ? 1 : 0;
  }

  public static Equipment getEquipmentFromId(List<Equipment> equipmentList, int id) {
    for (Equipment equipment : equipmentList) {
      if (equipment.getId() == id) {
        return equipment;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Equipment equipment = (Equipment) o;
    return id == equipment.id
        && Objects.equals(name, equipment.name)
        && Objects.equals(description, equipment.description)
        && Objects.equals(instructionManualFileName, equipment.instructionManualFileName)
        && displayDivider == equipment.displayDivider;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, instructionManualFileName, displayDivider);
  }

  @NonNull
  @Override
  public String toString() {
    return "Equipment(" + name + ')';
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateEquipment(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<Equipment> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(
        Constants.PREF.DB_LAST_TIME_EQUIPMENT, null
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
              dlHelper.grocyApi.getObjects(ENTITY.EQUIPMENT),
              uuid,
              response -> {
                Type type = new TypeToken<List<Equipment>>() {}.getType();
                ArrayList<Equipment> equipment = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Equipment: " + equipment);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.equipmentDao().deleteEquipment().blockingSubscribe();
                  dlHelper.appDatabase.equipmentDao()
                      .insertEquipment(equipment).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_EQUIPMENT, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(equipment);
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
        Log.i(dlHelper.tag, "downloadData: skipped Equipment download");
      }
      return null;
    }
  }
}
