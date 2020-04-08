package xyz.zedler.patrick.grocy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private final static boolean DEBUG = false;

    private RequestQueue requestQueue;
    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private long lastClick = 0;
    private Fragment fragmentCurrent;
    private BottomAppBarRefreshScrollBehavior scrollBehavior;
    private String uiMode = Constants.UI.STOCK_DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean("night_mode", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(R.layout.activity_main);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(
                getApplicationContext()
        ).getRequestQueue();

        // BOTTOM APP BAR

        CustomBottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        bottomAppBar.setNavigationOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(bottomAppBar.getNavigationIcon());
            showBottomSheet(new DrawerBottomSheetDialogFragment());
        });
        bottomAppBar.setOnMenuItemClickListener((MenuItem item) -> {
            if (SystemClock.elapsedRealtime() - lastClick < 500) return false;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(item);
            switch (item.getItemId()) {
                // STOCK DEFAULT
                case R.id.action_search:
                    if(!uiMode.equals(Constants.UI.STOCK_DEFAULT)) return false;
                    ((StockFragment) fragmentCurrent).setUpSearch();
                    break;
            }
            return true;
        });

        scrollBehavior = new BottomAppBarRefreshScrollBehavior(
                this
        );
        scrollBehavior.setUpBottomAppBar(bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(true);

        fragmentManager = getSupportFragmentManager();

        // STOCK FRAGMENT
        fragmentCurrent = new StockFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.linear_container_main, fragmentCurrent)
                .commit();
        bottomAppBar.changeMenu(R.menu.menu_stock, CustomBottomAppBar.MENU_END, false);

        if(sharedPrefs.getString(Constants.PREF.SERVER_URL, "").equals("")) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    public void updateUI(String uiMode, String origin) {
        Log.i(TAG, "updateUI: " + uiMode + ", origin = " + origin);
        this.uiMode = uiMode;

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
                scrollBehavior.setUpScroll(R.id.scroll_stock);
                /*String fabPosition;
                if(sharedPrefs.getBoolean(PREF_FAB_IN_FEED, DEFAULT_FAB_IN_FEED)) {
                    fabPosition = FAB_POSITION_CENTER;
                } else {
                    fabPosition = FAB_POSITION_GONE;
                }
                updateBottomAppBar(fabPosition, R.menu.menu_feed_default, animated);
                updateFab(
                        R.drawable.ic_round_add_anim,
                        R.string.action_add_channel,
                        FAB_TAG_ADD,
                        animated,
                        () -> {
                            showBottomSheet(new ChannelAddBottomSheetDialogFragment());
                            setUnreadCount(
                                    sharedPrefs.getInt(PREF_UNREAD_COUNT, 0) + 1
                            );
                        }
                );*/
                break;
            default: Log.e(TAG, "updateUI: wrong uiMode argument: " + uiMode);
        }
    }

    /*private void updateBottomAppBar(String newFabPosition,
                                    @MenuRes int newMenuId,
                                    boolean animated
    ) {
        switch (newFabPosition) {
            case FAB_POSITION_CENTER:
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(newMenuId, MENU_END, animated);
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER);
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
            case FAB_POSITION_END:
                if(fab.isOrWillBeHidden()) fab.show();
                bottomAppBar.changeMenu(newMenuId, MENU_START, animated);
                bottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END);
                bottomAppBar.hideNavigationIcon();
                scrollBehavior.setTopScrollVisibility(false);
                break;
            case FAB_POSITION_GONE:
                if(fab.isOrWillBeShown()) fab.hide();
                bottomAppBar.changeMenu(newMenuId, MENU_END, animated);
                bottomAppBar.showNavigationIcon(R.drawable.ic_round_menu_anim);
                scrollBehavior.setTopScrollVisibility(true);
                break;
        }
    }*/

    /*private void updateFab(@DrawableRes int iconResId,
                           @StringRes int tooltipStringId,
                           String tag,
                           boolean animated,
                           Runnable onClick
    ) {
        replaceFabIcon(iconResId, tag, animated);
        fab.setOnClickListener(v -> {
            startAnimatedIcon(fab);
            onClick.run();
        });
        fab.setTooltipText(getString(tooltipStringId));
    }*/

    @Override
    public void onBackPressed() {

        switch (uiMode) {
            case Constants.UI.STOCK_DEFAULT:
                super.onBackPressed();
                break;
            case Constants.UI.STOCK_SEARCH:
                ((StockFragment) fragmentCurrent).dismissSearch();
                break;

            default: Log.e(TAG, "onBackPressed: missing case, UI mode = " + uiMode);
        }
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showSnackbar(Snackbar snackbar) {
        snackbar.setAnchorView(findViewById(R.id.fab_add)).show();
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet) {
        String tag = bottomSheet.toString();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
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

    private void startAnimatedIcon(Drawable drawable) {
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }

    public void startAnimatedIcon(MenuItem item) {
        try {
            try {
                ((Animatable) item.getIcon()).start();
            } catch (ClassCastException e) {
                Log.e(TAG, "startAnimatedIcon(MenuItem) requires AVD!");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "startAnimatedIcon(MenuItem): Icon missing!");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestQueue.stop();
    }
}