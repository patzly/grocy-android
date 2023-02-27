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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;

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
      newRequestQueue();
    }
    return requestQueue;
  }

  public void newRequestQueue() {
    //requestQueue = Volley.newRequestQueue(ctx);

    Cache cache = new DiskBasedCache(ctx.getCacheDir(), 1024 * 1024);

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    boolean useTor = sharedPrefs.getBoolean(NETWORK.TOR, SETTINGS_DEFAULT.NETWORK.TOR);
    boolean useProxy = sharedPrefs.getBoolean(NETWORK.PROXY, SETTINGS_DEFAULT.NETWORK.PROXY);

    HurlStack stack;
    if (useTor || useProxy) {
      stack = new ProxyHurlStack(sharedPrefs, useTor);
    } else {
      try {
        stack = new HurlStack(null, new TLSSocketFactory());
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        stack = new HurlStack();
      }
    }
    Network network = new BasicNetwork(stack);
    requestQueue = new RequestQueue(cache, network, 6);
    requestQueue.start();
  }

  private static class TLSSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory internalSSLSocketFactory;

    public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      internalSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
      if((socket instanceof SSLSocket)) {
        ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2", "TLSv1.3"});
      }
      return socket;
    }
  }
}