package xyz.zedler.patrick.grocy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.behavior.BottomAppBarRefreshScrollBehavior;
import xyz.zedler.patrick.grocy.fragment.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragment;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private long lastClick = 0;
    private Fragment fragmentCurrent;

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

        // BOTTOM APP BAR

        CustomBottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        bottomAppBar.setNavigationOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            startAnimatedIcon(bottomAppBar.getNavigationIcon());
            showBottomSheet(new DrawerBottomSheetDialogFragment());
        });

        BottomAppBarRefreshScrollBehavior scrollBehavior = new BottomAppBarRefreshScrollBehavior(
                this
        );
        scrollBehavior.setUpBottomAppBar(bottomAppBar);
        scrollBehavior.setUpTopScroll(R.id.fab_scroll);
        scrollBehavior.setHideOnScroll(false);

        // FEED FRAGMENT
        fragmentCurrent = new StockOverviewFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.linear_container_main, fragmentCurrent)
                .commit();

        new Handler().postDelayed(() -> {
            scrollBehavior.setUpScroll(R.id.scroll_stock);
        }, 200);

        if(sharedPrefs.getString(Constants.PREF.SERVER_URL, "").equals("")) {
            startActivity(new Intent(this, LoginActivity.class));
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
        getSupportFragmentManager().beginTransaction().add(bottomSheet, "bottomSheet").commit();
        Log.i(TAG, "bottomSheetDialogFragment showed");
    }

    private void startAnimatedIcon(Drawable drawable) {
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }
}