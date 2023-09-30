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
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "userfield_table")
public class Userfield implements Parcelable {

  public static final String TYPE_CHECKBOX = "checkbox";
  public static final String TYPE_TEXT_SINGLE_LINE = "text-single-line";
  public static final String TYPE_TEXT_MULTI_LINE = "text-multi-line";

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "entity")
  @SerializedName("entity")
  private String entity;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "caption")
  @SerializedName("caption")
  private String caption;

  @ColumnInfo(name = "type")
  @SerializedName("type")
  private String type;

  @ColumnInfo(name = "show_as_column_in_tables")
  @SerializedName("show_as_column_in_tables")
  private String showAsColumnInTables;

  @ColumnInfo(name = "sort_number")
  @SerializedName("sort_number")
  private String sortNumber;

  @ColumnInfo(name = "input_required")
  @SerializedName("input_required")
  private String inputRequired;

  @ColumnInfo(name = "default_value")
  @SerializedName("default_value")
  private String defaultValue;

  public Userfield() {

  }

  public Userfield(Parcel parcel) {
    id = parcel.readInt();
    entity = parcel.readString();
    name = parcel.readString();
    caption = parcel.readString();
    type = parcel.readString();
    showAsColumnInTables = parcel.readString();
    sortNumber = parcel.readString();
    inputRequired = parcel.readString();
    defaultValue = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(entity);
    dest.writeString(name);
    dest.writeString(caption);
    dest.writeString(type);
    dest.writeString(showAsColumnInTables);
    dest.writeString(sortNumber);
    dest.writeString(inputRequired);
    dest.writeString(defaultValue);
  }

  public static final Creator<Userfield> CREATOR = new Creator<>() {

    @Override
    public Userfield createFromParcel(Parcel in) {
      return new Userfield(in);
    }

    @Override
    public Userfield[] newArray(int size) {
      return new Userfield[size];
    }
  };

  public static Chip fillChipWithUserfield(Chip chip, Userfield userfield, String value) {
    Context context = chip.getContext();

    if (userfield.getType().equals(Userfield.TYPE_CHECKBOX)) {
      chip.setText(context.getString(R.string.property_userfield_value_without_space,
          userfield.getCaption(), ""));
      ColorRoles colorGreen = ResUtil.getHarmonizedRoles(context, R.color.green);
      if (value != null && value.equals("1")) {
        chip.setCloseIcon(
            ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
        );
        chip.setCloseIconTint(ColorStateList.valueOf(colorGreen.getOnAccentContainer()));
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorGreen.getAccentContainer()));
        chip.setCloseIconVisible(true);
        return chip;
      } else {
        return null;
      }
    } else if (userfield.getType().equals(Userfield.TYPE_TEXT_SINGLE_LINE)
        || userfield.getType().equals(Userfield.TYPE_TEXT_MULTI_LINE)) {
      if (value == null) {
        return null;
      }
      boolean valueTooLong = value.length() > 20;
      chip.setText(context.getString(
          R.string.property_userfield_value,
          userfield.getCaption(),
          valueTooLong
              ? value.replace("\n", " ").substring(0, 20) + "..."
              : value
      ));
      if (valueTooLong) {
        chip.setCloseIcon(ContextCompat.getDrawable(context, R.drawable.ic_round_expand_more));
        chip.setCloseIconVisible(true);
        chip.setEnabled(true);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(v -> showInfoDialog(context, userfield.getCaption(), value));
      }
      return chip;
    } else {
      if (value == null) {
        return null;
      }
      chip.setChipIcon(ContextCompat.getDrawable(context, R.drawable.ic_round_error_outline));
      chip.setChipIconTint(chip.getCloseIconTint());
      chip.setTextStartPadding(12);
      boolean valueTooLong = value.length() > 20;
      chip.setText(context.getString(
          R.string.property_userfield_value,
          userfield.getCaption(),
          valueTooLong
              ? value.replace("\n", " ").substring(0, 20) + "..."
              : value
      ));
      chip.setCloseIcon(ContextCompat.getDrawable(context, R.drawable.ic_round_expand_more));
      chip.setCloseIconVisible(true);
      chip.setEnabled(true);
      chip.setClickable(true);
      chip.setFocusable(true);
      chip.setOnClickListener(v -> showInfoDialog(
          context, null, context.getString(R.string.error_userfield_type_not_supported))
      );
      return chip;
    }
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCaption() {
    return caption;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getShowAsColumnInTables() {
    return showAsColumnInTables;
  }

  public boolean getShowAsColumnInTablesBoolean() {
    return showAsColumnInTables != null && showAsColumnInTables.equals("1");
  }

  public void setShowAsColumnInTables(String showAsColumnInTables) {
    this.showAsColumnInTables = showAsColumnInTables;
  }

  public String getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(String sortNumber) {
    this.sortNumber = sortNumber;
  }

  public String getInputRequired() {
    return inputRequired;
  }

  public void setInputRequired(String inputRequired) {
    this.inputRequired = inputRequired;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
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
    Userfield userfield = (Userfield) o;
    return id == userfield.id && Objects.equals(entity, userfield.entity)
        && Objects.equals(name, userfield.name) && Objects.equals(caption,
        userfield.caption) && Objects.equals(type, userfield.type)
        && Objects.equals(showAsColumnInTables, userfield.showAsColumnInTables)
        && Objects.equals(sortNumber, userfield.sortNumber) && Objects.equals(
        inputRequired, userfield.inputRequired) && Objects.equals(defaultValue,
        userfield.defaultValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, entity, name, caption, type, showAsColumnInTables, sortNumber,
        inputRequired, defaultValue);
  }

  @NonNull
  @Override
  public String toString() {
    return "Userfield(" + name + ')';
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateUserfields(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<Userfield> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_USERFIELDS, null
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
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.USERFIELDS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Userfield>>() {
                }.getType();
                ArrayList<Userfield> userfields = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Userfields: " + userfields);
                }
                Single.fromCallable(() -> {
                      dlHelper.appDatabase.userfieldDao().deleteUserfields().blockingSubscribe();
                      dlHelper.appDatabase.userfieldDao().insertStores(userfields).blockingSubscribe();
                      dlHelper.sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_USERFIELDS, dbChangedTime).apply();
                      return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(userfields);
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
        Log.i(dlHelper.tag, "downloadData: skipped Userfields download");
      }
      return null;
    }
  }

  private static void showInfoDialog(Context context, String title, String message) {
    new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Grocy_AlertDialog)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
        .create().show();
  }
}
