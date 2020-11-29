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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.databinding.ActivityMainBinding;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.MasterLocationFragment;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductSimpleFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoreFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.fragment.MissingBatchItemsFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListEditFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ExitMissingBatchBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.UpdateInfoBottomSheetDialogFragment;
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
    private Fragment fragmentCurrent;

    private String uiMode = Constants.UI.STOCK_DEFAULT;
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
                if(fragmentCurrent == null) return;
                if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                    ((ShoppingListFragment) fragmentCurrent).updateConnectivity(netUtil.isOnline());
                }
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
                v -> showBottomSheet(new LogoutBottomSheetDialogFragment(), new Bundle())
        );

        // BOTTOM APP BAR

        binding.bottomAppBar.setNavigationOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            IconUtil.start(binding.bottomAppBar.getNavigationIcon());
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.UI_MODE, uiMode);
            showBottomSheet(new DrawerBottomSheetDialogFragment(), bundle);
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
        scrollBehavior.setUpBottomAppBar(binding.bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        fragmentManager = getSupportFragmentManager();

        if(!sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false)) {
            startActivityForResult(
                    new Intent(this, FeaturesActivity.class),
                    Constants.REQUEST.FEATURES
            );
            return;
        }

        if(isServerUrlEmpty()) {
            startActivityForResult(
                    new Intent(this, LoginActivity.class),
                    Constants.REQUEST.LOGIN
            );
        } else {
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
            if(isServerUrlEmpty()) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(Constants.EXTRA.AFTER_FEATURES_ACTIVITY, true);
                startActivityForResult(intent, Constants.REQUEST.LOGIN);
            }
        } else if(requestCode == Constants.REQUEST.LOGIN && resultCode == Activity.RESULT_OK) {
            grocyApi.loadCredentials();
            setUp(null);
        } else if(requestCode == Constants.REQUEST.SCAN_BATCH
                && resultCode == Activity.RESULT_OK
                && data != null
        ) {
            replaceFragment(
                    Constants.UI.MISSING_BATCH_ITEMS,
                    data.getBundleExtra(Constants.ARGUMENT.BUNDLE),
                    true
            );
        } else if((requestCode == Constants.REQUEST.SCAN_BATCH)
                && resultCode == Activity.RESULT_CANCELED
                && fragmentCurrent.getClass() == StockFragment.class
        ) {
            ((StockFragment) fragmentCurrent).refresh();
        }
    }

    private void setUp(Bundle savedInstanceState) {
        String serverUrl = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
        if(serverUrl != null && serverUrl.contains(".onion") && !OrbotHelper.get(this).init()) {
            showMessage(Snackbar.make(
                    binding.frameMainContainer,
                    getString(R.string.error_orbot_not_installed),
                    Snackbar.LENGTH_LONG
            ));
        }

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
                        )) showBottomSheet(new UpdateInfoBottomSheetDialogFragment(), null);
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
                            new CompatibilityBottomSheetDialogFragment(),
                            bundle
                    );
                },
                null
        );

        // FRAGMENT

        if(savedInstanceState != null) {
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
        }

        // SHORTCUT

        String action = getIntent() != null ? getIntent().getAction() : null;
        if(action != null) {
            // no animation for shortcut fragments
            Bundle bundleNoAnim = new Bundle();
            bundleNoAnim.putBoolean(Constants.ARGUMENT.ANIMATED, false);

            switch (action) {
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
            }
            getIntent().setAction(null);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        String tag = fragmentCurrent != null ? fragmentCurrent.toString() : null;
        if(tag != null) {
            fragmentManager.putFragment(outState, tag, fragmentCurrent);
            outState.putString(Constants.ARGUMENT.CURRENT_FRAGMENT, tag);
        }
    }

    public void setUI(String uiMode) {
        if(debug) Log.i(TAG, "setUI: " + uiMode);
        this.uiMode = uiMode;
    }

    public void updateUI(String uiMode, String origin) {
        updateUI(uiMode, true, origin);
    }

    public void updateUI(String uiMode, boolean animated, String origin) {
        if(debug) Log.i(TAG, "updateUI: " + uiMode + ", origin = " + origin);

        this.uiMode = uiMode;

        if(uiMode.startsWith(Constants.UI.STOCK) && isDemo()) {
            showDemoIndicator(animated);
        } else {
            hideDemoIndicator(animated);
        }

        if(fragmentCurrent == null) {
            recreate();
            return;
        }

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
            case Constants.UI.STOCK_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_stock);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER, R.menu.menu_stock, animated, () -> {
                            if(fragmentCurrent.getClass() == StockFragment.class) {
                                ((StockFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_barcode_scan,
                        R.string.action_scan,
                        Constants.FAB.TAG.SCAN,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == StockFragment.class) {
                                Intent intent = new Intent(
                                        this, ScanBatchActivity.class
                                );
                                intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
                                startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
                            }
                        }
                );
                break;
            case Constants.UI.SHOPPING_LIST_DEFAULT:
            case Constants.UI.SHOPPING_LIST_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_shopping_list);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER, R.menu.menu_shopping_list, animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                                ((ShoppingListFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                                ((ShoppingListFragment) fragmentCurrent).addItem();
                            }
                        }
                );
                break;
            case Constants.UI.SHOPPING_LIST_OFFLINE_DEFAULT:
            case Constants.UI.SHOPPING_LIST_OFFLINE_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_shopping_list);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.GONE, R.menu.menu_shopping_list_offline, animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                                ((ShoppingListFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                break;
            case Constants.UI.SHOPPING_LIST_EDIT:
                scrollBehavior.setUpScroll(R.id.scroll_shopping_list_edit);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_shopping_list_edit, animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListEditFragment.class) {
                                ((ShoppingListEditFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListEditFragment.class) {
                                ((ShoppingListEditFragment) fragmentCurrent).saveItem();
                            }
                        }
                );
                break;
            case Constants.UI.SHOPPING_LIST_ITEM_EDIT:
                scrollBehavior.setUpScroll(R.id.scroll_shopping_list_item_edit);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_shopping_list_item_edit, animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListItemEditFragment.class) {
                                ((ShoppingListItemEditFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ShoppingListItemEditFragment.class) {
                                ((ShoppingListItemEditFragment) fragmentCurrent).saveItem();
                            }
                        }
                );
                break;
            case Constants.UI.CONSUME:
                scrollBehavior.setUpScroll(R.id.scroll_consume);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_consume, animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ConsumeFragment.class) {
                                ((ConsumeFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_consume_product,
                        R.string.action_consume,
                        Constants.FAB.TAG.CONSUME,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == ConsumeFragment.class) {
                                ((ConsumeFragment) fragmentCurrent).consumeProduct();
                            }
                        }
                );
                break;
            case Constants.UI.PURCHASE:
                scrollBehavior.setUpScroll(R.id.scroll_purchase);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_purchase, animated,
                        () -> new Handler().postDelayed(
                                () -> {
                                    if(fragmentCurrent.getClass() == PurchaseFragment.class) {
                                        ((PurchaseFragment) fragmentCurrent).setUpBottomMenu();
                                    }
                                }, 50
                        )
                );
                updateFab(
                        R.drawable.ic_round_local_grocery_store,
                        R.string.action_purchase,
                        Constants.FAB.TAG.PURCHASE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == PurchaseFragment.class) {
                                ((PurchaseFragment) fragmentCurrent).purchaseProduct();
                            }
                        }
                );
                break;
            case Constants.UI.MISSING_BATCH_ITEMS:
                scrollBehavior.setUpScroll(R.id.scroll_missing_batch_items);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_missing_batch_items,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MissingBatchItemsFragment.class) {
                                ((MissingBatchItemsFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_PRODUCTS_DEFAULT:
            case Constants.UI.MASTER_PRODUCTS_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_master_products);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER,
                        R.menu.menu_master_products,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductsFragment.class) {
                                ((MasterProductsFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> replaceFragment(
                                Constants.UI.MASTER_PRODUCT_SIMPLE,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_LOCATIONS_DEFAULT:
            case Constants.UI.MASTER_LOCATIONS_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_master_locations);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER,
                        R.menu.menu_master_items,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterLocationsFragment.class) {
                                ((MasterLocationsFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> replaceFragment(
                                Constants.UI.MASTER_LOCATION,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_STORES_DEFAULT:
            case Constants.UI.MASTER_STORES_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_master_stores);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER,
                        R.menu.menu_master_items,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterStoresFragment.class) {
                                ((MasterStoresFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> replaceFragment(
                                Constants.UI.MASTER_STORE,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT:
            case Constants.UI.MASTER_QUANTITY_UNITS_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_master_quantity_units);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER,
                        R.menu.menu_master_items,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterQuantityUnitsFragment.class) {
                                ((MasterQuantityUnitsFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> replaceFragment(
                                Constants.UI.MASTER_QUANTITY_UNIT,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT:
            case Constants.UI.MASTER_PRODUCT_GROUPS_SEARCH:
                scrollBehavior.setUpScroll(R.id.scroll_master_product_groups);
                scrollBehavior.setHideOnScroll(true);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER,
                        R.menu.menu_master_items,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductGroupsFragment.class) {
                                ((MasterProductGroupsFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add,
                        Constants.FAB.TAG.ADD,
                        animated,
                        () -> replaceFragment(
                                Constants.UI.MASTER_PRODUCT_GROUP,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_PRODUCT_SIMPLE:
                scrollBehavior.setUpScroll(R.id.scroll_master_product_simple);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_product_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductSimpleFragment.class) {
                                ((MasterProductSimpleFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        true,
                        () -> {
                            if(getCurrentFragment().getClass()
                                    == MasterProductSimpleFragment.class
                            ) {
                                ((MasterProductSimpleFragment) fragmentCurrent).saveProduct();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_LOCATION:
                scrollBehavior.setUpScroll(R.id.scroll_master_location);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_item_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterLocationFragment.class) {
                                ((MasterLocationFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterLocationFragment.class) {
                                ((MasterLocationFragment) fragmentCurrent).saveLocation();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_STORE:
                scrollBehavior.setUpScroll(R.id.scroll_master_store);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_item_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterStoreFragment.class) {
                                ((MasterStoreFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterStoreFragment.class) {
                                ((MasterStoreFragment) fragmentCurrent).saveStore();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_QUANTITY_UNIT:
                scrollBehavior.setUpScroll(R.id.scroll_master_quantity_unit);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_item_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterQuantityUnitFragment.class) {
                                ((MasterQuantityUnitFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterQuantityUnitFragment.class) {
                                ((MasterQuantityUnitFragment) fragmentCurrent).saveQuantityUnit();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_PRODUCT_GROUP:
                scrollBehavior.setUpScroll(R.id.scroll_master_product_group);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_item_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductGroupFragment.class) {
                                ((MasterProductGroupFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductGroupFragment.class) {
                                ((MasterProductGroupFragment) fragmentCurrent).saveProductGroup();
                            }
                        }
                );
                break;
            default: if(debug) Log.e(TAG, "updateUI: no action for " + uiMode);
        }
    }

    private void updateBottomAppBar(
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
        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
                super.onBackPressed();
                break;
            case Constants.UI.STOCK_SEARCH:
                if(fragmentCurrent.getClass() == StockFragment.class) {
                    ((StockFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.SHOPPING_LIST_SEARCH:
                if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                    ((ShoppingListFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.SHOPPING_LIST_DEFAULT:
            case Constants.UI.SHOPPING_LIST_OFFLINE_DEFAULT:
            case Constants.UI.CONSUME:
            case Constants.UI.MASTER_PRODUCTS_DEFAULT:
            case Constants.UI.MASTER_LOCATIONS_DEFAULT:
            case Constants.UI.MASTER_STORES_DEFAULT:
            case Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT:
            case Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT:
                dismissFragments();
                break;
            case Constants.UI.MISSING_BATCH_ITEMS:
                if(fragmentCurrent.getClass() == MissingBatchItemsFragment.class) {
                    if(((MissingBatchItemsFragment)fragmentCurrent).getMissingBatchItemsSize() == 0
                    ) {
                        dismissFragments();
                    } else {
                        showBottomSheet(
                                new ExitMissingBatchBottomSheetDialogFragment(),
                                fragmentCurrent.getArguments()
                        );
                    }
                }
                break;
            case Constants.UI.MASTER_PRODUCTS_SEARCH:
                if(fragmentCurrent.getClass() == MasterProductsFragment.class) {
                    ((MasterProductsFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.MASTER_LOCATIONS_SEARCH:
                if(fragmentCurrent.getClass() == MasterLocationsFragment.class) {
                    ((MasterLocationsFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.MASTER_STORES_SEARCH:
                if(fragmentCurrent.getClass() == MasterStoresFragment.class) {
                    ((MasterStoresFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.MASTER_QUANTITY_UNITS_SEARCH:
                if(fragmentCurrent.getClass() == MasterQuantityUnitsFragment.class) {
                    ((MasterQuantityUnitsFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.MASTER_PRODUCT_GROUPS_SEARCH:
                if(fragmentCurrent.getClass() == MasterProductGroupsFragment.class) {
                    ((MasterProductGroupsFragment) fragmentCurrent).dismissSearch();
                }
                break;
            case Constants.UI.MASTER_PRODUCT_SIMPLE:
                if(fragmentCurrent.getClass() != MasterProductSimpleFragment.class) break;
                if(((MasterProductSimpleFragment) fragmentCurrent)
                        .getIntendedAction().equals(Constants.ACTION.CREATE_THEN_PURCHASE)
                ) {
                    dismissFragment(fragmentCurrent.getArguments());
                } else {
                    dismissFragment();
                }
                break;
            case Constants.UI.PURCHASE:
            case Constants.UI.SHOPPING_LIST_ITEM_EDIT:
            case Constants.UI.SHOPPING_LIST_EDIT:
            case Constants.UI.MASTER_LOCATION:
            case Constants.UI.MASTER_STORE:
            case Constants.UI.MASTER_QUANTITY_UNIT:
            case Constants.UI.MASTER_PRODUCT_GROUP:
                dismissFragment();
                break;
            default: if(debug) Log.e(TAG, "onBackPressed: missing case, UI mode = " + uiMode);
        }
    }

    public void replaceFragment(String fragmentNew, Bundle bundle, boolean animated) {
        Fragment fragmentOld = fragmentCurrent;
        switch (fragmentNew) {
            case Constants.UI.STOCK:
                fragmentCurrent = new StockFragment();
                break;
            case Constants.UI.SHOPPING_LIST:
            case Constants.UI.SHOPPING_LIST_OFFLINE_DEFAULT:
                fragmentCurrent = new ShoppingListFragment();
                break;
            case Constants.UI.SHOPPING_LIST_EDIT:
                fragmentCurrent = new ShoppingListEditFragment();
                break;
            case Constants.UI.SHOPPING_LIST_ITEM_EDIT:
                fragmentCurrent = new ShoppingListItemEditFragment();
                break;
            case Constants.UI.CONSUME:
                fragmentCurrent = new ConsumeFragment();
                break;
            case Constants.UI.PURCHASE:
                fragmentCurrent = new PurchaseFragment();
                break;
            case Constants.UI.MISSING_BATCH_ITEMS:
                fragmentCurrent = new MissingBatchItemsFragment();
                break;
            case Constants.UI.MASTER_PRODUCTS:
                fragmentCurrent = new MasterProductsFragment();
                break;
            case Constants.UI.MASTER_LOCATIONS:
                fragmentCurrent = new MasterLocationsFragment();
                break;
            case Constants.UI.MASTER_STORES:
                fragmentCurrent = new MasterStoresFragment();
                break;
            case Constants.UI.MASTER_QUANTITY_UNITS:
                fragmentCurrent = new MasterQuantityUnitsFragment();
                break;
            case Constants.UI.MASTER_PRODUCT_GROUPS:
                fragmentCurrent = new MasterProductGroupsFragment();
                break;
            case Constants.UI.MASTER_PRODUCT_SIMPLE:
                fragmentCurrent = new MasterProductSimpleFragment();
                break;
            case Constants.UI.MASTER_LOCATION:
                fragmentCurrent = new MasterLocationFragment();
                break;
            case Constants.UI.MASTER_STORE:
                fragmentCurrent = new MasterStoreFragment();
                break;
            case Constants.UI.MASTER_QUANTITY_UNIT:
                fragmentCurrent = new MasterQuantityUnitFragment();
                break;
            case Constants.UI.MASTER_PRODUCT_GROUP:
                fragmentCurrent = new MasterProductGroupFragment();
                break;
            default:
                if(debug) Log.e(TAG, "replaceFragment: invalid argument");
                return;
        }
        if(bundle != null) {
            fragmentCurrent.setArguments(bundle);
        }
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        (animated) ? R.anim.slide_in_up : R.anim.slide_no,
                        (animated) ? R.anim.fade_out : R.anim.slide_no,
                        R.anim.fade_in,
                        R.anim.slide_out_down)
                .hide(fragmentOld)
                .add(R.id.frame_main_container, fragmentCurrent, fragmentCurrent.toString())
                .addToBackStack(fragmentCurrent.toString())
                .commit();

        if(debug) Log.i(
                TAG, "replaceFragment: replaced " + fragmentOld
                        + " with "+ fragmentNew
                        + ", animated = " + animated
        );
    }

    public void replaceAll(String fragmentNew, Bundle bundle, boolean animated) {
        dismissFragments();
        new Handler().postDelayed(
                () -> replaceFragment(fragmentNew, bundle, animated),
                getResources().getInteger(R.integer.default_anim_duration)
        );
    }

    public void dismissFragments() {
        int count = fragmentManager.getBackStackEntryCount();
        if(count >= 1) {
            for(int i = 0; i < count ; i++) {
                fragmentManager.popBackStackImmediate();
            }
            fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);

            if(debug) Log.i(TAG, "dismissFragments: dismissed all fragments except stock");
        } else {
            if(debug) Log.e(
                    TAG, "dismissFragments: no fragments dismissed, backStackCount = " + count
            );
        }
        binding.bottomAppBar.show();
    }

    public void dismissFragment() {
        String tag;
        int count = fragmentManager.getBackStackEntryCount();
        if(count == 0) {
            if(debug) Log.e(TAG, "dismissFragment: no fragment dismissed, backStackCount = 0");
            return;
        }
        fragmentManager.popBackStackImmediate();
        count = fragmentManager.getBackStackEntryCount();
        if(count == 0) {
            tag = Constants.UI.STOCK;
            fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);
        } else {
            tag = fragmentManager.getBackStackEntryAt(
                    fragmentManager.getBackStackEntryCount()-1
            ).getName();
            fragmentCurrent = fragmentManager.findFragmentByTag(tag);
        }
        if(fragmentCurrent == null) {
            fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);
            if(debug) Log.e(TAG, "dismissFragment: " + tag + " not found");
        } else if(debug) {
            Log.i(TAG, "dismissFragment: fragment dismissed, current = " + tag);
        }
        binding.bottomAppBar.show();
    }

    public void dismissFragment(Bundle bundle) {
        int count = fragmentManager.getBackStackEntryCount();
        if(count >= 1) {
            fragmentManager.popBackStackImmediate();
            String tag;
            if(fragmentManager.getBackStackEntryCount() == 0) {
                tag = Constants.UI.STOCK;
                fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);
            } else {
                tag = fragmentManager.getBackStackEntryAt(
                        fragmentManager.getBackStackEntryCount()-1
                ).getName();
                fragmentCurrent = fragmentManager.findFragmentByTag(tag);
            }
            if(fragmentCurrent != null) {
                if(fragmentCurrent.getClass() == PurchaseFragment.class) {
                    ((PurchaseFragment) fragmentCurrent).giveBundle(bundle);
                } else if(fragmentCurrent.getClass() == ConsumeFragment.class) {
                    ((ConsumeFragment) fragmentCurrent).giveBundle(bundle);
                } else if(fragmentCurrent.getClass() == MissingBatchItemsFragment.class) {
                    ((MissingBatchItemsFragment) fragmentCurrent).createdOrEditedProduct(bundle);
                } else if(fragmentCurrent.getClass() == ShoppingListItemEditFragment.class) {
                    ((ShoppingListItemEditFragment) fragmentCurrent).setProductName(
                            bundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                    );
                } else if(fragmentCurrent.getClass() == ShoppingListFragment.class) {
                    ((ShoppingListFragment) fragmentCurrent).selectShoppingList(
                            bundle.getString(Constants.ARGUMENT.SHOPPING_LIST_NAME)
                    );
                }
                if(debug) Log.i(TAG, "dismissFragment: fragment dismissed, current = " + tag);
            } else {
                fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);
                if(debug) Log.e(TAG, "dismissFragment: " + tag + " not found");
            }
        } else {
            if(debug) Log.e(TAG, "dismissFragment: no fragment dismissed, backStackCount = " + count);
        }
        binding.bottomAppBar.show();
    }

    public boolean isOnline() {
        return netUtil.isOnline();
    }

    public void showMessage(Snackbar snackbar) {
        snackbar.setAnchorView(
                binding.fabMain.isOrWillBeShown() ? binding.fabMain : binding.bottomAppBar
        ).show();
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        String tag = bottomSheet.toString();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
            if(bundle != null) bottomSheet.setArguments(bundle);
            bottomSheet.show(fragmentManager, tag);
            if(debug) Log.i(TAG, "showBottomSheet: " + tag);
        } else if(debug) Log.e(TAG, "showBottomSheet: sheet already visible");
    }

    private void showDemoIndicator(boolean animated) {
        if(uiMode.startsWith(Constants.UI.STOCK)) {
            if(animated) {
                binding.frameMainDemo.setVisibility(View.VISIBLE);
                binding.frameMainDemo.animate().alpha(1).setDuration(200).start();
            } else {
                binding.frameMainDemo.setAlpha(1);
                binding.frameMainDemo.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideDemoIndicator(boolean animated) {
        if(animated) {
            binding.frameMainDemo.animate().alpha(0).setDuration(200).withEndAction(
                    () -> binding.frameMainDemo.setVisibility(View.GONE)
            );
        } else {
            binding.frameMainDemo.setAlpha(0);
            binding.frameMainDemo.setVisibility(View.GONE);
        }
    }

    public void showKeyboard(EditText editText) {
        ((InputMethodManager) Objects
                .requireNonNull(getSystemService(Context.INPUT_METHOD_SERVICE))
        ).showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
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

    public Fragment getCurrentFragment() {
        return fragmentCurrent;
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