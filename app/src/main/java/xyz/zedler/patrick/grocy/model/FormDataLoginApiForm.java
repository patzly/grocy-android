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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.google.android.material.textfield.TextInputEditText;
import java.net.URI;
import java.net.URISyntaxException;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.LoginApiFormFragmentArgs;
import xyz.zedler.patrick.grocy.util.Constants;

public class FormDataLoginApiForm {

  private final MutableLiveData<Boolean> showHelpTexts;
  private final MutableLiveData<Boolean> httpRadioButtonCheckedLive;
  private final MutableLiveData<Boolean> httpsRadioButtonCheckedLive;
  private final MutableLiveData<String> serverUrlLive;
  private final MutableLiveData<Integer> serverUrlErrorLive;
  private final LiveData<Boolean> serverUrlValidLive;
  private final LiveData<String> serverUrlPortInfoLive;
  private final MutableLiveData<String> longLivedAccessTokenLive;
  private final MutableLiveData<Integer> longLivedAccessTokenErrorLive;
  private final MutableLiveData<String> ingressProxyIdLive;
  private final MutableLiveData<Integer> ingressProxyIdErrorLive;
  private final MutableLiveData<String> apiKeyLive;
  private final MutableLiveData<Integer> apiKeyErrorLive;
  private final MutableLiveData<Boolean> usingGrocyHassAddOnLive;

  public FormDataLoginApiForm(Application application, LoginApiFormFragmentArgs args) {
    SharedPreferences sharedPrefsPrivate = application.getSharedPreferences(
        Constants.PREF.CREDENTIALS,
        Context.MODE_PRIVATE
    );
    showHelpTexts = new MutableLiveData<>(false);
    usingGrocyHassAddOnLive = new MutableLiveData<>(args.getGrocyIngressProxyId() != null);
    httpRadioButtonCheckedLive = new MutableLiveData<>(false);
    httpsRadioButtonCheckedLive = new MutableLiveData<>(false);
    serverUrlLive = new MutableLiveData<>();
    serverUrlErrorLive = new MutableLiveData<>();
    serverUrlValidLive = Transformations.map(
        serverUrlLive,
        serverUrl -> isServerUrlValid()
    );
    serverUrlPortInfoLive = Transformations.map(
        serverUrlLive,
        serverUrl -> {
          int port;
          boolean hasPortInUrl = false;
          try {
            URI uri = new URI(serverUrl);
            port = uri.getPort();
            if (port != -1) hasPortInUrl = true;
          } catch (URISyntaxException e) {
            port = -1;
          }
          if (port == -1 && URLUtil.isHttpUrl(serverUrl)) {
            port = 80;
          } else if (port == -1 && URLUtil.isHttpsUrl(serverUrl)) {
            port = 443;
          }
          if (hasPortInUrl) {
            return application.getString(
                R.string.msg_port_connection_info_with_port,
                String.valueOf(port)
            );
          } else {
            return application.getString(
                R.string.msg_port_connection_info_without_port,
                String.valueOf(port)
            );
          }
        }
    );
    longLivedAccessTokenLive = new MutableLiveData<>();
    longLivedAccessTokenErrorLive = new MutableLiveData<>();
    ingressProxyIdLive = new MutableLiveData<>(args.getGrocyIngressProxyId());
    ingressProxyIdErrorLive = new MutableLiveData<>();
    apiKeyLive = new MutableLiveData<>(args.getGrocyApiKey());
    apiKeyErrorLive = new MutableLiveData<>();

    if (sharedPrefsPrivate.getString(Constants.PREF.SERVER_URL, null) != null) {
      serverUrlLive.setValue(
          sharedPrefsPrivate.getString(Constants.PREF.SERVER_URL, null)
      );
    }
    if (sharedPrefsPrivate.getString(Constants.PREF.API_KEY, null) != null) {
      apiKeyLive.setValue(
          sharedPrefsPrivate.getString(Constants.PREF.API_KEY, null)
      );
    }
  }

  public MutableLiveData<Boolean> getShowHelpTexts() {
    return showHelpTexts;
  }

  public void toggleShowHelpTexts() {
    assert showHelpTexts.getValue() != null;
    showHelpTexts.setValue(!showHelpTexts.getValue());
  }

  public MutableLiveData<Boolean> getUsingGrocyHassAddOnLive() {
    return usingGrocyHassAddOnLive;
  }

  public boolean getUsingGrocyHassAddOn() {
    assert usingGrocyHassAddOnLive.getValue() != null;
    return usingGrocyHassAddOnLive.getValue();
  }

  public void toggleUsingGrocyHassAddOn() {
    usingGrocyHassAddOnLive.setValue(!getUsingGrocyHassAddOn());
  }

  public MutableLiveData<Boolean> getHttpRadioButtonCheckedLive() {
    return httpRadioButtonCheckedLive;
  }

  public MutableLiveData<Boolean> getHttpsRadioButtonCheckedLive() {
    return httpsRadioButtonCheckedLive;
  }

  public MutableLiveData<String> getServerUrlLive() {
    return serverUrlLive;
  }

  @NonNull
  public String getServerUrlTrimmed() {
    if (serverUrlLive.getValue() == null) {
      return "";
    }
    return serverUrlLive.getValue().replaceAll("/+$", "").trim();
  }

