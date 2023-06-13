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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.web;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomByteArrayRequest extends Request<byte[]> {

  private final String url;
  private final String apiKey;
  private final String homeAssistantIngressSessionKey;
  private final String hassLongLivedAccessToken;
  private final byte[] content;
  private final Runnable successListener;
  private final Runnable onRequestFinished;

  public CustomByteArrayRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      byte[] content,
      @Nullable Runnable successListener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag
  ) {
    super(method, url, error -> {
      if (onRequestFinished != null) {
        onRequestFinished.run();
      }
      if (errorListener != null) {
        errorListener.onErrorResponse(error);
      }
    });
    this.successListener = successListener;
    this.onRequestFinished = onRequestFinished;
    this.content = content;
    this.url = url;
    this.apiKey = apiKey;
    this.homeAssistantIngressSessionKey = homeAssistantIngressSessionKey;
    this.hassLongLivedAccessToken = null;
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

  @Override
  protected void deliverResponse(byte[] response) {
    if (onRequestFinished != null) {
      onRequestFinished.run();
    }
    if (successListener != null) successListener.run();
  }

  @Override
  protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
    return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
  }

  @Override
  public byte[] getBody() {
    return content;
  }

  @Override
  public String getBodyContentType() {
    return "application/octet-stream";
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> params = new HashMap<>();
    if (apiKey != null && !apiKey.isEmpty()) {
      params.put("GROCY-API-KEY", apiKey);
    }
    if (hassLongLivedAccessToken != null && !hassLongLivedAccessToken.isEmpty()) {
      params.put("Authorization", "Bearer " + hassLongLivedAccessToken);
    } else {
      Matcher matcher = Pattern.compile("(http|https)://(\\S+):(\\S+)@(\\S+)").matcher(url);
      if (matcher.matches()) {
        String user = matcher.group(2);
        String password = matcher.group(3);
        byte[] combination = (user + ":" + password).getBytes();
        String encoded = Base64.encodeToString(combination, Base64.DEFAULT);
        params.put("Authorization", "Basic " + encoded);
      }
    }
    if (homeAssistantIngressSessionKey != null) {
      params.put("Cookie", "ingress_session=" + homeAssistantIngressSessionKey);
    }
    return params.isEmpty() ? Collections.emptyMap() : params;
  }
}
