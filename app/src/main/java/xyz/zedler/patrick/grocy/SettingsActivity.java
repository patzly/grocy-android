package xyz.zedler.patrick.grocy;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class SettingsActivity extends AppCompatActivity
		implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private final static boolean DEBUG = false;
	private final static String TAG = "SettingsActivity";

	private long lastClick = 0;
	private SharedPreferences sharedPrefs;
	private ImageView imageViewDark;
	private SwitchMaterial switchDark;
	private NestedScrollView nestedScrollView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.activity_settings);

		findViewById(R.id.frame_back_settings).setOnClickListener(v -> {
			if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
			lastClick = SystemClock.elapsedRealtime();
			finish();
		});

		Toolbar toolbar = findViewById(R.id.toolbar_settings);
		toolbar.setOnMenuItemClickListener((MenuItem item) -> {
			if (SystemClock.elapsedRealtime() - lastClick < 1000) return false;
			lastClick = SystemClock.elapsedRealtime();
			switch (item.getItemId()) {
				case R.id.action_about:
					//startActivity(new Intent(this, AboutActivity.class));
					break;
				case R.id.action_feedback:
					showBottomSheet(new FeedbackBottomSheetDialogFragment());
					break;
			}
			return true;
		});

		(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_settings,
				R.id.linear_app_bar_settings,
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

		setOnCheckedChangeListeners(
				R.id.switch_setting_dark_mode
		);

		setOnClickListeners(
				R.id.linear_setting_dark_mode
		);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(getIntent() != null) {
			flashView(getIntent().getIntExtra(Constants.EXTRA.FLASH_VIEW_ID, 0));
			getIntent().removeExtra(Constants.EXTRA.FLASH_VIEW_ID);
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

		if(SystemClock.elapsedRealtime() - lastClick < 400) return;
		lastClick = SystemClock.elapsedRealtime();

		switch(v.getId()) {
			case R.id.linear_setting_dark_mode:
				switchDark.setChecked(!switchDark.isChecked());
				break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
			case R.id.switch_setting_dark_mode:
				startAnimatedIcon(R.id.image_setting_dark_mode);
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
		}
	}

	private void startAnimatedIcon(int viewId) {
		try {
			((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
		} catch (ClassCastException e) {
			if(DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
		}
	}

	private void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
		getSupportFragmentManager()
				.beginTransaction()
				.add(bottomSheet, bottomSheet.toString())
				.commit();
	}
}
