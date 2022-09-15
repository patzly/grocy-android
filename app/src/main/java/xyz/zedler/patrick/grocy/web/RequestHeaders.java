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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.preference.PreferenceManager;
import com.bumptech.glide.load.model.LazyHeaders;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;

public class RequestHeaders {

  public static HashMap<String, String> getGrocyAuthHeaders(Application application) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    String serverUrl = sharedPrefs.getString(PREF.SERVER_URL, "");
    String apiKey = sharedPrefs.getString(Constants.PREF.API_KEY, "");
    String homeAssistantIngressSessionKey = sharedPrefs
        .getString(Constants.PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);

    HashMap<String, String> params = new HashMap<>();
    Matcher matcher = Pattern.compile("(http|https)://(\\S+):(\\S+)@(\\S+)")
        .matcher(serverUrl != null ? serverUrl : "");
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
    if (homeAssistantIngressSessionKey != null) {
      params.put("Cookie", "ingress_session=" + homeAssistantIngressSessionKey);
    }
    return params;
  }

  public static LazyHeaders getGlideGrocyAuthHeaders(Context context) {
    LazyHeaders.Builder headersBuilder = new LazyHeaders.Builder();
    HashMap<String, String> authHeaders = RequestHeaders
        .getGrocyAuthHeaders((Application) context.getApplicationContext());
    for (HashMap.Entry<String, String> entry : authHeaders.entrySet()) {
      headersBuilder.addHeader(entry.getKey(), entry.getValue());
    }
    return headersBuilder.build();
  }

}
