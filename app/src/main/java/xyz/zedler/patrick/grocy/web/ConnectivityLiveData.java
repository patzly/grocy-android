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
 * A LiveData class which wraps the network connection status
 * Requires Permission: ACCESS_NETWORK_STATE
 *
 * See https://developer.android.com/training/monitoring-device-state/connectivity-monitoring
 * See https://developer.android.com/reference/android/net/ConnectivityManager
 * See https://developer.android.com/reference/android/net/ConnectivityManager#CONNECTIVITY_ACTION
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
        if(wasNeverActiveBefore) {
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