/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package org.odenix.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("UnusedReturnValue")
final class Extensions {
  private Extensions() {}

  static <E extends @Nullable Object> boolean removeAll(
      Collection<E> collection, Iterable<? extends E> elements) {
    return collection.removeAll(convertToListIfNotCollection(elements));
  }

  static <T extends @Nullable Object> boolean removeAll(
      Collection<? super T> collection, T[] elements) {
    return elements.length != 0 && collection.removeAll(Arrays.asList(elements));
  }

  static <E extends @Nullable Object> boolean addAll(
      Collection<E> collection, Iterable<? extends E> elements) {
    var modified = false;
    for (var element : elements) {
      modified |= collection.add(element);
    }
    return modified;
  }

  static <E extends @Nullable Object> boolean addAll(
      Collection<E> collection, Stream<? extends E> elements) {
    var modified = false;
    for (var element : elements.toList()) {
      modified |= collection.add(element);
    }
    return modified;
  }

  static <E extends @Nullable Object> boolean removeAll(
      Collection<E> collection, Stream<? extends E> elements) {
    var list = elements.toList();
    return !list.isEmpty() && collection.removeAll(list);
  }

  static <E extends @Nullable Object> boolean retainAll(
      Collection<E> collection, Iterable<? extends E> elements) {
    return collection.retainAll(convertToListIfNotCollection(elements));
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void putAll(
      Map<K, V> map, Iterable<? extends Map.Entry<? extends K, ? extends V>> pairs) {
    for (var pair : pairs) {
      map.put(pair.getKey(), pair.getValue());
    }
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void putAll(
      Map<K, V> map, Map.Entry<? extends K, ? extends V>[] pairs) {
    for (var pair : pairs) {
      map.put(pair.getKey(), pair.getValue());
    }
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void putAll(
      Map<K, V> map, Stream<? extends Map.Entry<? extends K, ? extends V>> pairs) {
    pairs.forEachOrdered(pair -> map.put(pair.getKey(), pair.getValue()));
  }

  static <K extends @Nullable Object> void minusAssign(
      Map<K, ?> map, Iterable<? extends K> keys) {
    for (K key : keys) {
      map.remove(key);
    }
  }

  static <K extends @Nullable Object> void minusAssign(Map<K, ?> map, K[] keys) {
    for (K key : keys) {
      map.remove(key);
    }
  }

  static <K extends @Nullable Object> void minusAssign(
      Map<K, ?> map, Stream<? extends K> keys) {
    keys.forEachOrdered(map::remove);
  }

  private static <T extends @Nullable Object> Collection<T> convertToListIfNotCollection(
      Iterable<T> iterable) {
    if (iterable instanceof Collection<T> collection) {
      return collection;
    }
    var list = new ArrayList<T>();
    for (T element : iterable) {
      list.add(element);
    }
    return list;
  }
}
