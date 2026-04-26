/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap;

import java.util.List;

import org.odenix.collections.PersistentMap;

import benchmarks.HashCodeTypes;

public final class Utils {
  private static final int BRANCHING_FACTOR = 32;
  private static final int LOG_BRANCHING_FACTOR = 5;

  private Utils() {}

  public static <K, V> PersistentMap<K, V> emptyPersistentMap(String implementation) {
    return switch (implementation) {
      case HashCodeTypes.HASH_IMPL -> PersistentMap.hashOf();
      case HashCodeTypes.ORDERED_IMPL -> PersistentMap.of();
      default -> throw new AssertionError("Unknown PersistentMap implementation: " + implementation);
    };
  }

  public static <K> PersistentMap<K, String> persistentMapPut(String implementation, List<K> keys) {
    var map = Utils.<K, String>emptyPersistentMap(implementation);
    for (var key : keys) {
      map = map.put(key, "some value");
    }
    return map;
  }

  public static <K> PersistentMap<K, String> persistentMapRemove(PersistentMap<K, String> persistentMap, List<K> keys) {
    var map = persistentMap;
    for (var key : keys) {
      map = map.remove(key);
    }
    return map;
  }

  /// Returns the size of a persistent map whose expected height is
  /// half of the specified {@code persistentMap}'s expected height.
  public static int sizeForHalfHeight(PersistentMap<?, ?> persistentMap) {
    var expectedHeight = expectedHeightOfPersistentMapWithSize(persistentMap.size());
    return 1 << ((expectedHeight / 2) * LOG_BRANCHING_FACTOR);
  }

  private static int expectedHeightOfPersistentMapWithSize(int size) {
    return (int) Math.ceil(Math.log(size) / Math.log(BRANCHING_FACTOR));
  }
}
