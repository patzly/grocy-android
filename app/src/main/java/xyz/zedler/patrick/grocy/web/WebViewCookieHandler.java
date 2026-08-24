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

import android.webkit.CookieManager;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Makes WebView authentication cookies available to Volley's HttpURLConnection stack.
 *
 * <p>This is used for servers protected by an interactive reverse proxy such as authentik.
 * The identity provider remains responsible for setting and expiring the session cookie.</p>
 */
public final class WebViewCookieHandler extends CookieHandler {

  private static final String COOKIE = "Cookie";
  private static final String SET_COOKIE = "Set-Cookie";
  private static final String SET_COOKIE_2 = "Set-Cookie2";

  private final CookieManager cookieManager;

  public WebViewCookieHandler() {
    cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
  }

  @Override
  public Map<String, List<String>> get(
      URI uri,
      Map<String, List<String>> requestHeaders
  ) throws IOException {
    if (!isHttp(uri)) {
      return Collections.emptyMap();
    }
    String cookies = cookieManager.getCookie(uri.toString());
    if (cookies == null || cookies.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(COOKIE, Collections.singletonList(cookies));
  }

  @Override
  public void put(
      URI uri,
      Map<String, List<String>> responseHeaders
  ) throws IOException {
    if (!isHttp(uri) || responseHeaders == null) {
      return;
    }
    for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
      String name = entry.getKey();
      if (name == null || (!SET_COOKIE.equalsIgnoreCase(name)
          && !SET_COOKIE_2.equalsIgnoreCase(name))) {
        continue;
      }
      for (String cookie : entry.getValue()) {
        cookieManager.setCookie(uri.toString(), cookie);
      }
    }
    cookieManager.flush();
  }

  private static boolean isHttp(URI uri) {
    if (uri == null || uri.getScheme() == null) {
      return false;
    }
    return "http".equalsIgnoreCase(uri.getScheme())
        || "https".equalsIgnoreCase(uri.getScheme());
  }
}
