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

import android.util.Log;
import androidx.annotation.NonNull;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager/tree/master
 */
public class Alias {

  private final static String TAG = "Alias";

  enum Type {
    KEYCHAIN("KC_"),
    KEYSTORE("KS_");

    private final String prefix;

    Type(String prefix) {
      this.prefix = prefix;
    }

    public String getPrefix() {
      return prefix;
    }

    public static Type parse(String prefix) throws IllegalArgumentException {
      for (Type type : Type.values()) {
        if (type.getPrefix().equals(prefix)) {
          return type;
        }
      }
      throw new IllegalArgumentException("unknown prefix");
    }
  }

  private final Type type;
  private final String alias;
  private final String hostname;
  private final Integer port;

  /**
   * Constructor of IKMAlias
   *
   * @param type     type of alias (KEYCHAIN or KEYSTORE)
   * @param alias    alias returned from KeyChain.choosePrivateKeyAlias respectively
   *                 PrivateKey.hashCode
   * @param hostname hostname for which the alias shall be used; null for any
   * @param port     port for which the alias shall be used (only if hostname is not null); null for
   *                 any
   */
  public Alias(Type type, String alias, String hostname, Integer port) {
    this.type = type;
    this.alias = alias;
    this.hostname = hostname;
    this.port = port;
  }

  /**
   * Constructor of IKMAlias
   *
   * @param alias value returned from IKMAlias.toString()
   */
  public Alias(String alias) throws IllegalArgumentException {
    String[] aliasFields = alias.split(":");
    if (aliasFields.length > 3 || aliasFields[0].length() < 4) {
      throw new IllegalArgumentException("alias was not returned by IKMAlias.toString(): " + alias);
    }
    this.type = Type.parse(aliasFields[0].substring(0, 3));
    this.alias = aliasFields[0].substring(3);
    this.hostname = aliasFields.length > 1 ? aliasFields[1] : null;
    this.port = aliasFields.length > 2 ? Integer.valueOf(aliasFields[2]) : null;
  }

  public Type getType() {
    return type;
  }

  public String getAlias() {
    return alias;
  }

  public Integer getPort() {
    return port;
  }

  @Override
  public @NonNull String toString() {
    StringBuilder constructedAlias = new StringBuilder();
    constructedAlias.append(type.getPrefix());
    constructedAlias.append(alias);
    if (hostname != null) {
      constructedAlias.append(":");
      constructedAlias.append(hostname);
      if (port != null) {
        constructedAlias.append(":");
        constructedAlias.append(port);
      }
    }
    return constructedAlias.toString();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Alias)) {
      return false;
    }
    Alias other = (Alias) object;
    return Objects.equals(type, other.type) &&
        Objects.equals(alias, other.alias) &&
        Objects.equals(hostname, other.hostname) &&
        Objects.equals(port, other.port);
  }

  /**
   * Check if IKMAlias matches the filter
   *
   * @param filter IKMAlias object used as filter
   * @return true if each non-null field of filter equals the same field of this instance; false
   * otherwise Exception: both hostname fields are resolved to an ip address before comparing if
   * possible.
   */
  public boolean matches(@NonNull Alias filter) {
    if (filter.type != null && !filter.type.equals(type)) {
      Log.d(TAG, "matches: alias " + this + " does not match type " + filter.type);
      return false;
    }
    if (filter.alias != null && !filter.alias.equals(alias)) {
      Log.d(TAG, "matches: alias " + this + " does not match original alias " + filter.alias);
      return false;
    }
    if (hostname != null && filter.hostname != null && !filter.hostname.equals(hostname)) {
      // Resolve hostname fields to ip addresses
      InetAddress address = null, filterAddress = null;
      try {
        address = InetAddress.getByName(hostname);
      } catch (UnknownHostException e) {
        Log.w(TAG, "matches: error resolving " + hostname);
      }
      try {
        filterAddress = InetAddress.getByName(filter.hostname);
      } catch (UnknownHostException e) {
        Log.w(TAG, "matches: error resolving " + filter.hostname);
      }
      // If resolution succeeded, compare addresses, otherwise host names
      if ((address == null || !address.equals(filterAddress))) {
        Log.d(TAG,
            "matches: alias " + this + " (address=" + address + ") does not match hostname " +
                filter.hostname + " (address=" + filterAddress + ")");
        return false;
      }
    }
    if (port != null && filter.port != null && !filter.port.equals(port)) {
      Log.d(TAG, "matches: alias " + this + " does not match port " + filter.port);
      return false;
    }
    return true;
  }
}