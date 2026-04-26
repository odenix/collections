/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableSet.builder;

import java.util.List;

import org.odenix.collections.PersistentSet;

import benchmarks.ImmutablePercentage;

final class Utils {
  private Utils() {}

  static <E> PersistentSet.Builder<E> persistentSetBuilderAdd(
      String implementation, List<E> elements, double immutablePercentage) {
    var immutableSize = ImmutablePercentage.immutableSize(elements.size(), immutablePercentage);

    var set = benchmarks.immutableSet.Utils.<E>emptyPersistentSet(implementation);
    for (var index = 0; index < immutableSize; index++) {
      set = set.add(elements.get(index));
    }

    var builder = set.builder();
    for (var index = immutableSize; index < elements.size(); index++) {
      builder.add(elements.get(index));
    }

    return builder;
  }
}
