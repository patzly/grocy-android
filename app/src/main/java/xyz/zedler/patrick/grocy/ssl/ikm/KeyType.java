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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Source: github.com/stephanritscher/InteractiveKeyManager
 */
public enum KeyType {

  RSA("RSA"),
  EC("EC", "ECDSA");

  private final Set<String> names;

  KeyType(String... names) {
    this.names = new HashSet<>(Arrays.asList(names));
  }

  public Set<String> getNames() {
    return names;
  }

  public static KeyType parse(String keyType) {
    for (KeyType type : KeyType.values()) {
      if (type.getNames().contains(keyType)) {
        return type;
      }
    }
    throw new IllegalArgumentException("unknown prefix");
  }

  public static Set<KeyType> parse(Iterable<String> keyTypes) {
    Set<KeyType> keyTypeSet = new HashSet<>();
    if (keyTypes != null) {
      for (String keyType : keyTypes) {
        keyTypeSet.add(parse(keyType));
      }
    }
    return keyTypeSet;
  }
}