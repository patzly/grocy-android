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

package xyz.zedler.patrick.grocy.web;

import android.util.Base64;
import androidx.annotation.Nullable;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomJsonObjectRequest extends JsonObjectRequest {

  private final Runnable onRequestFinished;
  private final String url;
  private final String apiKey;
  private final String homeAssistantIngressSessionKey;
  private final String hassLongLivedAccessToken;

  public CustomJsonObjectRequest(
      int method,
      String url,
      String apiKey,
      String homeAssistantIngressSessionKey,
      @Nullable JSONObject jsonRequest,
      Response.Listener<JSONObject> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag
  ) {
    super(method, url, jsonRequest, response -> {
      if (onRequestFinished != null) {
        onRequestFinished.run();
      }
      listener.onResponse(response);
    }, error -> {
      if (onRequestFinished != null) {
        onRequestFinished.run();
      }
      if (errorListener != null) {
        errorListener.onErrorResponse(error);
      }
    });
    this.onRequestFinished = onRequestFinished;
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

  public CustomJsonObjectRequest( // for Home Assistant
      String url,
      String hassLongLivedAccessToken,
      @Nullable JSONObject jsonRequest,
      Response.Listener<JSONObject> listener,
      @Nullable Response.ErrorListener errorListener,
      @Nullable Runnable onRequestFinished,
      int timeoutSeconds,
      String tag
  ) {
    super(Method.POST, url, jsonRequest, response -> {
      if (onRequestFinished != null) {
        onRequestFinished.run();
      }
      listener.onResponse(response);
    }, error -> {
      if (onRequestFinished != null) {
        onRequestFinished.run();
      }
      if (errorListener != null) {
        errorListener.onErrorResponse(error);
      }
    });
    this.onRequestFinished = onRequestFinished;
    this.url = url;
    this.apiKey = null;
    this.homeAssistantIngressSessionKey = null;
    this.hassLongLivedAccessToken = hassLongLivedAccessToken;
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
  protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
    try {
      String jsonString = new String(
          response.data,
          HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET)
      );
      JSONObject result = null;
      if (jsonString.length() > 0) {
        result = new JSONObject(jsonString);
      }
      return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
    } catch (UnsupportedEncodingException | JSONException e) {
      return Response.error(new ParseError(e));
    }
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
