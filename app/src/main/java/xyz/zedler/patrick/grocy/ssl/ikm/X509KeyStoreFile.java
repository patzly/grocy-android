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

import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public class X509KeyStoreFile {

  private final static String TAG = "X509KeyStoreFile";

  private final static String KEYSTORE_VERSION = "KeyStoreVersion";

  private final File file;
  private final String password;
  private KeyStore keyStore;
  private long loadedVersion;
  private final SharedPreferences sharedPrefs;

  /**
   * Constructor
   *
   * @param file        keystore file (BKS format)
   * @param password    keystore password
   * @param sharedPrefs preferences to be used for synchronizing file modifications; null to disable
   *                    sync
   */
  public X509KeyStoreFile(
      @NonNull File file, @NonNull String password, SharedPreferences sharedPrefs
  ) {
    this.file = file;
    this.password = password;
    this.sharedPrefs = sharedPrefs;
  }

  /**
   * Load keystore from file using password
   *
   * @param createNew create file with empty keystore if it does not exist; fail otherwise
   */
  public void load(boolean createNew) throws IOException, KeyStoreException {
    loadedVersion = -1;
    Log.d(TAG, "load(file=" + file + ", createNew=" + createNew + ")");
    // Initialize empty keystore
    keyStore = KeyStore.getInstance("BKS");
    try {
      keyStore.load(null, null);
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      throw new KeyStoreException("Could not create empty keystore");
    }
    // Save new keystore if file does not exist
    if (!file.exists() && createNew) {
      save();
    }
    // Load keystore from file
    InputStream is = null;
    try {
      is = new java.io.FileInputStream(file);
      keyStore.load(is, password.toCharArray());
      // Get loaded version from shared preferences
      if (sharedPrefs != null) {
        loadedVersion = sharedPrefs.getLong(sharedPrefVersionKey(), -1);
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      throw new KeyStoreException("Could not load keystore from file", e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          Log.w(TAG, "load(file=" + file + ") - exception closing input stream", e);
        }
      }
    }
  }

  /**
   * Save keystore to file
   */
  private void save() throws IOException {
    Log.d(TAG, "save(file=" + file + ")");
    // Check if file is up to date via shared preferences
    if (sharedPrefs != null
        && loadedVersion != sharedPrefs.getLong(sharedPrefVersionKey(), -1)) {
      Log.e(TAG, "save: keystore not up to date");
      return;
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      keyStore.store(fos, password.toCharArray());
      // Update version in shared preferences
      if (sharedPrefs != null) {
        loadedVersion += 1;
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(sharedPrefVersionKey(), loadedVersion);
        editor.apply();
      }
    } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
      Log.e(TAG, "save: could not save keystore", e);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          Log.w(TAG, "save(file=" + file + ") - exception closing output stream", e);
        }
      }
    }
  }

  /**
   * Calculate shared preferences key for the version of this keystore file
   *
   * @return version of this keystore file
   */
  private String sharedPrefVersionKey() {
    return KEYSTORE_VERSION + "." + file.getAbsolutePath();
  }

  /**
   *
   */
  private void reloadIfNeeded() throws IOException, KeyStoreException {
    if (sharedPrefs != null &&
        loadedVersion != sharedPrefs.getLong(sharedPrefVersionKey(), -1)) {
      load(false);
    }
  }

  /**
   * Get keystore aliases
   *
   * @return aliases
   */
  public @NonNull Collection<String> getAliases() throws KeyStoreException, IOException {
    reloadIfNeeded();
    return Collections.list(keyStore.aliases());
  }

  /**
   * Check if alias represents a key entry
   */
  public boolean isKeyEntry(@NonNull String alias) throws KeyStoreException, IOException {
    reloadIfNeeded();
    return keyStore.isKeyEntry(alias);
  }

  /**
   * Get certificate from keystore
   *
   * @param alias alias of the entry
   * @return certificate f the entry
   */
  public @NonNull X509Certificate getCertificate(@NonNull String alias)
      throws KeyStoreException, IOException {
    reloadIfNeeded();
    Certificate certificate = keyStore.getCertificate(alias);
    if (!(certificate instanceof X509Certificate)) {
      throw new KeyStoreException(
          "certificate of unexpected class " + certificate.getClass().getName());
    }
    return (X509Certificate) certificate;
  }

  /**
   * Get certificate chain from keystore
   *
   * @param alias alias of the entry
   * @return certificate chain of the entry
   */
  public @NonNull Collection<X509Certificate> getCertificateChain(@NonNull String alias)
      throws KeyStoreException, IOException {
    reloadIfNeeded();
    Certificate[] certificates = keyStore.getCertificateChain(alias);
    Collection<X509Certificate> x509Certificates = new LinkedList<>();
    if (certificates != null) {
      for (Certificate certificate : certificates) {
        if (!(certificate instanceof X509Certificate)) {
          throw new KeyStoreException(
              "certificate of unexpected class " + certificate.getClass().getName());
        }
        x509Certificates.add((X509Certificate) certificate);
      }
    }
    return x509Certificates;
  }

  /**
   * Get private key from keystore
   *
   * @param alias    alias of the entry
   * @param password password of private key
   * @return private key of the entry
   */
  public @NonNull PrivateKey getPrivateKey(@NonNull String alias, String password)
      throws KeyStoreException, IOException {
    reloadIfNeeded();
    try {
      Key key = keyStore.getKey(alias, password.toCharArray());
      if (!(key instanceof PrivateKey)) {
        throw new KeyStoreException("key is null or no PrivateKey");
      }
      return (PrivateKey) key;
    } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new KeyStoreException("Could not retrieve key from keystore", e);
    }

  }

  /**
   * Get subjects of certificate chain
   *
   * @param chain certificate chain
   * @return subjects separated by "/"
   */
  private @NonNull String getCertificateSubjects(Collection<X509Certificate> chain) {
    StringBuilder subjects = new StringBuilder();
    if (chain != null) {
      for (X509Certificate certificate : chain) {
        if (subjects.length() > 0) {
          subjects.append("/");
        }
        subjects.append(certificate.getSubjectDN().getName());
      }
    }
    return subjects.toString();
  }

  /**
   * Reload keystore from file, store private key in keystore, and save keystore back to file
   *
   * @param alias alias of the entry
   * @param key   private key of the entry
   * @param chain certificate chain of the entry
   */
  public void storeKey(@NonNull String alias, @NonNull PrivateKey key,
      Collection<X509Certificate> chain) throws IOException, KeyStoreException {
    Log.d(TAG, "storeKey(alias=" + alias + ", chain=" + getCertificateSubjects(chain) + ")");
    // Reload keystore from file
    load(false);
    // Store private key in keystore
    KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(
        password.toCharArray());
    KeyStore.PrivateKeyEntry entry;
    entry = new KeyStore.PrivateKeyEntry(key, chain.toArray(new X509Certificate[0]));
    keyStore.setEntry(alias, entry, protection);
    // Save keystore to file
    save();
  }
}