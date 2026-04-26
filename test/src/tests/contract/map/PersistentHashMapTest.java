/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentMap;
import org.odenix.collections.implementations.immutableMap.PersistentHashMap;

import tests.IntWrapper;

class PersistentHashMapTest {
  @Test
  void ifTheCollisionIsOfSize2AndOneOfTheKeysIsRemovedTheRemainingKeyMustBePromoted() {
    var map1 =
        (PersistentHashMap<Integer, String>)
            PersistentMap.hashOf(Map.entry(-1, "a"), Map.entry(0, "b"), Map.entry(32, "c"));
    var builder = map1.builder();
    var map2 = builder.build();

    assertTrue(map1.equals(builder));
    assertEquals(map1, TestExtensions.toMap(map2));
    assertEquals(map1, map2);

    var map3 = map1.remove(0);
    builder.remove(0);
    var map4 = builder.build();

    assertTrue(map3.equals(builder));
    assertEquals(map3, TestExtensions.toMap(map4));
    assertEquals(map3, map4);
  }

  @Test
  void builderShouldCorrectlyHandleMultipleElementRemovalsInCaseOfFullCollision() {
    var a = new IntWrapper(0, 0);
    var b = new IntWrapper(1, 0);
    var c = new IntWrapper(2, 0);

    var original =
        (PersistentHashMap<IntWrapper, String>)
            PersistentMap.hashOf(Map.entry(a, "a"), Map.entry(b, "b"), Map.entry(c, "c"));

    var onlyA = (PersistentHashMap<IntWrapper, String>) PersistentMap.hashOf(Map.entry(a, "a"));

    var builder = original.builder();
    builder.remove(b);
    builder.remove(c);
    var removedBC = builder.build();

    assertEquals(onlyA, removedBC);
  }

  @Test
  void builderShouldCorrectlyHandleMultipleElementRemovalsInCaseOfPartialCollision() {
    var a = new IntWrapper(0, 0);
    var b = new IntWrapper(1, 0);
    var c = new IntWrapper(2, 0);
    var d = new IntWrapper(3, 11);

    var original =
        (PersistentHashMap<IntWrapper, String>)
            PersistentMap.hashOf(Map.entry(a, "a"), Map.entry(b, "b"), Map.entry(c, "c"), Map.entry(d, "d"));

    var afterImmutableRemoving = original.remove(b).remove(c);

    var builder = original.builder();
    builder.remove(b);
    builder.remove(c);
    var afterMutableRemoving = builder.build();

    assertEquals(afterImmutableRemoving, afterMutableRemoving);
  }
}
