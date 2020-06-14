package xyz.zedler.patrick.grocy;

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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.RestartBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.SettingInputBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class SettingsActivity extends AppCompatActivity
		implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private final static boolean DEBUG = false;
	private final static String TAG = "SettingsActivity";

	private GrocyApi grocyApi;
	private RequestQueue requestQueue;
	private WebRequest request;
	private Gson gson = new Gson();

	private ArrayList<Location> locations = new ArrayList<>();
	private ArrayList<ProductGroup> productGroups = new ArrayList<>();
	private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();

	private ClickUtil clickUtil = new ClickUtil();
	private SharedPreferences sharedPrefs;
	private ImageView imageViewDark;
	private SwitchMaterial switchDark, switchFoodFacts, switchDebug, switchListIndicator;
	private TextView
			textViewExpiringSoonDays,
			textViewUpdateInterval,
			textViewAmountPurchase,
			textViewAmountConsume,
			textViewDefaultLocation,
			textViewDefaultProductGroup,
			textViewDefaultQuantityUnit;

	private int presetLocationId;
	private int presetProductGroupId;
	private int presetQuantityUnitId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);

		// PREFERENCES

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// WEB REQUESTS

		requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
		request = new WebRequest(requestQueue);

		// API

		grocyApi = new GrocyApi(this);

		// INITIALIZE VIEWS

		findViewById(R.id.frame_settings_back).setOnClickListener(v -> {
			if(clickUtil.isDisabled()) return;
			finish();
		});

		Toolbar toolbar = findViewById(R.id.toolbar_settings);
		toolbar.setOnMenuItemClickListener((MenuItem item) -> {
			if(clickUtil.isDisabled()) return false;
			switch (item.getItemId()) {
				case R.id.action_about:
					IconUtil.start(item);
					startActivity(new Intent(this, AboutActivity.class));
					break;
				case R.id.action_feedback:
					showBottomSheet(new FeedbackBottomSheetDialogFragment(), null);
					break;
			}
			return true;
		});

		(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_settings,
				R.id.linear_settings_app_bar,
				R.id.scroll_settings,
				true
		);

		switchDark = findViewById(R.id.switch_setting_dark_mode);
		switchDark.setChecked(sharedPrefs.getBoolean(Constants.PREF.DARK_MODE, false));
		imageViewDark = findViewById(R.id.image_setting_dark_mode);
		imageViewDark.setImageResource(
				sharedPrefs.getBoolean(Constants.PREF.DARK_MODE, false)
						? R.drawable.ic_round_dark_mode_off_anim
						: R.drawable.ic_round_dark_mode_on_anim
		);

		switchFoodFacts = findViewById(R.id.switch_setting_open_food_facts);
		switchFoodFacts.setChecked(
				sharedPrefs.getBoolean(Constants.PREF.FOOD_FACTS, false)
		);

		switchDebug = findViewById(R.id.switch_setting_debug);
		switchDebug.setChecked(sharedPrefs.getBoolean(Constants.PREF.DEBUG, false));

		switchListIndicator = findViewById(R.id.switch_setting_list_indicator);
		switchListIndicator.setChecked(
				sharedPrefs.getBoolean(
						Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
						true
				)
		);

		setOnCheckedChangeListeners(
				R.id.switch_setting_dark_mode,
				R.id.switch_setting_open_food_facts,
				R.id.switch_setting_debug,
				R.id.switch_setting_list_indicator
		);

		setOnClickListeners(
				R.id.linear_setting_reload_config,
				R.id.linear_setting_logout,
				R.id.linear_setting_dark_mode,
				R.id.linear_setting_open_food_facts,
				R.id.linear_setting_debug,
				R.id.linear_setting_list_indicator,
				R.id.linear_setting_expiring_soon_days,
				R.id.linear_setting_shopping_mode_update_interval,
				R.id.linear_setting_default_amount_purchase,
				R.id.linear_setting_default_amount_consume,
				R.id.linear_setting_default_location,
				R.id.linear_setting_default_product_group,
				R.id.linear_setting_default_quantity_unit
		);

		// VALUES

		((TextView) findViewById(R.id.text_setting_grocy_version)).setText(
				sharedPrefs.getString(
						Constants.PREF.GROCY_VERSION,
						getString(R.string.date_unknown)
				)
		);
		findViewById(R.id.text_setting_grocy_version_incompatible).setVisibility(
				isVersionCompatible() ? View.GONE : View.VISIBLE
		);

		String days = sharedPrefs.getString(
				Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
				String.valueOf(5)
		);
		textViewExpiringSoonDays = findViewById(R.id.text_setting_expiring_soon_days);
		textViewExpiringSoonDays.setText(
				days == null || days.isEmpty() || days.equals("null")
						? String.valueOf(5)
						: days
		);

		int updateInterval = sharedPrefs.getInt(
				Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
				10
		);
		textViewUpdateInterval = findViewById(R.id.text_setting_shopping_mode_update_interval);
		textViewUpdateInterval.setText(
				updateInterval == 0
						? getString(R.string.setting_sync_off)
						: String.valueOf(updateInterval)
		);

		String amountPurchase = sharedPrefs.getString(
				Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
				null
		);
		textViewAmountPurchase = findViewById(R.id.text_setting_default_amount_purchase);
		textViewAmountPurchase.setText(
				amountPurchase == null
						|| amountPurchase.isEmpty()
						|| amountPurchase.equals("null")
				? getString(R.string.setting_empty_value)
				: amountPurchase
		);

		String amountConsume = sharedPrefs.getString(
				Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
				null
		);
		textViewAmountConsume = findViewById(R.id.text_setting_default_amount_consume);
		textViewAmountConsume.setText(
				amountConsume == null
						|| amountConsume.isEmpty()
						|| amountConsume.equals("null")
						? getString(R.string.setting_empty_value)
						: amountConsume
		);

		presetLocationId = sharedPrefs.getInt(
				Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
				-1
		);
		textViewDefaultLocation = findViewById(R.id.text_setting_default_location);
		if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
			textViewDefaultLocation.setText(null);
		} else if(presetLocationId == -1) {
			textViewDefaultLocation.setText(getString(R.string.subtitle_none));
		} else {
			downloadLocations(
					() -> {
						Location location = getLocation(presetLocationId);
						if(location != null) {
							textViewDefaultLocation.setText(location.getName());
						} else {
							textViewDefaultLocation.setText(
									getString(R.string.subtitle_none)
							);
						}
					},
					() -> textViewDefaultLocation.setText(getString(R.string.setting_not_loaded))
			);
		}

		presetProductGroupId = sharedPrefs.getInt(
				Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
				-1
		);
		textViewDefaultProductGroup = findViewById(R.id.text_setting_default_product_group);
		if(presetProductGroupId == -1) {
			textViewDefaultProductGroup.setText(getString(R.string.subtitle_none));
		} else {
			downloadProductGroups(
					() -> {
						ProductGroup productGroup = getProductGroup(presetProductGroupId);
						if(productGroup != null) {
							textViewDefaultProductGroup.setText(productGroup.getName());
						} else {
							textViewDefaultProductGroup.setText(getString(R.string.subtitle_none));
						}
					},
					() -> textViewDefaultProductGroup.setText(
							getString(R.string.setting_not_loaded)
					)
			);
		}

		presetQuantityUnitId = sharedPrefs.getInt(
				Constants.PREF.PRODUCT_PRESETS_QU_ID,
				-1
		);
		textViewDefaultQuantityUnit = findViewById(R.id.text_setting_default_quantity_unit);
		if(presetQuantityUnitId == -1) {
			textViewDefaultQuantityUnit.setText(getString(R.string.subtitle_none));
		} else {
			downloadQuantityUnits(
					() -> {
						QuantityUnit quantityUnit = getQuantityUnit(presetQuantityUnitId);
						if(quantityUnit != null) {
							textViewDefaultQuantityUnit.setText(quantityUnit.getName());
						} else {
							textViewDefaultQuantityUnit.setText(getString(R.string.subtitle_none));
						}
					},
					() -> textViewDefaultQuantityUnit.setText(
							getString(R.string.setting_not_loaded)
					)
			);
		}

		hideDisabledFeatures();
	}

	private void hideDisabledFeatures() {
		if(isFeatureDisabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
			findViewById(R.id.linear_setting_list_indicator).setVisibility(View.GONE);
		}
		if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
			findViewById(R.id.linear_setting_expiring_soon_days).setVisibility(View.GONE);
		}
		if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
			findViewById(R.id.linear_setting_default_location).setVisibility(View.GONE);
		}
		if(isFeatureDisabled(Constants.PREF.FEATURE_SHOPPING_LIST)
				&& isFeatureDisabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)
		) {
			findViewById(R.id.text_setting_stock_overview_header).setVisibility(View.GONE);
		}
	}

	private void setOnClickListeners(@IdRes int... viewIds) {
		for (int viewId : viewIds) {
			findViewById(viewId).setOnClickListener(this);
		}
	}

	private void setOnCheckedChangeListeners(@IdRes int... viewIds) {
		for (int viewId : viewIds) {
			((CompoundButton) findViewById(viewId)).setOnCheckedChangeListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		if(clickUtil.isDisabled()) return;

		switch(v.getId()) {
			case R.id.linear_setting_reload_config:
				IconUtil.start(this, R.id.image_setting_reload_config);
				ConfigUtil.loadInfo(
						requestQueue,
						grocyApi,
						sharedPrefs,
						() -> showBottomSheet(new RestartBottomSheetDialogFragment(), null),
						this::showErrorMessage
				);
				break;
			case R.id.linear_setting_logout:
				Bundle bundle = null;
				if(isDemo()) bundle = new Bundle();
				// empty bundle for indicating demo type
				showBottomSheet(new LogoutBottomSheetDialogFragment(), bundle);
				break;
			case R.id.linear_setting_dark_mode:
				switchDark.setChecked(!switchDark.isChecked());
				break;
			case R.id.linear_setting_open_food_facts:
				switchFoodFacts.setChecked(!switchFoodFacts.isChecked());
				break;
			case R.id.linear_setting_debug:
				switchDebug.setChecked(!switchDebug.isChecked());
				break;
			case R.id.linear_setting_expiring_soon_days:
				IconUtil.start(this, R.id.image_setting_expiring_soon_days);
				Bundle bundleExpiringSoonDays = new Bundle();
				bundleExpiringSoonDays.putString(
						Constants.ARGUMENT.TYPE,
						Constants.PREF.STOCK_EXPIRING_SOON_DAYS
				);
				bundleExpiringSoonDays.putString(
						Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
						textViewExpiringSoonDays.getText().toString()
				);
				showBottomSheet(
						new SettingInputBottomSheetDialogFragment(),
						bundleExpiringSoonDays
				);
				break;
			case R.id.linear_setting_list_indicator:
				switchListIndicator.setChecked(!switchListIndicator.isChecked());
				break;
			case R.id.linear_setting_shopping_mode_update_interval:
				IconUtil.start(this, R.id.image_setting_shopping_mode_update_interval);
				Bundle bundleUpdateInterval = new Bundle();
				bundleUpdateInterval.putString(
						Constants.ARGUMENT.TYPE,
						Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL
				);
				bundleUpdateInterval.putString(
						Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
						String.valueOf(
								sharedPrefs.getInt(
										Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
										10
								)
						)
				);
				showBottomSheet(
						new SettingInputBottomSheetDialogFragment(),
						bundleUpdateInterval
				);
				break;
			case R.id.linear_setting_default_amount_purchase:
				IconUtil.start(this, R.id.image_setting_default_amount_purchase);
				Bundle bundleAmountPurchase = new Bundle();
				bundleAmountPurchase.putString(
						Constants.ARGUMENT.TYPE,
						Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT
				);
				bundleAmountPurchase.putString(
						Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
						sharedPrefs.getString(
								Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
								null
						)
				);
				showBottomSheet(
						new SettingInputBottomSheetDialogFragment(),
						bundleAmountPurchase
				);
				break;
			case R.id.linear_setting_default_amount_consume:
				IconUtil.start(this, R.id.image_setting_default_amount_consume);
				Bundle bundleAmountConsume = new Bundle();
				bundleAmountConsume.putString(
						Constants.ARGUMENT.TYPE,
						Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT
				);
				bundleAmountConsume.putString(
						Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
						sharedPrefs.getString(
								Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
								null
						)
				);
				showBottomSheet(
						new SettingInputBottomSheetDialogFragment(),
						bundleAmountConsume
				);
				break;
			case R.id.linear_setting_default_location:
				if(locations.isEmpty()) {
					downloadLocations(
							this::showLocationsBottomSheet,
							() -> showMessage(getString(R.string.setting_not_loaded))
					);
				} else {
					showLocationsBottomSheet();
				}
				break;
			case R.id.linear_setting_default_product_group:
				if(productGroups.isEmpty()) {
					downloadProductGroups(
							this::showProductGroupsBottomSheet,
							() -> showMessage(getString(R.string.setting_not_loaded))
					);
				} else {
					showProductGroupsBottomSheet();
				}
				break;
			case R.id.linear_setting_default_quantity_unit:
				if(quantityUnits.isEmpty()) {
					downloadQuantityUnits(
							this::showQuantityUnitsBottomSheet,
							() -> showMessage(getString(R.string.setting_not_loaded))
					);
				} else {
					showQuantityUnitsBottomSheet();
				}
				break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.switch_setting_dark_mode:
				IconUtil.start(this, R.id.image_setting_dark_mode);
				sharedPrefs.edit().putBoolean(Constants.PREF.DARK_MODE, isChecked).apply();
				new Handler().postDelayed(() -> {
					imageViewDark.setImageResource(
							isChecked
									? R.drawable.ic_round_dark_mode_off_anim
									: R.drawable.ic_round_dark_mode_on_anim

					);
					AppCompatDelegate.setDefaultNightMode(
							isChecked
									? AppCompatDelegate.MODE_NIGHT_YES
									: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
					);
					onStart();
				}, 300);
				break;
			case R.id.switch_setting_open_food_facts:
				sharedPrefs.edit().putBoolean(
						Constants.PREF.FOOD_FACTS,
						switchFoodFacts.isChecked()
				).apply();
				break;
			case R.id.switch_setting_debug:
				IconUtil.start(this, R.id.image_setting_debug);
				sharedPrefs.edit().putBoolean(
						Constants.PREF.DEBUG,
						switchDebug.isChecked()
				).apply();
				break;
			case R.id.switch_setting_list_indicator:
				JSONObject body = new JSONObject();
				try {
					body.put("value", switchListIndicator.isChecked());
				} catch (JSONException e) {
					Log.e(TAG, "onCheckedChanged: list indicator: " + e);
				}
				request.put(
						grocyApi.getUserSetting(Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK),
						body,
						response -> sharedPrefs.edit().putBoolean(
								Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
								switchListIndicator.isChecked()
						).apply(),
						error -> {
							sharedPrefs.edit().putBoolean(
									Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
									switchListIndicator.isChecked()
							).apply();
							showErrorMessage();
							Log.e(TAG, "onCheckedChanged: list indicator: " + error);
						}
				);
				break;
		}
	}

	private void downloadLocations(
			OnResponseListener responseListener,
			OnErrorListener errorListener
	) {
		request.get(
				grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
				response -> {
					try {
						locations = gson.fromJson(
								response,
								new TypeToken<List<Location>>(){}.getType()
						);
						responseListener.onResponse();
					} catch (JsonSyntaxException e) {
						errorListener.onError();
					}
				},
				error -> errorListener.onError()
		);
	}

	private void showLocationsBottomSheet() {
		Bundle bundleLocations = new Bundle();
		ArrayList<Location> tmpLocations = new ArrayList<>(locations);
		tmpLocations.add(0, new Location(-1, getString(R.string.subtitle_none)));
		bundleLocations.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, tmpLocations);
		bundleLocations.putInt(Constants.ARGUMENT.SELECTED_ID, presetLocationId);
		showBottomSheet(new LocationsBottomSheetDialogFragment(), bundleLocations);
	}

	private Location getLocation(int id) {
		for(Location location : locations) {
			if(location.getId() == id) {
				return location;
			}
		} return null;
	}

	public void setLocation(int locationId) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", locationId);
		} catch (JSONException e) {
			Log.e(TAG, "setLocation: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.PRODUCT_PRESETS_LOCATION_ID),
				body,
				response -> {
					Location location = getLocation(locationId);
					if(location == null) {
						textViewDefaultLocation.setText(getString(R.string.subtitle_none));
					} else {
						textViewDefaultLocation.setText(location.getName());
					}
					sharedPrefs.edit().putInt(
							Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
							locationId
					).apply();
					presetLocationId = locationId;
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setLocation: " + error);
				}
		);
	}

	private void downloadProductGroups(
			OnResponseListener responseListener,
			OnErrorListener errorListener
	) {
		request.get(
				grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
				response -> {
					try {
						productGroups = gson.fromJson(
								response,
								new TypeToken<List<ProductGroup>>(){}.getType()
						);
						responseListener.onResponse();
					} catch (JsonSyntaxException e) {
						errorListener.onError();
					}
				},
				error -> errorListener.onError()
		);
	}

	private void showProductGroupsBottomSheet() {
		Bundle bundleProductGroups = new Bundle();
		ArrayList<ProductGroup> tmpProductGroups = new ArrayList<>(productGroups);
		tmpProductGroups.add(0, new ProductGroup(-1, getString(R.string.subtitle_none)));
		bundleProductGroups.putParcelableArrayList(
				Constants.ARGUMENT.PRODUCT_GROUPS,
				tmpProductGroups
		);
		bundleProductGroups.putInt(Constants.ARGUMENT.SELECTED_ID, presetProductGroupId);
		showBottomSheet(new ProductGroupsBottomSheetDialogFragment(), bundleProductGroups);
	}

	private ProductGroup getProductGroup(int id) {
		for(ProductGroup productGroup : productGroups) {
			if(productGroup.getId() == id) {
				return productGroup;
			}
		} return null;
	}

	public void setProductGroup(int productGroupId) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", productGroupId);
		} catch (JSONException e) {
			Log.e(TAG, "setProductGroup: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID),
				body,
				response -> {
					ProductGroup productGroup = getProductGroup(productGroupId);
					if(productGroup == null) {
						textViewDefaultProductGroup.setText(getString(R.string.subtitle_none));
					} else {
						textViewDefaultProductGroup.setText(productGroup.getName());
					}
					sharedPrefs.edit().putInt(
							Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
							productGroupId
					).apply();
					presetProductGroupId = productGroupId;
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setProductGroup: " + error);
				}
		);
	}

	private void downloadQuantityUnits(
			OnResponseListener responseListener,
			OnErrorListener errorListener
	) {
		request.get(
				grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
				response -> {
					try {
						quantityUnits = gson.fromJson(
								response,
								new TypeToken<List<QuantityUnit>>(){}.getType()
						);
						responseListener.onResponse();
					} catch (JsonSyntaxException e) {
						errorListener.onError();
					}
				},
				error -> errorListener.onError()
		);
	}

	private void showQuantityUnitsBottomSheet() {
		Bundle bundleLocations = new Bundle();
		ArrayList<QuantityUnit> tmpQuantityUnits = new ArrayList<>(quantityUnits);
		tmpQuantityUnits.add(0, new QuantityUnit(-1, getString(R.string.subtitle_none)));
		bundleLocations.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, tmpQuantityUnits);
		bundleLocations.putInt(Constants.ARGUMENT.SELECTED_ID, presetQuantityUnitId);
		showBottomSheet(new QuantityUnitsBottomSheetDialogFragment(), bundleLocations);
	}

	private QuantityUnit getQuantityUnit(int id) {
		for(QuantityUnit quantityUnit : quantityUnits) {
			if(quantityUnit.getId() == id) {
				return quantityUnit;
			}
		} return null;
	}

	public void setQuantityUnit(int quantityUnitId) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", quantityUnitId);
		} catch (JSONException e) {
			Log.e(TAG, "setQuantityUnit: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.PRODUCT_PRESETS_QU_ID),
				body,
				response -> {
					QuantityUnit quantityUnit = getQuantityUnit(quantityUnitId);
					if(quantityUnit == null) {
						textViewDefaultQuantityUnit.setText(getString(R.string.subtitle_none));
					} else {
						textViewDefaultQuantityUnit.setText(quantityUnit.getName());
					}
					sharedPrefs.edit().putInt(
							Constants.PREF.PRODUCT_PRESETS_QU_ID,
							quantityUnitId
					).apply();
					presetQuantityUnitId = quantityUnitId;
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setQuantityUnit: " + error);
				}
		);
	}

	public void setExpiringSoonDays(String days) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", Integer.parseInt(days));
		} catch (JSONException e) {
			Log.e(TAG, "setExpiringSoonDays: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.STOCK_EXPIRING_SOON_DAYS),
				body,
				response -> {
					textViewExpiringSoonDays.setText(days);
					sharedPrefs.edit()
							.putString(Constants.PREF.STOCK_EXPIRING_SOON_DAYS, days)
							.apply();
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setExpiringSoonDays: " + error);
				}
		);
	}

	public void setAmountPurchase(String amount) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", amount);
		} catch (JSONException e) {
			Log.e(TAG, "setAmountPurchase: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT),
				body,
				response -> {
					String amountFormatted = amount == null || amount.isEmpty()
							? null
							: NumUtil.trim(NumUtil.stringToDouble(amount));
					textViewAmountPurchase.setText(
							amountFormatted != null
									? amountFormatted
									: getString(R.string.setting_empty_value)
					);
					sharedPrefs.edit().putString(
							Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
							amountFormatted
					).apply();
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setAmountPurchase: " + error);
				}
		);
	}

	public void setAmountConsume(String amount) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", amount);
		} catch (JSONException e) {
			Log.e(TAG, "setAmountConsume: " + e);
		}
		request.put(
				grocyApi.getUserSetting(Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT),
				body,
				response -> {
					String amountFormatted = amount == null || amount.isEmpty()
							? null
							: NumUtil.trim(NumUtil.stringToDouble(amount));
					textViewAmountConsume.setText(
							amountFormatted != null
									? amountFormatted
									: getString(R.string.setting_empty_value)
					);
					sharedPrefs.edit().putString(
							Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
							amountFormatted
					).apply();
				},
				error -> {
					showErrorMessage();
					Log.e(TAG, "setAmountConsume: " + error);
				}
		);
	}

	public void setUpdateInterval(String seconds) {
		int secondsFormatted = seconds == null || seconds.trim().isEmpty()
				? 0
				: (int) Double.parseDouble(seconds);
		textViewUpdateInterval.setText(
				secondsFormatted == 0
						? getString(R.string.setting_sync_off)
						: String.valueOf(secondsFormatted)
		);
		sharedPrefs.edit().putInt(
				Constants.PREF.SHOPPING_MODE_UPDATE_INTERVAL,
				secondsFormatted
		).apply();
	}

	private boolean isVersionCompatible() {
		ArrayList<String> supportedVersions = new ArrayList<>(
				Arrays.asList(
						getResources().getStringArray(R.array.compatible_grocy_versions)
				)
		);
		return supportedVersions.contains(
				sharedPrefs.getString(
						Constants.PREF.GROCY_VERSION,
						getString(R.string.date_unknown)
				)
		);
	}

	private void showMessage(String msg) {
		Snackbar.make(findViewById(R.id.scroll_settings), msg, Snackbar.LENGTH_SHORT).show();
	}

	private void showErrorMessage() {
		showMessage(getString(R.string.msg_error));
	}

	private boolean isDemo() {
		String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
		return server != null && server.contains("grocy.info");
	}

	private void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
		if(bundle != null) bottomSheet.setArguments(bundle);
		getSupportFragmentManager()
				.beginTransaction()
				.add(bottomSheet, bottomSheet.toString())
				.commit();
	}

	public void showKeyboard(EditText editText) {
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(
				editText,
				InputMethodManager.SHOW_IMPLICIT
		);
	}

	public void hideKeyboard() {
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
				.hideSoftInputFromWindow(
						findViewById(android.R.id.content).getWindowToken(),
						0
				);
	}

	private boolean isFeatureDisabled(String pref) {
		if(pref == null) return false;
		return !sharedPrefs.getBoolean(pref, true);
	}

	public interface OnResponseListener {
		void onResponse();
	}

	public interface OnErrorListener {
		void onError();
	}
}
