/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.LoginRequestFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.util.ConfigUtil;
import xyz.zedler.patrick.grocy.util.Constants;

public class LoginRequestViewModel extends BaseViewModel {

    private static final String TAG = LoginRequestViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final SharedPreferences sharedPrefsPrivate;
    private final DownloadHelper dlHelper;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> loginErrorOccurred;
    private final MutableLiveData<String> loginErrorMsg;
    private final MutableLiveData<String> loginErrorExactMsg;

    private final String serverUrl;
    private final String homeAssistantServerUrl;
    private final String homeAssistantLongLivedToken;
    private final String apiKey;
    private final boolean useHassLoginFlow;
    private final boolean debug;

    public LoginRequestViewModel(@NonNull Application application, LoginRequestFragmentArgs args) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        sharedPrefsPrivate = getApplication().getSharedPreferences(
                Constants.PREF.CREDENTIALS,
                Context.MODE_PRIVATE
        );
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        serverUrl = args.getGrocyServerUrl();
        homeAssistantServerUrl = args.getHomeAssistantServerUrl();
        homeAssistantLongLivedToken = args.getHomeAssistantToken();
        apiKey = args.getGrocyApiKey();
        useHassLoginFlow = args.getHomeAssistantServerUrl() != null;

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(
                getApplication(),
                serverUrl,
                apiKey,
                homeAssistantServerUrl,
                homeAssistantLongLivedToken,
                TAG,
                isLoadingLive::setValue
        );

        infoFullscreenLive = new MutableLiveData<>();
        loginErrorOccurred = new MutableLiveData<>(false);
        loginErrorMsg = new MutableLiveData<>();
        loginErrorExactMsg = new MutableLiveData<>();
    }

    public void login(boolean checkVersion) {
        loginErrorOccurred.setValue(false);
        loginErrorMsg.setValue(null);
        loginErrorExactMsg.setValue(null);

        dlHelper.getSystemInfo(
                response -> {
                    if(!response.contains("grocy_version")) {
                        loginErrorOccurred.setValue(true);
                        loginErrorMsg.setValue(getString(R.string.error_not_grocy_instance));
                        return;
                    }
                    try {
                        String grocyVersion = new JSONObject(response)
                                .getJSONObject("grocy_version")
                                .getString("Version");
                        ArrayList<String> supportedVersions = new ArrayList<>(
                                Arrays.asList(getResources()
                                        .getStringArray(R.array.compatible_grocy_versions))
                        );
                        if(checkVersion && !supportedVersions.contains(grocyVersion)) {
                            showCompatibilityBottomSheet(supportedVersions, grocyVersion);
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "requestLogin: " + e);
                    }

                    if(debug) Log.i(TAG, "requestLogin: successfully logged in");
                    sharedPrefs.edit()
                            .putString(Constants.PREF.SERVER_URL, serverUrl)
                            .putString(Constants.PREF.API_KEY, apiKey)
                            .apply();
                    if(useHassLoginFlow) {
                        sharedPrefs.edit().putString(
                                        Constants.PREF.HOME_ASSISTANT_SERVER_URL,
                                        homeAssistantServerUrl
                                ).putString(
                                        Constants.PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN,
                                        homeAssistantLongLivedToken
                                ).apply();
                    }
                    if(!isDemoServer()) {
                        sharedPrefsPrivate.edit()
                                .putString(
                                        Constants.PREF.SERVER_URL,
                                        useHassLoginFlow ? homeAssistantServerUrl : serverUrl
                                ).putString(Constants.PREF.API_KEY, apiKey)
                                .apply();
                    }
                    loadInfoAndFinish();
                },
                error -> {
                    Log.e(TAG, "requestLogin: VolleyError: " + error);
                    if(error instanceof AuthFailureError) {
                        loginErrorOccurred.setValue(true);
                        loginErrorMsg.setValue(getString(R.string.error_api_not_working));
                        loginErrorExactMsg.setValue(error.toString());
                    } else if(error instanceof NoConnectionError) {
                        if(error.toString().contains("SSLHandshakeException")) {
                            showMessage("SSLHandshakeException");
                            loginErrorOccurred.setValue(true);
                            loginErrorMsg.setValue(getString(R.string.error_handshake));
                            loginErrorExactMsg.setValue(getString(R.string.error_handshake_description));
                        } else if(error.toString().contains("Invalid host")) {
                            loginErrorOccurred.setValue(true);
                            loginErrorMsg.setValue(getString(R.string.error_invalid_url));
                        } else {
                            loginErrorOccurred.setValue(true);
                            loginErrorMsg.setValue(getString(R.string.error_failed_to_connect_to));
                            loginErrorExactMsg.setValue("Server: " + serverUrl + "\n\nError: " + error.toString());
                        }
                    } else if(error instanceof ServerError && error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        loginErrorOccurred.setValue(true);
                        loginErrorExactMsg.setValue("Code: " + code);
                        if (code == 404) {
                            loginErrorMsg.setValue(getString(R.string.error_not_grocy_instance));
                        } else {
                            loginErrorMsg.setValue(getString(R.string.error_unexpected_response_code));
                        }
                    } else if(error instanceof ServerError) {
                        loginErrorOccurred.setValue(true);
                        loginErrorMsg.setValue(getString(R.string.error_unexpected_response));
                    } else if(error instanceof TimeoutError) {
                        loginErrorOccurred.setValue(true);
                        loginErrorMsg.setValue(getString(R.string.error_timeout));
                    } else {
                        loginErrorOccurred.setValue(true);
                        loginErrorMsg.setValue(getString(R.string.error_undefined));
                        loginErrorExactMsg.setValue(error.toString());
                    }
                }
        ).perform(dlHelper.getUuid());
    }

    private void loadInfoAndFinish() {
        ConfigUtil.loadInfo(
                dlHelper,
                new GrocyApi(getApplication()),
                sharedPrefs,
                () -> sendEvent(Event.LOGIN_SUCCESS),
                error -> sendEvent(Event.LOGIN_SUCCESS)
        );
    }

    private void showCompatibilityBottomSheet(ArrayList<String> supportedVersions, String grocyVersion) {
        sharedPrefs.edit().remove(Constants.PREF.VERSION_COMPATIBILITY_IGNORED).apply();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.VERSION, grocyVersion);
        bundle.putStringArrayList(Constants.ARGUMENT.SUPPORTED_VERSIONS, supportedVersions);
        showBottomSheet(new CompatibilityBottomSheet(), bundle);
    }

    public MutableLiveData<Boolean> getLoginErrorOccurred() {
        return loginErrorOccurred;
    }

    public MutableLiveData<String> getLoginErrorMsg() {
        return loginErrorMsg;
    }

    public MutableLiveData<String> getLoginErrorExactMsg() {
        return loginErrorExactMsg;
    }

    private boolean isDemoServer() {
        return serverUrl.contains("demo.grocy.info");
    }

    @NonNull
    public MutableLiveData<Boolean> getIsLoadingLive() {
        return isLoadingLive;
    }

    @NonNull
    public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
        return infoFullscreenLive;
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }

    public static class LoginViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final LoginRequestFragmentArgs args;

        public LoginViewModelFactory(Application application, LoginRequestFragmentArgs args) {
            this.application = application;
            this.args = args;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new LoginRequestViewModel(application, args);
        }
    }
}
