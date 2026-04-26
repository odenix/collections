/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.set;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

final class TestExtensions {
  private TestExtensions() {}

  static <E extends @Nullable Object> List<E> addAll(
      Iterable<E> elements, Iterable<? extends E> other) {
    var result = toMutableList(elements);
    for (var element : other) {
      result.add(element);
    }
    return result;
  }

  static <E extends @Nullable Object> List<E> addAll(Iterable<E> elements, E element) {
    var result = toMutableList(elements);
    result.add(element);
    return result;
  }

  static <E extends @Nullable Object> E first(Iterable<E> elements) {
    return elements.iterator().next();
  }

  static <E extends @Nullable Object> Set<E> toSet(Iterable<E> elements) {
    var result = new LinkedHashSet<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }

  private static <E extends @Nullable Object> ArrayList<E> toMutableList(Iterable<E> elements) {
    var result = new ArrayList<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }
}
