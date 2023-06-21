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
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.ChoreEntry;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataAssignment;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataChoresStatus;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataTasksSort;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.repository.ChoresRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class ChoresViewModel extends BaseViewModel {

  private final static String TAG = ChoresViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ChoresRepository repository;
  private final DateUtil dateUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<ChoreEntry>> filteredChoreEntriesLive;
  private final MutableLiveData<Integer> currentUserIdLive;
  private final FilterChipLiveDataChoresStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataAssignment filterChipLiveDataAssignment;
  private final FilterChipLiveDataTasksSort filterChipLiveDataSort;

  private List<ChoreEntry> choreEntries;
  private HashMap<Integer, Chore> choreHashMap;
  private HashMap<Integer, User> usersHashMap;

  private String searchInput;
  private int choresDueTodayCount;
  private int choresDueSoonCount;
  private int choresOverdueCount;
  private final boolean debug;

  public ChoresViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new ChoresRepository(application);
    dateUtil = new DateUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    filteredChoreEntriesLive = new MutableLiveData<>();
    currentUserIdLive = new MutableLiveData<>(sharedPrefs.getInt(PREF.CURRENT_USER_ID, 1));

    filterChipLiveDataStatus = new FilterChipLiveDataChoresStatus(
        getApplication(),
        this::updateFilteredChoreEntries
    );
    filterChipLiveDataAssignment = new FilterChipLiveDataAssignment(
        getApplication(),
        this::updateFilteredChoreEntries
    );
    filterChipLiveDataSort = new FilterChipLiveDataTasksSort(
        getApplication(),
        this::updateFilteredChoreEntries
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      choreEntries = data.getChoreEntries();
      choreHashMap = ArrayUtil.getChoresHashMap(data.getChores());
      usersHashMap = ArrayUtil.getUsersHashMap(data.getUsers());
      filterChipLiveDataAssignment.setUsers(data.getUsers());

      choresDueTodayCount = 0;
      choresDueSoonCount = 0;
      choresOverdueCount = 0;
      for (ChoreEntry choreEntry : data.getChoreEntries()) {
        if (choreEntry.getNextEstimatedExecutionTime() == null
            || choreEntry.getNextEstimatedExecutionTime().isEmpty()) {
          continue;
        }
        int daysFromNow = DateUtil.getDaysFromNow(choreEntry.getNextEstimatedExecutionTime());
        if (daysFromNow < 0) {
          choresOverdueCount++;
        }
        if (daysFromNow == 0) {
          choresDueTodayCount++;
        }
        if (daysFromNow >= 0 && daysFromNow <= 5) {
          choresDueSoonCount++;
        }
      }

      filterChipLiveDataStatus
          .setDueTodayCount(choresDueTodayCount)
          .setDueSoonCount(choresDueSoonCount)
          .setOverdueCount(choresOverdueCount)
          .emitCounts();

      updateFilteredChoreEntries();
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        ChoreEntry.class,
        Chore.class,
        User.class
    );
  }

  public void updateFilteredChoreEntries() {
    ArrayList<ChoreEntry> filteredChoreEntries = new ArrayList<>();

    for (ChoreEntry choreEntry : this.choreEntries) {
      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = choreEntry.getChoreName().toLowerCase().contains(searchInput);
      }
      if (!searchContainsItem) {
        continue;
      }

      int daysFromNow = DateUtil.getDaysFromNow(choreEntry.getNextEstimatedExecutionTime());
      if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataChoresStatus.STATUS_OVERDUE
          && daysFromNow >= 0
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataChoresStatus.STATUS_DUE_TODAY
          && daysFromNow != 0
          || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataChoresStatus.STATUS_DUE_SOON
          && !(daysFromNow >= 0 && daysFromNow <= 5)) {
        if (choreEntry.getNextEstimatedExecutionTime() != null
            && !choreEntry.getNextEstimatedExecutionTime().isEmpty()) {
          continue;
        }
      }
      if (filterChipLiveDataAssignment.isActive()
          && !NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
          || filterChipLiveDataAssignment.isActive()
          && NumUtil.isStringInt(choreEntry.getNextExecutionAssignedToUserId())
          && filterChipLiveDataAssignment.getSelectedId()
          != Integer.parseInt(choreEntry.getNextExecutionAssignedToUserId())) {
        continue;
      }
      filteredChoreEntries.add(choreEntry);
    }

    boolean sortAscending = filterChipLiveDataSort.isSortAscending();
    if (filterChipLiveDataSort.getSortMode().equals(FilterChipLiveDataTasksSort.SORT_DUE_DATE)) {
      SortUtil.sortChoreEntriesByNextExecution(filteredChoreEntries, sortAscending);
    } else {
      SortUtil.sortChoreEntriesByName(filteredChoreEntries, sortAscending);
    }

    filteredChoreEntriesLive.setValue(filteredChoreEntries);
  }

  public void executeChore(int choreId, boolean skip) {
    Chore chore = choreHashMap.get(choreId);
    if (chore == null) {
      showErrorMessage();
      return;
    }
    executeChore(chore, skip);
  }

  public void executeChore(Chore chore, boolean skip) {
    JSONObject body = new JSONObject();
    try {
      body.put("skipped", skip);
      body.put("tracked_time", chore.getTrackDateOnlyBoolean()
          ? dateUtil.getCurrentDateWithoutTimeStr() : dateUtil.getCurrentDateWithTimeStr());
    } catch (JSONException e) {
      if (debug) {
        Log.i(TAG, "executeChore: " + e);
      }
      showErrorMessage();
      return;
    }
    dlHelper.post(
        grocyApi.executeChore(chore.getId()),
        body,
        response -> {
          showMessage(getApplication().getString(R.string.msg_chore_executed));
          downloadData(false);
          if (debug) {
            Log.i(TAG, "executeChore: " + response);
          }
        },
        error -> {
          showNetworkErrorMessage(error);
          if (debug) {
            Log.i(TAG, "executeChore: " + error);
          }
          downloadData(false);
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

  public MutableLiveData<ArrayList<ChoreEntry>> getFilteredChoreEntriesLive() {
    return filteredChoreEntriesLive;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataAssignment() {
    return () -> filterChipLiveDataAssignment;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredChoreEntries();
  }

  public String getSortMode() {
    return filterChipLiveDataSort.getSortMode();
  }

  public boolean isSortAscending() {
    return filterChipLiveDataSort.isSortAscending();
  }

  public HashMap<Integer, User> getUsersHashMap() {
    return usersHashMap;
  }

  public HashMap<Integer, Chore> getChoreHashMap() {
    return choreHashMap;
  }

  public boolean hasManualScheduling(int choreId) {
    Chore chore = choreHashMap.get(choreId);
    if (chore == null) return true;
    return chore.getPeriodType().equals(Chore.PERIOD_TYPE_MANUALLY);
  }

  @Override
  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
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
