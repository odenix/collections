/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2020 JetBrains s.r.o.
 * Copyright 2026 The Odenix Collections Authors
 */
package tests.contract.set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.odenix.collections.PersistentSet;
import org.odenix.collections.implementations.immutableSet.PersistentHashSet;

import tests.IntWrapper;

class PersistentHashSetBuilderTest {

  @Test
  void shouldCorrectlyIterateAfterRemovingIntegerElement() {
    var removedElement = 0;
    @SuppressWarnings("unchecked")
    var set = (PersistentHashSet<Integer>) PersistentSet.hashOf(1, 2, 3, removedElement, 32);

    validate(set, removedElement);
  }

  @Test
  void shouldCorrectlyIterateAfterRemovingIntWrapperElement() {
    var removedElement = new IntWrapper(0, 0);
    @SuppressWarnings("unchecked")
    var set =
        (PersistentHashSet<IntWrapper>)
            PersistentSet.hashOf(
                removedElement,
                new IntWrapper(1, 0),
                new IntWrapper(2, 32),
                new IntWrapper(3, 32));

    validate(set, removedElement);
  }

  private static <E extends @Nullable Object> void validate(PersistentHashSet<E> set, E removedElement) {
    var builder = set.builder();
    var iterator = builder.iterator();

    var expectedCount = set.size();
    var actualCount = 0;

    while (iterator.hasNext()) {
      var element = iterator.next();
      if (element.equals(removedElement)) {
        iterator.remove();
      }
      actualCount++;
    }

    var resultSet = builder.build();
    for (var element : set) {
      if (!element.equals(removedElement)) {
        assertTrue(resultSet.contains(element));
      } else {
        assertFalse(resultSet.contains(element));
      }
    }

    assertEquals(expectedCount, actualCount);
  }

  @Test
  void removingTwiceOnIteratorsThrowsIllegalStateException() {
    @SuppressWarnings("unchecked")
    var set = (PersistentHashSet<Integer>) PersistentSet.hashOf(1, 2, 3, 0, 32);
    var builder = set.builder();
    var iterator = builder.iterator();

    assertThrows(
        IllegalStateException.class,
        () -> {
          while (iterator.hasNext()) {
            var element = iterator.next();
            if (element == 0) {
              iterator.remove();
            }
            if (element == 0) {
              iterator.remove();
              iterator.remove();
            }
          }
        });
  }

  @Test
  void removingElementsFromDifferentIteratorsThrowsConcurrentModificationException() {
    @SuppressWarnings("unchecked")
    var set = (PersistentHashSet<Integer>) PersistentSet.hashOf(1, 2, 3, 0, 32);
    var builder = set.builder();
    var iterator1 = builder.iterator();
    var iterator2 = builder.iterator();

    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          while (iterator1.hasNext()) {
            var element1 = iterator1.next();
            iterator2.next();
            if (element1 == 0) {
              iterator1.remove();
            }
            if (element1 == 2) {
              iterator2.remove();
            }
          }
        });
  }

  @Test
  void removingElementFromOneIteratorAndAccessingAnotherThrowsConcurrentModificationException() {
    var set = PersistentSet.hashOf(1, 2, 3);
    var builder = set.builder();
    var iterator1 = builder.iterator();
    var iterator2 = builder.iterator();

    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          iterator1.next();
          iterator1.remove();
          iterator2.next();
        });
  }
}
