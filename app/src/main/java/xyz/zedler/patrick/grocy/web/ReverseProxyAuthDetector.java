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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Pure response and origin checks shared by onboarding and session renewal. */
public final class ReverseProxyAuthDetector {

  private ReverseProxyAuthDetector() {}

  public static boolean looksLikeLoginPage(@Nullable String response) {
    if (response == null) {
      return false;
    }
    String lower = response.toLowerCase(Locale.ROOT);
    return lower.contains("<!doctype html")
        || lower.contains("<html")
        || lower.contains("outpost.goauthentik.io")
        || lower.contains("/if/flow/");
  }

  public static boolean isSameOrigin(@Nullable String firstUrl, @Nullable String secondUrl) {
    if (firstUrl == null || secondUrl == null) {
      return false;
    }
    try {
      URI first = new URI(firstUrl);
      URI second = new URI(secondUrl);
      return first.getScheme() != null
          && first.getScheme().equalsIgnoreCase(second.getScheme())
          && first.getHost() != null
          && first.getHost().equalsIgnoreCase(second.getHost())
          && effectivePort(first) == effectivePort(second);
    } catch (URISyntaxException e) {
      return false;
    }
  }

  public static boolean isAuthenticationPath(@Nullable String path) {
    return path != null && (path.startsWith("/outpost.goauthentik.io/")
        || path.startsWith("/if/flow/"));
  }

  private static int effectivePort(URI uri) {
    if (uri.getPort() >= 0) {
      return uri.getPort();
    }
    return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }
}
