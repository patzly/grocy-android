package xyz.zedler.patrick.grocy.web;

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