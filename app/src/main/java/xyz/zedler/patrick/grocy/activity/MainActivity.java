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
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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
import xyz.zedler.patrick.grocy.bottomappbar.BottomAppBar;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.repository.MainRepository;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.APPEARANCE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.UnlockUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

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

  public boolean isScrollRestored = false;
  private boolean debug;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    insertConscrypt();

    // DARK MODE

    // this has to be placed before super.onCreate(savedInstanceState);
    // https://stackoverflow.com/a/53356918
    int theme = sharedPrefs.getInt(APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME);
    AppCompatDelegate.setDefaultNightMode(theme);

    // LANGUAGE

    Locale userLocale = LocaleUtil.getUserLocale(this);
    Locale.setDefault(userLocale);
    // base
    Resources resBase = getBaseContext().getResources();
    Configuration configBase = resBase.getConfiguration();
    configBase.setLocale(userLocale);
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

    super.onCreate(savedInstanceState);

    // UTILS

    clickUtil = new ClickUtil();
    netUtil = new NetUtil(this);

    // WEB

    networkReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
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
        .findFragmentById(R.id.nav_host_fragment);
    assert navHostFragment != null;
    navController = navHostFragment.getNavController();

    updateStartDestination();

    navController.addOnDestinationChangedListener((controller, dest, args) -> {
      if (isServerUrlEmpty() || dest.getId() == R.id.shoppingModeFragment
          || dest.getId() == R.id.onboardingFragment
      ) {
        binding.bottomAppBar.setVisibility(View.GONE);
        binding.fabMain.hide();
        new Handler().postDelayed(() -> setNavBarColor(R.color.background), 10);
      } else {
        binding.bottomAppBar.setVisibility(View.VISIBLE);
        setNavBarColor(R.color.primary);
      }
      setProperNavBarDividerColor(dest);
    });

    // BOTTOM APP BAR

    binding.bottomAppBar.setNavigationOnClickListener(v -> {
      if (clickUtil.isDisabled()) {
        return;
      }
      ViewUtil.startIcon(binding.bottomAppBar.getNavigationIcon());
      navController.navigate(R.id.action_global_drawerBottomSheetDialogFragment);
    });

    scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
    scrollBehavior.setUpBottomAppBar(binding.bottomAppBar);
    scrollBehavior.setUpTopScroll(R.id.fab_scroll);
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

    sharedPrefs.edit().putBoolean(PREF.PURCHASED, UnlockUtil.isKeyInstalled(this)).apply();
  }

  @Override
  protected void onDestroy() {
    if (networkReceiver != null) {
      unregisterReceiver(networkReceiver);
    }
    super.onDestroy();
  }

  @Override
  protected void attachBaseContext(Context base) {
    Locale userLocale = LocaleUtil.getUserLocale(base);
    Locale.setDefault(userLocale);
    Resources resources = base.getResources();
    Configuration configuration = resources.getConfiguration();
    configuration.setLocale(userLocale);
    resources.updateConfiguration(configuration, resources.getDisplayMetrics());

    super.attachBaseContext(base.createConfigurationContext(configuration));
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
      int newFabPosition,
      @MenuRes int newMenuId,
      Runnable onMenuChanged
  ) {
    int mode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER;
    switch (newFabPosition) {
      case Constants.FAB.POSITION.CENTER:
        if (!binding.fabMain.isShown() && !isServerUrlEmpty()) {
          binding.fabMain.show();
        }
        scrollBehavior.setTopScrollVisibility(true);
        break;
      case Constants.FAB.POSITION.END:
        if (!binding.fabMain.isShown() && !isServerUrlEmpty()) {
          binding.fabMain.show();
        }
        mode = BottomAppBar.FAB_ALIGNMENT_MODE_END;
        scrollBehavior.setTopScrollVisibility(false);
        break;
      case Constants.FAB.POSITION.GONE:
        if (binding.fabMain.isShown()) {
          binding.fabMain.hide();
        }
        scrollBehavior.setTopScrollVisibility(true);
        break;
    }
    binding.bottomAppBar.setFabAlignmentModeAndReplaceMenu(mode, newMenuId, onMenuChanged);
  }

  public void updateBottomAppBar(
      int newFabPosition,
      @MenuRes int newMenuId,
      Toolbar.OnMenuItemClickListener onMenuItemClickListener
  ) {
    int mode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER;
    switch (newFabPosition) {
      case Constants.FAB.POSITION.CENTER:
        if (!binding.fabMain.isShown() && !isServerUrlEmpty()) {
          binding.fabMain.show();
        }
        scrollBehavior.setTopScrollVisibility(true);
        break;
      case Constants.FAB.POSITION.END:
        if (!binding.fabMain.isShown() && !isServerUrlEmpty()) {
          binding.fabMain.show();
        }
        mode = BottomAppBar.FAB_ALIGNMENT_MODE_END;
        scrollBehavior.setTopScrollVisibility(false);
        break;
      case Constants.FAB.POSITION.GONE:
        if (binding.fabMain.isShown()) {
          binding.fabMain.hide();
        }
        scrollBehavior.setTopScrollVisibility(true);
        break;
    }
    binding.bottomAppBar.setFabAlignmentModeAndReplaceMenu(mode, newMenuId, null);
    binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
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
        binding.bottomAppBar.show();
        //isScrollRestored = true;
      }
    }
    hideKeyboard();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    BaseFragment currentFragment = getCurrentFragment();
    return currentFragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    BaseFragment currentFragment = getCurrentFragment();
    return currentFragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
  }

  public void navigateUp() {
    NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
        .findFragmentById(R.id.nav_host_fragment);
    assert navHostFragment != null;
    NavController navController = navHostFragment.getNavController();
    navController.navigateUp();
    binding.bottomAppBar.show();
    hideKeyboard();
  }

  public boolean isOnline() {
    return netUtil.isOnline();
  }

  public void showSnackbar(Snackbar snackbar) {
    if (binding.fabMain.isOrWillBeShown()) {
      snackbar.setAnchorView(binding.fabMain);
    } else if (binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      snackbar.setAnchorView(binding.bottomAppBar);
    }
    snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.retro_green_fg_invert));
    snackbar.show();
  }

  public void showMessage(String message) {
    Snackbar bar = Snackbar.make(binding.frameMainContainer, message, Snackbar.LENGTH_LONG);
    View v = bar.getView();
    TextView text = v.findViewById(com.google.android.material.R.id.snackbar_text);
    text.setMaxLines(4);
    showSnackbar(bar);
  }

  public void showMessage(@StringRes int message) {
    showMessage(getString(message));
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
    Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
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

  public void setNavBarColor(@ColorRes int color) {
    int nightModeFlags = getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK;
    if (Build.VERSION.SDK_INT <= VERSION_CODES.O
        && nightModeFlags != Configuration.UI_MODE_NIGHT_YES
    ) {
      color = R.color.black;
    }
    getWindow().setNavigationBarColor(ResourcesCompat.getColor(getResources(), color, null));
  }

  public void setStatusBarColor(@ColorRes int color) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
        && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
    ) {
      color = R.color.black;
    }
    getWindow().setStatusBarColor(ResourcesCompat.getColor(getResources(), color, null));
  }

  /**
   * If SDK version is 28 or higher this tints the navBarDivider.
   */
  private void setNavBarDividerColor(@ColorRes int color) {
    if (Build.VERSION.SDK_INT >= 28) {
      getWindow().setNavigationBarDividerColor(ContextCompat.getColor(this, color));
    } else if (debug) {
      Log.i(TAG, "setNavBarDividerColor: activity is null or SDK < 28");
    }
  }

  private void setProperNavBarDividerColor(NavDestination dest) {
    if (binding.bottomAppBar.getVisibility() == View.GONE) {
      int orientation = getResources().getConfiguration().orientation;
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        if (dest.getId() == R.id.loginIntroFragment
            || dest.getId() == R.id.loginRequestFragment
            || dest.getId() == R.id.loginApiFormFragment
            || dest.getId() == R.id.loginApiQrCodeFragment
        ) {
          setNavBarDividerColor(R.color.transparent);
        } else {
          new Handler().postDelayed(
              () -> setNavBarDividerColor(R.color.stroke_secondary),
              10
          );
        }
      } else {
        setNavBarDividerColor(R.color.stroke_secondary);
      }
    }
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

  @Override
  public void applyOverrideConfiguration(Configuration overrideConfiguration) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      overrideConfiguration.setLocale(LocaleUtil.getUserLocale(this));
    }
    super.applyOverrideConfiguration(overrideConfiguration);
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
}