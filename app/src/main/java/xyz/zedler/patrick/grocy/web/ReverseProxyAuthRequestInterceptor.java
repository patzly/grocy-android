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
import com.android.volley.AuthFailureError;
import com.android.volley.Response;

/** Completes string requests that were replaced by a reverse-proxy login response. */
final class ReverseProxyAuthRequestInterceptor {

  private ReverseProxyAuthRequestInterceptor() {}

  static boolean intercept(
      String requestUrl,
      @Nullable String response,
      @Nullable Response.ErrorListener errorListener
  ) {
    if (!ReverseProxyAuthManager.handleResponse(requestUrl, response)) {
      return false;
    }
    if (errorListener != null) {
      errorListener.onErrorResponse(
          new AuthFailureError("Reverse proxy authentication required")
      );
    }
    return true;
  }
}
