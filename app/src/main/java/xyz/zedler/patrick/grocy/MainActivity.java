package xyz.zedler.patrick.grocy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.PurchaseBatchFragment;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.MasterLocationFragment;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductEditSimpleFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoreFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private final static boolean DEBUG = true;

    private RequestQueue requestQueue;
    private WebRequest request;
    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private GrocyApi grocyApi;
    private long lastClick = 0;
    private BottomAppBarRefreshScrollBehavior scrollBehavior;
    private String uiMode = Constants.UI.STOCK_DEFAULT;

    private CustomBottomAppBar bottomAppBar;
    private Fragment fragmentCurrent;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.edit().putBoolean(Constants.PREF.ANIM_UI_UPDATE, false).apply();

        // DARK MODE

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean(Constants.PREF.DARK_MODE, false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(R.layout.activity_main);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(this);

        // LOAD CONFIG

        request.get(
                grocyApi.getSystemConfig(),
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        sharedPrefs.edit()
                                // GET ALL NEEDED CONFIGS
                                .putString(
                                        Constants.PREF.CURRENCY,
                                        jsonObject.getString("CURRENCY")
                                ).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(DEBUG) Log.i(
                            TAG, "downloadConfig: config = " + response
                    );
                }, error -> {}
        );

        request.get(
                grocyApi.getUserSettings(),
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        sharedPrefs.edit()
                                // GET SETTINGS
                                .putInt(
                                        Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
                                        jsonObject.getInt("product_presets_location_id")
                                ).putString(
                                        Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                                        jsonObject.getString(
                                                "product_presets_product_group_id"
                                        )
                                ).putInt(
                                        Constants.PREF.PRODUCT_PRESETS_QU_ID,
                                        jsonObject.getInt("product_presets_qu_id")
                                ).putInt(
                                        Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
                                        jsonObject.getInt("stock_expring_soon_days")
                                ).putFloat(
                                        Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
                                        (float) jsonObject.getDouble(
                                                "stock_default_purchase_amount"
                                        )
                                ).putFloat(
                                        Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
                                        (float) jsonObject.getDouble(
                                                "stock_default_consume_amount"
                                        )
                                ).putBoolean(
                                        Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
                                        jsonObject.getBoolean(
                                                "show_icon_on_stock_overview_page_" +
                                                        "when_product_is_on_shopping_list"
                                        )
                                ).putBoolean(
                                        Constants.PREF.RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP,
                                        jsonObject.getBoolean(
                                                "recipe_ingredients_group_by_product_group"
                                        )
                                ).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(DEBUG) Log.i(
                            TAG, "downloadUserSettings: settings = " + response
                    );
                }, error -> {}
        );

        request.get(
                grocyApi.getSystemInfo(),
                response -> {
                    try {
                        sharedPrefs.edit()
                                .putString(
                                        Constants.PREF.GROCY_VERSION,
                                        new JSONObject(response).getJSONObject(
                                                "grocy_version"
                                        ).getString("Version")
                                ).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {}
        ); // TODO: info if version is not supported

        // VIEWS

        bottomAppBar = findViewById(R.id.bottom_app_bar);
        fab = findViewById(R.id.fab_main);

        // BOTTOM APP BAR

        bottomAppBar.setNavigationOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(bottomAppBar.getNavigationIcon());
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.UI_MODE, uiMode);
            showBottomSheet(new DrawerBottomSheetDialogFragment(), bundle);
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(this);
        scrollBehavior.setUpBottomAppBar(bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        fragmentManager = getSupportFragmentManager();

        String serverUrl = sharedPrefs.getString(Constants.PREF.SERVER_URL, "");
        if(serverUrl == null || serverUrl.equals("")) {
            startActivityForResult(
                    new Intent(this, LoginActivity.class),
                    Constants.REQUEST.LOGIN
            );
        } else {
            setUp(savedInstanceState);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.REQUEST.LOGIN && resultCode == Activity.RESULT_OK) {
            grocyApi.loadCredentials();
            setUp(null);
        } else if(requestCode == Constants.REQUEST.SCAN_PURCHASE
                && resultCode == Activity.RESULT_OK
                && data != null
        ) {
            replaceFragment(
                    Constants.UI.BATCH_PURCHASE,
                    data.getBundleExtra(Constants.ARGUMENT.BUNDLE),
                    true
            );
        }
    }

    private void setUp(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            String tag = savedInstanceState.getString(Constants.ARGUMENT.CURRENT_FRAGMENT);
            if(tag != null) {
                fragmentCurrent = fragmentManager.getFragment(savedInstanceState, tag);
            }
        } else {
            // STOCK FRAGMENT
            fragmentCurrent = new StockFragment();
            fragmentManager.beginTransaction()
                    .replace(
                            R.id.linear_container_main,
                            fragmentCurrent,
                            Constants.UI.STOCK
                    ).commit();
            bottomAppBar.changeMenu(R.menu.menu_stock, CustomBottomAppBar.MENU_END, false);
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

    public void updateUI(String uiMode, String origin) {
        if(DEBUG) Log.i(TAG, "updateUI: " + uiMode + ", origin = " + origin);
        boolean animated = sharedPrefs.getBoolean(Constants.PREF.ANIM_UI_UPDATE, false);
        this.uiMode = uiMode;

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
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
                        R.string.action_scan, // TODO: Correct string
                        Constants.FAB.TAG.SCAN,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == StockFragment.class) {
                                Intent intent = new Intent(
                                        this, ScanBatchActivity.class
                                );
                                intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
                                startActivityForResult(intent, Constants.REQUEST.SCAN_CONSUME);
                            }
                        }
                );
                break;
            case Constants.UI.CONSUME:
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_consume, animated,
                        () -> new Handler().postDelayed(
                                () -> {
                                    if(fragmentCurrent.getClass() == ConsumeFragment.class) {
                                        ((ConsumeFragment) fragmentCurrent).setUpBottomMenu();
                                    }
                                },
                                50
                        )
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
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END, R.menu.menu_purchase, animated,
                        () -> new Handler().postDelayed(
                                () -> {
                                    if(fragmentCurrent.getClass() == PurchaseFragment.class) {
                                        //((PurchaseFragment) fragmentCurrent).setUpBottomMenu();
                                    }
                                },
                                50
                        )
                );
                updateFab(
                        R.drawable.ic_round_local_grocery_store,
                        R.string.action_purchase,
                        Constants.FAB.TAG.PURCHASE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == PurchaseFragment.class) {
                                //((PurchaseFragment) fragmentCurrent).consumeProduct();
                            }
                        }
                );
                break;
            case Constants.UI.BATCH_PURCHASE:
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.CENTER, R.menu.menu_consume, animated, () -> {
                            if(fragmentCurrent.getClass() == PurchaseBatchFragment.class) {
                                ((PurchaseBatchFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_barcode_scan,
                        R.string.action_back,
                        Constants.FAB.TAG.SCAN,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == PurchaseBatchFragment.class) {
                                //((PurchaseBatchFragment) fragmentCurrent).openBarcodeScanner();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_PRODUCTS_DEFAULT:
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
                                Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_LOCATIONS_DEFAULT:
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
                                Constants.UI.MASTER_LOCATION_EDIT,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_STORES_DEFAULT:
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
                                Constants.UI.MASTER_STORE_EDIT,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT:
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
                                Constants.UI.MASTER_QUANTITY_UNIT_EDIT,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT:
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
                                Constants.UI.MASTER_PRODUCT_GROUP_EDIT,
                                null,
                                true
                        )
                );
                break;
            case Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE:
                scrollBehavior.setUpScroll(R.id.scroll_master_product_edit_simple);
                scrollBehavior.setHideOnScroll(false);
                updateBottomAppBar(
                        Constants.FAB.POSITION.END,
                        R.menu.menu_master_product_edit,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductEditSimpleFragment.class) {
                                ((MasterProductEditSimpleFragment) fragmentCurrent).setUpBottomMenu();
                            }
                        }
                );
                updateFab(
                        R.drawable.ic_round_backup,
                        R.string.action_save,
                        Constants.FAB.TAG.SAVE,
                        animated,
                        () -> {
                            if(fragmentCurrent.getClass() == MasterProductEditSimpleFragment.class) {
                                ((MasterProductEditSimpleFragment) fragmentCurrent).saveProduct();
                            }
                        }
                );
                break;
            case Constants.UI.MASTER_LOCATION_EDIT:
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
            case Constants.UI.MASTER_STORE_EDIT:
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
            case Constants.UI.MASTER_QUANTITY_UNIT_EDIT:
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
            case Constants.UI.MASTER_PRODUCT_GROUP_EDIT:
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
            default: if(DEBUG) Log.e(TAG, "updateUI: no action for " + uiMode);
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
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_END, animated, onMenuChanged
                );
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER);
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
            case Constants.FAB.POSITION.END:
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_START, animated, onMenuChanged
                );
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
                bottomAppBar.hideNavigationIcon();
                scrollBehavior.setTopScrollVisibility(false);
                break;
            case Constants.FAB.POSITION.GONE:
                if(fab.isOrWillBeShown()) fab.hide();
                bottomAppBar.changeMenu(
                        newMenuId, CustomBottomAppBar.MENU_END, animated, onMenuChanged
                );
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
        }
    }

    private void updateFab(
            @DrawableRes int iconResId,
            @StringRes int tooltipStringId,
            String tag,
            boolean animated,
            Runnable onClick
    ) {
        replaceFabIcon(iconResId, tag, animated);
        fab.setOnClickListener(v -> {
            startAnimatedIcon(fab.getDrawable());
            onClick.run();
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fab.setTooltipText(getString(tooltipStringId));
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
            case Constants.UI.CONSUME:
            case Constants.UI.PURCHASE:
            case Constants.UI.BATCH_PURCHASE: // TODO
            case Constants.UI.MASTER_PRODUCTS_DEFAULT:
            case Constants.UI.MASTER_LOCATIONS_DEFAULT:
            case Constants.UI.MASTER_STORES_DEFAULT:
            case Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT:
            case Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT:
                dismissFragments();
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
            case Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE:
            case Constants.UI.MASTER_LOCATION_EDIT:
            case Constants.UI.MASTER_STORE_EDIT:
            case Constants.UI.MASTER_QUANTITY_UNIT_EDIT:
            case Constants.UI.MASTER_PRODUCT_GROUP_EDIT:
                dismissFragment();
                break;
            default: if(DEBUG) Log.e(TAG, "onBackPressed: missing case, UI mode = " + uiMode);
        }
    }

    public void replaceFragment(String newFragment, Bundle bundle, boolean animated) {
        switch (newFragment) {
            case Constants.UI.STOCK:
                fragmentCurrent = new StockFragment();
                break;
            case Constants.UI.CONSUME:
                fragmentCurrent = new ConsumeFragment();
                break;
            case Constants.UI.PURCHASE:
                fragmentCurrent = new PurchaseFragment();
                break;
            case Constants.UI.BATCH_PURCHASE:
                fragmentCurrent = new PurchaseBatchFragment();
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
            case Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE:
                fragmentCurrent = new MasterProductEditSimpleFragment();
                break;
            case Constants.UI.MASTER_LOCATION_EDIT:
                fragmentCurrent = new MasterLocationFragment();
                break;
            case Constants.UI.MASTER_STORE_EDIT:
                fragmentCurrent = new MasterStoreFragment();
                break;
            case Constants.UI.MASTER_QUANTITY_UNIT_EDIT:
                fragmentCurrent = new MasterQuantityUnitFragment();
                break;
            case Constants.UI.MASTER_PRODUCT_GROUP_EDIT:
                fragmentCurrent = new MasterProductGroupFragment();
                break;
            default:
                if(DEBUG) Log.e(TAG, "replaceFragment: invalid argument");
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
                .replace(R.id.linear_container_main, fragmentCurrent, fragmentCurrent.toString())
                .addToBackStack(fragmentCurrent.toString())
                .commit();
        //bottomAppBar.show(fab.isOrWillBeShown());

        if(DEBUG) Log.i(
                TAG,
                "replaceFragment: replaced with " + newFragment + ", animated = " + animated
        );
    }

    private void dismissFragments() {
        int count = fragmentManager.getBackStackEntryCount();
        if(count >= 1) {
            for(int i = 0; i < count ; i++) {
                fragmentManager.popBackStack();
            }
            fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);

            if(DEBUG) Log.i(TAG, "dismissFragments: dismissed all fragments except stock");
        } else {
            if(DEBUG) Log.e(
                    TAG, "dismissFragments: no fragments dismissed, backStackCount = " + count
            );
        }
        bottomAppBar.show();
    }

    public void dismissFragment() {
        int count = fragmentManager.getBackStackEntryCount();
        if(count > 1) {
            fragmentManager.popBackStack();
            String tag = fragmentManager.getBackStackEntryAt(0).getName();
            fragmentCurrent = fragmentManager.findFragmentByTag(tag);
            if(fragmentCurrent != null) {
                if(DEBUG) Log.i(TAG, "dismissFragment: fragment dismissed, current = " + tag);
            } else {
                fragmentCurrent = fragmentManager.findFragmentByTag(Constants.UI.STOCK);
                if(DEBUG) Log.e(TAG, "dismissFragment: " + tag + " not found");
            }
        } else {
            if(DEBUG) Log.e(
                    TAG, "dismissFragment: no fragment dismissed, backStackCount = " + count
            );
        }
        bottomAppBar.show();
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showSnackbar(Snackbar snackbar) {
        snackbar.setAnchorView(fab.isOrWillBeShown() ? fab : bottomAppBar).show();
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        String tag = bottomSheet.toString();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
            if(bundle != null) bottomSheet.setArguments(bundle);
            fragmentManager.beginTransaction().add(bottomSheet, tag).commit();
            if(DEBUG) Log.i(TAG, "showBottomSheet: " + tag);
        } else if(DEBUG) Log.e(TAG, "showBottomSheet: sheet already visible");
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public void showKeyboard(EditText editText) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(
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

    public GrocyApi getGrocy() {
        return grocyApi;
    }

    public Menu getBottomMenu() {
        return bottomAppBar.getMenu();
    }

    public Fragment getCurrentFragment() {
        return fragmentCurrent;
    }

    private void replaceFabIcon(@DrawableRes int icon, String tag, boolean animated) {
        if(!tag.equals(fab.getTag())) {
            if(animated) {
                int duration = 400;
                ValueAnimator animOut = ValueAnimator.ofInt(fab.getImageAlpha(), 0);
                animOut.addUpdateListener(
                        animation -> fab.setImageAlpha((int) animation.getAnimatedValue())
                );
                animOut.setDuration(duration / 2);
                animOut.setInterpolator(new FastOutSlowInInterpolator());
                animOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fab.setImageResource(icon);
                        ValueAnimator animIn = ValueAnimator.ofInt(0, 255);
                        animIn.addUpdateListener(
                                anim -> fab.setImageAlpha((int) (anim.getAnimatedValue()))
                        );
                        animIn.setDuration(duration / 2);
                        animIn.setInterpolator(new FastOutSlowInInterpolator());
                        animIn.start();
                    }
                });
                animOut.start();
            } else {
                fab.setImageResource(icon);
            }
            fab.setTag(tag);
            if(DEBUG) Log.i(TAG, "replaceFabIcon: replaced, animated = " + animated);
        } else {
            if(DEBUG) Log.i(TAG, "replaceFabIcon: not replaced, tags are identical");
        }
    }

    private void startAnimatedIcon(Drawable drawable) {
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            if(DEBUG) Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }

    public void startAnimatedIcon(MenuItem item) {
        try {
            try {
                ((Animatable) item.getIcon()).start();
            } catch (ClassCastException e) {
                if(DEBUG) Log.e(TAG, "startAnimatedIcon(MenuItem) requires AVD!");
            }
        } catch (NullPointerException e) {
            if(DEBUG) Log.e(TAG, "startAnimatedIcon(MenuItem): Icon missing!");
        }
    }
}