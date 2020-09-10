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
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.LoginFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public ActivityMainBinding binding;
    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private GrocyApi grocyApi;
    private ClickUtil clickUtil;
    private NetUtil netUtil;
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

        // BOTTOM APP BAR

        binding.bottomAppBar.setNavigationOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            IconUtil.start(binding.bottomAppBar.getNavigationIcon());
            NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
                    .findFragmentById(R.id.nav_host_fragment);
            assert navHostFragment != null;
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.action_global_drawerBottomSheetDialogFragment);
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
        scrollBehavior.setUpBottomAppBar(binding.bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
                .findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.i(TAG, "onCreate: " + destination.getLabel());
            if(!sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false) && isServerUrlEmpty()) {
                navController.navigate(R.id.action_global_loginFragment);
                navController.navigate(R.id.action_global_featuresFragment);
            } else if(!sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false) && destination.getId() != R.id.featuresFragment) {
                navController.navigate(R.id.action_global_featuresFragment);
            } else if((destination.getId() != R.id.loginFragment && destination.getId() != R.id.aboutFragment && destination.getId() != R.id.featuresFragment) && isServerUrlEmpty()) {
                navController.navigate(R.id.action_global_loginFragment);
            }
            if(destination.getId() != R.id.loginFragment && destination.getId() != R.id.aboutFragment) {
                getWindow().setStatusBarColor(ResourcesCompat.getColor(
                        getResources(),
                        R.color.primary,
                        null
                ));
            }
            if(isServerUrlEmpty()) {
                binding.bottomAppBar.setVisibility(View.GONE);
                binding.fabMain.hide();
                getWindow().setNavigationBarColor(ResourcesCompat.getColor(
                        getResources(),
                        R.color.background,
                        null
                ));
            } else {
                binding.bottomAppBar.setVisibility(View.VISIBLE);
                getWindow().setNavigationBarColor(ResourcesCompat.getColor(
                        getResources(),
                        R.color.primary,
                        null
                ));
            }
        });

        /*if(isServerUrlEmpty()) {
            startActivityForResult(
                    new Intent(this, LoginActivity.class),
                    Constants.REQUEST.LOGIN
            );
        } else {
            setUp(savedInstanceState);
        }*/

        if(!isServerUrlEmpty()) {
            setUp(savedInstanceState);
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

        if(requestCode == Constants.REQUEST.FEATURES) {
            /*if(isServerUrlEmpty()) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(Constants.EXTRA.AFTER_FEATURES_ACTIVITY, true);
                startActivityForResult(intent, Constants.REQUEST.LOGIN);
            }*/
        } else if(requestCode == Constants.REQUEST.LOGIN && resultCode == Activity.RESULT_OK) {
            grocyApi.loadCredentials();
            setUp(null);
        } else if(requestCode == Constants.REQUEST.SCAN_BATCH
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

    private void setUp(Bundle savedInstanceState) {
        ConfigUtil.loadInfo(
                new DownloadHelper(this, TAG),
                grocyApi,
                sharedPrefs,
                null,
                null
        );

        // FRAGMENT

        /*if(savedInstanceState != null) {
            String tag = savedInstanceState.getString(Constants.ARGUMENT.CURRENT_FRAGMENT);
            if(tag != null) {
                fragmentCurrent = fragmentManager.getFragment(savedInstanceState, tag);
            }
        } else { // default is stock
            fragmentCurrent = new StockFragment();
            Bundle bundleNoAnim = new Bundle();
            bundleNoAnim.putBoolean(Constants.ARGUMENT.ANIMATED, false);
            fragmentCurrent.setArguments(bundleNoAnim);
            fragmentManager.beginTransaction().replace(
                    R.id.frame_main_container,
                    fragmentCurrent,
                    Constants.UI.STOCK
            ).commit();
        }*/

        // SHORTCUT

        String action = getIntent() != null ? getIntent().getAction() : null;
        if(action != null) {
            // no animation for shortcut fragments
            Bundle bundleNoAnim = new Bundle();
            bundleNoAnim.putBoolean(Constants.ARGUMENT.ANIMATED, false);

            /*switch (action) {
                case Constants.SHORTCUT_ACTION.CONSUME:
                    Intent intentConsume = new Intent(this, ScanBatchActivity.class);
                    intentConsume.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
                    startActivityForResult(intentConsume, Constants.REQUEST.SCAN_BATCH);
                    break;
                case Constants.SHORTCUT_ACTION.PURCHASE:
                    Intent intentPurchase = new Intent(this, ScanBatchActivity.class);
                    intentPurchase.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE);
                    startActivityForResult(intentPurchase, Constants.REQUEST.SCAN_BATCH);
                    break;
                case Constants.SHORTCUT_ACTION.SHOPPING_LIST:
                    replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                    break;
                case Constants.SHORTCUT_ACTION.ADD_ENTRY:
                    replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                    Bundle bundleCreate = new Bundle();
                    bundleCreate.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE);
                    bundleCreate.putBoolean(Constants.ARGUMENT.ANIMATED, false);
                    replaceFragment(
                            Constants.UI.SHOPPING_LIST_ITEM_EDIT,
                            bundleCreate,
                            false
                    );
                    break;
                case Constants.SHORTCUT_ACTION.SHOPPING_MODE:
                    replaceFragment(Constants.UI.SHOPPING_LIST, bundleNoAnim, false);
                    startActivity(new Intent(this, ShoppingActivity.class));
                    break;
            }*/
            getIntent().setAction(null);
        }
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
        switch (newFabPosition) {
            case Constants.FAB.POSITION.CENTER:
                if(binding.fabMain.isOrWillBeHidden()) binding.fabMain.show();
                binding.bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_END, animated, onMenuChanged
                );
                binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER);
                binding.bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim, animated);
                scrollBehavior.setTopScrollVisibility(true);
                break;
            case Constants.FAB.POSITION.END:
                if(binding.fabMain.isOrWillBeHidden()) binding.fabMain.show();
                binding.bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_START, animated, onMenuChanged
                );
                binding.bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
                binding.bottomAppBar.hideNavigationIcon(animated);
                scrollBehavior.setTopScrollVisibility(false);
                break;
            case Constants.FAB.POSITION.GONE:
                if(binding.fabMain.isOrWillBeShown()) binding.fabMain.hide();
                binding.bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_END, animated, onMenuChanged
                );
                binding.bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim, animated);
                scrollBehavior.setTopScrollVisibility(true);
                break;
        }
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

    public void replaceFragment(String fragmentNew, Bundle bundle, boolean animated) {}

    public void dismissFragment() {}

    public boolean isOnline() {
        return netUtil.isOnline();
    }

    public void showSnackbar(Snackbar snackbar) {
        if(binding.bottomAppBar.getVisibility() == View.GONE && !binding.fabMain.isOrWillBeShown()) {
            snackbar.show();
        } else {
            snackbar.setAnchorView(
                    binding.fabMain.isOrWillBeShown() ? binding.fabMain : binding.bottomAppBar
            ).show();
        }
    }

    public void showMessage(@StringRes int message) {
        showSnackbar(
                Snackbar.make(binding.frameMainContainer, getString(message), Snackbar.LENGTH_LONG)
        );
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        String tag = bottomSheet.toString();
        bottomSheet.setArguments(bundle);
        bottomSheet.show(fragmentManager, tag);
        if(debug) Log.i(TAG, "showBottomSheet: " + bottomSheet.toString());
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
}