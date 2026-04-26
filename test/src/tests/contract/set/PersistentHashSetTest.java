/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentSet;
import org.odenix.collections.implementations.immutableSet.PersistentHashSet;

class PersistentHashSetTest {

  @Test
  void persistentHashSetAndTheirBuilderShouldBeEqualBeforeAndAfterModification() {
    var set1 = PersistentSet.hashOf(-1, 0, 32);
    var builder = set1.builder();

    assertTrue(set1.equals(builder));
    assertEquals(set1, builder.build());
    assertEquals(set1, PersistentSet.hashFrom(builder.build()));

    var set2 = set1.remove(0);
    builder.remove(0);

    assertEquals(set2, PersistentSet.hashFrom(builder.build()));
    assertEquals(set2, builder.build());
  }

  @Test
  void removingMultipleBatchesShouldLeaveOnlyRemainingElements() {
    var firstBatch = List.of(4554, 9380, 4260, 6602);
    var secondBatch = List.of(1188, 14794);
    var extraElement = 7450;

    var set = PersistentSet.<Integer>hashOf().addAll(firstBatch).addAll(secondBatch).add(extraElement);
    var result = set.removeAll(PersistentSet.hashFrom(firstBatch)).removeAll(secondBatch);
    assertEquals(1, result.size());
    assertEquals(extraElement, result.iterator().next());
  }

  @Test
  void afterRemovingElementsFromOneCollisionTheRemainingOneElementMustBePromotedToTheRoot() {
    @SuppressWarnings("unchecked")
    var set1 = (PersistentHashSet<Integer>) PersistentSet.hashOf(0, 32768, 65536);
    @SuppressWarnings("unchecked")
    var set2 = (PersistentHashSet<Integer>) PersistentSet.hashOf(0, 32768);

    var expected = PersistentSet.hashOf(65536);
    var actual = set1.removeAll(set2);

    assertEquals(expected, actual);
  }
}
