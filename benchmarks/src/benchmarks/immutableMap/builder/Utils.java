/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableMap.builder;

import java.util.List;

import org.odenix.collections.PersistentMap;

import benchmarks.ImmutablePercentage;
import benchmarks.ObjectWrapper;

final class Utils {
  private Utils() {}

  static PersistentMap.Builder<ObjectWrapper<Integer>, String> persistentMapBuilderPut(
      String implementation, List<ObjectWrapper<Integer>> keys, double immutablePercentage) {
    var immutableSize = ImmutablePercentage.immutableSize(keys.size(), immutablePercentage);

    var map = benchmarks.immutableMap.Utils.<ObjectWrapper<Integer>, String>emptyPersistentMap(implementation);
    for (var index = 0; index < immutableSize; index++) {
      map = map.put(keys.get(index), "some value");
    }

    var builder = map.builder();
    for (var index = immutableSize; index < keys.size(); index++) {
      builder.put(keys.get(index), "some value");
    }

    return builder;
  }
}
