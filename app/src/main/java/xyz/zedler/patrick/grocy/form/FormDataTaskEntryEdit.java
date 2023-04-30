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

package xyz.zedler.patrick.grocy.form;

import android.app.Application;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.DateUtil;

public class FormDataTaskEntryEdit {

  private final Application application;
  private final MutableLiveData<String> nameLive;
  private final MutableLiveData<Integer> nameErrorLive;
  private final MutableLiveData<String> descriptionLive;
  private final MutableLiveData<Boolean> useMultilineDescriptionLive;
  private final MutableLiveData<String> dueDateLive;
  private final LiveData<String> dueDateTextLive;
  private final LiveData<String> dueDateTextHumanLive;
  private final MutableLiveData<TaskCategory> taskCategoryLive;
  private final LiveData<String> taskCategoryNameLive;
  private final MutableLiveData<User> userLive;
  private final LiveData<String> userNameLive;
  private boolean filledWithTaskEntry;

  public FormDataTaskEntryEdit(Application application) {
    this.application = application;
    DateUtil dateUtil = new DateUtil(application);
    nameLive = new MutableLiveData<>();
    nameErrorLive = new MutableLiveData<>();
    descriptionLive = new MutableLiveData<>();
    useMultilineDescriptionLive = new MutableLiveData<>(false);
    dueDateLive = new MutableLiveData<>();
    dueDateTextLive = Transformations.map(
        dueDateLive,
        date -> {
          if (date == null) {
            return getString(R.string.subtitle_none_selected);
          } else {
            return dateUtil.getLocalizedDate(date, DateUtil.FORMAT_MEDIUM);
          }
        }
    );
    dueDateTextHumanLive = Transformations.map(
        dueDateLive,
        date -> {
          if (date == null) {
            return null;
          }
          return dateUtil.getHumanForDaysFromNow(date);
        }
    );
    dueDateLive.setValue(null);
    taskCategoryLive = new MutableLiveData<>();
    taskCategoryNameLive = Transformations.map(
        taskCategoryLive,
        taskCategory -> taskCategory != null ? taskCategory.getName() : null
    );
    userLive = new MutableLiveData<>();
    userNameLive = Transformations.map(
        userLive,
        user -> user != null ? user.getDisplayName() : null
    );
    filledWithTaskEntry = false;
  }

  public MutableLiveData<String> getNameLive() {
    return nameLive;
  }

  public MutableLiveData<Integer> getNameErrorLive() {
    return nameErrorLive;
  }

  public MutableLiveData<String> getDescriptionLive() {
    return descriptionLive;
  }

  public MutableLiveData<Boolean> getUseMultilineDescriptionLive() {
    return useMultilineDescriptionLive;
  }

  public void setUseMultilineDescriptionLive(boolean useMultiline) {
    useMultilineDescriptionLive.setValue(useMultiline);
  }

  public MutableLiveData<String> getDueDateLive() {
    return dueDateLive;
  }

  public void deleteDueDate() {
    dueDateLive.setValue(null);
  }

  public LiveData<String> getDueDateTextLive() {
    return dueDateTextLive;
  }

  public LiveData<String> getDueDateTextHumanLive() {
    return dueDateTextHumanLive;
  }

  public MutableLiveData<TaskCategory> getTaskCategoryLive() {
    return taskCategoryLive;
  }

  public LiveData<String> getTaskCategoryNameLive() {
    return taskCategoryNameLive;
  }

  public MutableLiveData<User> getUserLive() {
    return userLive;
  }

  public LiveData<String> getUserNameLive() {
    return userNameLive;
  }

  public boolean isFilledWithTaskEntry() {
    return filledWithTaskEntry;
  }

  public void setFilledWithTaskEntry(boolean filled) {
    this.filledWithTaskEntry = filled;
  }

  public boolean isNameValid() {
    if (nameLive.getValue() == null || nameLive.getValue().isEmpty()) {
      nameErrorLive.setValue(R.string.error_empty);
      return false;
    }
    nameErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    return isNameValid();
  }

  public Task fillTaskEntry(@Nullable Task taskEntry) {
    if (!isFormValid()) {
      return null;
    }

    if (taskEntry == null) {
      taskEntry = new Task();
      taskEntry.setDone(false);
    }
    taskEntry.setName(nameLive.getValue());
    taskEntry.setDescription(descriptionLive.getValue());
    taskEntry.setDueDate(dueDateLive.getValue());
    taskEntry.setCategoryId(taskCategoryLive.getValue() != null
        ? String.valueOf(taskCategoryLive.getValue().getId()) : null);
    taskEntry.setAssignedToUserId(userLive.getValue() != null
        ? String.valueOf(userLive.getValue().getId()) : null);
    return taskEntry;
  }

  public void clearForm() {
    nameLive.setValue(null);
    descriptionLive.setValue(null);
    dueDateLive.setValue(null);
    taskCategoryLive.setValue(null);
    userLive.setValue(null);
    new Handler().postDelayed(() -> nameErrorLive.setValue(null), 50);
  }

  private String getString(@StringRes int res) {
    return application.getString(res);
  }
}
