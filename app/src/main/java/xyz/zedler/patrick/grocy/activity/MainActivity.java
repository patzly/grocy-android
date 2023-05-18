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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
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
import android.content.res.ColorStateList;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat;
import androidx.core.view.WindowInsetsAnimationCompat.Callback;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.math.MathUtils;
import com.google.android.material.snackbar.Snackbar;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.NavigationMainDirections;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomScrollBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.repository.MainRepository;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NavUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.ShortcutUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = MainActivity.class.getSimpleName();

  public ActivityMainBinding binding;
  private SharedPreferences sharedPrefs;
  private FragmentManager fragmentManager;
  private GrocyApi grocyApi;
  private MainRepository repository;
  private ClickUtil clickUtil;
  public NavUtil navUtil;
  public NetUtil netUtil;
  private BroadcastReceiver networkReceiver;
  private BottomScrollBehavior scrollBehavior;
  private SystemBarBehavior systemBarBehavior;
  public HapticUtil hapticUtil;
  private boolean runAsSuperClass;
  private boolean debug;
  private boolean wasKeyboardOpened;
  private float fabBaseY;
  private int focusedScrollOffset;

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

    // DARK MODE AND THEME

    // this has to be placed before super.onCreate(savedInstanceState);
    // https://stackoverflow.com/a/53356918
    int modeNight = PrefsUtil.getModeNight(sharedPrefs);
    AppCompatDelegate.setDefaultNightMode(modeNight);
    ResUtil.applyConfigToResources(this, modeNight);

    UiUtil.setTheme(this, sharedPrefs);

    Bundle bundleInstanceState = getIntent().getBundleExtra(ARGUMENT.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    // UTILS

    clickUtil = new ClickUtil();
    hapticUtil = new HapticUtil(this);
    hapticUtil.setEnabled(PrefsUtil.areHapticsEnabled(sharedPrefs, this));
    netUtil = new NetUtil(this, sharedPrefs, debug, TAG);
    netUtil.insertConscrypt();
    netUtil.createWebSocketClient();

    // LANGUAGE

    LocaleUtil.setLocalizedGrocyDemoInstance(this, sharedPrefs);  // set localized demo instance
    ShortcutUtil.refreshShortcuts(this);  // refresh shortcut language

    // COLOR

    ResUtil.applyColorHarmonization(this);

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

    // NAVIGATION
    fragmentManager = getSupportFragmentManager();
    navUtil = new NavUtil(this, (controller, dest, args) -> {
      if (PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
        binding.fabMain.hide();
      }
    }, sharedPrefs, TAG);
    navUtil.updateStartDestination();

    // BOTTOM APP BAR

    binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
    binding.bottomAppBar.setMenuAlignmentMode(BottomAppBar.MENU_ALIGNMENT_MODE_START);

    // Use reflection to store bottomInset in BAB manually
    // The automatic method includes IME insets which is bad behavior for BABs
    ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar, (v, insets) -> {
      int bottomInset = insets.getInsets(Type.systemBars()).bottom;
      ViewCompat.setPaddingRelative(v, 0, 0, 0, bottomInset);
      Class<?> classBottomAppBar = BottomAppBar.class;
      Object objectBottomAppBar = classBottomAppBar.cast(binding.bottomAppBar);
      Field fieldBottomInset = null;
      try {
        if (objectBottomAppBar != null && objectBottomAppBar.getClass().getSuperclass() != null) {
          fieldBottomInset = objectBottomAppBar.getClass().getDeclaredField("bottomInset");
        } else {
          Log.e(TAG, "onCreate: reflection for bottomInset not working");
        }
      } catch (NoSuchFieldException e) {
        Log.e(TAG, "onCreate: ", e);
      }
      if (fieldBottomInset != null) {
        fieldBottomInset.setAccessible(true);
        try {
          fieldBottomInset.set(objectBottomAppBar, bottomInset);
        } catch (IllegalAccessException e) {
          Log.e(TAG, "onCreate: ", e);
        }
      }
      // Calculate initial FAB y position for restoring after shifted by keyboard
      int babHeight = UiUtil.dpToPx(this, 80);
      int fabHeight = UiUtil.dpToPx(this, 56);
      int bottom = UiUtil.getDisplayHeight(this);
      fabBaseY = bottom - bottomInset - (babHeight / 2f) - (fabHeight / 2f);
      return insets;
    });
    updateBottomNavigationMenuButton();
    binding.bottomAppBar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this));
    binding.fabMainScroll.setBackgroundTintList(
        ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(this))
    );
    ViewUtil.setTooltipText(binding.fabMainScroll, R.string.action_top_scroll);

    scrollBehavior = new BottomScrollBehavior(
        this, binding.bottomAppBar, binding.fabMain, binding.fabMainScroll, binding.anchor
    );

    // IME ANIMATION

    Callback callback = new Callback(Callback.DISPATCH_MODE_STOP) {
      WindowInsetsAnimationCompat animation;
      int bottomInsetStart, bottomInsetEnd;
      float yStart, yEnd;
      int yScrollStart, yScrollEnd;

      @Override
      public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        this.animation = animation;
        if (systemBarBehavior != null) {
          bottomInsetStart = systemBarBehavior.getAdditionalBottomInset();
        }
        yStart = binding.fabMain.getY();
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          yScrollStart = scrollView.getScrollY();
        }
      }

      @NonNull
      @Override
      public BoundsCompat onStart(
          @NonNull WindowInsetsAnimationCompat animation, @NonNull BoundsCompat bounds) {
        if (systemBarBehavior != null) {
          bottomInsetEnd = systemBarBehavior.getAdditionalBottomInset();
          systemBarBehavior.setAdditionalBottomInset(bottomInsetStart);
          systemBarBehavior.refresh(false);
        }
        yEnd = binding.fabMain.getY();
        binding.fabMain.setY(yStart);
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          yScrollEnd = yScrollStart + focusedScrollOffset;
        }
        return bounds;
      }

      @NonNull
      @Override
      public WindowInsetsCompat onProgress(
          @NonNull WindowInsetsCompat insets,
          @NonNull List<WindowInsetsAnimationCompat> animations) {
        if (systemBarBehavior != null) {
          systemBarBehavior.setAdditionalBottomInset(
              (int) MathUtils.lerp(
                  bottomInsetStart, bottomInsetEnd, animation.getInterpolatedFraction()
              )
          );
          systemBarBehavior.refresh(false);
        }
        binding.fabMain.setY(MathUtils.lerp(yStart, yEnd, animation.getInterpolatedFraction()));
        // scroll offset to keep focused view visible
        ViewGroup scrollView = scrollBehavior.getScrollView();
        if (scrollView != null) {
          scrollView.setScrollY(
              (int) MathUtils.lerp(yScrollStart, yScrollEnd, animation.getInterpolatedFraction())
          );
        }
        return insets;
      }
    };
    ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorMain, (v, insets) -> {
      int bottomInset = insets.getInsets(Type.ime()).bottom;
      if (systemBarBehavior != null) {
        systemBarBehavior.setAdditionalBottomInset(bottomInset);
        systemBarBehavior.refresh(false);
      }
      if (insets.isVisible(Type.ime())) {
        wasKeyboardOpened = true;
        binding.fabMain.setTranslationY(-bottomInset - UiUtil.dpToPx(this, 16));
        int keyboardY = UiUtil.getDisplayHeight(this) - bottomInset;
        if (keyboardY < scrollBehavior.getSnackbarAnchorY()) {
          binding.anchor.setY(keyboardY);
        } else {
          scrollBehavior.updateSnackbarAnchor();
        }
        float elevation = UiUtil.dpToPx(this, 6);
        ViewCompat.setElevation(binding.fabMain, elevation);
        binding.fabMain.setCompatElevation(elevation);

        // scroll offset to keep focused view visible
        View focused = getCurrentFocus();
        if (focused != null) {
          int[] location = new int[2];
          focused.getLocationInWindow(location);
          location[1] += focused.getHeight();
          int screenHeight = UiUtil.getDisplayHeight(this);
          int bottomSpace = screenHeight - location[1];
          focusedScrollOffset = bottomInset - bottomSpace;
        } else {
          focusedScrollOffset = 0;
        }
      } else {
        binding.fabMain.setY(fabBaseY);
        scrollBehavior.updateSnackbarAnchor();
        ViewCompat.setElevation(binding.fabMain, 0);
        binding.fabMain.setCompatElevation(0);
        // If the keyboard was shown and the page was therefore scrollable
        // and the bottom bar has disappeared caused by scrolling down,
        // then the bottom bar should not stay hidden when the keyboard disappears
        if (wasKeyboardOpened) {
          wasKeyboardOpened = false;
          scrollBehavior.setBottomBarVisibility(true);
        }
        // scroll offset to keep focused view visible
        focusedScrollOffset = 0;
      }
      return insets;
    });
    ViewCompat.setWindowInsetsAnimationCallback(binding.coordinatorMain, callback);

    // UPDATE CONFIG | CHECK GROCY COMPATIBILITY
    if (!PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
      ConfigUtil.loadInfo(
          new DownloadHelper(this, TAG),
          grocyApi,
          sharedPrefs,
          () -> VersionUtil.showCompatibilityBottomSheetIfNecessary(this, sharedPrefs),
          null
      );
    }

    // Show changelog if app was updated
    VersionUtil.showVersionChangelogIfAppUpdated(this, sharedPrefs);
  }

  @Override
  protected void onDestroy() {
    if (networkReceiver != null) {
      unregisterReceiver(networkReceiver);
    }
    netUtil.closeWebSocketClient("fragment destroyed");
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    netUtil.cancelHassSessionTimer();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (runAsSuperClass) {
      return;
    }
    netUtil.createWebSocketClient();
    netUtil.resetHassSessionTimer();
    if (!sharedPrefs.contains(Constants.SETTINGS.BEHAVIOR.HAPTIC)) {
      hapticUtil.setEnabled(HapticUtil.areSystemHapticsTurnedOn(this));
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (runAsSuperClass) {
      super.attachBaseContext(base);
    } else {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(base);
      // Night mode
      int modeNight = sharedPrefs.getInt(
          SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
      );
      int uiMode = base.getResources().getConfiguration().uiMode;
      switch (modeNight) {
        case AppCompatDelegate.MODE_NIGHT_NO:
          uiMode = Configuration.UI_MODE_NIGHT_NO;
          break;
        case AppCompatDelegate.MODE_NIGHT_YES:
          uiMode = Configuration.UI_MODE_NIGHT_YES;
          break;
      }
      AppCompatDelegate.setDefaultNightMode(modeNight);
      // Apply config to resources
      Resources resources = base.getResources();
      Configuration config = resources.getConfiguration();
      config.uiMode = uiMode;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      super.attachBaseContext(base.createConfigurationContext(config));
    }
  }

  public BottomScrollBehavior getScrollBehavior() {
    return scrollBehavior;
  }

  public void setSystemBarBehavior(SystemBarBehavior behavior) {
    systemBarBehavior = behavior;
  }

  public void updateBottomAppBar(
      boolean showFab,
      @MenuRes int newMenuId,
      @Nullable OnMenuItemClickListener onMenuItemClickListener
  ) {
    UiUtil.updateBottomAppBar(binding, this, sharedPrefs, showFab,
        newMenuId, onMenuItemClickListener);
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
    ViewUtil.setTooltipText(binding.fabMain, tooltipStringId);
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
      if (!PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
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

  public boolean isOnline() {
    return netUtil.isOnline();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    getCurrentFragment().getActivityResult(requestCode, resultCode, data);
  }

  public Snackbar getSnackbar(String msg, boolean showLong) {
    return Snackbar.make(
        binding.coordinatorMain, msg, showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT
    );
  }

  public Snackbar getSnackbar(@StringRes int resId, boolean showLong) {
    return getSnackbar(getString(resId), showLong);
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.anchor);
    snackbar.setAnchorViewLayoutListenerEnabled(true);
    snackbar.show();
  }

  public void showSnackbar(String msg, boolean showLong) {
    showSnackbar(getSnackbar(msg, showLong));
  }

  public void showSnackbar(@StringRes int resId, boolean showLong) {
    showSnackbar(getSnackbar(resId, showLong));
  }

  public void showToast(@StringRes int resId, boolean showLong) {
    Toast toast = Toast.makeText(
        this, resId, showLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
    );
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      LinearLayout linearLayout = (LinearLayout) toast.getView();
      if (linearLayout != null) {
        TextView textView = (TextView) linearLayout.getChildAt(0);
        if (textView != null) {
          textView.setTypeface(ResourcesCompat.getFont(this, R.font.jost_book));
        }
      }
    }
    toast.show();
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

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    NavigationMainDirections.ActionGlobalTextDialog action
        = NavigationMainDirections.actionGlobalTextDialog();
    action.setTitle(title);
    action.setFile(file);
    if (link != 0) {
      action.setLink(link);
    }
    navUtil.navigate(action);
  }

  public void showHelpBottomSheet() {
    showTextBottomSheet(R.raw.help, R.string.title_help, 0);
  }

  public void showFeedbackBottomSheet() {
    showBottomSheet(new FeedbackBottomSheet());
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
      WindowCompat.getInsetsController(getWindow(), editText).show(Type.ime());
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

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public void updateGrocyApi() {
    grocyApi = new GrocyApi(getApplication());
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

  public void updateBottomNavigationMenuButton() {
    if (sharedPrefs.getBoolean(BEHAVIOR.SHOW_MAIN_MENU_BUTTON,
        SETTINGS_DEFAULT.BEHAVIOR.SHOW_MAIN_MENU_BUTTON)) {
      binding.bottomAppBar.setNavigationIcon(
          AppCompatResources.getDrawable(this, R.drawable.ic_round_menu_anim)
      );
      binding.bottomAppBar.setNavigationOnClickListener(v -> {
        if (clickUtil.isDisabled()) {
          return;
        }
        ViewUtil.startIcon(binding.bottomAppBar.getNavigationIcon());
        navUtil.navigate(NavigationMainDirections.actionGlobalDrawerBottomSheetDialogFragment());
      });
    } else {
      binding.bottomAppBar.setNavigationIcon(null);
    }
  }

  public void startIconAnimation(View view, boolean hasFocus) {
    if (!hasFocus) {
      return;
    }
    ViewUtil.startIcon(view);
  }

  public void setHapticEnabled(boolean enabled) {
    hapticUtil.setEnabled(enabled);
  }

  public void saveInstanceState(Bundle outState) {
    onSaveInstanceState(outState);
  }
}