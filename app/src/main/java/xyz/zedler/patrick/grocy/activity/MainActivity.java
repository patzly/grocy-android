package xyz.zedler.patrick.grocy.activity;

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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import xyz.zedler.patrick.grocy.NavGraphDirections;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.bottomappbar.BottomAppBar;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.LoginFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.UpdateInfoBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public ActivityMainBinding binding;
    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private GrocyApi grocyApi;
    private ClickUtil clickUtil;
    private NetUtil netUtil;
    private NavController navController;
    private BroadcastReceiver networkReceiver;
    private BottomAppBarRefreshScrollBehavior scrollBehavior;

    private boolean debug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // DARK MODE

        // this has to be placed before super.onCreate(savedInstanceState);
        // https://stackoverflow.com/a/53356918
        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean(Constants.PREF.DARK_MODE, false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );

        super.onCreate(savedInstanceState);

        // UTILS

        clickUtil = new ClickUtil();
        netUtil = new NetUtil(this);

        // WEB

        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getCurrentFragment().updateConnectivity(netUtil.isOnline());
            }
        };
        registerReceiver(
                networkReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        );

        // API

        grocyApi = new GrocyApi(this);

        // VIEWS

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.frameMainDemo.setOnClickListener(
                // bottomSheet only checks if bundle is != null, then it's of type demo
                v -> showBottomSheet(new LogoutBottomSheet(), new Bundle())
        );

        fragmentManager = getSupportFragmentManager();

        NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
                .findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();

        navController.addOnDestinationChangedListener((controller, dest, args) -> {
            // conditional navigation
            boolean newDestIsLogin = false;
            boolean introShown = sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false);
            if(!introShown && isServerUrlEmpty()) {
                controller.navigate(R.id.action_global_loginFragment);
                controller.navigate(R.id.action_global_featuresFragment);
            } else if(!introShown) {
                controller.navigate(R.id.action_global_featuresFragment);
            } else if((dest.getId() != R.id.loginFragment
                    && dest.getId() != R.id.aboutFragment
                    && dest.getId() != R.id.featuresFragment
                    && dest.getId() != R.id.settingsFragment)
                    && isServerUrlEmpty()
            ) {
                controller.navigate(R.id.action_global_loginFragment);
                newDestIsLogin = true;
            }

            if(isServerUrlEmpty()) {
                binding.bottomAppBar.setVisibility(View.GONE);
                binding.fabMain.hide();
                setNavBarColor(R.color.background);
            } else {
                binding.bottomAppBar.setVisibility(View.VISIBLE);
                setNavBarColor(R.color.primary);
            }

            setProperNavBarDividerColor(dest, newDestIsLogin);
        });

        // BOTTOM APP BAR

        binding.bottomAppBar.setNavigationOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            IconUtil.start(binding.bottomAppBar.getNavigationIcon());
            navController.navigate(R.id.action_global_drawerBottomSheetDialogFragment);
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
        scrollBehavior.setUpBottomAppBar(binding.bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        if(!isServerUrlEmpty()) {
            ConfigUtil.loadInfo(
                    new DownloadHelper(this, TAG),
                    grocyApi,
                    sharedPrefs,
                    () -> {
                        String version = sharedPrefs.getString(Constants.PREF.GROCY_VERSION, "");
                        if(version.isEmpty()) return;
                        ArrayList<String> supportedVersions = new ArrayList<>(
                                Arrays.asList(
                                        getResources().getStringArray(
                                                R.array.compatible_grocy_versions
                                        )
                                )
                        );
                        if(supportedVersions.contains(version)) {
                            if(!isDemo() && !sharedPrefs.getBoolean(
                                    Constants.PREF.UPDATE_INFO_READ,
                                    false
                            )) showBottomSheet(new UpdateInfoBottomSheet(), null);
                            return;
                        }

                        // If user already ignored warning, do not display again
                        String ignoredVersion = sharedPrefs.getString(
                                Constants.PREF.VERSION_COMPATIBILITY_IGNORED, null
                        );
                        if(ignoredVersion != null && ignoredVersion.equals(version)) return;

                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.VERSION, version);
                        bundle.putStringArrayList(
                                Constants.ARGUMENT.SUPPORTED_VERSIONS,
                                supportedVersions
                        );
                        showBottomSheet(
                                new CompatibilityBottomSheet(),
                                bundle
                        );
                    },
                    null
            );
            handleShortcutAction();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(networkReceiver != null) unregisterReceiver(networkReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.REQUEST.SCAN_BATCH
                && resultCode == Activity.RESULT_OK
                && data != null
        ) {
            NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
                    .findFragmentById(R.id.nav_host_fragment);
            assert navHostFragment != null;
            NavController navController = navHostFragment.getNavController();
            /*navController.navigate((new NavDirections);
            replaceFragment(
                    Constants.UI.MISSING_BATCH_ITEMS,
                    data.getParcelableArrayListExtra(Constants.ARGUMENT.BUNDLE),
                    true
            );*/
        }
    }

    public void handleShortcutAction() {
        if(getIntent() == null || getIntent().getAction() == null) return;

        // no animation for shortcut fragments
        Bundle bundleNoAnim = new Bundle();
        bundleNoAnim.putBoolean(Constants.ARGUMENT.ANIMATED, false);

        switch (getIntent().getAction()) {
            case Constants.SHORTCUT_ACTION.CONSUME:
                navController.navigate(
                        NavGraphDirections.actionGlobalScanBatchFragment(Constants.ACTION.CONSUME)
                );
                break;
            case Constants.SHORTCUT_ACTION.PURCHASE:
                navController.navigate(
                        NavGraphDirections.actionGlobalScanBatchFragment(Constants.ACTION.PURCHASE)
                );
                break;
            case Constants.SHORTCUT_ACTION.SHOPPING_LIST:
                //replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                break;
            case Constants.SHORTCUT_ACTION.ADD_ENTRY:
                // TODO: Preselect last used shopping list!!
                //replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                Bundle bundleCreate = new Bundle();
                bundleCreate.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE);
                bundleCreate.putBoolean(Constants.ARGUMENT.ANIMATED, false);
                //replaceFragment(Constants.UI.SHOPPING_LIST_ITEM_EDIT, bundleCreate, false);
                break;
            case Constants.SHORTCUT_ACTION.SHOPPING_MODE:
                //replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                startActivity(new Intent(this, ShoppingActivity.class));
                break;
        }
        getIntent().setAction(null);
    }

    public BottomAppBarRefreshScrollBehavior getScrollBehavior() {
        return scrollBehavior;
    }

    public void updateBottomAppBar(
            int newFabPosition,
            @MenuRes int newMenuId,
            boolean animated,
            Runnable onMenuChanged
    ) {
        updateBottomAppBar(newFabPosition, newMenuId, onMenuChanged);
    }

    public void updateBottomAppBar(
            int newFabPosition,
            @MenuRes int newMenuId,
            Runnable onMenuChanged
    ) {
        int mode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER;
        switch (newFabPosition) {
            case Constants.FAB.POSITION.CENTER:
                if(!binding.fabMain.isShown()) binding.fabMain.show();
                scrollBehavior.setTopScrollVisibility(true);
                break;
            case Constants.FAB.POSITION.END:
                if(!binding.fabMain.isShown()) binding.fabMain.show();
                mode = BottomAppBar.FAB_ALIGNMENT_MODE_END;
                scrollBehavior.setTopScrollVisibility(false);
                break;
            case Constants.FAB.POSITION.GONE:
                if(binding.fabMain.isShown()) binding.fabMain.hide();
                scrollBehavior.setTopScrollVisibility(true);
                break;
        }
        binding.bottomAppBar.setFabAlignmentModeAndReplaceMenu(mode, newMenuId, onMenuChanged);
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
                onClick
        );
    }

    public void updateFab(
            Drawable icon,
            @StringRes int tooltipStringId,
            String tag,
            boolean animated,
            Runnable onClick
    ) {
        replaceFabIcon(icon, tag, animated);
        binding.fabMain.setOnClickListener(v -> {
            IconUtil.start(binding.fabMain.getDrawable());
            onClick.run();
        });
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.fabMain.setTooltipText(getString(tooltipStringId));
        }
    }

    @Override
    public void onBackPressed() {
        BaseFragment currentFragment = getCurrentFragment();
        if(currentFragment.isSearchVisible()) {
            currentFragment.dismissSearch();
        } else {
            boolean handled = currentFragment.onBackPressed();
            if(!handled) super.onBackPressed();
            if(!(currentFragment instanceof LoginFragment)) binding.bottomAppBar.show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        BaseFragment currentFragment = getCurrentFragment();
        return currentFragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    public void navigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
                .findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        navController.navigateUp();
        binding.bottomAppBar.show();
    }

    public void dismissFragment() {}

    public boolean isOnline() {
        return netUtil.isOnline();
    }

    public void showSnackbar(Snackbar snackbar) {
        if(binding.fabMain.isOrWillBeShown()) {
            snackbar.setAnchorView(binding.fabMain);
        } else if(binding.bottomAppBar.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(binding.bottomAppBar);
        }
        snackbar.show();
    }

    public void showMessage(@StringRes int message) {
        showSnackbar(
                Snackbar.make(binding.frameMainContainer, getString(message), Snackbar.LENGTH_LONG)
        );
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
        String tag = bottomSheet.toString();
        bottomSheet.show(fragmentManager, tag);
        if(debug) Log.i(TAG, "showBottomSheet: " + bottomSheet.toString());
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        bottomSheet.setArguments(bundle);
        showBottomSheet(bottomSheet);
    }

    public void showHideDemoIndicator(Fragment fragment, boolean animated) {
        if(fragment instanceof StockFragment && isDemo()) {
            if(animated) {
                binding.frameMainDemo.setVisibility(View.VISIBLE);
                binding.frameMainDemo.animate().alpha(1).setDuration(200).start();
            } else {
                binding.frameMainDemo.setAlpha(1);
                binding.frameMainDemo.setVisibility(View.VISIBLE);
            }
        } else {
            if(animated) {
                binding.frameMainDemo.animate().alpha(0).setDuration(200).withEndAction(
                        () -> binding.frameMainDemo.setVisibility(View.GONE)
                );
            } else {
                binding.frameMainDemo.setAlpha(0);
                binding.frameMainDemo.setVisibility(View.GONE);
            }
        }
    }

    public void setNavBarColor(@ColorRes int color) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
        ) {
            color = R.color.black;
        }
        getWindow().setNavigationBarColor(ResourcesCompat.getColor(
                getResources(),
                color,
                null
        ));
    }

    public void setStatusBarColor(@ColorRes int color) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
        ) {
            color = R.color.black;
        }
        getWindow().setStatusBarColor(ResourcesCompat.getColor(
                getResources(),
                color,
                null
        ));
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

    private boolean isDemo() {
        String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
        return server != null && server.contains("grocy.info");
    }

    public GrocyApi getGrocy() {
        return grocyApi;
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

    private void replaceFabIcon(Drawable icon, String tag, boolean animated) {
        if(!tag.equals(binding.fabMain.getTag())) {
            if(animated) {
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
            if(debug) Log.i(TAG, "replaceFabIcon: not replaced, tags are identical");
        }
    }

    public void setFabIcon(Drawable icon) {
        binding.fabMain.setImageDrawable(icon);
    }

    /**
     * If SDK version is 28 or higher this tints the navBarDivider.
     */
    private void setNavBarDividerColor(@ColorRes int color) {
        if(Build.VERSION.SDK_INT >= 28) {
            getWindow().setNavigationBarDividerColor(ContextCompat.getColor(this, color));
        } else if(debug) Log.i(TAG, "setNavBarDividerColor: activity is null or SDK < 28");
    }

    private void setProperNavBarDividerColor(NavDestination dest, boolean newDestIsLogin) {
        if(binding.bottomAppBar.getVisibility() == View.GONE) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                if(dest.getId() == R.id.loginFragment || newDestIsLogin) {
                    setNavBarDividerColor(R.color.transparent);
                } else {
                    setNavBarDividerColor(R.color.stroke_secondary);
                }
            } else {
                setNavBarDividerColor(R.color.stroke_secondary);
            }
        }
    }

    public void executeOnStart() {
        onStart();
    }
}