  public MutableLiveData<Integer> getServerUrlErrorLive() {
    return serverUrlErrorLive;
  }

  public LiveData<Boolean> getServerUrlValidLive() {
    return serverUrlValidLive;
  }

  public LiveData<String> getServerUrlPortInfoLive() {
    return serverUrlPortInfoLive;
  }

  public void clearServerUrlErrorAndUpdateRadioButtons() {
    serverUrlErrorLive.setValue(null);
    updateRadioButtons();
  }

  public void onCheckedHttpsButton(TextInputEditText editTextServer) {
    String serverUrl = serverUrlLive.getValue() != null ? serverUrlLive.getValue() : "";
    if (!serverUrl.contains("https://") && !serverUrl.contains("http://")) {
      serverUrlLive.setValue("https://" + serverUrl);
      cursorJumpToEnd(editTextServer);
    } else if (serverUrl.contains("http://")) {
      serverUrlLive.setValue(serverUrl.replace("http://", "https://"));
      cursorJumpToEnd(editTextServer);
    }
    updateRadioButtons();
  }

  public void onCheckedHttpButton(TextInputEditText editTextServer) {
    String serverUrl = serverUrlLive.getValue() != null ? serverUrlLive.getValue() : "";
    if (!serverUrl.contains("https://") && !serverUrl.contains("http://")) {
      serverUrlLive.setValue("http://" + serverUrl);
      cursorJumpToEnd(editTextServer);
    } else if (serverUrl.contains("https://")) {
      serverUrlLive.setValue(serverUrl.replace("https://", "http://"));
      cursorJumpToEnd(editTextServer);
    }
    updateRadioButtons();
  }

  private void cursorJumpToEnd(TextInputEditText editTextServer) {
    editTextServer.post(() -> {
      if (editTextServer.getText() == null || !editTextServer.hasFocus()) return;
      editTextServer.setSelection(editTextServer.getText().length());
    });
  }

  public void updateRadioButtons() {
    String serverUrl = serverUrlLive.getValue() != null ? serverUrlLive.getValue() : "";
    httpsRadioButtonCheckedLive.setValue(serverUrl.contains("https://"));
    httpRadioButtonCheckedLive.setValue(serverUrl.contains("http://"));
  }

  public MutableLiveData<String> getLongLivedAccessTokenLive() {
    return longLivedAccessTokenLive;
  }

  @Nullable
  public String getLongLivedAccessTokenTrimmed() {
    String longLivedAccessToken = longLivedAccessTokenLive.getValue();
    if (longLivedAccessToken != null) {
      longLivedAccessToken = longLivedAccessToken.trim();
    }
    return longLivedAccessToken != null && longLivedAccessToken.isEmpty()
        ? null : longLivedAccessToken;
  }

  public MutableLiveData<Integer> getLongLivedAccessTokenErrorLive() {
    return longLivedAccessTokenErrorLive;
  }

  public MutableLiveData<String> getIngressProxyIdLive() {
    return ingressProxyIdLive;
  }

  @Nullable
  public String getIngressProxyIdTrimmed() {
    String proxyId = ingressProxyIdLive.getValue();
    if (proxyId != null) {
      proxyId = proxyId.trim();
    }
    return proxyId != null && proxyId.isEmpty() ? null : proxyId;
  }

  public MutableLiveData<Integer> getIngressProxyIdErrorLive() {
    return ingressProxyIdErrorLive;
  }

  public MutableLiveData<String> getApiKeyLive() {
    return apiKeyLive;
  }

  @NonNull
  public String getApiKeyTrimmed() {
    if (apiKeyLive.getValue() == null) {
      return "";
    }
    return apiKeyLive.getValue().trim();
  }

  public MutableLiveData<Integer> getApiKeyErrorLive() {
    return apiKeyErrorLive;
  }

  public void clearApiKeyError() {
    apiKeyErrorLive.setValue(null);
  }

  public boolean isServerUrlValid() {
    String serverUrl = getServerUrlTrimmed();
    if (serverUrl.isEmpty()) {
      serverUrlErrorLive.setValue(R.string.error_empty);
      return false;
    } else if (!URLUtil.isHttpUrl(serverUrl) && !URLUtil.isHttpsUrl(serverUrl)) {
      serverUrlErrorLive.setValue(R.string.error_invalid_url);
      return false;
    }
    serverUrlErrorLive.setValue(null);
    return true;
  }

  public boolean isAccessTokenUrlValid() {
    String accessToken = getLongLivedAccessTokenTrimmed();
    if (getUsingGrocyHassAddOn() && accessToken == null) {
      longLivedAccessTokenErrorLive.setValue(R.string.error_empty);
      return false;
    }
    longLivedAccessTokenErrorLive.setValue(null);
    return true;
  }

  public boolean isIngressProxyIdValid() {
    String proxyId = getIngressProxyIdTrimmed();
    if (getUsingGrocyHassAddOn() && proxyId == null) {
      ingressProxyIdErrorLive.setValue(R.string.error_empty);
      return false;
    }
    ingressProxyIdErrorLive.setValue(null);
    return true;
  }

  public boolean isFormValid() {
    boolean valid = isServerUrlValid();
    valid = isAccessTokenUrlValid() && valid;
    valid = isIngressProxyIdValid() && valid;
    return valid;
  }
}
