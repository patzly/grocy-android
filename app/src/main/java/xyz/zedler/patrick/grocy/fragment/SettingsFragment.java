package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.RestartBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.SettingInputBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.SettingCategory;
import xyz.zedler.patrick.grocy.view.SettingEntryCard;
import xyz.zedler.patrick.grocy.view.SettingEntryClick;
import xyz.zedler.patrick.grocy.view.SettingEntryHeading;
import xyz.zedler.patrick.grocy.view.SettingEntrySwitch;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsFragment extends BaseFragment {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private FragmentSettingsBinding binding;
    private MainActivity activity;
    private SettingsViewModel viewModel;
    private SharedPreferences sharedPrefs;
    private DownloadHelper.Queue queue;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        activity = (MainActivity) requireActivity();
        SettingsFragmentArgs args = SettingsFragmentArgs.fromBundle(requireArguments());
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);
        queue = viewModel.getDownloadHelper().newQueue(
                null,
                this::showVolleyError
        );

        String category = args.getShowCategory();
        if(category == null) {
            showOverview();
        } else if(category.equals(Constants.SETTINGS.SERVER.class.getSimpleName())) {
            showCategoryServer();
        } else if(category.equals(Constants.SETTINGS.APPEARANCE.class.getSimpleName())) {
            showCategoryAppearance();
        } else if(category.equals(Constants.SETTINGS.NETWORK.class.getSimpleName())) {
            showCategoryNetwork();
        } else if(category.equals(Constants.SETTINGS.BEHAVIOR.class.getSimpleName())) {
            showCategoryBehavior();
        } else if(category.equals(Constants.SETTINGS.SCANNER.class.getSimpleName())) {
            showCategoryScanner();
        } else if(category.equals(Constants.SETTINGS.STOCK.class.getSimpleName())) {
            showCategoryStock();
        } else if(category.equals(Constants.SETTINGS.SHOPPING_MODE.class.getSimpleName())) {
            showCategoryShoppingMode();
        } else if(category.equals(Constants.SETTINGS.PRESETS.class.getSimpleName())) {
            showCategoryPresets();
        } else if(category.equals(Constants.SETTINGS.DEBUGGING.class.getSimpleName())) {
            showCategoryDebugging();
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
        binding.frameBack.setOnClickListener(v -> activity.navigateUp());

        if(activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
            activity.showHideDemoIndicator(this, true);
            activity.getScrollBehavior().setUpScroll(binding.scroll);
            activity.getScrollBehavior().setHideOnScroll(true);
            activity.updateBottomAppBar(
                    Constants.FAB.POSITION.GONE,
                    R.menu.menu_empty,
                    false,
                    () -> {}
            );
            activity.binding.fabMain.hide();
        }

        setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);

        queue.start();
    }

    private void showOverview() {
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_server,
                sharedPrefs.getString(Constants.PREF.SERVER_URL, getString(R.string.error_unknown)),
                R.drawable.ic_round_settings_system,
                () -> goTo(Constants.SETTINGS.SERVER.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_appearance,
                R.drawable.ic_round_dark_mode_on_anim,
                () -> goTo(Constants.SETTINGS.APPEARANCE.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_network,
                R.drawable.ic_round_dark_mode_on_anim,
                () -> goTo(Constants.SETTINGS.NETWORK.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_behavior,
                R.drawable.ic_round_dark_mode_on_anim,
                () -> goTo(Constants.SETTINGS.BEHAVIOR.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_barcode_scanner,
                R.drawable.ic_round_barcode_scan,
                () -> goTo(Constants.SETTINGS.SCANNER.class.getSimpleName())
        ));
        if(isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)
                || isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)
        ) {
            binding.linearBody.addView(new SettingCategory(
                    requireContext(),
                    R.string.title_stock_overview,
                    R.drawable.ic_round_view_list, // TODO: Shelf icon would be good
                    () -> goTo(Constants.SETTINGS.STOCK.class.getSimpleName())
            ));
        }
        if(isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
            binding.linearBody.addView(new SettingCategory(
                    requireContext(),
                    R.string.title_shopping_mode,
                    R.drawable.ic_round_storefront,
                    () -> goTo(Constants.SETTINGS.SHOPPING_MODE.class.getSimpleName())
            ));
        }
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_purchase_consume,
                R.drawable.ic_round_pasta,
                () -> goTo(Constants.SETTINGS.PURCHASE_CONSUME.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_presets,
                R.drawable.ic_round_widgets,
                () -> goTo(Constants.SETTINGS.PRESETS.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_debugging,
                R.drawable.ic_round_bug_report_anim,
                () -> goTo(Constants.SETTINGS.DEBUGGING.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.title_about_this_app,
                R.drawable.ic_round_info_outline_anim_menu,
                () -> navigate(SettingsFragmentDirections.actionSettingsFragmentToAboutFragment())
        ));
    }

    /**
     * Navigates to a category (it opens SettingsFragment with the specified category).
     * @param category (String) if null, the category overview will be shown
     */
    private void goTo(@Nullable String category) {
        navigate(SettingsFragmentDirections.actionSettingsFragmentSelf().setShowCategory(category));
    }

    private void showCategoryServer() {
        binding.appBarTitle.setText(R.string.category_server);
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.SERVER.GROCY_URL,
                R.string.hint_server,
                sharedPrefs.getString(Constants.PREF.SERVER_URL, getString(R.string.date_unknown)),
                null,
                R.drawable.ic_round_settings_system,
                null
        ));
        boolean isVersionCompatible = viewModel.isVersionCompatible();
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.SERVER.GROCY_VERSION,
                R.string.info_grocy_version,
                sharedPrefs.getString(
                        Constants.PREF.GROCY_VERSION,
                        getString(R.string.date_unknown)
                ),
                isVersionCompatible
                        ? R.string.info_grocy_version_compatible
                        : R.string.info_grocy_version_incompatible,
                isVersionCompatible ? R.color.retro_green_fg : R.color.retro_red_fg,
                true,
                R.drawable.ic_round_settings_system,
                entry -> {
                    if(!isVersionCompatible) {
                        activity.showBottomSheet(viewModel.getCompatibilityBottomSheet());
                    }
                }
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.SERVER.RELOAD_CONFIG,
                R.string.setting_reload_config,
                R.string.setting_reload_config_description,
                R.drawable.ic_round_sync_anim,
                entry -> ConfigUtil.loadInfo(
                        viewModel.getDownloadHelper(),
                        viewModel.getGrocyApi(),
                        sharedPrefs,
                        () -> activity.showBottomSheet(new RestartBottomSheet()),
                        this::showVolleyError
                )
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.SERVER.LOGOUT,
                R.string.setting_logout,
                R.string.setting_logout_description,
                R.drawable.ic_round_logout,
                entry -> {
                    Bundle bundle = null;
                    if(viewModel.isDemo()) bundle = new Bundle(); // empty bundle for indicating demo type
                    activity.showBottomSheet(new LogoutBottomSheet(), bundle);
                }
        ));
    }

    private void showCategoryAppearance() {
        binding.appBarTitle.setText(R.string.category_appearance);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.APPEARANCE.DARK_MODE,
                Constants.SETTINGS_DEFAULT.APPEARANCE.DARK_MODE_DEFAULT,
                getString(R.string.setting_dark_mode),
                getString(R.string.setting_dark_mode_description),
                R.drawable.ic_round_dark_mode_off_anim,
                R.drawable.ic_round_dark_mode_on_anim,
                this::updateTheme
        ));
    }

    private void showCategoryNetwork() {
        binding.appBarTitle.setText(R.string.category_network);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.APPEARANCE.DARK_MODE,
                Constants.SETTINGS_DEFAULT.APPEARANCE.DARK_MODE_DEFAULT,
                getString(R.string.setting_dark_mode),
                getString(R.string.setting_dark_mode_description),
                R.drawable.ic_round_dark_mode_off_anim,
                R.drawable.ic_round_dark_mode_on_anim,
                this::updateTheme
        ));
    }

    private void showCategoryBehavior() {
        binding.appBarTitle.setText(R.string.category_behavior);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.BEHAVIOR.START_DESTINATION,
                Constants.SETTINGS_DEFAULT.APPEARANCE.DARK_MODE_DEFAULT,
                getString(R.string.setting_dark_mode),
                getString(R.string.setting_dark_mode_description),
                R.drawable.ic_round_dark_mode_off_anim,
                R.drawable.ic_round_dark_mode_on_anim,
                this::updateTheme
        ));
    }

    private void showCategoryScanner() {
        binding.appBarTitle.setText(R.string.category_barcode_scanner);
        binding.linearBody.addView(new SettingEntrySwitch(
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
        ));
    }

    private void showCategoryStock() {
        binding.appBarTitle.setText(R.string.title_stock_overview);
        binding.linearBody.addView(new SettingEntryCard(
                requireContext(),
                R.string.setting_synchronized
        ));
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            String days = sharedPrefs.getString(
                    Constants.SETTINGS.STOCK.EXPIRING_SOON_DAYS,
                    Constants.SETTINGS_DEFAULT.STOCK.EXPIRING_SOON_DAYS
            );
            if(days == null || days.isEmpty() || days.equals("null")) {
                days = Constants.SETTINGS_DEFAULT.STOCK.EXPIRING_SOON_DAYS;
            }
            binding.linearBody.addView(new SettingEntryClick(
                    requireContext(),
                    Constants.SETTINGS.STOCK.EXPIRING_SOON_DAYS,
                    R.string.setting_expiring_soon_days,
                    days,
                    null,
                    R.drawable.ic_round_event,
                    this::showSettingInputBottomSheet
            ));
        }
        if(isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
            binding.linearBody.addView(new SettingEntrySwitch(
                    requireContext(),
                    Constants.SETTINGS.STOCK.DISPLAY_DOTS_IN_STOCK,
                    Constants.SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK,
                    getString(R.string.setting_list_indicator),
                    getString(R.string.setting_list_indicator_description),
                    getDrawable(R.drawable.ic_round_shopping_list_long),
                    this::updatePrefOnServerBoolean
            ));
        }
    }

    private void showCategoryShoppingMode() {
        binding.appBarTitle.setText(R.string.title_shopping_mode);
        binding.linearBody.addView(new SettingEntrySwitch(
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
                            Constants.SETTINGS.PRESETS.LOCATION,
                            Constants.SETTINGS_DEFAULT.PRESETS.LOCATION
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
    }

    private void showCategoryPresets() {
        binding.appBarTitle.setText(R.string.category_presets);
        binding.linearBody.addView(new SettingEntryCard(
                requireContext(),
                R.string.setting_synchronized
        ));
        binding.linearBody.addView(new SettingEntryHeading(
                requireContext(),
                R.string.category_presets_new_products
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.PRESETS.LOCATION,
                R.string.property_location,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_place,
                entry -> viewModel.getDownloadHelper().getLocations(arrayList -> {
                    int prefLocationId = sharedPrefs.getInt(
                            Constants.SETTINGS.PRESETS.LOCATION,
                            Constants.SETTINGS_DEFAULT.PRESETS.LOCATION
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
                Constants.SETTINGS.PRESETS.PRODUCT_GROUP,
                R.string.property_product_group,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_category,
                entry -> viewModel.getDownloadHelper().getProductGroups(arrayList -> {
                    int prefProductGroupId = sharedPrefs.getInt(
                            Constants.SETTINGS.PRESETS.PRODUCT_GROUP,
                            Constants.SETTINGS_DEFAULT.PRESETS.PRODUCT_GROUP
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
                Constants.SETTINGS.PRESETS.QUANTITY_UNIT,
                R.string.property_quantity_unit,
                getString(R.string.setting_not_loaded),
                null,
                R.drawable.ic_round_weights,
                entry -> viewModel.getDownloadHelper().getQuantityUnits(arrayList -> {
                    int prefQuId = sharedPrefs.getInt(
                            Constants.SETTINGS.PRESETS.QUANTITY_UNIT,
                            Constants.SETTINGS_DEFAULT.PRESETS.QUANTITY_UNIT
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
                    Constants.SETTINGS.PRESETS.LOCATION,
                    Constants.SETTINGS_DEFAULT.PRESETS.LOCATION
            );
            updateLocationSetting(viewModel.getLocation(arrayList, prefLocationId));
        }));
        queue.append(viewModel.getDownloadHelper().getProductGroups(arrayList -> {
            int prefProductGroupId = sharedPrefs.getInt(
                    Constants.SETTINGS.PRESETS.PRODUCT_GROUP,
                    Constants.SETTINGS_DEFAULT.PRESETS.PRODUCT_GROUP
            );
            updateProductGroupSetting(viewModel.getProductGroup(arrayList, prefProductGroupId));
        }));
        queue.append(viewModel.getDownloadHelper().getQuantityUnits(arrayList -> {
            int prefQuId = sharedPrefs.getInt(
                    Constants.SETTINGS.PRESETS.QUANTITY_UNIT,
                    Constants.SETTINGS_DEFAULT.PRESETS.QUANTITY_UNIT
            );
            updateQuantityUnitSetting(viewModel.getQuantityUnit(arrayList, prefQuId));
        }));
    }

    private void showCategoryDebugging() {
        binding.appBarTitle.setText(R.string.category_debugging);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
                Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING,
                getString(R.string.setting_debug),
                getString(R.string.setting_debug_description),
                getDrawable(R.drawable.ic_round_bug_report_anim)
        ));
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.ENABLE_INFO_LOGS,
                Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_INFO_LOGS,
                getString(R.string.setting_info_logs)
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.SHOW_LOGS,
                R.string.title_logs,
                R.string.setting_logs_description,
                entry -> navigate(SettingsFragmentDirections.actionSettingsFragmentToLogFragment())
        ));
    }

    private void updatePrefOnServerBoolean(String pref, boolean isChecked) {
        sharedPrefs.edit().putBoolean(pref, isChecked).apply();
        JSONObject body = new JSONObject();
        try {
            body.put("value", isChecked);
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "updatePrefBoolean: " + e);
        }
        viewModel.getDownloadHelper().put(
                viewModel.getGrocyApi().getUserSetting(pref),
                body,
                response -> {},
                this::showVolleyError
        );
    }

    private void updateLocationSetting(Location location) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.LOCATION
        );
        if(entry == null) return;
        if(location != null) {
            entry.setTitle(location.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.PRESETS.LOCATION,
                    location.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.PRESETS.LOCATION, -1).apply();
        }
    }

    private void updateProductGroupSetting(ProductGroup productGroup) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.PRODUCT_GROUP
        );
        if(entry == null) return;
        if(productGroup != null) {
            entry.setTitle(productGroup.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.PRESETS.PRODUCT_GROUP,
                    productGroup.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.PRESETS.PRODUCT_GROUP, -1).apply();
        }
    }

    private void updateQuantityUnitSetting(QuantityUnit quantityUnit) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.QUANTITY_UNIT
        );
        if(entry == null) return;
        if(quantityUnit != null) {
            entry.setTitle(quantityUnit.getName());
            sharedPrefs.edit().putInt(
                    Constants.SETTINGS.PRESETS.QUANTITY_UNIT,
                    quantityUnit.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.SETTINGS.PRESETS.QUANTITY_UNIT, -1).apply();
        }
    }

    @Override
    public void setOption(Object value, String option) {
        JSONObject body = new JSONObject();
        switch (option) {
            case Constants.SETTINGS.PRESETS.LOCATION:
                try {
                    body.put("value", ((Location) value).getId());
                } catch (JSONException e) {
                    if (debug) Log.e(TAG, "setValue: " + e);
                }
                viewModel.getDownloadHelper().put(
                        viewModel.getGrocyApi().getUserSetting(option),
                        body,
                        response -> updateLocationSetting((Location) value),
                        this::showVolleyError
                );
                break;
            case Constants.SETTINGS.PRESETS.PRODUCT_GROUP:
                try {
                    body.put("value", ((ProductGroup) value).getId());
                } catch (JSONException e) {
                    if (debug) Log.e(TAG, "setValue: " + e);
                }
                viewModel.getDownloadHelper().put(
                        viewModel.getGrocyApi().getUserSetting(option),
                        body,
                        response -> updateProductGroupSetting((ProductGroup) value),
                        this::showVolleyError
                );
                break;
            case Constants.SETTINGS.PRESETS.QUANTITY_UNIT:
                try {
                    body.put("value", ((QuantityUnit) value).getId());
                } catch (JSONException e) {
                    if (debug) Log.e(TAG, "setValue: " + e);
                }
                viewModel.getDownloadHelper().put(
                        viewModel.getGrocyApi().getUserSetting(option),
                        body,
                        response -> updateQuantityUnitSetting((QuantityUnit) value),
                        this::showVolleyError
                );
                break;
            case Constants.SETTINGS.STOCK.EXPIRING_SOON_DAYS:
                try {
                    body.put("value", Integer.parseInt((String) value));
                } catch (JSONException e) {
                    if(debug) Log.e(TAG, "setValue: " + e);
                }
                viewModel.getDownloadHelper().put(
                        viewModel.getGrocyApi().getUserSetting(option),
                        body,
                        response -> {
                            SettingEntryClick entry = binding.linearBody.findViewWithTag(option);
                            entry.setTitle((String) value);
                            sharedPrefs.edit()
                                    .putString(option, (String) value).apply();
                        },
                        this::showVolleyError
                );
        }
    }

    private void showSettingInputBottomSheet(SettingEntryClick entry) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.PREFERENCE, (String) entry.getTag());
        bundle.putString(Constants.ARGUMENT.TEXT, entry.getTitle());
        activity.showBottomSheet(new SettingInputBottomSheet(), bundle);
    }

    private void showVolleyError(VolleyError error) {
        if(debug) Log.e(TAG, "showVolleyError: " + error.getLocalizedMessage());
        if(error.networkResponse == null) {
            activity.showMessage(R.string.error_network);
        } else {
            activity.showMessage(R.string.error_undefined);
        }
    }

    private void updateTheme(boolean forceDarkMode) {
        sharedPrefs.edit().putBoolean(Constants.PREF.DARK_MODE, forceDarkMode).apply();
        AppCompatDelegate.setDefaultNightMode(forceDarkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        activity.executeOnStart();
    }

    private boolean isFeatureEnabled(String pref) {
        if(pref == null) return false;
        return sharedPrefs.getBoolean(pref, true);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
    }
}
