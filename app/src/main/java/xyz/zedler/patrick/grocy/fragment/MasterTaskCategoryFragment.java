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
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterTaskCategoryBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterTaskCategoryFragment extends BaseFragment {

  private final static String TAG = MasterTaskCategoryFragment.class.getSimpleName();

  private static final String DIALOG_DELETE = "dialog_delete";

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterTaskCategoryBinding binding;

  private ArrayList<TaskCategory> taskCategories;
  private ArrayList<String> taskCategoryNames;
  private TaskCategory editTaskCategory;
  private AlertDialog dialogDelete;

  private boolean isRefresh;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterTaskCategoryBinding.inflate(
        inflater, container, false
    );
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
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    taskCategories = new ArrayList<>();
    taskCategoryNames = new ArrayList<>();
    editTaskCategory = null;

    isRefresh = false;

    // VIEWS

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

    // swipe refresh
    binding.swipe.setOnRefreshListener(this::refresh);

    // name
    binding.editTextName.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageName);
          }
        });

    // description
    binding.editTextDescription.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageDescription);
          }
        });

    MasterTaskCategoryFragmentArgs args = MasterTaskCategoryFragmentArgs
        .fromBundle(requireArguments());
    editTaskCategory = args.getTaskCategory();
    if (editTaskCategory != null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      resetAll();
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextName),
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
        binding.appBar, false, binding.scroll, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        editTaskCategory != null ? R.menu.menu_master_item_edit : R.menu.menu_empty,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        (getArguments() == null
            || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
            && savedInstanceState == null,
        this::saveTaskCategory
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

    outState.putParcelableArrayList("taskCategories", taskCategories);
    outState.putStringArrayList("taskCategoryNames", taskCategoryNames);

    outState.putParcelable("editTaskCategory", editTaskCategory);

    outState.putBoolean("isRefresh", isRefresh);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    taskCategories = savedInstanceState.getParcelableArrayList("taskCategories");
    taskCategoryNames = savedInstanceState.getStringArrayList("taskCategoryNames");

    editTaskCategory = savedInstanceState.getParcelable("editTaskCategory");

    isRefresh = savedInstanceState.getBoolean("isRefresh");

    binding.swipe.setRefreshing(false);
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
      binding.swipe.setRefreshing(false);
      activity.showSnackbar(
          Snackbar.make(
              activity.binding.coordinatorMain,
              activity.getString(R.string.msg_no_connection),
              Snackbar.LENGTH_SHORT
          ).setAction(
              activity.getString(R.string.action_retry),
              v1 -> refresh()
          )
      );
    }
  }

  private void download() {
    binding.swipe.setRefreshing(true);
    downloadProductGroups();
  }

  @SuppressLint("ShowToast")
  private void downloadProductGroups() {
    dlHelper.get(
        grocyApi.getObjects(ENTITY.TASK_CATEGORIES),
        response -> {
          taskCategories = gson.fromJson(
              response,
              new TypeToken<ArrayList<TaskCategory>>() {
              }.getType()
          );
          SortUtil.sortTaskCategoriesByName(taskCategories, true);
          taskCategoryNames = getTaskCategoryNames();

          binding.swipe.setRefreshing(false);

          updateEditReferences();

          if (isRefresh && editTaskCategory != null) {
            fillWithEditReferences();
          } else {
            resetAll();
          }
        },
        error -> {
          binding.swipe.setRefreshing(false);
          activity.showSnackbar(
              Snackbar.make(
                  activity.binding.coordinatorMain,
                  getErrorMessage(error),
                  Snackbar.LENGTH_SHORT
              ).setAction(
                  activity.getString(R.string.action_retry),
                  v1 -> download()
              )
          );
        }
    );
  }

  private void updateEditReferences() {
    if (editTaskCategory != null) {
      TaskCategory editTaskCategory = TaskCategory
          .getTaskCategoryFromId(taskCategories, this.editTaskCategory.getId());
      if (editTaskCategory != null) {
        this.editTaskCategory = editTaskCategory;
      }
    }
  }

  private ArrayList<String> getTaskCategoryNames() {
    ArrayList<String> names = new ArrayList<>();
    if (taskCategories != null) {
      for (TaskCategory taskCategory : taskCategories) {
        if (editTaskCategory != null) {
          if (taskCategory.getId() != editTaskCategory.getId()) {
            names.add(taskCategory.getName().trim());
          }
        } else {
          names.add(taskCategory.getName().trim());
        }
      }
    }
    return names;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editTaskCategory != null) {
      binding.editTextName.setText(editTaskCategory.getName());
      binding.editTextDescription.setText(editTaskCategory.getDescription());
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputName.clearFocus();
    binding.textInputName.setErrorEnabled(false);
    binding.textInputDescription.clearFocus();
    binding.textInputDescription.setErrorEnabled(false);
  }

  public void saveTaskCategory() {
    if (isFormInvalid()) {
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      Editable name = binding.editTextName.getText();
      Editable description = binding.editTextDescription.getText();
      jsonObject.put("name", (name != null ? name : "").toString().trim());
      jsonObject.put(
          "description", (description != null ? description : "").toString().trim()
      );
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveTaskCategory: " + e);
      }
    }
    if (editTaskCategory != null) {
      dlHelper.put(
          grocyApi.getObject(ENTITY.TASK_CATEGORIES, editTaskCategory.getId()),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveTaskCategory: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(ENTITY.TASK_CATEGORIES),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveTaskCategory: " + error);
            }
          }
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    boolean isInvalid = false;

    String name = String.valueOf(binding.editTextName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputName.setError(activity.getString(R.string.error_empty));
      isInvalid = true;
    } else if (!taskCategoryNames.isEmpty() && taskCategoryNames.contains(name)) {
      binding.textInputName.setError(activity.getString(R.string.error_duplicate));
      isInvalid = true;
    }

    return isInvalid;
  }

  private void resetAll() {
    if (editTaskCategory != null) {
      return;
    }
    clearInputFocusAndErrors();
    binding.editTextName.setText(null);
    binding.editTextDescription.setText(null);
  }

  @Override
  public void deleteObject(int taskCategoryId) {
    dlHelper.delete(
        grocyApi.getObject(ENTITY.TASK_CATEGORIES, taskCategoryId),
        response -> activity.navigateUp(),
        this::showErrorMessage
    );
  }

  private void showErrorMessage(VolleyError volleyError) {
    activity.showSnackbar(
        Snackbar.make(
            activity.binding.coordinatorMain,
            getErrorMessage(volleyError),
            Snackbar.LENGTH_SHORT
        )
    );
  }

  private void showDeleteConfirmationDialog() {
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            activity.getString(
                R.string.msg_master_delete,
                getString(R.string.property_task_category),
                editTaskCategory.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          if (editTaskCategory == null) {
            return;
          }
          deleteObject(editTaskCategory.getId());
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
