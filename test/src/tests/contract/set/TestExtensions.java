/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

  @SafeVarargs
  static <E extends @Nullable Object> Set<E> setOf(E... elements) {
    return new LinkedHashSet<>(Arrays.asList(elements));
  }

  static <E extends @Nullable Object> E[] emptyElements() {
    @SuppressWarnings("unchecked")
    var empty = (E[]) new Object[0];
    return empty;
  }

  static List<Character> chars(String content) {
    var result = new ArrayList<Character>(content.length());
    for (var index = 0; index < content.length(); index++) {
      result.add(content.charAt(index));
    }
    return result;
  }

  static <E extends @Nullable Object> void compareSets(Set<E> expected, Iterable<E> actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, toSet(actual));
  }

  static <E extends @Nullable Object> void compareOrderedSets(Set<E> expected, Iterable<E> actual) {
    org.junit.jupiter.api.Assertions.assertEquals(new ArrayList<>(expected), toMutableList(actual));
  }

  static <E extends @Nullable Object> HashSet<E> toMutableSet(Iterable<E> elements) {
    return new HashSet<>(toSet(elements));
  }

  static <E extends @Nullable Object> ArrayList<E> toMutableList(Iterable<E> elements) {
    var result = new ArrayList<E>();
    for (var element : elements) {
      result.add(element);
    }
    return result;
  }
}
