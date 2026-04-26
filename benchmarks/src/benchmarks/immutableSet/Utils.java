/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet;

import java.util.List;

import org.odenix.collections.PersistentSet;

import benchmarks.HashCodeTypes;

public final class Utils {
  private static final int BRANCHING_FACTOR = 32;
  private static final int LOG_BRANCHING_FACTOR = 5;

  private Utils() {}

  public static <E> PersistentSet<E> emptyPersistentSet(String implementation) {
    return switch (implementation) {
      case HashCodeTypes.HASH_IMPL -> PersistentSet.hashOf();
      case HashCodeTypes.ORDERED_IMPL -> PersistentSet.of();
      default -> throw new AssertionError("Unknown PersistentSet implementation: " + implementation);
    };
  }

  public static <E> PersistentSet<E> persistentSetAdd(String implementation, List<E> elements) {
    var set = Utils.<E>emptyPersistentSet(implementation);
    for (var element : elements) {
      set = set.add(element);
    }
    return set;
  }

  public static <E> PersistentSet<E> persistentSetRemove(PersistentSet<E> persistentSet, List<E> elements) {
    var set = persistentSet;
    for (var element : elements) {
      set = set.remove(element);
    }
    return set;
  }

  /// Returns the size of a persistent set whose expected height is
  /// half of the specified {@code persistentSet}'s expected height.
  public static int sizeForHalfHeight(PersistentSet<?> persistentSet) {
    var expectedHeight = expectedHeightOfPersistentSetWithSize(persistentSet.size());
    return 1 << ((expectedHeight / 2) * LOG_BRANCHING_FACTOR);
  }

  private static int expectedHeightOfPersistentSetWithSize(int size) {
    return (int) Math.ceil(Math.log(size) / Math.log(BRANCHING_FACTOR));
  }
}
