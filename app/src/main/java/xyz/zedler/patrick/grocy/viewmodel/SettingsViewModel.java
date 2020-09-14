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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = SettingsViewModel.class.getSimpleName();
    private SharedPreferences sharedPrefs;
    private boolean debug;

    private DownloadHelper dlHelper;
    private NetUtil netUtil;
    private Gson gson;
    private GrocyApi grocyApi;
    private EventHandler eventHandler;

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
        eventHandler = new EventHandler();

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

    public CompatibilityBottomSheet getCompatibilityBottomSheet() {
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
        bottomSheet.setArguments(bundle);
        return bottomSheet;
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

    private void showErrorMessage() {
        showMessage(getString(R.string.error_undefined));
    }

    private void showMessage(@NonNull String message) {
        showSnackbar(new SnackbarMessage(message));
    }

    private void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
        eventHandler.setValue(snackbarMessage);
    }

    private void sendEvent(int type) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}
        });
    }

    private void sendEvent(int type, Bundle bundle) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}

            @Override
            public Bundle getBundle() {return bundle;}
        });
    }

    @NonNull
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    private String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }

    private String getString(@StringRes int resId, Object... formatArgs) {
        return getApplication().getString(resId, formatArgs);
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }
}
