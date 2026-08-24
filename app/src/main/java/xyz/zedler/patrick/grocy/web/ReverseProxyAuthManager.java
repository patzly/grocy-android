/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.web;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/** Coordinates reauthentication when an API response is replaced by a proxy login page. */
public final class ReverseProxyAuthManager {

  private static final MutableLiveData<String> authenticationRequired = new MutableLiveData<>();
  private static String configuredServer;

  private ReverseProxyAuthManager() {}

  public static void configure(@Nullable String serverUrl) {
    configuredServer = serverUrl == null || serverUrl.isEmpty() ? null : serverUrl;
  }

  public static LiveData<String> getAuthenticationRequired() {
    return authenticationRequired;
  }

  public static void consumeAuthenticationRequest() {
    authenticationRequired.setValue(null);
  }

  /** Consumes every delivered challenge, but only opens a new flow when none is active. */
  public static boolean shouldLaunchAuthentication(
      @Nullable String serverUrl,
      boolean authenticationOpen
  ) {
    if (serverUrl == null) {
      return false;
    }
    consumeAuthenticationRequest();
    return !authenticationOpen;
  }

  public static boolean handleResponse(String requestUrl, @Nullable String response) {
    if (!isConfiguredServerRequest(requestUrl) || !ReverseProxyAuthDetector.looksLikeLoginPage(response)) {
      return false;
    }
    authenticationRequired.postValue(configuredServer);
    return true;
  }

  static boolean isConfiguredServerRequest(String requestUrl) {
    if (configuredServer == null) {
      return false;
    }
    return ReverseProxyAuthDetector.isSameOrigin(configuredServer, requestUrl);
  }
}
