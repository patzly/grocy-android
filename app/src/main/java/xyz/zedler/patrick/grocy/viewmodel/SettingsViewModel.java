package xyz.zedler.patrick.grocy.viewmodel;

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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNumberBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LogoutBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.RestartBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShortcutsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class SettingsViewModel extends BaseViewModel {

    private static final String TAG = SettingsViewModel.class.getSimpleName();
    private final SharedPreferences sharedPrefs;
    private boolean debug;

    private final DownloadHelper dlHelper;
    private NetUtil netUtil;
    private Gson gson;
    private final GrocyApi grocyApi;

    private MutableLiveData<Boolean> isLoadingLive;

    public SettingsViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        dlHelper = new DownloadHelper(
                getApplication(),
                TAG,
                isLoading -> isLoadingLive.setValue(isLoading)
        );
        netUtil = new NetUtil(getApplication());
        gson = new Gson();
        grocyApi = new GrocyApi(getApplication());

        isLoadingLive = new MutableLiveData<>(false);
    }

    public boolean isDemo() {
        String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
        return server != null && server.contains("grocy.info");
    }

    public boolean isVersionCompatible() {
        return getSupportedVersions().contains(
                sharedPrefs.getString(
                        Constants.PREF.GROCY_VERSION,
                        getString(R.string.date_unknown)
                )
        );
    }

    public void showCompatibilityBottomSheet() {
        if(isVersionCompatible()) return;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.SERVER, sharedPrefs.getString(
                Constants.PREF.SERVER_URL,
                getString(R.string.date_unknown)
        ));
        bundle.putString(Constants.ARGUMENT.KEY, sharedPrefs.getString(
                Constants.PREF.API_KEY,
                getString(R.string.date_unknown)
        ));
        bundle.putString(Constants.ARGUMENT.VERSION, sharedPrefs.getString(
                Constants.PREF.GROCY_VERSION,
                getString(R.string.date_unknown)
        ));
        bundle.putBoolean(Constants.ARGUMENT.DEMO_CHOSEN, isDemo());
        bundle.putStringArrayList(
                Constants.ARGUMENT.SUPPORTED_VERSIONS,
                getSupportedVersions()
        );
        CompatibilityBottomSheet bottomSheet = new CompatibilityBottomSheet();
        showBottomSheet(bottomSheet, bundle);
    }

    public void reloadConfiguration() {
        ConfigUtil.loadInfo(
                dlHelper,
                grocyApi,
                sharedPrefs,
                () -> showBottomSheet(new RestartBottomSheet(), null),
                error -> showErrorMessage()
        );
    }

    public void showLogoutBottomSheet() {
        Bundle bundle = null;
        if(isDemo()) bundle = new Bundle(); // empty bundle for indicating demo type
        showBottomSheet(new LogoutBottomSheet(), bundle);
    }

    public void showShortcutsBottomSheet() {
        showBottomSheet(new ShortcutsBottomSheet(), null);
    }

    public boolean getDarkMode() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.APPEARANCE.DARK_MODE,
                Constants.SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
        );
    }

    public void setDarkMode(boolean dark) {
        sharedPrefs.edit().putBoolean(Constants.SETTINGS.APPEARANCE.DARK_MODE, dark).apply();
    }

    public void showLoadingTimeoutBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ARGUMENT.NUMBER, getLoadingTimeout());
        bundle.putString(Constants.ARGUMENT.HINT, getString(R.string.property_seconds));
        showBottomSheet(new InputNumberBottomSheet(), bundle);
    }

    public int getLoadingTimeout() {
        return sharedPrefs.getInt(
                Constants.SETTINGS.NETWORK.LOADING_TIMEOUT,
                Constants.SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT
        );
    }

    public void setLoadingTimeout(int seconds) {
        sharedPrefs.edit().putInt(Constants.SETTINGS.NETWORK.LOADING_TIMEOUT, seconds).apply();
    }

    public boolean getLoggingEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
                Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
        );
    }

    public void setLoggingEnabled(boolean enabled) {
        sharedPrefs.edit()
                .putBoolean(Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING, enabled).apply();
    }

    public boolean getInfoLogsEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.DEBUGGING.ENABLE_INFO_LOGS,
                Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_INFO_LOGS
        );
    }

    public void setInfoLogsEnabled(boolean enabled) {
        sharedPrefs.edit()
                .putBoolean(Constants.SETTINGS.DEBUGGING.ENABLE_INFO_LOGS, enabled).apply();
    }

    public boolean getBeginnerModeEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
        );
    }

    public void setBeginnerModeEnabled(boolean enabled) {
        sharedPrefs.edit()
                .putBoolean(Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE, enabled).apply();
    }

    public boolean getExpandBottomSheetsEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
        );
    }

    public void setExpandBottomSheetsEnabled(boolean enabled) {
        sharedPrefs.edit()
                .putBoolean(Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS, enabled).apply();
    }

    public ArrayList<String> getSupportedVersions() {
        return new ArrayList<>(Arrays.asList(
                getApplication().getResources().getStringArray(R.array.compatible_grocy_versions)
        ));
    }

    public Location getLocation(ArrayList<Location> locations, int id) {
        for(Location location : locations) {
            if(location.getId() == id) {
                return location;
            }
        } return null;
    }

    public ProductGroup getProductGroup(ArrayList<ProductGroup> productGroups, int id) {
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == id) {
                return productGroup;
            }
        } return null;
    }

    public QuantityUnit getQuantityUnit(ArrayList<QuantityUnit> quantityUnits, int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) {
                return quantityUnit;
            }
        } return null;
    }

    public DownloadHelper getDownloadHelper() {
        return dlHelper;
    }

    public GrocyApi getGrocyApi() {
        return grocyApi;
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }
}
