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

package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterLocationBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterLocationFragment extends BaseFragment {

  private final static String TAG = MasterLocationFragment.class.getSimpleName();

  private static final String DIALOG_DELETE = "dialog_delete";

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterLocationBinding binding;

  private ArrayList<Location> locations;
  private ArrayList<String> locationNames;
  private Location editLocation;
  private AlertDialog dialogDelete;

  private boolean isRefresh;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterLocationBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (dialogDelete != null) {
      // Else it throws an leak exception because the context is somehow from the activity
      dialogDelete.dismiss();
    }

    binding = null;
    dlHelper.destroy();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    activity = (MainActivity) requireActivity();

    // PREFERENCES

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    // WEB REQUESTS

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    // INITIALIZE VARIABLES

    locations = new ArrayList<>();
    locationNames = new ArrayList<>();

    editLocation = null;
    isRefresh = false;

    // INITIALIZE VIEWS

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterLocation);
    systemBarBehavior.setScroll(binding.scrollMasterLocation, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

    // swipe refresh
    binding.swipeMasterLocation.setOnRefreshListener(this::refresh);

    // name
    binding.editTextMasterLocationName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
      if (hasFocus) {
        ViewUtil.startIcon(binding.imageMasterLocationName);
      }
    });

    // description
    binding.editTextMasterLocationDescription.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterLocationDescription);
          }
        });

    // is freezer
    binding.checkboxMasterLocationFreezer.setOnCheckedChangeListener(
        (buttonView, isChecked) -> ViewUtil.startIcon(binding.imageMasterLocationFreezer)
    );
    binding.linearMasterLocationFreezer.setOnClickListener(v -> {
      ViewUtil.startIcon(binding.imageMasterLocationFreezer);
      binding.checkboxMasterLocationFreezer.setChecked(
          !binding.checkboxMasterLocationFreezer.isChecked()
      );
    });

    MasterLocationFragmentArgs args = MasterLocationFragmentArgs.fromBundle(requireArguments());
    editLocation = args.getLocation();
    if (editLocation != null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      resetAll();
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextMasterLocationName),
          50
      );
    }

    // START

    if (savedInstanceState == null) {
      load();
    } else {
      restoreSavedInstanceState(savedInstanceState);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scrollMasterLocation, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        editLocation != null ? R.menu.menu_master_item_edit : R.menu.menu_empty,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        (getArguments() == null
            || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
            && savedInstanceState == null,
        this::saveLocation
    );

    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_DELETE)) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showDeleteConfirmationDialog, 1
      );
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (isHidden()) {
      return;
    }

    boolean isShowing = dialogDelete != null && dialogDelete.isShowing();
    outState.putBoolean(DIALOG_DELETE, isShowing);

    outState.putParcelableArrayList("locations", locations);
    outState.putStringArrayList("locationNames", locationNames);

    outState.putParcelable("editLocation", editLocation);

    outState.putBoolean("isRefresh", isRefresh);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    locations = savedInstanceState.getParcelableArrayList("locations");
    locationNames = savedInstanceState.getStringArrayList("locationNames");

    editLocation = savedInstanceState.getParcelable("editLocation");

    isRefresh = savedInstanceState.getBoolean("isRefresh");
    binding.swipeMasterLocation.setRefreshing(false);

    updateEditReferences();

    if (isRefresh && editLocation != null) {
      fillWithEditReferences();
    } else {
      resetAll();
    }
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    if (!hidden && getView() != null) {
      onViewCreated(getView(), null);
    }
  }

  private void load() {
    if (activity.isOnline()) {
      download();
    }
  }

  @SuppressLint("ShowToast")
  private void refresh() {
    // for only fill with up-to-date data on refresh,
    // not on startup as the bundle should contain everything needed
    isRefresh = true;
    if (activity.isOnline()) {
      download();
    } else {
      binding.swipeMasterLocation.setRefreshing(false);
      activity.showSnackbar(
          activity.getSnackbar(R.string.msg_no_connection, false).setAction(
              R.string.action_retry,
              v1 -> refresh()
          )
      );
    }
  }

  private void download() {
    binding.swipeMasterLocation.setRefreshing(true);
    downloadLocations();
  }

  @SuppressLint("ShowToast")
  private void downloadLocations() {
    dlHelper.get(
        grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
        response -> {
          locations = gson.fromJson(
              response,
              new TypeToken<ArrayList<Location>>() {
              }.getType()
          );
          SortUtil.sortLocationsByName(locations, true);
          locationNames = getLocationNames();

          binding.swipeMasterLocation.setRefreshing(false);

          updateEditReferences();

          if (isRefresh && editLocation != null) {
            fillWithEditReferences();
          } else {
            resetAll();
          }
        },
        error -> {
          binding.swipeMasterLocation.setRefreshing(false);
          activity.showSnackbar(
              activity.getSnackbar(getErrorMessage(error), false).setAction(
                  R.string.action_retry,
                  v1 -> download()
              )
          );
        }
    );
  }

  private void updateEditReferences() {
    if (editLocation != null) {
      Location editLocation = getLocation(this.editLocation.getId());
      if (editLocation != null) {
        this.editLocation = editLocation;
      }
    }
  }

  private ArrayList<String> getLocationNames() {
    ArrayList<String> names = new ArrayList<>();
    if (locations != null) {
      for (Location location : locations) {
        if (editLocation != null) {
          if (location.getId() != editLocation.getId()) {
            names.add(location.getName().trim());
          }
        } else {
          names.add(location.getName().trim());
        }
      }
    }
    return names;
  }

  private Location getLocation(int locationId) {
    for (Location location : locations) {
      if (location.getId() == locationId) {
        return location;
      }
    }
    return null;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editLocation != null) {
      // name
      binding.editTextMasterLocationName.setText(editLocation.getName());
      // description
      binding.editTextMasterLocationDescription.setText(editLocation.getDescription());
      // is freezer
      binding.checkboxMasterLocationFreezer.setChecked(editLocation.getIsFreezerInt() == 1);
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputMasterLocationName.clearFocus();
    binding.textInputMasterLocationName.setErrorEnabled(false);
    binding.textInputMasterLocationDescription.clearFocus();
    binding.textInputMasterLocationDescription.setErrorEnabled(false);
  }

  public void saveLocation() {
    if (isFormInvalid()) {
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      Editable name = binding.editTextMasterLocationName.getText();
      Editable description = binding.editTextMasterLocationDescription.getText();
      jsonObject.put(
          "name", (name != null ? name : "").toString().trim()
      );
      jsonObject.put(
          "description", (description != null ? description : "").toString().trim()
      );
      jsonObject.put("is_freezer", binding.checkboxMasterLocationFreezer.isChecked());
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveLocation: " + e);
      }
    }
    if (editLocation != null) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.LOCATIONS, editLocation.getId()),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveLocation: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveLocation: " + error);
            }
          }
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    boolean isInvalid = false;

    String name = String.valueOf(binding.editTextMasterLocationName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputMasterLocationName.setError(activity.getString(R.string.error_empty));
      isInvalid = true;
    } else if (!locationNames.isEmpty() && locationNames.contains(name)) {
      binding.textInputMasterLocationName.setError(
          activity.getString(R.string.error_duplicate)
      );
      isInvalid = true;
    }

    return isInvalid;
  }

  private void resetAll() {
    if (editLocation != null) {
      return;
    }
    clearInputFocusAndErrors();
    binding.editTextMasterLocationName.setText(null);
    binding.editTextMasterLocationDescription.setText(null);
    binding.checkboxMasterLocationFreezer.setChecked(false);
  }

  @Override
  public void deleteObject(int locationId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.LOCATIONS, locationId),
        response -> activity.navigateUp(),
        this::showErrorMessage
    );
  }

  private void showErrorMessage(VolleyError volleyError) {
    activity.showSnackbar(getErrorMessage(volleyError), true);
  }

  private void showDeleteConfirmationDialog() {
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            activity.getString(
                R.string.msg_master_delete,
                getString(R.string.property_location),
                editLocation.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          if (editLocation == null) {
            return;
          }
          deleteObject(editLocation.getId());
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  public Toolbar.OnMenuItemClickListener getBottomMenuClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_delete) {
        ViewUtil.startIcon(item);
        showDeleteConfirmationDialog();
        return true;
      }
      return false;
    };
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
