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
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataShoppingListEdit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class ShoppingListEditViewModel extends AndroidViewModel {

  private static final String TAG = ShoppingListEditViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final EventHandler eventHandler;
  private final ShoppingListRepository repository;
  private final FormDataShoppingListEdit formData;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;

  private ArrayList<ShoppingList> shoppingLists;

  private DownloadHelper.Queue currentQueueLoading;
  private final ShoppingList startupShoppingList;
  private final boolean debug;

  public ShoppingListEditViewModel(
      @NonNull Application application,
      @Nullable ShoppingList startupShoppingList
  ) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    eventHandler = new EventHandler();
    repository = new ShoppingListRepository(application);
    formData = new FormDataShoppingListEdit(startupShoppingList);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    this.startupShoppingList = startupShoppingList;
  }

  public FormDataShoppingListEdit getFormData() {
    return formData;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadShoppingListsFromDatabase(
        shoppingLists -> {
          this.shoppingLists = shoppingLists;
          formData.setShoppingListNames(getShoppingListNames(shoppingLists));
          if (downloadAfterLoading) {
            downloadData();
          }
        }
    );
  }

  private ArrayList<String> getShoppingListNames(ArrayList<ShoppingList> shoppingLists) {
    ArrayList<String> shoppingListNames = new ArrayList<>();
    for (ShoppingList sl : shoppingLists) {
      shoppingListNames.add(sl.getName());
    }
    return shoppingListNames;
  }

  private String getLastTime(String sharedPref) {
    return sharedPrefs.getString(sharedPref, null);
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
      dlHelper.getTimeDbChanged(
          this::downloadData,
          () -> onDownloadError(null)
      );
      return;
    }

    DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    queue.append(dlHelper.updateShoppingLists(dbChangedTime, shoppingLists -> {
      this.shoppingLists = shoppingLists;
      formData.setShoppingListNames(getShoppingListNames(shoppingLists));
    }));

    if (queue.isEmpty()) {
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
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LISTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    repository.updateDatabase(this.shoppingLists, () -> {
    });
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

  public void saveShoppingList() {
    if (offlineLive.getValue()) {
      showMessage(getString(R.string.error_offline));
      return;
    }
    if (!formData.isFormValid()) {
      return;
    }

    String name = formData.getNameLive().getValue().trim();
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("name", name);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveShoppingList: " + e);
      }
    }

    if (startupShoppingList != null) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, startupShoppingList.getId()),
          jsonObject,
          response -> sendEvent(Event.NAVIGATE_UP),
          error -> {
            showErrorMessage();
            if (debug) {
              Log.e(TAG, "saveShoppingList: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
          jsonObject,
          response -> {
            int objectId;
            try {
              objectId = response.getInt("created_object_id");
              Log.i(TAG, "saveShoppingList: " + objectId);
            } catch (JSONException e) {
              if (debug) {
                Log.e(TAG, "saveShoppingList: " + e);
              }
              objectId = 1;
            }
            setShoppingListForPreviousFragment(objectId);
            sendEvent(Event.NAVIGATE_UP);
          },
          error -> {
            showErrorMessage();
            if (debug) {
              Log.e(TAG, "saveShoppingList: " + error);
            }
          }
      );
    }
  }

  public void safeDeleteShoppingList() {
    if (startupShoppingList == null) {
      return;
    }
    clearAllItems(this::deleteShoppingList);
  }

  public void deleteShoppingList() {
    if (startupShoppingList == null) {
      return;
    }
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LISTS, startupShoppingList.getId()),
        response -> {
          resetShoppingListForPreviousFragmentIfNecessary();
          showMessage(getString(
              R.string.msg_shopping_list_deleted,
              startupShoppingList.getName()
          ));
          sendEvent(Event.NAVIGATE_UP);
        },
        error -> {
          showErrorMessage();
          if (debug) {
            Log.i(TAG, "deleteShoppingList: " + error);
          }
          downloadData();
        }
    );
  }

  public void clearAllItems(Runnable onResponse) {
    if (startupShoppingList == null) {
      return;
    }
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("list_id", startupShoppingList.getId());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "clearShoppingList: " + e);
      }
    }
    dlHelper.post(
        grocyApi.clearShoppingList(),
        jsonObject,
        response -> {
          if (onResponse != null) {
            onResponse.run();
          }
        },
        error -> {
          showMessage(getString(R.string.error_undefined));
          if (debug) {
            Log.e(
                TAG, "clearShoppingList: "
                    + startupShoppingList.getName()
                    + ": " + error
            );
          }
        }
    );
  }

  public void resetShoppingListForPreviousFragmentIfNecessary() {
    if (startupShoppingList == null) {
      return;
    }
    int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
    if (startupShoppingList.getId() != lastId) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, 1);
    sendEvent(Event.SET_SHOPPING_LIST_ID, bundle);
  }

  public void setShoppingListForPreviousFragment(int selectedId) {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
    sendEvent(Event.SET_SHOPPING_LIST_ID, bundle);
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

  private void showErrorMessage() {
    showMessage(getString(R.string.error_undefined));
  }

  private void showMessage(@NonNull String message) {
    showSnackbar(new SnackbarMessage(message));
  }

  private void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
    eventHandler.setValue(snackbarMessage);
  }

  private void sendEvent(@SuppressWarnings("SameParameterValue") int type) {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return type;
      }
    });
  }

  private void sendEvent(@SuppressWarnings("SameParameterValue") int type, Bundle bundle) {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return type;
      }

      @Override
      public Bundle getBundle() {
        return bundle;
      }
    });
  }

  @NonNull
  public EventHandler getEventHandler() {
    return eventHandler;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  private String getString(@StringRes int resId) {
    return getApplication().getString(resId);
  }

  private String getString(@SuppressWarnings("SameParameterValue") @StringRes int resId, Object... formatArgs) {
    return getApplication().getString(resId, formatArgs);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class ShoppingListEditViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final ShoppingList shoppingList;

    public ShoppingListEditViewModelFactory(Application application, ShoppingList shoppingList) {
      this.application = application;
      this.shoppingList = shoppingList;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new ShoppingListEditViewModel(application, shoppingList);
    }
  }
}
