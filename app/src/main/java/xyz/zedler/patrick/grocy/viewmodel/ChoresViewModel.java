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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import xyz.zedler.patrick.grocy.util.Constants.PREF;
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
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<ChoreEntry>> filteredChoreEntriesLive;
  private final MutableLiveData<Integer> currentUserIdLive;
  private final FilterChipLiveDataChoresStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataAssignment filterChipLiveDataAssignment;
  private final FilterChipLiveDataTasksSort filterChipLiveDataSort;

  private List<ChoreEntry> choreEntries;
  private List<Chore> chores;
  private HashMap<Integer, User> usersHashMap;

  private DownloadHelper.Queue currentQueueLoading;
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
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new ChoresRepository(application);
    dateUtil = new DateUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
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
      chores = data.getChores();
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
        int daysFromNow = DateUtil
            .getDaysFromNowWithTime(choreEntry.getNextEstimatedExecutionTime());
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
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredChoreEntries();
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::updateFilteredChoreEntries, this::onDownloadError);
    queue.append(
        dlHelper.updateChoreEntries(dbChangedTime, choreEntries -> {
          this.choreEntries = choreEntries;

          choresDueTodayCount = 0;
          choresDueSoonCount = 0;
          choresOverdueCount = 0;
          for (ChoreEntry choreEntry : choreEntries) {
            if (choreEntry.getNextEstimatedExecutionTime() == null
                || choreEntry.getNextEstimatedExecutionTime().isEmpty()) {
              continue;
            }
            int daysFromNow = DateUtil
                .getDaysFromNowWithTime(choreEntry.getNextEstimatedExecutionTime());
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
        }), dlHelper.updateChores(dbChangedTime, chores -> {
          this.chores = chores;

        }), dlHelper.updateUsers(dbChangedTime, users -> {
          usersHashMap = ArrayUtil.getUsersHashMap(users);
          filterChipLiveDataAssignment.setUsers(users);
        })
    );

    if (queue.isEmpty()) {
      updateFilteredChoreEntries();
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
    editPrefs.putString(PREF.DB_LAST_TIME_CHORE_ENTRIES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_CHORES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_USERS, null);
    editPrefs.apply();
    downloadData();
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

      int daysFromNow = DateUtil.getDaysFromNowWithTime(choreEntry.getNextEstimatedExecutionTime());
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
      SortUtil.sortChoreEntriesByName(getApplication(), filteredChoreEntries, sortAscending);
    }

    filteredChoreEntriesLive.setValue(filteredChoreEntries);
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

  public List<Chore> getChores() {
    return chores;
  }

  public boolean hasManualScheduling(int choreId) {
    Chore chore = Chore.getFromId(chores, choreId);
    if (chore == null) return true;
    return chore.getPeriodType().equals(Chore.PERIOD_TYPE_MANUALLY);
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
