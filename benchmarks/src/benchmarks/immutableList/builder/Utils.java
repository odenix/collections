/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList.builder;

import org.odenix.collections.PersistentList;

import benchmarks.ImmutablePercentage;

final class Utils {
  private Utils() {}

  static PersistentList.Builder<String> persistentListBuilderAdd(int size, double immutablePercentage) {
    var immutableSize = ImmutablePercentage.immutableSize(size, immutablePercentage);
    var builder = benchmarks.immutableList.Utils.persistentListAdd(immutableSize).builder();
    for (var i = 0; i < size - immutableSize; i++) {
      builder.add("some element");
    }
    return builder;
  }
}
