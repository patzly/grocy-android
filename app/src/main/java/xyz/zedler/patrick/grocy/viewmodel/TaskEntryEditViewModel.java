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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.TaskEntryEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DateBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TaskCategoriesBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.UsersBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FormDataTaskEntryEdit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.repository.TaskEntryEditRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class TaskEntryEditViewModel extends BaseViewModel {

  private static final String TAG = TaskEntryEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final TaskEntryEditRepository repository;
  private final FormDataTaskEntryEdit formData;
  private final TaskEntryEditFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private List<TaskCategory> taskCategories;
  private List<User> users;

  private DownloadHelper.Queue currentQueueLoading;
  private Runnable queueEmptyAction;
  private final boolean debug;
  private final boolean isActionEdit;

  public TaskEntryEditViewModel(
      @NonNull Application application,
      @NonNull TaskEntryEditFragmentArgs startupArgs
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new TaskEntryEditRepository(application);
    formData = new FormDataTaskEntryEdit(application);
    args = startupArgs;
    isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
  }

  public FormDataTaskEntryEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.taskCategories = data.getTaskCategories();
      this.users = data.getUsers();
      fillWithTaskEntryIfNecessary();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(
        dlHelper.updateTaskCategories(dbChangedTime, taskCategories -> {
          this.taskCategories = taskCategories;
        }), dlHelper.updateUsers(dbChangedTime, users -> this.users = users
        )
    );
    if (queue.isEmpty()) {
      onQueueEmpty();
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_TASK_CATEGORIES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_USERS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
      return;
    }
    fillWithTaskEntryIfNecessary();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void saveEntry() {
    if (!formData.isFormValid()) {
      showMessage(R.string.error_missing_information);
      return;
    }

    Task entry = null;
    if (isActionEdit) {
      entry = args.getTaskEntry();
    }
    entry = formData.fillTaskEntry(entry);
    JSONObject jsonObject = Task.getJsonFromTask(entry, debug, TAG);

    if (isActionEdit) {
      dlHelper.put(
          grocyApi.getObject(ENTITY.TASKS, entry.getId()),
          jsonObject,
          response -> navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveEntry: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(ENTITY.TASKS),
          jsonObject,
          response -> navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveEntry: " + error);
            }
          }
      );
    }
  }

  private void fillWithTaskEntryIfNecessary() {
    if (!isActionEdit || formData.isFilledWithTaskEntry()) {
      return;
    }

    Task entry = args.getTaskEntry();
    assert entry != null;

    formData.getNameLive().setValue(entry.getName());
    formData.getDescriptionLive().setValue(entry.getDescription());
    formData.getDueDateLive().setValue(entry.getDueDate() != null && !entry.getDueDate().isEmpty()
        ? entry.getDueDate() : null);
    TaskCategory category;
    if (NumUtil.isStringInt(entry.getCategoryId())) {
      category = TaskCategory.getTaskCategoryFromId(
          taskCategories, Integer.parseInt(entry.getCategoryId())
      );
    } else {
      category = null;
    }
    formData.getTaskCategoryLive().setValue(category);
    User user;
    if (NumUtil.isStringInt(entry.getAssignedToUserId())) {
      user = User.getUserFromId(
          users, Integer.parseInt(entry.getAssignedToUserId())
      );
    } else {
      user = null;
    }
    formData.getUserLive().setValue(user);
    formData.setFilledWithTaskEntry(true);
  }

  public void deleteEntry() {
    if (!isActionEdit()) {
      return;
    }
    Task task = args.getTaskEntry();
    assert task != null;
    dlHelper.delete(
        grocyApi.getObject(
            ENTITY.TASKS,
            task.getId()
        ),
        response -> navigateUp(),
        this::showErrorMessage
    );
  }

  public void showDueDateBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putString(
        Constants.ARGUMENT.SELECTED_DATE,
        formData.getDueDateLive().getValue()
    );
    bundle.putString(ARGUMENT.DEFAULT_DAYS_FROM_NOW, String.valueOf(0));
    bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.DUE_DATE);
    bundle.putBoolean(ARGUMENT.SHOW_OPTION_NEVER_EXPIRES, false);
    showBottomSheet(new DateBottomSheet(), bundle);
  }

  public void showCategoriesBottomSheet() {
    if (taskCategories == null || taskCategories.isEmpty()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.TASK_CATEGORIES, new ArrayList<>(taskCategories));
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        formData.getTaskCategoryLive().getValue() != null
            ? formData.getTaskCategoryLive().getValue().getId()
            : -1
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new TaskCategoriesBottomSheet(), bundle);
  }

  public void showUsersBottomSheet() {
    if (users == null || users.isEmpty()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.USERS, new ArrayList<>(users));
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        formData.getUserLive().getValue() != null
            ? formData.getUserLive().getValue().getId()
            : -1
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new UsersBottomSheet(), bundle);
  }

  public boolean isActionEdit() {
    return isActionEdit;
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class TaskEntryEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final TaskEntryEditFragmentArgs args;

    public TaskEntryEditViewModelFactory(
        Application application,
        TaskEntryEditFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new TaskEntryEditViewModel(application, args);
    }
  }
}
