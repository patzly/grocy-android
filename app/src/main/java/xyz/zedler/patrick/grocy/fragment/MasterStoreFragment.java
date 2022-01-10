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
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterStoreBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MasterStoreFragment extends BaseFragment {

  private final static String TAG = MasterStoreFragment.class.getSimpleName();

  private MainActivity activity;
  private Gson gson;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private FragmentMasterStoreBinding binding;

  private ArrayList<Store> stores;
  private ArrayList<String> storeNames;
  private Store editStore;

  private boolean isRefresh;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterStoreBinding.inflate(inflater, container, false);
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
    debug = PrefsUtil.isDebuggingEnabled(activity);

    // WEB

    dlHelper = new DownloadHelper(activity, TAG);
    grocyApi = activity.getGrocyApi();
    gson = new Gson();

    // VARIABLES

    stores = new ArrayList<>();
    storeNames = new ArrayList<>();

    editStore = null;
    isRefresh = false;

    // VIEWS

    binding.frameMasterStoreCancel.setOnClickListener(v -> activity.onBackPressed());

    // swipe refresh
    binding.swipeMasterStore.setProgressBackgroundColorSchemeColor(
        ContextCompat.getColor(activity, R.color.surface)
    );
    binding.swipeMasterStore.setColorSchemeColors(
        ContextCompat.getColor(activity, R.color.secondary)
    );
    binding.swipeMasterStore.setOnRefreshListener(this::refresh);

    // name
    binding.editTextMasterStoreName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
      if (hasFocus) {
        ViewUtil.startIcon(binding.imageMasterStoreName);
      }
    });

    // description
    binding.editTextMasterStoreDescription.setOnFocusChangeListener(
        (View v, boolean hasFocus) -> {
          if (hasFocus) {
            ViewUtil.startIcon(binding.imageMasterStoreDescription);
          }
        });

    MasterStoreFragmentArgs args = MasterStoreFragmentArgs.fromBundle(requireArguments());
    editStore = args.getStore();
    if (editStore != null) {
      fillWithEditReferences();
    } else if (savedInstanceState == null) {
      resetAll();
      new Handler().postDelayed(
          () -> activity.showKeyboard(binding.editTextMasterStoreName),
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
    activity.getScrollBehavior().setUpScroll(R.id.scroll_master_store);
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
        this::saveStore
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (isHidden()) {
      return;
    }

    outState.putParcelableArrayList("stores", stores);
    outState.putStringArrayList("storeNames", storeNames);

    outState.putParcelable("editStore", editStore);
  }

  private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
    if (isHidden()) {
      return;
    }

    stores = savedInstanceState.getParcelableArrayList("stores");
    storeNames = savedInstanceState.getStringArrayList("storeNames");

    editStore = savedInstanceState.getParcelable("editStore");

    isRefresh = false;
    binding.swipeMasterStore.setRefreshing(false);

    updateEditReferences();

    if (isRefresh && editStore != null) {
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
      binding.swipeMasterStore.setRefreshing(false);
      activity.showSnackbar(
          Snackbar.make(
              activity.findViewById(R.id.frame_main_container),
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
    binding.swipeMasterStore.setRefreshing(true);
    downloadStores();
  }

  @SuppressLint("ShowToast")
  private void downloadStores() {
    dlHelper.get(
        grocyApi.getObjects(GrocyApi.ENTITY.STORES),
        response -> {
          stores = gson.fromJson(
              response,
              new TypeToken<List<Store>>() {
              }.getType()
          );
          SortUtil.sortStoresByName(requireContext(), stores, true);
          storeNames = getStoreNames();

          binding.swipeMasterStore.setRefreshing(false);

          updateEditReferences();

          if (isRefresh && editStore != null) {
            fillWithEditReferences();
          } else {
            resetAll();
          }
        },
        error -> {
          binding.swipeMasterStore.setRefreshing(false);
          activity.showSnackbar(
              Snackbar.make(
                  activity.findViewById(R.id.frame_main_container),
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
    if (editStore != null) {
      Store editStore = getStore(this.editStore.getId());
      if (editStore != null) {
        this.editStore = editStore;
      }
    }
  }

  private ArrayList<String> getStoreNames() {
    ArrayList<String> names = new ArrayList<>();
    if (stores != null) {
      for (Store store : stores) {
        if (editStore != null) {
          if (store.getId() != editStore.getId()) {
            names.add(store.getName().trim());
          }
        } else {
          names.add(store.getName().trim());
        }
      }
    }
    return names;
  }

  private Store getStore(int storeId) {
    for (Store store : stores) {
      if (store.getId() == storeId) {
        return store;
      }
    }
    return null;
  }

  private void fillWithEditReferences() {
    clearInputFocusAndErrors();
    if (editStore != null) {
      // name
      binding.editTextMasterStoreName.setText(editStore.getName());
      // description
      binding.editTextMasterStoreDescription.setText(editStore.getDescription());
    }
  }

  private void clearInputFocusAndErrors() {
    activity.hideKeyboard();
    binding.textInputMasterStoreName.clearFocus();
    binding.textInputMasterStoreName.setErrorEnabled(false);
    binding.textInputMasterStoreDescription.clearFocus();
    binding.textInputMasterStoreDescription.setErrorEnabled(false);
  }

  public void saveStore() {
    if (isFormInvalid()) {
      return;
    }

    JSONObject jsonObject = new JSONObject();
    try {
      Editable name = binding.editTextMasterStoreName.getText();
      Editable description = binding.editTextMasterStoreDescription.getText();
      jsonObject.put("name", (name != null ? name : "").toString().trim());
      jsonObject.put(
          "description", (description != null ? description : "").toString().trim()
      );
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "saveStore: " + e);
      }
    }
    if (editStore != null) {
      dlHelper.put(
          grocyApi.getObject(GrocyApi.ENTITY.STORES, editStore.getId()),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveStore: " + error);
            }
          }
      );
    } else {
      dlHelper.post(
          grocyApi.getObjects(GrocyApi.ENTITY.STORES),
          jsonObject,
          response -> activity.navigateUp(),
          error -> {
            showErrorMessage(error);
            if (debug) {
              Log.e(TAG, "saveStore: " + error);
            }
          }
      );
    }
  }

  private boolean isFormInvalid() {
    clearInputFocusAndErrors();
    boolean isInvalid = false;

    String name = String.valueOf(binding.editTextMasterStoreName.getText()).trim();
    if (name.isEmpty()) {
      binding.textInputMasterStoreName.setError(activity.getString(R.string.error_empty));
      isInvalid = true;
    } else if (!storeNames.isEmpty() && storeNames.contains(name)) {
      binding.textInputMasterStoreName.setError(activity.getString(R.string.error_duplicate));
      isInvalid = true;
    }

    return isInvalid;
  }

  private void resetAll() {
    if (editStore != null) {
      return;
    }
    clearInputFocusAndErrors();
    binding.editTextMasterStoreName.setText(null);
    binding.editTextMasterStoreDescription.setText(null);
  }

  public void deleteStoreSafely() {
    if (editStore == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.ENTITY, GrocyApi.ENTITY.STORES);
    bundle.putInt(Constants.ARGUMENT.OBJECT_ID, editStore.getId());
    bundle.putString(Constants.ARGUMENT.OBJECT_NAME, editStore.getName());
    activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
  }

  @Override
  public void deleteObject(int storeId) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.STORES, storeId),
        response -> activity.navigateUp(),
        error -> showErrorMessage(error)
    );
  }

  private void showErrorMessage(VolleyError volleyError) {
    activity.showSnackbar(
        Snackbar.make(
            activity.findViewById(R.id.frame_main_container),
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
        deleteStoreSafely();
        return true;
      });
      delete.setVisible(editStore != null);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
