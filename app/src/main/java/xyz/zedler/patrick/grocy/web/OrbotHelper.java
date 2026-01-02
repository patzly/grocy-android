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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.web;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Utility class to simplify setting up a proxy connection
 * to Orbot.
 * <p>
 * If you are using classes in the info.guardianproject.netcipher.client
 * package, call OrbotHelper.get(this).init(); from onCreate()
 * of a custom Application subclass, or from some other guaranteed
 * entry point to your app. At that point, the
 * info.guardianproject.netcipher.client classes will be ready
 * for use.
 */
public class OrbotHelper {

  private final static int REQUEST_CODE_STATUS = 100;

  public final static String ORBOT_PACKAGE_NAME = "org.torproject.android";
  public final static String ORBOT_MARKET_URI = "market://details?id=" + ORBOT_PACKAGE_NAME;
  public final static String ORBOT_FDROID_URI = "https://f-droid.org/repository/browse/?fdid="
      + ORBOT_PACKAGE_NAME;
  public final static String ORBOT_PLAY_URI = "https://play.google.com/store/apps/details?id="
      + ORBOT_PACKAGE_NAME;

  public final static String DEFAULT_PROXY_HOST = "localhost";//"127.0.0.1";
  public final static int DEFAULT_PROXY_HTTP_PORT = 8118;
  public final static int DEFAULT_PROXY_SOCKS_PORT = 9050;

  /**
   * A request to Orbot to transparently start Tor services
   */
  public final static String ACTION_START = "org.torproject.android.intent.action.START";

  /**
   * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
   * included as an {@link #EXTRA_STATUS} {@code String}.  Your app should
   * always receive {@code ACTION_STATUS Intent}s since any other app could
   * start Orbot.  Also, user-triggered starts and stops will also cause
   * {@code ACTION_STATUS Intent}s to be broadcast.
   */
  public final static String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";

  /**
   * {@code String} that contains a status constant: {@link #STATUS_ON},
   * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
   * {@link #STATUS_STOPPING}
   */
  public final static String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
  /**
   * A {@link String} {@code packageName} for Orbot to direct its status reply
   * to, used in {@link #ACTION_START} {@link Intent}s sent to Orbot
   */
  public final static String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";

  public final static String EXTRA_PROXY_PORT_HTTP = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";
  public final static String EXTRA_PROXY_PORT_SOCKS = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";


  /**
   * All tor-related services and daemons are stopped
   */
  public final static String STATUS_OFF = "OFF";
  /**
   * All tor-related services and daemons have completed starting
   */
  public final static String STATUS_ON = "ON";
  public final static String STATUS_STARTING = "STARTING";
  public final static String STATUS_STOPPING = "STOPPING";
  /**
   * The user has disabled the ability for background starts triggered by
   * apps. Fallback to the old Intent that brings up Orbot.
   */
  public final static String STATUS_STARTS_DISABLED = "STARTS_DISABLED";

  public final static String ACTION_START_TOR = "org.torproject.android.START_TOR";
  public final static String ACTION_REQUEST_HS = "org.torproject.android.REQUEST_HS_PORT";
  public final static int START_TOR_RESULT = 0x9234;
  public final static int HS_REQUEST_CODE = 9999;

  public final static String FDROID_PACKAGE_NAME = "org.fdroid.fdroid";
  public final static String PLAY_PACKAGE_NAME = "com.android.vending";

  /**
   * Test whether a {@link URL} is a Tor Hidden Service host name, also known
   * as an ".onion address".
   *
   * @return whether the host name is a Tor .onion address
   */
  public static boolean isOnionAddress(URL url) {
    return url.getHost().endsWith(".onion");
  }

  /**
   * Test whether a URL {@link String} is a Tor Hidden Service host name, also known
   * as an ".onion address".
   *
   * @return whether the host name is a Tor .onion address
   */
  public static boolean isOnionAddress(String urlString) {
    try {
      return isOnionAddress(new URL(urlString));
    } catch (MalformedURLException e) {
      return false;
    }
  }

  /**
   * Test whether a {@link Uri} is a Tor Hidden Service host name, also known
   * as an ".onion address".
   *
   * @return whether the host name is a Tor .onion address
   */
  public static boolean isOnionAddress(Uri uri) {
    return uri.getHost() != null && uri.getHost().endsWith(".onion");
  }

