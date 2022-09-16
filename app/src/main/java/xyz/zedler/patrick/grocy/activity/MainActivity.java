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

package xyz.zedler.patrick.grocy.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.conscrypt.Conscrypt;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.repository.MainRepository;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.Constants.THEME;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = MainActivity.class.getSimpleName();

  public ActivityMainBinding binding;
  private SharedPreferences sharedPrefs;
  private FragmentManager fragmentManager;
  private GrocyApi grocyApi;
  private MainRepository repository;
  private ClickUtil clickUtil;
  private NetUtil netUtil;
  private NavController navController;
  private BroadcastReceiver networkReceiver;
  private BottomAppBarRefreshScrollBehavior scrollBehavior;
  private HapticUtil hapticUtil;
  private boolean runAsSuperClass;

  public boolean isScrollRestored = false;
  private boolean debug;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    runAsSuperClass = savedInstanceState != null
        && savedInstanceState.getBoolean(ARGUMENT.RUN_AS_SUPER_CLASS, false);

    if (runAsSuperClass) {
      super.onCreate(savedInstanceState);
      return;
    }

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    insertConscrypt();

    // DARK MODE

    // this has to be placed before super.onCreate(savedInstanceState);
    // https://stackoverflow.com/a/53356918
    int modeNight = sharedPrefs.getInt(
        SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
    );
    int uiMode = getResources().getConfiguration().uiMode;
    switch (modeNight) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        uiMode = Configuration.UI_MODE_NIGHT_NO;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        uiMode = Configuration.UI_MODE_NIGHT_YES;
        break;
    }
    AppCompatDelegate.setDefaultNightMode(modeNight);

    // LANGUAGE

    Locale userLocale = LocaleUtil.getUserLocale(this);
    Locale.setDefault(userLocale);
    // base
    Resources resBase = getBaseContext().getResources();
    Configuration configBase = resBase.getConfiguration();
    configBase.setLocale(userLocale);
    configBase.uiMode = uiMode;
    resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());
    // app
    Resources resApp = getApplicationContext().getResources();
    Configuration configApp = resApp.getConfiguration();
    configApp.setLocale(userLocale);
    resApp.updateConfiguration(configApp, getResources().getDisplayMetrics());
    // set localized demo instance
    String serverUrl = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
    if (serverUrl != null && serverUrl.contains("demo.grocy.info")
            && !serverUrl.contains("test-")) {
      List<Language> languages = LocaleUtil.getLanguages(this);
      String demoDomain = null;
      for (Language language : languages) {
        if (language.getCode().equals(userLocale.getLanguage())) {
          demoDomain = language.getDemoDomain();
        }
      }
      if (demoDomain != null && !serverUrl.contains(demoDomain)) {
        serverUrl = serverUrl.replaceAll(
            "[a-z]+-?[a-z]*\\.demo\\.grocy\\.info", demoDomain
        );
        sharedPrefs.edit().putString(Constants.PREF.SERVER_URL, serverUrl).apply();
      }
    }

    switch (sharedPrefs.getString(SETTINGS.APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME)) {
      case THEME.RED:
        setTheme(R.style.Theme_Grocy_Red);
        break;
      case THEME.YELLOW:
        setTheme(R.style.Theme_Grocy_Yellow);
        break;
      case THEME.LIME:
        setTheme(R.style.Theme_Grocy_Lime);
        break;
      case THEME.GREEN:
        setTheme(R.style.Theme_Grocy_Green);
        break;
      case THEME.TEAL:
        setTheme(R.style.Theme_Grocy_Teal);
        break;
      case THEME.BLUE:
        setTheme(R.style.Theme_Grocy_Blue);
        break;
      case THEME.PURPLE:
        setTheme(R.style.Theme_Grocy_Purple);
        break;
      default:
        if (DynamicColors.isDynamicColorAvailable()) {
          DynamicColors.applyToActivityIfAvailable(
              this,
              new DynamicColorsOptions.Builder().setOnAppliedCallback(
                  activity -> HarmonizedColors.applyToContextIfAvailable(
                      this, HarmonizedColorsOptions.createMaterialDefaults()
                  )
              ).build()
          );
        } else {
          setTheme(R.style.Theme_Grocy_Green);
        }
        break;
    }

    Bundle bundleInstanceState = getIntent().getBundleExtra(ARGUMENT.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    // UTILS

    hapticUtil = new HapticUtil(this);
    clickUtil = new ClickUtil();
    netUtil = new NetUtil(this);

    // WEB

    networkReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.fragment_main_nav_host);
        assert navHostFragment != null;
        if (navHostFragment.getChildFragmentManager().getFragments().size() == 0) {
          return;
        }
        getCurrentFragment().updateConnectivity(netUtil.isOnline());
      }
    };
    registerReceiver(
        networkReceiver,
        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    );

    boolean useTor = sharedPrefs.getBoolean(NETWORK.TOR, SETTINGS_DEFAULT.NETWORK.TOR);
    if (useTor && !OrbotHelper.get(this).init()) {
      OrbotHelper.get(this).installOrbot(this);
    }

    // API

    updateGrocyApi();
    repository = new MainRepository(getApplication());

    // VIEWS

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    fragmentManager = getSupportFragmentManager();

    NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
        .findFragmentById(R.id.fragment_main_nav_host);
    assert navHostFragment != null;
    navController = navHostFragment.getNavController();

    updateStartDestination();

    navController.addOnDestinationChangedListener((controller, dest, args) -> {
      if (isServerUrlEmpty() || dest.getId() == R.id.shoppingModeFragment
          || dest.getId() == R.id.onboardingFragment
      ) {
        binding.bottomAppBar.setVisibility(View.GONE);
        binding.fabMain.hide();
      } else {
        binding.bottomAppBar.setVisibility(View.VISIBLE);
      }
    });

    // BOTTOM APP BAR

    binding.bottomAppBar.setNavigationOnClickListener(v -> {
      if (clickUtil.isDisabled()) {
        return;
      }
      ViewUtil.startIcon(binding.bottomAppBar.getNavigationIcon());
      navController.navigate(R.id.action_global_drawerBottomSheetDialogFragment);
    });
    binding.bottomAppBar.setHideOnScroll(true);
    binding.bottomAppBar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));

    // TODO: remake behavior for new BottomAppBar
    scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
    scrollBehavior.setUpBottomAppBar(new BottomAppBar(this));
    //scrollBehavior.setUpTopScroll(R.id.fab_main_scroll);
    scrollBehavior.setHideOnScroll(true);

    Runnable onSuccessConfigLoad = () -> {
      String version = sharedPrefs.getString(Constants.PREF.GROCY_VERSION, null);
      if (version == null || version.isEmpty()) {
        return;
      }
      ArrayList<String> supportedVersions = new ArrayList<>(
          Arrays.asList(getResources().getStringArray(R.array.compatible_grocy_versions))
      );
      if (supportedVersions.contains(version)) {
        return;
      }

      // If user already ignored warning, do not display again
      String ignoredVersion = sharedPrefs.getString(
          Constants.PREF.VERSION_COMPATIBILITY_IGNORED, null
      );
      if (ignoredVersion != null && ignoredVersion.equals(version)) {
        return;
      }

      Bundle bundle = new Bundle();
      bundle.putString(Constants.ARGUMENT.VERSION, version);
      bundle.putStringArrayList(Constants.ARGUMENT.SUPPORTED_VERSIONS, supportedVersions);
      showBottomSheet(new CompatibilityBottomSheet(), bundle);
    };
    if (!isServerUrlEmpty()) {
      ConfigUtil.loadInfo(
          new DownloadHelper(this, TAG),
          grocyApi,
          sharedPrefs,
          onSuccessConfigLoad,
          null
      );
    }
  }

  @Override
  protected void onDestroy() {
    if (networkReceiver != null) {
      unregisterReceiver(networkReceiver);
    }
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (runAsSuperClass) {
      return;
    }
    hapticUtil.setEnabled(HapticUtil.areSystemHapticsTurnedOn(this));
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (runAsSuperClass) {
      super.attachBaseContext(base);
    } else {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(base);
      // Language
      Locale userLocale = LocaleUtil.getUserLocale(base);
      Locale.setDefault(userLocale);
      // Night mode
      int modeNight = sharedPrefs.getInt(
          SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
      );
      int uiMode = base.getResources().getConfiguration().uiMode;
      switch (modeNight) {
        case SettingsViewModel.DARK_MODE_NO:
          uiMode = Configuration.UI_MODE_NIGHT_NO;
          break;
        case SettingsViewModel.DARK_MODE_YES:
          uiMode = Configuration.UI_MODE_NIGHT_YES;
          break;
      }
      AppCompatDelegate.setDefaultNightMode(modeNight);
      // Apply config to resources
      Resources resources = base.getResources();
      Configuration config = resources.getConfiguration();
      config.setLocale(userLocale);
      config.uiMode = uiMode;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      super.attachBaseContext(base.createConfigurationContext(config));
    }
  }

  @Override
  public void applyOverrideConfiguration(Configuration overrideConfiguration) {
    if (!runAsSuperClass && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      overrideConfiguration.setLocale(LocaleUtil.getUserLocale(this));
    }
    super.applyOverrideConfiguration(overrideConfiguration);
  }

  public void updateStartDestination() {
    NavInflater navInflater = navController.getNavInflater();
    NavGraph graph = navInflater.inflate(R.navigation.navigation_main);
    boolean introShown = sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false);
    if (!introShown) {
      graph.setStartDestination(R.id.onboardingFragment);
    } else if (isServerUrlEmpty()) {
      graph.setStartDestination(R.id.navigation_login);
    } else {
      graph.setStartDestination(R.id.overviewStartFragment);
    }
    navController.setGraph(graph);
  }

  public BottomAppBarRefreshScrollBehavior getScrollBehavior() {
    return scrollBehavior;
  }

  public void updateBottomAppBar(
      boolean showFab,
      @MenuRes int newMenuId,
      @Nullable Toolbar.OnMenuItemClickListener onMenuItemClickListener
  ) {
    scrollBehavior.setTopScrollVisibility(true);
    if (showFab) {
      if (!binding.fabMain.isShown() && !isServerUrlEmpty()) {
        binding.fabMain.show();
      }
    } else {
      if (binding.fabMain.isShown()) {
        binding.fabMain.hide();
      }
    }
    binding.bottomAppBar.replaceMenu(newMenuId);
    binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
  }

  public void updateBottomAppBar(boolean showFab, @MenuRes int newMenuId) {
    updateBottomAppBar(showFab, newMenuId, null);
  }

  public void updateFab(
      @DrawableRes int resId,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick
  ) {
    updateFab(
        ContextCompat.getDrawable(this, resId),
        tooltipStringId,
        tag,
        animated,
        onClick,
        null
    );
  }

  public void updateFab(
      @DrawableRes int resId,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick,
      Runnable onLongClick
  ) {
    updateFab(
        ContextCompat.getDrawable(this, resId),
        tooltipStringId,
        tag,
        animated,
        onClick,
        onLongClick
    );
  }

  public void updateFab(
      Drawable icon,
      @StringRes int tooltipStringId,
      String tag,
      boolean animated,
      Runnable onClick,
      Runnable onLongClick
  ) {
    replaceFabIcon(icon, tag, animated);
    binding.fabMain.setOnClickListener(v -> {
      Drawable drawable = binding.fabMain.getDrawable();
      if (drawable instanceof AnimationDrawable) {
        ViewUtil.startIcon(drawable);
      }
      onClick.run();
    });
    binding.fabMain.setOnLongClickListener(v -> {
      if (onLongClick == null) {
        return false;
      }
      Drawable drawable = binding.fabMain.getDrawable();
      if (drawable instanceof AnimationDrawable) {
        ViewUtil.startIcon(drawable);
      }
      onLongClick.run();
      return true;
    });
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      binding.fabMain.setTooltipText(getString(tooltipStringId));
    }
  }

  @Override
  public void onBackPressed() {
    BaseFragment currentFragment = getCurrentFragment();
    if (currentFragment.isSearchVisible()) {
      currentFragment.dismissSearch();
    } else {
      boolean handled = currentFragment.onBackPressed();
      if (!handled) {
        super.onBackPressed();
      }
      if (!isServerUrlEmpty()) {
        binding.bottomAppBar.performShow();
        //isScrollRestored = true;
      }
    }
    hideKeyboard();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    try {
      BaseFragment currentFragment = getCurrentFragment();
      return currentFragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    } catch (Exception e) {
      Log.e(TAG, "onKeyDown: fragmentManager or currentFragment is null");
      return false;
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    try {
      BaseFragment currentFragment = getCurrentFragment();
      return currentFragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    } catch (Exception e) {
      Log.e(TAG, "onKeyUp: fragmentManager or currentFragment is null");
      return false;
    }
  }

  public void navigate(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateUp() {
    NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
        .findFragmentById(R.id.fragment_main_nav_host);
    assert navHostFragment != null;
    NavController navController = navHostFragment.getNavController();
    navController.navigateUp();
    binding.bottomAppBar.performShow();
    hideKeyboard();
  }

  public boolean isOnline() {
    return netUtil.isOnline();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    getCurrentFragment().getActivityResult(requestCode, resultCode, data);
  }

  public Snackbar getSnackbar(@StringRes int resId, int duration) {
    return Snackbar.make(binding.coordinatorMain, getString(resId), duration);
  }

  public void showSnackbar(Snackbar snackbar) {
    if (binding.fabMain.isOrWillBeShown()) {
      snackbar.setAnchorView(binding.fabMain);
    } else if (binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      snackbar.setAnchorView(binding.bottomAppBar);
    }
    snackbar.show();
  }

  public void showSnackbar(String message) {
    showSnackbar(Snackbar.make(binding.coordinatorMain, message, Snackbar.LENGTH_LONG));
  }

  public void showSnackbar(@StringRes int resId) {
    showSnackbar(getString(resId));
  }

  public void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
    String tag = bottomSheet.toString();
    bottomSheet.show(fragmentManager, tag);
    if (debug) {
      Log.i(TAG, "showBottomSheet: " + bottomSheet);
    }
  }

  public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
    bottomSheet.setArguments(bundle);
    showBottomSheet(bottomSheet);
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title) {
    showTextBottomSheet(file, title, 0);
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.TITLE, title);
    bundle.putInt(Constants.ARGUMENT.FILE, file);
    if (link != 0) {
      bundle.putString(Constants.ARGUMENT.LINK, getString(link));
    }
    showBottomSheet(new TextBottomSheet(), bundle);
  }

  public void showChangelogBottomSheet() {
    Bundle bundle = new Bundle();
    bundle.putInt(Constants.ARGUMENT.TITLE, R.string.info_changelog);
    bundle.putInt(Constants.ARGUMENT.FILE, R.raw.changelog);
    bundle.putStringArray(
        Constants.ARGUMENT.HIGHLIGHTS,
        new String[]{"New:", "Improved:", "Fixed:"}
    );
    showBottomSheet(new TextBottomSheet(), bundle);
  }

  public SharedPreferences getSharedPrefs() {
    if (sharedPrefs != null) {
      return sharedPrefs;
    } else {
      return PreferenceManager.getDefaultSharedPreferences(this);
    }
  }

  public void showKeyboard(EditText editText) {
    new Handler().postDelayed(() -> {
      editText.requestFocus();
      ((InputMethodManager) Objects
          .requireNonNull(getSystemService(Context.INPUT_METHOD_SERVICE))
      ).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }, 100);
  }

  public void hideKeyboard() {
    ((InputMethodManager) Objects
        .requireNonNull(getSystemService(Context.INPUT_METHOD_SERVICE)))
        .hideSoftInputFromWindow(
            findViewById(android.R.id.content).getWindowToken(),
            0
        );
  }

  private boolean isServerUrlEmpty() {
    String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
    return server == null || server.isEmpty();
  }

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public void updateGrocyApi() {
    grocyApi = new GrocyApi(getApplication());
  }

  public Menu getBottomMenu() {
    return binding.bottomAppBar.getMenu();
  }

  @NonNull
  public BaseFragment getCurrentFragment() {
    Fragment navHostFragment = fragmentManager.findFragmentById(R.id.fragment_main_nav_host);
    assert navHostFragment != null;
    return (BaseFragment) navHostFragment.getChildFragmentManager().getFragments().get(0);
  }

  public void clearOfflineDataAndRestart() {
    repository.clearAllTables();
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.remove(PREF.DB_LAST_TIME_STOCK_ITEMS);
    editPrefs.remove(PREF.DB_LAST_TIME_STORES);
    editPrefs.remove(PREF.DB_LAST_TIME_LOCATIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS);
    editPrefs.remove(PREF.DB_LAST_TIME_SHOPPING_LISTS);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCT_GROUPS);
    editPrefs.remove(PREF.DB_LAST_TIME_QUANTITY_UNITS);
    editPrefs.remove(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCT_BARCODES);
    editPrefs.remove(PREF.DB_LAST_TIME_VOLATILE);
    editPrefs.remove(PREF.DB_LAST_TIME_VOLATILE_MISSING);
    editPrefs.remove(PREF.DB_LAST_TIME_TASKS);
    editPrefs.remove(PREF.DB_LAST_TIME_TASK_CATEGORIES);
    editPrefs.remove(PREF.DB_LAST_TIME_CHORES);
    editPrefs.remove(PREF.DB_LAST_TIME_CHORE_ENTRIES);
    editPrefs.remove(PREF.DB_LAST_TIME_USERS);

    editPrefs.remove(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY);
    editPrefs.remove(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME);
    editPrefs.remove(PREF.SERVER_URL);
    editPrefs.remove(PREF.HOME_ASSISTANT_SERVER_URL);
    editPrefs.remove(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN);
    editPrefs.remove(PREF.API_KEY);
    editPrefs.remove(PREF.SHOPPING_LIST_LAST_ID);
    editPrefs.remove(PREF.GROCY_VERSION);
    editPrefs.remove(PREF.CURRENT_USER_ID);
    editPrefs.apply();
    new Handler().postDelayed(() -> RestartUtil.restartApp(this), 1000);
  }

  private void replaceFabIcon(Drawable icon, String tag, boolean animated) {
    if (!tag.equals(binding.fabMain.getTag())) {
      if (animated) {
        int duration = 400;
        ValueAnimator animOut = ValueAnimator.ofInt(binding.fabMain.getImageAlpha(), 0);
        animOut.addUpdateListener(
            animation -> binding.fabMain.setImageAlpha(
                (int) animation.getAnimatedValue()
            )
        );
        animOut.setDuration(duration / 2);
        animOut.setInterpolator(new FastOutSlowInInterpolator());
        animOut.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            binding.fabMain.setImageDrawable(icon);
            binding.fabMain.setTag(tag);
            ValueAnimator animIn = ValueAnimator.ofInt(0, 255);
            animIn.addUpdateListener(
                anim -> binding.fabMain.setImageAlpha(
                    (int) (anim.getAnimatedValue())
                )
            );
            animIn.setDuration(duration / 2);
            animIn.setInterpolator(new FastOutSlowInInterpolator());
            animIn.start();
          }
        });
        animOut.start();
      } else {
        binding.fabMain.setImageDrawable(icon);
        binding.fabMain.setTag(tag);
      }
    } else {
      if (debug) {
        Log.i(TAG, "replaceFabIcon: not replaced, tags are identical");
      }
    }
  }

  @Deprecated
  public void setStatusBarColor(@ColorRes int color) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
        && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
    ) {
      color = R.color.black;
    }
    //getWindow().setStatusBarColor(ResourcesCompat.getColor(getResources(), color, null));
  }

  public void startIconAnimation(View view, boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    ViewUtil.startIcon(view);
  }

  public void executeOnStart() {
    onStart();
  }

  private void insertConscrypt() {
    Security.insertProviderAt(Conscrypt.newProvider(), 1);

    try {
      Conscrypt.Version version = Conscrypt.version();
      if (debug) {
        Log.i(TAG, "insertConscrypt: Using Conscrypt/" + version.major() + "."
            + version.minor() + "." + version.patch() + " for TLS");
      }
      SSLEngine engine = SSLContext.getDefault().createSSLEngine();
      if (debug) {
        Log.i(TAG, "Enabled protocols: "
            + Arrays.toString(engine.getEnabledProtocols()) + " }");
      }
      if (debug) {
        Log.i(TAG, "Enabled ciphers: "
            + Arrays.toString(engine.getEnabledCipherSuites()) + " }");
      }
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "insertConscrypt: NoSuchAlgorithmException");
      Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
    }
  }

  public void performHapticClick() {
    hapticUtil.click();
  }

  public void performHapticHeavyClick() {
    hapticUtil.heavyClick();
  }
}