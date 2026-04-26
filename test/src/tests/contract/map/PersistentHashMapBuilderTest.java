/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;

import tests.IntWrapper;

class PersistentHashMapBuilderTest {

  @Test
  void shouldCorrectlyIterateAfterRemovingIntegerKeyAndPromotionCollidingKeyDuringIteration() {
    var removedKey = 0;
    PersistentHashMap<Integer, String> map =
        (PersistentHashMap<Integer, String>)
            PersistentMap.hashOf(
                Map.entry(1, "a"),
                Map.entry(2, "b"),
                Map.entry(3, "c"),
                Map.entry(removedKey, "y"),
                Map.entry(32, "z"));

    validatePromotion(map, removedKey);
  }

  @Test
  void shouldCorrectlyIterateAfterRemovingIntWrapperKeyAndPromotionCollidingKeyDuringIteration() {
    var removedKey = new IntWrapper(0, 0);
    PersistentHashMap<IntWrapper, String> map =
        (PersistentHashMap<IntWrapper, String>)
            PersistentMap.hashOf(
                Map.entry(removedKey, "a"),
                Map.entry(new IntWrapper(1, 0), "b"),
                Map.entry(new IntWrapper(2, 32), "c"),
                Map.entry(new IntWrapper(3, 32), "d"));

    validatePromotion(map, removedKey);
  }

  private static <K extends @Nullable Object> void validatePromotion(
      PersistentHashMap<K, ?> map, K removedKey) {
    var builder = map.builder();
    var iterator = builder.entrySet().iterator();

    var expectedCount = map.size();
    var actualCount = 0;

    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (entry.getKey().equals(removedKey)) {
        iterator.remove();
      }
      actualCount++;
    }

    var resultMap = builder.build();
    for (var entry : map.entries()) {
      var key = entry.getKey();
      var value = entry.getValue();
      if (!key.equals(removedKey)) {
        assertTrue(resultMap.containsKey(key));
        assertEquals(resultMap.get(key), value);
      } else {
        assertFalse(resultMap.containsKey(key));
      }
    }

    assertEquals(expectedCount, actualCount);
  }

  @Test
  void removingTwiceOnIteratorsThrowsIllegalStateException() {
    PersistentHashMap<Integer, String> map =
        (PersistentHashMap<Integer, String>)
            PersistentMap.hashOf(
                Map.entry(1, "a"),
                Map.entry(2, "b"),
                Map.entry(3, "c"),
                Map.entry(0, "y"),
                Map.entry(32, "z"));
    var builder = map.builder();
    var iterator = builder.entrySet().iterator();

    assertThrows(
        IllegalStateException.class,
        () -> {
          while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey() == 0) {
              iterator.remove();
            }
            if (entry.getKey() == 0) {
              iterator.remove();
              iterator.remove();
            }
          }
        });
  }

  @Test
  void removingElementsFromDifferentIteratorsThrowsConcurrentModificationException() {
    PersistentHashMap<Integer, String> map =
        (PersistentHashMap<Integer, String>)
            PersistentMap.hashOf(
                Map.entry(1, "a"),
                Map.entry(2, "b"),
                Map.entry(3, "c"),
                Map.entry(0, "y"),
                Map.entry(32, "z"));
    var builder = map.builder();
    var iterator1 = builder.entrySet().iterator();
    var iterator2 = builder.entrySet().iterator();

    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          while (iterator1.hasNext()) {
            var entry = iterator1.next();
            iterator2.next();
            if (entry.getKey() == 0) {
              iterator1.remove();
            }
            if (entry.getKey() == 2) {
              iterator2.remove();
            }
          }
        });
  }

  @Test
  void removingElementFromOneIteratorAndAccessingAnotherThrowsConcurrentModificationException() {
    var map = PersistentMap.hashOf(Map.entry(1, "a"), Map.entry(2, "b"), Map.entry(3, "c"));
    var builder = map.builder();
    var iterator1 = builder.entrySet().iterator();
    var iterator2 = builder.entrySet().iterator();

    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          iterator1.next();
          iterator1.remove();
          iterator2.next();
        });
  }
}
