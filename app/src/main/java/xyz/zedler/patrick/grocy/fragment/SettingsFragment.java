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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsFragment extends BaseFragment {

  private final static String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;
  private SharedPreferences sharedPrefs;
  private SettingsFragmentArgs args;
  private DownloadHelper.Queue queue;
  private ClickUtil clickUtil;
  private boolean debug;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    activity = (MainActivity) requireActivity();
    args = SettingsFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    clickUtil = new ClickUtil();
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    queue = viewModel.getDownloadHelper().newQueue(
        null,
        this::showVolleyError
    );
    binding.setFragment(this);
    binding.setActivity(activity);
    binding.setClickUtil(clickUtil);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    String category = args.getShowCategory();
    if (category == null) {
      //showOverview();
    } else if (category.equals(Constants.SETTINGS.SERVER.class.getSimpleName())) {
      //showCategoryServer();
    } else if (category.equals(Constants.SETTINGS.APPEARANCE.class.getSimpleName())) {
      //showCategoryAppearance();
    } else if (category.equals(Constants.SETTINGS.NETWORK.class.getSimpleName())) {
      //showCategoryNetwork();
    } else if (category.equals(Constants.SETTINGS.BEHAVIOR.class.getSimpleName())) {
      //showCategoryBehavior();
    } else if (category.equals(Constants.SETTINGS.SCANNER.class.getSimpleName())) {
      showCategoryScanner();
    } else if (category.equals(Constants.SETTINGS.STOCK.class.getSimpleName())) {
      showCategoryStock();
    } else if (category.equals(Constants.SETTINGS.SHOPPING_MODE.class.getSimpleName())) {
      showCategoryShoppingMode();
    } else if (category.equals(Constants.SETTINGS.DEBUGGING.class.getSimpleName())) {
      //showCategoryDebugging();
    }
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(binding.scroll);
      activity.getScrollBehavior().setHideOnScroll(true);
      activity.updateBottomAppBar(
          Constants.FAB.POSITION.GONE,
          R.menu.menu_empty,
          false,
          () -> {
          }
      );
      activity.binding.fabMain.hide();
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);

    queue.start();
  }

  private void showCategoryScanner() {
    binding.appBarTitle.setText(R.string.category_scanner);
        /*binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.SCANNER.FOOD_FACTS,
                Constants.SETTINGS_DEFAULT.SCANNER.FOOD_FACTS,
                getString(R.string.setting_open_food_facts),
                getString(R.string.setting_open_food_facts_description),
                getDrawable(R.drawable.ic_round_barcode)
        ));
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.SCANNER.FRONT_CAM,
                Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM,
                getString(R.string.setting_use_front_cam),
                getString(R.string.setting_use_front_cam_description),
                getDrawable(R.drawable.ic_round_camera_front)
        ));*/
  }

  private void showCategoryStock() {
    binding.appBarTitle.setText(R.string.title_stock_overview);
        /*binding.linearBody.addView(new SettingEntryCard(
                requireContext(),
                R.string.setting_synchronized
        ));
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            String days = sharedPrefs.getString(
                    Constants.SETTINGS.STOCK.DUE_SOON_DAYS,
                    Constants.SETTINGS_DEFAULT.STOCK.EXPIRING_SOON_DAYS
            );
            if(days == null || days.isEmpty() || days.equals("null")) {
                days = Constants.SETTINGS_DEFAULT.STOCK.EXPIRING_SOON_DAYS;
            }
            binding.linearBody.addView(new SettingEntryClick(
                    requireContext(),
                    Constants.SETTINGS.STOCK.DUE_SOON_DAYS,
                    R.string.setting_due_soon_days,
                    days,
                    null,
                    R.drawable.ic_round_event,
                    this::showSettingInputBottomSheet
            ));
        }*/
    if (isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
            /*binding.linearBody.addView(new SettingEntrySwitch(
                    requireContext(),
                    Constants.SETTINGS.STOCK.DISPLAY_DOTS_IN_STOCK,
                    Constants.SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK,
                    getString(R.string.setting_list_indicator),
                    getString(R.string.setting_list_indicator_description),
                    getDrawable(R.drawable.ic_round_shopping_list_long),
                    this::updatePrefOnServerBoolean
            ));*/
    }
  }

  private void showCategoryShoppingMode() {
    binding.appBarTitle.setText(R.string.title_shopping_mode);
        /*binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.SHOPPING_MODE.KEEP_SCREEN_ON,
                Constants.SETTINGS_DEFAULT.SHOPPING_MODE.KEEP_SCREEN_ON,
                getString(R.string.setting_keep_screen_on),
                getString(R.string.setting_keep_screen_on_description),
                getDrawable(R.drawable.ic_round_visibility)
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.SHOPPING_MODE.UPDATE_INTERVAL,
                R.string.setting_shopping_mode_update_interval,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_place,
                entry -> viewModel.getDownloadHelper().getLocations(arrayList -> {
                    int prefLocationId = sharedPrefs.getInt(
                            Constants.SETTINGS.STOCK.LOCATION,
                            Constants.SETTINGS_DEFAULT.STOCK.LOCATION
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new Location(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefLocationId);
                    bundle.putString(Constants.ARGUMENT.PREFERENCE, (String) entry.getTag());
                    activity.showBottomSheet(new LocationsBottomSheet(), bundle);
                }, this::showVolleyError).perform(UUID.randomUUID().toString())
        ));*/
  }

  private void showCategoryPresets() {
    //binding.appBarTitle.setText(R.string.category_presets);
        /*binding.linearBody.addView(new SettingEntryCard(
                requireContext(),
                R.string.setting_synchronized
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.STOCK.LOCATION,
                R.string.property_location,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_place,
                entry -> viewModel.getDownloadHelper().getLocations(arrayList -> {
                    int prefLocationId = sharedPrefs.getInt(
                            Constants.SETTINGS.STOCK.LOCATION,
                            Constants.SETTINGS_DEFAULT.STOCK.LOCATION
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new Location(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefLocationId);
                    bundle.putString(Constants.ARGUMENT.PREFERENCE, (String) entry.getTag());
                    activity.showBottomSheet(new LocationsBottomSheet(), bundle);
                }, this::showVolleyError).perform(UUID.randomUUID().toString())
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.STOCK.PRODUCT_GROUP,
                R.string.property_product_group,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_category,
                entry -> viewModel.getDownloadHelper().getProductGroups(arrayList -> {
                    int prefProductGroupId = sharedPrefs.getInt(
                            Constants.SETTINGS.STOCK.PRODUCT_GROUP,
                            Constants.SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new ProductGroup(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCT_GROUPS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefProductGroupId);
                    bundle.putString(Constants.ARGUMENT.PREFERENCE, (String) entry.getTag());
                    activity.showBottomSheet(new ProductGroupsBottomSheet(), bundle);
                }, this::showVolleyError).perform(UUID.randomUUID().toString())
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.STOCK.QUANTITY_UNIT,
                R.string.property_quantity_unit,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_weights,
                entry -> viewModel.getDownloadHelper().getQuantityUnits(arrayList -> {
                    int prefQuId = sharedPrefs.getInt(
                            Constants.SETTINGS.STOCK.QUANTITY_UNIT,
                            Constants.SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new QuantityUnit(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefQuId);
                    bundle.putString(Constants.ARGUMENT.PREFERENCE, (String) entry.getTag());
                    activity.showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
                }, this::showVolleyError).perform(UUID.randomUUID().toString())
        ));
        queue.append(viewModel.getDownloadHelper().getLocations(arrayList -> {
            int prefLocationId = sharedPrefs.getInt(
                    Constants.SETTINGS.STOCK.LOCATION,
                    Constants.SETTINGS_DEFAULT.STOCK.LOCATION
            );
            updateLocationSetting(viewModel.getLocation(arrayList, prefLocationId));
        }));
        queue.append(viewModel.getDownloadHelper().getProductGroups(arrayList -> {
            int prefProductGroupId = sharedPrefs.getInt(
                    Constants.SETTINGS.STOCK.PRODUCT_GROUP,
                    Constants.SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP
            );
            updateProductGroupSetting(viewModel.getProductGroup(arrayList, prefProductGroupId));
        }));
        queue.append(viewModel.getDownloadHelper().getQuantityUnits(arrayList -> {
            int prefQuId = sharedPrefs.getInt(
                    Constants.SETTINGS.STOCK.QUANTITY_UNIT,
                    Constants.SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT
            );
            updateQuantityUnitSetting(viewModel.getQuantityUnit(arrayList, prefQuId));
        }));*/
  }

  private void updatePrefOnServerBoolean(String pref, boolean isChecked) {
    sharedPrefs.edit().putBoolean(pref, isChecked).apply();
    JSONObject body = new JSONObject();
    try {
      body.put("value", isChecked);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "updatePrefBoolean: " + e);
      }
    }
    viewModel.getDownloadHelper().put(
        viewModel.getGrocyApi().getUserSetting(pref),
        body,
        response -> {
        },
        this::showVolleyError
    );
  }

  private void updateLocationSetting(Location location) {
        /*SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.STOCK.LOCATION
        );
        if(entry == null) return;
        if(location != null) {
            entry.setTitle(location.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.STOCK.LOCATION,
                    location.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.STOCK.LOCATION, -1).apply();
        }*/
  }

  private void updateProductGroupSetting(ProductGroup productGroup) {
        /*SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.STOCK.PRODUCT_GROUP
        );
        if(entry == null) return;
        if(productGroup != null) {
            entry.setTitle(productGroup.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.STOCK.PRODUCT_GROUP,
                    productGroup.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.STOCK.PRODUCT_GROUP, -1).apply();
        }*/
  }

  private void updateQuantityUnitSetting(QuantityUnit quantityUnit) {
        /*SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.STOCK.QUANTITY_UNIT
        );
        if(entry == null) return;
        if(quantityUnit != null) {
            entry.setTitle(quantityUnit.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.STOCK.QUANTITY_UNIT,
                    quantityUnit.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.STOCK.QUANTITY_UNIT, -1).apply();
        }*/
  }

  @Override
  public void setOption(Object value, String option) {
    JSONObject body = new JSONObject();
    switch (option) {
      case Constants.SETTINGS.STOCK.LOCATION:
        try {
          body.put("value", ((Location) value).getId());
        } catch (JSONException e) {
          if (debug) {
            Log.e(TAG, "setValue: " + e);
          }
        }
        viewModel.getDownloadHelper().put(
            viewModel.getGrocyApi().getUserSetting(option),
            body,
            response -> updateLocationSetting((Location) value),
            this::showVolleyError
        );
        break;
      case Constants.SETTINGS.STOCK.PRODUCT_GROUP:
        try {
          body.put("value", ((ProductGroup) value).getId());
        } catch (JSONException e) {
          if (debug) {
            Log.e(TAG, "setValue: " + e);
          }
        }
        viewModel.getDownloadHelper().put(
            viewModel.getGrocyApi().getUserSetting(option),
            body,
            response -> updateProductGroupSetting((ProductGroup) value),
            this::showVolleyError
        );
        break;
      case Constants.SETTINGS.STOCK.QUANTITY_UNIT:
        try {
          body.put("value", ((QuantityUnit) value).getId());
        } catch (JSONException e) {
          if (debug) {
            Log.e(TAG, "setValue: " + e);
          }
        }
        viewModel.getDownloadHelper().put(
            viewModel.getGrocyApi().getUserSetting(option),
            body,
            response -> updateQuantityUnitSetting((QuantityUnit) value),
            this::showVolleyError
        );
        break;
      case Constants.SETTINGS.STOCK.DUE_SOON_DAYS:
        try {
          body.put("value", Integer.parseInt((String) value));
        } catch (JSONException e) {
          if (debug) {
            Log.e(TAG, "setValue: " + e);
          }
        }
        viewModel.getDownloadHelper().put(
            viewModel.getGrocyApi().getUserSetting(option),
            body,
            response -> {
                            /*SettingEntryClick entry = binding.linearBody.findViewWithTag(option);
                            entry.setTitle((String) value);
                            sharedPrefs.edit()
                                    .putString(option, (String) value).apply();*/
            },
            this::showVolleyError
        );
    }
  }

  private void showVolleyError(VolleyError error) {
    if (debug) {
      Log.e(TAG, "showVolleyError: " + error.getLocalizedMessage());
    }
    if (error.networkResponse == null) {
      activity.showMessage(R.string.error_network);
    } else {
      activity.showMessage(R.string.error_undefined);
    }
  }

  public boolean shouldNavigateToBehavior() {
    return args.getShowCategory() != null
        && args.getShowCategory().equals(Constants.SETTINGS.BEHAVIOR.class.getSimpleName());
  }

  public boolean shouldNavigateToServer() {
    return args.getShowCategory() != null
        && args.getShowCategory().equals(Constants.SETTINGS.SERVER.class.getSimpleName());
  }

  private boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return false;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary, () -> {
      if (shouldNavigateToBehavior()) {
        setArguments(new SettingsFragmentArgs.Builder(args)
            .setShowCategory(null).build().toBundle());
        new Handler().postDelayed(() -> navigate(SettingsFragmentDirections
            .actionSettingsFragmentToSettingsCatBehaviorFragment()), 200);
      } else if (shouldNavigateToServer()) {
        setArguments(new SettingsFragmentArgs.Builder(args)
            .setShowCategory(null).build().toBundle());
        new Handler().postDelayed(() -> navigate(SettingsFragmentDirections
            .actionSettingsFragmentToSettingsCatServerFragment()), 200);
      }
    });
  }
}
