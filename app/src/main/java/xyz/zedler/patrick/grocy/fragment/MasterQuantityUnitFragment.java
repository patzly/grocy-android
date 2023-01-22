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
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
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
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterQuantityUnitBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterQuantityUnitFragment extends BaseFragment {

  private final static String TAG = MasterQuantityUnitFragment.class.getSimpleName();

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterQuantityUnitBinding binding;
  private PluralUtil pluralUtil;

  private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
  private ArrayList<String> quantityUnitNames = new ArrayList<>();
  private QuantityUnit editQuantityUnit;

  private boolean isRefresh = false;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterQuantityUnitBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    dlHelper.destroy();
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    // PREFERENCES

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    // WEB

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    pluralUtil = new PluralUtil(activity);

    // VARIABLES

    quantityUnits = new ArrayList<>();
    quantityUnitNames = new ArrayList<>();
    editQuantityUnit = null;

    isRefresh = false;

    // VIEWS

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterQuantityUnit);
    systemBarBehavior.setScroll(binding.scrollMasterQuantityUnit, binding.linearContainerScroll);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

    // swipe refresh
    binding.swipeMasterQuantityUnit.setOnRefreshListener(this::refresh);

    // name
    binding.editTextMasterQuantityUnitName.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterQuantityUnitName);
          }
        });

    // name plural
    binding.editTextMasterQuantityUnitNamePlural.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterQuantityUnitNamePlural);
          }
        });
    binding.linearMasterQuantityUnitForms.setVisibility(
        pluralUtil.isPluralFormsFieldNecessary() ? View.VISIBLE : View.GONE
    );

    if (pluralUtil.languageRulesNotImplemented()) {
      binding.cardPluralFormsNotSupportedInfo.setVisibility(View.VISIBLE);
    }

    // description
    binding.editTextMasterQuantityUnitDescription.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterQuantityUnitDescription);
          }
        });

    MasterQuantityUnitFragmentArgs args = MasterQuantityUnitFragmentArgs
        .fromBundle(requireArguments());
    editQuantityUnit = args.getQuantityUnit();
    if (editQuantityUnit != null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      resetAll();
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextMasterQuantityUnitName),
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

    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scrollMasterQuantityUnit, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        editQuantityUnit != null ? R.menu.menu_master_item_edit : R.menu.menu_empty,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        (getArguments() == null
            || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
            && savedInstanceState == null,
        this::saveQuantityUnit
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (isHidden()) {
      return;
    }

    outState.putParcelableArrayList("quantityUnits", quantityUnits);
    outState.putStringArrayList("quantityUnitNames", quantityUnitNames);

    outState.putParcelable("editQuantityUnit", editQuantityUnit);

    outState.putBoolean("isRefresh", isRefresh);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");
    quantityUnitNames = savedInstanceState.getStringArrayList("quantityUnitNames");

    editQuantityUnit = savedInstanceState.getParcelable("editQuantityUnit");

    isRefresh = savedInstanceState.getBoolean("isRefresh");

    binding.swipeMasterQuantityUnit.setRefreshing(false);
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
      binding.swipeMasterQuantityUnit.setRefreshing(false);
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
    binding.swipeMasterQuantityUnit.setRefreshing(true);
    downloadQuantityUnits();
  }

  @SuppressLint("ShowToast")
  private void downloadQuantityUnits() {
    dlHelper.get(
        grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
        response -> {
          quantityUnits = gson.fromJson(
              response,
              new TypeToken<ArrayList<QuantityUnit>>() {
              }.getType()
          );
          SortUtil.sortQuantityUnitsByName(quantityUnits, true);
          quantityUnitNames = getQuantityUnitNames();

          binding.swipeMasterQuantityUnit.setRefreshing(false);

          updateEditReferences();

          if (isRefresh && editQuantityUnit != null) {
            fillWithEditReferences();
          } else {
            resetAll();
          }
        },
        error -> {
          binding.swipeMasterQuantityUnit.setRefreshing(false);
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
    if (editQuantityUnit != null) {
      QuantityUnit editQuantityUnit = getQuantityUnit(this.editQuantityUnit.getId());
      if (editQuantityUnit != null) {
        this.editQuantityUnit = editQuantityUnit;
      }
    }
  }

  private ArrayList<String> getQuantityUnitNames() {
    ArrayList<String> names = new ArrayList<>();
    if (quantityUnits != null) {
      for (QuantityUnit quantityUnit : quantityUnits) {
        if (editQuantityUnit != null) {
          if (quantityUnit.getId() != editQuantityUnit.getId()) {
            names.add(quantityUnit.getName());
          }
        } else {
          names.add(quantityUnit.getName());
        }
      }
    }
    return names;
  }

  private QuantityUnit getQuantityUnit(int quantityUnitId) {
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == quantityUnitId) {
        return quantityUnit;
      }
    }
    return null;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editQuantityUnit != null) {
      // name
      binding.editTextMasterQuantityUnitName.setText(editQuantityUnit.getName());
      // name (plural form)
      binding.editTextMasterQuantityUnitNamePlural.setText(
          editQuantityUnit.getNamePluralCanNull()
      );
      // plural forms
      binding.editTextMasterQuantityUnitForms.setText(
          editQuantityUnit.getPluralForms()
      );
      // description
      binding.editTextMasterQuantityUnitDescription.setText(
          editQuantityUnit.getDescription()
      );
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputMasterQuantityUnitName.clearFocus();
    binding.textInputMasterQuantityUnitName.setErrorEnabled(false);
    binding.textInputMasterQuantityUnitNamePlural.clearFocus();
    binding.textInputMasterQuantityUnitNamePlural.setErrorEnabled(false);
    binding.textInputMasterQuantityUnitForms.clearFocus();
    binding.textInputMasterQuantityUnitForms.setErrorEnabled(false);
    binding.textInputMasterQuantityUnitDescription.clearFocus();
    binding.textInputMasterQuantityUnitDescription.setErrorEnabled(false);
  }

  public void saveQuantityUnit() {
    if (isFormInvalid()) {
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      Editable name = binding.editTextMasterQuantityUnitName.getText();
      Editable plural = binding.editTextMasterQuantityUnitNamePlural.getText();
      Editable forms = binding.editTextMasterQuantityUnitForms.getText();
      Editable description = binding.editTextMasterQuantityUnitDescription.getText();
      jsonObject.put("name", (name != null ? name : "").toString().trim());
      jsonObject.put("name_plural", (plural != null ? plural : "").toString().trim());
      if(forms != null && !forms.toString().isEmpty()) {
        jsonObject.put("plural_forms", forms.toString().replaceAll("(?m)^\\s+$", ""));
      }
      jsonObject.put(
          "description", (description != null ? description : "").toString().trim()
      );
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveQuantityUnit: " + e);
      }
    }
    if (editQuantityUnit != null) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.QUANTITY_UNITS, editQuantityUnit.getId()),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveQuantityUnit: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveQuantityUnit: " + error);
            }
          }
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    boolean isInvalid = false;

    String name = String.valueOf(binding.editTextMasterQuantityUnitName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputMasterQuantityUnitName.setError(
          activity.getString(R.string.error_empty)
      );
      isInvalid = true;
    } else if (!quantityUnitNames.isEmpty() && quantityUnitNames.contains(name)) {
      binding.textInputMasterQuantityUnitName.setError(
          activity.getString(R.string.error_duplicate)
      );
      isInvalid = true;
    }

    return isInvalid;
  }

  private void resetAll() {
    if (editQuantityUnit != null) {
      return;
    }
    clearInputFocusAndErrors();
    binding.editTextMasterQuantityUnitName.setText(null);
    binding.editTextMasterQuantityUnitNamePlural.setText(null);
    binding.editTextMasterQuantityUnitForms.setText(null);
    binding.editTextMasterQuantityUnitDescription.setText(null);
  }

  public void deleteQuantityUnitSafely() {
    if (editQuantityUnit == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.ENTITY, GrocyApi.ENTITY.QUANTITY_UNITS);
    bundle.putInt(Constants.ARGUMENT.OBJECT_ID, editQuantityUnit.getId());
    bundle.putString(Constants.ARGUMENT.OBJECT_NAME, editQuantityUnit.getName());
    activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
  }

  @Override
  public void deleteObject(int quantityUnitId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.QUANTITY_UNITS, quantityUnitId),
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

  public Toolbar.OnMenuItemClickListener getBottomMenuClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_delete) {
        ViewUtil.startIcon(item);
        deleteQuantityUnitSafely();
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
