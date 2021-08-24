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

package xyz.zedler.patrick.grocy.viewmodel;

import static xyz.zedler.patrick.grocy.model.HorizontalFilterBarMultiTasks.TASK_CATEGORY;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMultiTasks;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingleTasks;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.repository.TasksRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class TasksViewModel extends BaseViewModel {

  private final static String TAG = TasksViewModel.class.getSimpleName();
  public final static String SORT_NAME = "sort_name";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final TasksRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<Task>> filteredTasksLive;
    private final MutableLiveData<ArrayList<TaskCategory>> taskCategoriesLive;
  private final MutableLiveData<Boolean> scannerVisibilityLive;

  private ArrayList<Task> tasks;
  private HashMap<Integer, Task> taskHashMap;
  private ArrayList<Task> doneTasksTemp;
  private ArrayList<Task> notDoneTasksTemp;

  private DownloadHelper.Queue currentQueueLoading;
  private String searchInput;
  private String sortMode;
  private final HorizontalFilterBarSingleTasks horizontalFilterBarSingleTasks;
  private final HorizontalFilterBarMultiTasks HorizontalFilterBarMultiTasks;
  private int tasksNotDoneCount;
  private int tasksDoneCount;
  private boolean sortAscending;
  private final boolean debug;

  public TasksViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new TasksRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredTasksLive = new MutableLiveData<>();
    taskCategoriesLive = new MutableLiveData<>();
    scannerVisibilityLive = new MutableLiveData<>(false);

    horizontalFilterBarSingleTasks = new HorizontalFilterBarSingleTasks(
        this::updateFilteredTasks,
        HorizontalFilterBarSingleTasks.DONE,
        HorizontalFilterBarSingleTasks.NOT_DONE,
        HorizontalFilterBarSingleTasks.DUE,
        HorizontalFilterBarSingleTasks.OVERDUE
    );
    tasksDoneCount = 0;
    tasksNotDoneCount = 0;
    HorizontalFilterBarMultiTasks = new HorizontalFilterBarMultiTasks(
        this::updateFilteredTasks
    );
    sortMode = sharedPrefs.getString(Constants.PREF.STOCK_SORT_MODE, SORT_NAME);
    sortAscending = sharedPrefs.getBoolean(Constants.PREF.STOCK_SORT_ASCENDING, true);
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(
        (taskCategories, tasks) -> {
          this.taskCategoriesLive.setValue(taskCategories);
          this.tasks = tasks;
          taskHashMap = new HashMap<>();
          for (Task task : tasks) {
            taskHashMap.put(task.getId(), task);
          }

          tasksDoneCount = 0;
          tasksNotDoneCount = 0;
          for (Task task : tasks) {
            if (task.isDone()) {
              tasksDoneCount++;
            } else {
              tasksNotDoneCount++;
            }
          }

          updateFilteredTasks();
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredTasks();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.OnQueueEmptyListener onQueueEmptyListener = () -> {
      if (doneTasksTemp == null || notDoneTasksTemp == null) {
        downloadDataForceUpdate();
        return;
      }

      HashMap<Integer, Task> taskHashMap = new HashMap<>();
      for (Task task : tasks) {
        taskHashMap.put(task.getId(), task);
      }

      for (Task taskDone : doneTasksTemp) {
        Task task = taskHashMap.get(taskDone.getId());
        if (task == null) {
          continue;
        }
      }
      for (Task taskNotDone : notDoneTasksTemp) {
        Task task = taskHashMap.get(taskNotDone.getId());
        if (task == null) {
          continue;
        }
      }
    };

    sharedPrefs.edit().putString(Constants.PREF.DB_LAST_TIME_VOLATILE, null).apply();

    DownloadHelper.Queue queue = dlHelper.newQueue(onQueueEmptyListener, this::onDownloadError);
    queue.append(
        dlHelper.updateTaskCategories(dbChangedTime, this.taskCategoriesLive::setValue),
        dlHelper.updateTasks(dbChangedTime, tasks -> {
          this.tasks = tasks;
          taskHashMap = new HashMap<>();
          for (Task task : tasks) {
            taskHashMap.put(task.getId(), task);
          }
        })
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASKS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    repository.updateDatabase(
        this.taskCategoriesLive.getValue(),
        this.tasks
    );
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

  public void updateFilteredTasks() {
    ArrayList<Task> filteredTasks = new ArrayList<>();

    for (Task task : this.tasks) {

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = task.getName().toLowerCase().contains(searchInput);
      }
      if (!searchContainsItem) {
        continue;
      }

      if (HorizontalFilterBarMultiTasks.areFiltersActive()) {
        HorizontalFilterBarMultiTasks.Filter taskGroup = HorizontalFilterBarMultiTasks
            .getFilter(TASK_CATEGORY);
        if (taskGroup != null && NumUtil.isStringInt(task.getCategoryId())
            && taskGroup.getObjectId() != Integer
            .parseInt(task.getCategoryId())) {
          continue;
        }
      }

      if (horizontalFilterBarSingleTasks.isNoFilterActive()
          || horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.DONE)
          && task.isDone()
          || horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.NOT_DONE)
          && !task.isDone()
          || horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.DUE)
          && !task.isDue()
          || horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.OVERDUE)
          && !task.isOverdue()
      ) {
        filteredTasks.add(task);
      }
    }

    switch (sortMode) {
      case SORT_NAME:
        SortUtil.sortTasksByName(getApplication(), filteredTasks, sortAscending);
        break;
    }

    filteredTasksLive.setValue(filteredTasks);
  }

  public void performAction(String action, Task task) {
    switch (action) {
      case Constants.ACTION.COMPLETE:
        markTask(task, true);
        break;
      case Constants.ACTION.UNDO:
        markTask(task, false);
        break;
    }
  }

  private void markTask(Task task, boolean completed) {
    // TODO Not just the done_time?
    LocalDateTime done_time = LocalDateTime.now();
    JSONObject body = new JSONObject();
    try {
      body.put("done_time", done_time);
    } catch (JSONException e) {
      if (debug) {
        if (completed) {
          Log.e(TAG, "completeTask: " + e);
        } else {
          Log.e(TAG, "undoTask: " + e);
        }
      }
    }
    dlHelper.postWithArray(
        completed ? grocyApi.completeTask(task.getId()) : grocyApi.undoTask(task.getId()),
        body,
        response -> {
          String transactionId = null;
          try {
            transactionId = response.getJSONObject(0)
                .getString("transaction_id");
          } catch (JSONException e) {
            if (completed) {
              Log.e(TAG, "completeTask: " + e);
            } else {
              Log.e(TAG, "undoTask: " + e);
            }
          }

          String msg = getApplication().getString(
              completed ? R.string.msg_completed : R.string.msg_undo, task.getName()
          );
          SnackbarMessage snackbarMsg = new SnackbarMessage(msg, 15);

          downloadData();
          showSnackbar(snackbarMsg);
          if (completed) {
            Log.i(
                TAG, "completeTask: completed " + task.getName()
            );
          } else {
            Log.i(TAG, "undoTask: undone" + task.getName());
          }
        },
        error -> {
          showErrorMessage();
          if (debug) {
            if (completed) {
              Log.i(TAG, "completeTask: " + error);
            } else {
              Log.i(TAG, "undoTask: " + error);
            }
          }
        }
    );
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<Task>> getFilteredTasksLive() {
    return filteredTasksLive;
  }

  public int getTasksDoneCount() {
    return tasksDoneCount;
  }

  public int getTasksNotDoneCount() {
    return tasksNotDoneCount;
  }

  public int getTasksDueCount() {
    // TODO
    return 0;
  }

  public int getTasksOverdueCount(){
    // TODO
    return 0;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredTasks();
  }

  public HorizontalFilterBarSingleTasks getHorizontalFilterBarTasksSingle() {
    return horizontalFilterBarSingleTasks;
  }

  public HorizontalFilterBarMultiTasks getHorizontalFilterBarTasksMulti() {
    return HorizontalFilterBarMultiTasks;
  }

  public String getSortMode() {
    return sortMode;
  }

  public void setSortMode(String sortMode) {
    this.sortMode = sortMode;
    sharedPrefs.edit().putString(Constants.PREF.STOCK_SORT_MODE, sortMode).apply();
    updateFilteredTasks();
  }

  public boolean isSortAscending() {
    return sortAscending;
  }

  public void setSortAscending(boolean sortAscending) {
    this.sortAscending = sortAscending;
    sharedPrefs.edit().putBoolean(Constants.PREF.STOCK_SORT_ASCENDING, sortAscending).apply();
    updateFilteredTasks();
  }

  public MutableLiveData<ArrayList<TaskCategory>> getTaskCategoriesLive() {
    return taskCategoriesLive;
  }

  public HashMap<Integer, Task> getTaskHashMap() {
    return taskHashMap;
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
}
