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

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.ConnectivityLiveData;

public class ScanExternalInputViewModel extends BaseViewModel {

    private static final String TAG = ScanExternalInputViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final MasterProductRepository repository;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final ConnectivityLiveData isOnlineLive;

    private DownloadHelper.Queue currentQueueLoading;
    private final boolean debug;

    public ScanExternalInputViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        repository = new MasterProductRepository(application);

        infoFullscreenLive = new MutableLiveData<>();
        isOnlineLive = new ConnectivityLiveData(application);
    }

    public String getConfiguredOptions() {
        String prefix = sharedPrefs.getString(
                Constants.SETTINGS.SCANNER.EXTERNAL_PREFIX,
                Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_PREFIX
        );
        String suffix = sharedPrefs.getString(
                Constants.SETTINGS.SCANNER.EXTERNAL_SUFFIX,
                Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SUFFIX
        );
        assert prefix != null;
        assert suffix != null;
        return getApplication().getString(
                R.string.msg_help_external_scanner_settings,
                prefix.isEmpty() ? getString(R.string.subtitle_none) : prefix,
                suffix.isEmpty() ? getString(R.string.subtitle_none) : suffix
        );
    }

    private boolean isOffline() {
        return !isOnlineLive.getValue();
    }

    public ConnectivityLiveData getIsOnlineLive() {
        return isOnlineLive;
    }

    @NonNull
    public MutableLiveData<Boolean> getIsLoadingLive() {
        return isLoadingLive;
    }

    @NonNull
    public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
        return infoFullscreenLive;
    }

    public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
        currentQueueLoading = queueLoading;
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
