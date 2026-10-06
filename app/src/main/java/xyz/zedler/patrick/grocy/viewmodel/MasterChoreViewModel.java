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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class MasterChoreViewModel extends BaseViewModel {

  private static final String TAG = MasterChoreViewModel.class.getSimpleName();
  private static final String CREATED_ID = "created_chore_id";
  private static final String REQUEST_PENDING = "request_pending";

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final SavedStateHandle state;
  private final MutableLiveData<Boolean> savingLive = new MutableLiveData<>(false);

  public MasterChoreViewModel(@NonNull Application application, SavedStateHandle state) {
    super(application);
    this.state = state;
    dlHelper = new DownloadHelper(application, TAG);
    grocyApi = new GrocyApi(application);
  }

  public SavedStateHandle getState() {
    return state;
  }

  public MutableLiveData<Boolean> getSavingLive() {
    return savingLive;
  }

  public boolean hasCreatedChore() {
    return state.contains(CREATED_ID);
  }

  public void save(JSONObject values) {
    if (Boolean.TRUE.equals(savingLive.getValue())) return;
    if (!VersionUtil.isGrocyServerMin400(getSharedPrefs())) {
      showMessage(getString(R.string.msg_chore_create_server_version));
      return;
    }
    if (hasCreatedChore()) {
      calculateAssignments();
      return;
    }
    // After a timeout or process death the POST may have reached the server.
    // Do not create a second record when its outcome is unknown.
    if (Boolean.TRUE.equals(state.get(REQUEST_PENDING))) {
      showMessage(getString(R.string.msg_chore_create_check_server));
      return;
    }
    savingLive.setValue(true);
    state.set(REQUEST_PENDING, true);
    dlHelper.postWithoutRetry(
        grocyApi.getObjects(GrocyApi.ENTITY.CHORES), values,
        response -> {
          int id = response != null ? response.optInt("created_object_id", -1) : -1;
          if (id <= 0) {
            savingLive.setValue(false);
            showMessage(getString(R.string.msg_chore_create_check_server));
            return;
          }
          state.set(CREATED_ID, id);
          state.set(REQUEST_PENDING, false);
          calculateAssignments();
        },
        error -> {
          savingLive.setValue(false);
          // A definite client rejection can be corrected and resubmitted.
          if (error.networkResponse != null && error.networkResponse.statusCode >= 400
              && error.networkResponse.statusCode < 500) {
            state.set(REQUEST_PENDING, false);
            showNetworkErrorMessage(error);
          } else {
            showMessage(getString(R.string.msg_chore_create_check_server));
          }
        }
    );
  }

  private void calculateAssignments() {
    savingLive.setValue(true);
    JSONObject body = new JSONObject();
    try {
      body.put("chore_id", (Integer) state.get(CREATED_ID));
    } catch (org.json.JSONException e) {
      savingLive.setValue(false);
      onError(e, TAG);
      return;
    }
    dlHelper.post(
        grocyApi.calculateChoreAssignments(), body,
        response -> {
          savingLive.setValue(false);
          sendEvent(Event.NAVIGATE_UP);
        },
        error -> {
          savingLive.setValue(false);
          showMessage(getString(R.string.msg_chore_created_assignment_retry));
        }
    );
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
