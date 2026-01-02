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

package xyz.zedler.patrick.grocy.ssl.mtm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import xyz.zedler.patrick.grocy.R;

/**
 * A X509 trust manager implementation which asks the user about invalid certificates and memorizes
 * their decision.
 * <p>
 * The certificate validity is checked using the system default X509 TrustManager, creating a query
 * Dialog if the check fails.
 * <p>
 * <b>WARNING:</b> This only works if a dedicated thread is used for opening sockets!
 * Source: github.com/stephanritscher/MemorizingTrustManager
 */
@SuppressLint("CustomX509TrustManager")
public class MemorizingTrustManager implements X509TrustManager {

  private final static String TAG = MemorizingTrustManager.class.getName();

  private final static String DECISION_INTENT = "de.duenndns.ssl.DECISION";
  public final static String DECISION_INTENT_ID = DECISION_INTENT + ".decisionId";
  public final static String DECISION_INTENT_CERT = DECISION_INTENT + ".cert";

  public final static String DECISION_TITLE_ID = DECISION_INTENT + ".titleId";
  private final static int NOTIFICATION_ID = 100509;

  private final static String KEYSTORE_DIR = "KeyStore";
  private final static String KEYSTORE_FILE = "KeyStore.bks";

  private Context context;
  private NotificationManager notificationManager;
  private static final String CHANNEL_ID = "memorizingtrustmanager";
  private static int decisionId = 0;
  private static final SparseArray<Decision> openDecisions = new SparseArray<>();

  private Handler masterHandler;
  private File keyStoreFile;
  private KeyStore appKeyStore;
  private final X509TrustManager defaultTrustManager;
  private X509TrustManager appTrustManager;

  /**
   * Creates an instance of the MemorizingTrustManager class that falls back to a custom
   * TrustManager. You need to supply the application context. This has to be one of: - Application
   * - Activity - Service The context is used for file management, to display the dialog /
   * notification and for obtaining translated strings.
   *
   * @param m                   Context for the application.
   * @param defaultTrustManager Delegate trust management to this TM. If null, the user must accept
   *                            every certificate.
   */
  public MemorizingTrustManager(Context m, X509TrustManager defaultTrustManager) {
    init(m);
    this.appTrustManager = getTrustManager(appKeyStore);
    this.defaultTrustManager = defaultTrustManager;
  }

  /**
   * Creates an instance of the MemorizingTrustManager class using the system X509TrustManager.
   * <p>
   * You need to supply the application context. This has to be one of: - Application - Activity -
   * Service
   * <p>
   * The context is used for file management, to display the dialog / notification and for obtaining
   * translated strings.
   *
   * @param m Context for the application.
   */
  public MemorizingTrustManager(Context m) {
    init(m);
    this.appTrustManager = getTrustManager(appKeyStore);
    this.defaultTrustManager = getTrustManager(null);
  }

