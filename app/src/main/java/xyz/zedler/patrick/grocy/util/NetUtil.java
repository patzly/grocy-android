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

package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import androidx.browser.customtabs.CustomTabsIntent;
import dev.gustavoavila.websocketclient.WebSocketClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.conscrypt.Conscrypt;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;

public class NetUtil {

  private final SharedPreferences sharedPrefs;
  private final ConnectivityManager cm;
  private WebSocketClient webSocketClient;
  private Timer hassSessionTimer;
  private TimerTask hassSessionTimerTask;
  private int hassWebsocketIdCounter;

  private final String TAG;
  private final boolean debug;

  public NetUtil(Activity activity, SharedPreferences sharedPrefs, boolean debug, String tag) {
    this.sharedPrefs = sharedPrefs;
    cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    this.TAG = tag;
    this.debug = debug;
  }

  public boolean isOnline() {
    if (cm == null) {
      return false;
    }
    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnectedOrConnecting();
  }

  public void insertConscrypt() {
    Security.insertProviderAt(Conscrypt.newProvider(), 1);

    try {
      Conscrypt.Version version = Conscrypt.version();
      if (debug) {
        Log.i(TAG, "insertConscrypt: Using Conscrypt/" + version.major() + "."
            + version.minor() + "." + version.patch() + " for TLS");
      }
      SSLEngine engine = SSLContext.getDefault().createSSLEngine();
      if (debug) {
        Log.i(TAG, "Enabled protocols: "
            + Arrays.toString(engine.getEnabledProtocols()) + " }");
      }
      if (debug) {
        Log.i(TAG, "Enabled ciphers: "
            + Arrays.toString(engine.getEnabledCipherSuites()) + " }");
      }
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "insertConscrypt: NoSuchAlgorithmException");
      Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
    }
  }

  public void createWebSocketClient() {
    String hassLongLivedAccessToken = sharedPrefs
        .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
    String hassServerUrl = sharedPrefs.getString(PREF.HOME_ASSISTANT_SERVER_URL, null);
    if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()
        || hassServerUrl == null || hassServerUrl.isEmpty()) {
      return;
    }
    if (webSocketClient != null) {
      webSocketClient.close(0, 0, "recreate websocket client");
    }

    URI uri;
    try {
      String hassWebSocketUrl = hassServerUrl
          .replaceFirst("https", "wss")
          .replaceFirst("http", "ws");
      uri = new URI(hassWebSocketUrl + "/api/websocket");
    }
    catch (URISyntaxException e) {
      e.printStackTrace();
      return;
    }

    hassWebsocketIdCounter = 1;
    webSocketClient = new WebSocketClient(uri) {
      @Override
      public void onOpen() {}
      @Override
      public void onPingReceived(byte[] data) {}
      @Override
      public void onPongReceived(byte[] data) {}
      @Override
      public void onBinaryReceived(byte[] data) {}

      @Override
      public void onTextReceived(String message) {
        if (message.contains("auth_required")) {
          String hassLongLivedAccessToken = sharedPrefs
              .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
          if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()) return;
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "auth");
            jsonObject.put("access_token", hassLongLivedAccessToken);
            webSocketClient.send(jsonObject.toString());
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else if (message.contains("auth_ok")) {
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "supervisor/api");
            jsonObject.put("endpoint", "/ingress/session");
            jsonObject.put("method", "post");
            jsonObject.put("id", hassWebsocketIdCounter);
            webSocketClient.send(jsonObject.toString());
            hassWebsocketIdCounter++;
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else if (message.contains("result")) {
          try {
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.getBoolean("success")) {
              if (jsonObject.has("result")
                  && jsonObject.getJSONObject("result").has("session")) {
                sharedPrefs.edit().putString(
                    PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY,
                    jsonObject.getJSONObject("result").getString("session")
                ).apply();
              } else {
                if (debug) Log.i(TAG, "onTextReceived: " + message);
              }
            } else {
              Log.e(TAG, "createWebSocketClient: onTextReceived: " + message);
            }


          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        } else {
          if (debug) Log.i(TAG, "createWebSocketClient: onTextReceived: " + message);
        }
      }

      @Override
      public void onException(Exception e) {
        Log.e(TAG, "createWebSocketClient: onException: " + e.getMessage());
      }

      @Override
      public void onCloseReceived(int reason, String description) {
        if (debug) Log.i(TAG, "createWebSocketClient: onCloseReceived: " + description);
        new Handler().postDelayed(() -> webSocketClient.connect(), 5000);
      }
    };

    webSocketClient.setConnectTimeout(10000);
    webSocketClient.setReadTimeout(60000);
    webSocketClient.enableAutomaticReconnection(5000);
    webSocketClient.connect();
  }

  public void resetHassSessionTimer() {
    String hassLongLivedAccessToken = sharedPrefs
        .getString(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN, null);
    if (hassLongLivedAccessToken == null || hassLongLivedAccessToken.isEmpty()) {
      return;
    }
    hassSessionTimer = new Timer();
    if (hassSessionTimerTask != null) {
      hassSessionTimerTask.cancel();
    }
    hassSessionTimerTask = new TimerTask() {
      @Override
      public void run() {
        if (debug) {
          Log.i(TAG, "Home Assistant session: validate session token");
        }
        if (webSocketClient != null) {
          String sessionToken = sharedPrefs
              .getString(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY, null);
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "supervisor/api");
            jsonObject.put("endpoint", "/ingress/validate_session");
            jsonObject.put("method", "post");
            JSONObject innerJsonObject = new JSONObject();
            innerJsonObject.put("session", sessionToken);
            jsonObject.put("data", innerJsonObject);
            jsonObject.put("id", hassWebsocketIdCounter);
            webSocketClient.send(jsonObject.toString());
            hassWebsocketIdCounter++;
          } catch (JSONException e) {
            Log.e(TAG, "createWebSocketClient: onTextReceived: " + e);
          }
        }
      }
    };
    hassSessionTimer.schedule(hassSessionTimerTask, 60 * 1000L, 60 * 1000L);
  }

  public void cancelHassSessionTimer() {
    if (hassSessionTimer != null) {
      hassSessionTimer.cancel();
    }
  }

  public void closeWebSocketClient(String reason) {
    if (webSocketClient != null) {
      webSocketClient.close(0, 0, reason);
    }
  }

  public static boolean openURL(MainActivity activity, String url) {
    boolean useSliding = activity.getSharedPrefs().getBoolean(
        Constants.SETTINGS.APPEARANCE.USE_SLIDING,
        Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
    );
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    if (useSliding) {
      builder.setStartAnimations(activity, R.anim.slide_from_end, R.anim.slide_to_start);
      builder.setExitAnimations(activity, R.anim.slide_from_start, R.anim.slide_to_end);
    } else {
      builder.setStartAnimations(activity, R.anim.enter_end_fade, R.anim.exit_start_fade);
      builder.setExitAnimations(activity, R.anim.enter_start_fade, R.anim.exit_end_fade);
    }
    CustomTabsIntent customTabsIntent = builder.build();
    try {
      customTabsIntent.launchUrl(activity, Uri.parse(url));
      return true;
    } catch (ActivityNotFoundException ex) {
      return false;
    }
  }
}
