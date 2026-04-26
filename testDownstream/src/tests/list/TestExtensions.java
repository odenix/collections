/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.list;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

final class TestExtensions {
  private TestExtensions() {}

  static <E extends @Nullable Object> List<E> toList(Iterable<E> iterable) {
    var result = new ArrayList<E>();
    for (var element : iterable) {
      result.add(element);
    }
    return result;
  }
}
