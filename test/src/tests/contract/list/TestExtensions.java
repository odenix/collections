/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.list;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.odenix.collections.ImmutableList;
import org.odenix.collections.PersistentList;

final class TestExtensions {
  private TestExtensions() {}

  static <T extends @Nullable Object, R extends @Nullable Object> List<R> map(
      List<T> list, Function<? super T, ? extends R> transform) {
    var result = new ArrayList<R>();
    for (var element : list) {
      result.add(transform.apply(element));
    }
    return result;
  }

  static <E extends @Nullable Object> @Nullable E firstOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  static <E extends @Nullable Object> E first(List<E> list) {
    return list.get(0);
  }

  static <E extends @Nullable Object> @Nullable E firstOrNull(ImmutableList<E> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  static <E extends @Nullable Object> E first(ImmutableList<E> list) {
    return list.get(0);
  }

  static <E extends @Nullable Object> @Nullable E lastOrNull(List<E> list) {
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  static <E extends @Nullable Object> E last(List<E> list) {
    return list.get(list.size() - 1);
  }

  static <E extends @Nullable Object> @Nullable E lastOrNull(ImmutableList<E> list) {
    return list.isEmpty() ? null : list.get(list.size() - 1);
  }

  static <E extends @Nullable Object> E last(ImmutableList<E> list) {
    return list.get(list.size() - 1);
  }

  static <E extends @Nullable Object> List<E> toList(List<E> list) {
    return new ArrayList<>(list);
  }

  static <E extends @Nullable Object> List<E> toList(Iterable<E> iterable) {
    var result = new ArrayList<E>();
    for (var element : iterable) {
      result.add(element);
    }
    return result;
  }

  static <E extends @Nullable Object> List<E> toList(ImmutableList<E> list) {
    return toList((Iterable<E>) list);
  }

  static <E extends @Nullable Object> List<E> toMutableList(List<E> list) {
    return new ArrayList<>(list);
  }

  static <E extends @Nullable Object> List<E> toMutableList(ImmutableList<E> list) {
    return toList(list);
  }

  static <E extends @Nullable Object> List<E> toMutableList(
      PersistentList.Builder<E> list) {
    return new ArrayList<>(list);
  }
}
