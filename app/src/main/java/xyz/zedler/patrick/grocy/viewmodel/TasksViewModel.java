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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataTasksSort;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataTasksStatus;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.repository.TasksRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
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
  private final DateUtil dateUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<Task>> filteredTasksLive;
  private final FilterChipLiveDataTasksStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataTasksSort filterChipLiveDataSort;

  private List<Task> tasks;
  private List<TaskCategory> taskCategories;
  private HashMap<Integer, TaskCategory> taskCategoriesHashMap;
  private HashMap<Integer, User> usersHashMap;

  private String searchInput;
  private int tasksDueTodayCount;
  private int tasksDueSoonCount;
  private int tasksOverdueCount;
  private final boolean debug;

  public TasksViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new TasksRepository(application);
    pluralUtil = new PluralUtil(application);
    dateUtil = new DateUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    filteredTasksLive = new MutableLiveData<>();

    filterChipLiveDataStatus = new FilterChipLiveDataTasksStatus(
        getApplication(),
        this::updateFilteredTasks
    );
    filterChipLiveDataSort = new FilterChipLiveDataTasksSort(
        getApplication(),
        this::updateFilteredTasks
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      tasks = data.getTasks();
      taskCategories = data.getTaskGroups();
      taskCategoriesHashMap = ArrayUtil.getTaskCategoriesHashMap(data.getTaskGroups());
      usersHashMap = ArrayUtil.getUsersHashMap(data.getUsers());

      tasksDueTodayCount = 0;
      tasksDueSoonCount = 0;
      tasksOverdueCount = 0;
      for (Task task : data.getTasks()) {
        if (task.isDone()) continue;
        int daysFromNow = DateUtil.getDaysFromNow(task.getDueDate());
        if (daysFromNow < 0) {
          tasksOverdueCount++;
        }
        if (daysFromNow == 0) {
          tasksDueTodayCount++;
        }
        if (daysFromNow >= 0 && daysFromNow <= 5) {
          tasksDueSoonCount++;
        }
      }

      filterChipLiveDataStatus
          .setDueTodayCount(tasksDueTodayCount)
          .setDueSoonCount(tasksDueSoonCount)
          .setOverdueCount(tasksOverdueCount)
          .emitCounts();

      updateFilteredTasks();
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        }, error -> onError(error, TAG),
        forceUpdate,
        true,
        TaskCategory.class,
        Task.class,
        User.class
    );
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
      if (!filterChipLiveDataStatus.isShowDoneTasks() && task.isDone()) {
        continue;
      }
      int daysFromNow = DateUtil.getDaysFromNow(task.getDueDate());
      if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataTasksStatus.STATUS_OVERDUE
          && daysFromNow >= 0
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataTasksStatus.STATUS_DUE_TODAY
          && daysFromNow != 0
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataTasksStatus.STATUS_DUE_SOON
          && !(daysFromNow >= 0 && daysFromNow <= 5)) {
        continue;
      }
      filteredTasks.add(task);
    }

    boolean sortAscending = filterChipLiveDataSort.isSortAscending();
    if (filterChipLiveDataSort.getSortMode().equals(FilterChipLiveDataTasksSort.SORT_DUE_DATE)) {
      SortUtil.sortTasksByDueDate(filteredTasks, sortAscending);
    } else if (filterChipLiveDataSort.getSortMode()
        .equals(FilterChipLiveDataTasksSort.SORT_CATEGORY)) {
      SortUtil.sortTasksByCategory(filteredTasks, taskCategoriesHashMap, sortAscending);
    } else {
      SortUtil.sortTasksByName(filteredTasks, sortAscending);
    }

    filteredTasksLive.setValue(filteredTasks);
  }

  public void changeTaskDoneStatus(int taskId) {
    Task task = Task.getTaskFromId(tasks, taskId);
    if (task == null) return;
    JSONObject body = new JSONObject();
    try {
      body.put("done_time", dateUtil.getCurrentDateWithTimeStr());
    } catch (JSONException e) {
      if (debug) {
        if (!task.isDone()) {
          Log.e(TAG, "completeTask: " + e);
        } else {
          Log.e(TAG, "undoTask: " + e);
        }
      }
    }
    dlHelper.postWithArray(
        !task.isDone() ? grocyApi.completeTask(task.getId()) : grocyApi.undoTask(task.getId()),
        body,
        response -> {
          String msg = getApplication().getString(
              !task.isDone() ? R.string.msg_task_completed : R.string.msg_task_not_completed
          );
          showMessage(msg);
          downloadData(false);

          if (!task.isDone()) {
            Log.i(
                TAG, "completeTask: completed " + task.getName()
            );
          } else {
            Log.i(TAG, "undoTask: undone" + task.getName());
          }
        },
        error -> {
          showNetworkErrorMessage(error);
          if (debug) {
            if (!task.isDone()) {
              Log.i(TAG, "completeTask: " + error);
            } else {
              Log.i(TAG, "undoTask: " + error);
            }
          }
        }
    );
  }

  public void deleteTask(int taskId) {
    dlHelper.delete(
        grocyApi.getObject(ENTITY.TASKS, taskId),
        response -> downloadData(false),
        this::showNetworkErrorMessage
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

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredTasks();
  }

  public String getSortMode() {
    return filterChipLiveDataSort.getSortMode();
  }

  public boolean isSortAscending() {
    return filterChipLiveDataSort.isSortAscending();
  }

  public HashMap<Integer, TaskCategory> getTaskCategoriesHashMap() {
    return taskCategoriesHashMap;
  }

  public HashMap<Integer, User> getUsersHashMap() {
    return usersHashMap;
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
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
