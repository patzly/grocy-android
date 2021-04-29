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

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;

/**
 * A LiveData class which wraps the network connection status Requires Permission:
 * ACCESS_NETWORK_STATE
 * <p>
 * See https://developer.android.com/training/monitoring-device-state/connectivity-monitoring See
 * https://developer.android.com/reference/android/net/ConnectivityManager See
 * https://developer.android.com/reference/android/net/ConnectivityManager#CONNECTIVITY_ACTION
 */
public class ConnectivityLiveData extends LiveData<Boolean> {

  private boolean wasNeverActiveBefore = true;
  private final ConnectivityManager connectivityManager;
  private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
    @Override
    public void onAvailable(@NonNull Network network) {
      postValue(true);
    }

    @Override
    public void onUnavailable() {
      postValue(false);
    }

    @Override
    public void onLost(@NonNull Network network) {
      postValue(false);
    }
  };

  @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
  public ConnectivityLiveData(Application application) {
    super(((ConnectivityManager) application
        .getSystemService(Context.CONNECTIVITY_SERVICE))
        .getActiveNetworkInfo().isConnectedOrConnecting());
    connectivityManager = (ConnectivityManager) application
        .getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  @Override
  protected void onActive() {
    super.onActive();
    if (wasNeverActiveBefore) {
      wasNeverActiveBefore = false;
    } else {
      postValue(connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting());
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      connectivityManager.registerDefaultNetworkCallback(networkCallback);
    } else {
      NetworkRequest.Builder builder = new NetworkRequest.Builder();
      connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }
  }

  @Override
  protected void onInactive() {
    super.onInactive();
    connectivityManager.unregisterNetworkCallback(networkCallback);
  }
}