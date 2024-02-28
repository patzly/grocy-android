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