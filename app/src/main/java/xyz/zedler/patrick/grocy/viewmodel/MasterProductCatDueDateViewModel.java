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
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataMasterProductCatDueDate;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterProductCatDueDateViewModel extends AndroidViewModel {

    private static final String TAG = MasterProductCatDueDateViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final EventHandler eventHandler;
    private final MasterProductRepository repository;
    private final FormDataMasterProductCatDueDate formData;
    private final MasterProductFragmentArgs args;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> offlineLive;

    private DownloadHelper.Queue currentQueueLoading;
    private final boolean debug;
    private final boolean isActionEdit;

    public MasterProductCatDueDateViewModel(
            @NonNull Application application,
            @NonNull MasterProductFragmentArgs startupArgs
    ) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();
        repository = new MasterProductRepository(application);
        formData = new FormDataMasterProductCatDueDate(application);
        args = startupArgs;
        isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);
    }

    public FormDataMasterProductCatDueDate getFormData() {
        return formData;
    }

    public boolean isActionEdit() {
        return isActionEdit;
    }

    public Product getFilledProduct() {
        return formData.fillProduct(args.getProduct());
    }

    public void fillData() {
        formData.fillWithProductIfNecessary(args.getProduct());
    }

    @NonNull
    public MutableLiveData<Boolean> getOfflineLive() {
        return offlineLive;
    }

    public Boolean isOffline() {
        return offlineLive.getValue();
    }

    public void setOfflineLive(boolean isOffline) {
        offlineLive.setValue(isOffline);
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

    private void showErrorMessage() {
        showMessage(getString(R.string.error_undefined));
    }

    private void showMessage(@NonNull String message) {
        showSnackbar(new SnackbarMessage(message));
    }

    private void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
        eventHandler.setValue(snackbarMessage);
    }

    private void showBottomSheet(BaseBottomSheet bottomSheet, Bundle bundle) {
        eventHandler.setValue(new BottomSheetEvent(bottomSheet, bundle));
    }

    private void navigateUp() {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return Event.NAVIGATE_UP;}
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

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }

    public static class MasterProductCatDueDateViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MasterProductFragmentArgs args;

        public MasterProductCatDueDateViewModelFactory(
                Application application,
                MasterProductFragmentArgs args
        ) {
            this.application = application;
            this.args = args;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MasterProductCatDueDateViewModel(application, args);
        }
    }
}
