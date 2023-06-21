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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.json.JSONException;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class BaseViewModel extends AndroidViewModel {

  private final EventHandler eventHandler;
  private final MutableLiveData<Boolean> offlineLive;
  private final SharedPreferences sharedPrefs;
  private final Resources resources;
  private boolean isSearchVisible;
  private final boolean debug;

  public BaseViewModel(@NonNull Application application) {
    super(application);
    eventHandler = new EventHandler();
    offlineLive = new MutableLiveData<>(false);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    isSearchVisible = false;

    resources = getApplication().getResources();
    Configuration config = resources.getConfiguration();
    config.setLocale(LocaleUtil.getLocale());
    resources.updateConfiguration(config, resources.getDisplayMetrics());
  }

  SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  boolean getBeginnerModeEnabled() {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
        Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
    );
  }

  boolean isDebuggingEnabled() {
    return debug;
  }

  public boolean isOpenFoodFactsEnabled() {
    return sharedPrefs.getBoolean(
        SETTINGS.BEHAVIOR.FOOD_FACTS,
        SETTINGS_DEFAULT.BEHAVIOR.FOOD_FACTS
    );
  }

  public void onError(Object error, String TAG) {
    if (error instanceof VolleyError) {
      Log.e(TAG, "onError: VolleyError: " + error);
      showNetworkErrorMessage((VolleyError) error);
    } else if (error instanceof JSONException) {
      Log.e(TAG, "onError: JSONException: " + error);
      showJSONErrorMessage((JSONException) error);
    } else if (error instanceof Throwable) {
      Log.e(TAG, "onError: Throwable: " + error);
      showThrowableErrorMessage((Throwable) error);
    }
  }

  public void showErrorMessage() {
    showMessage(getString(R.string.error_undefined));
  }

  public void showThrowableErrorMessage(@Nullable Throwable error) {
    String messageShort;
    String messageLong;
    if (error instanceof SQLiteBlobTooBigException) {
      messageShort = getString(R.string.error_database);
      messageLong = getString(R.string.error_pictures_size_exceeds_limit);
    } else if (error != null && error.getLocalizedMessage() != null) {
      messageShort = getString(R.string.error_undefined);
      messageLong = getApplication()
          .getString(R.string.error_insert, error.getLocalizedMessage());
    } else {
      messageShort = getString(R.string.error_undefined);
      messageLong = messageShort;
    }
    if (error != null) {
      error.printStackTrace();
      showSnackbarWithDetailsAction(error, messageShort, messageLong);
    } else {
      showMessageLongDuration(messageShort);
    }
  }

  public void showNetworkErrorMessage(VolleyError error) {
    // similar method is also in BaseFragment
    String messageShort;
    String messageLong;
    if (error != null && error.networkResponse != null
        && error.networkResponse.statusCode == 403) {
      messageShort = getString(R.string.error_permission);
      messageLong = messageShort;
    } else if (error != null && error.getLocalizedMessage() != null) {
      messageShort = getString(R.string.error_network);
      messageLong = getString(R.string.error_network_exact, error.getLocalizedMessage());
    } else {
      messageShort = getString(R.string.error_network);
      messageLong = messageShort;
    }
    if (error != null) {
      error.printStackTrace();
      showSnackbarWithDetailsAction(error, messageShort, messageLong);
    } else {
      showMessageLongDuration(messageShort);
    }
  }

  public void showJSONErrorMessage(JSONException error) {
    String messageShort;
    String messageLong;
    if (error != null && error.getLocalizedMessage() != null) {
      messageShort = getString(R.string.error_undefined);
      messageLong = getApplication()
          .getString(R.string.error_insert, error.getLocalizedMessage());
    } else {
      messageShort = getString(R.string.error_undefined);
      messageLong = messageShort;
    }
    if (error != null) {
      error.printStackTrace();
      showSnackbarWithDetailsAction(error, messageShort, messageLong);
    } else {
      showMessageLongDuration(messageShort);
    }
  }

  private void showErrorDetailsAlertDialog(Context context, String message) {
    AlertDialog alertDialog = new MaterialAlertDialogBuilder(
        context, R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.error_details)
        .setMessage(message)
        .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.cancel())
        .setNegativeButton(R.string.action_copy, (dialog, which) -> {}).create();
    alertDialog.show();

    Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    negativeButton.setOnClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) context
          .getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText("Error details", message);
      clipboard.setPrimaryClip(clip);
      Toast.makeText(context, getString(R.string.msg_copied_clipboard), Toast.LENGTH_SHORT).show();
    });
  }

  private void showSnackbarWithDetailsAction(
      @NonNull Throwable error,
      String messageShort,
      String messageLong
  ) {
    SnackbarMessage snackbarMessage = new SnackbarMessage(messageShort);
    error.printStackTrace();

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    error.printStackTrace(pw);
    String stackTrace = sw.toString();
    snackbarMessage.setAction(
        getString(R.string.action_details),
        v -> showErrorDetailsAlertDialog(v.getContext(), messageLong + "\n\n" + stackTrace)
    );
    snackbarMessage.setDurationSecs(5);
    showSnackbar(snackbarMessage);
  }

  public void showMessageLongDuration(@Nullable String message) {
    if (message == null) {
      return;
    }
    showSnackbar(new SnackbarMessage(message).setDurationSecs(5));
  }

  public void showMessage(@Nullable String message) {
    if (message == null) {
      return;
    }
    showSnackbar(new SnackbarMessage(message));
  }

  public void showMessage(@StringRes int message) {
    showSnackbar(new SnackbarMessage(getString(message)));
  }

  void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
    eventHandler.setValue(snackbarMessage);
  }

  void showBottomSheet(@NonNull BaseBottomSheetDialogFragment bottomSheet, Bundle bundle) {
    eventHandler.setValue(new BottomSheetEvent(bottomSheet, bundle));
  }

  void showBottomSheet(@NonNull BaseBottomSheetDialogFragment bottomSheet) {
    eventHandler.setValue(new BottomSheetEvent(bottomSheet));
  }

  void navigateUp() {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return Event.NAVIGATE_UP;
      }
    });
  }

  void sendEvent(int type) {
    eventHandler.setValue(new Event() {
      @Override
      public int getType() {
        return type;
      }
    });
  }

  void sendEvent(@SuppressWarnings("SameParameterValue") int type, Bundle bundle) {
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

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  void setOfflineLive(boolean isOffline) {
    if (isOffline() != isOffline) offlineLive.setValue(isOffline);
  }

  String getString(@StringRes int resId) {
    return resources.getString(resId);
  }

  String getString(@StringRes int resId, Object... formatArgs) {
    return resources.getString(resId, formatArgs);
  }

  Resources getResources() {
    return resources;
  }

  public boolean isSearchVisible() {
    return isSearchVisible;
  }

  public void setIsSearchVisible(boolean visible) {
    isSearchVisible = visible;
  }
}
