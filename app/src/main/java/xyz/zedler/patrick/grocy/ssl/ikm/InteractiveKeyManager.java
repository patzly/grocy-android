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

package xyz.zedler.patrick.grocy.ssl.ikm;

import static xyz.zedler.patrick.grocy.ssl.ikm.Alias.Type.KEYCHAIN;
import static xyz.zedler.patrick.grocy.ssl.ikm.Alias.Type.KEYSTORE;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.webkit.ClientCertRequest;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.X509KeyManager;
import xyz.zedler.patrick.grocy.R;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public class InteractiveKeyManager
    implements X509KeyManager, Application.ActivityLifecycleCallbacks {

  private final static String TAG = "InteractiveKeyManager";

  private final static String DECISION_INTENT = "de.ritscher.ssl.DECISION";
  final static String DECISION_INTENT_ID = DECISION_INTENT + ".decisionId";
  final static String DECISION_INTENT_HOSTNAME_PORT = DECISION_INTENT + ".hostnamePort";
  private final static int NOTIFICATION_ID = 101319;

  private final static String KEYCHAIN_ALIASES = "KeyChainAliases";
  private final static String KEYSTORE_PASSWORD = "l^=alsk22:,.-32ß091HJK";

  private SharedPreferences sharedPreferences;
  private X509KeyStoreFile appKeyStore;

  final private Context context;
  private Handler masterHandler;
  private Handler toastHandler;
  private Activity foregroundAct;
  private NotificationManager notificationManager;

  private static int decisionId = 0;
  final private static SparseArray<Decision> openDecisions = new SparseArray<>();

  /**
   * Initialize InteractiveKeyManager
   *
   * @param context application context (instance of Activity, Application, or Service)
   */
  public InteractiveKeyManager(@NonNull Context context) {
    this.context = context;
    init();
  }

  /**
   * Perform initialization of global variables (except context) and load settings
   */
  private void init() {
    // Define handlers for async i/o
    masterHandler = new Handler(context.getMainLooper());
    notificationManager = (NotificationManager) context.getSystemService(
        Context.NOTIFICATION_SERVICE
    );
    toastHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(@NonNull Message message) {
        Toast.makeText(context, (String) message.obj, Toast.LENGTH_SHORT).show();
      }
    };
    // Determine application from context
    Application app;
    if (context instanceof Application) {
      app = (Application) context;
    } else if (context instanceof Service) {
      app = ((Service) context).getApplication();
    } else if (context instanceof Activity) {
      app = ((Activity) context).getApplication();
    } else {
      throw new ClassCastException(
          "InteractiveKeyManager context must be either Activity, Application, or Service!"
      );
    }
    // Initialize settings
    Log.d(TAG, "init(): Loading SharedPreferences named "
        + context.getPackageName() + ".InteractiveKeyManager"
    );
    sharedPreferences = context.getSharedPreferences(
        context.getPackageName() + "." + "InteractiveKeyManager", Context.MODE_PRIVATE
    );
    Log.d(TAG, "init(): keychain aliases = " + Arrays.toString(
        sharedPreferences.getStringSet(KEYCHAIN_ALIASES, new HashSet<>()).toArray())
    );

    File dir = app.getDir("InteractiveKeyManager", Context.MODE_PRIVATE);
    appKeyStore = new X509KeyStoreFile(
        new File(dir + File.separator + "appKeyStore.bks"),
        KEYSTORE_PASSWORD,
        sharedPreferences
    );
    try {
      appKeyStore.load(true);
      Log.d(
          TAG,
          "init(): keystore aliases = " + Arrays.toString(appKeyStore.getAliases().toArray())
      );
    } catch (KeyStoreException | IOException e) {
      Log.e(TAG, "Error loading keystore", e);
      toastHandler.obtainMessage(
          0, context.getString(R.string.ikm_load_app_keystore)
      ).sendToTarget();
    }
    // Register callbacks to determine current activity
    app.registerActivityLifecycleCallbacks(this);
  }

  /**
   * Add all private keys from keystore file to app keystore for use for connections to
   * hostname:port
   *
   * @param fileName     keystore file name
   * @param keyPasswords key passwords (all will be tried for each key in the keystore)
   * @param hostname     hostname for which the alias shall be used; null for any
   * @param port         port for which the alias shall be used (only if hostname is not null); null
   *                     for any
   * @return pair of successfully added aliases to be used in KEYCHAIN_ALIASES and original aliases
   * not successfully added
   */
  private @NonNull Pair<Collection<String>, Collection<String>> addKeyStore(
      @NonNull String fileName, @NonNull String[] keyPasswords, String hostname, Integer port)
      throws IOException, KeyStoreException {
    Collection<String> aliases = new LinkedList<>();
    Collection<String> aliasesError = new LinkedList<>();
    Log.d(TAG,
        "addKeyStore(fileName=" + fileName + ", hostname=" + hostname + ", port=" + port + ")");
    // Load keystore file using given keystore password
    X509KeyStoreFile ks = new X509KeyStoreFile(new File(fileName), "password", null);
    ks.load(false);
    // Try to read all keys from keystore
    for (String alias : ks.getAliases()) {
      Log.d(TAG, "addKeyStore(" + fileName + ", hostname=" + hostname + ", port=" + port +
          "): found alias " + alias);
      if (!ks.isKeyEntry(alias)) {
        Log.w(TAG, "addKeyStore(" + fileName + ", hostname=" + hostname + ", port=" + port +
            "): alias " + alias + " not a key entry");
        continue;
      }
      // Try unlocking key using all passwords
      boolean loadedKey = false;
      for (String password : keyPasswords) {
        try {
          // Decipher key using password
          Collection<X509Certificate> chain = ks.getCertificateChain(alias);
          PrivateKey key = ks.getPrivateKey(alias, password);
          // Store key to appKeyStore with new alias
          String constructedAlias = new Alias(KEYSTORE, Integer.toHexString(key.hashCode()),
              hostname, port).toString();
          appKeyStore.storeKey(constructedAlias, key, chain);
          aliases.add(constructedAlias);
          loadedKey = true;
          break;
        } catch (IOException | KeyStoreException e) {
          Log.w(TAG, "addKeyStore(" + fileName + ", hostname=" + hostname + ", port=" + port +
              "): Could not load key '" + alias + "'", e);
        }
      }
      if (!loadedKey) {
        aliasesError.add(alias);
      }
    }
    return Pair.create(aliases, aliasesError);
  }

  /**
   * Add KeyChain alias for use for connections to hostname:port
   *
   * @param keyChainAlias alias returned from KeyChain.choosePrivateKeyAlias
   * @param hostname      hostname for which the alias shall be used; null for any
   * @param port          port for which the alias shall be used (only if hostname is not null);
   *                      null for any
   * @return alias to be used in KEYCHAIN_ALIASES
   */
  public @NonNull String addKeyChain(@NonNull String keyChainAlias, String hostname,
      Integer port) {
    String alias = new Alias(KEYCHAIN, keyChainAlias, hostname, port).toString();
    Set<String> aliases = new HashSet<>(
        sharedPreferences.getStringSet(KEYCHAIN_ALIASES, new HashSet<>()));
    aliases.add(alias);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putStringSet(KEYCHAIN_ALIASES, aliases);
    if (editor.commit()) {
      Log.d(TAG,
          "addKeyChain(keyChainAlias=" + keyChainAlias + ", hostname=" + hostname + ", port=" +
              port + "): keychain aliases = " + Arrays.toString(aliases.toArray()));
    } else {
      Log.e(TAG,
          "addKeyChain(keyChainAlias=" + keyChainAlias + ", hostname=" + hostname + ", port=" +
              port + "): Could not save preferences");
    }
    return alias;
  }

  /**
   * Remove KeyChain aliases from KEYCHAIN_ALIASES based on filter and depending on causing
   * exception
   *
   * @param filter IKMAlias object used as filter
   * @param e      exception on retrieving certificate/key
   */
  private void removeKeyChain(Alias filter, KeyChainException e) throws IllegalArgumentException {
    if (Objects.requireNonNull(e.getMessage()).contains("keystore is LOCKED")) {
            /* This exception occurs after the start before the password is entered on an
            encrypted device. Don't remove alias in this case. */
      return;
    }
    removeKeyChain(filter);
  }

  /**
   * Remove KeyChain aliases from KEYCHAIN_ALIASES based on filter
   *
   * @param filter IKMAlias object used as filter
   */
  private void removeKeyChain(Alias filter) throws IllegalArgumentException {
    Set<String> aliases = new HashSet<>();
    for (String alias : sharedPreferences.getStringSet(KEYCHAIN_ALIASES, new HashSet<>())) {
      Alias ikmAlias = new Alias(alias);
      if (!ikmAlias.matches(filter)) {
        aliases.add(alias);
      }
    }
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putStringSet(KEYCHAIN_ALIASES, aliases);
    if (editor.commit()) {
      Log.d(TAG, "removeKeyChain(filter=" + filter + "): keychain aliases = " +
          Arrays.toString(aliases.toArray()));
    } else {
      Log.e(TAG, "removeKeyChain(filter=" + filter + "): Could not save preferences");
    }
  }

  /**
   * Get all KeyChain aliases matching the filter
   *
   * @param aliases collection of objects whose string representation is as returned from
   *                IKMAlias.toString()
   * @param filter  IKMAlias object used as filter
   * @return all aliases from KEYCHAIN_ALIASES which satisfy alias.matches(filter)
   */
  private static <T> Collection<String> filterAliases(Collection<T> aliases, Alias filter) {
    Collection<String> filtered = new LinkedList<>();
    for (Object alias : aliases) {
      if (new Alias(alias.toString()).matches(filter)) {
        filtered.add(((String) alias));
      }
    }
    return filtered;
  }

  /**
   * Get keystore and keychain aliases for use for connections to hostname:port
   *
   * @param keyTypes accepted keyTypes; null for any
   * @param issuers  issuers; null for any
   * @param hostname hostname of connection; null for any
   * @param port     port of connection; null for any
   * @return array of aliases
   */
  private @NonNull String[] getAliases(Set<KeyType> keyTypes, Principal[] issuers, String hostname,
      Integer port) {
    Set<Principal> issuersSet = new HashSet<>();
    if (issuers != null) {
      issuersSet.addAll(Arrays.asList(issuers));
    }
    List<String> validAliases = new LinkedList<>();
    try {
      // Check keystore aliases for use for connections to hostname:port
      Alias filter = new Alias(Alias.Type.KEYSTORE, null, hostname, port);
      for (String alias : filterAliases(appKeyStore.getAliases(), filter)) {
        // Check if certificate is valid
        X509Certificate certificate = appKeyStore.getCertificate(alias);
        // Check keyTypes
        if (keyTypes != null) {
          if (!keyTypes.contains(KeyType.parse(certificate.getPublicKey().getAlgorithm()))) {
            Log.d(TAG, "getAliases: " + alias + " has keyType " +
                certificate.getPublicKey().getAlgorithm() + ", not " + Arrays.toString(
                keyTypes.toArray()));
            continue;
          }
        }
        // Check issuers
        if (issuers != null) {
          if (!issuersSet.contains(certificate.getIssuerX500Principal())) {
            Log.d(TAG, "getAliases: " + alias + " has issuer " +
                certificate.getIssuerX500Principal() + ", not " + Arrays.toString(issuers));
            continue;
          }
        }
        // All checks passed
        validAliases.add(alias);
      }
    } catch (KeyStoreException | IOException e) {
      Log.e(TAG,
          "getAliases(keyTypes=" + (keyTypes != null ? Arrays.toString(keyTypes.toArray()) : null)
              + ", issuers=" + Arrays.toString(issuers)
              + ", hostname=" + hostname
              + ", port=" + port
              + ")", e);
      toastHandler.obtainMessage(0, context.getString(R.string.ikm_load_app_keystore))
          .sendToTarget();
    }
    // Check keychain aliases
    Alias filter = new Alias(Alias.Type.KEYCHAIN, null, hostname, port);
    validAliases.addAll(
        filterAliases(sharedPreferences.getStringSet(KEYCHAIN_ALIASES, new HashSet<>()), filter));

    Log.d(TAG,
        "getAliases(keyTypes=" + (keyTypes != null ? Arrays.toString(keyTypes.toArray()) : null)
            + ", issuers=" + Arrays.toString(issuers)
            + ", hostname=" + hostname
            + ", port=" + port
            + ") = " + Arrays.toString(validAliases.toArray()));
    return validAliases.toArray(new String[0]);
  }

  /**
   * Choose an alias for a connection, prompting for interaction if no stored alias is found
   *
   * @param keyTypes accepted keyTypes; null for any
   * @param issuers  accepted issuers; null for any
   * @param socket   connection socket
   * @return keychain or keystore alias to use for this connection
   */
  private String chooseAlias(String[] keyTypes, Principal[] issuers, @NonNull Socket socket) {
    // Determine connection parameters
    String hostname = socket.getInetAddress().getHostName();
    int port = socket.getPort();
    return chooseAlias(keyTypes, issuers, hostname, port);
  }

  /**
   * Choose an alias for a connection, prompting for interaction if no stored alias is found
   *
   * @param keyTypes accepted keyTypes; null for any
   * @param issuers  accepted issuers; null for any
   * @param hostname hostname of connection
   * @param port     port of connection
   * @return keychain or keystore alias to use for this connection
   */
  private String chooseAlias(String[] keyTypes, Principal[] issuers, @NonNull String hostname,
      int port) {
    // Select certificate for one connection at a time. This is important if multiple connections to the same host
    // are started in a short time and avoids prompting the user with multiple dialogs for the same host.
    synchronized (InteractiveKeyManager.class) {
      // Get stored aliases for connection
      String[] validAliases = getAliases(KeyType.parse(Arrays.asList(keyTypes)), issuers, hostname,
          port);
      if (validAliases.length > 0) {
        Log.d(TAG,
            "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" + Arrays.toString(
                issuers)
                + ", hostname=" + hostname + ", port=" + port + ") = " + validAliases[0]);
        // Return first alias found
        return validAliases[0];
      } else {
        Log.d(TAG,
            "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" + Arrays.toString(
                issuers)
                + ", hostname=" + hostname + ", port=" + port
                + "): no matching alias found, prompting user...");
        Decision decision = interactClientCert(hostname, port);
        String alias;
        switch (decision.state) {
          case Decision.DECISION_FILE: // Add key from keystore file for connection
            Pair<Collection<String>, Collection<String>> aliases;
            try {
              aliases = addKeyStore(decision.param, new
                  String[]{"password"}, decision.hostname, decision.port);
              if (aliases.first == null || aliases.first.isEmpty()) {
                throw new KeyStoreException("Could not load any key.");
              }
            } catch (KeyStoreException | IOException e) {
              Log.e(TAG, "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" +
                  Arrays.toString(issuers) + ", hostname=" + hostname + ", port=" + port + ")", e);
              toastHandler.obtainMessage(0, context.getString(R.string.ikm_add_from_keystore))
                  .sendToTarget();
              return null;
            }
            if (aliases.second != null && !aliases.second.isEmpty()) {
              Log.e(TAG, "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" +
                  Arrays.toString(issuers) + ", hostname=" + hostname + ", port=" + port +
                  "): Could not load keys " + Arrays.toString(aliases.second.toArray())
                  + " from file " +
                  decision.param);
              toastHandler.obtainMessage(0,
                  context.getString(R.string.ikm_add_from_keystore_alias) + " " +
                      Arrays.toString(aliases.second.toArray())).sendToTarget();
            }
            alias = aliases.first.iterator().next();
            Log.d(TAG, "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" +
                Arrays.toString(issuers) + ", hostname=" + hostname + ", port=" + port
                + "): Use alias " +
                alias);
            return alias;
          case Decision.DECISION_KEYCHAIN: // Add keychain alias for connection
            alias = addKeyChain(decision.param, decision.hostname, decision.port);
            Log.d(TAG, "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" +
                Arrays.toString(issuers) + ", hostname=" + hostname + ", port=" + port
                + "): Use alias " +
                alias);
            return alias;
          case Decision.DECISION_ABORT:
            Log.w(TAG, "chooseAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" +
                Arrays.toString(issuers) + ", hostname=" + hostname + ", port=" + port
                + ") - no alias selected");
            return null;
          default:
            throw new IllegalArgumentException("Unknown decision state " + decision.state);
        }
      }
    }
  }

  @Override
  public String chooseClientAlias(String[] keyTypes, Principal[] issuers, @NonNull Socket socket) {
    Log.d(TAG,
        "chooseClientAlias(keyTypes=" + Arrays.toString(keyTypes) + ", issuers=" + Arrays.toString(
            issuers) + ")");
    try {
      return chooseAlias(keyTypes, issuers, socket);
    } catch (Throwable t) {
      Log.e(TAG, "chooseClientAlias", t);
      return null;
    }
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, @NonNull Socket socket) {
    Log.d(TAG,
        "chooseServerAlias(keyType=" + keyType + ", issuers=" + Arrays.toString(issuers) + ")");
    return chooseAlias(new String[]{keyType}, issuers, socket);
  }

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers) {
    Log.d(TAG,
        "getClientAliases(keyType=" + keyType + ", issuers=" + Arrays.toString(issuers) + ")");
    return getAliases(KeyType.parse(Collections.singletonList(keyType)), issuers, null, null);
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers) {
    Log.d(TAG,
        "getServerAliases(keyType=" + keyType + ", issuers=" + Arrays.toString(issuers) + ")");
    return getAliases(KeyType.parse(Collections.singletonList(keyType)), issuers, null, null);
  }

  @Override
  public X509Certificate[] getCertificateChain(@NonNull String alias) {
    Log.d(TAG, "getCertificateChain(alias=" + alias + ")");
    Alias ikmAlias = new Alias(alias);
    if (ikmAlias.getType() == KEYCHAIN) {
      try {
        X509Certificate[] certificateChain = KeyChain.getCertificateChain(context,
            ikmAlias.getAlias());
        if (certificateChain == null) {
          throw new KeyChainException(
              "could not retrieve certificate chain for alias " + ikmAlias.getAlias());
        }
        return certificateChain;
      } catch (KeyChainException e) {
        Log.e(TAG,
            "getCertificateChain(alias=" + alias + ") - keychain alias=" + ikmAlias.getAlias(), e);
        removeKeyChain(ikmAlias, e);
        toastHandler.obtainMessage(0, context.getString(R.string.ikm_keychain) + " " +
            ikmAlias.getAlias()).sendToTarget();
        return null;
      } catch (InterruptedException e) {
        Log.d(TAG, "getCertificateChain(alias=" + alias + ")", e);
        Thread.currentThread().interrupt();
        return null;
      }
    } else if (ikmAlias.getType() == KEYSTORE) {
      try {
        return appKeyStore.getCertificateChain(alias).toArray(new X509Certificate[0]);
      } catch (KeyStoreException | IOException e) {
        Log.e(TAG, "getCertificateChain(alias=" + alias + ")", e);
        toastHandler.obtainMessage(0, context.getString(R.string.ikm_load_app_keystore))
            .sendToTarget();
        return null;
      }
    } else {
      throw new IllegalArgumentException("Invalid alias");
    }
  }

  @Override
  public PrivateKey getPrivateKey(@NonNull String alias) {
    Log.d(TAG, "getPrivateKey(alias=" + alias + ")");
    Alias ikmAlias = new Alias(alias);
    if (ikmAlias.getType() == KEYCHAIN) {
      try {
        PrivateKey key = KeyChain.getPrivateKey(context, ikmAlias.getAlias());
        if (key == null) {
          throw new KeyChainException(
              "could not retrieve private key for alias " + ikmAlias.getAlias());
        }
        return key;
      } catch (KeyChainException e) {
        Log.e(TAG, "getPrivateKey(alias=" + alias + ")", e);
        removeKeyChain(ikmAlias, e);
        toastHandler.obtainMessage(0, context.getString(R.string.ikm_keychain) + " " +
            ikmAlias.getAlias()).sendToTarget();
        return null;
      } catch (InterruptedException e) {
        Log.d(TAG, "getPrivateKey(alias=" + alias + ")", e);
        Thread.currentThread().interrupt();
        return null;
      }
    } else if (ikmAlias.getType() == KEYSTORE) {
      try {
        return appKeyStore.getPrivateKey(alias, KEYSTORE_PASSWORD);
      } catch (KeyStoreException | IOException e) {
        Log.e(TAG, "getPrivateKey(alias=" + alias + ")", e);
        toastHandler.obtainMessage(0, "Error reading keystore").sendToTarget();
        return null;
      }
    } else {
      throw new IllegalArgumentException("Invalid alias");
    }
  }

  /**
   * Generate a unique identifier for a decision and remember it in openDecisions
   *
   * @param decision decision to remember
   * @return unique decision identifier
   */
  private static int createDecisionId(@NonNull Decision decision) {
    int id;
    synchronized (openDecisions) {
      id = decisionId;
      openDecisions.put(id, decision);
      decisionId += 1;
    }
    return id;
  }

  private Notification.Builder getDeprecatedNotificationBuilder(Context ctx) {
    return new Notification.Builder(ctx);
  }

  private Notification.Builder getNotificationBuilder(Context ctx) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return getDeprecatedNotificationBuilder(ctx);
    } else {
      return new Notification.Builder(ctx, NotificationChannel.DEFAULT_CHANNEL_ID);
    }
  }

  private void startActivityNotification(@NonNull Intent intent, int decisionId,
      @NonNull String message) {
    final PendingIntent call = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE);
    final Notification notification = getNotificationBuilder(context.getApplicationContext())
        .setContentTitle(context.getString(R.string.ikm_notification))
        .setContentText(message)
        .setTicker(message)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(call)
        .setAutoCancel(true)
        .build();

    notificationManager.notify(NOTIFICATION_ID + decisionId, notification);
  }

  /**
   * Display an SelectKeyStoreActivity intent where the user can select a client certificate for the
   * connection. If the intent cannot be started directly, a notification is started instead. This
   * function will block until the user interaction is finished.
   *
   * @param hostname hostname of connection
   * @param port     port of connection
   * @return decision object with result of user interaction
   */
  private @NonNull Decision interactClientCert(@NonNull final String hostname, final int port) {
    Log.d(TAG, "interactClientCert(hostname=" + hostname + ", port=" + port + ")");
    Decision decision = new Decision();
    final int id = createDecisionId(decision);

    masterHandler.post(() -> {
      Intent ni = new Intent(context, SelectKeyStoreActivity.class);
      ni.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ni.setData(Uri.parse(SelectKeyStoreActivity.class.getName() + "/" + id));
      ni.putExtra(DECISION_INTENT_ID, id);
      ni.putExtra(DECISION_INTENT_HOSTNAME_PORT, hostname + ":" + port);

      // we try to directly start the activity and fall back to making a notification
      try {
        getUI().startActivity(ni);
      } catch (Exception e) {
        Log.d(TAG, "interactClientCert: startActivity(SelectKeyStoreActivity)", e);
        startActivityNotification(ni, id, context.getString(R.string.ikm_client_cert_notification) +
            " " + hostname + ":" + port);
      }
    });

    Log.d(TAG, "interactClientCert: openDecisions = " + openDecisions + ", waiting on " + id);
    try {
      synchronized (decision) {
        decision.wait();
      }
    } catch (InterruptedException e) {
      Log.d(TAG, "interactClientCert: InterruptedException", e);
      Thread.currentThread().interrupt();
    }
    Log.d(TAG, "interactClientCert: finished wait on " + id + ": state=" + decision.state +
        ", param=" + decision.state);
    return decision;
  }

  /**
   * Callback for SelectKeyStoreActivity to set the decision result.
   *
   * @param decisionId decision identifier
   * @param state      type of the result as defined in IKMDecision
   * @param param      keychain alias respectively keystore filename
   * @param hostname   hostname of connection
   * @param port       port of connection
   */
  static void interactResult(int decisionId, int state, String param, String hostname,
      Integer port) {
    Decision decision;
    Log.d(TAG, "interactResult(decisionId=" + decisionId + ", state=" + state + ", param=" + param +
        ", hostname=" + hostname + ", port=" + port);
    // Get decision object
    synchronized (openDecisions) {
      decision = openDecisions.get(decisionId);
      openDecisions.remove(decisionId);
    }
    if (decision == null) {
      Log.e(TAG, "interactResult: aborting due to stale decision reference!");
      return;
    }
    // Fill in result
    synchronized (decision) {
      decision.state = state;
      decision.param = param;
      decision.hostname = hostname;
      decision.port = port;
      decision.notify();
    }
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    foregroundAct = activity;
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    foregroundAct = null;
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {
  }

  /**
   * Returns the top-most entry of the activity stack.
   *
   * @return the context of the currently bound UI or the master context if none is bound
   */
  private Context getUI() {
    return (foregroundAct != null) ? foregroundAct : context;
  }
}