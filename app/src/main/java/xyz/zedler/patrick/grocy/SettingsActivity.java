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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.SettingInputBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ClickUtil;
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

	private ArrayList<Location> locations = new ArrayList<>();
	private ArrayList<ProductGroup> productGroups = new ArrayList<>();
	private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();

	private ClickUtil clickUtil = new ClickUtil();
	private SharedPreferences sharedPrefs;
	private ImageView imageViewDark;
	private SwitchMaterial switchDark, switchFoodFacts, switchListIndicator;
	private NestedScrollView nestedScrollView;
	private TextView
			textViewExpiringSoonDays,
			textViewAmountPurchase,
			textViewAmountConsume;

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

		nestedScrollView = findViewById(R.id.scroll_settings);

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
				R.id.switch_setting_list_indicator
		);

		setOnClickListeners(
				R.id.linear_setting_dark_mode,
				R.id.linear_setting_open_food_facts,
				R.id.linear_setting_logout,
				R.id.linear_setting_list_indicator,
				R.id.linear_setting_expiring_soon_days,
				R.id.linear_setting_default_amount_purchase,
				R.id.linear_setting_default_amount_consume
		);

		// VALUES

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

		hideDisabledFeatures();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(getIntent() != null) {
			flashView(getIntent().getIntExtra(Constants.EXTRA.FLASH_VIEW_ID, 0));
			getIntent().removeExtra(Constants.EXTRA.FLASH_VIEW_ID);
		}
	}

	private void hideDisabledFeatures() {
		if(!sharedPrefs.getBoolean(Constants.PREF.FEATURE_FLAG_SHOPPINGLIST, true)) {
			findViewById(R.id.linear_setting_list_indicator).setVisibility(View.GONE);
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

	private void flashView(int viewId) {
		if(viewId == 0 || findViewById(viewId) == null) return;
		long duration = 3000;
		View view = findViewById(viewId);
		nestedScrollView.requestChildFocus(view, view);
		ValueAnimator valueAnimator = ValueAnimator.ofArgb(
				ContextCompat.getColor(this, R.color.transparent),
				ContextCompat.getColor(this, R.color.secondary_translucent)
		);
		valueAnimator.addUpdateListener(
				animation -> view.setBackgroundTintList(
						new ColorStateList(
								new int[][] {new int[] {android.R.attr.state_enabled}},
								new int[] {(int) valueAnimator.getAnimatedValue()})
				)
		);
		valueAnimator.setDuration(duration / 6).setRepeatCount(5);
		valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
		valueAnimator.start();
		new Handler().postDelayed(
				() -> view.setBackgroundTintList(null),
				duration + 100
		);
	}

	@Override
	public void onClick(View v) {
		if(clickUtil.isDisabled()) return;

		switch(v.getId()) {
			case R.id.linear_setting_dark_mode:
				switchDark.setChecked(!switchDark.isChecked());
				break;
			case R.id.linear_setting_open_food_facts:
				switchFoodFacts.setChecked(!switchFoodFacts.isChecked());
				break;
			case R.id.linear_setting_logout:
				Bundle bundle = null;
				if(isDemo()) bundle = new Bundle();
				// empty bundle for indicating demo type
				showBottomSheet(new LogoutBottomSheetDialogFragment(), bundle);
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
			case R.id.switch_setting_list_indicator:
				JSONObject body = new JSONObject();
				try {
					body.put("value", switchListIndicator.isChecked());
				} catch (JSONException e) {
					if(DEBUG) Log.e(TAG, "onCheckedChanged: list indicator: " + e);
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
							showMessage(getString(R.string.msg_error));
							if(DEBUG) Log.e(TAG, "onCheckedChanged: list indicator: " + error);
						}
				);
				break;
		}
	}

	public void setExpiringSoonDays(String days) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", Integer.parseInt(days));
		} catch (JSONException e) {
			if(DEBUG) Log.e(TAG, "setExpiringSoonDays: " + e);
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
					showMessage(getString(R.string.msg_error));
					if(DEBUG) Log.e(TAG, "setExpiringSoonDays: " + error);
				}
		);
	}

	public void setAmountPurchase(String amount) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", amount);
		} catch (JSONException e) {
			if(DEBUG) Log.e(TAG, "setAmountPurchase: " + e);
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
					showMessage(getString(R.string.msg_error));
					if(DEBUG) Log.e(TAG, "setAmountPurchase: " + error);
				}
		);
	}

	public void setAmountConsume(String amount) {
		JSONObject body = new JSONObject();
		try {
			body.put("value", amount);
		} catch (JSONException e) {
			if(DEBUG) Log.e(TAG, "setAmountConsume: " + e);
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
					showMessage(getString(R.string.msg_error));
					if(DEBUG) Log.e(TAG, "setAmountConsume: " + error);
				}
		);
	}

	private void showMessage(String msg) {
		Snackbar.make(findViewById(R.id.scroll_settings), msg, Snackbar.LENGTH_SHORT).show();
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
}
