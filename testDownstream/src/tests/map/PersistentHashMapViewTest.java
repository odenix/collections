/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.PersistentSet;

class PersistentHashMapViewTest {
  @Test
  void hashMapEntryAndKeyViewsUseImmutableSetEquality() {
    var hashMap = PersistentMap.hashOf(Map.entry("a", 1), Map.entry("b", 2));
    var orderedMap = PersistentMap.of(Map.entry("a", 1), Map.entry("b", 2));

    assertEquals(orderedMap.entries(), hashMap.entries());
    assertEquals(hashMap.entries(), orderedMap.entries());
    assertEquals(orderedMap.keys(), hashMap.keys());
    assertEquals(hashMap.keys(), orderedMap.keys());
  }

  @Test
  void entryViewsAreNotEqualToNonEntryImmutableSets() {
    var hashMap = PersistentMap.hashOf(Map.entry("a", 1));
    var orderedMap = PersistentMap.of(Map.entry("a", 1));
    var nonEntrySet = PersistentSet.of("a");

    assertDoesNotThrow(() -> assertNotEquals(nonEntrySet, hashMap.entries()));
    assertDoesNotThrow(() -> assertNotEquals(nonEntrySet, orderedMap.entries()));
  }

  @Test
  void builderEntrySetValueInvalidatesBuiltMap() {
    // Upstream-pending: fix-ordered-map-builder-entry-set-value-cache.
    var hashBuilder = PersistentMap.hashOf(Map.entry("a", 1)).builder();
    hashBuilder.entrySet().iterator().next().setValue(2);

    var orderedBuilder = PersistentMap.of(Map.entry("a", 1)).builder();
    orderedBuilder.entrySet().iterator().next().setValue(2);

    assertEquals(2, hashBuilder.build().get("a"));
    assertEquals(2, orderedBuilder.build().get("a"));
  }

  @Test
  void hashBuilderEntryIteratorRemoveAllowsNullKey() {
    var builder = PersistentMap.<String, Integer>hashOf().put(null, 1).put("a", 2).builder();
    var iterator = builder.entrySet().iterator();

    assertEquals(null, iterator.next().getKey());
    // Kotlin's nullable hashCode maps null to 0; the Java reset path must not require a non-null key.
    assertDoesNotThrow(iterator::remove);

    assertEquals(PersistentMap.<String, Integer>hashOf().put("a", 2), builder.build());
  }
}
