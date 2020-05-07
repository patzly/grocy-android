package xyz.zedler.patrick.grocy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.behavior.AppBarScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

	private final static boolean DEBUG = false;
	private final static String TAG = "AboutActivity";

	private long lastClick = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean(Constants.PREF.DARK_MODE,false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(R.layout.activity_about);

		findViewById(R.id.frame_about_back).setOnClickListener(v -> {
			if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
			lastClick = SystemClock.elapsedRealtime();
			finish();
		});

		(new AppBarScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_about,
				R.id.linear_app_bar_about,
				R.id.scroll_about,
				true
		);

		setOnClickListeners(
				R.id.linear_intro,
				R.id.linear_changelog,
				R.id.linear_developer,
				R.id.linear_license_material_components,
				R.id.linear_license_material_icons,
				R.id.linear_license_roboto,
				R.id.linear_license_volley,
				R.id.linear_license_gson,
				R.id.linear_license_xzing,
				R.id.linear_license_picasso,
				R.id.linear_license_xzing_android
		);
	}

	private void setOnClickListeners(@IdRes int... viewIds) {
		for (int viewId : viewIds) {
			findViewById(viewId).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {

		if(SystemClock.elapsedRealtime() - lastClick < 600) return;
		lastClick = SystemClock.elapsedRealtime();

		switch(v.getId()) {
			case R.id.linear_intro:
				startAnimatedIcon(R.id.image_intro);
				/*new Handler().postDelayed(
						() -> startActivity(new Intent(this, FeaturesActivity.class)),
						150
				);*/
				break;
			case R.id.linear_changelog:
				startAnimatedIcon(R.id.image_changelog);
				showTextBottomSheet("changelog", R.string.info_changelog, 0);
				break;
			case R.id.linear_developer:
				startAnimatedIcon(R.id.image_developer);
				new Handler().postDelayed(
						() -> startActivity(
								new Intent(
										Intent.ACTION_VIEW,
										Uri.parse(getString(R.string.url_developer))
								)
						), 300
				);
				break;
			case R.id.linear_license_material_components:
				startAnimatedIcon(R.id.image_license_material_components);
				showTextBottomSheet(
						"apache",
						R.string.license_material_components,
						R.string.url_material_components
				);
				break;
			case R.id.linear_license_material_icons:
				startAnimatedIcon(R.id.image_license_material_icons);
				showTextBottomSheet(
						"apache",
						R.string.license_material_icons,
						R.string.url_material_icons
				);
				break;
			case R.id.linear_license_roboto:
				startAnimatedIcon(R.id.image_license_roboto);
				showTextBottomSheet(
						"apache",
						R.string.license_roboto,
						R.string.url_roboto
				);
				break;
			case R.id.linear_license_volley:
				startAnimatedIcon(R.id.image_license_volley);
				showTextBottomSheet(
						"apache",
						R.string.license_volley,
						R.string.url_volley
				);
				break;
			case R.id.linear_license_gson:
				startAnimatedIcon(R.id.image_license_gson);
				showTextBottomSheet(
						"apache",
						R.string.license_gson,
						R.string.url_gson
				);
				break;
			case R.id.linear_license_xzing:
				startAnimatedIcon(R.id.image_license_xzing);
				showTextBottomSheet(
						"apache",
						R.string.license_xzing,
						R.string.url_zxing
				);
				break;
			case R.id.linear_license_picasso:
				startAnimatedIcon(R.id.image_license_picasso);
				showTextBottomSheet(
						"apache",
						R.string.license_picasso,
						R.string.url_picasso
				);
				break;
			case R.id.linear_license_xzing_android:
				startAnimatedIcon(R.id.image_license_xzing_android);
				showTextBottomSheet(
						"apache",
						R.string.license_xzing_android,
						R.string.url_zxing_android
				);
				break;
		}
	}

	private void showTextBottomSheet(String file, @StringRes int title, @StringRes int link) {
		BottomSheetDialogFragment bottomSheet = new TextBottomSheetDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(Constants.BOTTOM_SHEET_TEXT.TITLE, getString(title));
		bundle.putString(Constants.BOTTOM_SHEET_TEXT.FILE, file);
		if(link != 0) {
			bundle.putString(Constants.BOTTOM_SHEET_TEXT.LINK, getString(link));
		}
		bottomSheet.setArguments(bundle);
		getSupportFragmentManager().beginTransaction()
				.add(bottomSheet, bottomSheet.toString())
				.commit();
	}

	private void startAnimatedIcon(int viewId) {
		try {
			((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
		} catch (ClassCastException e) {
			if(DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
		}
	}
}