  public static boolean isOrbotInstalled(Context context) {
    try {
      PackageManager pm = context.getPackageManager();
      pm.getPackageInfo(OrbotHelper.ORBOT_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public static void requestHiddenServiceOnPort(Activity activity, int port) {
    Intent intent = new Intent(ACTION_REQUEST_HS);
    intent.setPackage(ORBOT_PACKAGE_NAME);
    intent.putExtra("hs_port", port);

    activity.startActivityForResult(intent, HS_REQUEST_CODE);
  }

  /**
   * First, checks whether Orbot is installed. If Orbot is installed, then a
   * broadcast {@link Intent} is sent to request Orbot to start
   * transparently in the background. When Orbot receives this {@code
   * Intent}, it will immediately reply to the app that called this method
   * with an {@link #ACTION_STATUS} {@code Intent} that is broadcast to the
   * {@code packageName} of the provided {@link Context} (i.e.  {@link
   * Context#getPackageName()}.
   * <p>
   * That reply {@link #ACTION_STATUS} {@code Intent} could say that the user
   * has disabled background starts with the status
   * {@link #STATUS_STARTS_DISABLED}. That means that Orbot ignored this
   * request.  To directly prompt the user to start Tor, use
   * #requestShowOrbotStart(Activity), which will bring up
   * Orbot itself for the user to manually start Tor.  Orbot always broadcasts
   * it's status, so your app will receive those no matter how Tor gets
   * started.
   *
   * @param context the app {@link Context} will receive the reply
   * @return whether the start request was sent to Orbot
   */
  public static boolean requestStartTor(Context context) {
    if (OrbotHelper.isOrbotInstalled(context)) {
      Log.i("OrbotHelper", "requestStartTor " + context.getPackageName());
      Intent intent = getOrbotStartIntent(context);
      context.sendBroadcast(intent);
      return true;
    }
    return false;
  }

  /**
   * Gets an {@link Intent} for starting Orbot.  Orbot will reply with the
   * current status to the {@code packageName} of the app in the provided
   * {@link Context} (i.e.  {@link Context#getPackageName()}.
   */
  public static Intent getOrbotStartIntent(Context context) {
    Intent intent = new Intent(ACTION_START);
    intent.setPackage(ORBOT_PACKAGE_NAME);
    intent.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
    return intent;
  }

  /**
   * Gets a barebones {@link Intent} for starting Orbot.  This is deprecated
   * in favor of {@link #getOrbotStartIntent(Context)}.
   */
  @Deprecated
  public static Intent getOrbotStartIntent() {
    Intent intent = new Intent(ACTION_START);
    intent.setPackage(ORBOT_PACKAGE_NAME);
    return intent;
  }

  public static Intent getShowOrbotStartIntent() {
    Intent intent = new Intent(ACTION_START_TOR);
    intent.setPackage(ORBOT_PACKAGE_NAME);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return intent;
  }

  public static Intent getOrbotInstallIntent(Context context) {
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(ORBOT_MARKET_URI));

    PackageManager pm = context.getPackageManager();
    List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, 0);

    String foundPackageName = null;
    for (ResolveInfo r : resInfos) {
      Log.i("OrbotHelper", "market: " + r.activityInfo.packageName);
      if (TextUtils.equals(r.activityInfo.packageName, FDROID_PACKAGE_NAME)
          || TextUtils.equals(r.activityInfo.packageName, PLAY_PACKAGE_NAME)) {
        foundPackageName = r.activityInfo.packageName;
        break;
      }
    }

    if (foundPackageName == null) {
      intent.setData(Uri.parse(ORBOT_FDROID_URI));
    } else {
      intent.setPackage(foundPackageName);
    }
    return intent;
  }

  public boolean isInstalled(Context context) {
    return isOrbotInstalled(context);
  }

  public boolean requestStart(Context context) {
    return requestStartTor(context);
  }

  public Intent getInstallIntent(Context context) {
    return getOrbotInstallIntent(context);
  }

  public Intent getStartIntent(Context context) {
    return getOrbotStartIntent(context);
  }

  public String getName() {
    return "Orbot";
  }

  /* MLM additions */

  private final Context context;
  private final Handler handler;
  private boolean isInstalled = false;
  @Nullable
  private Intent lastStatusIntent = null;
  private final Set<StatusCallback> statusCallbacks =
      newSetFromMap(new WeakHashMap<>());
  private final Set<OrbotHelper.InstallCallback> installCallbacks =
      newSetFromMap(new WeakHashMap<>());
  private long statusTimeoutMs = 30000L;
  private long installTimeoutMs = 60000L;
  private boolean validateOrbot = true;

  abstract public static class SimpleStatusCallback
      implements StatusCallback {
    @Override
    public void onEnabled(Intent statusIntent) {
      // no-op; extend and override if needed
    }

    @Override
    public void onStarting() {
      // no-op; extend and override if needed
    }

    @Override
    public void onStopping() {
      // no-op; extend and override if needed
    }

    @Override
    public void onDisabled() {
      // no-op; extend and override if needed
    }

    @Override
    public void onNotYetInstalled() {
      // no-op; extend and override if needed
    }
  }

  /**
   * Callback interface used for reporting the results of an
   * attempt to install Orbot
   */
  public interface InstallCallback {
    void onInstalled();

    void onInstallTimeout();
  }

  private static volatile OrbotHelper instance;

  /**
   * Retrieves the singleton, initializing if if needed
   *
   * @param context any Context will do, as we will hold onto
   *                the Application
   * @return the singleton
   */
  synchronized public static OrbotHelper get(Context context) {
    if (instance == null) {
      instance = new OrbotHelper(context);
    }

    return (instance);
  }

  /**
   * Standard constructor
   *
   * @param context any Context will do; OrbotInitializer will hold
   *                onto the Application context
   */
  private OrbotHelper(Context context) {
    this.context = context.getApplicationContext();
    this.handler = new Handler(Looper.getMainLooper());
  }

  /**
   * Adds a StatusCallback to be called when we find out that
   * Orbot is ready. If Orbot is ready for use, your callback
   * will be called with onEnabled() immediately, before this
   * method returns.
   *
   * @param cb a callback
   * @return the singleton, for chaining
   */
  public OrbotHelper addStatusCallback(StatusCallback cb) {
    statusCallbacks.add(cb);

    if (lastStatusIntent != null) {
      String status =
          lastStatusIntent.getStringExtra(OrbotHelper.EXTRA_STATUS);

      if (status != null && status.equals(OrbotHelper.STATUS_ON)) {
        cb.onEnabled(lastStatusIntent);
      }
    }

    return (this);
  }

  /**
   * Removes an existing registered StatusCallback.
   *
   * @param cb the callback to remove
   * @return the singleton, for chaining
   */
  public OrbotHelper removeStatusCallback(StatusCallback cb) {
    statusCallbacks.remove(cb);

    return (this);
  }


  /**
   * Adds an InstallCallback to be called when we find out that
   * Orbot is installed
   *
   * @param cb a callback
   * @return the singleton, for chaining
   */
  public OrbotHelper addInstallCallback(
      OrbotHelper.InstallCallback cb) {
    installCallbacks.add(cb);

    return (this);
  }

  /**
   * Removes an existing registered InstallCallback.
   *
   * @param cb the callback to remove
   * @return the singleton, for chaining
   */
  public OrbotHelper removeInstallCallback(
      OrbotHelper.InstallCallback cb) {
    installCallbacks.remove(cb);

    return (this);
  }

  /**
   * Sets how long of a delay, in milliseconds, after trying
   * to get a status from Orbot before we give up.
   * Defaults to 30000ms = 30 seconds = 0.000347222 days
   *
   * @param timeoutMs delay period in milliseconds
   * @return the singleton, for chaining
   */
  public OrbotHelper statusTimeout(long timeoutMs) {
    statusTimeoutMs = timeoutMs;

    return (this);
  }

  /**
   * Sets how long of a delay, in milliseconds, after trying
   * to install Orbot do we assume that it's not happening.
   * Defaults to 60000ms = 60 seconds = 1 minute = 1.90259e-6 years
   *
   * @param timeoutMs delay period in milliseconds
   * @return the singleton, for chaining
   */
  public OrbotHelper installTimeout(long timeoutMs) {
    installTimeoutMs = timeoutMs;

    return (this);
  }

  /**
   * By default, NetCipher ensures that the Orbot on the
   * device is one of the official builds. Call this method
   * to skip that validation. Mostly, this is for developers
   * who have their own custom Orbot builds (e.g., for
   * dedicated hardware).
   *
   * @return the singleton, for chaining
   */
  public OrbotHelper skipOrbotValidation() {
    validateOrbot = false;

    return (this);
  }

  /**
   * @return true if Orbot is installed (the last time we checked),
   * false otherwise
   */
  public boolean isInstalled() {
    return (isInstalled);
  }

  /**
   * Initializes the connection to Orbot, revalidating that it is installed
   * and requesting fresh status broadcasts.  This is best run in your app's
   * {@link android.app.Application} subclass, in its
   * {@link android.app.Application#onCreate()} method.
   *
   * @return true if initialization is proceeding, false if Orbot is not installed,
   * or version of Orbot with a unofficial signing key is present.
   */
  public boolean init() {
    Intent orbot = OrbotHelper.getOrbotStartIntent(context);

    if (validateOrbot) {
      ArrayList<String> hashes = new ArrayList<String>();

      // Tor Project signing key
      hashes.add("A4:54:B8:7A:18:47:A8:9E:D7:F5:E7:0F:BA:6B:BA:96:F3:EF:29:C2:6E:09:81:20:4F:E3:47:BF:23:1D:FD:5B");
      // f-droid.org signing key
      hashes.add("A7:02:07:92:4F:61:FF:09:37:1D:54:84:14:5C:4B:EE:77:2C:55:C1:9E:EE:23:2F:57:70:E1:82:71:F7:CB:AE");

      orbot =
          validateBroadcastIntent(context, orbot,
              hashes, false);
    }

    if (orbot != null) {
      isInstalled = true;
      handler.postDelayed(onStatusTimeout, statusTimeoutMs);
      ContextCompat.registerReceiver(context, orbotStatusReceiver,
          new IntentFilter(OrbotHelper.ACTION_STATUS), ContextCompat.RECEIVER_EXPORTED);
      context.sendBroadcast(orbot);
    } else {
      isInstalled = false;

      for (StatusCallback cb : statusCallbacks) {
        cb.onNotYetInstalled();
      }
    }

    return (isInstalled);
  }

  /**
   * Given that init() returned false, calling installOrbot()
   * will trigger an attempt to install Orbot from an available
   * distribution channel (e.g., the Play Store). Only call this
   * if the user is expecting it, such as in response to tapping
   * a dialog button or an action bar item.
   * <p>
   * Note that installation may take a long time, even if
   * the user is proceeding with the installation, due to network
   * speeds, waiting for user input, and so on. Either specify
   * a long timeout, or consider the timeout to be merely advisory
   * and use some other user input to cause you to try
   * init() again after, presumably, Orbot has been installed
   * and configured by the user.
   * <p>
   * If the user does install Orbot, we will attempt init()
   * again automatically. Hence, you will probably need user input
   * to tell you when the user has gotten Orbot up and going.
   *
   * @param host the Activity that is triggering this work
   */
  public void installOrbot(Activity host) {
    handler.postDelayed(onInstallTimeout, installTimeoutMs);

    IntentFilter filter =
        new IntentFilter(Intent.ACTION_PACKAGE_ADDED);

    filter.addDataScheme("package");

    context.registerReceiver(orbotInstallReceiver, filter);
    host.startActivity(OrbotHelper.getOrbotInstallIntent(context));
  }

  private final BroadcastReceiver orbotStatusReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (TextUtils.equals(intent.getAction(), OrbotHelper.ACTION_STATUS)) {
        String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS);

        if (status == null) return;

        if (status.equals(OrbotHelper.STATUS_ON)) {
          lastStatusIntent = intent;
          handler.removeCallbacks(onStatusTimeout);

          for (StatusCallback cb : statusCallbacks) {
            cb.onEnabled(intent);
          }
        } else if (status.equals(OrbotHelper.STATUS_OFF)) {
          for (StatusCallback cb : statusCallbacks) {
            cb.onDisabled();
          }
        } else if (status.equals(OrbotHelper.STATUS_STARTING)) {
          for (StatusCallback cb : statusCallbacks) {
            cb.onStarting();
          }
        } else if (status.equals(OrbotHelper.STATUS_STOPPING)) {
          for (StatusCallback cb : statusCallbacks) {
            cb.onStopping();
          }
        }
      }
    }
  };

  private final Runnable onStatusTimeout = new Runnable() {
    @Override
    public void run() {
      context.unregisterReceiver(orbotStatusReceiver);

      for (StatusCallback cb : statusCallbacks) {
        cb.onStatusTimeout();
      }
    }
  };

  private final BroadcastReceiver orbotInstallReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (TextUtils.equals(intent.getAction(),
          Intent.ACTION_PACKAGE_ADDED)) {
        String pkgName = intent.getData() != null
            ? intent.getData().getEncodedSchemeSpecificPart() : null;

        if (OrbotHelper.ORBOT_PACKAGE_NAME.equals(pkgName)) {
          isInstalled = true;
          handler.removeCallbacks(onInstallTimeout);
          context.unregisterReceiver(orbotInstallReceiver);

          for (OrbotHelper.InstallCallback cb : installCallbacks) {
            cb.onInstalled();
          }

          init();
        }
      }
    }
  };

  private final Runnable onInstallTimeout = new Runnable() {
    @Override
    public void run() {
      context.unregisterReceiver(orbotInstallReceiver);

      for (OrbotHelper.InstallCallback cb : installCallbacks) {
        cb.onInstallTimeout();
      }
    }
  };

  public static Intent validateBroadcastIntent(Context context,
      Intent toValidate,
      List<String> sigHashes,
      boolean failIfHack) {
    PackageManager pm = context.getPackageManager();
    Intent result = null;
    List<ResolveInfo> receivers =
        pm.queryBroadcastReceivers(toValidate, 0);

    for (ResolveInfo info : receivers) {
      try {
        if (sigHashes.contains(getSignatureHash(context,
            info.activityInfo.packageName))) {
          ComponentName cn =
              new ComponentName(info.activityInfo.packageName,
                  info.activityInfo.name);

          result = new Intent(toValidate).setComponent(cn);
          break;
        } else if (failIfHack) {
          throw new SecurityException(
              "Package has signature hash mismatch: " +
                  info.activityInfo.packageName);
        }
      } catch (NoSuchAlgorithmException | NameNotFoundException e) {
        Log.w("SignatureUtils",
            "Exception when computing signature hash", e);
      }
    }

    return (result);
  }

  public static String getSignatureHash(Context context, String packageName)
      throws
      NameNotFoundException,
      NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    Signature sig =
        context.getPackageManager()
            .getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures[0];

    return (toHexStringWithColons(md.digest(sig.toByteArray())));
  }

  public static String toHexStringWithColons(byte[] bytes) {
    char[] hexArray =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F'};
    char[] hexChars = new char[(bytes.length * 3) - 1];
    int v;

    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 3] = hexArray[v / 16];
      hexChars[j * 3 + 1] = hexArray[v % 16];

      if (j < bytes.length - 1) {
        hexChars[j * 3 + 2] = ':';
      }
    }

    return new String(hexChars);
  }

  static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
    if (map.isEmpty()) {
      return new SetFromMap<>(map);
    }
    throw new IllegalArgumentException("map not empty");
  }

  static class SetFromMap<E> extends AbstractSet<E>
      implements Serializable {
    private static final long serialVersionUID = 2454657854757543876L;
    // Must be named as is, to pass serialization compatibility test.
    private final Map<E, Boolean> m;
    private transient Set<E> backingSet;

    SetFromMap(final Map<E, Boolean> map) {
      m = map;
      backingSet = map.keySet();
    }

    @Override
    public boolean equals(Object object) {
      return backingSet.equals(object);
    }

    @Override
    public int hashCode() {
      return backingSet.hashCode();
    }

    @Override
    public boolean add(E object) {
      return m.put(object, Boolean.TRUE) == null;
    }

    @Override
    public void clear() {
      m.clear();
    }

    @NonNull
    @Override
    public String toString() {
      return backingSet.toString();
    }

    @Override
    public boolean contains(Object object) {
      return backingSet.contains(object);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
      return backingSet.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
      return m.isEmpty();
    }

    @Override
    public boolean remove(Object object) {
      return m.remove(object) != null;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
      return backingSet.retainAll(collection);
    }

    @NonNull
    @Override
    public Object[] toArray() {
      return backingSet.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] contents) {
      return backingSet.toArray(contents);
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
      return backingSet.iterator();
    }

    @Override
    public int size() {
      return m.size();
    }

    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      backingSet = m.keySet();
    }
  }

  public interface StatusCallback {
    /**
     * Called when Orbot is operational
     *
     * @param statusIntent an Intent containing information about
     *                     Orbot, including proxy ports
     */
    void onEnabled(Intent statusIntent);

    /**
     * Called when Orbot reports that it is starting up
     */
    void onStarting();

    /**
     * Called when Orbot reports that it is shutting down
     */
    void onStopping();

    /**
     * Called when Orbot reports that it is no longer running
     */
    void onDisabled();

    /**
     * Called if our attempt to get a status from Orbot failed
     * after a defined period of time. See statusTimeout() on
     * OrbotInitializer.
     */
    void onStatusTimeout();

    /**
     * Called if Orbot is not yet installed. Usually, you handle
     * this by checking the return value from init() on OrbotInitializer
     * or calling isInstalled() on OrbotInitializer. However, if
     * you have need for it, if a callback is registered before
     * an init() call determines that Orbot is not installed, your
     * callback will be called with onNotYetInstalled().
     */
    void onNotYetInstalled();
  }
}
