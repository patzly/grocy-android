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
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.UsersBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.repository.ChoresRepository;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class ChoreEntryRescheduleViewModel extends BaseViewModel {

  private static final String TAG = ChoreEntryRescheduleViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final ChoresRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private final MutableLiveData<String> nextTrackingDateLive;
  private final LiveData<String> nextTrackingDateTextLive;
  private final LiveData<String> nextTrackingDateHumanTextLive;
  private final MutableLiveData<String> nextTrackingTimeLive;
  private final LiveData<String> nextTrackingTimeHumanTextLive;
  private final MutableLiveData<User> userLive;
  private final LiveData<String> userTextLive;

  private List<User> users;

  private NetworkQueue currentQueueLoading;
  private final Chore chore;
  private final boolean debug;

  public ChoreEntryRescheduleViewModel(@NonNull Application application, @NonNull Chore chore) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    DateUtil dateUtil = new DateUtil(application);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new ChoresRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);

    nextTrackingDateLive = new MutableLiveData<>(chore.getRescheduledDate() != null
        && !chore.getRescheduledDate().isEmpty() ? chore.getRescheduledDate() : null);
    nextTrackingDateTextLive = Transformations.map(
        nextTrackingDateLive,
        date -> date != null
            ? dateUtil.getLocalizedDate(date, DateUtil.FORMAT_MEDIUM)
            : getString(R.string.subtitle_none_selected)
    );
    nextTrackingDateHumanTextLive = Transformations.map(
        nextTrackingDateLive,
        date -> date != null ? dateUtil.getHumanForDaysFromNow(date) : null
    );
    nextTrackingTimeLive = new MutableLiveData<>(chore.getRescheduledDate() != null
        && chore.getRescheduledDate().split(" ").length == 2
        ? chore.getRescheduledDate().split(" ")[1]
        : null);
    nextTrackingTimeHumanTextLive = Transformations.map(
        nextTrackingTimeLive,
        time -> time != null ? dateUtil.getLocalizedTime(time) : null
    );
    userLive = new MutableLiveData<>();
    userTextLive = Transformations.map(
        userLive,
        user -> user != null && user.getId() != -1
            ? user.getUserName()
            : getString(R.string.subtitle_none_selected)
    );
    userLive.setValue(null);
    this.chore = chore;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(
        data -> {
          this.users = data.getUsers();
          if (userLive.getValue() == null
              && NumUtil.isStringInt(chore.getRescheduledNextExecutionAssignedToUserId())) {
            userLive.setValue(User.getUserFromId(
                data.getUsers(),
                Integer.parseInt(chore.getRescheduledNextExecutionAssignedToUserId())
            ));
          }
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  public void downloadData() {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    dlHelper.updateData(this::onQueueEmpty, this::onDownloadError, User.class);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_USERS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    loadFromDatabase(false);
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

  public void rescheduleChore() {
    if (offlineLive.getValue()) {
      showMessage(getString(R.string.error_offline));
      return;
    }
    Object date = JSONObject.NULL;
    if (nextTrackingDateLive.getValue() != null && chore.getTrackDateOnlyBoolean()) {
      date = nextTrackingDateLive.getValue();
    } else if (nextTrackingDateLive.getValue() != null && nextTrackingTimeLive.getValue() != null) {
      date = nextTrackingDateLive.getValue() + " " + nextTrackingTimeLive.getValue();
    }
    Object userId = JSONObject.NULL;
    if (userLive.getValue() != null && userLive.getValue().getId() != -1
        && nextTrackingDateLive.getValue() != null && !nextTrackingDateLive.getValue().isEmpty()) {
      userId = String.valueOf(userLive.getValue().getId());
    }
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("rescheduled_date", date);
      jsonObject.put("rescheduled_next_execution_assigned_to_user_id", userId);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "rescheduleChore: " + e);
      }
    }

    dlHelper.put(
        grocyApi.getObject(ENTITY.CHORES, chore.getId()),
        jsonObject,
        response -> sendEvent(Event.NAVIGATE_UP),
        error -> {
          showErrorMessage();
          if (debug) {
            Log.e(TAG, "rescheduleChore: " + error);
          }
        }
    );
  }

  public void showUsersBottomSheet() {
    if (users == null || users.isEmpty()) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.USERS, new ArrayList<>(users));
    bundle.putInt(
        Constants.ARGUMENT.SELECTED_ID,
        userLive.getValue() != null
            ? userLive.getValue().getId()
            : -1
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    showBottomSheet(new UsersBottomSheet(), bundle);
  }

  public void resetReschedule() {
    nextTrackingDateLive.setValue(null);
    nextTrackingTimeLive.setValue(null);
    userLive.setValue(null);
  }

  public MutableLiveData<String> getNextTrackingDateLive() {
    return nextTrackingDateLive;
  }

  public LiveData<String> getNextTrackingDateTextLive() {
    return nextTrackingDateTextLive;
  }

  public LiveData<String> getNextTrackingDateHumanTextLive() {
    return nextTrackingDateHumanTextLive;
  }

  public MutableLiveData<String> getNextTrackingTimeLive() {
    return nextTrackingTimeLive;
  }

  public LiveData<String> getNextTrackingTimeHumanTextLive() {
    return nextTrackingTimeHumanTextLive;
  }

  public MutableLiveData<User> getUserLive() {
    return userLive;
  }

  public LiveData<String> getUserTextLive() {
    return userTextLive;
  }

  public Chore getChore() {
    return chore;
  }

  public boolean showTimeField() {
    return !chore.getTrackDateOnlyBoolean();
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

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class ChoreEntryRescheduleViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final Chore chore;

    public ChoreEntryRescheduleViewModelFactory(Application application, Chore chore) {
      this.application = application;
      this.chore = chore;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new ChoreEntryRescheduleViewModel(application, chore);
    }
  }
}
