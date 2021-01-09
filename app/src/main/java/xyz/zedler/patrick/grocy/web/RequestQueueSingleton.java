package xyz.zedler.patrick.grocy.web;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import xyz.zedler.patrick.grocy.util.Constants;

public class RequestQueueSingleton {
    private static RequestQueueSingleton instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private RequestQueueSingleton(Context context) {
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();
    }

    public static synchronized RequestQueueSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new RequestQueueSingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            String serverUrl = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
            newRequestQueue(serverUrl);
        }
        return requestQueue;
    }

    public void newRequestQueue(String serverUrl) {
        //requestQueue = Volley.newRequestQueue(ctx);

        Cache cache = new DiskBasedCache(ctx.getCacheDir(), 1024 * 1024);

        BasicNetwork network;
        if(serverUrl != null && serverUrl.contains(".onion")) {
            network = new BasicNetwork(new ProxyHurlStack());
        } else {
            network = new BasicNetwork(new HurlStack());
        }
        requestQueue = new RequestQueue(cache, network, 6);
        requestQueue.start();
    }
}