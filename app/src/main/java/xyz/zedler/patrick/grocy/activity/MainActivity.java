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
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
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
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.math.MathUtils;
import com.google.android.material.snackbar.Snackbar;
import dev.gustavoavila.websocketclient.WebSocketClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.conscrypt.Conscrypt;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.BuildConfig;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.NavigationMainDirections;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomScrollBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.RecipeImportFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.repository.MainRepository;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.HapticUtil;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.ShortcutUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class MainActivity extends AppCompatActivity {

  private final static String TAG = MainActivity.class.getSimpleName();

  public ActivityMainBinding binding;
  private SharedPreferences sharedPrefs;
  private FragmentManager fragmentManager;
  private GrocyApi grocyApi;
  private WebSocketClient webSocketClient;
  private MainRepository repository;
  private ClickUtil clickUtil;
  private NetUtil netUtil;
  private Locale locale;
  private NavController navController;
  private BroadcastReceiver networkReceiver;
  private BottomScrollBehavior scrollBehavior;
  private SystemBarBehavior systemBarBehavior;
  private HapticUtil hapticUtil;
  private Timer hassSessionTimer;
  private TimerTask hassSessionTimerTask;
  private int hassWebsocketIdCounter;
  private boolean runAsSuperClass;
  private boolean debug;
  private boolean wasKeyboardOpened;
  private float fabBaseY;

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

    // APPLY CONFIG TO RESOURCES

    // base
    Resources resBase = getBaseContext().getResources();
    Configuration configBase = resBase.getConfiguration();
    configBase.uiMode = uiMode;
    resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());
    // app
    Resources resApp = getApplicationContext().getResources();
    Configuration configApp = resApp.getConfiguration();
    resApp.updateConfiguration(configApp, getResources().getDisplayMetrics());

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
      case THEME.TURQUOISE:
        setTheme(R.style.Theme_Grocy_Turquoise);
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
    hapticUtil.setEnabled(
        sharedPrefs.getBoolean(
            Constants.SETTINGS.BEHAVIOR.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(this)
        )
    );

    clickUtil = new ClickUtil();
    netUtil = new NetUtil(this);

    // LANGUAGE

    locale = LocaleUtil.getLocale();

    // refresh shortcut language
    ShortcutUtil.refreshShortcuts(this);

    // set localized demo instance
    String serverUrl = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
    if (serverUrl != null && serverUrl.contains("demo.grocy.info")
        && !serverUrl.contains("test-")) {
      List<Language> languages = LocaleUtil.getLanguages(this);
      String demoDomain = null;
      for (Language language : languages) {
        if (language.getCode().equals(locale.getLanguage())) {
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

    createWebSocketClient();

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
      if (isServerUrlEmpty()) {
        binding.fabMain.hide();
      }
    });

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

      @Override
      public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        this.animation = animation;
        if (systemBarBehavior != null) {
          bottomInsetStart = systemBarBehavior.getAdditionalBottomInset();
        }
        yStart = binding.fabMain.getY();
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
      }
      return insets;
    });
    ViewCompat.setWindowInsetsAnimationCallback(binding.coordinatorMain, callback);

    // SUPPORTED GROCY VERSIONS

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

    // Show changelog if app was updated
    int versionNew = BuildConfig.VERSION_CODE;
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showChangelogBottomSheet, 900
      );
    }

    // Extract recipe URL from the Intent
    Intent intent = getIntent();
    String url = intent.getStringExtra("url");
    if (url != null) {
      navigateFragment(
          R.id.recipeImportFragment,
          new RecipeImportFragmentArgs.Builder().setUrl(url).build().toBundle()
      );
    }
  }

  @Override
  protected void onDestroy() {
    if (networkReceiver != null) {
      unregisterReceiver(networkReceiver);
    }
    if (webSocketClient != null) {
      webSocketClient.close(0, 0, "fragment destroyed");
    }
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    if (hassSessionTimer != null) {
      hassSessionTimer.cancel();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (runAsSuperClass) {
      return;
    }
    createWebSocketClient();
    resetHassSessionTimer();
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
    // Handler with postDelayed is necessary for workaround of issue #552
    new Handler().postDelayed(() -> {
      if (showFab && !binding.fabMain.isShown() && !isServerUrlEmpty()) {
        binding.fabMain.show();
      } else if (!showFab && binding.fabMain.isShown()) {
        binding.fabMain.hide();
      }

      Drawable overflowIcon = binding.bottomAppBar.getOverflowIcon();

      // IF ANIMATIONS DISABLED
      if (!UiUtil.areAnimationsEnabled(this)) {
        binding.bottomAppBar.replaceMenu(newMenuId);
        Menu menu = binding.bottomAppBar.getMenu();
        int tint = ResUtil.getColorAttr(this, R.attr.colorOnSurfaceVariant);
        for (int i = 0; i < menu.size(); i++) {
          MenuItem item = menu.getItem(i);
          if (item.getIcon() != null) {
            item.getIcon().mutate();
            item.getIcon().setAlpha(255);
            item.getIcon().setTint(tint);
          }
        }
        if (overflowIcon != null && overflowIcon.isVisible()) {
          overflowIcon.setAlpha(255);
          overflowIcon.setTint(tint);
        }
        binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
        return;
      }

      long iconFadeOutDuration = 150;
      long iconFadeInDuration = 300;

      int alphaFrom = 255;
      // get better start value if animation was not finished yet
      if (binding.bottomAppBar.getMenu() != null
          && binding.bottomAppBar.getMenu().size() > 0
          && binding.bottomAppBar.getMenu().getItem(0) != null
          && binding.bottomAppBar.getMenu().getItem(0).getIcon() != null) {
        alphaFrom = binding.bottomAppBar.getMenu().getItem(0).getIcon().getAlpha();
      }
      ValueAnimator animatorFadeOut = ValueAnimator.ofInt(alphaFrom, 0);
      animatorFadeOut.addUpdateListener(animation -> {
        for (int i = 0; i < binding.bottomAppBar.getMenu().size(); i++) {
          MenuItem item = binding.bottomAppBar.getMenu().getItem(i);
          if (item.getIcon() != null && item.isVisible()) {
            item.getIcon().setAlpha((int) animation.getAnimatedValue());
          }
        }
        if (overflowIcon != null && overflowIcon.isVisible()) {
          overflowIcon.setAlpha((int) animation.getAnimatedValue());
        }
      });
      animatorFadeOut.setDuration(iconFadeOutDuration);
      animatorFadeOut.setInterpolator(new FastOutSlowInInterpolator());
      animatorFadeOut.start();

      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        binding.bottomAppBar.replaceMenu(newMenuId);

        int iconIndex = 0;
        int overflowCount = 0;
        int tint = ResUtil.getColorAttr(this, R.attr.colorOnSurfaceVariant);
        for (int i = 0; i < binding.bottomAppBar.getMenu().size(); i++) {
          MenuItem item = binding.bottomAppBar.getMenu().getItem(i);
          if (item.getIcon() == null || !item.isVisible()) {
            if (item.isVisible()) {
              overflowCount++;
            }
            continue;
          }
          iconIndex++;
          int index = iconIndex;
          item.getIcon().mutate();
          item.getIcon().setTint(tint);
          item.getIcon().setAlpha(0);
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rect bounds = item.getIcon().copyBounds();
            int top = bounds.top;
            int offset = UiUtil.dpToPx(this, 12);
            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
            animator.addUpdateListener(animation -> {
              bounds.offsetTo(
                  0,
                  (int) (top + (float) animation.getAnimatedValue() * offset)
              );
              item.getIcon().setBounds(bounds);
              item.getIcon().setAlpha(255 - (int) ((float) animation.getAnimatedValue() * 255));
              item.getIcon().invalidateSelf();
            });
            animator.setDuration(iconFadeInDuration - index * 50L);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
          }, index * 90L);
        }
        if (overflowCount > 0) {
          Drawable overflowIconNew = binding.bottomAppBar.getOverflowIcon();
          if (overflowIconNew == null || !overflowIconNew.isVisible()) {
            return;
          }
          iconIndex++;
          int index = iconIndex;
          overflowIconNew.setTint(tint);
          overflowIconNew.setAlpha(0);
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rect bounds = overflowIconNew.copyBounds();
            int top = bounds.top;
            int offset = UiUtil.dpToPx(this, 12);
            ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
            animator.addUpdateListener(animation -> {
              bounds.offsetTo(
                  0,
                  (int) (top + (float) animation.getAnimatedValue() * offset)
              );
              overflowIconNew.setBounds(bounds);
              overflowIconNew.setAlpha(255 - (int) ((float) animation.getAnimatedValue() * 255));
              overflowIconNew.invalidateSelf();
            });
            animator.setDuration(iconFadeInDuration - index * 50L);
            animator.setInterpolator(new FastOutSlowInInterpolator());
            animator.start();
          }, index * 90L);
        }
        binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
      }, iconFadeOutDuration);
    }, 10);
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

  // NAVIGATION

  public NavOptions.Builder getNavOptionsBuilderFragmentFadeOrSlide(boolean slideVertically) {
    if (UiUtil.areAnimationsEnabled(this)) {
      boolean useSliding = getSharedPrefs().getBoolean(
          Constants.SETTINGS.APPEARANCE.USE_SLIDING,
          Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
      );
      if (useSliding) {
        if (slideVertically) {
          return new NavOptions.Builder()
              .setEnterAnim(R.anim.slide_in_up)
              .setPopExitAnim(R.anim.slide_out_down)
              .setExitAnim(R.anim.slide_no);
        } else {
          return new NavOptions.Builder()
              .setEnterAnim(R.anim.slide_from_end)
              .setPopExitAnim(R.anim.slide_to_end)
              .setPopEnterAnim(R.anim.slide_from_start)
              .setExitAnim(R.anim.slide_to_start);
        }
      } else {
        return new NavOptions.Builder()
            .setEnterAnim(R.anim.enter_end_fade)
            .setExitAnim(R.anim.exit_start_fade)
            .setPopEnterAnim(R.anim.enter_start_fade)
            .setPopExitAnim(R.anim.exit_end_fade);
      }
    } else {
      return new NavOptions.Builder()
          .setEnterAnim(R.anim.fade_in_a11y)
          .setExitAnim(R.anim.fade_out_a11y)
          .setPopEnterAnim(R.anim.fade_in_a11y)
          .setPopExitAnim(R.anim.fade_out_a11y);
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

  public void navigate(NavDirections directions, @NonNull NavOptions navOptions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigate(NavDirections directions, @NonNull Navigator.Extras navigatorExtras) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions, navigatorExtras);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateUp() {
    if (navController == null) {
      NavHostFragment navHostFragment = (NavHostFragment) fragmentManager.findFragmentById(
          R.id.fragment_main_nav_host
      );
      assert navHostFragment != null;
      navController = navHostFragment.getNavController();
    }
    navController.navigateUp();
    binding.bottomAppBar.performShow();
    hideKeyboard();
  }

  public void navigateFragment(@IdRes int destination) {
    navigateFragment(destination, (Bundle) null);
  }

  public void navigateFragment(@IdRes int destination, @Nullable Bundle arguments) {
    if (navController == null ) {
      Log.e(TAG, "navigateFragment: controller is null");
      return;
    }
    try {
      navController.navigate(
          destination, arguments, getNavOptionsBuilderFragmentFadeOrSlide(true).build()
      );
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: ", e);
    }
  }

  public void navigateFragment(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigateFragment: controller or direction is null");
      return;
    }
    try {
      navController.navigate(
          directions, getNavOptionsBuilderFragmentFadeOrSlide(true).build()
      );
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: " + directions, e);
    }
  }

  public void navigateFragment(@IdRes int destination, @NonNull NavOptions navOptions) {
    if (navController == null ) {
      Log.e(TAG, "navigateFragment: controller is null");
      return;
    }
    try {
      navController.navigate(destination, null, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: ", e);
    }
  }

  public void navigateDeepLink(@NonNull Uri uri, boolean slideVertically) {
    if (navController == null ) {
      Log.e(TAG, "navigateDeepLink: controller is null");
      return;
    }
    try {
      navController.navigate(uri, getNavOptionsBuilderFragmentFadeOrSlide(slideVertically).build());
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateDeepLink: ", e);
    }
  }

  public void navigateDeepLink(@NonNull Uri uri, @NonNull NavOptions navOptions) {
    if (navController == null ) {
      Log.e(TAG, "navigateDeepLink: controller is null");
      return;
    }
    try {
      navController.navigate(uri, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateDeepLink: ", e);
    }
  }

  public void navigateDeepLink(String uri) {
    navigateDeepLink(Uri.parse(uri), true);
  }

  public void navigateDeepLink(@StringRes int uri) {
    navigateDeepLink(Uri.parse(getString(uri)), true);
  }

  public void navigateDeepLink(@StringRes int uri, @NonNull Bundle args) {
    navigateDeepLink(getUriWithArgs(getString(uri), args), true);
  }

  public static Uri getUriWithArgs(@NonNull String uri, @NonNull Bundle argsBundle) {
    String[] parts = uri.split("\\?");
    if (parts.length == 1) {
      return Uri.parse(uri);
    }
    String linkPart = parts[0];
    String argsPart = parts[parts.length - 1];
    String[] pairs = argsPart.split("&");
    StringBuilder finalDeepLink = new StringBuilder(linkPart + "?");
    for (int i = 0; i <= pairs.length - 1; i++) {
      String pair = pairs[i];
      String key = pair.split("=")[0];
      Object valueBundle = argsBundle.get(key);
      if (valueBundle == null) {
        continue;
      }
      try {
        String encoded;
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
          encoded = URLEncoder.encode(valueBundle.toString(), StandardCharsets.UTF_8);
        } else {
          encoded = URLEncoder.encode(valueBundle.toString(), "UTF-8");
        }
        finalDeepLink.append(key).append("=").append(encoded);
      } catch (Throwable ignore) {
      }
      if (i != pairs.length - 1) {
        finalDeepLink.append("&");
      }
    }
    return Uri.parse(finalDeepLink.toString());
  }

  public void restartToApply(long delay, @NonNull Bundle bundle) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      onSaveInstanceState(bundle);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        finish();
      }
      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra(ARGUMENT.INSTANCE_STATE, bundle);
      startActivity(intent);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        finish();
      }
      overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
    }, delay);
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

  public void showTextBottomSheet(@RawRes int file, @StringRes int title) {
    showTextBottomSheet(file, title, 0);
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    NavigationMainDirections.ActionGlobalTextDialog action
        = NavigationMainDirections.actionGlobalTextDialog();
    action.setTitle(title);
    action.setFile(file);
    if (link != 0) {
      action.setLink(link);
    }
    navigate(action);
  }

  public void showChangelogBottomSheet() {
    NavigationMainDirections.ActionGlobalTextDialog action
        = NavigationMainDirections.actionGlobalTextDialog();
    action.setTitle(R.string.info_changelog);
    action.setFile(R.raw.changelog);
    action.setHighlights(new String[]{"New:", "Improved:", "Fixed:"});
    navigate(action);
  }

  public void showHelpBottomSheet() {
    showTextBottomSheet(R.raw.help, R.string.title_help);
  }

  public void showFeedbackBottomSheet() {
    showBottomSheet(new FeedbackBottomSheet());
  }

  public Locale getLocale() {
    return locale;
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

  public void createWebSocketClient() {
    String hassLongLivedAccessToken = sharedPrefs
        .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
    String hassServerUrl = sharedPrefs.getString(PREF.HOME_ASSISTANT_SERVER_URL, null);
    if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()
        || hassServerUrl == null || hassServerUrl.isEmpty()) {
      return;
    }
    if (webSocketClient != null) {
      webSocketClient.close(0, 0, "recreate websocket client");
    }

    URI uri;
    try {
      String hassWebSocketUrl = hassServerUrl
          .replaceFirst("https", "wss")
          .replaceFirst("http", "ws");
      uri = new URI(hassWebSocketUrl + "/api/websocket");
    }
    catch (URISyntaxException e) {
      e.printStackTrace();
      return;
    }

    hassWebsocketIdCounter = 1;
    webSocketClient = new WebSocketClient(uri) {
      @Override
      public void onOpen() {}
      @Override
      public void onPingReceived(byte[] data) {}
      @Override
      public void onPongReceived(byte[] data) {}
      @Override
      public void onBinaryReceived(byte[] data) {}

      @Override
      public void onTextReceived(String message) {
        if (message.contains("auth_required")) {
          String hassLongLivedAccessToken = sharedPrefs
              .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
          if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()) return;
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "auth");
            jsonObject.put("access_token", hassLongLivedAccessToken);
            webSocketClient.send(jsonObject.toString());
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else if (message.contains("auth_ok")) {
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "supervisor/api");
            jsonObject.put("endpoint", "/ingress/session");
            jsonObject.put("method", "post");
            jsonObject.put("id", hassWebsocketIdCounter);
            webSocketClient.send(jsonObject.toString());
            hassWebsocketIdCounter++;
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else if (message.contains("result")) {
          try {
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.getBoolean("success")) {
              if (jsonObject.has("result")
                  && jsonObject.getJSONObject("result").has("session")) {
                sharedPrefs.edit().putString(
                    PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                    jsonObject.getJSONObject("result").getString("session")
                ).apply();
              } else {
                if (debug) Log.i(TAG, "onTextReceived: " + message);
              }
            } else {
              Log.e(TAG, "createWebSocketClient: onTextReceived: " + message);
            }


          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else {
          if (debug) Log.i(TAG, "createWebSocketClient: onTextReceived: " + message);
        }
      }

      @Override
      public void onException(Exception e) {
        Log.e(TAG, "createWebSocketClient: onException: " + e.getMessage());
      }

      @Override
      public void onCloseReceived(int reason, String description) {
        if (debug) Log.i(TAG, "createWebSocketClient: onCloseReceived: " + description);
        new Handler().postDelayed(() -> webSocketClient.connect(), 5000);
      }
    };

    webSocketClient.setConnectTimeout(10000);
    webSocketClient.setReadTimeout(60000);
    webSocketClient.enableAutomaticReconnection(5000);
    webSocketClient.connect();
  }

  public void resetHassSessionTimer() {
    String hassLongLivedAccessToken = sharedPrefs
        .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
    if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()) {
      return;
    }
    hassSessionTimer = new Timer();
    if (hassSessionTimerTask != null) {
      hassSessionTimerTask.cancel();
    }
    hassSessionTimerTask = new TimerTask() {
      @Override
      public void run() {
        if (debug) {
          Log.i(TAG, "Home Assistant session: validate session token");
        }
        if (webSocketClient != null) {
          String sessionToken = sharedPrefs
              .getString(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "supervisor/api");
            jsonObject.put("endpoint", "/ingress/validate_session");
            jsonObject.put("method", "post");
            JSONObject innerJsonObject = new JSONObject();
            innerJsonObject.put("session", sessionToken);
            jsonObject.put("data", innerJsonObject);
            jsonObject.put("id", hassWebsocketIdCounter);
            webSocketClient.send(jsonObject.toString());
            hassWebsocketIdCounter++;
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        }
      }
    };
    hassSessionTimer.schedule(hassSessionTimerTask, 60 * 1000L, 60 * 1000L);
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
        navController.navigate(R.id.action_global_drawerBottomSheetDialogFragment);
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

  public void setHapticEnabled(boolean enabled) {
    hapticUtil.setEnabled(enabled);
  }

  public void performHapticClick() {
    hapticUtil.click();
  }

  public void performHapticHeavyClick() {
    hapticUtil.heavyClick();
  }
}