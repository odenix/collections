/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;

class KT41278Test {
  private static void doContainsTest(
      Map<String, Integer> map,
      String key,
      int value,
      BiFunction<String, Integer, Map.Entry<String, Integer>> createEntry) {
    assertTrue(map.keySet().contains(key));
    assertEquals(value, map.get(key));
    assertTrue(map.entrySet().contains(createEntry.apply(key, value)));
    assertTrue(map.entrySet().stream().toList().contains(createEntry.apply(key, value)));

    assertFalse(map.entrySet().contains(null));
    assertFalse(map.entrySet().contains("not an entry"));
  }

  private static void doRemoveTest(
      Map<String, Integer> map,
      String key,
      int value,
      BiFunction<String, Integer, Map.Entry<String, Integer>> createEntry) {
    assertTrue(map.keySet().contains(key));
    assertEquals(value, map.get(key));
    assertTrue(map.entrySet().stream().toList().contains(createEntry.apply(key, value)));
    assertTrue(map.entrySet().remove(createEntry.apply(key, value)));
  }

  @Test
  void persistentOrderedMap() {
    var mapLetterToIndex = PersistentMap.<String, Integer>of();
    for (var i = 0; i < 26; i++) {
      mapLetterToIndex = mapLetterToIndex.put(Character.toString((char) ('a' + i)), i);
    }

    doContainsTest(TestExtensions.toMap(mapLetterToIndex), "h", 7, TestMapEntry::new);
    doContainsTest(TestExtensions.toMap(mapLetterToIndex), "h", 7, TestMutableMapEntry::new);

    doRemoveTest(mapLetterToIndex.builder(), "h", 7, TestMapEntry::new);
    doRemoveTest(mapLetterToIndex.builder(), "h", 7, TestMutableMapEntry::new);
  }

  @Test
  void persistentHashMap() {
    var mapLetterToIndex = PersistentMap.<String, Integer>hashOf();
    for (var i = 0; i < 26; i++) {
      mapLetterToIndex = mapLetterToIndex.put(Character.toString((char) ('a' + i)), i);
    }

    doContainsTest(TestExtensions.toMap(mapLetterToIndex), "h", 7, TestMapEntry::new);
    doContainsTest(TestExtensions.toMap(mapLetterToIndex), "h", 7, TestMutableMapEntry::new);

    doRemoveTest(mapLetterToIndex.builder(), "h", 7, TestMapEntry::new);
    doRemoveTest(mapLetterToIndex.builder(), "h", 7, TestMutableMapEntry::new);
  }

  @Test
  void persistentOrderedMapBuilder() {
    var mapLetterToIndex = PersistentMap.<String, Integer>of().builder();
    for (var i = 0; i < 26; i++) {
      mapLetterToIndex.put(Character.toString((char) ('a' + i)), i);
    }

    doContainsTest(mapLetterToIndex, "h", 7, TestMapEntry::new);
    doContainsTest(mapLetterToIndex, "h", 7, TestMutableMapEntry::new);

    doRemoveTest(mapLetterToIndex, "h", 7, TestMapEntry::new);
    doRemoveTest(mapLetterToIndex, "b", 1, TestMutableMapEntry::new);
  }

  @Test
  void persistentHashMapBuilder() {
    var mapLetterToIndex = PersistentMap.<String, Integer>hashOf().builder();
    for (var i = 0; i < 26; i++) {
      mapLetterToIndex.put(Character.toString((char) ('a' + i)), i);
    }

    doContainsTest(mapLetterToIndex, "h", 7, TestMapEntry::new);
    doContainsTest(mapLetterToIndex, "h", 7, TestMutableMapEntry::new);

    doRemoveTest(mapLetterToIndex, "h", 7, TestMapEntry::new);
    doRemoveTest(mapLetterToIndex, "b", 1, TestMutableMapEntry::new);
  }
}

final class TestMapEntry<K extends @Nullable Object, V extends @Nullable Object>
    implements Map.Entry<K, V> {
  private final K key;
  private final V value;

  TestMapEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return key + "=" + value;
  }

  @Override
  public int hashCode() {
    return key.hashCode() ^ value.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Map.Entry<?, ?> entry
        && key.equals(entry.getKey())
        && value.equals(entry.getValue());
  }
}

final class TestMutableMapEntry<K extends @Nullable Object, V extends @Nullable Object>
    extends SimpleEntry<K, V> {
  TestMutableMapEntry(K key, V value) {
    super(key, value);
  }
}
