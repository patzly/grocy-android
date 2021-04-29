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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.web;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.StringRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;

public class CustomStringRequest extends StringRequest {

  private final Runnable onRequestFinished;
  private final String url;
  private final String apiKey;
  private final String homeAssistantIngressSessionKey;
  private final String userAgent;

  public CustomStringRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      Response.Listener<String> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag,
      @Nullable String userAgent,
      boolean noLoadingProgress,
      DownloadHelper.OnLoadingListener onLoadingListener
  ) {
    super(
        method,
        url,
        response -> {
          if (noLoadingProgress) {
            if (onLoadingListener != null) {
              onLoadingListener.onLoadingChanged(false);
            }
          } else if (onRequestFinished != null) {
            onRequestFinished.run();
          }
          listener.onResponse(response);
        },
        error -> {
          if (noLoadingProgress) {
            if (onLoadingListener != null) {
              onLoadingListener.onLoadingChanged(false);
            }
          } else if (onRequestFinished != null) {
            onRequestFinished.run();
          }
          if (errorListener != null) {
            errorListener.onErrorResponse(error);
          }
        }
    );
    this.onRequestFinished = onRequestFinished;
    this.url = url;
    this.apiKey = apiKey;
    this.homeAssistantIngressSessionKey = homeAssistantIngressSessionKey;
    this.userAgent = userAgent;
    if (tag != null) {
      setTag(tag);
    }
    setShouldCache(false);
    RetryPolicy policy = new DefaultRetryPolicy(
        timeoutSeconds * 1000,
        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    );
    setRetryPolicy(policy);
  }

  public CustomStringRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      Response.Listener<String> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag
  ) {
    this(
        method,
        url,
        apiKey,
        homeAssistantIngressSessionKey,
        listener,
        errorListener,
        onRequestFinished,
        timeoutSeconds,
        tag,
        null,
        false,
        null
    );
  }

  public CustomStringRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      Response.Listener<String> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag,
      String userAgent
  ) {
    this(
        method,
        url,
        apiKey,
        homeAssistantIngressSessionKey,
        listener,
        errorListener,
        onRequestFinished,
        timeoutSeconds,
        tag,
        userAgent,
        false,
        null
    );
  }

  public CustomStringRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      Response.Listener<String> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag,
      boolean noLoadingProgress,
      DownloadHelper.OnLoadingListener onLoadingListener
  ) {
    this(
        method,
        url,
        apiKey,
        homeAssistantIngressSessionKey,
        listener,
        errorListener,
        onRequestFinished,
        timeoutSeconds,
        tag,
        null,
        noLoadingProgress,
        onLoadingListener
    );
  }

  @Override
  public void cancel() {
    super.cancel();
    if (onRequestFinished != null) {
      onRequestFinished.run();
    }
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> params = new HashMap<>();

    Matcher matcher = Pattern.compile("(http|https)://(\\S+):(\\S+)@(\\S+)").matcher(url);
    if (matcher.matches()) {
      String user = matcher.group(2);
      String password = matcher.group(3);
      byte[] combination = (user + ":" + password).getBytes();
      String encoded = Base64.encodeToString(combination, Base64.DEFAULT);
      params.put("Authorization", "Basic " + encoded);
    }
    if (apiKey != null && !apiKey.isEmpty()) {
      params.put("GROCY-API-KEY", apiKey);
    }
    if (userAgent == null && homeAssistantIngressSessionKey
        != null) { // should not put key into requests for OpenFoodFacts
      params.put("Cookie", "ingress_session=" + homeAssistantIngressSessionKey);
    }
    if (userAgent != null) {
      params.put("User-Agent", userAgent);
    }
    return params.isEmpty() ? Collections.emptyMap() : params;
  }
}
