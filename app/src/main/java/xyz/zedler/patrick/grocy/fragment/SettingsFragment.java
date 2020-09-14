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
        if(category.equals(Constants.SETTINGS.SERVER.class.getSimpleName())) {
            showCategoryServer();
        } else if(category.equals(Constants.SETTINGS.APPEARANCE.class.getSimpleName())) {
            showCategoryAppearance();
        } else if(category.equals(Constants.SETTINGS.SCANNER.class.getSimpleName())) {
            showCategoryScanner();
        } else if(category.equals(Constants.SETTINGS.STOCK.class.getSimpleName())) {
            showCategoryStock();
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

        setForPreviousFragment(Constants.ARGUMENT.ANIMATED, false);

        queue.start();
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
                R.string.setting_dark_mode,
                R.string.setting_dark_mode_description,
                R.drawable.ic_round_dark_mode_off_anim,
                R.drawable.ic_round_dark_mode_on_anim,
                Constants.PREF.DARK_MODE,
                isChecked -> {
                    AppCompatDelegate.setDefaultNightMode(isChecked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    );
                    activity.executeOnStart();
                }
        ));
    }

    private void showCategoryScanner() {
        binding.appBarTitle.setText(R.string.category_barcode_scanner);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.SCANNER.FOOD_FACTS,
                R.string.setting_open_food_facts,
                R.string.setting_open_food_facts_description,
                R.drawable.ic_round_barcode,
                Constants.PREF.FOOD_FACTS
        ));
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.SCANNER.FRONT_CAM,
                R.string.setting_use_front_cam,
                R.string.setting_use_front_cam_description,
                R.drawable.ic_round_camera_front,
                Constants.PREF.USE_FRONT_CAM
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
                    Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
                    String.valueOf(5)
            );
            if(days == null || days.isEmpty() || days.equals("null")) {
                days = String.valueOf(5);
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
                    Constants.SETTINGS.STOCK.DISPLAY_DOTS,
                    R.string.setting_list_indicator,
                    R.string.setting_list_indicator_description,
                    R.drawable.ic_round_shopping_list_long,
                    sharedPrefs.getBoolean(
                            Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK, false
                    ),
                    isChecked -> {
                        JSONObject body = new JSONObject();
                        try {
                            body.put("value", isChecked);
                        } catch (JSONException e) {
                            if(debug) Log.e(TAG, "showCategoryStock: list indicator: " + e);
                        }
                        viewModel.getDownloadHelper().put(
                                viewModel.getGrocyApi().getUserSetting(
                                        Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK
                                ),
                                body,
                                response -> sharedPrefs.edit().putBoolean(
                                        Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
                                        isChecked
                                ).apply(),
                                error -> {
                                    sharedPrefs.edit().putBoolean(
                                            Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
                                            isChecked
                                    ).apply();
                                    showVolleyError(error);
                                }
                        );
                    }
            ));
        }
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
                            Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
                            -1
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new Location(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefLocationId);
                    bundle.putString(Constants.ARGUMENT.OPTION, (String) entry.getTag());
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
                            Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                            -1
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new ProductGroup(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCT_GROUPS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefProductGroupId);
                    bundle.putString(Constants.ARGUMENT.OPTION, (String) entry.getTag());
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
                            Constants.PREF.PRODUCT_PRESETS_QU_ID,
                            -1
                    );
                    Bundle bundle = new Bundle();
                    arrayList.add(
                            0,
                            new QuantityUnit(-1, getString(R.string.subtitle_none_selected))
                    );
                    bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, arrayList);
                    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, prefQuId);
                    bundle.putString(Constants.ARGUMENT.OPTION, (String) entry.getTag());
                    activity.showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
                }, this::showVolleyError).perform(UUID.randomUUID().toString())
        ));
        queue.append(viewModel.getDownloadHelper().getLocations(arrayList -> {
            int prefLocationId = sharedPrefs.getInt(
                    Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
                    -1
            );
            updateLocationSetting(viewModel.getLocation(arrayList, prefLocationId));
        }));
        queue.append(viewModel.getDownloadHelper().getProductGroups(arrayList -> {
            int prefProductGroupId = sharedPrefs.getInt(
                    Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                    -1
            );
            updateProductGroupSetting(viewModel.getProductGroup(arrayList, prefProductGroupId));
        }));
        queue.append(viewModel.getDownloadHelper().getQuantityUnits(arrayList -> {
            int prefQuId = sharedPrefs.getInt(Constants.PREF.PRODUCT_PRESETS_QU_ID, -1);
            updateQuantityUnitSetting(viewModel.getQuantityUnit(arrayList, prefQuId));
        }));
    }

    private void showCategoryDebugging() {
        binding.appBarTitle.setText(R.string.category_debugging);
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
                R.string.setting_debug,
                R.string.setting_debug_description,
                R.drawable.ic_round_bug_report_anim,
                Constants.PREF.DEBUG
        ));
        binding.linearBody.addView(new SettingEntrySwitch(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.ENABLE_INFO_LOGS,
                R.string.setting_info_logs,
                Constants.PREF.SHOW_INFO_LOGS
        ));
        binding.linearBody.addView(new SettingEntryClick(
                requireContext(),
                Constants.SETTINGS.DEBUGGING.SHOW_LOGS,
                R.string.title_logs,
                R.string.setting_logs_description,
                entry -> navigate(SettingsFragmentDirections.actionSettingsFragmentToLogFragment())
        ));
    }

    public void updateLocationSetting(Location location) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.LOCATION
        );
        if(entry == null) return;
        if(location != null) {
            entry.setTitle(location.getName());
            sharedPrefs.edit().putInt(
                    Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
                    location.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.PREF.PRODUCT_PRESETS_LOCATION_ID, -1).apply();
        }
    }

    public void updateProductGroupSetting(ProductGroup productGroup) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.PRODUCT_GROUP
        );
        if(entry == null) return;
        if(productGroup != null) {
            entry.setTitle(productGroup.getName());
            sharedPrefs.edit().putInt(
                    Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                    productGroup.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID, -1).apply();
        }
    }

    public void updateQuantityUnitSetting(QuantityUnit quantityUnit) {
        SettingEntryClick entry = binding.linearBody.findViewWithTag(
                Constants.SETTINGS.PRESETS.QUANTITY_UNIT
        );
        if(entry == null) return;
        if(quantityUnit != null) {
            entry.setTitle(quantityUnit.getName());
            sharedPrefs.edit().putInt(
                    Constants.PREF.PRODUCT_PRESETS_QU_ID,
                    quantityUnit.getId()
            ).apply();
        } else {
            entry.setTitle(R.string.subtitle_none_selected);
            sharedPrefs.edit().putInt(Constants.PREF.PRODUCT_PRESETS_QU_ID, -1).apply();
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
                        viewModel.getGrocyApi().getUserSetting(
                                Constants.PREF.PRODUCT_PRESETS_LOCATION_ID
                        ),
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
                        viewModel.getGrocyApi().getUserSetting(
                                Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID
                        ),
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
                        viewModel.getGrocyApi().getUserSetting(
                                Constants.PREF.PRODUCT_PRESETS_QU_ID
                        ),
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
                        viewModel.getGrocyApi().getUserSetting(
                                Constants.PREF.STOCK_EXPIRING_SOON_DAYS
                        ),
                        body,
                        response -> {
                            SettingEntryClick entry = binding.linearBody.findViewWithTag(option);
                            entry.setTitle((String) value);
                            sharedPrefs.edit()
                                    .putString(
                                            Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
                                            (String) value
                                    ).apply();
                        },
                        this::showVolleyError
                );
        }
    }

    private void showSettingInputBottomSheet(SettingEntryClick entry) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.OPTION, (String) entry.getTag());
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

    private boolean isFeatureEnabled(String pref) {
        if(pref == null) return false;
        return sharedPrefs.getBoolean(pref, true);
    }
}
