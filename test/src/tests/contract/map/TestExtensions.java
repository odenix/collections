/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.odenix.collections.ImmutableMap;

final class TestExtensions {
  private TestExtensions() {}

  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMap(
      ImmutableMap<K, V> map) {
    var result = new HashMap<K, V>();
    for (var entry : map.entries()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  @SafeVarargs
  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> mapOf(
      Map.Entry<K, V>... entries) {
    var result = new HashMap<K, V>();
    for (var entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Map.Entry<K, V> entry(
      K key, V value) {
    return new SimpleImmutableEntry<>(key, value);
  }

  @SafeVarargs
  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> orderedMapOf(
      Map.Entry<K, V>... entries) {
    var result = new LinkedHashMap<K, V>();
    for (var entry : entries) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Map.Entry<K, V>[] emptyEntries() {
    @SuppressWarnings("unchecked")
    var typedArray = (Map.Entry<K, V>[]) new Map.Entry<?, ?>[0];
    return typedArray;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Map.Entry<K, V>[] toTypedArray(
      List<Map.Entry<K, V>> entries) {
    @SuppressWarnings("unchecked")
    var typedArray = (Map.Entry<K, V>[]) entries.toArray(Map.Entry[]::new);
    return typedArray;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> Map<K, V> toMutableMap(
      ImmutableMap<K, V> map) {
    return new HashMap<>(toMap(map));
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void compareMaps(
      Map<K, V> expected, ImmutableMap<K, V> actual) {
    Assertions.assertEquals(expected, toMap(actual));
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void compareMaps(
      Map<K, V> expected, Map<K, V> actual) {
    Assertions.assertEquals(expected, actual);
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void compareOrderedMaps(
      Map<K, V> expected, ImmutableMap<K, V> actual) {
    Assertions.assertEquals(entries(expected), entries(actual));
  }

  static <K extends @Nullable Object, V extends @Nullable Object> void compareOrderedMaps(
      Map<K, V> expected, Map<K, V> actual) {
    Assertions.assertEquals(entries(expected), entries(actual));
  }

  static <K extends @Nullable Object, V extends @Nullable Object> List<Map.Entry<K, V>> entries(
      Map<K, V> map) {
    var result = new ArrayList<Map.Entry<K, V>>(map.size());
    for (var entry : map.entrySet()) {
      result.add(new SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
    }
    return result;
  }

  static <K extends @Nullable Object, V extends @Nullable Object> List<Map.Entry<K, V>> entries(
      ImmutableMap<K, V> map) {
    var result = new ArrayList<Map.Entry<K, V>>(map.size());
    for (var entry : map.entries()) {
      result.add(new SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
    }
    return result;
  }

  static <K extends @Nullable Object> List<K> keys(ImmutableMap<K, ?> map) {
    var result = new ArrayList<K>(map.size());
    for (var key : map.keys()) {
      result.add(key);
    }
    return result;
  }

  static <V extends @Nullable Object> List<V> values(ImmutableMap<?, V> map) {
    var result = new ArrayList<V>(map.size());
    for (var value : map.values()) {
      result.add(value);
    }
    return result;
  }
}
