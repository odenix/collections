/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.stress.set;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

final class TestExtensions {
  private TestExtensions() {}

  static <E extends @Nullable Object> Set<E> toSet(Iterable<E> elements) {
    var result = new LinkedHashSet<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }
}
