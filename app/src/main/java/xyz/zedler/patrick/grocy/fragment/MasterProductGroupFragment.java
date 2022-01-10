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

package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductGroupBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterProductGroupFragment extends BaseFragment {

  private final static String TAG = MasterProductGroupFragment.class.getSimpleName();

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterProductGroupBinding binding;

  private ArrayList<ProductGroup> productGroups;
  private ArrayList<String> productGroupNames;
  private ProductGroup editProductGroup;

  private boolean isRefresh;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductGroupBinding.inflate(
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
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    activity = (MainActivity) requireActivity();

    // PREFERENCES

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    // WEB

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    // VARIABLES

    productGroups = new ArrayList<>();
    productGroupNames = new ArrayList<>();
    editProductGroup = null;

    isRefresh = false;

    // VIEWS

    binding.frameMasterProductGroupCancel.setOnClickListener(v -> activity.onBackPressed());

    // swipe refresh
    binding.swipeMasterProductGroup.setProgressBackgroundColorSchemeColor(
        ContextCompat.getColor(activity, R.color.surface)
    );
    binding.swipeMasterProductGroup.setColorSchemeColors(
        ContextCompat.getColor(activity, R.color.secondary)
    );
    binding.swipeMasterProductGroup.setOnRefreshListener(this::refresh);

    // name
    binding.editTextMasterProductGroupName.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterProductGroupName);
          }
        });

    // description
    binding.editTextMasterProductGroupDescription.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterProductGroupDescription);
          }
        });

    MasterProductGroupFragmentArgs args = MasterProductGroupFragmentArgs
        .fromBundle(requireArguments());
    editProductGroup = args.getProductGroup();
    if (editProductGroup != null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      resetAll();
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextMasterProductGroupName),
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
    updateUI((getArguments() == null
        || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
        && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll_master_product_group);
    activity.getScrollBehavior().setHideOnScroll(false);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        R.menu.menu_master_item_edit,
        animated,
        this::setUpBottomMenu
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        animated,
        this::saveProductGroup
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (isHidden()) {
      return;
    }

    outState.putParcelableArrayList("productGroups", productGroups);
    outState.putStringArrayList("productGroupNames", productGroupNames);

    outState.putParcelable("editProductGroup", editProductGroup);

    outState.putBoolean("isRefresh", isRefresh);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    productGroups = savedInstanceState.getParcelableArrayList("productGroups");
    productGroupNames = savedInstanceState.getStringArrayList("productGroupNames");

    editProductGroup = savedInstanceState.getParcelable("editProductGroup");

    isRefresh = savedInstanceState.getBoolean("isRefresh");

    binding.swipeMasterProductGroup.setRefreshing(false);
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
      binding.swipeMasterProductGroup.setRefreshing(false);
      activity.showSnackbar(
          Snackbar.make(
              activity.binding.frameMainContainer,
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
    binding.swipeMasterProductGroup.setRefreshing(true);
    downloadProductGroups();
  }

  @SuppressLint("ShowToast")
  private void downloadProductGroups() {
    dlHelper.get(
        grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
        response -> {
          productGroups = gson.fromJson(
              response,
              new TypeToken<ArrayList<ProductGroup>>() {
              }.getType()
          );
          SortUtil.sortProductGroupsByName(requireContext(), productGroups, true);
          productGroupNames = getProductGroupNames();

          binding.swipeMasterProductGroup.setRefreshing(false);

          updateEditReferences();

          if (isRefresh && editProductGroup != null) {
            fillWithEditReferences();
          } else {
            resetAll();
          }
        },
        error -> {
          binding.swipeMasterProductGroup.setRefreshing(false);
          activity.showSnackbar(
              Snackbar.make(
                  activity.binding.frameMainContainer,
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
    if (editProductGroup != null) {
      ProductGroup editProductGroup = getProductGroup(this.editProductGroup.getId());
      if (editProductGroup != null) {
        this.editProductGroup = editProductGroup;
      }
    }
  }

  private ArrayList<String> getProductGroupNames() {
    ArrayList<String> names = new ArrayList<>();
    if (productGroups != null) {
      for (ProductGroup productGroup : productGroups) {
        if (editProductGroup != null) {
          if (productGroup.getId() != editProductGroup.getId()) {
            names.add(productGroup.getName().trim());
          }
        } else {
          names.add(productGroup.getName().trim());
        }
      }
    }
    return names;
  }

  private ProductGroup getProductGroup(int productGroupId) {
    for (ProductGroup productGroup : productGroups) {
      if (productGroup.getId() == productGroupId) {
        return productGroup;
      }
    }
    return null;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editProductGroup != null) {
      // name
      binding.editTextMasterProductGroupName.setText(editProductGroup.getName());
      // description
      binding.editTextMasterProductGroupDescription.setText(
          editProductGroup.getDescription()
      );
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputMasterProductGroupName.clearFocus();
    binding.textInputMasterProductGroupName.setErrorEnabled(false);
    binding.textInputMasterProductGroupDescription.clearFocus();
    binding.textInputMasterProductGroupDescription.setErrorEnabled(false);
  }

  public void saveProductGroup() {
    if (isFormInvalid()) {
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      Editable name = binding.editTextMasterProductGroupName.getText();
      Editable description = binding.editTextMasterProductGroupDescription.getText();
      jsonObject.put("name", (name != null ? name : "").toString().trim());
      jsonObject.put(
          "description", (description != null ? description : "").toString().trim()
      );
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveProductGroup: " + e);
      }
    }
    if (editProductGroup != null) {
      dlHelper.put(
          grocyApi.getObject(
              GrocyApi.ENTITY.PRODUCT_GROUPS,
              editProductGroup.getId()
          ),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveProductGroup: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveProductGroup: " + error);
            }
          }
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    boolean isInvalid = false;

    String name = String.valueOf(binding.editTextMasterProductGroupName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputMasterProductGroupName.setError(
          activity.getString(R.string.error_empty)
      );
      isInvalid = true;
    } else if (!productGroupNames.isEmpty() && productGroupNames.contains(name)) {
      binding.textInputMasterProductGroupName.setError(
          activity.getString(R.string.error_duplicate)
      );
      isInvalid = true;
    }

    return isInvalid;
  }

  private void resetAll() {
    if (editProductGroup != null) {
      return;
    }
    clearInputFocusAndErrors();
    binding.editTextMasterProductGroupName.setText(null);
    binding.editTextMasterProductGroupDescription.setText(null);
  }

  public void deleteProductGroupSafely() {
    if (editProductGroup == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.ENTITY, GrocyApi.ENTITY.PRODUCT_GROUPS);
    bundle.putInt(Constants.ARGUMENT.OBJECT_ID, editProductGroup.getId());
    bundle.putString(Constants.ARGUMENT.OBJECT_NAME, editProductGroup.getName());
    activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
  }

  @Override
  public void deleteObject(int productGroupId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.PRODUCT_GROUPS, productGroupId),
        response -> activity.navigateUp(),
        error -> showErrorMessage(error)
    );
  }

  private void showErrorMessage(VolleyError volleyError) {
    activity.showSnackbar(
        Snackbar.make(
            activity.binding.frameMainContainer,
            getErrorMessage(volleyError),
            Snackbar.LENGTH_SHORT
        )
    );
  }

  public void setUpBottomMenu() {
    MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
    if (delete != null) {
      delete.setOnMenuItemClickListener(item -> {
        ViewUtil.startIcon(item);
        deleteProductGroupSafely();
        return true;
      });
      delete.setVisible(editProductGroup != null);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