  private void init(Context context) {
    this.context = context;
    masterHandler = new Handler(context.getMainLooper());
    notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
      if (channel == null) {
        channel = new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.mtm_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.mtm_notification));
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(channel);
      }
    }
    Application app;
    if (context instanceof Application) {
      app = (Application) context;
    } else if (context instanceof Service) {
      app = ((Service) context).getApplication();
    } else if (context instanceof Activity) {
      app = ((Activity) context).getApplication();
    } else {
      throw new ClassCastException(
          "MemorizingTrustManager context must be either Activity or Service!");
    }

    File dir = app.getDir(KEYSTORE_DIR, Context.MODE_PRIVATE);
    keyStoreFile = new File(dir + File.separator + KEYSTORE_FILE);
    Log.i(TAG, "init: Using keyStoreFile " + keyStoreFile.getPath());
    appKeyStore = loadAppKeyStore();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    checkCertTrusted(chain, authType, false);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    checkCertTrusted(chain, authType, true);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    Log.i(TAG, "getAcceptedIssuers");
    return defaultTrustManager.getAcceptedIssuers();
  }

  private X509TrustManager getTrustManager(KeyStore ks) {
    try {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
      tmf.init(ks);
      for (TrustManager t : tmf.getTrustManagers()) {
        if (t instanceof X509TrustManager) {
          return (X509TrustManager) t;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "getTrustManager: " + ks, e);
    }
    return null;
  }

  private KeyStore loadAppKeyStore() {
    KeyStore ks;
    try {
      ks = KeyStore.getInstance(KeyStore.getDefaultType());
    } catch (KeyStoreException e) {
      Log.e(TAG, "loadAppKeyStore: ", e);
      return null;
    }
    try {
      ks.load(null, null);
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      Log.e(TAG, "loadAppKeyStore: " + keyStoreFile, e);
    }
    if (keyStoreFile.exists()) {
      InputStream is = null;
      try {
        is = new java.io.FileInputStream(keyStoreFile);
        ks.load(is, "MTM".toCharArray());
      } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
        Toast.makeText(context, R.string.mtm_error_keystore, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "loadAppKeyStore: exception loading file key store: " + keyStoreFile, e);
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            Log.e(
                TAG,
                "loadAppKeyStore: error closing file key store input stream " + keyStoreFile,
                e
            );
            Toast.makeText(context, R.string.mtm_error_keystore, Toast.LENGTH_SHORT).show();
          }
        }
      }
    }
    return ks;
  }

  private void storeCert(String alias, Certificate cert) {
    try {
      appKeyStore.setCertificateEntry(alias, cert);
      Log.i(TAG, "storeCert: " + alias);
    } catch (KeyStoreException e) {
      Log.e(TAG, "storeCert: " + cert, e);
      return;
    }
    keyStoreUpdated();
  }

  private void storeCert(X509Certificate cert) {
    storeCert(cert.getSubjectDN().toString(), cert);
  }

  private void keyStoreUpdated() {
    // reload appTrustManager
    appTrustManager = getTrustManager(appKeyStore);

    // store KeyStore to file
    java.io.FileOutputStream fos = null;
    try {
      fos = new java.io.FileOutputStream(keyStoreFile);
      appKeyStore.store(fos, "MTM".toCharArray());
    } catch (Exception e) {
      Log.e(TAG, "keyStoreUpdated: " + keyStoreFile, e);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          Log.e(TAG, "keyStoreUpdated: " + keyStoreFile, e);
        }
      }
    }
  }

  // if the certificate is stored in the app key store, it is considered "known"
  private boolean isCertKnown(X509Certificate cert) {
    try {
      return appKeyStore.getCertificateAlias(cert) != null;
    } catch (KeyStoreException e) {
      return false;
    }
  }

  private static boolean isExpiredException(Throwable e) {
    do {
      if (e instanceof CertificateExpiredException) {
        return true;
      }
      e = e.getCause();
    } while (e != null);
    return false;
  }

  private static boolean isPathException(Throwable e) {
    do {
      if (e instanceof CertPathValidatorException) {
        return true;
      }
      e = e.getCause();
    } while (e != null);
    return false;
  }

  private void checkCertTrusted(X509Certificate[] chain, String authType, boolean isServer)
      throws CertificateException {
    Log.i(
        TAG, "checkCertTrusted: " + (chain == null ? "null" :
            Arrays.stream(chain).map(X509Certificate::getSubjectDN).map(Principal::getName)
                .collect(Collectors.joining(";")))
            + ", " + authType + ", " + isServer
    );
    try {
      Log.i(TAG, "checkCertTrusted: trying appTrustManager");
      if (isServer) {
        appTrustManager.checkServerTrusted(chain, authType);
      } else {
        appTrustManager.checkClientTrusted(chain, authType);
      }
    } catch (CertificateException ae) {
      Log.w(TAG,
          "checkCertTrusted: appTrustManager did not verify certificate. Will fall back to secondary verification mechanisms (if any).",
          ae);
      if (chain != null && chain.length >= 1 && isCertKnown(chain[0])) {
        Log.i(TAG, "checkCertTrusted: accepting cert already stored in keystore");
        return;
      }
      try {
        if (defaultTrustManager == null) {
          Log.i(TAG,
              "checkCertTrusted: No defaultTrustManager set. Verification failed, throwing " + ae);
          throw ae;
        }
        Log.i(TAG, "checkCertTrusted: trying defaultTrustManager");
        if (isServer) {
          defaultTrustManager.checkServerTrusted(chain, authType);
        } else {
          defaultTrustManager.checkClientTrusted(chain, authType);
        }
      } catch (CertificateException e) {
        Log.e(TAG, "checkCertTrusted: defaultTrustManager failed", e);
        interactCert(chain, e);
      }
    }
  }

  private static int createDecisionId(Decision d) {
    int myId;
    synchronized (openDecisions) {
      myId = decisionId;
      openDecisions.put(myId, d);
      decisionId += 1;
    }
    return myId;
  }

  private static String hexString(byte[] data) {
    StringBuilder si = new StringBuilder();
    for (int i = 0; i < data.length; i++) {
      si.append(String.format("%02x", data[i]));
      if (i < data.length - 1) {
        si.append(":");
      }
    }
    return si.toString();
  }

  private static String certHash(final X509Certificate cert, String digest) {
    try {
      MessageDigest md = MessageDigest.getInstance(digest);
      md.update(cert.getEncoded());
      return hexString(md.digest());
    } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
      return e.getMessage();
    }
  }

  private void certDetails(StringBuilder si, X509Certificate c) {
    SimpleDateFormat validityDateFormater = new SimpleDateFormat(
        "yyyy-MM-dd", Locale.ENGLISH
    );
    si.append("\n");
    si.append(context.getString(R.string.mtm_valid_for));
    si.append("\n");
    try {
      Collection<List<?>> sans = c.getSubjectAlternativeNames();
      if (sans == null) {
        si.append(c.getSubjectDN());
        si.append("\n");
      } else {
        for (List<?> altName : sans) {
          Object name = altName.get(1);
          if (name instanceof String) {
            si.append("[");
            si.append(altName.get(0));
            si.append("] ");
            si.append(name);
            String idn = IDN.toUnicode((String) name, IDN.ALLOW_UNASSIGNED);
            if (!name.equals(idn)) {
              si.append(" (").append(idn).append(")");
            }
            si.append("\n");
          }
        }
      }
    } catch (CertificateParsingException e) {
      Log.e(TAG, "certDetails: ", e);
      si.append("<Parsing error: ");
      si.append(e.getLocalizedMessage());
      si.append(">\n");
    }
    si.append("\n");
    si.append(context.getString(R.string.mtm_cert_details));
    si.append("\n");
    si.append(validityDateFormater.format(c.getNotBefore()));
    si.append(" - ");
    si.append(validityDateFormater.format(c.getNotAfter()));
    si.append("\nSHA-256: ");
    si.append(certHash(c, "SHA-256"));
    si.append("\nSHA-1: ");
    si.append(certHash(c, "SHA-1"));
    si.append("\nSigned by: ");
    si.append(c.getIssuerDN().toString());
    si.append("\n");
  }

  private String certChainMessage(final X509Certificate[] chain, CertificateException cause) {
    Throwable e = cause;
    Log.i(TAG, "certChainMessage: for " + e);
    StringBuilder si = new StringBuilder();
    if (isPathException(e)) {
      si.append(context.getString(R.string.mtm_trust_anchor));
    } else if (isExpiredException(e)) {
      si.append(context.getString(R.string.mtm_cert_expired));
    } else {
      // get to the cause
      while (e.getCause() != null) {
        e = e.getCause();
      }
      si.append(e.getLocalizedMessage());
    }
    si.append("\n\n");
    si.append(context.getString(R.string.mtm_trust_certificate));
    si.append("\n\n");
    if (chain != null) {
      for (X509Certificate c : chain) {
        certDetails(si, c);
      }
    }
    return si.toString();
  }

  private void startActivityNotification(Intent intent, int decisionId, String certName) {
    final PendingIntent call;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      call = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
    } else {
      call = PendingIntent.getActivity(context, 0, intent, 0);
    }
    final String mtmNotification = context.getString(R.string.mtm_notification);
    final long currentMillis = System.currentTimeMillis();

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
        context, CHANNEL_ID
    );
    Notification notification = notificationBuilder
        .setContentTitle(mtmNotification)
        .setContentText(certName)
        .setTicker(certName)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setWhen(currentMillis)
        .setContentIntent(call)
        .setAutoCancel(true)
        .build();
    notificationManager.notify(NOTIFICATION_ID + decisionId, notification);
  }

  private int interact(final String message, final int titleId) {
    /* prepare the MTMDecision blocker object */
    Decision choice = new Decision();
    final int myId = createDecisionId(choice);

    masterHandler.post(() -> {
      Intent ni = new Intent(context, MemorizingActivity.class);
      ni.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ni.setData(Uri.parse(MemorizingTrustManager.class.getName() + "/" + myId));
      ni.putExtra(DECISION_INTENT_ID, myId);
      ni.putExtra(DECISION_INTENT_CERT, message);
      ni.putExtra(DECISION_TITLE_ID, titleId);

      // we try to directly start the activity and fall back to
      // making a notification. If no foreground activity is set
      // (foregroundAct==null) or if the app developer set an
      // invalid / expired activity, the catch-all fallback is
      // deployed.
      try {
        context.startActivity(ni);
      } catch (Exception e) {
        Log.e(TAG, "interact: startActivity(MemorizingActivity)", e);
        startActivityNotification(ni, myId, message);
      }
    });

    Log.i(TAG, "interact: openDecisions: " + openDecisions + ", waiting on " + myId);
    try {
      synchronized (choice) {
        choice.wait();
      }
    } catch (InterruptedException e) {
      Log.e(TAG, "interact: ", e);
    }
    Log.i(TAG, "interact: finished wait on " + myId + ": " + choice.state);
    return choice.state;
  }

  private void interactCert(final X509Certificate[] chain, CertificateException cause)
      throws CertificateException {
    switch (interact(certChainMessage(chain, cause), R.string.mtm_security_risk)) {
      case Decision.DECISION_ALWAYS:
        storeCert(chain[0]); // only store the server cert, not the whole chain
      case Decision.DECISION_ONCE:
        break;
      default:
        throw (cause);
    }
  }

  protected static void interactResult(int decisionId, int choice) {
    Decision decision;
    synchronized (openDecisions) {
      decision = openDecisions.get(decisionId);
      openDecisions.remove(decisionId);
    }
    if (decision == null) {
      Log.e(TAG, "interactResult: aborting due to stale decision reference!");
      return;
    }
    synchronized (decision) {
      decision.state = choice;
      decision.notify();
    }
  }
}