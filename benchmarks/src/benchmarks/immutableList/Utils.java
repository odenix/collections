/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package benchmarks.immutableList;

import org.odenix.collections.PersistentList;

public final class Utils {
  private Utils() {}

  public static PersistentList<String> persistentListAdd(int size) {
    var list = PersistentList.<String>of();
    for (var i = 0; i < size; i++) {
      list = list.add("some element");
    }
    return list;
  }
}
